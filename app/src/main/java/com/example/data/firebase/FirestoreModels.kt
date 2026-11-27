package com.example.data.firebase

import androidx.annotation.Keep
import com.example.data.CalibrationEngine
import com.example.data.local.LicenseManager
import com.example.data.local.OperatorIdentityFormatter
import com.example.data.local.SoundingLogEntity
import com.example.data.model.ShiftReportDraft
import com.example.util.SoundingFormatters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
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
    const val COLLECTION_DEVICE_LICENSES = "device_licenses"
    const val COLLECTION_ACTIVATION_CODES = "activation_codes"
    const val COLLECTION_USERS = "users"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val shiftDraftAdapter = moshi.adapter(ShiftReportDraft::class.java)

    fun tankDocIdFor(tankId: String): String {
        val sanitized = tankId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return sanitized.ifEmpty { "TANK_UNKNOWN" }
    }

    fun reportDocIdFor(dateKey: String, accountName: String): String {
        val cleanDate = dateKey.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifEmpty { "DATE" }
        val cleanAccount = OperatorIdentityFormatter.ensureValidAccountName(accountName)
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
        return "${cleanDate}_${cleanAccount}".take(120)
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
        return runCatching {
            val raw = shiftDraftAdapter.fromJson(json) ?: return@runCatching null
            sanitizeShiftDraft(raw)
        }.getOrNull()
    }

    /**
     * Defensively sanitizes a Moshi-deserialized ShiftReportDraft so legacy APK JSON
     * (which may contain unexpected nulls or extra shift fields) never causes NPEs.
     */
    @Suppress("USELESS_ELVIS")
    fun sanitizeShiftDraft(raw: ShiftReportDraft): ShiftReportDraft {
        val safeTransfers = (raw.transfers ?: emptyList()).mapNotNull { item ->
            runCatching {
                item.copy(
                    jam = item.jam ?: "",
                    keterangan = item.keterangan ?: "",
                    qtyKg = item.qtyKg ?: ""
                )
            }.getOrNull()
        }
        val safeRows = (raw.tankRows ?: emptyList()).mapNotNull { row ->
            runCatching {
                val tankId = (row.tankId ?: "").trim()
                if (tankId.isEmpty()) return@mapNotNull null
                val safeGroup = row.group ?: if (tankId in com.example.data.model.ShiftDraftEngine.PREBLEND_TANKS) {
                    com.example.data.model.TankGroup.PREBLEND
                } else {
                    com.example.data.model.TankGroup.TOWERTANK
                }
                val safeAwal = sanitizeSoundingPoint(row.sAwal)
                val safeAkhir = sanitizeSoundingPoint(row.sAkhir)
                row.copy(
                    tankId = tankId,
                    group = safeGroup,
                    oilType = row.oilType ?: "",
                    sAwal = safeAwal,
                    sAkhir = safeAkhir,
                    summaryNote = row.summaryNote ?: "",
                    detailNote = row.detailNote ?: ""
                )
            }.getOrNull()
        }
        return raw.copy(
            dateLabel = raw.dateLabel ?: "",
            timeLabel = raw.timeLabel ?: "",
            operatorName = raw.operatorName ?: "",
            nbNote = raw.nbNote ?: "",
            transfers = safeTransfers,
            tankRows = safeRows
        )
    }

    @Suppress("USELESS_ELVIS")
    private fun sanitizeSoundingPoint(point: com.example.data.model.SoundingPointDraft?): com.example.data.model.SoundingPointDraft {
        if (point == null) return com.example.data.model.SoundingPointDraft()
        return point.copy(
            soundingCm = point.soundingCm ?: "",
            tableLevelCm = point.tableLevelCm ?: "",
            highLevelCm = point.highLevelCm ?: "",
            volumeL = point.volumeL ?: "",
            vcf = point.vcf ?: "",
            density = point.density ?: "",
            tempC = point.tempC ?: "",
            quantityKg = point.quantityKg ?: "",
            timestampLabel = point.timestampLabel ?: ""
        )
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
    val accountName: String = "",
    val operatorName: String = "",
    val dateKey: String = "",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val recordedAtEpochMs: Long = 0L,
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Legacy read fallbacks for earlier cloud documents
    val lastOperator: String = "",
    val operatorId: String = "",
    val operatorUsername: String = ""
) {
    val updatedAtEpochMs: Long
        get() = recordedAtEpochMs.takeIf { it > 0L } ?: updatedAt?.toDate()?.time ?: 0L

    val displayAccountName: String
        get() {
            val raw = accountName.ifBlank {
                operatorName.ifBlank {
                    operatorUsername.ifBlank { operatorId.ifBlank { lastOperator } }
                }
            }
            return OperatorIdentityFormatter.sanitizeUsername(raw).ifBlank { raw.trim() }
        }
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
    val accountName: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    // Legacy read fallbacks for earlier cloud documents
    val operatorId: String = "",
    val operatorUsername: String = ""
) {
    fun effectiveEpochMs(): Long {
        if (recordedAtEpochMs > 0L) return recordedAtEpochMs
        return createdAt?.toDate()?.time ?: 0L
    }

    val displayAccountName: String
        get() {
            val raw = accountName.ifBlank {
                operatorName.ifBlank {
                    operatorUsername.ifBlank { operatorId }
                }
            }
            return OperatorIdentityFormatter.sanitizeUsername(raw).ifBlank { raw.trim() }
        }

    fun toSoundingLogEntity(): SoundingLogEntity {
        val epoch = effectiveEpochMs()
        val syntheticId = -(id.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
        return SoundingLogEntity(
            id = syntheticId,
            timestampIso = SoundingFormatters.formatIsoUtc(epoch),
            createdAtEpochMs = epoch,
            shift = "",
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
    val timeLabel: String = "",
    val accountName: String = "",
    val operatorName: String = "",
    val nbNote: String = "",
    val totalAwalKg: Long = 0L,
    val totalAkhirKg: Long = 0L,
    val transferCount: Long = 0L,
    val draftJson: String = "",
    val recordedAtEpochMs: Long = 0L,
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Legacy read fallbacks for earlier cloud documents
    val operatorId: String = "",
    val operatorUsername: String = ""
) {
    val updatedAtEpochMs: Long
        get() = updatedAt?.toDate()?.time ?: recordedAtEpochMs

    val displayAccountName: String
        get() {
            val raw = accountName.ifBlank {
                operatorName.ifBlank {
                    operatorUsername.ifBlank { operatorId }
                }
            }
            return OperatorIdentityFormatter.sanitizeUsername(raw).ifBlank { raw.trim() }
        }

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
    val accountName: String = "",
    val operatorName: String = "",
    val dateKey: String = "",
    val isCloudSynced: Boolean = false
) {
    val stableKey: String
        get() = cloudDocId?.let { "cloud_$it" } ?: "local_${entity.id}"

    val displayAccountName: String
        get() = accountName.ifBlank { operatorName }
}

@Keep
@IgnoreExtraProperties
data class DeviceLicenseDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val deviceId: String = "",
    val firstInstalledAt: Long = 0L,
    val trialEndsAt: Long = 0L,
    @PropertyName("isActivated")
    @get:PropertyName("isActivated")
    val isActivated: Boolean = false,
    val activationCode: String = "",
    val activatedAtEpochMs: Long = 0L,
    val lastUsername: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Legacy read fallbacks for seamless compatibility with earlier documents
    val firstLaunchEpochMs: Long = 0L,
    val activatedCode: String = "",
    val lastOperatorId: String = ""
) {
    val effectiveFirstInstalledAt: Long
        get() = firstInstalledAt.takeIf { it > 0L } ?: firstLaunchEpochMs

    val effectiveTrialEndsAt: Long
        get() = trialEndsAt.takeIf { it > 0L }
            ?: effectiveFirstInstalledAt.takeIf { it > 0L }?.let { it + LicenseManager.TRIAL_DURATION_MS }
            ?: 0L

    val effectiveActivationCode: String
        get() = activationCode.ifBlank { activatedCode }

    val effectiveLastUsername: String
        get() = lastUsername.ifBlank { lastOperatorId }
}

data class DeviceLicenseSyncResult(
    val doc: DeviceLicenseDoc,
    val revokedOfflineCode: String? = null
)

@Keep
@IgnoreExtraProperties
data class ActivationCodeDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val code: String = "",
    val used: Boolean = false,
    val boundDeviceId: String = "",
    val claimedByUsername: String = "",
    val claimedByUid: String = "",
    val claimedAtEpochMs: Long = 0L,
    @ServerTimestamp val claimedAt: Timestamp? = null,
    // Legacy read fallback
    val claimedByOperatorId: String = ""
) {
    val isClaimed: Boolean
        get() = used || boundDeviceId.isNotBlank()
}

sealed interface CloudCodeClaimResult {
    data class Claimed(val code: String, val boundDeviceId: String) : CloudCodeClaimResult
    data class AlreadyBoundToThisDevice(val code: String, val claimedAtEpochMs: Long) : CloudCodeClaimResult
    data class BoundToAnotherDevice(val code: String) : CloudCodeClaimResult
}

@Keep
@IgnoreExtraProperties
data class UserAccountDoc(
    @DocumentId val id: String = "",
    val plantUnit: String = FirestorePlantConfig.PLANT_UNIT,
    val username: String = "",
    val email: String = "",
    val passwordHash: String = "",
    @get:PropertyName("isVerified")
    @PropertyName("isVerified")
    val isVerified: Boolean = false,
    val uid: String = "",
    val verifiedAtEpochMs: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
