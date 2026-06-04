package com.example.timelineplanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, ChatMessageEntity::class, CourseEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun courseDao(): CourseDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `courses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `location` TEXT NOT NULL DEFAULT '',
                        `teacher` TEXT NOT NULL DEFAULT '',
                        `daysOfWeek` TEXT NOT NULL,
                        `startMinute` INTEGER NOT NULL,
                        `endMinute` INTEGER NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#4A90D9',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
