package com.example.filemanager

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class FileManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val contentResolver = context.contentResolver

    suspend fun createFolder(parentUri: Uri, folderName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val docId = DocumentsContract.getTreeDocumentId(parentUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId)
            DocumentsContract.createDocument(
                contentResolver,
                parentDocUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                folderName
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun rename(docUri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.renameDocument(contentResolver, docUri, newName) != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(docUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = DocumentsContract.deleteDocument(contentResolver, docUri)
            if (deleted) {
                database.mediaDao().deleteByUri(docUri.toString())
                database.folderDao().deleteByUri(docUri.toString())
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }

    suspend fun copy(
        sourceDocUri: Uri,
        destinationFolderTreeUri: Uri,
        sourceName: String,
        sourceMime: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val destDocId = DocumentsContract.getTreeDocumentId(destinationFolderTreeUri)
            val destParentDocUri = DocumentsContract.buildDocumentUriUsingTree(destinationFolderTreeUri, destDocId)
            val createdDocUri = DocumentsContract.createDocument(
                contentResolver,
                destParentDocUri,
                sourceMime,
                sourceName
            ) ?: return@withContext null

            contentResolver.openInputStream(sourceDocUri)?.use { input: InputStream ->
                contentResolver.openOutputStream(createdDocUri)?.use { output: OutputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
            createdDocUri
        } catch (e: Exception) {
            null
        }
    }

    suspend fun move(
        sourceDocUri: Uri,
        destinationFolderTreeUri: Uri,
        sourceName: String,
        sourceMime: String
    ): Boolean = withContext(Dispatchers.IO) {
        val copied = copy(sourceDocUri, destinationFolderTreeUri, sourceName, sourceMime)
        if (copied != null) {
            delete(sourceDocUri)
            true
        } else {
            false
        }
    }
}
