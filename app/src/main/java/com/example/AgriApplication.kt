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

    @Volatile
    private var isAppInForeground: Boolean = false

    override fun onCreate() {
        com.example.util.CrashReporter.install(this)
        super.onCreate()
        instance = this
        com.example.util.CrashReporter.checkHistoricalExitReasons(this)
        Log.d("AgriApplication", "Starting SafeFirebase.init(this)...")
        SafeFirebase.init(this)
        val firebaseApp = SafeFirebase.ensureFirebaseApp(this)
        Log.d("AgriApplication", "SafeFirebase.init(this) completed. FirebaseApp instance: $firebaseApp")

        com.example.notifications.NotificationHelper.createNotificationChannels(this)

        // Initialize Zego call invitation service at process start if user session exists
        try {
            val prefs = com.example.chat.data.ChatPreferences(this)
            val currentUid = SafeFirebase.getAuth(this)?.currentUser?.uid
            val lastUsername = prefs.lastUsername
            val willRestore = !currentUid.isNullOrBlank() && !lastUsername.isNullOrBlank()
            com.example.chat.call.ZegoCallManager.trace("T1 app start uid=$currentUid lastUsername=$lastUsername restore=$willRestore")
            if (willRestore) {
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
                    isAppInForeground = true
                    val uid = SafeFirebase.getAuth(this@AgriApplication)?.currentUser?.uid ?: return
                    val prefs = com.example.chat.data.ChatPreferences(this@AgriApplication)
                    if (!prefs.isOnlineStatusEnabled) return
                    presenceScope.launch {
                        com.example.chat.data.ChatRepository(this@AgriApplication).markOnlineIfForeground(uid)
                    }
                }

                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    isAppInForeground = false
                    val uid = SafeFirebase.getAuth(this@AgriApplication)?.currentUser?.uid ?: return
                    presenceScope.launch(kotlinx.coroutines.NonCancellable) {
                        com.example.chat.data.ChatRepository(this@AgriApplication).setOnlinePresence(uid, false)
                    }
                }
            }
        )

        // Keep presence updated if user signs in or auth state finishes restoring while app is already in foreground
        SafeFirebase.getAuth(this)?.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null && isAppInForeground) {
                val prefs = com.example.chat.data.ChatPreferences(this@AgriApplication)
                if (prefs.isOnlineStatusEnabled) {
                    presenceScope.launch {
                        com.example.chat.data.ChatRepository(this@AgriApplication).markOnlineIfForeground(uid)
                    }
                }
            }
        }
    }
}
