package com.example.ui.viewmodel

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DashboardSummary
import com.example.data.model.SheetConfig
import com.example.data.model.SheetConnectionMode
import com.example.data.model.SortOrder
import com.example.data.model.StudyTask
import com.example.data.model.SubjectProgress
import com.example.data.model.SyncStatusUi
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType
import com.example.data.remote.GoogleAuthHelper
import com.example.data.remote.GoogleAuthOutcome
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS("Focus", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val focusMinutes: Int = 25,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val completedSessions: Int = 0,
    val linkedTaskId: String? = null,
    val completionMessage: String? = null
) {
    val formattedRemaining: String
        get() = formatSeconds(remainingSeconds)

    companion object {
        fun formatSeconds(totalSeconds: Int): String {
            val safe = totalSeconds.coerceAtLeast(0)
            val mins = safe / 60
            val secs = safe % 60
            return "%02d:%02d".format(mins, secs)
        }
    }
}

data class TaskFilterState(
    val searchQuery: String = "",
    val selectedSubject: String? = null,
    val selectedStatus: TaskStatus? = null,
    val selectedPriority: TaskPriority? = null,
    val selectedType: TaskType? = null,
    val sortOrder: SortOrder = SortOrder.DEADLINE_SOONEST
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedSubject != null ||
            selectedStatus != null ||
            selectedPriority != null ||
            selectedType != null
}

enum class PendingOAuthActionType {
    SYNC_NOW,
    PULL_ONLY,
    CONNECT_EXISTING,
    CREATE_NEW
}

data class PendingOAuthAction(
    val type: PendingOAuthActionType,
    val param1: String = "",
    val param2: String = ""
)

class StudyPlannerViewModel(
    private val repository: StudyRepository,
    private val authHelper: GoogleAuthHelper
) : ViewModel() {

    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatusUi>(SyncStatusUi.Idle)
    val syncStatus: StateFlow<SyncStatusUi> = _syncStatus.asStateFlow()

    private val _pendingConsentIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingConsentIntent: StateFlow<PendingIntent?> = _pendingConsentIntent.asStateFlow()

    private val _pomodoroState = MutableStateFlow(PomodoroUiState())
    val pomodoroState: StateFlow<PomodoroUiState> = _pomodoroState.asStateFlow()

    private var timerJob: Job? = null
    private var pendingOAuthAction: PendingOAuthAction? = null

    val allTasks: StateFlow<List<StudyTask>> = repository.activeTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sheetConfig: StateFlow<SheetConfig> = repository.sheetConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SheetConfig())

    val availableSubjects: StateFlow<List<String>> = allTasks
        .map { tasks ->
            tasks.map { it.subject.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<StudyTask>> = combine(allTasks, _filterState) { tasks, filter ->
        applyFiltersAndSort(tasks, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = allTasks
        .map { tasks -> buildDashboardSummary(tasks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectSubjectFilter(subject: String?) {
        _filterState.value = _filterState.value.copy(selectedSubject = subject)
    }

    fun selectStatusFilter(status: TaskStatus?) {
        _filterState.value = _filterState.value.copy(selectedStatus = status)
    }

    fun selectPriorityFilter(priority: TaskPriority?) {
        _filterState.value = _filterState.value.copy(selectedPriority = priority)
    }

    fun selectTypeFilter(type: TaskType?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun cycleSortOrder() {
        val next = when (_filterState.value.sortOrder) {
            SortOrder.DEADLINE_SOONEST -> SortOrder.DEADLINE_LATEST
            SortOrder.DEADLINE_LATEST -> SortOrder.PRIORITY_HIGHEST
            SortOrder.PRIORITY_HIGHEST -> SortOrder.DEADLINE_SOONEST
        }
        _filterState.value = _filterState.value.copy(sortOrder = next)
    }

    fun setSortOrder(order: SortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = order)
    }

    fun clearFilters() {
        _filterState.value = TaskFilterState(sortOrder = _filterState.value.sortOrder)
    }

    fun saveTask(task: StudyTask) {
        viewModelScope.launch {
            repository.upsertTask(task)
            maybeAutoSync()
        }
    }

    fun toggleTaskComplete(task: StudyTask) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
            maybeAutoSync()
        }
    }

    fun cycleTaskStatus(task: StudyTask) {
        viewModelScope.launch {
            repository.cycleTaskStatus(task)
            maybeAutoSync()
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            maybeAutoSync()
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSync(enabled)
        }
    }

    fun disconnectSheet() {
        viewModelScope.launch {
            repository.disconnectSheet()
            _syncStatus.value = SyncStatusUi.Idle
        }
    }

    fun clearSyncMessage() {
        _syncStatus.value = SyncStatusUi.Idle
    }

    fun togglePomodoroRun() {
        val current = _pomodoroState.value
        if (current.isRunning) {
            pausePomodoro()
        } else {
            startPomodoro()
        }
    }

    fun startPomodoro() {
        val current = _pomodoroState.value
        val resetIfZero = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
        _pomodoroState.value = current.copy(
            remainingSeconds = resetIfZero,
            isRunning = true,
            completionMessage = null
        )
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.isRunning && _pomodoroState.value.remainingSeconds > 0) {
                delay(1000L)
                val latest = _pomodoroState.value
                if (!latest.isRunning) break
                val nextRemaining = (latest.remainingSeconds - 1).coerceAtLeast(0)
                if (nextRemaining == 0) {
                    _pomodoroState.value = onPhaseFinished(latest)
                    break
                } else {
                    _pomodoroState.value = latest.copy(remainingSeconds = nextRemaining)
                }
            }
        }
    }

    fun pausePomodoro() {
        timerJob?.cancel()
        timerJob = null
        _pomodoroState.value = _pomodoroState.value.copy(isRunning = false)
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        timerJob = null
        val current = _pomodoroState.value
        val seconds = durationSecondsFor(current.phase, current.focusMinutes)
        _pomodoroState.value = current.copy(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = false,
            completionMessage = null
        )
    }

    fun selectPomodoroPhase(phase: PomodoroPhase) {
        timerJob?.cancel()
        timerJob = null
        val current = _pomodoroState.value
        val seconds = durationSecondsFor(phase, current.focusMinutes)
        _pomodoroState.value = current.copy(
            phase = phase,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = false,
            completionMessage = null
        )
    }

    fun setPomodoroFocusMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(5, 90)
        timerJob?.cancel()
        timerJob = null
        val current = _pomodoroState.value
        val seconds = if (current.phase == PomodoroPhase.FOCUS) safeMinutes * 60 else current.totalSeconds
        _pomodoroState.value = current.copy(
            focusMinutes = safeMinutes,
            totalSeconds = if (current.phase == PomodoroPhase.FOCUS) seconds else current.totalSeconds,
            remainingSeconds = if (current.phase == PomodoroPhase.FOCUS) seconds else current.remainingSeconds,
            isRunning = if (current.phase == PomodoroPhase.FOCUS) false else current.isRunning,
            completionMessage = null
        )
    }

    fun linkTaskToPomodoro(task: StudyTask?) {
        val current = _pomodoroState.value
        if (task == null) {
            _pomodoroState.value = current.copy(linkedTaskId = null)
            return
        }
        val focusSeconds = current.focusMinutes * 60
        _pomodoroState.value = current.copy(
            linkedTaskId = task.id,
            phase = PomodoroPhase.FOCUS,
            totalSeconds = focusSeconds,
            remainingSeconds = focusSeconds,
            completionMessage = null
        )
        if (task.status == TaskStatus.NOT_STARTED) {
            saveTask(task.copy(status = TaskStatus.IN_PROGRESS))
        }
        startPomodoro()
    }

    fun dismissPomodoroBanner() {
        _pomodoroState.value = _pomodoroState.value.copy(completionMessage = null)
    }

    fun connectAppsScript(webAppUrl: String, tabName: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusUi.Syncing
            val result = repository.connectAppsScript(webAppUrl, tabName)
            _syncStatus.value = result.fold(
                onSuccess = { SyncStatusUi.Success(it) },
                onFailure = { SyncStatusUi.Error(it.message ?: "Failed to connect Google Sheet.") }
            )
        }
    }

    fun connectExistingGoogleSheet(spreadsheetIdOrUrl: String, tabName: String) {
        startGoogleAuthAction(
            PendingOAuthAction(
                type = PendingOAuthActionType.CONNECT_EXISTING,
                param1 = spreadsheetIdOrUrl,
                param2 = tabName
            )
        )
    }

    fun createNewGoogleSheet(title: String, tabName: String) {
        startGoogleAuthAction(
            PendingOAuthAction(
                type = PendingOAuthActionType.CREATE_NEW,
                param1 = title,
                param2 = tabName
            )
        )
    }

    fun syncNow(pullOnly: Boolean = false) {
        viewModelScope.launch {
            val config = repository.sheetConfigFlow.first()
            when (config.mode) {
                SheetConnectionMode.NONE -> {
                    _syncStatus.value = SyncStatusUi.Error("Connect a Google Sheet in the Google Sheets tab first.")
                }
                SheetConnectionMode.APPS_SCRIPT -> {
                    _syncStatus.value = SyncStatusUi.Syncing
                    val res = repository.syncNow(accessToken = null, pullOnly = pullOnly)
                    _syncStatus.value = res.fold(
                        onSuccess = { SyncStatusUi.Success(it) },
                        onFailure = { SyncStatusUi.Error(it.message ?: "Sync failed.") }
                    )
                }
                SheetConnectionMode.GOOGLE_OAUTH -> {
                    startGoogleAuthAction(
                        PendingOAuthAction(
                            type = if (pullOnly) PendingOAuthActionType.PULL_ONLY else PendingOAuthActionType.SYNC_NOW
                        )
                    )
                }
            }
        }
    }

    private fun startGoogleAuthAction(action: PendingOAuthAction) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusUi.Syncing
            pendingOAuthAction = action
            when (val outcome = authHelper.requestSheetsAccess()) {
                is GoogleAuthOutcome.Authorized -> {
                    pendingOAuthAction = null
                    executeOAuthAction(action, outcome.accessToken)
                }
                is GoogleAuthOutcome.NeedsConsent -> {
                    _pendingConsentIntent.value = outcome.pendingIntent
                }
                is GoogleAuthOutcome.Failed -> {
                    pendingOAuthAction = null
                    _syncStatus.value = SyncStatusUi.Error(outcome.reason)
                }
            }
        }
    }

    fun onConsentIntentLaunched() {
        _pendingConsentIntent.value = null
    }

    fun handleGoogleAuthActivityResult(resultOk: Boolean, data: Intent?) {
        val action = pendingOAuthAction
        pendingOAuthAction = null
        if (!resultOk) {
            _syncStatus.value = SyncStatusUi.Error("Google authorization was cancelled.")
            return
        }
        val token = authHelper.extractTokenFromResultIntent(data)
        if (token.isNullOrBlank() || action == null) {
            _syncStatus.value = SyncStatusUi.Error("Could not obtain Google Sheets access token.")
            return
        }
        viewModelScope.launch {
            executeOAuthAction(action, token)
        }
    }

    private suspend fun executeOAuthAction(action: PendingOAuthAction, accessToken: String) {
        _syncStatus.value = SyncStatusUi.Syncing
        val result = when (action.type) {
            PendingOAuthActionType.SYNC_NOW -> repository.syncNow(accessToken, pullOnly = false)
            PendingOAuthActionType.PULL_ONLY -> repository.syncNow(accessToken, pullOnly = true)
            PendingOAuthActionType.CONNECT_EXISTING ->
                repository.connectExistingGoogleSheet(accessToken, action.param1, action.param2)
            PendingOAuthActionType.CREATE_NEW ->
                repository.createNewGoogleSheet(accessToken, action.param1, action.param2)
        }
        _syncStatus.value = result.fold(
            onSuccess = { SyncStatusUi.Success(it) },
            onFailure = { SyncStatusUi.Error(it.message ?: "Google Sheets request failed.") }
        )
    }

    private suspend fun maybeAutoSync() {
        val config = repository.sheetConfigFlow.first()
        if (!config.isConfigured || !config.autoSyncEnabled) return
        if (config.mode == SheetConnectionMode.APPS_SCRIPT) {
            _syncStatus.value = SyncStatusUi.Syncing
            val res = repository.syncNow(null, pullOnly = false)
            _syncStatus.value = res.fold(
                onSuccess = { SyncStatusUi.Success(it) },
                onFailure = { SyncStatusUi.Error(it.message ?: "Auto-sync failed.") }
            )
        } else if (config.mode == SheetConnectionMode.GOOGLE_OAUTH) {
            // Silently attempt if already consented without forcing a popup mid-edit
            val outcome = authHelper.requestSheetsAccess()
            if (outcome is GoogleAuthOutcome.Authorized) {
                _syncStatus.value = SyncStatusUi.Syncing
                val res = repository.syncNow(outcome.accessToken, pullOnly = false)
                _syncStatus.value = res.fold(
                    onSuccess = { SyncStatusUi.Success(it) },
                    onFailure = { SyncStatusUi.Error(it.message ?: "Auto-sync failed.") }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun durationSecondsFor(phase: PomodoroPhase, focusMinutes: Int): Int = when (phase) {
            PomodoroPhase.FOCUS -> focusMinutes * 60
            PomodoroPhase.SHORT_BREAK -> PomodoroPhase.SHORT_BREAK.defaultMinutes * 60
            PomodoroPhase.LONG_BREAK -> PomodoroPhase.LONG_BREAK.defaultMinutes * 60
        }

        fun onPhaseFinished(current: PomodoroUiState): PomodoroUiState {
            return if (current.phase == PomodoroPhase.FOCUS) {
                val nextSessions = current.completedSessions + 1
                val nextPhase = if (nextSessions % 4 == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
                val nextSeconds = durationSecondsFor(nextPhase, current.focusMinutes)
                current.copy(
                    phase = nextPhase,
                    totalSeconds = nextSeconds,
                    remainingSeconds = nextSeconds,
                    isRunning = false,
                    completedSessions = nextSessions,
                    completionMessage = "Focus session #$nextSessions complete! Ready for ${nextPhase.label.lowercase()}."
                )
            } else {
                val nextSeconds = durationSecondsFor(PomodoroPhase.FOCUS, current.focusMinutes)
                current.copy(
                    phase = PomodoroPhase.FOCUS,
                    totalSeconds = nextSeconds,
                    remainingSeconds = nextSeconds,
                    isRunning = false,
                    completionMessage = "Break finished! Ready for your next focus session."
                )
            }
        }

        fun applyFiltersAndSort(
            tasks: List<StudyTask>,
            filter: TaskFilterState
        ): List<StudyTask> {
            val query = filter.searchQuery.trim().lowercase()
            val filtered = tasks.filter { task ->
                val matchesQuery = query.isEmpty() ||
                    task.title.lowercase().contains(query) ||
                    task.subject.lowercase().contains(query) ||
                    task.notes.lowercase().contains(query) ||
                    task.id.lowercase().contains(query)
                val matchesSubject = filter.selectedSubject == null ||
                    task.subject.equals(filter.selectedSubject, ignoreCase = true)
                val matchesStatus = filter.selectedStatus == null || task.status == filter.selectedStatus
                val matchesPriority = filter.selectedPriority == null || task.priority == filter.selectedPriority
                val matchesType = filter.selectedType == null || task.taskType == filter.selectedType

                matchesQuery && matchesSubject && matchesStatus && matchesPriority && matchesType
            }

            return when (filter.sortOrder) {
                SortOrder.DEADLINE_SOONEST -> filtered.sortedWith(
                    compareBy<StudyTask> { it.isCompleted }
                        .thenBy { it.deadline }
                        .thenBy { it.priority.sortRank }
                )
                SortOrder.DEADLINE_LATEST -> filtered.sortedWith(
                    compareBy<StudyTask> { it.isCompleted }
                        .thenByDescending { it.deadline }
                        .thenBy { it.priority.sortRank }
                )
                SortOrder.PRIORITY_HIGHEST -> filtered.sortedWith(
                    compareBy<StudyTask> { it.isCompleted }
                        .thenBy { it.priority.sortRank }
                        .thenBy { it.deadline }
                )
            }
        }

        fun buildDashboardSummary(tasks: List<StudyTask>): DashboardSummary {
            val total = tasks.size
            val completed = tasks.count { it.status == TaskStatus.COMPLETED }
            val notStarted = tasks.count { it.status == TaskStatus.NOT_STARTED }
            val inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS }
            val pending = notStarted + inProgress
            val overdue = tasks.count { it.isOverdue }
            val fraction = if (total == 0) 0f else completed.toFloat() / total.toFloat()

            val urgent = tasks
                .filter { it.isUrgent }
                .sortedWith(
                    compareByDescending<StudyTask> { it.isOverdue }
                        .thenBy { it.deadline }
                        .thenBy { it.priority.sortRank }
                )

            val subjects = tasks
                .groupBy { it.subject.ifBlank { "General" } }
                .map { (subject, group) ->
                    SubjectProgress(
                        subject = subject,
                        total = group.size,
                        completed = group.count { it.isCompleted }
                    )
                }
                .sortedByDescending { it.total - it.completed }

            return DashboardSummary(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = pending,
                overdueTasks = overdue,
                notStartedTasks = notStarted,
                inProgressTasks = inProgress,
                completionFraction = fraction,
                urgentTasks = urgent,
                subjectBreakdown = subjects
            )
        }
    }

    class Factory(
        private val repository: StudyRepository,
        private val authHelper: GoogleAuthHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyPlannerViewModel(repository, authHelper) as T
        }
    }
}
