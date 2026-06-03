package com.example.timelineplanner.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timelineplanner.model.Task
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BoxScope.TaskBar(
    task: Task,
    columnCount: Int,
    columnIndex: Int,
    hourHeightDp: Float,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val pixelsPerMinute = hourHeightDp / 60f
    val topDp = ((task.startMinute - TIMELINE_START_HOUR * 60) * pixelsPerMinute).dp
    val actualHeightDp = ((task.endMinute - task.startMinute) * pixelsPerMinute).dp
    val isCompact = actualHeightDp < 28.dp
    val startTime = minuteToTimeString(task.startMinute)
    val endTime = minuteToTimeString(task.endMinute)
    val barColor = parseColor(task.color)
    val cornerRadius = if (isCompact) 4.dp else 8.dp
    val textFontSize = (actualHeightDp.value * 0.38f).coerceIn(5f, 11f).sp

    val fraction = 1f / columnCount

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val targetWidth = (constraints.maxWidth * fraction).toInt().coerceAtLeast(0)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = targetWidth, maxWidth = targetWidth)
                )
                val x = (constraints.maxWidth * fraction * columnIndex).roundToInt()
                val y = topDp.roundToPx()
                layout(placeable.width, placeable.height) {
                    placeable.place(x, y)
                }
            }
            .height(actualHeightDp)
            .padding(horizontal = 2.dp)
            .shadow(2.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val startX = down.position.x
                    val startY = down.position.y
                    var moved = false
                    val completed = withTimeoutOrNull(500L) {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (abs(pressed[0].position.x - startX) > 10f ||
                                abs(pressed[0].position.y - startY) > 10f
                            ) {
                                moved = true
                                break
                            }
                        }
                    }
                    if (completed == null) {
                        // 超时 = 长按
                        onLongClick()
                        // 等松手
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                    } else if (!moved) {
                        // 没超时、没移动 = 点击
                        onClick()
                    }
                    // moved = 滚动，不处理
                }
            }
            .background(barColor)
            .then(
                if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(cornerRadius))
                else Modifier
            )
            .padding(horizontal = 4.dp, vertical = if (isCompact) 1.dp else 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isCompact) {
            Text(
                text = "${task.title} $startTime-$endTime",
                fontSize = textFontSize,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = textFontSize
            )
        } else {
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$startTime-$endTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }

        // 选中勾选标记
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = barColor
                )
            }
        }
    }
}
