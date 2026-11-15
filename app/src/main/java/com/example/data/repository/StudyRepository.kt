package com.example.data.repository

import com.example.data.local.SyncPreferences
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.SheetConfig
import com.example.data.model.SheetConnectionMode
import com.example.data.model.StudyTask
import com.example.data.model.SyncState
import com.example.data.model.TaskStatus
import com.example.data.remote.AppsScriptClient
import com.example.data.remote.CreateSpreadsheetRequestDto
import com.example.data.remote.GoogleSheetsApi
import com.example.data.remote.SheetMetaDto
import com.example.data.remote.SheetPropertiesDto
import com.example.data.remote.SheetRowParser
import com.example.data.remote.SpreadsheetPropertiesDto
import com.example.data.remote.ValueRangeDto
import com.example.util.DateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URLEncoder

class StudyRepository(
    private val taskDao: TaskDao,
    private val syncPreferences: SyncPreferences,
    private val googleSheetsApi: GoogleSheetsApi,
    private val appsScriptClient: AppsScriptClient
) {
    val activeTasksFlow: Flow<List<StudyTask>> =
        taskDao.observeActiveTasks().map { entities -> entities.map { it.toDomain() } }

    val sheetConfigFlow: Flow<SheetConfig> = syncPreferences.sheetConfigFlow

    suspend fun upsertTask(task: StudyTask) {
        val updated = task.copy(
            syncState = SyncState.PENDING_UPSERT,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.upsertTask(TaskEntity.fromDomain(updated))
    }

    suspend fun toggleTaskCompleted(task: StudyTask) {
        val nextStatus = if (task.status == TaskStatus.COMPLETED) {
            TaskStatus.NOT_STARTED
        } else {
            TaskStatus.COMPLETED
        }
        upsertTask(task.copy(status = nextStatus))
    }

    suspend fun cycleTaskStatus(task: StudyTask) {
        val nextStatus = when (task.status) {
            TaskStatus.NOT_STARTED -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> TaskStatus.NOT_STARTED
        }
        upsertTask(task.copy(status = nextStatus))
    }

    suspend fun deleteTask(task: StudyTask) {
        val config = sheetConfigFlow.first()
        if (config.isConfigured) {
            val marked = task.copy(
                syncState = SyncState.PENDING_DELETE,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.upsertTask(TaskEntity.fromDomain(marked))
        } else {
            taskDao.hardDeleteById(task.id)
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        syncPreferences.setAutoSync(enabled)
    }

    suspend fun disconnectSheet() {
        syncPreferences.disconnectSheet()
    }

    suspend fun connectAppsScript(webAppUrl: String, tabName: String): Result<String> = runCatching {
        val cleanUrl = webAppUrl.trim()
        require(cleanUrl.startsWith("https://")) {
            "Web App URL must start with https:// (e.g., https://script.google.com/macros/s/.../exec)"
        }
        val cleanTab = tabName.trim().ifBlank { "Tasks" }
        val response = appsScriptClient.fetchRows(cleanUrl, cleanTab)
        syncPreferences.saveAppsScriptUrl(cleanUrl, cleanTab)
        if (!response.title.isNullOrBlank()) {
            syncPreferences.updateLastSynced(0L, response.title)
        }
        syncWithAppsScript(cleanUrl, cleanTab, pullOnly = false).getOrThrow()
    }

    suspend fun connectExistingGoogleSheet(
        accessToken: String,
        spreadsheetIdOrUrl: String,
        tabName: String
    ): Result<String> = runCatching {
        val id = DateHelper.extractSpreadsheetId(spreadsheetIdOrUrl)
        require(id.isNotBlank()) { "Please enter a valid Google Sheet link or Spreadsheet ID." }
        val bearer = "Bearer $accessToken"
        val meta = googleSheetsApi.getSpreadsheetMetadata(bearer, id)
        val availableTabs = meta.sheets.orEmpty().mapNotNull { it.properties?.title }
        val resolvedTab = when {
            tabName.isNotBlank() && (availableTabs.isEmpty() || tabName in availableTabs) -> tabName.trim()
            availableTabs.isNotEmpty() -> availableTabs.first()
            else -> "Tasks"
        }
        syncPreferences.saveGoogleOAuthSheet(
            spreadsheetId = id,
            spreadsheetTitle = meta.properties?.title ?: "Study Planner Sheet",
            tabName = resolvedTab
        )
        syncWithGoogleSheetsApi(accessToken, id, resolvedTab, pullOnly = false).getOrThrow()
    }

    suspend fun createNewGoogleSheet(
        accessToken: String,
        title: String,
        tabName: String
    ): Result<String> = runCatching {
        val cleanTitle = title.trim().ifBlank { "Student Study Planner" }
        val cleanTab = tabName.trim().ifBlank { "Tasks" }
        val bearer = "Bearer $accessToken"
        val created = googleSheetsApi.createSpreadsheet(
            bearerToken = bearer,
            request = CreateSpreadsheetRequestDto(
                properties = SpreadsheetPropertiesDto(title = cleanTitle),
                sheets = listOf(SheetMetaDto(properties = SheetPropertiesDto(title = cleanTab)))
            )
        )
        val newId = created.spreadsheetId
            ?: throw IllegalStateException("Google Sheets did not return a spreadsheet ID.")
        syncPreferences.saveGoogleOAuthSheet(
            spreadsheetId = newId,
            spreadsheetTitle = created.properties?.title ?: cleanTitle,
            tabName = cleanTab
        )
        syncWithGoogleSheetsApi(accessToken, newId, cleanTab, pullOnly = false).getOrThrow()
    }

    suspend fun syncNow(accessToken: String? = null, pullOnly: Boolean = false): Result<String> {
        val config = syncPreferences.sheetConfigFlow.first()
        return when (config.mode) {
            SheetConnectionMode.NONE -> {
                Result.failure(IllegalStateException("Connect a Google Sheet first in the Google Sheets tab."))
            }
            SheetConnectionMode.APPS_SCRIPT -> {
                syncWithAppsScript(config.appsScriptUrl, config.sheetTabName, pullOnly)
            }
            SheetConnectionMode.GOOGLE_OAUTH -> {
                if (accessToken.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Google authorization token required."))
                } else {
                    syncWithGoogleSheetsApi(
                        accessToken = accessToken,
                        spreadsheetId = config.spreadsheetId,
                        tabName = config.sheetTabName,
                        pullOnly = pullOnly
                    )
                }
            }
        }
    }

    private suspend fun syncWithAppsScript(
        webAppUrl: String,
        tabName: String,
        pullOnly: Boolean
    ): Result<String> = runCatching {
        val remoteResponse = appsScriptClient.fetchRows(webAppUrl, tabName)
        val remoteTasks = SheetRowParser.parseRows(remoteResponse.rows.orEmpty())
        val localTasks = taskDao.getAllTasksIncludingPendingDeletes().map { it.toDomain() }

        val finalTasks = if (pullOnly) {
            remoteTasks.map { it.copy(syncState = SyncState.SYNCED) }
        } else {
            val merged = SheetRowParser.mergeTwoWay(localTasks, remoteTasks)
            val sheetRows = SheetRowParser.toSheetValues(merged)
            appsScriptClient.writeRows(webAppUrl, tabName, sheetRows)
            merged
        }

        taskDao.replaceSyncedState(finalTasks.map { TaskEntity.fromDomain(it) })
        syncPreferences.updateLastSynced(System.currentTimeMillis(), remoteResponse.title)
        "Synced ${finalTasks.size} tasks with ${remoteResponse.title ?: "Google Sheet"}"
    }

    private suspend fun syncWithGoogleSheetsApi(
        accessToken: String,
        spreadsheetId: String,
        tabName: String,
        pullOnly: Boolean
    ): Result<String> = runCatching {
        val bearer = "Bearer $accessToken"
        val escapedTab = "'${tabName.replace("'", "''")}'"
        val readRange = encodeRange("$escapedTab!A1:H1000")

        val remoteValues = runCatching {
            googleSheetsApi.getValues(bearer, spreadsheetId, readRange).values.orEmpty()
        }.getOrElse { emptyList() }

        val remoteTasks = SheetRowParser.parseRows(remoteValues)
        val localTasks = taskDao.getAllTasksIncludingPendingDeletes().map { it.toDomain() }

        val finalTasks = if (pullOnly) {
            remoteTasks.map { it.copy(syncState = SyncState.SYNCED) }
        } else {
            val merged = SheetRowParser.mergeTwoWay(localTasks, remoteTasks)
            val sheetRows = SheetRowParser.toSheetValues(merged)
            val writeRange = encodeRange("$escapedTab!A1:H${sheetRows.size}")
            googleSheetsApi.updateValues(
                bearerToken = bearer,
                spreadsheetId = spreadsheetId,
                range = writeRange,
                body = ValueRangeDto(values = sheetRows)
            )
            // Clear leftover rows below the current table if previous sheet had more rows
            if (remoteValues.size > sheetRows.size) {
                val clearStart = sheetRows.size + 1
                val clearRange = encodeRange("$escapedTab!A$clearStart:H${remoteValues.size + 10}")
                runCatching {
                    googleSheetsApi.clearValues(bearer, spreadsheetId, clearRange)
                }
            }
            merged
        }

        taskDao.replaceSyncedState(finalTasks.map { TaskEntity.fromDomain(it) })
        syncPreferences.updateLastSynced(System.currentTimeMillis())
        "Synced ${finalTasks.size} tasks with Google Sheets"
    }

    private fun encodeRange(range: String): String =
        URLEncoder.encode(range, "UTF-8").replace("+", "%20")
}
