package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseInitializer

class MeterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseInitializer.ensureInitialized(this)
            Log.d("MeterApplication", "Firebase successfully initialized in Application.onCreate")
        } catch (e: Throwable) {
            Log.e("MeterApplication", "Error initializing Firebase in Application.onCreate: ${e.message}", e)
        }
    }
}
