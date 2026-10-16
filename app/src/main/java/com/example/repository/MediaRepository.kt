package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.database.AppDatabase
import com.example.database.FolderEntity
import com.example.database.MediaEntity
import com.example.database.RootEntity
import com.example.filemanager.FileManager
import com.example.model.MediaType
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.scanner.StorageScanner
import com.example.thumbnail.ThumbnailEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val scanner = StorageScanner(context, database)
    val thumbnailEngine = ThumbnailEngine(context)
    val fileManager = FileManager(context, database)

    fun getRoot(): Flow<RootEntity?> = database.rootDao().getRoot()

    suspend fun setRoot(treeUri: Uri, rootName: String) {
        database.rootDao().setRoot(
            RootEntity(
                id = 1,
                treeUriString = treeUri.toString(),
                rootName = rootName,
                lastScannedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRoot() {
        database.rootDao().clearRoot()
        database.folderDao().clearAll()
    }

    fun getSubfolders(parentUri: String): Flow<List<FolderEntity>> {
        return database.folderDao().getSubfolders(parentUri)
    }

    fun getSubfoldersWithCounts(parentUri: String): Flow<List<com.example.database.FolderWithCounts>> {
        return if (parentUri.isBlank()) {
            database.folderDao().getRootSubfoldersWithCounts()
        } else {
            database.folderDao().getSubfoldersWithCounts(parentUri)
        }
    }

    suspend fun setFolderCustomProfile(folderUri: String, imagePath: String) {
        database.folderDao().setFolderProfile(com.example.database.FolderCustomProfile(folderUri, imagePath))
    }

    suspend fun removeFolderCustomProfile(folderUri: String) {
        database.folderDao().removeFolderProfile(folderUri)
    }

    suspend fun updateFolderDisplayName(uriString: String, newName: String) {
        database.folderDao().updateFolderDisplayName(uriString, newName)
    }

    fun getMediaInFolder(
        parentUri: String,
        sortField: SortField = SortField.NAME,
        sortDirection: SortDirection = SortDirection.ASCENDING
    ): Flow<List<MediaEntity>> {
        return database.mediaDao().getMediaInFolder(parentUri).map { list ->
            sortList(list, sortField, sortDirection)
        }
    }

    fun getAllMedia(): Flow<List<MediaEntity>> = database.mediaDao().getAllMedia()

    fun getMediaByType(type: MediaType): Flow<List<MediaEntity>> = database.mediaDao().getMediaByType(type)

    fun getFavoriteMedia(): Flow<List<MediaEntity>> = database.mediaDao().getFavoriteMedia()

    fun getLikedMedia(): Flow<List<MediaEntity>> = database.mediaDao().getLikedMedia()

    fun getRecentMedia(): Flow<List<MediaEntity>> = database.mediaDao().getRecentMedia()

    fun searchMedia(query: String): Flow<List<MediaEntity>> = database.mediaDao().searchMedia(query)

    suspend fun toggleFavorite(media: MediaEntity) {
        database.mediaDao().setFavorite(media.uriString, !media.isFavorite)
    }

    suspend fun toggleLiked(media: MediaEntity) {
        database.mediaDao().setLiked(media.uriString, !media.isLiked)
    }

    suspend fun updatePlaybackPosition(uriString: String, position: Long) {
        database.mediaDao().updatePlaybackPosition(uriString, position)
    }

    suspend fun deleteMedia(uriString: String): Boolean {
        return fileManager.delete(Uri.parse(uriString))
    }

    suspend fun deleteMediaBatch(uriStrings: List<String>): Int {
        var count = 0
        for (uri in uriStrings) {
            if (fileManager.delete(Uri.parse(uri))) count++
        }
        return count
    }

    // Counts for collections
    fun getTotalMediaCount(): Flow<Int> = database.mediaDao().getTotalMediaCount()
    fun getFavoriteCount(): Flow<Int> = database.mediaDao().getFavoriteCount()
    fun getLikedCount(): Flow<Int> = database.mediaDao().getLikedCount()
    fun getRecentCount(): Flow<Int> = database.mediaDao().getRecentCount()
    fun getVideoCount(): Flow<Int> = database.mediaDao().getCountByType(MediaType.VIDEO)
    fun getPhotoCount(): Flow<Int> = database.mediaDao().getCountByType(MediaType.PHOTO)
    fun getGifCount(): Flow<Int> = database.mediaDao().getCountByType(MediaType.GIF)
    fun getAudioCount(): Flow<Int> = database.mediaDao().getCountByType(MediaType.AUDIO)

    private fun sortList(list: List<MediaEntity>, field: SortField, dir: SortDirection): List<MediaEntity> {
        val comparator = when (field) {
            SortField.NAME -> compareBy<MediaEntity> { it.displayName.lowercase() }
            SortField.DATE -> compareBy<MediaEntity> { it.dateModified }
            SortField.SIZE -> compareBy<MediaEntity> { it.size }
        }
        return if (dir == SortDirection.ASCENDING) {
            list.sortedWith(comparator)
        } else {
            list.sortedWith(comparator.reversed())
        }
    }
}
