package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Crash-safe Firebase initializer and accessor.
 * Guarantees FirebaseApp.initializeApp(...) is invoked safely before accessing
 * FirebaseAuth or the project's named FirebaseFirestore database, including a
 * resource-lookup fallback across both applicationId and Gradle namespace packages,
 * and returns null instead of throwing if Firebase configuration is unavailable.
 */
object FirebaseSafeProvider {

    private const val TAG = "FirebaseSafeProvider"
    private const val APP_NAMESPACE = "com.example"

    @Volatile
    private var cachedApp: FirebaseApp? = null

    @Volatile
    private var cachedFirestore: FirebaseFirestore? = null

    fun ensureInitialized(context: Context): Boolean {
        return getOrInitializeApp(context) != null
    }

    private fun getOrInitializeApp(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        cachedApp?.let { existingCached ->
            val stillRegistered = runCatching {
                FirebaseApp.getApps(appContext).isNotEmpty()
            }.getOrDefault(false)
            if (stillRegistered) return existingCached
        }

        return synchronized(this) {
            runCatching {
                val existingApps = FirebaseApp.getApps(appContext)
                if (existingApps.isNotEmpty()) {
                    val resolved = runCatching { FirebaseApp.getInstance() }
                        .getOrElse { existingApps.first() }
                    cachedApp = resolved
                    return@runCatching resolved
                }

                // 1. Standard initialization via Google Services generated resources
                val defaultApp = FirebaseApp.initializeApp(appContext)
                if (defaultApp != null) {
                    cachedApp = defaultApp
                    return@runCatching defaultApp
                }

                // 2. Fallback: explicitly build FirebaseOptions from generated string resources
                // checking both applicationId (appContext.packageName) and Gradle namespace ("com.example")
                val fallbackOptions = buildOptionsFromResources(appContext)
                if (fallbackOptions != null) {
                    val initializedFromResources = FirebaseApp.initializeApp(appContext, fallbackOptions)
                    cachedApp = initializedFromResources
                    Log.i(TAG, "FirebaseApp initialized via explicit resource-based FirebaseOptions")
                    return@runCatching initializedFromResources
                }

                Log.w(
                    TAG,
                    "FirebaseApp.initializeApp returned null (google-services resources missing for package=${appContext.packageName}); running in OFFLINE • LOKAL mode"
                )
                cachedApp = null
                null
            }.onFailure { error ->
                cachedApp = null
                Log.e(TAG, "Failed to initialize FirebaseApp; falling back to OFFLINE • LOKAL mode", error)
            }.getOrNull()
        }
    }

    private fun buildOptionsFromResources(appContext: Context): FirebaseOptions? {
        val appId = resolveStringResource(appContext, "google_app_id")
        val apiKey = resolveStringResource(appContext, "google_api_key")
        val projectId = resolveStringResource(appContext, "project_id")
        val senderId = resolveStringResource(appContext, "gcm_defaultSenderId")
        val storageBucket = resolveStringResource(appContext, "google_storage_bucket")

        if (appId.isNullOrBlank() || apiKey.isNullOrBlank()) {
            Log.w(
                TAG,
                "Google Services string resources unavailable (package=${appContext.packageName}, hasAppId=${!appId.isNullOrBlank()}, hasApiKey=${!apiKey.isNullOrBlank()}, hasProjectId=${!projectId.isNullOrBlank()})"
            )
            return null
        }

        val builder = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setApiKey(apiKey)

        if (!projectId.isNullOrBlank()) {
            builder.setProjectId(projectId)
        }
        if (!senderId.isNullOrBlank()) {
            builder.setGcmSenderId(senderId)
        }
        if (!storageBucket.isNullOrBlank()) {
            builder.setStorageBucket(storageBucket)
        }
        return builder.build()
    }

    private fun resolveStringResource(appContext: Context, resourceName: String): String? {
        val resources = appContext.resources
        val candidatePackages = listOf(appContext.packageName, APP_NAMESPACE).distinct()
        for (pkg in candidatePackages) {
            val resId = runCatching {
                resources.getIdentifier(resourceName, "string", pkg)
            }.getOrDefault(0)
            if (resId != 0) {
                val value = runCatching { resources.getString(resId).trim() }.getOrNull()
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }

    fun getAuth(context: Context): FirebaseAuth? {
        val app = getOrInitializeApp(context) ?: return null
        return runCatching {
            FirebaseAuth.getInstance(app)
        }.onFailure { error ->
            Log.e(TAG, "Failed to obtain FirebaseAuth instance", error)
        }.getOrNull()
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        cachedFirestore?.let { return it }
        val app = getOrInitializeApp(context) ?: return null
        val appContext = context.applicationContext
        return synchronized(this) {
            cachedFirestore ?: runCatching {
                val databaseId = appContext.getString(R.string.firestore_database_id).trim()
                if (databaseId.isEmpty()) {
                    Log.e(TAG, "firestore_database_id resource is empty; Firestore disabled safely")
                    return@runCatching null
                }
                val instance = FirebaseFirestore.getInstance(app, databaseId)
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
