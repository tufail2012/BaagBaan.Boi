package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SafeFirebase {
    private const val TAG = "SafeFirebase"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var lastInitError: Throwable? = null

    @Volatile
    var lastAuthError: Throwable? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureFirebaseApp(appContext)
    }

    fun ensureFirebaseApp(context: Context? = appContext): FirebaseApp? {
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null && appContext == null) {
            appContext = ctx
        }

        // 1. Check if default instance exists already
        try {
            return FirebaseApp.getInstance()
        } catch (e: Throwable) {
            lastInitError = e
        }

        // 2. Try standard initializeApp with context
        if (ctx != null) {
            try {
                val app = FirebaseApp.initializeApp(ctx)
                if (app != null) {
                    lastInitError = null
                    return app
                }
            } catch (e: Throwable) {
                lastInitError = e
                Log.w(TAG, "Default initializeApp failed, trying explicit options: ${e.message}", e)
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCUqscvq8alYrON5if374wQeUUzAHwzDGI")
                        .setApplicationId("1:858579936461:android:28f0738bf4352fb5d2f584")
                        .setProjectId("baagbaan-boi-20")
                        .setGcmSenderId("858579936461")
                        .setStorageBucket("baagbaan-boi-20.firebasestorage.app")
                        .build()
                    val app = FirebaseApp.initializeApp(ctx, options)
                    lastInitError = null
                    return app
                } catch (e2: Throwable) {
                    lastInitError = e2
                    Log.e(TAG, "Explicit initializeApp failed: ${e2.message}", e2)
                }
            }
        }

        return null
    }

    fun getAuth(context: Context? = null): FirebaseAuth? {
        val app = ensureFirebaseApp(context)
        return try {
            val authInstance = if (app != null) {
                FirebaseAuth.getInstance(app)
            } else {
                FirebaseAuth.getInstance()
            }
            lastAuthError = null
            authInstance
        } catch (e: Throwable) {
            lastAuthError = e
            Log.e(TAG, "FirebaseAuth instance failed: ${e.message}", e)
            null
        }
    }

    fun getDb(context: Context? = null): FirebaseFirestore? {
        val app = ensureFirebaseApp(context)
        return try {
            if (app != null) {
                FirebaseFirestore.getInstance(app)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseFirestore instance failed: ${e.message}", e)
            null
        }
    }

    val auth: FirebaseAuth?
        get() = getAuth()

    val db: FirebaseFirestore?
        get() = getDb()
}
