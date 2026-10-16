package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.FolderEntity
import com.example.database.FolderWithCounts
import com.example.database.MediaEntity
import com.example.database.RootEntity
import com.example.model.Breadcrumb
import com.example.model.MediaType
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.repository.MediaRepository
import com.example.security.SecurityManager
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Browse : Screen()
    object Collections : Screen()
    object Settings : Screen()
    data class CollectionView(val title: String, val type: CollectionType) : Screen()
    data class MediaViewer(val currentIndex: Int) : Screen()
    object Search : Screen()
    data class ScrapProfiles(val siteId: String? = null) : Screen()
    data class ScrapProfileMedia(val profileId: String) : Screen()
}

enum class CollectionType {
    ALL,
    VIDEOS,
    PHOTOS,
    GIFS,
    AUDIO,
    LIKED,
    FAVORITES,
    RECENT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = MediaRepository(application, database)
    val securityManager = SecurityManager(application)

    // Current navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Browse)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Screen navigation stack for proper back handling
    private val screenStack = mutableListOf<Screen>()

    // Current folder hierarchy: rootTreeUriString when at root, subfolderUri when inside subfolder
    private val _currentFolderUri = MutableStateFlow<String?>(null)
    val currentFolderUri: StateFlow<String?> = _currentFolderUri.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<Breadcrumb>>(emptyList())
    val breadcrumbs: StateFlow<List<Breadcrumb>> = _breadcrumbs.asStateFlow()

    // Sorting & filtering
    private val _sortField = MutableStateFlow(SortField.NAME)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-selection mode
    private val _selectedMediaUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaUris: StateFlow<Set<String>> = _selectedMediaUris.asStateFlow()

    // Scanning status indicator
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow("")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    // Security lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Root persistence
    val rootEntity: StateFlow<RootEntity?> = repository.getRoot().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // Current folder subfolders: query Room by parentUriString == currentFolderUri
    val currentSubfolders: StateFlow<List<FolderWithCounts>> = _currentFolderUri.flatMapLatest { parentUri ->
        val uri = parentUri ?: rootEntity.value?.treeUriString ?: ""
        repository.getSubfoldersWithCounts(uri)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected media type filter in current folder (null = ALL, or VIDEO, PHOTO, GIF, AUDIO)
    private val _selectedMediaTypeFilter = MutableStateFlow<MediaType?>(null)
    val selectedMediaTypeFilter: StateFlow<MediaType?> = _selectedMediaTypeFilter.asStateFlow()

    fun setMediaTypeFilter(type: MediaType?) {
        _selectedMediaTypeFilter.value = type
    }

    // All media items in current folder before filtering
    val rawCurrentMediaItems: StateFlow<List<MediaEntity>> = combine(
        _currentFolderUri,
        _sortField,
        _sortDirection
    ) { folderUri, sortField, sortDir ->
        Triple(folderUri, sortField, sortDir)
    }.flatMapLatest { (folderUri, sortField, sortDir) ->
        val uri = folderUri ?: rootEntity.value?.treeUriString ?: ""
        repository.getMediaInFolder(uri, sortField, sortDir)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current folder media items filtered by selectedMediaTypeFilter
    val currentMediaItems: StateFlow<List<MediaEntity>> = combine(
        rawCurrentMediaItems,
        _selectedMediaTypeFilter
    ) { list, filter ->
        if (filter == null) list else list.filter { it.mediaType == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveFolderProfile(folderUri: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getApplication<Application>().filesDir, "folder_profiles")
                if (!dir.exists()) dir.mkdirs()
                val safeName = "folder_${folderUri.hashCode().toString().replace("-", "n")}.jpg"
                val destFile = File(dir, safeName)
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                repository.setFolderCustomProfile(folderUri, destFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeFolderProfile(folderUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFolderCustomProfile(folderUri)
        }
    }

    fun updateFolderDisplayName(folderUri: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFolderDisplayName(folderUri, newName)
        }
    }

    // Active media list for viewer
    private val _activeViewerList = MutableStateFlow<List<MediaEntity>>(emptyList())
    val activeViewerList: StateFlow<List<MediaEntity>> = _activeViewerList.asStateFlow()

    // Collections counts
    val totalCount = repository.getTotalMediaCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val videoCount = repository.getVideoCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val photoCount = repository.getPhotoCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val gifCount = repository.getGifCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val audioCount = repository.getAudioCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val favoriteCount = repository.getFavoriteCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val likedCount = repository.getLikedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val recentCount = repository.getRecentCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            securityManager.isLockEnabled.collect { enabled ->
                if (enabled) {
                    _isAppLocked.value = true
                }
            }
        }

        viewModelScope.launch {
            rootEntity.collect { root ->
                if (root != null && _currentFolderUri.value == null) {
                    _currentFolderUri.value = root.treeUriString
                    _breadcrumbs.value = listOf(Breadcrumb(root.rootName, root.treeUriString))
                    startScan(Uri.parse(root.treeUriString))
                }
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun setRootFolder(treeUri: Uri, name: String) {
        viewModelScope.launch {
            repository.setRoot(treeUri, name)
            _currentFolderUri.value = treeUri.toString()
            _breadcrumbs.value = listOf(Breadcrumb(name, treeUri.toString()))
            startScan(treeUri)
        }
    }

    /**
     * Immediate on-demand scan of root direct children first, then background recursive indexing.
     */
    fun startScan(rootTreeUri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Indexing..."
            try {
                // Priority 1: scan direct children of the selected Root folder
                repository.scanner.scanSingleFolder(rootTreeUri, rootTreeUri.toString())

                // Priority 2: background recursive scan of subfolders
                repository.scanner.scanRecursive(rootTreeUri) { folders, media ->
                    _scanProgressText.value = "$folders folders · $media items indexed"
                }
            } catch (ignored: Exception) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun startBackgroundScan(rootTreeUri: Uri) = startScan(rootTreeUri)

    fun openFolder(folder: FolderEntity) {
        val uri = folder.uriString
        _selectedMediaTypeFilter.value = null
        _currentFolderUri.value = uri
        val updatedCrumbs = _breadcrumbs.value.toMutableList()
        updatedCrumbs.add(Breadcrumb(folder.displayName, uri))
        _breadcrumbs.value = updatedCrumbs

        // Immediate single folder fast-scan for on-demand response
        viewModelScope.launch {
            repository.scanner.scanSingleFolder(Uri.parse(uri), uri)
        }
    }

    fun navigateToBreadcrumb(index: Int) {
        val crumbs = _breadcrumbs.value
        if (index in crumbs.indices) {
            _selectedMediaTypeFilter.value = null
            val target = crumbs[index]
            _currentFolderUri.value = target.uriString
            _breadcrumbs.value = crumbs.take(index + 1)
        }
    }

    fun navigateBack(): Boolean {
        if (_selectedMediaUris.value.isNotEmpty()) {
            _selectedMediaUris.value = emptySet()
            return true
        }

        val current = _currentScreen.value
        if (current is Screen.MediaViewer || current is Screen.CollectionView || current is Screen.Search || current is Screen.ScrapProfileMedia || current is Screen.ScrapProfiles) {
            if (screenStack.isNotEmpty()) {
                _currentScreen.value = screenStack.removeAt(screenStack.lastIndex)
            } else {
                _currentScreen.value = Screen.Browse
            }
            return true
        }

        val crumbs = _breadcrumbs.value
        if (crumbs.size > 1) {
            navigateToBreadcrumb(crumbs.size - 2)
            return true
        }

        if (current != Screen.Browse) {
            _currentScreen.value = Screen.Browse
            return true
        }

        return false
    }

    fun setScreen(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun openMediaViewer(items: List<MediaEntity>, startIndex: Int) {
        _activeViewerList.value = items
        setScreen(Screen.MediaViewer(startIndex))
    }

    fun toggleFavorite(media: MediaEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(media)
        }
    }

    fun toggleLiked(media: MediaEntity) {
        viewModelScope.launch {
            repository.toggleLiked(media)
        }
    }

    fun toggleSelection(uriString: String) {
        val current = _selectedMediaUris.value.toMutableSet()
        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            current.add(uriString)
        }
        _selectedMediaUris.value = current
    }

    fun selectAll(mediaList: List<MediaEntity>) {
        _selectedMediaUris.value = mediaList.map { it.uriString }.toSet()
    }

    fun clearSelection() {
        _selectedMediaUris.value = emptySet()
    }

    fun deleteSelected(onComplete: (Int) -> Unit) {
        val toDelete = _selectedMediaUris.value.toList()
        viewModelScope.launch {
            val count = repository.deleteMediaBatch(toDelete)
            _selectedMediaUris.value = emptySet()
            onComplete(count)
        }
    }

    fun savePlaybackPosition(uriString: String, pos: Long) {
        viewModelScope.launch {
            repository.updatePlaybackPosition(uriString, pos)
        }
    }

    fun setSort(field: SortField, dir: SortDirection) {
        _sortField.value = field
        _sortDirection.value = dir
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
