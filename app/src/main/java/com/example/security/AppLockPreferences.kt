package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class AppLockPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "agri_app_lock_prefs"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_UNLOCK_METHOD = "key_unlock_method"
        private const val KEY_LOCK_AFTER_MS = "key_lock_after_ms"
        private const val KEY_HIDE_IN_RECENTS = "key_hide_in_recents"
        private const val KEY_PROTECT_NOTIFICATIONS = "key_protect_notifications"
        private const val KEY_CREDENTIAL_HASH = "key_credential_hash"
        private const val KEY_DEVICE_SALT = "key_device_salt"
    }

    private fun getOrCreateDeviceSalt(): String {
        var salt = prefs.getString(KEY_DEVICE_SALT, null)
        if (salt.isNullOrBlank()) {
            val random = ByteArray(32)
            SecureRandom().nextBytes(random)
            salt = Base64.encodeToString(random, Base64.NO_WRAP)
            prefs.edit().putString(KEY_DEVICE_SALT, salt).apply()
        }
        return salt
    }

    fun hashCredential(input: String): String {
        val salt = getOrCreateDeviceSalt()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    fun saveCredential(credential: String) {
        val hashed = hashCredential(credential)
        prefs.edit().putString(KEY_CREDENTIAL_HASH, hashed).apply()
    }

    fun verifyCredential(input: String): Boolean {
        val storedHash = prefs.getString(KEY_CREDENTIAL_HASH, null) ?: return false
        val computedHash = hashCredential(input)
        return storedHash == computedHash
    }

    fun clearCredential() {
        prefs.edit().remove(KEY_CREDENTIAL_HASH).apply()
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var unlockMethod: UnlockMethod
        get() {
            val raw = prefs.getString(KEY_UNLOCK_METHOD, UnlockMethod.PIN.name)
            return try {
                UnlockMethod.valueOf(raw ?: UnlockMethod.PIN.name)
            } catch (_: Exception) {
                UnlockMethod.PIN
            }
        }
        set(value) = prefs.edit().putString(KEY_UNLOCK_METHOD, value.name).apply()

    var lockAfterDuration: LockAfterDuration
        get() {
            val ms = prefs.getLong(KEY_LOCK_AFTER_MS, LockAfterDuration.IMMEDIATELY.durationMs)
            return LockAfterDuration.fromDurationMs(ms)
        }
        set(value) = prefs.edit().putLong(KEY_LOCK_AFTER_MS, value.durationMs).apply()

    var hideContentInRecentApps: Boolean
        get() = prefs.getBoolean(KEY_HIDE_IN_RECENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_IN_RECENTS, value).apply()

    var protectNotifications: Boolean
        get() = prefs.getBoolean(KEY_PROTECT_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECT_NOTIFICATIONS, value).apply()

    fun resetAll() {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .remove(KEY_UNLOCK_METHOD)
            .remove(KEY_CREDENTIAL_HASH)
            .apply()
    }
}
