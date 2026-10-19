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

    var isOnlineStatusEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_STATUS, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLINE_STATUS, value).apply()

    var isReadReceiptsEnabled: Boolean
        get() = prefs.getBoolean(KEY_READ_RECEIPTS, true)
        set(value) = prefs.edit().putBoolean(KEY_READ_RECEIPTS, value).apply()

    var pendingFcmToken: String?
        get() = prefs.getString(KEY_PENDING_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_PENDING_FCM_TOKEN, value).apply()

    fun getViewedStatusIds(): Set<String> {
        return prefs.getStringSet(KEY_VIEWED_STATUS_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun markStatusViewed(statusId: String): Set<String> {
        if (statusId.isBlank()) return getViewedStatusIds()
        val updated = (getViewedStatusIds() + statusId)
        prefs.edit().putStringSet(KEY_VIEWED_STATUS_IDS, updated).apply()
        return updated
    }

    companion object {
        private const val PREFS_NAME = "baagbaan_chat_prefs"
        private const val KEY_NOTIFICATIONS = "chat_notifications_enabled"
        private const val KEY_LAST_UID = "chat_last_uid"
        private const val KEY_LAST_USERNAME = "chat_last_username"
        private const val KEY_ONLINE_STATUS = "chat_online_status_enabled"
        private const val KEY_READ_RECEIPTS = "chat_read_receipts_enabled"
        private const val KEY_PENDING_FCM_TOKEN = "chat_pending_fcm_token"
        private const val KEY_VIEWED_STATUS_IDS = "chat_viewed_status_ids"
    }
}
