package com.example.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.util.MediaDetector
import kotlin.math.abs

/**
 * Background video gesture layer:
 * - Single tap: invokes onToggleControls() reliably.
 * - Double tap: seeks forward or backward by 10s based on left/right half. Single tap is not fired on double tap.
 * - Horizontal drag: seeks proportionally with visual HUD.
 * Vertical drag is left unconsumed so the parent Reel/Pager vertical container handles smooth continuous media navigation!
 * Uses rememberUpdatedState so pointerInput is keyed to Unit and NEVER interrupted during touch gestures.
 */
@Composable
fun PlayerGestureDetector(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onToggleControls: () -> Unit,
    onDoubleTapSeek: (Boolean) -> Unit, // true = right (+10s), false = left (-10s)
    onFeedbackUpdate: (PlayerGestureFeedback) -> Unit,
    onFeedbackDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentPositionState = rememberUpdatedState(currentPositionMs)
    val durationState = rememberUpdatedState(durationMs)
    val onSeekState = rememberUpdatedState(onSeek)
    val onToggleControlsState = rememberUpdatedState(onToggleControls)
    val onDoubleTapSeekState = rememberUpdatedState(onDoubleTapSeek)
    val onFeedbackUpdateState = rememberUpdatedState(onFeedbackUpdate)
    val onFeedbackDismissState = rememberUpdatedState(onFeedbackDismiss)

    var gestureMode by remember { mutableStateOf(GestureMode.NONE) }
    var touchStartX by remember { mutableFloatStateOf(0f) }
    var touchStartY by remember { mutableFloatStateOf(0f) }

    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var currentSeekOffsetMs by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Tap & Double tap gestures: detectTapGestures cleanly separates single tap from double tap
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onToggleControlsState.value()
                    },
                    onDoubleTap = { offset ->
                        val isRightHalf = offset.x > size.width / 2
                        onDoubleTapSeekState.value(isRightHalf)
                    }
                )
            }
            // Horizontal drag for seeking with stable Unit key: avoids cancelling during 200ms playback ticks!
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (changes.isEmpty()) continue
                        val firstChange = changes[0]

                        // Touch down
                        if (firstChange.pressed && gestureMode == GestureMode.NONE && changes.size == 1) {
                            touchStartX = firstChange.position.x
                            touchStartY = firstChange.position.y
                            gestureMode = GestureMode.NONE
                            initialSeekPosition = currentPositionState.value
                            currentSeekOffsetMs = 0L
                        }

                        // Drag in progress
                        if (firstChange.pressed && changes.size == 1) {
                            val dx = firstChange.position.x - touchStartX
                            val dy = firstChange.position.y - touchStartY

                            val threshold = 30f

                            // Lock strictly to HORIZONTAL seek if dx > dy
                            if (gestureMode == GestureMode.NONE) {
                                if (abs(dx) > threshold && abs(dx) > abs(dy) * 1.5f) {
                                    gestureMode = GestureMode.HORIZONTAL_SEEK
                                }
                            }

                            if (gestureMode == GestureMode.HORIZONTAL_SEEK) {
                                firstChange.consume()
                                val dur = durationState.value
                                val factor = (dx / size.width).coerceIn(-1f, 1f)
                                val maxSeekSpan = (dur.takeIf { it > 0 }?.coerceAtMost(300000L) ?: 60000L)
                                val seekDelta = (factor * maxSeekSpan).toLong()
                                currentSeekOffsetMs = seekDelta

                                val targetPos = (initialSeekPosition + seekDelta).coerceIn(0L, dur.coerceAtLeast(0L))
                                val icon = if (seekDelta >= 0) FeedbackIcon.SEEK_FORWARD else FeedbackIcon.SEEK_BACKWARD
                                val sign = if (seekDelta >= 0) "+" else ""
                                val deltaSec = seekDelta / 1000

                                onFeedbackUpdateState.value(
                                    PlayerGestureFeedback(
                                        iconType = icon,
                                        text = "$sign${deltaSec}s (${MediaDetector.formatDuration(targetPos)})",
                                        percentage = if (dur > 0) targetPos.toFloat() / dur.toFloat() else 0f,
                                        isVisible = true
                                    )
                                )
                            }
                        }

                        // Touch up
                        if (!firstChange.pressed) {
                            if (gestureMode == GestureMode.HORIZONTAL_SEEK) {
                                val dur = durationState.value
                                val finalTarget = (initialSeekPosition + currentSeekOffsetMs).coerceIn(0L, dur.coerceAtLeast(0L))
                                onSeekState.value(finalTarget)
                                onFeedbackDismissState.value()
                            }
                            gestureMode = GestureMode.NONE
                        }
                    }
                }
            }
    ) {
        content()
    }
}
