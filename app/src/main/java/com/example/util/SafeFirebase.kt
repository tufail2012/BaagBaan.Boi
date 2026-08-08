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
        } catch (_: Throwable) {
            // Default app not initialized yet
        }

        // 2. Try standard initializeApp with context
        if (ctx != null) {
            try {
                return FirebaseApp.initializeApp(ctx)
            } catch (e: Throwable) {
                Log.w(TAG, "Default initializeApp failed, trying explicit options: ${e.message}")
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCUqscvq8alYrON5if374wQeUUzAHwzDGI")
                        .setApplicationId("1:858579936461:android:28f0738bf4352fb5d2f584")
                        .setProjectId("baagbaan-boi-20")
                        .setGcmSenderId("858579936461")
                        .setStorageBucket("baagbaan-boi-20.firebasestorage.app")
                        .build()
                    return FirebaseApp.initializeApp(ctx, options)
                } catch (e2: Throwable) {
                    Log.e(TAG, "Explicit initializeApp failed: ${e2.message}", e2)
                }
            }
        }

        return null
    }

    fun getAuth(context: Context? = null): FirebaseAuth? {
        val app = ensureFirebaseApp(context)
        return try {
            if (app != null) {
                FirebaseAuth.getInstance(app)
            } else {
                FirebaseAuth.getInstance()
            }
        } catch (e: Throwable) {
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
