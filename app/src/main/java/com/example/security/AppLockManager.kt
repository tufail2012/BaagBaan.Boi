package com.example.security

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager private constructor(private val context: Context) {
    val preferences = AppLockPreferences(context)

    private val _isLocked = MutableStateFlow(preferences.isAppLockEnabled)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(preferences.isAppLockEnabled)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _currentUnlockMethod = MutableStateFlow(preferences.unlockMethod)
    val currentUnlockMethod: StateFlow<UnlockMethod> = _currentUnlockMethod.asStateFlow()

    private val _lockAfterDuration = MutableStateFlow(preferences.lockAfterDuration)
    val lockAfterDuration: StateFlow<LockAfterDuration> = _lockAfterDuration.asStateFlow()

    private val _hideInRecentApps = MutableStateFlow(preferences.hideContentInRecentApps)
    val hideInRecentApps: StateFlow<Boolean> = _hideInRecentApps.asStateFlow()

    private val _protectNotifications = MutableStateFlow(preferences.protectNotifications)
    val protectNotifications: StateFlow<Boolean> = _protectNotifications.asStateFlow()

    private var lastBackgroundTimeMs: Long = 0L
    private var isAppInBackground: Boolean = false

    companion object {
        @Volatile
        private var instance: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return instance ?: synchronized(this) {
                instance ?: AppLockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        setupProcessLifecycleObserver()
    }

    private fun setupProcessLifecycleObserver() {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    onForeground()
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    onBackground()
                }
            })
        } catch (_: Exception) {
            // Safe fallback if ProcessLifecycleOwner is initialized differently in tests
        }
    }

    fun onBackground() {
        isAppInBackground = true
        lastBackgroundTimeMs = System.currentTimeMillis()
        if (preferences.isAppLockEnabled && preferences.lockAfterDuration == LockAfterDuration.IMMEDIATELY) {
            _isLocked.value = true
        }
    }

    fun onForeground() {
        val wasInBackground = isAppInBackground
        isAppInBackground = false

        if (preferences.isAppLockEnabled) {
            if (_isLocked.value) {
                // Already locked
                return
            }

            if (wasInBackground) {
                val elapsed = System.currentTimeMillis() - lastBackgroundTimeMs
                val timeoutMs = preferences.lockAfterDuration.durationMs
                if (timeoutMs == 0L || elapsed >= timeoutMs) {
                    _isLocked.value = true
                }
            }
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockAppImmediately() {
        if (preferences.isAppLockEnabled) {
            _isLocked.value = true
        }
    }

    fun enableAppLock(method: UnlockMethod, credential: String?) {
        if (!credential.isNullOrBlank()) {
            preferences.saveCredential(credential)
        }
        preferences.unlockMethod = method
        preferences.isAppLockEnabled = true
        _isAppLockEnabled.value = true
        _currentUnlockMethod.value = method
        _isLocked.value = false
    }

    fun disableAppLock() {
        preferences.clearCredential()
        preferences.isAppLockEnabled = false
        _isAppLockEnabled.value = false
        _isLocked.value = false
    }

    fun updateUnlockMethod(method: UnlockMethod, credential: String?) {
        if (!credential.isNullOrBlank()) {
            preferences.saveCredential(credential)
        }
        preferences.unlockMethod = method
        _currentUnlockMethod.value = method
    }

    fun updateLockAfterDuration(duration: LockAfterDuration) {
        preferences.lockAfterDuration = duration
        _lockAfterDuration.value = duration
    }

    fun updateHideInRecentApps(hide: Boolean) {
        preferences.hideContentInRecentApps = hide
        _hideInRecentApps.value = hide
    }

    fun updateProtectNotifications(protect: Boolean) {
        preferences.protectNotifications = protect
        _protectNotifications.value = protect
    }

    fun verifyCredential(input: String): Boolean {
        return preferences.verifyCredential(input)
    }

    fun applySecureWindowFlag(activity: Activity) {
        try {
            // Explicitly clear FLAG_SECURE so screenshots, screen recording, and screen sharing are never restricted or blacked out
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (_: Exception) {
            // Ignore window manipulation errors if activity is detaching
        }
    }
}
