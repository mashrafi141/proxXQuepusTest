package com.example.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

class ThumbnailEngine(private val context: Context) {

    // In-memory cache for ultra-fast UI rendering
    private val memoryCache: LruCache<String, Bitmap>
    private val videoFramesCache: LruCache<String, List<Bitmap>>

    // App-private disk cache folder: NEVER stored inside user's root folder
    private val diskCacheDir: File

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 6 // Generous allocation for fast scrolling
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        videoFramesCache = object : LruCache<String, List<Bitmap>>(maxMemory / 10) {
            override fun sizeOf(key: String, value: List<Bitmap>): Int {
                return value.sumOf { it.byteCount } / 1024
            }
        }

        diskCacheDir = File(context.cacheDir, "proxxquepus_thumbs").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getCacheKey(uriString: String, size: Long, dateModified: Long): String {
        val raw = "$uriString-$size-$dateModified"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun getThumbnail(
        uriString: String,
        mediaType: MediaType,
        fileSize: Long,
        dateModified: Long,
        targetWidth: Int = 300,
        targetHeight: Int = 300
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = getCacheKey(uriString, fileSize, dateModified)

        // 1. Memory check (instant)
        memoryCache.get(key)?.let { return@withContext it }

        // 2. Disk cache check (very fast)
        val cachedFile = File(diskCacheDir, "$key.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                cachedFile.delete()
            }
        }

        // 3. Generate from actual media without modifying original storage
        val generated = when (mediaType) {
            MediaType.VIDEO -> generateVideoThumbnail(uriString, targetWidth, targetHeight)
            MediaType.PHOTO -> generateImageThumbnail(uriString, targetWidth, targetHeight)
            MediaType.GIF -> generateGifThumbnail(uriString, targetWidth, targetHeight)
            MediaType.AUDIO, MediaType.OTHER -> null
        }

        if (generated != null) {
            memoryCache.put(key, generated)
            try {
                FileOutputStream(cachedFile).use { out ->
                    generated.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (ignored: Exception) {
            }
        }

        generated
    }

    private fun generateVideoThumbnail(uriString: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uriString))
            val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                scaleDown(frame, targetWidth, targetHeight)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Extracts lightweight animated keyframe sequence across the video duration for live card preview.
     */
    suspend fun getVideoPreviewFrames(
        uriString: String,
        durationMs: Long,
        frameCount: Int = 4,
        targetWidth: Int = 280,
        targetHeight: Int = 280
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val cacheKey = "video_frames_$uriString"
        videoFramesCache.get(cacheKey)?.let { return@withContext it }

        val frames = mutableListOf<Bitmap>()
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uriString))
            val dur = if (durationMs > 0) durationMs else {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
            }
            val stepUs = (dur * 1000L) / (frameCount + 1).coerceAtLeast(2)
            for (i in 1..frameCount) {
                val timeUs = stepUs * i
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs)
                if (frame != null) {
                    val scaled = scaleDown(frame, targetWidth, targetHeight)
                    frames.add(scaled)
                }
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Exception) {}
        }

        if (frames.isNotEmpty()) {
            videoFramesCache.put(cacheKey, frames)
        }
        frames
    }

    /**
     * Robust SAF image thumbnail generation:
     * Reads actual bytes from SAF ContentResolver stream.
     * Works for normal files (.jpg, .png) and extra-extension files (.jpg.dat, .png.secret).
     */
    private fun generateImageThumbnail(uriString: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val uri = Uri.parse(uriString)

        // Try ImageDecoder on Android P+ first if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val size = info.size
                    val sample = calculateSampleSize(size.width, size.height, targetWidth, targetHeight)
                    decoder.setTargetSampleSize(sample)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                if (bitmap != null) {
                    return scaleDown(bitmap, targetWidth, targetHeight)
                }
            } catch (ignored: Exception) {
                // Fall back to robust stream decoding below
            }
        }

        // Direct bytes reading via ContentResolver
        return try {
            val bytes = readStreamBytes(uri, maxBytes = 4 * 1024 * 1024) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (decoded != null) {
                scaleDown(decoded, targetWidth, targetHeight)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts an actual representative frame from GIF media (animation.gif, animation.gif.dat, etc.)
     */
    private fun generateGifThumbnail(uriString: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val uri = Uri.parse(uriString)

        // Android P+ ImageDecoder natively decodes animated GIF first frame
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val sample = calculateSampleSize(info.size.width, info.size.height, targetWidth, targetHeight)
                    decoder.setTargetSampleSize(sample)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                if (bitmap != null) {
                    return scaleDown(bitmap, targetWidth, targetHeight)
                }
            } catch (ignored: Exception) {}
        }

        // Fallback: BitmapFactory decodes first frame of GIF directly from stream bytes
        return generateImageThumbnail(uriString, targetWidth, targetHeight)
    }

    private fun readStreamBytes(uri: Uri, maxBytes: Int): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                var bytesRead: Int
                var total = 0
                while (input.read(chunk).also { bytesRead = it } != -1) {
                    buffer.write(chunk, 0, bytesRead)
                    total += bytesRead
                    if (total >= maxBytes) break
                }
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleDown(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= targetWidth && height <= targetHeight) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width > height) {
            targetWidth to (targetWidth / ratio).toInt()
        } else {
            (targetHeight * ratio).toInt() to targetHeight
        }
        val safeW = newW.coerceAtLeast(1)
        val safeH = newH.coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, safeW, safeH, true)
    }

    fun clearCache() {
        memoryCache.evictAll()
        diskCacheDir.deleteRecursively()
        diskCacheDir.mkdirs()
    }
}
