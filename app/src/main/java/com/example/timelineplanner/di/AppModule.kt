package com.example.timelineplanner.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.timelineplanner.data.db.AppDatabase
import com.example.timelineplanner.data.db.ChatMessageDao
import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.repository.AiTaskRepository
import com.example.timelineplanner.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        ).build()
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
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }

    @Provides
    @Singleton
    fun provideAiSettingsPrefs(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "ai_settings_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Migrate from old plain SharedPreferences if needed
        val oldPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        if (oldPrefs.contains("api_key") && !encryptedPrefs.contains("api_key")) {
            encryptedPrefs.edit().apply {
                oldPrefs.all.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                    }
                }
                apply()
            }
            oldPrefs.edit().clear().apply()
        }

        return encryptedPrefs
    }

    @Provides
    @Singleton
    fun provideAiTaskRepository(
        prefs: SharedPreferences,
        taskRepository: TaskRepository
    ): AiTaskRepository {
        return AiTaskRepository(prefs, taskRepository)
    }
}
