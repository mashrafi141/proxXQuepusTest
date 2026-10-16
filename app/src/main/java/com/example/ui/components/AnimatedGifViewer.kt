package com.example.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AnimatedGifViewer(
    uriString: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var drawable by remember(uriString) { mutableStateOf<Drawable?>(null) }
    var isLoading by remember(uriString) { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        isLoading = true
        drawable = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeDrawable(source)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        isLoading = false
    }

    DisposableEffect(drawable) {
        val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            drawable as? AnimatedImageDrawable
        } else null

        anim?.start()

        onDispose {
            anim?.stop()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (drawable != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(drawable)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        (drawable as? AnimatedImageDrawable)?.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            CircularProgressIndicator(color = CyanPrimary)
        }
    }
}
