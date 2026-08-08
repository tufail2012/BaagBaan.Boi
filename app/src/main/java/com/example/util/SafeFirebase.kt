package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SafeFirebase {
    private const val TAG = "SafeFirebase"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureFirebaseApp()
    }

    private fun ensureFirebaseApp() {
        try {
            val ctx = appContext
            if (ctx != null) {
                if (FirebaseApp.getApps(ctx).isEmpty()) {
                    FirebaseApp.initializeApp(ctx)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseApp initialization failed: ${e.message}")
        }
    }

    val auth: FirebaseAuth?
        get() {
            ensureFirebaseApp()
            return try {
                FirebaseAuth.getInstance()
            } catch (e: Throwable) {
                Log.e(TAG, "FirebaseAuth.getInstance() failed: ${e.message}")
                null
            }
        }

    val db: FirebaseFirestore?
        get() {
            ensureFirebaseApp()
            return try {
                FirebaseFirestore.getInstance()
            } catch (e: Throwable) {
                Log.e(TAG, "FirebaseFirestore.getInstance() failed: ${e.message}")
                null
            }
        }
}
