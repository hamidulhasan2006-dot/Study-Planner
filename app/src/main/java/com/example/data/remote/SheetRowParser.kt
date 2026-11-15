package com.example.data.remote

import com.example.data.model.StudyTask
import com.example.data.model.SyncState
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.util.DateHelper

object SheetRowParser {

    fun parseRows(rawRows: List<List<String>>): List<StudyTask> {
        if (rawRows.isEmpty()) return emptyList()

        val firstRow = rawRows.first().map { it.trim().lowercase() }
        val hasHeader = firstRow.any {
            it == "task id" || it == "task title" || it == "title" || it == "subject"
        }

        val columnMap = if (hasHeader) {
            buildColumnIndexMap(firstRow)
        } else {
            defaultColumnIndexMap()
        }

        val dataRows = if (hasHeader) rawRows.drop(1) else rawRows
        val now = System.currentTimeMillis()

        return dataRows.mapNotNull { row ->
            if (row.all { it.isBlank() }) return@mapNotNull null
            val title = row.getCell(columnMap.titleIdx)
            if (title.isBlank()) return@mapNotNull null

            val rawId = row.getCell(columnMap.idIdx)
            val id = rawId.ifBlank { StudyTask.generateTaskId() }
            val subject = row.getCell(columnMap.subjectIdx).ifBlank { "General" }
            val taskType = TaskType.fromRaw(row.getCell(columnMap.typeIdx))
            val deadline = DateHelper.normalizeDateString(row.getCell(columnMap.deadlineIdx))
            val priority = TaskPriority.fromRaw(row.getCell(columnMap.priorityIdx))
            val status = TaskStatus.fromRaw(row.getCell(columnMap.statusIdx))
            val notes = row.getCell(columnMap.notesIdx)

            StudyTask(
                id = id,
                title = title,
                subject = subject,
                taskType = taskType,
                deadline = deadline,
                priority = priority,
                status = status,
                notes = notes,
                syncState = if (rawId.isBlank()) SyncState.PENDING_UPSERT else SyncState.SYNCED,
                updatedAt = now
            )
        }
    }

    fun toSheetValues(tasks: List<StudyTask>): List<List<String>> {
        val sorted = tasks.sortedWith(
            compareBy<StudyTask> { it.isCompleted }
                .thenBy { it.deadline }
                .thenBy { it.priority.sortRank }
        )
        return buildList {
            add(StudyTask.SHEET_HEADERS)
            sorted.forEach { add(it.toSheetRow()) }
        }
    }

    /**
     * Deterministic two-way reconciliation between local Room tasks and remote Google Sheet rows:
     * - Local PENDING_DELETE deletes the row from the merged result.
     * - Local PENDING_UPSERT overrides or appends to remote rows.
     * - Remote additions/edits (for SYNCED or new IDs) are preserved in the merged result.
     * - Tasks that were previously SYNCED locally but removed from the Sheet are removed locally.
     */
    fun mergeTwoWay(
        localTasks: List<StudyTask>,
        remoteTasks: List<StudyTask>
    ): List<StudyTask> {
        val deletedIds = localTasks
            .filter { it.syncState == SyncState.PENDING_DELETE }
            .map { it.id }
            .toSet()

        val pendingUpserts = localTasks
            .filter { it.syncState == SyncState.PENDING_UPSERT }
            .associateBy { it.id }

        val mergedMap = LinkedHashMap<String, StudyTask>()

        // 1. Start with remote rows, skipping locally deleted tasks
        for (remote in remoteTasks) {
            if (remote.id in deletedIds) continue
            val localOverride = pendingUpserts[remote.id]
            if (localOverride != null) {
                mergedMap[remote.id] = localOverride.copy(syncState = SyncState.SYNCED)
            } else {
                mergedMap[remote.id] = remote.copy(syncState = SyncState.SYNCED)
            }
        }

        // 2. Append new local tasks not yet on the sheet
        for ((id, pending) in pendingUpserts) {
            if (!mergedMap.containsKey(id) && id !in deletedIds) {
                mergedMap[id] = pending.copy(syncState = SyncState.SYNCED)
            }
        }

        return mergedMap.values.toList()
    }

    private data class ColumnIndexMap(
        val idIdx: Int,
        val titleIdx: Int,
        val subjectIdx: Int,
        val typeIdx: Int,
        val deadlineIdx: Int,
        val priorityIdx: Int,
        val statusIdx: Int,
        val notesIdx: Int
    )

    private fun defaultColumnIndexMap() = ColumnIndexMap(
        idIdx = 0,
        titleIdx = 1,
        subjectIdx = 2,
        typeIdx = 3,
        deadlineIdx = 4,
        priorityIdx = 5,
        statusIdx = 6,
        notesIdx = 7
    )

    private fun buildColumnIndexMap(headers: List<String>): ColumnIndexMap {
        fun findIndex(vararg aliases: String, fallback: Int): Int {
            val found = headers.indexOfFirst { header -> aliases.any { header.contains(it) } }
            return if (found >= 0) found else fallback
        }
        return ColumnIndexMap(
            idIdx = findIndex("task id", "id", fallback = 0),
            titleIdx = findIndex("task title", "title", "task", fallback = 1),
            subjectIdx = findIndex("subject", "course", fallback = 2),
            typeIdx = findIndex("task type", "type", "category", fallback = 3),
            deadlineIdx = findIndex("deadline", "due", "date", fallback = 4),
            priorityIdx = findIndex("priority", fallback = 5),
            statusIdx = findIndex("status", "state", fallback = 6),
            notesIdx = findIndex("notes", "note", "description", fallback = 7)
        )
    }

    private fun List<String>.getCell(index: Int): String =
        if (index in indices) this[index].trim() else ""
}
