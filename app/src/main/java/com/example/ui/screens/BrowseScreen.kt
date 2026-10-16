package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.database.FolderWithCounts
import com.example.model.MediaType
import com.example.ui.components.CropImageDialog
import com.example.ui.components.FolderDetailsDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.FolderCard
import com.example.ui.components.MediaCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootEntity by viewModel.rootEntity.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val subfolders by viewModel.currentSubfolders.collectAsState()
    val rawMediaItems by viewModel.rawCurrentMediaItems.collectAsState()
    val mediaItems by viewModel.currentMediaItems.collectAsState()
    val selectedMediaTypeFilter by viewModel.selectedMediaTypeFilter.collectAsState()
    val selectedUris by viewModel.selectedMediaUris.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgressText by viewModel.scanProgressText.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()

    val gridState = rememberLazyGridState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Folder Profile Image Picking & Interactive Cropping state
    var profileTargetFolderUri by remember { mutableStateOf<String?>(null) }
    var profileTargetFolderName by remember { mutableStateOf<String>("") }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var folderForDetails by remember { mutableStateOf<FolderWithCounts?>(null) }
    var activePreviewVideoUri by remember { mutableStateOf<String?>(null) }

    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && profileTargetFolderUri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val loadedBitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (loadedBitmap != null) {
                    bitmapToCrop = loadedBitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Folder Picker Contract for Storage Access Framework
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (ignored: Exception) {}
            val folderName = uri.lastPathSegment?.split(":")?.lastOrNull() ?: "Root Storage"
            viewModel.setRootFolder(uri, folderName)
        }
    }

    val isSelectionMode = selectedUris.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Selection Top Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedUris.size} selected",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("selection_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll(mediaItems) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = CyanPrimary)
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("selection_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color(0xFFFF5252))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            } else {
                // Standard Top Bar with logo icon above ProxXQuepus and actions
                TopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_proxxquepus_logo),
                                contentDescription = "ProxXQuepus Logo",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ProxXQuepus",
                                    color = CyanPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isScanning) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        color = CyanPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            if (isScanning && scanProgressText.isNotBlank()) {
                                Text(
                                    text = scanProgressText,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(
                            onClick = { viewModel.setScreen(Screen.Search) },
                            modifier = Modifier.testTag("search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }

                        // Sort Menu
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(DarkSurfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSort(SortField.NAME, SortDirection.ASCENDING)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z to A)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSort(SortField.NAME, SortDirection.DESCENDING)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSort(SortField.DATE, SortDirection.DESCENDING)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Oldest first)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSort(SortField.DATE, SortDirection.ASCENDING)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest first)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSort(SortField.SIZE, SortDirection.DESCENDING)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Rescan / Refresh Button
                        IconButton(onClick = {
                            rootEntity?.let {
                                viewModel.startBackgroundScan(Uri.parse(it.treeUriString))
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            }
        },
        bottomBar = {
            FloatingBottomBar(
                currentScreen = Screen.Browse,
                onNavigate = { viewModel.setScreen(it) }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->

        // If no root selected yet, prompt user with stylish glass banner
        if (rootEntity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Root Folder",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ProxXQuepus exposes and plays media inside hidden and dot-prefixed folders without modifying your storage.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("select_root_folder_button")
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Grant Folder Access", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Main media & folder explorer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Breadcrumbs bar
                if (breadcrumbs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        breadcrumbs.forEachIndexed { index, crumb ->
                            Text(
                                text = crumb.title,
                                color = if (index == breadcrumbs.lastIndex) CyanPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.navigateToBreadcrumb(index) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            if (index < breadcrumbs.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Dynamic Media Type Filter Bar (Instant professional type-wise filtering inside folder)
                if (rawMediaItems.isNotEmpty()) {
                    val allCount = rawMediaItems.size
                    val photoCount = remember(rawMediaItems) { rawMediaItems.count { it.mediaType == MediaType.PHOTO } }
                    val videoCount = remember(rawMediaItems) { rawMediaItems.count { it.mediaType == MediaType.VIDEO } }
                    val gifCount = remember(rawMediaItems) { rawMediaItems.count { it.mediaType == MediaType.GIF } }
                    val audioCount = remember(rawMediaItems) { rawMediaItems.count { it.mediaType == MediaType.AUDIO } }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Filter
                        val isAllSelected = selectedMediaTypeFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { viewModel.setMediaTypeFilter(null) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ViewModule,
                                    contentDescription = null,
                                    tint = if (isAllSelected) Color.Black else CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "All ($allCount)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllSelected) Color.Black else TextPrimary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                containerColor = DarkSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = if (isAllSelected) CyanPrimary else DarkSurfaceBorder
                            )
                        )

                        // Photos Filter
                        if (photoCount > 0) {
                            val isPhotoSelected = selectedMediaTypeFilter == MediaType.PHOTO
                            FilterChip(
                                selected = isPhotoSelected,
                                onClick = {
                                    viewModel.setMediaTypeFilter(if (isPhotoSelected) null else MediaType.PHOTO)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = if (isPhotoSelected) Color.Black else CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Photos ($photoCount)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isPhotoSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPhotoSelected) Color.Black else TextPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    containerColor = DarkSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isPhotoSelected,
                                    borderColor = if (isPhotoSelected) CyanPrimary else DarkSurfaceBorder
                                )
                            )
                        }

                        // Videos Filter
                        if (videoCount > 0) {
                            val isVideoSelected = selectedMediaTypeFilter == MediaType.VIDEO
                            FilterChip(
                                selected = isVideoSelected,
                                onClick = {
                                    viewModel.setMediaTypeFilter(if (isVideoSelected) null else MediaType.VIDEO)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = if (isVideoSelected) Color.Black else CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Videos ($videoCount)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isVideoSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isVideoSelected) Color.Black else TextPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    containerColor = DarkSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isVideoSelected,
                                    borderColor = if (isVideoSelected) CyanPrimary else DarkSurfaceBorder
                                )
                            )
                        }

                        // GIFs Filter
                        if (gifCount > 0) {
                            val isGifSelected = selectedMediaTypeFilter == MediaType.GIF
                            FilterChip(
                                selected = isGifSelected,
                                onClick = {
                                    viewModel.setMediaTypeFilter(if (isGifSelected) null else MediaType.GIF)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = if (isGifSelected) Color.Black else CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "GIFs ($gifCount)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isGifSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isGifSelected) Color.Black else TextPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    containerColor = DarkSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isGifSelected,
                                    borderColor = if (isGifSelected) CyanPrimary else DarkSurfaceBorder
                                )
                            )
                        }

                        // Audio Filter
                        if (audioCount > 0) {
                            val isAudioSelected = selectedMediaTypeFilter == MediaType.AUDIO
                            FilterChip(
                                selected = isAudioSelected,
                                onClick = {
                                    viewModel.setMediaTypeFilter(if (isAudioSelected) null else MediaType.AUDIO)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isAudioSelected) Color.Black else CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Audio ($audioCount)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isAudioSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAudioSelected) Color.Black else TextPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    containerColor = DarkSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isAudioSelected,
                                    borderColor = if (isAudioSelected) CyanPrimary else DarkSurfaceBorder
                                )
                            )
                        }
                    }
                }

                // Grid containing Subfolders and Media (6-column base grid: exactly 2 folders per row with span 3, 3 media items per row with span 2)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    state = gridState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Subfolders section: span 3 means exactly 2 cards per row
                    if (subfolders.isNotEmpty()) {
                        item(key = "header_folders", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "FOLDERS (${subfolders.size})",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(
                            items = subfolders,
                            key = { "folder_${it.folder.uriString}" },
                            span = { GridItemSpan(3) }
                        ) { folderWithCounts ->
                            FolderCard(
                                folder = folderWithCounts.folder,
                                childFolderCount = folderWithCounts.childFolderCount,
                                mediaCount = folderWithCounts.mediaCount,
                                customProfilePath = folderWithCounts.customProfilePath,
                                onClick = { viewModel.openFolder(folderWithCounts.folder) },
                                onLongClick = { folderForDetails = folderWithCounts }
                            )
                        }
                    }

                    // Media items section
                    if (mediaItems.isNotEmpty()) {
                        item(key = "header_media", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "MEDIA (${mediaItems.size})",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(
                            items = mediaItems,
                            key = { "media_${it.uriString}" },
                            span = { GridItemSpan(2) }
                        ) { media ->
                            val isSelected = selectedUris.contains(media.uriString)
                            val isPreviewing = activePreviewVideoUri == media.uriString
                            MediaCard(
                                media = media,
                                thumbnailEngine = viewModel.repository.thumbnailEngine,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                isPreviewing = isPreviewing,
                                onTogglePreview = {
                                    activePreviewVideoUri = if (activePreviewVideoUri == media.uriString) null else media.uriString
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(media.uriString)
                                    } else {
                                        activePreviewVideoUri = null
                                        val index = mediaItems.indexOf(media)
                                        viewModel.openMediaViewer(mediaItems, index)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(media.uriString)
                                }
                            )
                        }
                    }

                    // Empty state in folder
                    if (subfolders.isEmpty() && mediaItems.isEmpty() && !isScanning) {
                        item(key = "empty_state", span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (selectedMediaTypeFilter != null) "No ${selectedMediaTypeFilter?.name?.lowercase()} files found" else "No media files in this folder",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                    if (selectedMediaTypeFilter != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.setMediaTypeFilter(null) }) {
                                            Text("Show All Media", color = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Folder Details & Edit Dialog (triggered on folder card long-press)
        if (folderForDetails != null) {
            FolderDetailsDialog(
                folderWithCounts = folderForDetails!!,
                onChangeProfile = {
                    profileTargetFolderUri = folderForDetails!!.folder.uriString
                    profileTargetFolderName = folderForDetails!!.folder.displayName
                    profileImagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveProfile = {
                    viewModel.removeFolderProfile(folderForDetails!!.folder.uriString)
                    folderForDetails = null
                },
                onSaveName = { newName ->
                    viewModel.updateFolderDisplayName(folderForDetails!!.folder.uriString, newName)
                },
                onOpenFolder = {
                    viewModel.openFolder(folderForDetails!!.folder)
                },
                onDismiss = { folderForDetails = null }
            )
        }

        // Interactive Folder Profile Cropping Dialog
        if (bitmapToCrop != null && profileTargetFolderUri != null) {
            CropImageDialog(
                sourceBitmap = bitmapToCrop!!,
                folderName = profileTargetFolderName,
                onCropConfirmed = { cropped ->
                    viewModel.saveFolderProfile(profileTargetFolderUri!!, cropped)
                    bitmapToCrop = null
                    profileTargetFolderUri = null
                    folderForDetails = null
                },
                onDismiss = {
                    bitmapToCrop = null
                    profileTargetFolderUri = null
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Media", color = TextPrimary) },
                text = {
                    Text(
                        "Are you sure you want to delete ${selectedUris.size} selected file(s)? This action cannot be undone.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteSelected { }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}
