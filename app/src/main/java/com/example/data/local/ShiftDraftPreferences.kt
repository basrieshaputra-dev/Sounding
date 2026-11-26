package com.example.data.local

import android.content.Context
import com.example.data.model.ShiftReportDraft
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ShiftDraftPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(ShiftReportDraft::class.java)

    fun loadDraft(): ShiftReportDraft? {
        val json = prefs.getString(KEY_SHIFT_DRAFT_JSON, null) ?: return null
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }

    fun saveDraft(draft: ShiftReportDraft) {
        val json = runCatching { adapter.toJson(draft) }.getOrNull() ?: return
        prefs.edit()
            .putString(KEY_SHIFT_DRAFT_JSON, json)
            .apply()
    }

    fun clearDraft() {
        prefs.edit()
            .remove(KEY_SHIFT_DRAFT_JSON)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "sounding_shift_draft_prefs"
        private const val KEY_SHIFT_DRAFT_JSON = "shift_report_draft_v1"
    }
}
