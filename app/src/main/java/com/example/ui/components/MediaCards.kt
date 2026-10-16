package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import android.graphics.BitmapFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Circle
import java.io.File
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.FolderEntity
import com.example.database.MediaEntity
import com.example.model.MediaType
import com.example.thumbnail.ThumbnailEngine
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecondary
import com.example.util.MediaDetector

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    media: MediaEntity,
    thumbnailEngine: ThumbnailEngine,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreviewing: Boolean = false,
    onTogglePreview: () -> Unit = {}
) {
    var thumbnailBitmap by remember(media.uriString, media.dateModified) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(media.uriString, media.dateModified) {
        thumbnailBitmap = thumbnailEngine.getThumbnail(
            uriString = media.uriString,
            mediaType = media.mediaType,
            fileSize = media.size,
            dateModified = media.dateModified,
            targetWidth = 320,
            targetHeight = 320
        )
    }

    val cardBorderModifier = if (isPreviewing) {
        Modifier
    } else {
        Modifier.border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CyanPrimary else DarkSurfaceBorder,
            shape = RoundedCornerShape(10.dp)
        )
    }

    Column(
        modifier = modifier
            .testTag("media_card_${media.displayName}")
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(cardBorderModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
                .background(Color(0xFF0F131A))
        ) {
            if (isPreviewing && media.mediaType == MediaType.VIDEO) {
                InlineVideoPreview(
                    uriString = media.uriString,
                    onStopPreview = onTogglePreview,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = media.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder preserving dimensions
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val fallbackIcon = when (media.mediaType) {
                            MediaType.VIDEO -> Icons.Default.Movie
                            MediaType.PHOTO -> Icons.Default.Photo
                            MediaType.GIF -> Icons.Default.Movie
                            MediaType.AUDIO -> Icons.Default.MusicNote
                            else -> Icons.Default.Photo
                        }
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Interactive Video Play Preview button
                if (media.mediaType == MediaType.VIDEO && !isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, CyanPrimary.copy(alpha = 0.85f), CircleShape)
                            .clickable { onTogglePreview() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Video",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Gradients and badges only when NOT previewing
            if (!isPreviewing) {
                // Top gradient overlay for tags
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x99000000), Color.Transparent)
                            )
                        )
                )

                // Bottom gradient overlay for duration/size
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xB3000000))
                            )
                        )
                )

                // Badges: Hidden / Obfuscated
                if (media.isHidden || media.isObfuscated) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (media.isObfuscated) "VIRTUAL" else "HIDDEN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                // Duration badge for video
                if (media.mediaType == MediaType.VIDEO && media.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = MediaDetector.formatDuration(media.durationMs),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Liked/Favorite icons
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    if (media.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (media.isLiked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Liked",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Selection indicator
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isSelected) CyanPrimary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Compact title text
        Text(
            text = media.displayName,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: FolderEntity,
    childFolderCount: Int = 0,
    mediaCount: Int = 0,
    customProfilePath: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profileBitmap = remember(customProfilePath) {
        if (!customProfilePath.isNullOrBlank()) {
            val file = File(customProfilePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    }

    Column(
        modifier = modifier
            .testTag("folder_card_${folder.displayName}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp)
    ) {
        // Prominent Profile / Cover Area (Clean, no corner icons)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D121B))
                .border(
                    width = if (profileBitmap != null) 1.5.dp else 1.dp,
                    color = if (profileBitmap != null) CyanPrimary.copy(alpha = 0.6f) else Color(0x2200E5FF),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = "${folder.displayName} Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom subtle shadow gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Hidden badge at top-left
            if (folder.isHidden) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HIDDEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = VioletSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full folder name display
        Text(
            text = folder.displayName,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Count Badges row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x2200E5FF))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$childFolderCount folders",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = CyanPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$mediaCount media",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}
