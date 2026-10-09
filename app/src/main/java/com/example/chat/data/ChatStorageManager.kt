package com.example.chat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.util.SafeFirebase
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.UUID

object ChatStorageManager {
    private const val TAG = "ChatStorageManager"
    private const val MAX_RETRY_ATTEMPTS = 3

    /**
     * Resolves display name and file size from a content URI.
     */
    fun queryFileDetails(context: Context, uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query file details: ${e.message}")
        }
        return Pair(name, size)
    }

    /**
     * Resolves mime type from content URI or file name extension.
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.let { ext ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            }
            ?: "application/octet-stream"
    }

    /**
     * Uploads a file from content URI to Firebase Storage with progress tracking and automatic retry.
     * Path structure: chats/{chatId}/{mediaType}/{uniqueFileName}
     */
    suspend fun uploadFile(
        chatId: String,
        mediaType: String,
        fileUri: Uri,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val storage = SafeFirebase.getStorage(context)
            ?: return Result.failure(IllegalStateException("Firebase Storage is not available"))

        val mime = getMimeType(context, fileUri)
        val (originalName, _) = queryFileDetails(context, fileUri)
        val ext = originalName.substringAfterLast('.', "").let { if (it.isNotBlank()) ".$it" else "" }
        val uniqueFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}$ext"

        val storagePath = "chats/$chatId/$mediaType/$uniqueFileName"
        val ref = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(mime)
            .setCustomMetadata("originalName", originalName)
            .setCustomMetadata("mediaType", mediaType)
            .build()

        var lastException: Throwable? = null

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            var stream: InputStream? = null
            try {
                Log.d(TAG, "Uploading $mediaType file to $storagePath (Attempt $attempt of $MAX_RETRY_ATTEMPTS)...")
                stream = context.contentResolver.openInputStream(fileUri)
                    ?: throw IllegalStateException("Cannot open input stream for URI: $fileUri")

                val uploadTask = ref.putStream(stream, metadata)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    if (total > 0) {
                        val progress = (taskSnapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }

                uploadTask.await()
                onProgress(1f)

                val downloadUrl = ref.downloadUrl.await().toString()
                Log.i(TAG, "Upload succeeded for $storagePath -> $downloadUrl")
                return Result.success(downloadUrl)
            } catch (e: Throwable) {
                lastException = e
                Log.w(TAG, "Upload attempt $attempt failed for $storagePath: ${e.message}")
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    delay(1000L * attempt) // Exponential backoff
                }
            } finally {
                try {
                    stream?.close()
                } catch (_: Exception) {}
            }
        }

        return Result.failure(lastException ?: IllegalStateException("Upload failed after $MAX_RETRY_ATTEMPTS attempts"))
    }

    /**
     * Uploads in-memory byte array (e.g. voice note audio or captured photo bytes)
     * with progress tracking and automatic retry.
     */
    suspend fun uploadBytes(
        chatId: String,
        mediaType: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
        context: Context? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val storage = SafeFirebase.getStorage(context)
            ?: return Result.failure(IllegalStateException("Firebase Storage is not available"))

        val storagePath = "chats/$chatId/$mediaType/$fileName"
        val ref = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("mediaType", mediaType)
            .build()

        var lastException: Throwable? = null

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            try {
                Log.d(TAG, "Uploading bytes to $storagePath (Attempt $attempt of $MAX_RETRY_ATTEMPTS)...")
                val uploadTask = ref.putBytes(bytes, metadata)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    if (total > 0) {
                        val progress = (taskSnapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }

                uploadTask.await()
                onProgress(1f)

                val downloadUrl = ref.downloadUrl.await().toString()
                Log.i(TAG, "Byte upload succeeded for $storagePath -> $downloadUrl")
                return Result.success(downloadUrl)
            } catch (e: Throwable) {
                lastException = e
                Log.w(TAG, "Byte upload attempt $attempt failed for $storagePath: ${e.message}")
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    delay(1000L * attempt)
                }
            }
        }

        return Result.failure(lastException ?: IllegalStateException("Upload failed after $MAX_RETRY_ATTEMPTS attempts"))
    }
}
