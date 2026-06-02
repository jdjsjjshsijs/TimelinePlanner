package com.example.timelineplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.timelineplanner.ui.navigation.AppNavigation
import com.example.timelineplanner.ui.theme.TimelinePlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimelinePlannerTheme {
                AppNavigation()
            }
        }
    }
}
