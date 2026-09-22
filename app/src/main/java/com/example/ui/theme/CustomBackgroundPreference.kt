package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Lets the user replace the default ambient background with their own photo,
 * one for light mode and one for dark mode. Images are downsampled and
 * re-saved as JPEG in app-private storage so they survive restarts and don't
 * bloat memory. A version counter (persisted) is bumped on every change so
 * Compose knows to redecode and every screen picks up the new image.
 */
object CustomBackgroundPreference {
    private const val PREFS_NAME = "custom_background_prefs"
    private const val KEY_LIGHT_VERSION = "custom_bg_light_version"
    private const val KEY_DARK_VERSION = "custom_bg_dark_version"
    private const val LIGHT_FILE_NAME = "custom_bg_light.jpg"
    private const val DARK_FILE_NAME = "custom_bg_dark.jpg"
    private const val MAX_DIMENSION = 1600

    private var initialized = false

    var lightVersion by mutableStateOf(0L)
        private set
    var darkVersion by mutableStateOf(0L)
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val p = prefs(context)
        lightVersion = p.getLong(KEY_LIGHT_VERSION, 0L)
        darkVersion = p.getLong(KEY_DARK_VERSION, 0L)
    }

    fun lightFile(context: Context): File = File(context.applicationContext.filesDir, LIGHT_FILE_NAME)
    fun darkFile(context: Context): File = File(context.applicationContext.filesDir, DARK_FILE_NAME)

    fun hasLight(context: Context): Boolean = lightVersion > 0L && lightFile(context).exists()
    fun hasDark(context: Context): Boolean = darkVersion > 0L && darkFile(context).exists()

    /** Returns true on success. Downsamples + re-encodes the picked image before saving. */
    fun setLightImage(context: Context, uri: Uri): Boolean =
        saveImage(context, uri, lightFile(context)) { v ->
            lightVersion = v
            prefs(context).edit().putLong(KEY_LIGHT_VERSION, v).apply()
        }

    fun setDarkImage(context: Context, uri: Uri): Boolean =
        saveImage(context, uri, darkFile(context)) { v ->
            darkVersion = v
            prefs(context).edit().putLong(KEY_DARK_VERSION, v).apply()
        }

    fun clearLight(context: Context) {
        lightFile(context).delete()
        lightVersion = 0L
        prefs(context).edit().putLong(KEY_LIGHT_VERSION, 0L).apply()
    }

    fun clearDark(context: Context) {
        darkFile(context).delete()
        darkVersion = 0L
        prefs(context).edit().putLong(KEY_DARK_VERSION, 0L).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun saveImage(context: Context, uri: Uri, dest: File, onSuccess: (Long) -> Unit): Boolean {
        return try {
            val bitmap = decodeSampled(context, uri, MAX_DIMENSION, MAX_DIMENSION)
            if (bitmap == null) {
                android.util.Log.e("CustomBackground", "decodeSampled returned null for uri=$uri")
                return false
            }
            dest.parentFile?.mkdirs()
            dest.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            bitmap.recycle()
            onSuccess(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            android.util.Log.e("CustomBackground", "Failed to save custom background, uri=$uri", e)
            false
        }
    }

    private fun decodeSampled(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val resolver = context.applicationContext.contentResolver

        // 1. Try file descriptor first (handles seekable streams & avoids re-opening content stream)
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fd = pfd.fileDescriptor
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFileDescriptor(fd, null, boundsOptions)
                if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                    var sample = 1
                    var halfW = boundsOptions.outWidth / 2
                    var halfH = boundsOptions.outHeight / 2
                    while (halfW / sample >= reqWidth && halfH / sample >= reqHeight) sample *= 2
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, decodeOptions)
                    if (bitmap != null) return bitmap
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CustomBackground", "decodeFileDescriptor failed, falling back to stream", e)
        }

        // 2. Fallback to reading bytes once so we don't reopen the content stream
        return try {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            var sample = 1
            if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                var halfW = boundsOptions.outWidth / 2
                var halfH = boundsOptions.outHeight / 2
                while (halfW / sample >= reqWidth && halfH / sample >= reqHeight) sample *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (e: Exception) {
            android.util.Log.e("CustomBackground", "decodeByteArray failed for uri=$uri", e)
            null
        }
    }
}
