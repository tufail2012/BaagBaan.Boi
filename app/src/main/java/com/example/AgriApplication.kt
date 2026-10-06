package com.example

import android.app.Application
import android.util.Log
import com.example.util.SafeFirebase
import kotlinx.coroutines.launch

class AgriApplication : Application() {
    companion object {
        lateinit var instance: AgriApplication
            private set
        val appContext: android.content.Context
            get() = instance.applicationContext
    }

    private val presenceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    override fun onCreate() {
        com.example.util.CrashReporter.install(this)
        super.onCreate()
        instance = this
        com.example.util.CrashReporter.checkHistoricalExitReasons(this)
        Log.d("AgriApplication", "Starting SafeFirebase.init(this)...")
        SafeFirebase.init(this)
        val firebaseApp = SafeFirebase.ensureFirebaseApp(this)
        Log.d("AgriApplication", "SafeFirebase.init(this) completed. FirebaseApp instance: $firebaseApp")

        // Initialize Zego call invitation service at process start if user session exists
        try {
            val prefs = com.example.chat.data.ChatPreferences(this)
            val currentUid = SafeFirebase.getAuth(this)?.currentUser?.uid
            val lastUsername = prefs.lastUsername
            if (!currentUid.isNullOrBlank() && !lastUsername.isNullOrBlank()) {
                Log.d("AgriApplication", "Restoring Zego Call Invitation for user @$lastUsername ($currentUid)")
                com.example.chat.call.ZegoCallManager.init(this, currentUid, lastUsername)
            }
        } catch (e: Throwable) {
            Log.e("AgriApplication", "Failed to restore Zego Call Service at startup: ${e.message}", e)
        }

        // Track whole-app foreground/background for chat "online" presence
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    val uid = SafeFirebase.getAuth(this@AgriApplication)?.currentUser?.uid ?: return
                    val prefs = com.example.chat.data.ChatPreferences(this@AgriApplication)
                    if (!prefs.isOnlineStatusEnabled) return
                    presenceScope.launch {
                        com.example.chat.data.ChatRepository(this@AgriApplication).setOnlinePresence(uid, true)
                    }
                }

                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    val uid = SafeFirebase.getAuth(this@AgriApplication)?.currentUser?.uid ?: return
                    presenceScope.launch {
                        com.example.chat.data.ChatRepository(this@AgriApplication).setOnlinePresence(uid, false)
                    }
                }
            }
        )
    }
}
