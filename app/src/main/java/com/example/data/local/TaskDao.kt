package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM study_tasks WHERE syncState != 'PENDING_DELETE' ORDER BY deadline ASC, updatedAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM study_tasks")
    suspend fun getAllTasksIncludingPendingDeletes(): List<TaskEntity>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun hardDeleteById(id: String)

    @Query("DELETE FROM study_tasks")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceSyncedState(tasks: List<TaskEntity>) {
        clearAll()
        if (tasks.isNotEmpty()) {
            upsertTasks(tasks)
        }
    }
}
