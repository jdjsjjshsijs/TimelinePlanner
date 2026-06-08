package com.example.timelineplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 预设任务颜色调色板（柔和色）
val TaskColors = listOf(
    "#4A90D9", "#E74C3C", "#2ECC71", "#F39C12",
    "#9B59B6", "#1ABC9C", "#E67E22", "#3498DB",
    "#E91E63", "#00BCD4", "#FF5722", "#607D8B",
    "#8BC34A", "#FF9800", "#795548", "#009688"
)

// Light theme
val md_theme_light_primary = Color(0xFF38608F)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = Color(0xFF535F70)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD7E3F7)
val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)
val md_theme_light_background = Color(0xFFF8F9FF)
val md_theme_light_onBackground = Color(0xFF1A1C20)
val md_theme_light_surface = Color(0xFFF8F9FF)
val md_theme_light_onSurface = Color(0xFF1A1C20)
val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
val md_theme_light_error = Color(0xFFBA1A1A)

// Dark theme
val md_theme_dark_primary = Color(0xFF9ECAFF)
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF1E4875)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
val md_theme_dark_secondary = Color(0xFFBBC7DB)
val md_theme_dark_onSecondary = Color(0xFF253140)
val md_theme_dark_secondaryContainer = Color(0xFF3B4858)
val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F7)
val md_theme_dark_background = Color(0xFF1A1C20)
val md_theme_dark_onBackground = Color(0xFFE3E2E6)
val md_theme_dark_surface = Color(0xFF1A1C20)
val md_theme_dark_onSurface = Color(0xFFE3E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF43474E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC3C7CF)
val md_theme_dark_error = Color(0xFFFFB4AB)

// 时间轴特定颜色（跟随主题）
val TimelineGridLineLight = Color(0xFFE0E0E0)
val TimelineGridLineDark = Color(0xFF3A3A3A)
val TimelineHourLineLight = Color(0xFFBDBDBD)
val TimelineHourLineDark = Color(0xFF555555)
val TimelineHourTextLight = Color(0xFF757575)
val TimelineHourTextDark = Color(0xFF9E9E9E)
val TimelineCurrentTimeLine = Color(0xFFE53935)

// Task bar 文字颜色（task bar 背景是用户选的颜色，文字始终白色即可）
val TaskBarTextColor = Color.White

// 同步成功颜色
val SyncSuccessLight = Color(0xFF2E7D32)
val SyncSuccessDark = Color(0xFF81C784)
val SyncSuccessBgLight = Color(0xFFE8F5E9)
val SyncSuccessBgDark = Color(0xFF1B3A1E)

// 练习准确率颜色（深色模式下用更亮的色调保证可见性）
val AccuracyHighLight = Color(0xFF2ECC71)
val AccuracyHighDark = Color(0xFF66BB6A)
val AccuracyMidLight = Color(0xFFF39C12)
val AccuracyMidDark = Color(0xFFFFB74D)
val AccuracyLowLight = Color(0xFFE74C3C)
val AccuracyLowDark = Color(0xFFEF5350)

// 练习图表中的白色点和淡色背景
val ChartDotColor = Color.White
val ChartDotBgLight = Color.White.copy(alpha = 0.3f)
val ChartDotBgDark = Color(0xFFE3E2E6).copy(alpha = 0.3f)

// 提供 Composable 辅助函数，根据当前深色模式返回正确的颜色
@Composable
fun timelineGridLineColor(): Color = if (isSystemInDarkTheme()) TimelineGridLineDark else TimelineGridLineLight

@Composable
fun timelineHourLineColor(): Color = if (isSystemInDarkTheme()) TimelineHourLineDark else TimelineHourLineLight

@Composable
fun timelineHourTextColor(): Color = if (isSystemInDarkTheme()) TimelineHourTextDark else TimelineHourTextLight

@Composable
fun syncSuccessColor(): Color = if (isSystemInDarkTheme()) SyncSuccessDark else SyncSuccessLight

@Composable
fun syncSuccessBgColor(): Color = if (isSystemInDarkTheme()) SyncSuccessBgDark else SyncSuccessBgLight

@Composable
fun accuracyHighColor(): Color = if (isSystemInDarkTheme()) AccuracyHighDark else AccuracyHighLight

@Composable
fun accuracyMidColor(): Color = if (isSystemInDarkTheme()) AccuracyMidDark else AccuracyMidLight

@Composable
fun accuracyLowColor(): Color = if (isSystemInDarkTheme()) AccuracyLowDark else AccuracyLowLight

@Composable
fun chartDotBgColor(): Color = if (isSystemInDarkTheme()) ChartDotBgDark else ChartDotBgLight
