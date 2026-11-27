package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseSafeProvider

/**
 * Custom Application class ensuring FirebaseApp.initializeApp(this) runs safely
 * at process startup before any Activity, ViewModel, or Repository accesses Firebase.
 */
class SoundingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching {
            FirebaseSafeProvider.ensureInitialized(this)
        }.onFailure { error ->
            Log.e("SoundingApplication", "Safe startup initialization encountered an error", error)
        }
    }
}
