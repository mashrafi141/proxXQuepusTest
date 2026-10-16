package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.MediaEntity
import com.example.player.FeedbackIcon
import com.example.player.PlayerGestureFeedback
import com.example.player.PlayerUiState
import com.example.player.VideoPlayerController
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.util.MediaDetector

@Composable
fun VideoPlayerOverlay(
    media: MediaEntity,
    playerController: VideoPlayerController,
    playerUiState: PlayerUiState,
    areControlsVisible: Boolean,
    isControlsLocked: Boolean,
    isFavorite: Boolean,
    isLiked: Boolean,
    gestureFeedback: PlayerGestureFeedback,
    doubleTapFeedbackSide: Boolean? = null,
    onDoubleTapSeek: (Boolean) -> Unit = {},
    onToggleControlsLock: () -> Unit,
    onToggleControls: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLiked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Double-tap 10s seek HUD indicator (Left for backward -10s, Right for forward +10s)
        if (doubleTapFeedbackSide != null) {
            val isForward = doubleTapFeedbackSide == true
            Box(
                modifier = Modifier
                    .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 32.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD0A0C10))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = if (isForward) "+10s" else "-10s",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isForward) "+10s" else "-10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center gesture feedback HUD
        if (gestureFeedback.isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0A0C10))
                    .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val icon = when (gestureFeedback.iconType) {
                        FeedbackIcon.SEEK_FORWARD -> Icons.Default.FastForward
                        FeedbackIcon.SEEK_BACKWARD -> Icons.Default.FastRewind
                        FeedbackIcon.NEXT_MEDIA -> Icons.Default.SkipNext
                        FeedbackIcon.PREV_MEDIA -> Icons.Default.SkipPrevious
                        FeedbackIcon.BRIGHTNESS -> Icons.Default.BrightnessLow
                        FeedbackIcon.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
                        FeedbackIcon.MUTE -> Icons.AutoMirrored.Filled.VolumeMute
                        FeedbackIcon.NONE -> null
                    }
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = gestureFeedback.text,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Buffering indicator
        if (playerUiState.isBuffering) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CyanPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Lock button (always visible when locked, top right)
        if (isControlsLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 24.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = onToggleControlsLock,
                    modifier = Modifier
                        .testTag("player_unlock_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = CyanPrimary
                    )
                }
            }
        }

        // Full Interactive HUD with guaranteed click dispatch
        AnimatedVisibility(
            visible = areControlsVisible && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Tapping empty space on the overlay toggles controls; double-tapping seeks 10s backward (left) or forward (right)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onToggleControls()
                            },
                            onDoubleTap = { offset ->
                                val isRightHalf = offset.x > size.width / 2
                                onDoubleTapSeek(isRightHalf)
                            }
                        )
                    }
            ) {

                // Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xEE000000), Color(0x99000000), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .testTag("player_back_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = media.displayName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = MediaDetector.formatFileSize(media.size),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Like Button (Instant 1-tap feedback)
                        IconButton(
                            onClick = onToggleLiked,
                            modifier = Modifier
                                .testTag("player_like_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLiked) CyanPrimary else Color.White
                            )
                        }

                        // Favorite Button (Instant 1-tap feedback)
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .testTag("player_fav_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF5252) else Color.White
                            )
                        }

                        // Lock Button
                        IconButton(
                            onClick = onToggleControlsLock,
                            modifier = Modifier
                                .testTag("player_lock_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Bottom Controls Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xEB000000))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Scrubber Slider + Compact Play/Pause + Times
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("player_play_pause_button")
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                                    .clickable { playerController.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerUiState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = MediaDetector.formatDuration(playerUiState.currentPositionMs),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            var sliderPosition by remember { mutableFloatStateOf(0f) }
                            var isUserDragging by remember { mutableStateOf(false) }

                            val currentFrac = if (playerUiState.durationMs > 0) {
                                (playerUiState.currentPositionMs.toFloat() / playerUiState.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Slider(
                                value = if (isUserDragging) sliderPosition else currentFrac,
                                onValueChange = { frac ->
                                    isUserDragging = true
                                    sliderPosition = frac
                                },
                                onValueChangeFinished = {
                                    isUserDragging = false
                                    val targetMs = (sliderPosition * playerUiState.durationMs).toLong()
                                    playerController.seekTo(targetMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanPrimary,
                                    activeTrackColor = CyanPrimary,
                                    inactiveTrackColor = Color(0x40FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Text(
                                text = MediaDetector.formatDuration(playerUiState.durationMs),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Action Row (Mute, Replay, Speed)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { playerController.toggleMute() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (playerUiState.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute Toggle",
                                        tint = if (playerUiState.isMuted) Color(0xFFFF5252) else Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { playerController.replay() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Replay",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Speed Selector Button (1x, 1.25x, 1.5x, 2x)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3300E5FF))
                                    .clickable {
                                        val nextSpeed = when (playerUiState.playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        playerController.setPlaybackSpeed(nextSpeed)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${playerUiState.playbackSpeed}x",
                                    color = CyanPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
