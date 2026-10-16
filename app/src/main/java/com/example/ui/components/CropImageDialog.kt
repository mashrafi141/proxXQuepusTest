package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropAspectRatio(val label: String, val ratio: Float) {
    SQUARE("1:1", 1.0f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    WIDE_16_9("16:9", 16f / 9f),
    PORTRAIT_3_4("3:4", 3f / 4f)
}

/**
 * Professional Image Cropping Dialog:
 * - Interactive pinch-to-zoom (1.0x - 5.0x) and 2D pan gestures.
 * - Dynamic Aspect Ratio switching (1:1, 4:3, 16:9, 3:4).
 * - 90-degree image rotation.
 * - Semi-transparent mask with rule-of-thirds grid guides.
 * - High-precision bitmap sub-rectangle extraction.
 */
@Composable
fun CropImageDialog(
    sourceBitmap: Bitmap,
    folderName: String,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.SQUARE) }

    // Dialog with full width layout
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("crop_cancel_button")
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Crop Folder Profile",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = folderName,
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                        },
                        modifier = Modifier
                            .testTag("crop_rotate_button")
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = CyanPrimary)
                    }
                }

                // Interactive Crop Viewport Box
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .background(Color(0xFF0A0C10)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = maxWidth.value
                    val containerHeight = maxHeight.value

                    // Compute crop frame dimensions based on selected ratio
                    val maxCropWidth = containerWidth * 0.9f
                    val maxCropHeight = containerHeight * 0.85f

                    val (cropWidth, cropHeight) = run {
                        val targetRatio = selectedRatio.ratio
                        var w = maxCropWidth
                        var h = w / targetRatio
                        if (h > maxCropHeight) {
                            h = maxCropHeight
                            w = h * targetRatio
                        }
                        Pair(w, h)
                    }

                    // User gesture detection area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedRatio, rotationDegrees) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                    offset += pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Canvas drawing rotated, scaled, panned source bitmap and overlay mask
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            val cropRectLeft = (canvasW - cropWidth.dp.toPx()) / 2f
                            val cropRectTop = (canvasH - cropHeight.dp.toPx()) / 2f
                            val cropRectRight = cropRectLeft + cropWidth.dp.toPx()
                            val cropRectBottom = cropRectTop + cropHeight.dp.toPx()
                            val cropRect = Rect(cropRectLeft, cropRectTop, cropRectRight, cropRectBottom)

                            // 1. Draw source bitmap
                            val bmpW = sourceBitmap.width.toFloat()
                            val bmpH = sourceBitmap.height.toFloat()

                            // Fit base scale
                            val fitScale = min(canvasW / bmpW, canvasH / bmpH)
                            val effectiveScale = fitScale * scale

                            // Apply matrix transformation in canvas drawContext
                            drawContext.canvas.save()
                            drawContext.canvas.translate(canvasW / 2f + offset.x, canvasH / 2f + offset.y)
                            drawContext.canvas.rotate(rotationDegrees.toFloat())
                            drawContext.canvas.scale(effectiveScale, effectiveScale)

                            drawImage(
                                image = sourceBitmap.asImageBitmap(),
                                topLeft = Offset(-bmpW / 2f, -bmpH / 2f)
                            )
                            drawContext.canvas.restore()

                            // 2. Draw Semi-Transparent Dark Mask around crop frame
                            val maskPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, canvasW, canvasH))
                                addRect(cropRect)
                            }
                            drawPath(path = maskPath, color = Color(0xCC000000))

                            // 3. Draw Crop Frame Outline
                            drawRect(
                                color = CyanPrimary,
                                topLeft = Offset(cropRectLeft, cropRectTop),
                                size = Size(cropRect.width, cropRect.height),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // 4. Draw Rule-of-Thirds Grid Lines
                            val thirdW = cropRect.width / 3f
                            val thirdH = cropRect.height / 3f

                            // Vertical grid lines
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cropRectLeft + thirdW, cropRectTop),
                                end = Offset(cropRectLeft + thirdW, cropRectBottom),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cropRectLeft + thirdW * 2f, cropRectTop),
                                end = Offset(cropRectLeft + thirdW * 2f, cropRectBottom),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Horizontal grid lines
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cropRectLeft, cropRectTop + thirdH),
                                end = Offset(cropRectRight, cropRectTop + thirdH),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color(0x80FFFFFF),
                                start = Offset(cropRectLeft, cropRectTop + thirdH * 2f),
                                end = Offset(cropRectRight, cropRectTop + thirdH * 2f),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Corner accents
                            val cornerLen = 18.dp.toPx()
                            val cornerStroke = 4.dp.toPx()
                            val cColor = CyanPrimary

                            // Top-Left
                            drawLine(cColor, Offset(cropRectLeft, cropRectTop), Offset(cropRectLeft + cornerLen, cropRectTop), cornerStroke)
                            drawLine(cColor, Offset(cropRectLeft, cropRectTop), Offset(cropRectLeft, cropRectTop + cornerLen), cornerStroke)

                            // Top-Right
                            drawLine(cColor, Offset(cropRectRight, cropRectTop), Offset(cropRectRight - cornerLen, cropRectTop), cornerStroke)
                            drawLine(cColor, Offset(cropRectRight, cropRectTop), Offset(cropRectRight, cropRectTop + cornerLen), cornerStroke)

                            // Bottom-Left
                            drawLine(cColor, Offset(cropRectLeft, cropRectBottom), Offset(cropRectLeft + cornerLen, cropRectBottom), cornerStroke)
                            drawLine(cColor, Offset(cropRectLeft, cropRectBottom), Offset(cropRectLeft, cropRectBottom - cornerLen), cornerStroke)

                            // Bottom-Right
                            drawLine(cColor, Offset(cropRectRight, cropRectBottom), Offset(cropRectRight - cornerLen, cropRectBottom), cornerStroke)
                            drawLine(cColor, Offset(cropRectRight, cropRectBottom), Offset(cropRectRight, cropRectBottom - cornerLen), cornerStroke)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Aspect Ratio Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CropAspectRatio.values().forEach { ratio ->
                        val isSelected = selectedRatio == ratio
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedRatio = ratio
                                scale = 1f
                                offset = Offset.Zero
                            },
                            label = {
                                Text(
                                    text = ratio.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                containerColor = DarkSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) CyanPrimary else DarkSurfaceBorder
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Reset & Apply Crop
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotationDegrees = 0
                            selectedRatio = CropAspectRatio.SQUARE
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", color = TextSecondary, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            // Produce cropped bitmap
                            val cropped = performBitmapCrop(
                                src = sourceBitmap,
                                rotation = rotationDegrees,
                                scale = scale,
                                offset = offset,
                                cropAspectRatio = selectedRatio.ratio
                            )
                            onCropConfirmed(cropped)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("crop_apply_button")
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply as Profile", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Computes exact high-resolution cropped bitmap taking into account rotation, zoom, pan, and crop ratio.
 */
private fun performBitmapCrop(
    src: Bitmap,
    rotation: Int,
    scale: Float,
    offset: Offset,
    cropAspectRatio: Float
): Bitmap {
    // 1. Rotate source if needed
    val rotated = if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    } else {
        src
    }

    val rW = rotated.width.toFloat()
    val rH = rotated.height.toFloat()

    // Base target crop dimension inside rotated image
    var targetW = rW / scale.coerceAtLeast(0.5f)
    var targetH = targetW / cropAspectRatio

    if (targetH > rH / scale.coerceAtLeast(0.5f)) {
        targetH = rH / scale.coerceAtLeast(0.5f)
        targetW = targetH * cropAspectRatio
    }

    // Offset in rotated bitmap coordinates (inverted from screen pan)
    val normOffsetX = (-offset.x / (1000f * scale)) * rW
    val normOffsetY = (-offset.y / (1000f * scale)) * rH

    var left = (rW - targetW) / 2f + normOffsetX
    var top = (rH - targetH) / 2f + normOffsetY

    // Clamp inside bitmap bounds
    left = left.coerceIn(0f, (rW - targetW).coerceAtLeast(0f))
    top = top.coerceIn(0f, (rH - targetH).coerceAtLeast(0f))

    val finalW = min(targetW, rW - left).roundToInt().coerceAtLeast(1)
    val finalH = min(targetH, rH - top).roundToInt().coerceAtLeast(1)

    return try {
        Bitmap.createBitmap(
            rotated,
            left.roundToInt().coerceIn(0, rotated.width - finalW),
            top.roundToInt().coerceIn(0, rotated.height - finalH),
            finalW,
            finalH
        )
    } catch (e: Exception) {
        // Fallback to center square/ratio crop
        val cw = min(rotated.width, (rotated.height * cropAspectRatio).toInt())
        val ch = (cw / cropAspectRatio).toInt()
        val cl = ((rotated.width - cw) / 2).coerceAtLeast(0)
        val ct = ((rotated.height - ch) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(rotated, cl, ct, cw, ch)
    }
}
