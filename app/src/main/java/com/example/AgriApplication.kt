package com.example

import android.app.Application
import com.example.util.SafeFirebase

class AgriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SafeFirebase.init(this)
    }
}
