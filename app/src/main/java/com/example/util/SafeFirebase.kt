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

    val trace = mutableListOf<String>()

    fun getTraceString(): String = synchronized(trace) {
        trace.joinToString("\n")
    }

    fun logTrace(msg: String) {
        synchronized(trace) {
            trace.add(msg)
        }
    }

    fun clearTrace() {
        synchronized(trace) {
            trace.clear()
        }
    }

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
            Log.d(TAG, "Found existing default FirebaseApp: ${existingApp.name}")
            return existingApp
        } catch (e: Throwable) {
            // Default instance not created yet, proceed to Step 2
            Log.d(TAG, "No existing default FirebaseApp instance, initializing...")
        }

        // 2. Try standard initializeApp with context
        if (ctx != null) {
            try {
                val app = FirebaseApp.initializeApp(ctx)
                if (app != null) {
                    lastInitError = null
                    Log.d(TAG, "Standard initializeApp succeeded: ${app.name}")
                    return app
                }
            } catch (e: Throwable) {
                lastInitError = e
                val msg2e = "Standard initializeApp failed: ${e.javaClass.simpleName} - ${e.message}"
                Log.w(TAG, msg2e, e)
                logTrace(msg2e)
            }

            // 3. Step 3 fallback (executes if Step 2 returned null OR threw an exception)
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
                Log.d(TAG, "Explicit initializeApp succeeded: ${app.name}")
                return app
            } catch (e2: Throwable) {
                lastInitError = e2
                val msg3e = "Explicit initializeApp failed: ${e2.javaClass.simpleName} - ${e2.message}"
                Log.e(TAG, msg3e, e2)
                logTrace(msg3e)
            }
        } else {
            val msgSkip = "Initialization Skipped: Context is null"
            Log.d(TAG, msgSkip)
            logTrace(msgSkip)
        }

        return null
    }

    fun getAuth(context: Context? = null): FirebaseAuth? {
        val msgInit = "getAuth called with context=${context?.javaClass?.simpleName ?: "null"}"
        Log.d(TAG, msgInit)
        logTrace(msgInit)

        val app = ensureFirebaseApp(context)
        return try {
            val authInstance = if (app != null) {
                val msgA = "getAuth Path A: Getting FirebaseAuth.getInstance(app) with app instance ${app.name}"
                Log.d(TAG, msgA)
                logTrace(msgA)
                FirebaseAuth.getInstance(app)
            } else {
                val msgB = "getAuth Path B: app is null, falling back to raw FirebaseAuth.getInstance()"
                Log.d(TAG, msgB)
                logTrace(msgB)
                FirebaseAuth.getInstance()
            }
            lastAuthError = null
            val msgSuccess = "getAuth Success: FirebaseAuth instance retrieved successfully"
            Log.d(TAG, msgSuccess)
            logTrace(msgSuccess)
            authInstance
        } catch (e: Throwable) {
            lastAuthError = e
            val msgErr = "getAuth Exception: FirebaseAuth.getInstance failed: ${e.javaClass.simpleName} - ${e.message}"
            Log.e(TAG, msgErr, e)
            logTrace(msgErr)
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
