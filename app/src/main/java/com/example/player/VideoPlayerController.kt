package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val errorMessage: String? = null
)

class VideoPlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _uiState.value = _uiState.value.copy(isBuffering = true, errorMessage = null)
                        }
                        Player.STATE_READY -> {
                            _uiState.value = _uiState.value.copy(
                                isBuffering = false,
                                durationMs = duration.coerceAtLeast(0L),
                                errorMessage = null
                            )
                        }
                        Player.STATE_ENDED -> {
                            _uiState.value = _uiState.value.copy(isEnded = true, isPlaying = false)
                        }
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.value = _uiState.value.copy(
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Playback error"
                    )
                }
            })
        }
    }

    /**
     * CRITICAL: Direct SAF playback without copying the video file to cache!
     * Media3 handles ContentResolver SAF URIs directly through its native DataSource.
     */
    fun setSource(uriString: String, startPositionMs: Long = 0L) {
        val player = getPlayer()
        player.stop()
        player.clearMediaItems()

        val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
        player.setMediaItem(mediaItem)
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.prepare()
        player.playWhenReady = true

        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(200)
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _uiState.value = _uiState.value.copy(currentPositionMs = clamped)
    }

    fun seekRelative(offsetMs: Long) {
        val player = exoPlayer ?: return
        seekTo(player.currentPosition + offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun toggleMute() {
        val player = exoPlayer ?: return
        val willMute = player.volume > 0f
        player.volume = if (willMute) 0f else 1f
        _uiState.value = _uiState.value.copy(isMuted = willMute)
    }

    fun replay() {
        seekTo(0L)
        play()
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
