package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    private const val APP_ID = "1:965439409911:android:aecb605b6222d36696cdf2"
    private const val API_KEY = "AIzaSyDvCAx1EU-o0XztFDbt7isO44vh-jSqI1Q"
    private const val PROJECT_ID = "kinza-digital-hub"
    private const val STORAGE_BUCKET = "kinza-digital-hub.firebasestorage.app"
    private const val GCM_SENDER_ID = "965439409911"

    @Volatile
    private var isInitialized = false

    fun ensureInitialized(context: Context): Boolean {
        if (isInitialized) return true
        synchronized(this) {
            if (isInitialized) return true
            val appContext = context.applicationContext ?: context
            try {
                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    isInitialized = true
                    Log.d(TAG, "FirebaseApp already initialized.")
                    return true
                }

                // First attempt default auto-init
                try {
                    FirebaseApp.initializeApp(appContext)
                    isInitialized = true
                    Log.d(TAG, "FirebaseApp initialized via default resources.")
                    return true
                } catch (e: Throwable) {
                    Log.w(TAG, "Default FirebaseApp init failed, initializing with explicit FirebaseOptions: ${e.message}")
                }

                // Fallback to explicit options
                val options = FirebaseOptions.Builder()
                    .setApplicationId(APP_ID)
                    .setApiKey(API_KEY)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(GCM_SENDER_ID)
                    .build()

                FirebaseApp.initializeApp(appContext, options)
                isInitialized = true
                Log.d(TAG, "FirebaseApp initialized successfully with explicit FirebaseOptions.")
                return true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize FirebaseApp: ${e.message}", e)
                return false
            }
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        return try {
            ensureInitialized(context)
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Could not get FirebaseAuth instance: ${e.message}")
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            ensureInitialized(context)
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (se: Throwable) {
                // Ignore settings exception if already initialized
            }
            db
        } catch (e: Throwable) {
            Log.e(TAG, "Could not get FirebaseFirestore instance: ${e.message}")
            null
        }
    }
}
