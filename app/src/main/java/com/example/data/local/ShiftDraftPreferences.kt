package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.firebase.FirestorePlantConfig
import com.example.data.model.ShiftReportDraft
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ShiftDraftPreferences(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure { err ->
        Log.e(TAG, "Failed opening shift draft SharedPreferences", err)
    }.getOrNull()

    private val adapter by lazy {
        runCatching {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(ShiftReportDraft::class.java)
        }.onFailure { err ->
            Log.e(TAG, "Failed initializing Moshi ShiftReportDraft adapter", err)
        }.getOrNull()
    }

    fun loadDraft(): ShiftReportDraft? {
        val json = runCatching {
            prefs?.getString(KEY_SHIFT_DRAFT_JSON, null)
        }.onFailure { err ->
            Log.w(TAG, "Failed reading legacy shift draft key; clearing corrupted value", err)
            clearDraft()
        }.getOrNull() ?: return null

        if (json.isBlank()) return null

        val parsed = runCatching {
            val raw = adapter?.fromJson(json) ?: return@runCatching null
            FirestorePlantConfig.sanitizeShiftDraft(raw)
        }.onFailure { err ->
            Log.w(TAG, "Failed parsing cached ShiftReportDraft from earlier APK; resetting draft safely", err)
            clearDraft()
        }.getOrNull()

        return parsed
    }

    fun saveDraft(draft: ShiftReportDraft) {
        runCatching {
            val json = adapter?.toJson(draft) ?: return
            prefs?.edit()
                ?.putString(KEY_SHIFT_DRAFT_JSON, json)
                ?.apply()
        }.onFailure { err ->
            Log.w(TAG, "Failed saving ShiftReportDraft locally", err)
        }
    }

    fun clearDraft() {
        runCatching {
            prefs?.edit()
                ?.remove(KEY_SHIFT_DRAFT_JSON)
                ?.apply()
        }.onFailure { err ->
            Log.w(TAG, "Failed clearing ShiftReportDraft preference", err)
        }
    }

    companion object {
        private const val TAG = "ShiftDraftPreferences"
        private const val PREFS_NAME = "sounding_shift_draft_prefs"
        private const val KEY_SHIFT_DRAFT_JSON = "shift_report_draft_v1"
    }
}
