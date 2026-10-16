package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

/**
 * Continuous Google Photos-style zoomable photo viewer:
 * - Stable pointerInput (keyed by uriKey)
 * - Continuous smooth pinch zoom (1.0x -> 5.0x) centered around the pinch centroid
 * - Combined pinch + pan while zoomed
 * - Double tap at scale > 1x: smooth animated reset to 1.0x (offset = 0)
 * - Double tap at scale == 1x: smooth animated zoom to 2.5x around tap position
 * - Bounded pan prevents dragging image completely off-screen
 * - Reports isZoomed state via onZoomStateChanged so container disables vertical media swipe when zoomed
 */
@Composable
fun ZoomablePhoto(
    bitmap: Bitmap,
    contentDescription: String?,
    uriKey: String,
    onZoomStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim = remember(uriKey) { Animatable(1f) }
    val offsetAnim = remember(uriKey) { Animatable(Offset.Zero, Offset.VectorConverter) }

    var lastTapTime by remember(uriKey) { androidx.compose.runtime.mutableLongStateOf(0L) }
    var lastTapPosition by remember(uriKey) { androidx.compose.runtime.mutableStateOf(Offset.Zero) }

    LaunchedEffect(scaleAnim.value) {
        onZoomStateChanged(scaleAnim.value > 1.05f)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        fun clampOffset(offset: Offset, currentScale: Float): Offset {
            if (currentScale <= 1f) return Offset.Zero
            val maxOffsetX = (containerWidth * (currentScale - 1f)) / 2f
            val maxOffsetY = (containerHeight * (currentScale - 1f)) / 2f
            return Offset(
                x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
                y = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uriKey) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position

                        var isTransforming = false

                        val isDoubleTapCandidate = (downTime - lastTapTime < 300L) &&
                                ((downPos - lastTapPosition).getDistance() < 100f)

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val pointerCount = event.changes.count { it.pressed }

                            if (pointerCount == 0) {
                                if (!isTransforming) {
                                    if (isDoubleTapCandidate) {
                                        val currentScale = scaleAnim.value
                                        coroutineScope.launch {
                                            if (currentScale > 1.05f) {
                                                launch { scaleAnim.animateTo(1f, animationSpec = tween(250)) }
                                                launch { offsetAnim.animateTo(Offset.Zero, animationSpec = tween(250)) }
                                            } else {
                                                val targetScale = 2.5f
                                                val targetOffset = clampOffset(
                                                    (Offset(containerWidth / 2f, containerHeight / 2f) - downPos) * (targetScale - 1f),
                                                    targetScale
                                                )
                                                launch { scaleAnim.animateTo(targetScale, animationSpec = tween(250)) }
                                                launch { offsetAnim.animateTo(targetOffset, animationSpec = tween(250)) }
                                            }
                                        }
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = downTime
                                        lastTapPosition = downPos
                                    }
                                }
                                break
                            }

                            if (pointerCount >= 2) {
                                isTransforming = true
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                val oldScale = scaleAnim.value
                                val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)

                                val centroidOffset = centroid - Offset(containerWidth / 2f, containerHeight / 2f)
                                val currentOffset = offsetAnim.value
                                val updatedOffset = (currentOffset + panChange - centroidOffset) * (newScale / oldScale) + centroidOffset

                                coroutineScope.launch {
                                    scaleAnim.snapTo(newScale)
                                    offsetAnim.snapTo(clampOffset(updatedOffset, newScale))
                                }

                                event.changes.forEach { it.consume() }
                            } else if (pointerCount == 1) {
                                val change = event.changes.first { it.pressed }
                                val currentScale = scaleAnim.value

                                if (currentScale > 1.05f) {
                                    // In zoomed state: consume and pan image
                                    isTransforming = true
                                    val pan = change.position - change.previousPosition
                                    val newOffset = clampOffset(offsetAnim.value + pan, currentScale)
                                    coroutineScope.launch {
                                        offsetAnim.snapTo(newOffset)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        translationX = offsetAnim.value.x
                        translationY = offsetAnim.value.y
                    }
            )
        }
    }
}
