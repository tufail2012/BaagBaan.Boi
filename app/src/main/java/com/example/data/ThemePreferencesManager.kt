package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferencesManager(private val context: Context) {

    companion object {
        val ACCENT_COLOR_ARGB_KEY = longPreferencesKey("accent_color_argb")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        const val DEFAULT_ACCENT_COLOR_ARGB: Long = 0xFFD32F2FL // Crimson Red default
    }

    val accentColorArgbFlow: Flow<Long> = context.themeDataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_ARGB_KEY] ?: DEFAULT_ACCENT_COLOR_ARGB
    }

    val themeModeFlow: Flow<String> = context.themeDataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "SYSTEM"
    }

    suspend fun setAccentColor(colorArgb: Long) {
        context.themeDataStore.edit { prefs ->
            prefs[ACCENT_COLOR_ARGB_KEY] = colorArgb
        }
    }

    suspend fun setThemeMode(modeName: String) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = modeName
        }
    }
}
