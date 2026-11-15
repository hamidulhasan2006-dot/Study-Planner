package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.StudyTask
import com.example.data.model.SyncState
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskType

@Entity(tableName = "study_tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val taskType: String,
    val deadline: String,
    val priority: String,
    val status: String,
    val notes: String,
    val syncState: String,
    val updatedAt: Long
) {
    fun toDomain(): StudyTask = StudyTask(
        id = id,
        title = title,
        subject = subject,
        taskType = TaskType.fromRaw(taskType),
        deadline = deadline,
        priority = TaskPriority.fromRaw(priority),
        status = TaskStatus.fromRaw(status),
        notes = notes,
        syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.PENDING_UPSERT),
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(task: StudyTask): TaskEntity = TaskEntity(
            id = task.id,
            title = task.title,
            subject = task.subject,
            taskType = task.taskType.name,
            deadline = task.deadline,
            priority = task.priority.name,
            status = task.status.name,
            notes = task.notes,
            syncState = task.syncState.name,
            updatedAt = task.updatedAt
        )
    }
}
