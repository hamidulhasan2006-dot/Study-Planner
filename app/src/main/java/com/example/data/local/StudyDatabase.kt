package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "student_study_planner.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
