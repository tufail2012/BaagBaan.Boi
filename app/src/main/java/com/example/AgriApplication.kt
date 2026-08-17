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
        super.onCreate()
        instance = this
        Log.d("AgriApplication", "Starting SafeFirebase.init(this)...")
        SafeFirebase.init(this)
        val firebaseApp = SafeFirebase.ensureFirebaseApp(this)
        Log.d("AgriApplication", "SafeFirebase.init(this) completed. FirebaseApp instance: $firebaseApp")
    }
}

