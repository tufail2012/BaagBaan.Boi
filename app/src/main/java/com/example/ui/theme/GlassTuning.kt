package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp

object GlassTuning {
    const val DEFAULT_PERCENT = 100
    const val MAX_PERCENT = 200

    private const val PREFS_NAME = "liquid_glass_prefs"
    private const val KEY_BLUR = "glass_blur_percent"
    private const val KEY_LENS_HEIGHT = "glass_lens_height_percent"
    private const val KEY_LENS_AMOUNT = "glass_lens_amount_percent"
    private const val KEY_SURFACE = "glass_surface_percent"

    private var initialized = false

    var blurPercent by mutableIntStateOf(DEFAULT_PERCENT)
        private set
    var lensHeightPercent by mutableIntStateOf(DEFAULT_PERCENT)
        private set
    var lensAmountPercent by mutableIntStateOf(DEFAULT_PERCENT)
        private set
    var surfacePercent by mutableIntStateOf(DEFAULT_PERCENT)
        private set

    // Call once from MainActivity.onCreate, next to LiquidGlassPreference.init.
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = prefs(context)
        blurPercent = clamp(prefs.getInt(KEY_BLUR, DEFAULT_PERCENT))
        lensHeightPercent = clamp(prefs.getInt(KEY_LENS_HEIGHT, DEFAULT_PERCENT))
        lensAmountPercent = clamp(prefs.getInt(KEY_LENS_AMOUNT, DEFAULT_PERCENT))
        surfacePercent = clamp(prefs.getInt(KEY_SURFACE, DEFAULT_PERCENT))
    }

    fun setBlurPercent(context: Context, value: Int) {
        blurPercent = clamp(value)
        save(context, KEY_BLUR, blurPercent)
        LiquidGlassSettings.setBlurRadius(context, 3f * blurPercent / 100f)
    }

    fun setLensHeightPercent(context: Context, value: Int) {
        lensHeightPercent = clamp(value)
        save(context, KEY_LENS_HEIGHT, lensHeightPercent)
        LiquidGlassSettings.setLensHeight(context, 0.60f * lensHeightPercent / 100f)
    }

    fun setLensAmountPercent(context: Context, value: Int) {
        lensAmountPercent = clamp(value)
        save(context, KEY_LENS_AMOUNT, lensAmountPercent)
        LiquidGlassSettings.setLensAmount(context, 0.69f * lensAmountPercent / 100f)
    }

    fun setSurfacePercent(context: Context, value: Int) {
        surfacePercent = clamp(value)
        save(context, KEY_SURFACE, surfacePercent)
        LiquidGlassSettings.setSurfaceOpacity(context, (0.5f * surfacePercent / 100f).coerceIn(0f, 1f))
    }

    fun resetAll(context: Context) {
        setBlurPercent(context, DEFAULT_PERCENT)
        setLensHeightPercent(context, DEFAULT_PERCENT)
        setLensAmountPercent(context, DEFAULT_PERCENT)
        setSurfacePercent(context, DEFAULT_PERCENT)
    }

    fun scaleBlur(defaultDp: Float): Float = defaultDp * blurPercent / 100f
    fun scaleBlur(defaultDp: Dp): Dp = defaultDp * (blurPercent / 100f)
    fun scaleLensHeight(default: Float): Float = default * lensHeightPercent / 100f
    fun scaleLensHeight(defaultDp: Dp): Dp = defaultDp * (lensHeightPercent / 100f)
    fun scaleLensAmount(default: Float): Float = default * lensAmountPercent / 100f
    fun scaleLensAmount(defaultDp: Dp): Dp = defaultDp * (lensAmountPercent / 100f)
    fun scaleSurface(default: Float): Float = (default * surfacePercent / 100f).coerceIn(0f, 1f)

    private fun clamp(value: Int): Int = value.coerceIn(0, MAX_PERCENT)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun save(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }
}
