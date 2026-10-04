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

    var lastUid: String?
        get() = prefs.getString(KEY_LAST_UID, null)
        set(value) = prefs.edit().putString(KEY_LAST_UID, value).apply()

    var lastUsername: String?
        get() = prefs.getString(KEY_LAST_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_LAST_USERNAME, value).apply()

    companion object {
        private const val PREFS_NAME = "baagbaan_chat_prefs"
        private const val KEY_NOTIFICATIONS = "chat_notifications_enabled"
        private const val KEY_LAST_UID = "chat_last_uid"
        private const val KEY_LAST_USERNAME = "chat_last_username"
    }
}
