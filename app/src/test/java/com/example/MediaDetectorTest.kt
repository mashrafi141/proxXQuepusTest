package com.example

import com.example.model.MediaType
import com.example.util.MediaDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDetectorTest {

    @Test
    fun detectsStandardVideo() {
        val res = MediaDetector.detect("vacation_clip.mp4")
        assertEquals(MediaType.VIDEO, res.mediaType)
        assertEquals("vacation_clip.mp4", res.displayName)
        assertFalse(res.isObfuscated)
    }

    @Test
    fun detectsObfuscatedVideo() {
        val res = MediaDetector.detect("secret_video.mp4.dat")
        assertEquals(MediaType.VIDEO, res.mediaType)
        assertEquals("secret_video.mp4", res.displayName)
        assertTrue(res.isObfuscated)
    }

    @Test
    fun detectsObfuscatedMovieWithCustomSuffix() {
        val res = MediaDetector.detect("movie.mkv.hid")
        assertEquals(MediaType.VIDEO, res.mediaType)
        assertEquals("movie.mkv", res.displayName)
        assertTrue(res.isObfuscated)
    }

    @Test
    fun detectsArbitrarySuffixes() {
        val res1 = MediaDetector.detect("rootVideo.mp4.dat")
        assertEquals(MediaType.VIDEO, res1.mediaType)
        assertEquals("rootVideo.mp4", res1.displayName)

        val res2 = MediaDetector.detect("rootMovie.mkv.hid")
        assertEquals(MediaType.VIDEO, res2.mediaType)
        assertEquals("rootMovie.mkv", res2.displayName)

        val res3 = MediaDetector.detect("photo.jpg.secret")
        assertEquals(MediaType.PHOTO, res3.mediaType)
        assertEquals("photo.jpg", res3.displayName)

        val res4 = MediaDetector.detect("image.png.xyz")
        assertEquals(MediaType.PHOTO, res4.mediaType)
        assertEquals("image.png", res4.displayName)

        val res5 = MediaDetector.detect("rootGif.gif.xyz")
        assertEquals(MediaType.GIF, res5.mediaType)
        assertEquals("rootGif.gif", res5.displayName)

        val res6 = MediaDetector.detect("rootAudio.mp3")
        assertEquals(MediaType.AUDIO, res6.mediaType)
        assertEquals("rootAudio.mp3", res6.displayName)

        val res7 = MediaDetector.detect("song.m4a.anything")
        assertEquals(MediaType.AUDIO, res7.mediaType)
        assertEquals("song.m4a", res7.displayName)
        assertTrue(res7.isObfuscated)
    }

    @Test
    fun ignoresNonMediaFiles() {
        val res = MediaDetector.detect("random.txt")
        assertEquals(MediaType.OTHER, res.mediaType)

        val resBackup = MediaDetector.detect("my.mp4.backup.txt")
        assertEquals(MediaType.OTHER, resBackup.mediaType)
    }

    @Test
    fun detectsHiddenDotPrefixedPhoto() {
        val res = MediaDetector.detect(".private_pic.jpg")
        assertEquals(MediaType.PHOTO, res.mediaType)
        assertEquals("private_pic.jpg", res.displayName)
    }

    @Test
    fun formatsDurationCorrectly() {
        assertEquals("0:00", MediaDetector.formatDuration(0))
        assertEquals("1:05", MediaDetector.formatDuration(65000))
        assertEquals("1:01:05", MediaDetector.formatDuration(3665000))
    }

    @Test
    fun formatsFileSizeCorrectly() {
        assertEquals("0 B", MediaDetector.formatFileSize(0))
        assertEquals("1.5 KB", MediaDetector.formatFileSize(1536))
        assertEquals("10.0 MB", MediaDetector.formatFileSize(10 * 1024 * 1024))
    }
}
