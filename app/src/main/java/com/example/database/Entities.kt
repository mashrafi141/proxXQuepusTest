package com.example.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.MediaType

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["parentUriString"]),
        Index(value = ["mediaType"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isLiked"]),
        Index(value = ["lastViewedTimestamp"]),
        Index(value = ["dateModified"]),
        Index(value = ["displayName"])
    ]
)
data class MediaEntity(
    @PrimaryKey val uriString: String,
    val physicalName: String,
    val displayName: String,
    val parentUriString: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String,
    val mediaType: MediaType,
    val isHidden: Boolean,
    val isObfuscated: Boolean,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false,
    val isLiked: Boolean = false,
    val lastViewedTimestamp: Long? = null,
    val playbackPositionMs: Long = 0L,
    val thumbnailPath: String? = null
)

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentUriString"]),
        Index(value = ["displayName"])
    ]
)
data class FolderEntity(
    @PrimaryKey val uriString: String,
    val physicalName: String,
    val displayName: String,
    val parentUriString: String?,
    val isHidden: Boolean,
    val itemCount: Int = 0,
    val dateModified: Long = 0L
)

@Entity(tableName = "app_root")
data class RootEntity(
    @PrimaryKey val id: Int = 1,
    val treeUriString: String,
    val rootName: String,
    val lastScannedTimestamp: Long = 0L
)

@Entity(tableName = "folder_custom_profiles")
data class FolderCustomProfile(
    @PrimaryKey val folderUriString: String,
    val imagePath: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class FolderWithCounts(
    @Embedded val folder: FolderEntity,
    val childFolderCount: Int = 0,
    val mediaCount: Int = 0,
    val customProfilePath: String? = null
)
