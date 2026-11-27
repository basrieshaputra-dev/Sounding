package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Crash-safe Firebase initializer and accessor.
 * Guarantees FirebaseApp.initializeApp(...) is invoked safely before accessing
 * FirebaseAuth or the project's named FirebaseFirestore database, and returns null
 * instead of throwing if Firebase configuration or play services are unavailable.
 */
object FirebaseSafeProvider {

    private const val TAG = "FirebaseSafeProvider"

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var cachedFirestore: FirebaseFirestore? = null

    fun ensureInitialized(context: Context): Boolean {
        val appContext = context.applicationContext
        if (initialized) {
            val hasApps = runCatching { FirebaseApp.getApps(appContext).isNotEmpty() }.getOrDefault(false)
            if (hasApps) return true
        }
        return synchronized(this) {
            runCatching {
                val existing = FirebaseApp.getApps(appContext)
                if (existing.isEmpty()) {
                    val app = FirebaseApp.initializeApp(appContext)
                    initialized = app != null
                    if (app == null) {
                        Log.w(TAG, "FirebaseApp.initializeApp returned null; running in OFFLINE • LOKAL mode")
                    }
                } else {
                    initialized = true
                }
                initialized
            }.onFailure { error ->
                initialized = false
                Log.e(TAG, "Failed to initialize FirebaseApp; falling back to OFFLINE • LOKAL mode", error)
            }.getOrDefault(false)
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        if (!ensureInitialized(context)) return null
        return runCatching {
            FirebaseAuth.getInstance()
        }.onFailure { error ->
            Log.e(TAG, "Failed to obtain FirebaseAuth instance", error)
        }.getOrNull()
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        cachedFirestore?.let { return it }
        if (!ensureInitialized(context)) return null
        val appContext = context.applicationContext
        return synchronized(this) {
            cachedFirestore ?: runCatching {
                val databaseId = appContext.getString(R.string.firestore_database_id).trim()
                if (databaseId.isEmpty()) {
                    Log.e(TAG, "firestore_database_id resource is empty; Firestore disabled safely")
                    return@runCatching null
                }
                val instance = FirebaseFirestore.getInstance(databaseId)
                runCatching {
                    instance.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(
                            persistentCacheSettings {
                                setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            }
                        )
                    }
                }.onFailure { settingsErr ->
                    Log.w(TAG, "Could not apply persistent Firestore cache settings", settingsErr)
                }
                cachedFirestore = instance
                instance
            }.onFailure { error ->
                Log.e(TAG, "Failed to obtain named FirebaseFirestore instance", error)
            }.getOrNull()
        }
    }
}
