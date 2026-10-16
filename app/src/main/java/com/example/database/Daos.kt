package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items WHERE parentUriString = :parentUri ORDER BY displayName ASC")
    fun getMediaInFolder(parentUri: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE parentUriString = :parentUri")
    suspend fun getMediaInFolderList(parentUri: String): List<MediaEntity>

    @Query("SELECT * FROM media_items WHERE uriString = :uriString LIMIT 1")
    fun getMediaByUri(uriString: String): Flow<MediaEntity?>

    @Query("SELECT * FROM media_items WHERE uriString = :uriString LIMIT 1")
    suspend fun getMediaByUriDirect(uriString: String): MediaEntity?

    @Query("SELECT * FROM media_items ORDER BY dateModified DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateModified DESC")
    fun getMediaByType(type: MediaType): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateModified DESC")
    fun getFavoriteMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isLiked = 1 ORDER BY dateModified DESC")
    fun getLikedMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE lastViewedTimestamp IS NOT NULL ORDER BY lastViewedTimestamp DESC LIMIT 100")
    fun getRecentMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE displayName LIKE '%' || :query || '%' OR physicalName LIKE '%' || :query || '%' ORDER BY dateModified DESC")
    fun searchMedia(query: String): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaEntity)

    @Update
    suspend fun updateMedia(item: MediaEntity)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE uriString = :uriString")
    suspend fun setFavorite(uriString: String, isFavorite: Boolean)

    @Query("UPDATE media_items SET isLiked = :isLiked WHERE uriString = :uriString")
    suspend fun setLiked(uriString: String, isLiked: Boolean)

    @Query("UPDATE media_items SET lastViewedTimestamp = :timestamp, playbackPositionMs = :position WHERE uriString = :uriString")
    suspend fun updatePlaybackPosition(uriString: String, position: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET thumbnailPath = :path WHERE uriString = :uriString")
    suspend fun updateThumbnail(uriString: String, path: String)

    @Query("DELETE FROM media_items WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("DELETE FROM media_items WHERE uriString IN (:uriStrings)")
    suspend fun deleteByUris(uriStrings: List<String>)

    @Query("DELETE FROM media_items WHERE parentUriString = :parentUri AND uriString NOT IN (:currentUris)")
    suspend fun removeDeletedFromFolder(parentUri: String, currentUris: List<String>)

    @Query("SELECT COUNT(*) FROM media_items")
    fun getTotalMediaCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    fun getCountByType(type: MediaType): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE isLiked = 1")
    fun getLikedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE lastViewedTimestamp IS NOT NULL")
    fun getRecentCount(): Flow<Int>
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE parentUriString = :parentUri ORDER BY displayName ASC")
    fun getSubfolders(parentUri: String): Flow<List<FolderEntity>>

    @Query("""
        SELECT f.*, 
            (SELECT COUNT(*) FROM folders cf WHERE cf.parentUriString = f.uriString) AS childFolderCount,
            (SELECT COUNT(*) FROM media_items m WHERE m.parentUriString = f.uriString) AS mediaCount,
            (SELECT p.imagePath FROM folder_custom_profiles p WHERE p.folderUriString = f.uriString LIMIT 1) AS customProfilePath
        FROM folders f 
        WHERE f.parentUriString = :parentUri 
        ORDER BY f.displayName ASC
    """)
    fun getSubfoldersWithCounts(parentUri: String): Flow<List<FolderWithCounts>>

    @Query("""
        SELECT f.*, 
            (SELECT COUNT(*) FROM folders cf WHERE cf.parentUriString = f.uriString) AS childFolderCount,
            (SELECT COUNT(*) FROM media_items m WHERE m.parentUriString = f.uriString) AS mediaCount,
            (SELECT p.imagePath FROM folder_custom_profiles p WHERE p.folderUriString = f.uriString LIMIT 1) AS customProfilePath
        FROM folders f 
        WHERE f.parentUriString IS NULL OR f.parentUriString = '' 
        ORDER BY f.displayName ASC
    """)
    fun getRootSubfoldersWithCounts(): Flow<List<FolderWithCounts>>

    @Query("SELECT * FROM folders WHERE parentUriString = :parentUri")
    suspend fun getSubfoldersList(parentUri: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentUriString IS NULL OR parentUriString = '' ORDER BY displayName ASC")
    fun getRootSubfolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE uriString = :uriString LIMIT 1")
    suspend fun getFolderByUri(uriString: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFolderProfile(profile: FolderCustomProfile)

    @Query("DELETE FROM folder_custom_profiles WHERE folderUriString = :folderUri")
    suspend fun removeFolderProfile(folderUri: String)

    @Query("SELECT imagePath FROM folder_custom_profiles WHERE folderUriString = :folderUri LIMIT 1")
    suspend fun getFolderProfile(folderUri: String): String?

    @Query("UPDATE folders SET displayName = :newName WHERE uriString = :uriString")
    suspend fun updateFolderDisplayName(uriString: String, newName: String)

    @Query("DELETE FROM folders WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("DELETE FROM folders WHERE parentUriString = :parentUri AND uriString NOT IN (:currentUris)")
    suspend fun removeDeletedFromParent(parentUri: String, currentUris: List<String>)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}

@Dao
interface RootDao {

    @Query("SELECT * FROM app_root WHERE id = 1 LIMIT 1")
    fun getRoot(): Flow<RootEntity?>

    @Query("SELECT * FROM app_root WHERE id = 1 LIMIT 1")
    suspend fun getRootDirect(): RootEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRoot(root: RootEntity)

    @Query("DELETE FROM app_root")
    suspend fun clearRoot()
}
