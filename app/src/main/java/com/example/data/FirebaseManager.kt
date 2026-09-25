package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    var isInitialized = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1029384756:android:sihsmartverify")
                    .setProjectId("sih-smartverify-2026")
                    .setApiKey("AIzaSyMockKeyForHackathonLocalBuild1234")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                Log.d(TAG, "FirebaseApp initialized with demo options")
            } else {
                Log.d(TAG, "FirebaseApp was already initialized by Google Services")
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
            isInitialized = false
        }
    }

    val auth: FirebaseAuth?
        get() = try {
            if (isInitialized) FirebaseAuth.getInstance() else null
        } catch (e: Exception) {
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            if (isInitialized) FirebaseFirestore.getInstance() else null
        } catch (e: Exception) {
            null
        }
}
