package com.example

import android.app.Application
import android.util.Log
import com.example.util.SafeFirebase

class AgriApplication : Application() {
    companion object {
        lateinit var instance: AgriApplication
            private set
        val appContext: android.content.Context
            get() = instance.applicationContext
    }

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
            if (!currentUid.isNullOrBlank() && !lastUsername.isNullOrBlank() && prefs.lastUid == currentUid) {
                Log.d("AgriApplication", "Restoring Zego Call Invitation for user @$lastUsername ($currentUid)")
                com.example.chat.call.ZegoCallManager.init(this, currentUid, lastUsername)
            }
        } catch (e: Throwable) {
            Log.e("AgriApplication", "Failed to restore Zego Call Service at startup: ${e.message}", e)
        }
    }
}

