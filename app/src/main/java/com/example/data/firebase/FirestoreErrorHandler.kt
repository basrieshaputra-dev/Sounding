package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?
): Nothing {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerArray = JSONArray()
    currentUser?.providerData?.forEach { info ->
        providerArray.put(
            JSONObject().apply {
                put("providerId", info.providerId)
                put("displayName", info.displayName)
                put("email", info.email)
                put("photoUrl", info.photoUrl?.toString())
            }
        )
    }

    val payload = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.value)
        put("path", path)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid)
                put("email", currentUser?.email)
                put("emailVerified", currentUser?.isEmailVerified)
                put("isAnonymous", currentUser?.isAnonymous)
                put("tenantId", currentUser?.tenantId)
                put("providerInfo", providerArray)
            }
        )
    }

    val json = payload.toString()
    Log.e("FirestoreError", json)
    throw IllegalStateException("Firestore Error: $json", error)
}
