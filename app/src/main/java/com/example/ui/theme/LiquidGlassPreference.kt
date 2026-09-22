package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Master on/off switch for real Liquid Glass rendering across the whole app
 * (nav/header pills, cards, dropdowns, dialogs, per-field boxes). When off,
 * every one of those falls back to its existing flat/Haze look. Backed by
 * SharedPreferences so the choice survives app restarts.
 */
object LiquidGlassPreference {
    private const val PREFS_NAME = "liquid_glass_prefs"
    private const val KEY_ENABLED = "liquid_glass_enabled"
    private var initialized = false

    var enabled by mutableStateOf(true)
        private set

    /** Call once, as early as possible (MainActivity.onCreate). */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, value)
            .apply()
    }
}
