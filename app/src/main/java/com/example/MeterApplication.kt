package com.example

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import com.example.data.firebase.FirebaseInitializer

class MeterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register early global exception handler for entire application process
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e("MeterApplication", "Uncaught exception in thread ${thread.name}", throwable)
                try {
                    val intent = Intent(this, CrashReportActivity::class.java).apply {
                        putExtra("error_msg", Log.getStackTraceString(throwable))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MeterApplication", "Failed to launch CrashReportActivity", e)
                    defaultHandler?.uncaughtException(thread, throwable)
                }
                Process.killProcess(Process.myPid())
                System.exit(10)
            }
        } catch (e: Exception) {
            Log.e("MeterApplication", "Error installing crash handler", e)
        }

        try {
            FirebaseInitializer.ensureInitialized(this)
            Log.d("MeterApplication", "Firebase successfully initialized in Application.onCreate")
        } catch (e: Throwable) {
            Log.e("MeterApplication", "Error initializing Firebase in Application.onCreate: ${e.message}", e)
        }
    }
}

