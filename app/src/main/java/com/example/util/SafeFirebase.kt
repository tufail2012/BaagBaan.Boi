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
            val existingApp = FirebaseApp.getInstance()
            Log.d(TAG, "Step 1 Success: Found existing default FirebaseApp: ${existingApp.name}")
            return existingApp
        } catch (e: Throwable) {
            lastInitError = e
            Log.d(TAG, "Step 1 Exception: FirebaseApp.getInstance() failed: ${e.javaClass.simpleName} - ${e.message}")
        }

        // 2. Try standard initializeApp with context
        if (ctx != null) {
            try {
                Log.d(TAG, "Step 2 Attempt: Calling FirebaseApp.initializeApp(ctx) with context package ${ctx.packageName}")
                val app = FirebaseApp.initializeApp(ctx)
                if (app != null) {
                    lastInitError = null
                    Log.d(TAG, "Step 2 Success: Standard initializeApp succeeded: ${app.name}")
                    return app
                } else {
                    Log.d(TAG, "Step 2 Result: FirebaseApp.initializeApp(ctx) returned null")
                }
            } catch (e: Throwable) {
                lastInitError = e
                Log.w(TAG, "Step 2 Exception: Standard initializeApp failed: ${e.javaClass.simpleName} - ${e.message}", e)
                try {
                    Log.d(TAG, "Step 3 Attempt: Calling FirebaseApp.initializeApp(ctx, options) with explicit FirebaseOptions")
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCUqscvq8alYrON5if374wQeUUzAHwzDGI")
                        .setApplicationId("1:858579936461:android:28f0738bf4352fb5d2f584")
                        .setProjectId("baagbaan-boi-20")
                        .setGcmSenderId("858579936461")
                        .setStorageBucket("baagbaan-boi-20.firebasestorage.app")
                        .build()
                    val app = FirebaseApp.initializeApp(ctx, options)
                    lastInitError = null
                    Log.d(TAG, "Step 3 Success: Explicit initializeApp succeeded: ${app.name}")
                    return app
                } catch (e2: Throwable) {
                    lastInitError = e2
                    Log.e(TAG, "Step 3 Exception: Explicit initializeApp failed: ${e2.javaClass.simpleName} - ${e2.message}", e2)
                }
            }
        } else {
            Log.d(TAG, "Step 2/3 Skipped: Context is null")
        }

        return null
    }

    fun getAuth(context: Context? = null): FirebaseAuth? {
        Log.d(TAG, "getAuth called with context=${context?.javaClass?.simpleName ?: "null"}")
        val app = ensureFirebaseApp(context)
        return try {
            val authInstance = if (app != null) {
                Log.d(TAG, "getAuth Path A: Getting FirebaseAuth.getInstance(app) with app instance ${app.name}")
                FirebaseAuth.getInstance(app)
            } else {
                Log.d(TAG, "getAuth Path B: app is null, falling back to raw FirebaseAuth.getInstance()")
                FirebaseAuth.getInstance()
            }
            lastAuthError = null
            Log.d(TAG, "getAuth Success: FirebaseAuth instance retrieved successfully")
            authInstance
        } catch (e: Throwable) {
            lastAuthError = e
            Log.e(TAG, "getAuth Exception: FirebaseAuth.getInstance failed: ${e.javaClass.simpleName} - ${e.message}", e)
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
