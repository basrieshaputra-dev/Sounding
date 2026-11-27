package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.CalibrationEngine
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseState
import com.example.data.local.OperatorIdentityFormatter
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingCalculation
import com.example.util.SoundingFormatters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class FirestoreSoundingRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?
) {
    constructor(context: Context) : this(
        db = FirebaseSafeProvider.getFirestore(context.applicationContext),
        auth = FirebaseSafeProvider.getAuth(context.applicationContext)
    )

    val isFirebaseReady: Boolean
        get() = db != null && auth != null

    private fun requireDb(): FirebaseFirestore =
        db ?: throw IllegalStateException("Layanan Firestore belum siap di perangkat ini.")

    private fun requireUserId(): String =
        runCatching { auth?.currentUser?.uid?.takeIf { it.isNotBlank() } }.getOrNull()
            ?: throw IllegalStateException("Sesi cloud belum terhubung.")

    fun currentUserUid(): String? =
        runCatching { auth?.currentUser?.uid?.takeIf { it.isNotBlank() } }.getOrNull()

    fun observeCurrentTankStocks(): Flow<CloudSnapshotResult<List<CurrentTankStockDoc>>> = callbackFlow {
        val firestore = db
        if (firestore == null || currentUserUid() == null) {
            trySend(CloudSnapshotResult(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS)
                .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeCurrentTankStocks snapshot error: ${error.message}", error)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val list = runCatching {
                        snapshot.documents.mapNotNull { doc ->
                            runCatching {
                                doc.toObject(
                                    CurrentTankStockDoc::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                            }.onFailure { parseErr ->
                                Log.w(TAG, "Skipping malformed current_tank_stocks doc ${doc.id}", parseErr)
                            }.getOrNull()
                        }.sortedBy { item ->
                            val idx = ShiftDraftEngine.ALL_STANDARD_TANKS.indexOf(item.tankId)
                            if (idx >= 0) idx else Int.MAX_VALUE
                        }
                    }.onFailure { err ->
                        Log.w(TAG, "Failed to map current_tank_stocks snapshot", err)
                    }.getOrDefault(emptyList())

                    trySend(
                        CloudSnapshotResult(
                            data = list,
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register current_tank_stocks listener", e)
            close(e)
        }

        awaitClose {
            runCatching { registration?.remove() }
                .onFailure { Log.w(TAG, "Failed removing current_tank_stocks listener", it) }
        }
    }

    fun observeSoundingHistory(): Flow<CloudSnapshotResult<List<SoundingHistoryDoc>>> = callbackFlow {
        val firestore = db
        if (firestore == null || currentUserUid() == null) {
            trySend(CloudSnapshotResult(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY)
                .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeSoundingHistory snapshot error: ${error.message}", error)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val list = runCatching {
                        snapshot.documents.mapNotNull { doc ->
                            runCatching {
                                doc.toObject(
                                    SoundingHistoryDoc::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                            }.onFailure { parseErr ->
                                Log.w(TAG, "Skipping malformed sounding_history doc ${doc.id}", parseErr)
                            }.getOrNull()
                        }.sortedByDescending { it.effectiveEpochMs() }
                    }.onFailure { err ->
                        Log.w(TAG, "Failed to map sounding_history snapshot", err)
                    }.getOrDefault(emptyList())

                    trySend(
                        CloudSnapshotResult(
                            data = list,
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register sounding_history listener", e)
            close(e)
        }

        awaitClose {
            runCatching { registration?.remove() }
                .onFailure { Log.w(TAG, "Failed removing sounding_history listener", it) }
        }
    }

    fun observeShiftReports(): Flow<CloudSnapshotResult<List<ShiftReportCloudDoc>>> = callbackFlow {
        val firestore = db
        if (firestore == null || currentUserUid() == null) {
            trySend(CloudSnapshotResult(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(FirestorePlantConfig.COLLECTION_SHIFT_REPORTS)
                .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeShiftReports snapshot error: ${error.message}", error)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val list = runCatching {
                        snapshot.documents.mapNotNull { doc ->
                            runCatching {
                                doc.toObject(
                                    ShiftReportCloudDoc::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                            }.onFailure { parseErr ->
                                Log.w(TAG, "Skipping malformed shift_reports doc ${doc.id}", parseErr)
                            }.getOrNull()
                        }.sortedWith(
                            compareByDescending<ShiftReportCloudDoc> { it.updatedAtEpochMs }
                                .thenByDescending { it.dateKey }
                        )
                    }.onFailure { err ->
                        Log.w(TAG, "Failed to map shift_reports snapshot", err)
                    }.getOrDefault(emptyList())

                    trySend(
                        CloudSnapshotResult(
                            data = list,
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register shift_reports listener", e)
            close(e)
        }

        awaitClose {
            runCatching { registration?.remove() }
                .onFailure { Log.w(TAG, "Failed removing shift_reports listener", it) }
        }
    }

    suspend fun saveSoundingAndTankStock(
        calc: SoundingCalculation,
        accountName: String,
        operatorName: String,
        recordedAtEpochMs: Long
    ): Result<String> {
        var path = FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY
        return try {
            val firestore = requireDb()
            val uid = requireUserId()
            val normalizedProduct = CalibrationEngine.normalizeProductName(calc.product).take(100)
            val cleanTankId = calc.tankId.trim().take(24)
            if (cleanTankId.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tank ID tidak boleh kosong"))
            }

            val dateKey = FirestorePlantConfig.formatDateKey(recordedAtEpochMs)
            val dateLabel = SoundingFormatters.formatLocalDate(recordedAtEpochMs).take(60)
            val timeLabel = SoundingFormatters.formatLocalTime(recordedAtEpochMs).take(20)
            val normalizedAccount = OperatorIdentityFormatter.ensureValidAccountName(
                accountName.ifBlank { operatorName }
            )
            val cleanOperator = operatorName.trim().ifBlank { normalizedAccount }.take(100)
            val cleanNote = calc.note.trim().take(300)
            val safeTonase = round(calc.stockTon * 1000.0) / 1000.0

            val historyRef = firestore.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY).document()
            path = "${FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY}/${historyRef.id}"
            val tankDocId = FirestorePlantConfig.tankDocIdFor(cleanTankId)
            val tankStockRef = firestore.collection(FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS).document(tankDocId)

            val historyPayload = mapOf(
                "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                "userId" to uid,
                "dateKey" to dateKey,
                "dateLabel" to dateLabel,
                "timeLabel" to timeLabel,
                "accountName" to normalizedAccount,
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
                "accountName" to normalizedAccount,
                "operatorName" to cleanOperator,
                "dateKey" to dateKey,
                "dateLabel" to dateLabel,
                "timeLabel" to timeLabel,
                "recordedAtEpochMs" to recordedAtEpochMs,
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = firestore.batch()
            batch.set(historyRef, historyPayload)
            batch.set(tankStockRef, tankStockPayload)

            batch.commit()
                .addOnFailureListener { err ->
                    Log.e(TAG, "Batch write failed for $path: ${err.message}", err)
                }
                .await()
            Result.success(historyRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    suspend fun deleteSoundingHistory(docId: String): Result<Unit> {
        val cleanId = docId.trim()
        if (cleanId.isEmpty()) return Result.success(Unit)
        val path = "${FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY}/$cleanId"
        return try {
            val firestore = requireDb()
            requireUserId()
            firestore.collection(FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY)
                .document(cleanId)
                .delete()
                .addOnFailureListener { err ->
                    Log.e(TAG, "Delete failed for $path: ${err.message}", err)
                }
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
        dateKey: String,
        accountName: String,
        recordedAtEpochMs: Long = System.currentTimeMillis()
    ): Result<String> {
        var path = FirestorePlantConfig.COLLECTION_SHIFT_REPORTS
        return try {
            val firestore = requireDb()
            val uid = requireUserId()
            val safeDateKey = dateKey.trim().ifBlank {
                FirestorePlantConfig.formatDateKey(recordedAtEpochMs)
            }.take(20)
            val normalizedAccount = OperatorIdentityFormatter.ensureValidAccountName(
                accountName.ifBlank { draft.operatorName }
            )
            val docId = FirestorePlantConfig.reportDocIdFor(safeDateKey, normalizedAccount)
            path = "${FirestorePlantConfig.COLLECTION_SHIFT_REPORTS}/$docId"

            val timeLabel = SoundingFormatters.formatLocalTime(recordedAtEpochMs).take(20)
            val draftWithTime = draft.copy(
                timeLabel = draft.timeLabel.ifBlank { timeLabel },
                operatorName = draft.operatorName.ifBlank { normalizedAccount }
            )
            val draftJson = FirestorePlantConfig.serializeShiftDraft(draftWithTime).take(59_000)
            val totalAwal = ShiftDraftEngine.totalInitialKg(draft.tankRows).coerceAtLeast(0L)
            val totalAkhir = ShiftDraftEngine.totalFinalKg(draft.tankRows).coerceAtLeast(0L)

            val payload = mapOf(
                "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                "dateKey" to safeDateKey,
                "dateLabel" to draft.dateLabel.trim().ifBlank { safeDateKey }.take(80),
                "timeLabel" to timeLabel,
                "accountName" to normalizedAccount,
                "operatorName" to draft.operatorName.trim().ifBlank { normalizedAccount }.take(120),
                "nbNote" to draft.nbNote.trim().take(2000),
                "totalAwalKg" to totalAwal,
                "totalAkhirKg" to totalAkhir,
                "transferCount" to draft.transfers.size.toLong().coerceIn(0L, 200L),
                "draftJson" to draftJson,
                "recordedAtEpochMs" to recordedAtEpochMs,
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestorePlantConfig.COLLECTION_SHIFT_REPORTS)
                .document(docId)
                .set(payload)
                .addOnFailureListener { err ->
                    Log.e(TAG, "Save shift report failed for $path: ${err.message}", err)
                }
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    suspend fun syncDeviceLicense(
        deviceId: String,
        localState: LicenseState,
        activeUsername: String
    ): Result<DeviceLicenseSyncResult> {
        var path = FirestorePlantConfig.COLLECTION_DEVICE_LICENSES
        return try {
            val firestore = requireDb()
            val uid = requireUserId()
            val cleanDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120)
            if (cleanDeviceId.isEmpty()) {
                return Result.failure(IllegalArgumentException("Device ID kosong"))
            }

            val safeUsername = if (activeUsername.isBlank()) {
                ""
            } else {
                OperatorIdentityFormatter.ensureValidAccountName(activeUsername)
            }
            val docRef = firestore.collection(FirestorePlantConfig.COLLECTION_DEVICE_LICENSES).document(cleanDeviceId)
            path = "${FirestorePlantConfig.COLLECTION_DEVICE_LICENSES}/$cleanDeviceId"

            val localNormalizedCode = LicenseManager.normalizeCode(localState.activatedCode)
            val hasLocalCandidateCode = localState.isActivated && localNormalizedCode in LicenseManager.VALID_ACTIVATION_CODES
            val candidateCodeRef = if (hasLocalCandidateCode) {
                firestore.collection(FirestorePlantConfig.COLLECTION_ACTIVATION_CODES).document(localNormalizedCode)
            } else {
                null
            }

            val syncResult = firestore.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                val remote = if (snapshot.exists()) {
                    runCatching { snapshot.toObject(DeviceLicenseDoc::class.java) }.getOrNull()
                } else {
                    null
                }
                val codeSnap = candidateCodeRef?.let { tx.get(it) }
                val now = System.currentTimeMillis()

                val remoteFirst = remote?.effectiveFirstInstalledAt ?: 0L
                val earliestFirstInstalledAt = when {
                    remoteFirst > 0L && localState.firstLaunchEpochMs > 0L ->
                        minOf(remoteFirst, localState.firstLaunchEpochMs)
                    remoteFirst > 0L -> remoteFirst
                    localState.firstLaunchEpochMs > 0L -> localState.firstLaunchEpochMs
                    else -> now
                }

                val maxTrialEnd = earliestFirstInstalledAt + LicenseManager.TRIAL_DURATION_MS
                val trialCandidates = buildList {
                    val remoteEnd = remote?.effectiveTrialEndsAt ?: 0L
                    if (remoteEnd >= earliestFirstInstalledAt) add(remoteEnd)
                    if (localState.trialEndEpochMs >= earliestFirstInstalledAt) add(localState.trialEndEpochMs)
                    add(maxTrialEnd)
                }
                val earliestTrialEndsAt = trialCandidates.min().coerceIn(earliestFirstInstalledAt, maxTrialEnd)

                var revokedOfflineCode: String? = null
                var localActivationVerified = hasLocalCandidateCode
                var shouldClaimCodeInCloud = false

                if (hasLocalCandidateCode && remote?.isActivated != true && codeSnap != null) {
                    if (codeSnap.exists()) {
                        val existingCode = runCatching { codeSnap.toObject(ActivationCodeDoc::class.java) }.getOrNull()
                        val alreadyClaimed = existingCode?.isClaimed == true
                        if (alreadyClaimed && existingCode?.boundDeviceId != cleanDeviceId) {
                            localActivationVerified = false
                            revokedOfflineCode = localNormalizedCode
                        } else if (!alreadyClaimed) {
                            shouldClaimCodeInCloud = true
                        }
                    } else {
                        shouldClaimCodeInCloud = true
                    }
                }

                val remoteCode = LicenseManager.normalizeCode(remote?.effectiveActivationCode.orEmpty())
                val remoteIsActivated = remote?.isActivated == true && remoteCode in LicenseManager.VALID_ACTIVATION_CODES
                val mergedActivated = remoteIsActivated || localActivationVerified
                val mergedCode = when {
                    remoteIsActivated -> remoteCode
                    localActivationVerified -> localNormalizedCode
                    else -> ""
                }
                val mergedActivatedAt = when {
                    remoteIsActivated && (remote?.activatedAtEpochMs ?: 0L) > 0L -> remote!!.activatedAtEpochMs
                    localActivationVerified && localState.activatedAtEpochMs > 0L -> localState.activatedAtEpochMs
                    mergedActivated -> now
                    else -> 0L
                }

                if (shouldClaimCodeInCloud && candidateCodeRef != null && mergedActivated && mergedCode.isNotEmpty()) {
                    tx.set(
                        candidateCodeRef,
                        mapOf(
                            "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                            "code" to mergedCode,
                            "used" to true,
                            "boundDeviceId" to cleanDeviceId,
                            "claimedByUsername" to safeUsername,
                            "claimedByUid" to uid,
                            "claimedAtEpochMs" to mergedActivatedAt,
                            "claimedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val payload = mapOf(
                    "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                    "deviceId" to cleanDeviceId,
                    "firstInstalledAt" to earliestFirstInstalledAt,
                    "trialEndsAt" to earliestTrialEndsAt,
                    "isActivated" to mergedActivated,
                    "activationCode" to mergedCode,
                    "activatedAtEpochMs" to mergedActivatedAt,
                    "lastUsername" to safeUsername,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val needsWrite = remote == null ||
                    remote.firstInstalledAt <= 0L ||
                    remote.trialEndsAt <= 0L ||
                    earliestFirstInstalledAt < remote.effectiveFirstInstalledAt ||
                    earliestTrialEndsAt < remote.effectiveTrialEndsAt ||
                    (mergedActivated && !remoteIsActivated) ||
                    (safeUsername.isNotBlank() && remote.effectiveLastUsername != safeUsername)

                if (needsWrite) {
                    tx.set(docRef, payload)
                }

                DeviceLicenseSyncResult(
                    doc = DeviceLicenseDoc(
                        id = cleanDeviceId,
                        plantUnit = FirestorePlantConfig.PLANT_UNIT,
                        deviceId = cleanDeviceId,
                        firstInstalledAt = earliestFirstInstalledAt,
                        trialEndsAt = earliestTrialEndsAt,
                        isActivated = mergedActivated,
                        activationCode = mergedCode,
                        activatedAtEpochMs = mergedActivatedAt,
                        lastUsername = safeUsername,
                        updatedByUid = uid
                    ),
                    revokedOfflineCode = revokedOfflineCode
                )
            }
                .addOnFailureListener { err ->
                    Log.w(TAG, "syncDeviceLicense transaction failed for $path: ${err.message}", err)
                }
                .await()
            Result.success(syncResult)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    suspend fun claimActivationCodeForDevice(
        code: String,
        deviceId: String,
        activeUsername: String,
        activatedAtEpochMs: Long,
        firstInstalledAt: Long,
        trialEndsAt: Long
    ): Result<CloudCodeClaimResult> {
        var path = FirestorePlantConfig.COLLECTION_ACTIVATION_CODES
        return try {
            val firestore = requireDb()
            val uid = requireUserId()
            val normalizedCode = LicenseManager.normalizeCode(code)
            val cleanDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120)
            if (cleanDeviceId.isEmpty() || normalizedCode !in LicenseManager.VALID_ACTIVATION_CODES) {
                return Result.failure(IllegalArgumentException("Kode aktivasi atau Device ID tidak valid"))
            }

            val safeUsername = if (activeUsername.isBlank()) {
                ""
            } else {
                OperatorIdentityFormatter.ensureValidAccountName(activeUsername)
            }
            val codeRef = firestore.collection(FirestorePlantConfig.COLLECTION_ACTIVATION_CODES).document(normalizedCode)
            val deviceRef = firestore.collection(FirestorePlantConfig.COLLECTION_DEVICE_LICENSES).document(cleanDeviceId)
            path = "${FirestorePlantConfig.COLLECTION_ACTIVATION_CODES}/$normalizedCode"

            val outcome = firestore.runTransaction { tx ->
                val codeSnap = tx.get(codeRef)
                val deviceSnap = tx.get(deviceRef)
                val existingDevice = if (deviceSnap.exists()) {
                    runCatching { deviceSnap.toObject(DeviceLicenseDoc::class.java) }.getOrNull()
                } else {
                    null
                }
                val remoteFirst = existingDevice?.effectiveFirstInstalledAt ?: 0L
                val earliestFirstInstalledAt = when {
                    remoteFirst > 0L && firstInstalledAt > 0L -> minOf(remoteFirst, firstInstalledAt)
                    remoteFirst > 0L -> remoteFirst
                    firstInstalledAt > 0L -> firstInstalledAt
                    else -> activatedAtEpochMs
                }
                val maxTrialEnd = earliestFirstInstalledAt + LicenseManager.TRIAL_DURATION_MS
                val trialCandidates = buildList {
                    val remoteEnd = existingDevice?.effectiveTrialEndsAt ?: 0L
                    if (remoteEnd >= earliestFirstInstalledAt) add(remoteEnd)
                    if (trialEndsAt >= earliestFirstInstalledAt) add(trialEndsAt)
                    add(maxTrialEnd)
                }
                val earliestTrialEndsAt = trialCandidates.min().coerceIn(earliestFirstInstalledAt, maxTrialEnd)

                if (codeSnap.exists()) {
                    val existingBinding = runCatching { codeSnap.toObject(ActivationCodeDoc::class.java) }.getOrNull()
                    if (existingBinding?.isClaimed == true) {
                        val existingBoundDevice = existingBinding.boundDeviceId
                        if (existingBoundDevice == cleanDeviceId) {
                            val effectiveAt = existingBinding.claimedAtEpochMs.takeIf { it > 0L } ?: activatedAtEpochMs
                            tx.set(
                                deviceRef,
                                mapOf(
                                    "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                                    "deviceId" to cleanDeviceId,
                                    "firstInstalledAt" to earliestFirstInstalledAt,
                                    "trialEndsAt" to earliestTrialEndsAt,
                                    "isActivated" to true,
                                    "activationCode" to normalizedCode,
                                    "activatedAtEpochMs" to effectiveAt,
                                    "lastUsername" to safeUsername,
                                    "updatedByUid" to uid,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            return@runTransaction CloudCodeClaimResult.AlreadyBoundToThisDevice(
                                code = normalizedCode,
                                claimedAtEpochMs = effectiveAt
                            )
                        } else {
                            return@runTransaction CloudCodeClaimResult.BoundToAnotherDevice(normalizedCode)
                        }
                    }
                }

                tx.set(
                    codeRef,
                    mapOf(
                        "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                        "code" to normalizedCode,
                        "used" to true,
                        "boundDeviceId" to cleanDeviceId,
                        "claimedByUsername" to safeUsername,
                        "claimedByUid" to uid,
                        "claimedAtEpochMs" to activatedAtEpochMs,
                        "claimedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    deviceRef,
                    mapOf(
                        "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                        "deviceId" to cleanDeviceId,
                        "firstInstalledAt" to earliestFirstInstalledAt,
                        "trialEndsAt" to earliestTrialEndsAt,
                        "isActivated" to true,
                        "activationCode" to normalizedCode,
                        "activatedAtEpochMs" to activatedAtEpochMs,
                        "lastUsername" to safeUsername,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                CloudCodeClaimResult.Claimed(code = normalizedCode, boundDeviceId = cleanDeviceId)
            }
                .addOnFailureListener { err ->
                    Log.w(TAG, "claimActivationCode transaction failed for $path: ${err.message}", err)
                }
                .await()
            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    /**
     * Persists a verified operator account mapping to Firestore users/{username}
     * with salted PBKDF2 passwordHash and isVerified = true. Never stores plaintext passwords.
     */
    suspend fun upsertVerifiedUserProfile(
        username: String,
        email: String,
        passwordHash: String,
        verifiedAtEpochMs: Long
    ): Result<Unit> {
        val cleanUsername = OperatorIdentityFormatter.sanitizeUsername(username)
        val cleanEmail = email.trim().lowercase()
        val path = "${FirestorePlantConfig.COLLECTION_USERS}/$cleanUsername"
        return try {
            val firestore = requireDb()
            val uid = requireUserId()
            if (cleanUsername.isEmpty() || cleanEmail.isEmpty() || passwordHash.isBlank()) {
                return Result.failure(IllegalArgumentException("Data profil akun tidak lengkap."))
            }
            val payload = mapOf(
                "plantUnit" to FirestorePlantConfig.PLANT_UNIT,
                "username" to cleanUsername,
                "email" to cleanEmail,
                "passwordHash" to passwordHash,
                "isVerified" to true,
                "uid" to uid,
                "verifiedAtEpochMs" to verifiedAtEpochMs.coerceAtLeast(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(FirestorePlantConfig.COLLECTION_USERS)
                .document(cleanUsername)
                .set(payload)
                .addOnFailureListener { err ->
                    Log.w(TAG, "upsertVerifiedUserProfile failed for $path: ${err.message}", err)
                }
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    /**
     * Fetches a verified operator profile from Firestore users/{username} when online.
     * Supports direct lookup even before Firebase Auth sign-in (for Username+Password login
     * and Lupa Password email resolution) per firestore.rules direct single-doc get rule.
     */
    suspend fun fetchVerifiedUserProfile(username: String): Result<UserAccountDoc?> {
        val cleanUsername = OperatorIdentityFormatter.sanitizeUsername(username)
        val path = "${FirestorePlantConfig.COLLECTION_USERS}/$cleanUsername"
        return try {
            val firestore = requireDb()
            if (cleanUsername.isEmpty()) return Result.success(null)
            val snap = firestore.collection(FirestorePlantConfig.COLLECTION_USERS)
                .document(cleanUsername)
                .get()
                .addOnFailureListener { err ->
                    Log.w(TAG, "fetchVerifiedUserProfile failed for $path: ${err.message}", err)
                }
                .await()
            if (!snap.exists()) return Result.success(null)
            val parsed = runCatching {
                snap.toObject(
                    UserAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }.getOrNull()
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.GET, path) }
            Result.failure(e)
        }
    }

    /**
     * Finds the signed-in user's own verified profile in Firestore users/{username}
     * by matching uid first, then by matching email (self-scoped query per firestore.rules).
     */
    suspend fun findVerifiedUserProfileByUidOrEmail(
        uid: String,
        email: String
    ): Result<UserAccountDoc?> {
        val cleanUid = uid.trim()
        val cleanEmail = email.trim().lowercase()
        val path = FirestorePlantConfig.COLLECTION_USERS
        return try {
            val firestore = requireDb()
            requireUserId()

            if (cleanUid.isNotEmpty()) {
                val byUidSnap = runCatching {
                    firestore.collection(FirestorePlantConfig.COLLECTION_USERS)
                        .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
                        .whereEqualTo("uid", cleanUid)
                        .limit(1)
                        .get()
                        .addOnFailureListener { err ->
                            Log.w(TAG, "findVerifiedUserProfileByUid failed: ${err.message}", err)
                        }
                        .await()
                }.getOrNull()
                val byUidDoc = byUidSnap?.documents?.firstOrNull()?.let { doc ->
                    runCatching {
                        doc.toObject(
                            UserAccountDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
                if (byUidDoc != null) {
                    return Result.success(byUidDoc)
                }
            }

            if (cleanEmail.isNotEmpty()) {
                val byEmailSnap = runCatching {
                    firestore.collection(FirestorePlantConfig.COLLECTION_USERS)
                        .whereEqualTo("plantUnit", FirestorePlantConfig.PLANT_UNIT)
                        .whereEqualTo("email", cleanEmail)
                        .limit(1)
                        .get()
                        .addOnFailureListener { err ->
                            Log.w(TAG, "findVerifiedUserProfileByEmail failed: ${err.message}", err)
                        }
                        .await()
                }.getOrNull()
                val byEmailDoc = byEmailSnap?.documents?.firstOrNull()?.let { doc ->
                    runCatching {
                        doc.toObject(
                            UserAccountDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
                if (byEmailDoc != null) {
                    return Result.success(byEmailDoc)
                }
            }

            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, OperationType.LIST, path) }
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirestoreSoundingRepo"

        fun createDatabase(context: Context): FirebaseFirestore? =
            FirebaseSafeProvider.getFirestore(context)
    }
}
