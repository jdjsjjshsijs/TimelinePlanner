package com.example.timelineplanner.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

@Singleton
class ThemePreferences @Inject constructor(
    @Named("theme_prefs") private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        return ThemeMode.fromValue(prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.value))
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.value).apply()
        _themeMode.value = mode
        applyToAppCompat(mode)
    }

    /** 在 Activity.onCreate 之前调用，确保主题在 setContentView 之前生效 */
    fun applySavedTheme() {
        applyToAppCompat(loadThemeMode())
    }

    private fun applyToAppCompat(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}
