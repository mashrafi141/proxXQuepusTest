package com.example.util

import com.example.model.MediaType

object MediaDetector {

    // Standard video extensions
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "3gp", "flv", "wmv", "ts", "m4v"
    )

    // Standard photo extensions
    private val PHOTO_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "svg"
    )

    // Standard GIF extensions
    private val GIF_EXTENSIONS = setOf(
        "gif"
    )

    // Standard audio extensions
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "wav", "flac", "aac", "ogg", "opus", "wma"
    )

    data class DetectionResult(
        val mediaType: MediaType,
        val displayName: String,
        val detectedExtension: String,
        val isObfuscated: Boolean
    )

    /**
     * Deterministic media classifier supporting:
     * 1. Standard normal files (e.g. "video.mp4", "photo.jpg", "song.mp3", "animation.gif")
     * 2. Obfuscated / extra-extension files where valid media extension is followed by arbitrary dot-suffixes
     *    (e.g. "video.mp4.dat", "movie.mkv.hid", "photo.jpg.secret", "audio.mp3.xyz", "rootVideo.mp4.dat")
     * 3. Dot-prefixed hidden files (e.g. ".private_video.mp4", ".hidden.jpg.dat")
     *
     * The physical filename is NEVER modified.
     * Edge case prevention: "my.mp4.backup.txt" is NOT a video because its text after .mp4 is ".backup.txt" where
     * "backup" is not an arbitrary single suffix after extension. Specifically, we inspect the dot-separated segments.
     * If a segment matching a media extension exists near the end (either the last segment, or followed by 1-2 suffixes
     * without other known document/archive extensions), we classify accurately.
     */
    fun detect(physicalName: String, mimeTypeFromSaf: String? = null): DetectionResult {
        // Strip leading dot only for display name creation
        val cleanDisplayBase = if (physicalName.startsWith(".")) physicalName.removePrefix(".") else physicalName

        val lower = physicalName.lowercase()
        val segments = lower.split('.').filter { it.isNotBlank() }

        if (segments.isEmpty()) {
            return DetectionResult(MediaType.OTHER, cleanDisplayBase, "", false)
        }

        // Case 1: Physical name ends directly with a media extension (Normal media)
        val lastExt = segments.last()
        if (lastExt in VIDEO_EXTENSIONS) {
            return DetectionResult(MediaType.VIDEO, cleanDisplayBase, lastExt, false)
        }
        if (lastExt in GIF_EXTENSIONS) {
            return DetectionResult(MediaType.GIF, cleanDisplayBase, lastExt, false)
        }
        if (lastExt in PHOTO_EXTENSIONS) {
            return DetectionResult(MediaType.PHOTO, cleanDisplayBase, lastExt, false)
        }
        if (lastExt in AUDIO_EXTENSIONS) {
            return DetectionResult(MediaType.AUDIO, cleanDisplayBase, lastExt, false)
        }

        // Case 2: Obfuscated media: ORIGINAL_MEDIA_EXTENSION + "." + EXTRA_SUFFIX(ES)
        // Find if any segment from right-to-left (before the end) is a recognized media extension.
        // e.g. "video.mp4.dat" -> segments: ["video", "mp4", "dat"] -> index 1 is "mp4"
        // e.g. "rootMovie.mkv.hid" -> index 1 is "mkv"
        for (i in segments.lastIndex - 1 downTo 0) {
            val segment = segments[i]
            val type = when (segment) {
                in VIDEO_EXTENSIONS -> MediaType.VIDEO
                in GIF_EXTENSIONS -> MediaType.GIF
                in PHOTO_EXTENSIONS -> MediaType.PHOTO
                in AUDIO_EXTENSIONS -> MediaType.AUDIO
                else -> null
            }

            if (type != null) {
                // Check edge cases: if any subsequent segment is an explicit text/doc type like "txt", "pdf", "apk", "doc"
                val subsequent = segments.subList(i + 1, segments.size)
                val isExplicitNonMedia = subsequent.any { it in NON_MEDIA_EXTENSIONS }
                if (!isExplicitNonMedia) {
                    val virtualDisplayName = buildVirtualName(physicalName, i)
                    return DetectionResult(type, virtualDisplayName, segment, true)
                }
            }
        }

        // Case 3: Fallback check against SAF mimeType if provided
        val safMime = mimeTypeFromSaf?.lowercase() ?: ""
        if (safMime.startsWith("video/")) {
            return DetectionResult(MediaType.VIDEO, cleanDisplayBase, "mp4", false)
        }
        if (safMime == "image/gif") {
            return DetectionResult(MediaType.GIF, cleanDisplayBase, "gif", false)
        }
        if (safMime.startsWith("image/")) {
            return DetectionResult(MediaType.PHOTO, cleanDisplayBase, "jpg", false)
        }
        if (safMime.startsWith("audio/")) {
            return DetectionResult(MediaType.AUDIO, cleanDisplayBase, "mp3", false)
        }

        return DetectionResult(MediaType.OTHER, cleanDisplayBase, lastExt, false)
    }

    private val NON_MEDIA_EXTENSIONS = setOf(
        "txt", "doc", "docx", "pdf", "apk", "exe", "zip", "rar", "7z", "tar", "gz", "html", "css", "js", "json", "xml"
    )

    private fun buildVirtualName(physicalName: String, mediaExtIndex: Int): String {
        val clean = if (physicalName.startsWith(".")) physicalName.removePrefix(".") else physicalName
        val dotParts = clean.split('.')
        return if (mediaExtIndex < dotParts.size) {
            dotParts.take(mediaExtIndex + 1).joinToString(".")
        } else {
            clean
        }
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = ms / 1000
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hr = totalSec / 3600
        return if (hr > 0) {
            String.format("%d:%02d:%02d", hr, min, sec)
        } else {
            String.format("%d:%02d", min, sec)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
