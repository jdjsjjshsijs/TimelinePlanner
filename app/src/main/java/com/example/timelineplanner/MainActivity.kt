package com.example.timelineplanner

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.timelineplanner.ui.navigation.AppNavigation
import com.example.timelineplanner.ui.theme.TimelinePlannerTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    var isKioskMode = false
        private set

    var isKioskSuspended = false
        private set

    @Inject @Named("kiosk_prefs") lateinit var kioskPrefs: SharedPreferences

    fun getWhitelistedPackages(): Set<String> {
        return kioskPrefs.getStringSet("kiosk_whitelist", emptySet()) ?: emptySet()
    }

    fun setWhitelistedPackages(packages: Set<String>) {
        kioskPrefs.edit().putStringSet("kiosk_whitelist", packages).apply()
    }

    fun launchWhitelistedApp(packageName: String) {
        if (!isKioskMode) return
        isKioskSuspended = true
        try { stopLockTask() } catch (_: Exception) {}
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimelinePlannerTheme {
                AppNavigation()
            }
        }
    }

    // 锁屏解锁后重新进入 kiosk 模式
    override fun onResume() {
        super.onResume()
        if (isKioskMode || isKioskSuspended) {
            isKioskSuspended = false
            applyImmersiveMode()
            try { startLockTask() } catch (_: Exception) {}
        }
    }

    // 窗口获得焦点时重新进入 kiosk 模式
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isKioskMode) {
            applyImmersiveMode()
        }
    }

    // 拦截返回键
    override fun onBackPressed() {
        if (isKioskMode) return
        super.onBackPressed()
    }

    // 拦截所有按键（Home、Recent 等）
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isKioskMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_MENU -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun enterKioskMode() {
        isKioskMode = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()
        try { startLockTask() } catch (_: Exception) {}
    }

    fun exitKioskMode() {
        isKioskMode = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        try { stopLockTask() } catch (_: Exception) {}
    }

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
}
