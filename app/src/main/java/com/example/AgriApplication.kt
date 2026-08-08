package com.example

import android.app.Application
import android.util.Log
import com.example.util.SafeFirebase

class AgriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("AgriApplication", "Starting SafeFirebase.init(this)...")
        SafeFirebase.init(this)
        val firebaseApp = SafeFirebase.ensureFirebaseApp(this)
        Log.d("AgriApplication", "SafeFirebase.init(this) completed. FirebaseApp instance: $firebaseApp")
    }
}

