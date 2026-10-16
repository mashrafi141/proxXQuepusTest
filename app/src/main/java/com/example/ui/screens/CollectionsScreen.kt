package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.MediaEntity
import com.example.model.MediaType
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.MediaCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecondary
import com.example.viewmodel.CollectionType
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val totalCount by viewModel.totalCount.collectAsState()
    val videoCount by viewModel.videoCount.collectAsState()
    val photoCount by viewModel.photoCount.collectAsState()
    val gifCount by viewModel.gifCount.collectAsState()
    val audioCount by viewModel.audioCount.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()
    val likedCount by viewModel.likedCount.collectAsState()
    val recentCount by viewModel.recentCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_proxxquepus_logo),
                            contentDescription = "Logo",
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Collections",
                            color = CyanPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            FloatingBottomBar(
                currentScreen = Screen.Collections,
                onNavigate = { viewModel.setScreen(it) }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SAVED COLLECTIONS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CollectionCard(
                    title = "Favorites",
                    count = favoriteCount,
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFFF5252),
                    onClick = { viewModel.setScreen(Screen.CollectionView("Favorites", CollectionType.FAVORITES)) },
                    modifier = Modifier.weight(1f)
                )

                CollectionCard(
                    title = "Liked",
                    count = likedCount,
                    icon = Icons.Default.ThumbUp,
                    iconColor = CyanPrimary,
                    onClick = { viewModel.setScreen(Screen.CollectionView("Liked Media", CollectionType.LIKED)) },
                    modifier = Modifier.weight(1f)
                )
            }

            CollectionCard(
                title = "Recent Media",
                count = recentCount,
                icon = Icons.Default.History,
                iconColor = VioletSecondary,
                onClick = { viewModel.setScreen(Screen.CollectionView("Recent Media", CollectionType.RECENT)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MEDIA TYPES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CollectionCard(
                    title = "Videos",
                    count = videoCount,
                    icon = Icons.Default.VideoLibrary,
                    iconColor = CyanPrimary,
                    onClick = { viewModel.setScreen(Screen.CollectionView("All Videos", CollectionType.VIDEOS)) },
                    modifier = Modifier.weight(1f)
                )

                CollectionCard(
                    title = "Photos",
                    count = photoCount,
                    icon = Icons.Default.Photo,
                    iconColor = Color(0xFF00E676),
                    onClick = { viewModel.setScreen(Screen.CollectionView("All Photos", CollectionType.PHOTOS)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CollectionCard(
                    title = "GIFs",
                    count = gifCount,
                    icon = Icons.Default.Gif,
                    iconColor = Color(0xFFFF9100),
                    onClick = { viewModel.setScreen(Screen.CollectionView("All GIFs", CollectionType.GIFS)) },
                    modifier = Modifier.weight(1f)
                )

                CollectionCard(
                    title = "Audio",
                    count = audioCount,
                    icon = Icons.Default.MusicNote,
                    iconColor = Color(0xFF00E5FF),
                    onClick = { viewModel.setScreen(Screen.CollectionView("All Audio", CollectionType.AUDIO)) },
                    modifier = Modifier.weight(1f)
                )
            }

            CollectionCard(
                title = "All Media",
                count = totalCount,
                icon = Icons.Default.ViewModule,
                iconColor = Color.White,
                onClick = { viewModel.setScreen(Screen.CollectionView("All Media", CollectionType.ALL)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CollectionCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$count items",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    title: String,
    type: CollectionType,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val itemsFlow: kotlinx.coroutines.flow.Flow<List<MediaEntity>> = remember(type) {
        when (type) {
            CollectionType.ALL -> viewModel.repository.getAllMedia()
            CollectionType.VIDEOS -> viewModel.repository.getMediaByType(MediaType.VIDEO)
            CollectionType.PHOTOS -> viewModel.repository.getMediaByType(MediaType.PHOTO)
            CollectionType.GIFS -> viewModel.repository.getMediaByType(MediaType.GIF)
            CollectionType.AUDIO -> viewModel.repository.getMediaByType(MediaType.AUDIO)
            CollectionType.FAVORITES -> viewModel.repository.getFavoriteMedia()
            CollectionType.LIKED -> viewModel.repository.getLikedMedia()
            CollectionType.RECENT -> viewModel.repository.getRecentMedia()
        }
    }
    val mediaItems: List<MediaEntity> by itemsFlow.collectAsState(initial = emptyList())
    val selectedUris by viewModel.selectedMediaUris.collectAsState()
    var activePreviewVideoUri by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = CyanPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No items found in this collection", color = TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 105.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(mediaItems, key = { it.uriString }) { media ->
                    val isPreviewing = activePreviewVideoUri == media.uriString
                    MediaCard(
                        media = media,
                        thumbnailEngine = viewModel.repository.thumbnailEngine,
                        isSelected = selectedUris.contains(media.uriString),
                        isSelectionMode = false,
                        isPreviewing = isPreviewing,
                        onTogglePreview = {
                            activePreviewVideoUri = if (activePreviewVideoUri == media.uriString) null else media.uriString
                        },
                        onClick = {
                            activePreviewVideoUri = null
                            val index = mediaItems.indexOf(media)
                            viewModel.openMediaViewer(mediaItems, index)
                        },
                        onLongClick = {}
                    )
                }
            }
        }
    }
}
