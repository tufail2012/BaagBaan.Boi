package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistence for the post-login runtime permission onboarding flow.
 * Stores whether the onboarding was completed or dismissed so subsequent
 * launches bypass it directly to the dashboard.
 */
class PermissionOnboardingPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean(KEY_COMPLETED, completed)
            .putLong(KEY_COMPLETED_AT, if (completed) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun resetOnboarding() {
        prefs.edit()
            .remove(KEY_COMPLETED)
            .remove(KEY_COMPLETED_AT)
            .apply()
    }

    fun getCompletedTimestamp(): Long {
        return prefs.getLong(KEY_COMPLETED_AT, 0L)
    }

    companion object {
        private const val PREFS_NAME = "AgriCropPermissionOnboardingPrefs"
        private const val KEY_COMPLETED = "permission_onboarding_completed"
        private const val KEY_COMPLETED_AT = "permission_onboarding_completed_at"
    }
}
