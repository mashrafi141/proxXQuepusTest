package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.database.MediaEntity
import com.example.model.MediaType
import com.example.player.PlayerGestureDetector
import com.example.player.PlayerGestureFeedback
import com.example.player.VideoPlayerController
import com.example.ui.components.AnimatedGifViewer
import com.example.ui.components.VerticalReelMediaContainer
import com.example.ui.components.VideoPlayerOverlay
import com.example.ui.components.ZoomablePhoto
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun MediaViewerScreen(
    initialIndex: Int,
    mediaList: List<MediaEntity>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitialPage = remember { initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0)) }
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { mediaList.size }
    )
    val currentIndex = pagerState.currentPage
    val currentMedia = mediaList.getOrNull(currentIndex) ?: mediaList.first()

    // BackHandler for proper backstack return
    BackHandler {
        onBack()
    }

    // Local optimistic states keyed strictly to currentMedia.uriString
    var isFavoriteLocal by remember(currentMedia.uriString) { mutableStateOf(currentMedia.isFavorite) }
    var isLikedLocal by remember(currentMedia.uriString) { mutableStateOf(currentMedia.isLiked) }

    // Synchronize local states when media item changes
    LaunchedEffect(currentMedia.uriString) {
        isFavoriteLocal = currentMedia.isFavorite
        isLikedLocal = currentMedia.isLiked
    }

    // Centralized navigation functions
    fun goToNextMedia() {
        if (pagerState.currentPage < mediaList.lastIndex) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    fun goToPreviousMedia() {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    // Single shared VideoPlayerController
    val playerController = remember { VideoPlayerController(context) }
    val playerUiState by playerController.uiState.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf(PlayerGestureFeedback()) }
    var doubleTapFeedbackSide by remember { mutableStateOf<Boolean?>(null) }
    var doubleTapFeedbackJob by remember { mutableStateOf<Job?>(null) }

    fun handleDoubleTapSeek(isForward: Boolean) {
        val delta = if (isForward) 10000L else -10000L
        playerController.seekRelative(delta)
        doubleTapFeedbackSide = isForward
        doubleTapFeedbackJob?.cancel()
        doubleTapFeedbackJob = coroutineScope.launch {
            delay(500)
            doubleTapFeedbackSide = null
        }
    }

    var isPhotoZoomed by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of video playback
    LaunchedEffect(areControlsVisible, playerUiState.isPlaying) {
        if (areControlsVisible && playerUiState.isPlaying && !isControlsLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Lifecycle for video playback: load video when currentIndex changes
    LaunchedEffect(currentIndex) {
        isPhotoZoomed = false
        val item = mediaList[currentIndex]
        if (item.mediaType == MediaType.VIDEO || item.mediaType == MediaType.AUDIO) {
            playerController.setSource(item.uriString, item.playbackPositionMs)
        } else {
            playerController.pause()
        }
    }

    // Save playback position on item change or exit
    DisposableEffect(currentIndex) {
        onDispose {
            if (currentMedia.mediaType == MediaType.VIDEO || currentMedia.mediaType == MediaType.AUDIO) {
                val lastPos = playerUiState.currentPositionMs
                if (lastPos > 0) {
                    viewModel.savePlaybackPosition(currentMedia.uriString, lastPos)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerController.release()
        }
    }

    // Vertical Reel/Paged Container that renders current media with smooth drag animations
    VerticalReelMediaContainer(
        pagerState = pagerState,
        isZoomed = isPhotoZoomed,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { renderIndex ->
        val mediaItem = mediaList[renderIndex]
        val isCurrentItem = renderIndex == currentIndex

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (mediaItem.mediaType) {
                MediaType.VIDEO, MediaType.AUDIO -> {
                    if (isCurrentItem) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 1. Background video surface with seek and tap detection
                            PlayerGestureDetector(
                                currentPositionMs = playerUiState.currentPositionMs,
                                durationMs = playerUiState.durationMs,
                                onSeek = { targetMs -> playerController.seekTo(targetMs) },
                                onToggleControls = {
                                    if (!isControlsLocked) {
                                        areControlsVisible = !areControlsVisible
                                    }
                                },
                                onDoubleTapSeek = { isForward ->
                                    handleDoubleTapSeek(isForward)
                                },
                                onFeedbackUpdate = { feedback -> gestureFeedback = feedback },
                                onFeedbackDismiss = { gestureFeedback = gestureFeedback.copy(isVisible = false) }
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = playerController.getPlayer()
                                            useController = false
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 2. Interactive Overlay with single-tap responsive buttons
                            VideoPlayerOverlay(
                                media = mediaItem,
                                playerController = playerController,
                                playerUiState = playerUiState,
                                areControlsVisible = areControlsVisible,
                                isControlsLocked = isControlsLocked,
                                isFavorite = isFavoriteLocal,
                                isLiked = isLikedLocal,
                                gestureFeedback = gestureFeedback,
                                doubleTapFeedbackSide = doubleTapFeedbackSide,
                                onDoubleTapSeek = { isForward ->
                                    handleDoubleTapSeek(isForward)
                                },
                                onToggleControlsLock = {
                                    isControlsLocked = !isControlsLocked
                                    if (isControlsLocked) areControlsVisible = false
                                },
                                onToggleControls = {
                                    if (!isControlsLocked) {
                                        areControlsVisible = !areControlsVisible
                                    }
                                },
                                onPrevious = { goToPreviousMedia() },
                                onNext = { goToNextMedia() },
                                onBack = onBack,
                                onToggleFavorite = {
                                    isFavoriteLocal = !isFavoriteLocal
                                    viewModel.toggleFavorite(mediaItem)
                                },
                                onToggleLiked = {
                                    isLikedLocal = !isLikedLocal
                                    viewModel.toggleLiked(mediaItem)
                                }
                            )
                        }
                    } else {
                        // Adjacent video placeholder
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                MediaType.PHOTO -> {
                    var imageBitmap by remember(mediaItem.uriString) { mutableStateOf<Bitmap?>(null) }
                    var isLoadingImage by remember(mediaItem.uriString) { mutableStateOf(true) }

                    LaunchedEffect(mediaItem.uriString) {
                        isLoadingImage = true
                        imageBitmap = loadOptimizedBitmap(context, mediaItem.uriString)
                        isLoadingImage = false
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (imageBitmap != null) {
                            ZoomablePhoto(
                                bitmap = imageBitmap!!,
                                contentDescription = mediaItem.displayName,
                                uriKey = mediaItem.uriString,
                                onZoomStateChanged = { zoomed ->
                                    if (isCurrentItem) {
                                        isPhotoZoomed = zoomed
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isLoadingImage) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(48.dp))
                            }
                        }

                        if (isCurrentItem) {
                            MediaTopBar(
                                displayName = mediaItem.displayName,
                                currentIndex = currentIndex,
                                totalCount = mediaList.size,
                                isLiked = isLikedLocal,
                                isFavorite = isFavoriteLocal,
                                onBack = onBack,
                                onToggleLiked = {
                                    isLikedLocal = !isLikedLocal
                                    viewModel.toggleLiked(mediaItem)
                                },
                                onToggleFavorite = {
                                    isFavoriteLocal = !isFavoriteLocal
                                    viewModel.toggleFavorite(mediaItem)
                                }
                            )
                        }
                    }
                }

                MediaType.GIF -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedGifViewer(
                            uriString = mediaItem.uriString,
                            contentDescription = mediaItem.displayName,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isCurrentItem) {
                            MediaTopBar(
                                displayName = mediaItem.displayName,
                                currentIndex = currentIndex,
                                totalCount = mediaList.size,
                                isLiked = isLikedLocal,
                                isFavorite = isFavoriteLocal,
                                onBack = onBack,
                                onToggleLiked = {
                                    isLikedLocal = !isLikedLocal
                                    viewModel.toggleLiked(mediaItem)
                                },
                                onToggleFavorite = {
                                    isFavoriteLocal = !isFavoriteLocal
                                    viewModel.toggleFavorite(mediaItem)
                                }
                            )
                        }
                    }
                }

                MediaType.OTHER -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unsupported media file", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTopBar(
    displayName: String,
    currentIndex: Int,
    totalCount: Int,
    isLiked: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleLiked: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("image_back_button")
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color(0x80000000))
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
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onToggleLiked,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color(0x80000000))
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (isLiked) CyanPrimary else Color.White
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color(0x80000000))
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF5252) else Color.White
                )
            }
        }
    }
}

/**
 * Robust SAF Bitmap decoding for Photo:
 * Reads stream directly with appropriate downsampling to prevent huge memory allocations
 * while retaining high quality for up to 5x zoom.
 */
private suspend fun loadOptimizedBitmap(context: android.content.Context, uriString: String): Bitmap? = withContext(Dispatchers.IO) {
    val uri = Uri.parse(uriString)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val maxDim = 2048
                if (info.size.width > maxDim || info.size.height > maxDim) {
                    val sample = if (info.size.width > info.size.height) info.size.width / maxDim else info.size.height / maxDim
                    decoder.setTargetSampleSize(sample.coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            if (bitmap != null) return@withContext bitmap
        } catch (ignored: Exception) {}
    }

    try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return@withContext null

        val maxDim = 2048
        var sample = 1
        if (options.outWidth > maxDim || options.outHeight > maxDim) {
            val larger = if (options.outWidth > options.outHeight) options.outWidth else options.outHeight
            sample = larger / maxDim
        }
        options.inSampleSize = sample.coerceAtLeast(1)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (e: Exception) {
        null
    }
}
