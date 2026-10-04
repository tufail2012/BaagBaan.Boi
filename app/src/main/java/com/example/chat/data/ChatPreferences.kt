package com.example.chat.data

import android.content.Context
import android.content.SharedPreferences

class ChatPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    companion object {
        private const val PREFS_NAME = "baagbaan_chat_prefs"
        private const val KEY_NOTIFICATIONS = "chat_notifications_enabled"
    }
}
