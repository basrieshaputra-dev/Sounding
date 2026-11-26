package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.CalibrationEngine
import com.example.data.model.ReportTimeSlot
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingCalculation
import com.example.util.SoundingFormatters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class FirestoreSoundingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = createDatabase(context.applicationContext),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna belum masuk ke akun Google.")

    fun currentUserUid(): String? = auth.currentUser?.uid

    fun observeCurrentTankStocks(): Flow<CloudSnapshotResult<List<CurrentTankStockDoc>>> {
        return db.collection(FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS)
            .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CurrentTankStockDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedBy { item ->
                    val idx = ShiftDraftEngine.ALL_STANDARD_TANKS.indexOf(item.tankId)
                    if (idx >= 0) idx else Int.MAX_VALUE
                }
                CloudSnapshotResult(
                    data = list,
                    isFromCache = snapshot.metadata.isFromCache,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
            }
    }

    fun observeSoundingHistory(): Flow<CloudSnapshotResult<List<SoundingHistoryDoc>>> {
        return db.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY)
            .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        SoundingHistoryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.effectiveEpochMs() }
                CloudSnapshotResult(
                    data = list,
                    isFromCache = snapshot.metadata.isFromCache,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
            }
    }

    fun observeShiftReports(): Flow<CloudSnapshotResult<List<ShiftReportCloudDoc>>> {
        return db.collection(FirestorePlantConfig.COLLECTION_SHIFT_REPORTS)
            .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ShiftReportCloudDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedWith(
                    compareByDescending<ShiftReportCloudDoc> { it.dateKey }
                        .thenByDescending { shiftSortOrder(it.shift) }
                        .thenByDescending { it.updatedAtEpochMs }
                )
                CloudSnapshotResult(
                    data = list,
                    isFromCache = snapshot.metadata.isFromCache,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
            }
    }

    suspend fun saveSoundingAndTankStock(
        calc: SoundingCalculation,
        shift: ReportTimeSlot,
        operatorName: String,
        recordedAtEpochMs: Long
    ): Result<String> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val normalizedProduct = CalibrationEngine.normalizeProductName(calc.product).take(100)
        val cleanTankId = calc.tankId.trim().take(24)
        require(cleanTankId.isNotEmpty()) { "Tank ID tidak boleh kosong" }

        val dateKey = FirestorePlantConfig.formatDateKey(recordedAtEpochMs)
        val dateLabel = SoundingFormatters.formatLocalDate(recordedAtEpochMs).take(60)
        val timeLabel = SoundingFormatters.formatLocalTime(recordedAtEpochMs).take(20)
        val cleanOperator = operatorName.trim().ifBlank {
            auth.currentUser?.displayName?.trim().orEmpty()
        }.take(100)
        val cleanNote = calc.note.trim().take(300)
        val safeTonase = round(calc.stockTon * 1000.0) / 1000.0

        val historyRef = db.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY).document()
        val tankDocId = FirestorePlantConfig.tankDocIdFor(cleanTankId)
        val tankStockRef = db.collection(FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS).document(tankDocId)

        val historyPayload = mapOf(
            "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
            "userId" to uid,
            "dateKey" to dateKey,
            "dateLabel" to dateLabel,
            "timeLabel" to timeLabel,
            "shift" to shift.label,
            "operatorName" to cleanOperator,
            "tankId" to cleanTankId,
            "product" to normalizedProduct,
            "soundingCm" to calc.sounding,
            "tableLevelCm" to calc.tableLevel,
            "highLevelCm" to calc.highLevel,
            "temp" to calc.temp,
            "totalVolumeL" to calc.totalVol,
            "density" to calc.density,
            "vcf" to calc.vcf,
            "totalStockKg" to calc.stockKg.coerceAtLeast(0L),
            "tonase" to safeTonase.coerceAtLeast(0.0),
            "manualKg" to calc.isManualKg,
            "note" to cleanNote,
            "recordedAtEpochMs" to recordedAtEpochMs,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val tankStockPayload = mapOf(
            "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
            "tankId" to cleanTankId,
            "product" to normalizedProduct,
            "soundingCm" to calc.sounding,
            "tableLevelCm" to calc.tableLevel,
            "highLevelCm" to calc.highLevel,
            "totalVolumeL" to calc.totalVol,
            "totalStockKg" to calc.stockKg.coerceAtLeast(0L),
            "tonase" to safeTonase.coerceAtLeast(0.0),
            "temp" to calc.temp,
            "density" to calc.density,
            "vcf" to calc.vcf,
            "manualKg" to calc.isManualKg,
            "note" to cleanNote,
            "lastShift" to shift.label,
            "lastOperator" to cleanOperator,
            "dateKey" to dateKey,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(historyRef, historyPayload)
        batch.set(tankStockRef, tankStockPayload)

        return try {
            // Firestore offline persistence immediately commits to local cache and queues cloud sync
            batch.commit().await()
            Result.success(historyRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching {
                handleFirestoreError(
                    error = e,
                    operationType = OperationType.WRITE,
                    path = "${FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY}/${historyRef.id}"
                )
            }
            Result.failure(e)
        }
    }

    suspend fun deleteSoundingHistory(docId: String): Result<Unit> {
        runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val cleanId = docId.trim()
        if (cleanId.isEmpty()) return Result.success(Unit)
        val path = "${FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY}/$cleanId"
        return try {
            db.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY)
                .document(cleanId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.DELETE, path) }
            Result.failure(e)
        }
    }

    suspend fun saveShiftReport(
        draft: ShiftReportDraft,
        dateKey: String
    ): Result<String> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeDateKey = dateKey.trim().ifBlank {
            FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        }.take(20)
        val docId = FirestorePlantConfig.shiftReportDocIdFor(safeDateKey, draft.shift)
        val path = "${FirestorePlantConfig.COLLECTION_SHIFT_REPORTS}/$docId"

        val draftJson = FirestorePlantConfig.serializeShiftDraft(draft).take(59_000)
        val totalAwal = ShiftDraftEngine.totalInitialKg(draft.tankRows).coerceAtLeast(0L)
        val totalAkhir = ShiftDraftEngine.totalFinalKg(draft.tankRows).coerceAtLeast(0L)

        val payload = mapOf(
            "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
            "dateKey" to safeDateKey,
            "dateLabel" to draft.dateLabel.trim().ifBlank { safeDateKey }.take(80),
            "shift" to draft.shift.label,
            "operatorName" to draft.operatorName.trim().ifBlank {
                auth.currentUser?.displayName?.trim().orEmpty()
            }.take(120),
            "nbNote" to draft.nbNote.trim().take(2000),
            "totalAwalKg" to totalAwal,
            "totalAkhirKg" to totalAkhir,
            "transferCount" to draft.transfers.size.toLong().coerceIn(0L, 200L),
            "draftJson" to draftJson,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.collection(FirestorePlantConfig.COLLECTION_SHIFT_REPORTS)
                .document(docId)
                .set(payload)
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    private fun shiftSortOrder(shiftLabel: String): Int = when (shiftLabel) {
        ReportTimeSlot.MALAM.label -> 3
        ReportTimeSlot.SORE.label -> 2
        ReportTimeSlot.PAGI.label -> 1
        else -> 0
    }

    companion object {
        @Volatile
        private var configuredDb: FirebaseFirestore? = null

        fun createDatabase(context: Context): FirebaseFirestore {
            configuredDb?.let { return it }
            return synchronized(this) {
                configuredDb ?: run {
                    val databaseId = context.getString(R.string.firestore_database_id)
                    val instance = FirebaseFirestore.getInstance(databaseId)
                    // Enable Firestore built-in persistent local cache for offline-first tank area operation
                    runCatching {
                        instance.firestoreSettings = firestoreSettings {
                            setLocalCacheSettings(
                                persistentCacheSettings {
                                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                }
                            )
                        }
                    }
                    configuredDb = instance
                    instance
                }
            }
        }
    }
}
