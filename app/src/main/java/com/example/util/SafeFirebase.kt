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

        return try {
            val apps = if (ctx != null) FirebaseApp.getApps(ctx) else emptyList()
            if (apps.isNotEmpty()) {
                FirebaseApp.getInstance()
            } else if (ctx != null) {
                try {
                    FirebaseApp.initializeApp(ctx)
                } catch (e: Throwable) {
                    Log.w(TAG, "Default initializeApp failed, trying explicit options: ${e.message}")
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCUqscvq8alYrON5if374wQeUUzAHwzDGI")
                        .setApplicationId("1:858579936461:android:28f0738bf4352fb5d2f584")
                        .setProjectId("baagbaan-boi-20")
                        .setGcmSenderId("858579936461")
                        .setStorageBucket("baagbaan-boi-20.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(ctx, options)
                }
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "ensureFirebaseApp error: ${e.message}", e)
            try {
                FirebaseApp.getInstance()
            } catch (_: Throwable) {
                null
            }
        }
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
