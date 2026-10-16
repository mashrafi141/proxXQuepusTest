package com.example.scanner

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.database.AppDatabase
import com.example.database.FolderEntity
import com.example.database.MediaEntity
import com.example.model.MediaType
import com.example.util.MediaDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class StorageScanner(
    private val context: Context,
    private val database: AppDatabase
) {
    private val contentResolver = context.contentResolver

    /**
     * Enumerate ALL direct children (both normal and dot-prefixed) of a folder.
     * The parentUriString is stored consistently so that:
     * - Direct children of the Root folder have parentUriString = rootTreeUriString
     * - Direct children of a subfolder have parentUriString = subfolderDocUriString
     *
     * No folders or files are skipped based on dot prefix or obfuscated suffix.
     * Unsupported files (e.g. .txt, .pdf) are simply not added as media.
     */
    suspend fun scanSingleFolder(folderUri: Uri, parentUriString: String): FolderScanResult = withContext(Dispatchers.IO) {
        val mediaDao = database.mediaDao()
        val folderDao = database.folderDao()

        val discoveredMedia = mutableListOf<MediaEntity>()
        val discoveredFolders = mutableListOf<FolderEntity>()
        val mediaUris = mutableListOf<String>()
        val folderUris = mutableListOf<String>()

        try {
            // Determine the Document ID for this folder
            val documentId = try {
                if (DocumentsContract.isDocumentUri(context, folderUri)) {
                    DocumentsContract.getDocumentId(folderUri)
                } else {
                    DocumentsContract.getTreeDocumentId(folderUri)
                }
            } catch (e: Exception) {
                DocumentsContract.getTreeDocumentId(folderUri)
            }

            // Build child documents URI
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                documentId
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor != null) {
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val modCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val childDocId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val mime = cursor.getString(mimeCol) ?: ""
                        val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                        val mod = if (cursor.isNull(modCol)) 0L else cursor.getLong(modCol)

                        // Child document URI
                        val childDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, childDocId)
                        val childDocUriStr = childDocUri.toString()

                        val isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val isHidden = name.startsWith(".")

                        if (isDirectory) {
                            // EVERY DIRECTORY IS A NAVIGABLE FOLDER (Normal or Hidden)
                            folderUris.add(childDocUriStr)
                            val cleanName = if (name.startsWith(".")) name.removePrefix(".") else name
                            discoveredFolders.add(
                                FolderEntity(
                                    uriString = childDocUriStr,
                                    physicalName = name,
                                    displayName = cleanName,
                                    parentUriString = parentUriString,
                                    isHidden = isHidden,
                                    dateModified = mod
                                )
                            )
                        } else {
                            // Detect media: works for normal (video.mp4) AND obfuscated (video.mp4.dat)
                            val detection = MediaDetector.detect(name, mime)
                            if (detection.mediaType != MediaType.OTHER) {
                                mediaUris.add(childDocUriStr)
                                discoveredMedia.add(
                                    MediaEntity(
                                        uriString = childDocUriStr,
                                        physicalName = name,
                                        displayName = detection.displayName,
                                        parentUriString = parentUriString,
                                        size = size,
                                        dateModified = mod,
                                        mimeType = mime,
                                        mediaType = detection.mediaType,
                                        isHidden = isHidden,
                                        isObfuscated = detection.isObfuscated
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageScanner", "Error querying children for $folderUri", e)
            } finally {
                cursor?.close()
            }

            // Batch insert direct children into Room
            if (discoveredFolders.isNotEmpty()) {
                folderDao.insertFolders(discoveredFolders)
            }
            if (discoveredMedia.isNotEmpty()) {
                mediaDao.insertMedia(discoveredMedia)
            }

            // Reconcile deleted direct children
            mediaDao.removeDeletedFromFolder(parentUriString, mediaUris)
            folderDao.removeDeletedFromParent(parentUriString, folderUris)

        } catch (e: Exception) {
            Log.e("StorageScanner", "Failed to scan folder $folderUri", e)
        }

        FolderScanResult(discoveredFolders, discoveredMedia)
    }

    /**
     * Recursive background indexing starting from Root.
     * Does NOT block the UI thread.
     * Maintains correct hierarchical parentUriString for each folder.
     */
    suspend fun scanRecursive(rootTreeUri: Uri, onProgress: ((Int, Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        val rootUriStr = rootTreeUri.toString()
        val queue = ArrayDeque<Pair<Uri, String>>()
        queue.add(rootTreeUri to rootUriStr)

        var foldersCount = 0
        var mediaCount = 0

        while (queue.isNotEmpty()) {
            val (currentUri, currentParentUriStr) = queue.poll()
            val result = scanSingleFolder(currentUri, currentParentUriStr)
            foldersCount += result.folders.size
            mediaCount += result.media.size
            onProgress?.invoke(foldersCount, mediaCount)

            for (subfolder in result.folders) {
                try {
                    val subUri = Uri.parse(subfolder.uriString)
                    // The subfolder's children will have parentUriString = subfolder.uriString
                    queue.add(subUri to subfolder.uriString)
                } catch (ignored: Exception) {}
            }
        }
    }

    data class FolderScanResult(
        val folders: List<FolderEntity>,
        val media: List<MediaEntity>
    )
}
