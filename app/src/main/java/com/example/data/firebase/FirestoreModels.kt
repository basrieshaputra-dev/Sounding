package com.example.data.firebase

import androidx.annotation.Keep
import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.data.model.ReportTimeSlot
import com.example.data.model.ShiftReportDraft
import com.example.util.SoundingFormatters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FirestorePlantConfig {
    const val PLANT_UNIT = "FAC-TEXTURIZING"
    const val COLLECTION_CURRENT_TANK_STOCKS = "current_tank_stocks"
    const val COLLECTION_SOUNDING_HISTORY = "sounding_history"
    const val COLLECTION_SHIFT_REPORTS = "shift_reports"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val shiftDraftAdapter = moshi.adapter(ShiftReportDraft::class.java)

    fun tankDocIdFor(tankId: String): String {
        val sanitized = tankId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return sanitized.ifEmpty { "TANK_UNKNOWN" }
    }

    fun shiftReportDocIdFor(dateKey: String, shift: ReportTimeSlot): String {
        val cleanDate = dateKey.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return "${cleanDate}_${shift.name}"
    }

    fun formatDateKey(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(epochMs))
    }

    fun serializeShiftDraft(draft: ShiftReportDraft): String {
        return runCatching { shiftDraftAdapter.toJson(draft) }.getOrNull() ?: "{}"
    }

    fun deserializeShiftDraft(json: String): ShiftReportDraft? {
        if (json.isBlank()) return null
        return runCatching { shiftDraftAdapter.fromJson(json) }.getOrNull()
    }

    fun parseShiftLabel(raw: String): ReportTimeSlot {
        return when (raw.trim().lowercase(Locale.US)) {
            "pagi" -> ReportTimeSlot.PAGI
            "sore" -> ReportTimeSlot.SORE
            "malam" -> ReportTimeSlot.MALAM
            else -> ReportTimeSlot.PAGI
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CurrentTankStockDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val tankId: String = "",
    val product: String = "",
    val soundingCm: Double = 0.0,
    val tableLevelCm: Double = 0.0,
    val highLevelCm: Double = 0.0,
    val totalVolumeL: Double = 0.0,
    val totalStockKg: Long = 0L,
    val tonase: Double = 0.0,
    val temp: Double = 30.0,
    val density: Double = 0.0,
    val vcf: Double = 1.0,
    val manualKg: Boolean = false,
    val note: String = "",
    val lastShift: String = ReportTimeSlot.PAGI.label,
    val lastOperator: String = "",
    val dateKey: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val updatedAtEpochMs: Long
        get() = updatedAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class SoundingHistoryDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val userId: String = "",
    val dateKey: String = "",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val shift: String = ReportTimeSlot.PAGI.label,
    val operatorName: String = "",
    val tankId: String = "",
    val product: String = "",
    val soundingCm: Double = 0.0,
    val tableLevelCm: Double = 0.0,
    val highLevelCm: Double = 0.0,
    val temp: Double = 30.0,
    val totalVolumeL: Double = 0.0,
    val density: Double = 0.0,
    val vcf: Double = 1.0,
    val totalStockKg: Long = 0L,
    val tonase: Double = 0.0,
    val manualKg: Boolean = false,
    val note: String = "",
    val recordedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun effectiveEpochMs(): Long {
        if (recordedAtEpochMs > 0L) return recordedAtEpochMs
        return createdAt?.toDate()?.time ?: 0L
    }

    fun toSoundingLogEntity(): SoundingLogEntity {
        val epoch = effectiveEpochMs()
        val syntheticId = -(id.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
        return SoundingLogEntity(
            id = syntheticId,
            timestampIso = SoundingFormatters.formatIsoUtc(epoch),
            createdAtEpochMs = epoch,
            shift = shift,
            tankId = tankId,
            product = CalibrationEngine.normalizeProductName(product),
            sounding = soundingCm,
            tableLevel = tableLevelCm,
            highLevel = highLevelCm,
            temp = temp,
            totalVol = totalVolumeL,
            density = density,
            vcf = vcf,
            stockKg = totalStockKg,
            stockTon = tonase,
            isManualKg = manualKg,
            note = note
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ShiftReportCloudDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val dateKey: String = "",
    val dateLabel: String = "",
    val shift: String = ReportTimeSlot.PAGI.label,
    val operatorName: String = "",
    val nbNote: String = "",
    val totalAwalKg: Long = 0L,
    val totalAkhirKg: Long = 0L,
    val transferCount: Long = 0L,
    val draftJson: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val updatedAtEpochMs: Long
        get() = updatedAt?.toDate()?.time ?: 0L

    fun toShiftReportDraft(): ShiftReportDraft? {
        return FirestorePlantConfig.deserializeShiftDraft(draftJson)
    }
}

data class CloudSnapshotResult<T>(
    val data: T,
    val isFromCache: Boolean = true,
    val hasPendingWrites: Boolean = false
)

data class HistoryRecordItem(
    val entity: SoundingLogEntity,
    val cloudDocId: String? = null,
    val authorUid: String? = null,
    val operatorName: String = "",
    val shiftLabel: String = "",
    val dateKey: String = "",
    val isCloudSynced: Boolean = false
) {
    val stableKey: String
        get() = cloudDocId?.let { "cloud_$it" } ?: "local_${entity.id}"
}
