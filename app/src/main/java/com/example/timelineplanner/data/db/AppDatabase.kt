package com.example.timelineplanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, ChatMessageEntity::class, CourseEntity::class, PracticeSubjectEntity::class, PracticeRecordEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun courseDao(): CourseDao
    abstract fun practiceDao(): PracticeDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `practice_subjects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#4A90D9',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `practice_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correctQuestions` INTEGER NOT NULL,
                        `accuracy` REAL NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`subjectId`) REFERENCES `practice_subjects`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_practice_records_subjectId` ON `practice_records` (`subjectId`)")
            }
        }
    }
}
