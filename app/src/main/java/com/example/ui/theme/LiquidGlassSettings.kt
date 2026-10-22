package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class GlassStyle {
    LIQUID,
    BLUR,
    TRANSPARENT
}

@Immutable
data class LiquidGlassConfig(
    val vibrancy: Float,
    val blurRadius: Float,
    val menuBackdropResolution: Float,
    val lensHeight: Float,
    val lensAmount: Float,
    val chromaticAberration: Boolean,
    val depthEffect: Boolean,
    val surfaceTintColor: Color,
    val surfaceOpacity: Float,
    val glassStyle: GlassStyle,
    val selectionPillColor: Color,
    val selectionPillOpacity: Float,
    val highlightColor: Color,
    val highlightOpacity: Float,
    val adaptiveContrast: Boolean,
    val glassTextColor: Color,
    val navBarGlassEnabled: Boolean,
    val chatGlassEnabled: Boolean
) {
    val normalCaptureScale: Float
        get() = 1f - (blurRadius / 2.5f).coerceIn(0f, 1f) * 0.7f

    val backdropSaturation: Float
        get() = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)

    val lensRefractionHeightDp: Float
        get() = lensHeight * 48f

    val lensRefractionAmountDp: Float
        get() = lensAmount * 48f
}

val LocalLiquidGlassConfig = compositionLocalOf {
    LiquidGlassSettings.resolveConfig(isDark = true)
}

object LiquidGlassSettings {
    const val PREFS_NAME = "liquid_glass_settings"
    private const val LEGACY_PREFS_NAME = "liquid_glass_prefs"
    private const val KEY_MIGRATED_FROM_TUNING = "migrated_from_glass_tuning"

    private const val KEY_VIBRANCY = "vibrancy"
    private const val KEY_BLUR_RADIUS = "blur_radius"
    private const val KEY_MENU_BACKDROP_RESOLUTION = "menu_backdrop_resolution"
    private const val KEY_LENS_HEIGHT = "lens_height"
    private const val KEY_LENS_AMOUNT = "lens_amount"
    private const val KEY_CHROMATIC_ABERRATION = "chromatic_aberration"
    private const val KEY_DEPTH_EFFECT = "depth_effect"
    private const val KEY_SURFACE_TINT_COLOR = "surface_tint_color"
    private const val KEY_SURFACE_OPACITY = "surface_opacity"
    private const val KEY_GLASS_STYLE = "glass_style"
    private const val KEY_SELECTION_PILL_COLOR = "selection_pill_color"
    private const val KEY_SELECTION_PILL_OPACITY = "selection_pill_opacity"
    private const val KEY_HIGHLIGHT_COLOR = "highlight_color"
    private const val KEY_HIGHLIGHT_OPACITY = "highlight_opacity"
    private const val KEY_ADAPTIVE_CONTRAST = "adaptive_contrast"
    private const val KEY_GLASS_TEXT_COLOR = "glass_text_color"
    private const val KEY_NAV_BAR_GLASS_ENABLED = "nav_bar_glass_enabled"
    private const val KEY_CHAT_GLASS_ENABLED = "chat_glass_enabled"

    const val DEFAULT_VIBRANCY = 2.0f
    const val DEFAULT_BLUR_RADIUS = 3.0f
    const val DEFAULT_MENU_BACKDROP_RESOLUTION = 1.0f
    const val DEFAULT_LENS_HEIGHT = 0.60f
    const val DEFAULT_LENS_AMOUNT = 0.69f
    const val DEFAULT_CHROMATIC_ABERRATION = false
    const val DEFAULT_DEPTH_EFFECT = false
    const val DEFAULT_SURFACE_TINT_COLOR = 0
    const val DEFAULT_SURFACE_OPACITY = 0.5f
    val DEFAULT_GLASS_STYLE = GlassStyle.LIQUID
    const val DEFAULT_SELECTION_PILL_COLOR = 0
    const val DEFAULT_SELECTION_PILL_OPACITY = 0.8f
    const val DEFAULT_HIGHLIGHT_COLOR = 0
    const val DEFAULT_HIGHLIGHT_OPACITY = 0.33f
    const val DEFAULT_ADAPTIVE_CONTRAST = true
    const val DEFAULT_GLASS_TEXT_COLOR = 0
    const val DEFAULT_NAV_BAR_GLASS_ENABLED = true
    const val DEFAULT_CHAT_GLASS_ENABLED = true

    const val DARK_SURFACE_TINT_ARGB = 0xFF171717.toInt()
    const val LIGHT_SURFACE_TINT_ARGB = 0xFFFBFBFB.toInt()
    const val DARK_SELECTION_PILL_ARGB = 0xFF2E2E2E.toInt()
    const val LIGHT_SELECTION_PILL_ARGB = 0xFFD1D1D1.toInt()
    const val DARK_HIGHLIGHT_ARGB = 0xFFE8E8E8.toInt()
    const val LIGHT_HIGHLIGHT_ARGB = 0xFFFFFFFF.toInt()
    const val DARK_GLASS_TEXT_ARGB = 0xFFFFFFFF.toInt()
    const val LIGHT_GLASS_TEXT_ARGB = 0xFF1A1A1A.toInt()

    const val DARK_SURFACE_OPACITY_RESET = 0.50f
    const val LIGHT_SURFACE_OPACITY_RESET = 0.40f
    const val DARK_SELECTION_PILL_OPACITY_RESET = 0.80f
    const val LIGHT_SELECTION_PILL_OPACITY_RESET = 0.67f
    const val DARK_HIGHLIGHT_OPACITY_RESET = 0.33f
    const val LIGHT_HIGHLIGHT_OPACITY_RESET = 0.60f

    enum class Field {
        VIBRANCY,
        BLUR_RADIUS,
        MENU_BACKDROP_RESOLUTION,
        LENS_HEIGHT,
        LENS_AMOUNT,
        CHROMATIC_ABERRATION,
        DEPTH_EFFECT,
        SURFACE_TINT_COLOR,
        SURFACE_OPACITY,
        GLASS_STYLE,
        SELECTION_PILL_COLOR,
        SELECTION_PILL_OPACITY,
        HIGHLIGHT_COLOR,
        HIGHLIGHT_OPACITY,
        ADAPTIVE_CONTRAST,
        GLASS_TEXT_COLOR,
        NAV_BAR_GLASS_ENABLED,
        CHAT_GLASS_ENABLED
    }

    private var appContext: Context? = null
    private var initialized = false

    private var _vibrancy by mutableFloatStateOf(DEFAULT_VIBRANCY)
    private var _blurRadius by mutableFloatStateOf(DEFAULT_BLUR_RADIUS)
    private var _menuBackdropResolution by mutableFloatStateOf(DEFAULT_MENU_BACKDROP_RESOLUTION)
    private var _lensHeight by mutableFloatStateOf(DEFAULT_LENS_HEIGHT)
    private var _lensAmount by mutableFloatStateOf(DEFAULT_LENS_AMOUNT)
    private var _chromaticAberration by mutableStateOf(DEFAULT_CHROMATIC_ABERRATION)
    private var _depthEffect by mutableStateOf(DEFAULT_DEPTH_EFFECT)
    private var _surfaceTintColor by mutableIntStateOf(DEFAULT_SURFACE_TINT_COLOR)
    private var _surfaceOpacity by mutableFloatStateOf(DEFAULT_SURFACE_OPACITY)
    private var _glassStyle by mutableStateOf(DEFAULT_GLASS_STYLE)
    private var _selectionPillColor by mutableIntStateOf(DEFAULT_SELECTION_PILL_COLOR)
    private var _selectionPillOpacity by mutableFloatStateOf(DEFAULT_SELECTION_PILL_OPACITY)
    private var _highlightColor by mutableIntStateOf(DEFAULT_HIGHLIGHT_COLOR)
    private var _highlightOpacity by mutableFloatStateOf(DEFAULT_HIGHLIGHT_OPACITY)
    private var _adaptiveContrast by mutableStateOf(DEFAULT_ADAPTIVE_CONTRAST)
    private var _glassTextColor by mutableIntStateOf(DEFAULT_GLASS_TEXT_COLOR)
    private var _navBarGlassEnabled by mutableStateOf(DEFAULT_NAV_BAR_GLASS_ENABLED)
    private var _chatGlassEnabled by mutableStateOf(DEFAULT_CHAT_GLASS_ENABLED)

    var vibrancy: Float
        get() = _vibrancy
        set(value) {
            val clamped = value.coerceIn(0.0f, 2.0f)
            _vibrancy = clamped
            saveFloat(KEY_VIBRANCY, clamped)
        }

    var blurRadius: Float
        get() = _blurRadius
        set(value) {
            val clamped = value.coerceIn(0.0f, 100.0f)
            _blurRadius = clamped
            saveFloat(KEY_BLUR_RADIUS, clamped)
        }

    var menuBackdropResolution: Float
        get() = _menuBackdropResolution
        set(value) {
            val clamped = value.coerceIn(0.3f, 1.0f)
            _menuBackdropResolution = clamped
            saveFloat(KEY_MENU_BACKDROP_RESOLUTION, clamped)
        }

    var lensHeight: Float
        get() = _lensHeight
        set(value) {
            val clamped = value.coerceIn(0.0f, 2.0f)
            _lensHeight = clamped
            saveFloat(KEY_LENS_HEIGHT, clamped)
        }

    var lensAmount: Float
        get() = _lensAmount
        set(value) {
            val clamped = value.coerceIn(0.0f, 2.0f)
            _lensAmount = clamped
            saveFloat(KEY_LENS_AMOUNT, clamped)
        }

    var chromaticAberration: Boolean
        get() = _chromaticAberration
        set(value) {
            _chromaticAberration = value
            saveBoolean(KEY_CHROMATIC_ABERRATION, value)
        }

    var depthEffect: Boolean
        get() = _depthEffect
        set(value) {
            _depthEffect = value
            saveBoolean(KEY_DEPTH_EFFECT, value)
        }

    var surfaceTintColor: Int
        get() = _surfaceTintColor
        set(value) {
            _surfaceTintColor = value
            saveInt(KEY_SURFACE_TINT_COLOR, value)
        }

    var surfaceOpacity: Float
        get() = _surfaceOpacity
        set(value) {
            val clamped = value.coerceIn(0.0f, 1.0f)
            _surfaceOpacity = clamped
            saveFloat(KEY_SURFACE_OPACITY, clamped)
        }

    var glassStyle: GlassStyle
        get() = _glassStyle
        set(value) {
            _glassStyle = value
            saveString(KEY_GLASS_STYLE, value.name)
        }

    var selectionPillColor: Int
        get() = _selectionPillColor
        set(value) {
            _selectionPillColor = value
            saveInt(KEY_SELECTION_PILL_COLOR, value)
        }

    var selectionPillOpacity: Float
        get() = _selectionPillOpacity
        set(value) {
            val clamped = value.coerceIn(0.0f, 1.0f)
            _selectionPillOpacity = clamped
            saveFloat(KEY_SELECTION_PILL_OPACITY, clamped)
        }

    var highlightColor: Int
        get() = _highlightColor
        set(value) {
            _highlightColor = value
            saveInt(KEY_HIGHLIGHT_COLOR, value)
        }

    var highlightOpacity: Float
        get() = _highlightOpacity
        set(value) {
            val clamped = value.coerceIn(0.0f, 1.0f)
            _highlightOpacity = clamped
            saveFloat(KEY_HIGHLIGHT_OPACITY, clamped)
        }

    var adaptiveContrast: Boolean
        get() = _adaptiveContrast
        set(value) {
            _adaptiveContrast = value
            saveBoolean(KEY_ADAPTIVE_CONTRAST, value)
        }

    var glassTextColor: Int
        get() = _glassTextColor
        set(value) {
            _glassTextColor = value
            saveInt(KEY_GLASS_TEXT_COLOR, value)
        }

    var navBarGlassEnabled: Boolean
        get() = _navBarGlassEnabled
        set(value) {
            _navBarGlassEnabled = value
            saveBoolean(KEY_NAV_BAR_GLASS_ENABLED, value)
        }

    var chatGlassEnabled: Boolean
        get() = _chatGlassEnabled
        set(value) {
            _chatGlassEnabled = value
            saveBoolean(KEY_CHAT_GLASS_ENABLED, value)
        }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        initialized = true

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MIGRATED_FROM_TUNING, false)) {
            migrateFromGlassTuning(context, prefs)
        }

        _vibrancy = prefs.getFloat(KEY_VIBRANCY, DEFAULT_VIBRANCY).coerceIn(0.0f, 2.0f)
        _blurRadius = prefs.getFloat(KEY_BLUR_RADIUS, DEFAULT_BLUR_RADIUS).coerceIn(0.0f, 100.0f)
        _menuBackdropResolution = prefs.getFloat(KEY_MENU_BACKDROP_RESOLUTION, DEFAULT_MENU_BACKDROP_RESOLUTION).coerceIn(0.3f, 1.0f)
        _lensHeight = prefs.getFloat(KEY_LENS_HEIGHT, DEFAULT_LENS_HEIGHT).coerceIn(0.0f, 2.0f)
        _lensAmount = prefs.getFloat(KEY_LENS_AMOUNT, DEFAULT_LENS_AMOUNT).coerceIn(0.0f, 2.0f)
        _chromaticAberration = prefs.getBoolean(KEY_CHROMATIC_ABERRATION, DEFAULT_CHROMATIC_ABERRATION)
        _depthEffect = prefs.getBoolean(KEY_DEPTH_EFFECT, DEFAULT_DEPTH_EFFECT)
        _surfaceTintColor = prefs.getInt(KEY_SURFACE_TINT_COLOR, DEFAULT_SURFACE_TINT_COLOR)
        _surfaceOpacity = prefs.getFloat(KEY_SURFACE_OPACITY, DEFAULT_SURFACE_OPACITY).coerceIn(0.0f, 1.0f)
        val styleName = prefs.getString(KEY_GLASS_STYLE, DEFAULT_GLASS_STYLE.name) ?: DEFAULT_GLASS_STYLE.name
        _glassStyle = runCatching { GlassStyle.valueOf(styleName) }.getOrDefault(DEFAULT_GLASS_STYLE)
        _selectionPillColor = prefs.getInt(KEY_SELECTION_PILL_COLOR, DEFAULT_SELECTION_PILL_COLOR)
        _selectionPillOpacity = prefs.getFloat(KEY_SELECTION_PILL_OPACITY, DEFAULT_SELECTION_PILL_OPACITY).coerceIn(0.0f, 1.0f)
        _highlightColor = prefs.getInt(KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR)
        _highlightOpacity = prefs.getFloat(KEY_HIGHLIGHT_OPACITY, DEFAULT_HIGHLIGHT_OPACITY).coerceIn(0.0f, 1.0f)
        _adaptiveContrast = prefs.getBoolean(KEY_ADAPTIVE_CONTRAST, DEFAULT_ADAPTIVE_CONTRAST)
        _glassTextColor = prefs.getInt(KEY_GLASS_TEXT_COLOR, DEFAULT_GLASS_TEXT_COLOR)
        _navBarGlassEnabled = prefs.getBoolean(KEY_NAV_BAR_GLASS_ENABLED, DEFAULT_NAV_BAR_GLASS_ENABLED)
        _chatGlassEnabled = prefs.getBoolean(KEY_CHAT_GLASS_ENABLED, DEFAULT_CHAT_GLASS_ENABLED)
    }

    private fun migrateFromGlassTuning(context: Context, targetPrefs: SharedPreferences) {
        val legacyPrefs = context.applicationContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = targetPrefs.edit()
        if (legacyPrefs.contains("glass_blur_percent")) {
            val percent = legacyPrefs.getInt("glass_blur_percent", 100)
            val migratedBlur = (3f * percent / 100f).coerceIn(0.0f, 100.0f)
            editor.putFloat(KEY_BLUR_RADIUS, migratedBlur)
        }
        if (legacyPrefs.contains("glass_lens_height_percent")) {
            val lensHeightPercent = legacyPrefs.getInt("glass_lens_height_percent", 100)
            val migratedLensHeight = (0.60f * lensHeightPercent / 100f).coerceIn(0.0f, 2.0f)
            editor.putFloat(KEY_LENS_HEIGHT, migratedLensHeight)
        }
        if (legacyPrefs.contains("glass_lens_amount_percent")) {
            val lensAmountPercent = legacyPrefs.getInt("glass_lens_amount_percent", 100)
            val migratedLensAmount = (0.69f * lensAmountPercent / 100f).coerceIn(0.0f, 2.0f)
            editor.putFloat(KEY_LENS_AMOUNT, migratedLensAmount)
        }
        if (legacyPrefs.contains("glass_surface_percent")) {
            val surfacePercent = legacyPrefs.getInt("glass_surface_percent", 100)
            val migratedSurface = (0.5f * surfacePercent / 100f).coerceIn(0.0f, 1.0f)
            editor.putFloat(KEY_SURFACE_OPACITY, migratedSurface)
        }
        editor.putBoolean(KEY_MIGRATED_FROM_TUNING, true).apply()
    }

    fun setVibrancy(context: Context, value: Float) {
        ensureContext(context)
        vibrancy = value
    }

    fun setBlurRadius(context: Context, value: Float) {
        ensureContext(context)
        blurRadius = value
    }

    fun setMenuBackdropResolution(context: Context, value: Float) {
        ensureContext(context)
        menuBackdropResolution = value
    }

    fun setLensHeight(context: Context, value: Float) {
        ensureContext(context)
        lensHeight = value
    }

    fun setLensAmount(context: Context, value: Float) {
        ensureContext(context)
        lensAmount = value
    }

    fun setChromaticAberration(context: Context, value: Boolean) {
        ensureContext(context)
        chromaticAberration = value
    }

    fun setDepthEffect(context: Context, value: Boolean) {
        ensureContext(context)
        depthEffect = value
    }

    fun setSurfaceTintColor(context: Context, value: Int) {
        ensureContext(context)
        surfaceTintColor = value
    }

    fun setSurfaceOpacity(context: Context, value: Float) {
        ensureContext(context)
        surfaceOpacity = value
    }

    fun setGlassStyle(context: Context, value: GlassStyle) {
        ensureContext(context)
        glassStyle = value
    }

    fun setSelectionPillColor(context: Context, value: Int) {
        ensureContext(context)
        selectionPillColor = value
    }

    fun setSelectionPillOpacity(context: Context, value: Float) {
        ensureContext(context)
        selectionPillOpacity = value
    }

    fun setHighlightColor(context: Context, value: Int) {
        ensureContext(context)
        highlightColor = value
    }

    fun setHighlightOpacity(context: Context, value: Float) {
        ensureContext(context)
        highlightOpacity = value
    }

    fun setAdaptiveContrast(context: Context, value: Boolean) {
        ensureContext(context)
        adaptiveContrast = value
    }

    fun setGlassTextColor(context: Context, value: Int) {
        ensureContext(context)
        glassTextColor = value
    }

    fun setNavBarGlassEnabled(context: Context, value: Boolean) {
        ensureContext(context)
        navBarGlassEnabled = value
    }

    fun setChatGlassEnabled(context: Context, value: Boolean) {
        ensureContext(context)
        chatGlassEnabled = value
    }

    fun defaultSurfaceTintColor(isDark: Boolean): Color =
        if (isDark) Color(DARK_SURFACE_TINT_ARGB) else Color(LIGHT_SURFACE_TINT_ARGB)

    fun defaultSelectionPillColor(isDark: Boolean): Color =
        if (isDark) Color(DARK_SELECTION_PILL_ARGB) else Color(LIGHT_SELECTION_PILL_ARGB)

    fun defaultHighlightColor(isDark: Boolean): Color =
        if (isDark) Color(DARK_HIGHLIGHT_ARGB) else Color(LIGHT_HIGHLIGHT_ARGB)

    fun defaultGlassTextColor(isDark: Boolean): Color =
        if (isDark) Color.White else Color(LIGHT_GLASS_TEXT_ARGB)

    fun defaultSurfaceOpacity(isDark: Boolean): Float =
        if (isDark) DARK_SURFACE_OPACITY_RESET else LIGHT_SURFACE_OPACITY_RESET

    fun defaultSelectionPillOpacity(isDark: Boolean): Float =
        if (isDark) DARK_SELECTION_PILL_OPACITY_RESET else LIGHT_SELECTION_PILL_OPACITY_RESET

    fun defaultHighlightOpacity(isDark: Boolean): Float =
        if (isDark) DARK_HIGHLIGHT_OPACITY_RESET else LIGHT_HIGHLIGHT_OPACITY_RESET

    fun reset(field: Field, isDark: Boolean = true, context: Context? = null) {
        if (context != null) ensureContext(context)
        when (field) {
            Field.VIBRANCY -> vibrancy = DEFAULT_VIBRANCY
            Field.BLUR_RADIUS -> blurRadius = DEFAULT_BLUR_RADIUS
            Field.MENU_BACKDROP_RESOLUTION -> menuBackdropResolution = DEFAULT_MENU_BACKDROP_RESOLUTION
            Field.LENS_HEIGHT -> lensHeight = DEFAULT_LENS_HEIGHT
            Field.LENS_AMOUNT -> lensAmount = DEFAULT_LENS_AMOUNT
            Field.CHROMATIC_ABERRATION -> chromaticAberration = DEFAULT_CHROMATIC_ABERRATION
            Field.DEPTH_EFFECT -> depthEffect = DEFAULT_DEPTH_EFFECT
            Field.SURFACE_TINT_COLOR -> surfaceTintColor = DEFAULT_SURFACE_TINT_COLOR
            Field.SURFACE_OPACITY -> surfaceOpacity = defaultSurfaceOpacity(isDark)
            Field.GLASS_STYLE -> glassStyle = DEFAULT_GLASS_STYLE
            Field.SELECTION_PILL_COLOR -> selectionPillColor = DEFAULT_SELECTION_PILL_COLOR
            Field.SELECTION_PILL_OPACITY -> selectionPillOpacity = defaultSelectionPillOpacity(isDark)
            Field.HIGHLIGHT_COLOR -> highlightColor = DEFAULT_HIGHLIGHT_COLOR
            Field.HIGHLIGHT_OPACITY -> highlightOpacity = defaultHighlightOpacity(isDark)
            Field.ADAPTIVE_CONTRAST -> adaptiveContrast = DEFAULT_ADAPTIVE_CONTRAST
            Field.GLASS_TEXT_COLOR -> glassTextColor = DEFAULT_GLASS_TEXT_COLOR
            Field.NAV_BAR_GLASS_ENABLED -> navBarGlassEnabled = DEFAULT_NAV_BAR_GLASS_ENABLED
            Field.CHAT_GLASS_ENABLED -> chatGlassEnabled = DEFAULT_CHAT_GLASS_ENABLED
        }
    }

    fun reset(context: Context, field: Field, isDark: Boolean = true) {
        reset(field = field, isDark = isDark, context = context)
    }

    fun resetAll(isDark: Boolean = true, context: Context? = null) {
        if (context != null) ensureContext(context)
        Field.entries.forEach { field ->
            reset(field = field, isDark = isDark)
        }
    }

    fun resetAll(context: Context, isDark: Boolean = true) {
        resetAll(isDark = isDark, context = context)
    }

    fun resolveConfig(isDark: Boolean): LiquidGlassConfig {
        val resolvedSurfaceTint = if (surfaceTintColor == 0) {
            defaultSurfaceTintColor(isDark)
        } else {
            Color(surfaceTintColor)
        }
        val resolvedSelectionPill = if (selectionPillColor == 0) {
            defaultSelectionPillColor(isDark)
        } else {
            Color(selectionPillColor)
        }
        val resolvedHighlight = if (highlightColor == 0) {
            defaultHighlightColor(isDark)
        } else {
            Color(highlightColor)
        }
        val resolvedGlassText = if (glassTextColor == 0) {
            defaultGlassTextColor(isDark)
        } else {
            Color(glassTextColor)
        }

        return LiquidGlassConfig(
            vibrancy = vibrancy,
            blurRadius = blurRadius,
            menuBackdropResolution = menuBackdropResolution,
            lensHeight = lensHeight,
            lensAmount = lensAmount,
            chromaticAberration = chromaticAberration,
            depthEffect = depthEffect,
            surfaceTintColor = resolvedSurfaceTint,
            surfaceOpacity = surfaceOpacity,
            glassStyle = glassStyle,
            selectionPillColor = resolvedSelectionPill,
            selectionPillOpacity = selectionPillOpacity,
            highlightColor = resolvedHighlight,
            highlightOpacity = highlightOpacity,
            adaptiveContrast = adaptiveContrast,
            glassTextColor = resolvedGlassText,
            navBarGlassEnabled = navBarGlassEnabled,
            chatGlassEnabled = chatGlassEnabled
        )
    }

    @Composable
    fun rememberResolvedConfig(isDark: Boolean): LiquidGlassConfig = resolveConfig(isDark)

    private fun ensureContext(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun prefs(context: Context? = appContext): SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun saveFloat(key: String, value: Float) {
        prefs()?.edit()?.putFloat(key, value)?.apply()
    }

    private fun saveInt(key: String, value: Int) {
        prefs()?.edit()?.putInt(key, value)?.apply()
    }

    private fun saveBoolean(key: String, value: Boolean) {
        prefs()?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun saveString(key: String, value: String) {
        prefs()?.edit()?.putString(key, value)?.apply()
    }
}
