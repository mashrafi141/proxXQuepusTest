package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(imageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val loaded = BitmapFactory.decodeStream(stream, null, opts)
                    sourceBitmap = loaded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF00A0C10))
        ) {
            if (isLoading || sourceBitmap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading Image...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                val bmp = sourceBitmap!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }

                        Text(
                            text = "Crop Folder Profile",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                val cropped = cropBitmap(
                                    source = bmp,
                                    scale = scale,
                                    offsetX = offset.x,
                                    offsetY = offset.y,
                                    rotation = rotationDegrees,
                                    cropBoxSizePx = 600
                                )
                                onCropConfirmed(cropped)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Crop Viewport with rule-of-thirds grid
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val cropBoxSizeDp = min(maxWidth.value, maxHeight.value).dp * 0.85f

                        Box(
                            modifier = Modifier
                                .size(cropBoxSizeDp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(2.dp, CyanPrimary, RoundedCornerShape(16.dp))
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                        offset = Offset(
                                            x = offset.x + pan.x,
                                            y = offset.y + pan.y
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // The Image being transformed
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Crop Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                        rotationZ = rotationDegrees
                                    }
                            )

                            // Grid Lines Overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val gridColor = Color(0x5500E5FF)
                                // 1/3 and 2/3 vertical
                                drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1f)
                                drawLine(gridColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1f)
                                // 1/3 and 2/3 horizontal
                                drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1f)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Zoom slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Zoom", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.8f..4f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = DarkSurfaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action Controls: Rotate and Reset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate 90°", color = TextPrimary, fontSize = 13.sp)
                        }

                        TextButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                                rotationDegrees = 0f
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Perform high-precision software crop from transformed coordinates
 */
private fun cropBitmap(
    source: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Float,
    cropBoxSizePx: Int = 600
): Bitmap {
    var workingBitmap = source
    if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        workingBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    val out = Bitmap.createBitmap(cropBoxSizePx, cropBoxSizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)

    // Calculate drawing matrix
    val matrix = Matrix()
    // Center alignment
    val bmpWidth = workingBitmap.width.toFloat()
    val bmpHeight = workingBitmap.height.toFloat()

    // Fit scale factor
    val baseScale = max(cropBoxSizePx / bmpWidth, cropBoxSizePx / bmpHeight)
    val finalScale = baseScale * scale

    // Pivot center
    val px = bmpWidth / 2f
    val py = bmpHeight / 2f

    matrix.postTranslate(-px, -py)
    matrix.postScale(finalScale, finalScale)
    matrix.postTranslate(cropBoxSizePx / 2f + offsetX, cropBoxSizePx / 2f + offsetY)

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(workingBitmap, matrix, paint)

    return out
}
