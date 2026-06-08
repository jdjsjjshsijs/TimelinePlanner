package com.example.timelineplanner.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.timelineplanner.data.db.AppDatabase
import com.example.timelineplanner.data.db.ChatMessageDao
import com.example.timelineplanner.data.db.CourseDao
import com.example.timelineplanner.data.db.GoalDao
import com.example.timelineplanner.data.db.PracticeDao
import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.remote.SyncApi
import com.example.timelineplanner.data.remote.SyncClient
import com.example.timelineplanner.data.repository.AiTaskRepository
import com.example.timelineplanner.data.repository.CourseRepository
import com.example.timelineplanner.data.repository.GoalRepository
import com.example.timelineplanner.data.repository.PracticeRepository
import com.example.timelineplanner.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "timeline_planner.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideCourseDao(database: AppDatabase): CourseDao {
        return database.courseDao()
    }

    @Provides
    @Singleton
    fun providePracticeDao(database: AppDatabase): PracticeDao {
        return database.practiceDao()
    }

    @Provides
    @Singleton
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideCourseRepository(courseDao: CourseDao, syncApi: SyncApi): CourseRepository {
        val repo = CourseRepository(courseDao)
        repo.setSyncApi(syncApi)
        return repo
    }

    @Provides
    @Singleton
    fun providePracticeRepository(practiceDao: PracticeDao): PracticeRepository {
        return PracticeRepository(practiceDao)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(goalDao: GoalDao): GoalRepository {
        return GoalRepository(goalDao)
    }

    @Provides
    @Singleton
    fun provideAiSettingsPrefs(@ApplicationContext context: Context): SharedPreferences {
        // Use plain SharedPreferences so settings survive uninstall/reinstall
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

        // One-time migration from old EncryptedSharedPreferences if needed
        if (!prefs.contains("migrated_from_encrypted")) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val enc = EncryptedSharedPreferences.create(
                    context,
                    "ai_settings_encrypted",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                if (enc.contains("api_key")) {
                    prefs.edit().apply {
                        enc.all.forEach { (key, value) ->
                            when (value) {
                                is String -> putString(key, value)
                                is Boolean -> putBoolean(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is Float -> putFloat(key, value)
                            }
                        }
                        putBoolean("migrated_from_encrypted", true)
                        apply()
                    }
                } else {
                    prefs.edit().putBoolean("migrated_from_encrypted", true).apply()
                }
            } catch (_: Exception) {
                prefs.edit().putBoolean("migrated_from_encrypted", true).apply()
            }
        }

        return prefs
    }

    @Provides
    @Singleton
    @Named("semester_prefs")
    fun provideSemesterPrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("semester_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideSyncApi(aiSettingsPrefs: SharedPreferences): SyncApi {
        return SyncClient.createApi(aiSettingsPrefs)
    }

    @Provides
    @Singleton
    fun provideAiTaskRepository(
        prefs: SharedPreferences,
        taskRepository: TaskRepository
    ): AiTaskRepository {
        return AiTaskRepository(prefs, taskRepository)
    }

    @Provides
    @Singleton
    @Named("kiosk_prefs")
    fun provideKioskPrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named("theme_prefs")
    fun provideThemePrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    }
}
