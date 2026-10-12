package com.example.chat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ChatWallpaperStore {
    private const val TAG = "ChatWallpaperStore"
    private const val PREFS = "chat_wallpapers"

    fun get(context: Context, uid: String, chatId: String): File? {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "${uid}_${chatId}"
        val path = sp.getString(key, null) ?: return null
        val file = File(path)
        return if (file.exists() && file.isFile) {
            file
        } else {
            sp.edit().remove(key).apply()
            null
        }
    }

    suspend fun save(context: Context, uid: String, chatId: String, source: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                // 1. Compute inSampleSize to avoid OOM, scaling longest side to ~1600px
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(source)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }

                val width = options.outWidth
                val height = options.outHeight
                if (width <= 0 || height <= 0) {
                    Log.w(TAG, "Failed to decode image bounds for $source")
                    return@withContext null
                }

                val longestSide = maxOf(width, height)
                var sampleSize = 1
                while ((longestSide / sampleSize) > 1600) {
                    sampleSize *= 2
                }

                // 2. Decode sampled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap: Bitmap? = context.contentResolver.openInputStream(source)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
                if (bitmap == null) {
                    Log.w(TAG, "Failed to decode bitmap from $source")
                    return@withContext null
                }

                // 3. Write JPEG (quality 85) to app files directory
                val safeName = "${uid}_${chatId}".replace("[^A-Za-z0-9_-]".toRegex(), "_")
                val dir = File(context.filesDir, "chat_wallpapers")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val targetFile = File(dir, "${safeName}.jpg")

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }
                bitmap.recycle()

                // 4. Store absolute path in SharedPreferences
                val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val key = "${uid}_${chatId}"
                sp.edit().putString(key, targetFile.absolutePath).apply()

                targetFile
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to save chat wallpaper: ${t.message}", t)
                null
            }
        }

    fun clear(context: Context, uid: String, chatId: String) {
        try {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = "${uid}_${chatId}"
            val path = sp.getString(key, null)
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                sp.edit().remove(key).apply()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to clear chat wallpaper: ${t.message}", t)
        }
    }
}
