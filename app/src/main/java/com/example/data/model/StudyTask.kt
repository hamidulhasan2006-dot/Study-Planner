package com.example.data.model

import com.example.util.DateHelper
import java.util.UUID

enum class TaskType(val label: String) {
    ASSIGNMENT("Assignment"),
    PRESENTATION("Presentation"),
    EXAM("Exam"),
    STUDY("Study"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): TaskType {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull {
                it.label.lowercase() == normalized || it.name.lowercase() == normalized
            } ?: OTHER
        }
    }
}

enum class TaskPriority(val label: String, val sortRank: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2);

    companion object {
        fun fromRaw(raw: String?): TaskPriority {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull {
                it.label.lowercase() == normalized || it.name.lowercase() == normalized
            } ?: MEDIUM
        }
    }
}

enum class TaskStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): TaskStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull {
                it.label.lowercase() == normalized ||
                    it.name.lowercase() == normalized ||
                    (normalized == "done" && it == COMPLETED) ||
                    (normalized == "todo" && it == NOT_STARTED)
            } ?: NOT_STARTED
        }
    }
}

enum class SyncState {
    SYNCED,
    PENDING_UPSERT,
    PENDING_DELETE
}

enum class SortOrder(val label: String) {
    DEADLINE_SOONEST("Soonest Deadline"),
    DEADLINE_LATEST("Latest Deadline"),
    PRIORITY_HIGHEST("Highest Priority")
}

enum class SheetConnectionMode(val label: String) {
    NONE("Local Only"),
    GOOGLE_OAUTH("Google Account (Sheets API)"),
    APPS_SCRIPT("Apps Script Web App")
}

data class StudyTask(
    val id: String = generateTaskId(),
    val title: String,
    val subject: String,
    val taskType: TaskType = TaskType.ASSIGNMENT,
    val deadline: String = DateHelper.todayIso(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val notes: String = "",
    val syncState: SyncState = SyncState.PENDING_UPSERT,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val isOverdue: Boolean
        get() = DateHelper.isOverdue(deadline, status)

    val isUrgent: Boolean
        get() = !isCompleted && (isOverdue || DateHelper.isDueSoon(deadline) || priority == TaskPriority.HIGH)

    fun toSheetRow(): List<String> = listOf(
        id,
        title,
        subject,
        taskType.label,
        deadline,
        priority.label,
        status.label,
        notes
    )

    companion object {
        val SHEET_HEADERS = listOf(
            "Task ID",
            "Task Title",
            "Subject",
            "Task Type",
            "Deadline",
            "Priority",
            "Status",
            "Notes"
        )

        fun generateTaskId(): String {
            val short = UUID.randomUUID().toString().substring(0, 6).uppercase()
            return "ST-$short"
        }
    }
}

data class SubjectProgress(
    val subject: String,
    val total: Int,
    val completed: Int
) {
    val progress: Float
        get() = if (total == 0) 0f else completed.toFloat() / total.toFloat()
}

data class DashboardSummary(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val notStartedTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val completionFraction: Float = 0f,
    val urgentTasks: List<StudyTask> = emptyList(),
    val subjectBreakdown: List<SubjectProgress> = emptyList()
)

data class SheetConfig(
    val mode: SheetConnectionMode = SheetConnectionMode.NONE,
    val spreadsheetId: String = "",
    val spreadsheetTitle: String = "",
    val sheetTabName: String = "Tasks",
    val appsScriptUrl: String = "",
    val autoSyncEnabled: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val connectedAccountEmail: String = ""
) {
    val isConfigured: Boolean
        get() = when (mode) {
            SheetConnectionMode.NONE -> false
            SheetConnectionMode.GOOGLE_OAUTH -> spreadsheetId.isNotBlank()
            SheetConnectionMode.APPS_SCRIPT -> appsScriptUrl.isNotBlank()
        }
}

sealed interface SyncStatusUi {
    data object Idle : SyncStatusUi
    data object Syncing : SyncStatusUi
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatusUi
    data class Error(val message: String) : SyncStatusUi
}
