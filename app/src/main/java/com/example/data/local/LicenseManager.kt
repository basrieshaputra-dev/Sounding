package com.example.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

data class LicenseState(
    val deviceId: String = "",
    val isActivated: Boolean = false,
    val activatedCode: String = "",
    val activatedAtEpochMs: Long = 0L,
    val firstLaunchEpochMs: Long = 0L,
    val trialEndEpochMs: Long = 0L,
    val remainingTrialDays: Int = LicenseManager.TRIAL_DURATION_DAYS,
    val isTrialExpired: Boolean = false,
    val usedCodes: Set<String> = emptySet()
)

sealed class LicenseActivationResult {
    data class Success(val code: String, val state: LicenseState) : LicenseActivationResult()
    data object EmptyCode : LicenseActivationResult()
    data class AlreadyUsed(val code: String) : LicenseActivationResult()
    data class BoundToOtherDevice(val code: String) : LicenseActivationResult()
    data class InvalidCode(val code: String) : LicenseActivationResult()
    data class AlreadyActivated(val activeCode: String, val state: LicenseState) : LicenseActivationResult()
}

interface LicenseStorage {
    fun getDeviceId(): String = "local_test_device"
    fun getFirstLaunchEpochMs(): Long
    fun setFirstLaunchEpochMs(epochMs: Long)
    fun getTrialEndsAtEpochMs(): Long = 0L
    fun setTrialEndsAtEpochMs(epochMs: Long) {}
    fun getMaxObservedEpochMs(): Long
    fun setMaxObservedEpochMs(epochMs: Long)
    fun isActivated(): Boolean
    fun getActivatedCode(): String
    fun getActivatedAtEpochMs(): Long
    fun getUsedCodes(): Set<String>
    fun persistActivation(code: String, activatedAtEpochMs: Long, usedCodes: Set<String>)
    fun persistUsedCodes(usedCodes: Set<String>)
    fun revokeUnverifiedActivation(blockedCode: String) {}
}

/**
 * Encrypted local cache for device license & trialEndsAt.
 * Encrypts all license fields inside a single AES-256-GCM blob bound to ANDROID_ID (AAD)
 * so tampering or copying prefs from another phone cannot bypass the 60-day trial.
 */
class SharedPreferencesLicenseStorage(context: Context) : LicenseStorage {

    private val appContext = context.applicationContext
    private val prefs = runCatching {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure { err ->
        android.util.Log.e(TAG, "Failed opening license SharedPreferences", err)
    }.getOrNull()

    private data class EncryptedLicenseBlob(
        val firstInstalledAt: Long = 0L,
        val trialEndsAt: Long = 0L,
        val maxObservedEpochMs: Long = 0L,
        val isActivated: Boolean = false,
        val activationCode: String = "",
        val activatedAtEpochMs: Long = 0L,
        val usedCodes: Set<String> = emptySet()
    )

    @Volatile
    private var cachedBlob: EncryptedLicenseBlob? = null

    @SuppressLint("HardwareIds")
    override fun getDeviceId(): String {
        val rawAndroidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim().orEmpty()

        val sanitizedAndroidId = rawAndroidId
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
            .lowercase(Locale.US)

        if (sanitizedAndroidId.length >= 4) {
            return "and_$sanitizedAndroidId".take(120)
        }

        val cached = runCatching {
            prefs?.getString(KEY_FALLBACK_DEVICE_ID, null)?.trim().orEmpty()
        }.getOrDefault("")
        if (cached.length >= 4) return cached

        val generated = "dev_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        runCatching {
            prefs?.edit()?.putString(KEY_FALLBACK_DEVICE_ID, generated)?.apply()
        }
        return generated
    }

    @Synchronized
    private fun loadBlob(): EncryptedLicenseBlob {
        cachedBlob?.let { return it }
        val p = prefs ?: return EncryptedLicenseBlob().also { cachedBlob = it }
        val deviceId = getDeviceId()
        val encrypted = runCatching { p.getString(KEY_ENCRYPTED_LICENSE_BLOB, null) }.getOrNull()
        if (!encrypted.isNullOrBlank()) {
            val decrypted = runCatching { SecureLicenseCipher.decrypt(encrypted, deviceId) }.getOrNull()
            if (decrypted != null) {
                val parsed = parseJson(decrypted)
                cachedBlob = parsed
                return parsed
            }
        }

        // One-time crash-safe migration from legacy plaintext keys into encrypted AES-256-GCM blob
        val legacyFirstLaunch = runCatching { p.getLong(KEY_LEGACY_FIRST_LAUNCH_EPOCH_MS, 0L) }.getOrDefault(0L)
        val legacyMaxObserved = runCatching { p.getLong(KEY_LEGACY_MAX_OBSERVED_EPOCH_MS, 0L) }.getOrDefault(0L)
        val legacyActivated = runCatching { p.getBoolean(KEY_LEGACY_IS_ACTIVATED, false) }.getOrDefault(false)
        val legacyCode = LicenseManager.normalizeCode(
            runCatching { p.getString(KEY_LEGACY_ACTIVATED_CODE, "") }.getOrNull().orEmpty()
        )
        val legacyActivatedAt = runCatching { p.getLong(KEY_LEGACY_ACTIVATED_AT_EPOCH_MS, 0L) }.getOrDefault(0L)
        val legacyUsed = runCatching {
            p.getStringSet(KEY_LEGACY_USED_CODES, emptySet())
                ?.filterNotNull()
                .orEmpty()
                .map { LicenseManager.normalizeCode(it) }
                .filter { it.isNotEmpty() }
                .toSet()
        }.getOrDefault(emptySet())

        val migrated = EncryptedLicenseBlob(
            firstInstalledAt = legacyFirstLaunch,
            trialEndsAt = if (legacyFirstLaunch > 0L) legacyFirstLaunch + LicenseManager.TRIAL_DURATION_MS else 0L,
            maxObservedEpochMs = legacyMaxObserved,
            isActivated = legacyActivated,
            activationCode = legacyCode,
            activatedAtEpochMs = legacyActivatedAt,
            usedCodes = legacyUsed
        )

        if (legacyFirstLaunch > 0L || legacyActivated || legacyUsed.isNotEmpty()) {
            saveBlob(migrated, removeLegacyPlaintext = true)
        } else {
            cachedBlob = migrated
        }
        return migrated
    }

    @Synchronized
    private fun saveBlob(blob: EncryptedLicenseBlob, removeLegacyPlaintext: Boolean = false) {
        cachedBlob = blob
        val p = prefs ?: return
        runCatching {
            val deviceId = getDeviceId()
            val json = toJson(blob)
            val encrypted = SecureLicenseCipher.encrypt(json, deviceId)
            val editor = p.edit()
            if (encrypted.isNotBlank()) {
                editor.putString(KEY_ENCRYPTED_LICENSE_BLOB, encrypted)
            }
            if (removeLegacyPlaintext) {
                editor
                    .remove(KEY_LEGACY_FIRST_LAUNCH_EPOCH_MS)
                    .remove(KEY_LEGACY_MAX_OBSERVED_EPOCH_MS)
                    .remove(KEY_LEGACY_IS_ACTIVATED)
                    .remove(KEY_LEGACY_ACTIVATED_CODE)
                    .remove(KEY_LEGACY_ACTIVATED_AT_EPOCH_MS)
                    .remove(KEY_LEGACY_USED_CODES)
            }
            editor.apply()
        }.onFailure { err ->
            android.util.Log.w(TAG, "Failed saving encrypted license cache safely", err)
        }
    }

    private fun toJson(blob: EncryptedLicenseBlob): String {
        val usedArray = JSONArray().apply {
            blob.usedCodes.forEach { put(it) }
        }
        return JSONObject().apply {
            put("firstInstalledAt", blob.firstInstalledAt)
            put("trialEndsAt", blob.trialEndsAt)
            put("maxObservedEpochMs", blob.maxObservedEpochMs)
            put("isActivated", blob.isActivated)
            put("activationCode", blob.activationCode)
            put("activatedAtEpochMs", blob.activatedAtEpochMs)
            put("usedCodes", usedArray)
        }.toString()
    }

    private fun parseJson(raw: String): EncryptedLicenseBlob {
        return runCatching {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("usedCodes")
            val codes = buildSet {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val c = LicenseManager.normalizeCode(arr.optString(i, ""))
                        if (c.isNotEmpty()) add(c)
                    }
                }
            }
            EncryptedLicenseBlob(
                firstInstalledAt = obj.optLong("firstInstalledAt", 0L),
                trialEndsAt = obj.optLong("trialEndsAt", 0L),
                maxObservedEpochMs = obj.optLong("maxObservedEpochMs", 0L),
                isActivated = obj.optBoolean("isActivated", false),
                activationCode = LicenseManager.normalizeCode(obj.optString("activationCode", "")),
                activatedAtEpochMs = obj.optLong("activatedAtEpochMs", 0L),
                usedCodes = codes
            )
        }.getOrDefault(EncryptedLicenseBlob())
    }

    override fun getFirstLaunchEpochMs(): Long = loadBlob().firstInstalledAt

    override fun setFirstLaunchEpochMs(epochMs: Long) {
        val current = loadBlob()
        val computedTrialEnds = epochMs + LicenseManager.TRIAL_DURATION_MS
        val nextTrialEnds = if (current.trialEndsAt > 0L) {
            minOf(current.trialEndsAt, computedTrialEnds)
        } else {
            computedTrialEnds
        }
        saveBlob(current.copy(firstInstalledAt = epochMs, trialEndsAt = nextTrialEnds))
    }

    override fun getTrialEndsAtEpochMs(): Long = loadBlob().trialEndsAt

    override fun setTrialEndsAtEpochMs(epochMs: Long) {
        val current = loadBlob()
        saveBlob(current.copy(trialEndsAt = epochMs))
    }

    override fun getMaxObservedEpochMs(): Long = loadBlob().maxObservedEpochMs

    override fun setMaxObservedEpochMs(epochMs: Long) {
        val current = loadBlob()
        saveBlob(current.copy(maxObservedEpochMs = epochMs))
    }

    override fun isActivated(): Boolean = loadBlob().isActivated

    override fun getActivatedCode(): String = loadBlob().activationCode

    override fun getActivatedAtEpochMs(): Long = loadBlob().activatedAtEpochMs

    override fun getUsedCodes(): Set<String> = loadBlob().usedCodes

    override fun persistActivation(code: String, activatedAtEpochMs: Long, usedCodes: Set<String>) {
        val current = loadBlob()
        saveBlob(
            current.copy(
                isActivated = true,
                activationCode = LicenseManager.normalizeCode(code),
                activatedAtEpochMs = activatedAtEpochMs,
                usedCodes = usedCodes.map { LicenseManager.normalizeCode(it) }.filter { it.isNotEmpty() }.toSet()
            )
        )
    }

    override fun persistUsedCodes(usedCodes: Set<String>) {
        val current = loadBlob()
        saveBlob(
            current.copy(
                usedCodes = usedCodes.map { LicenseManager.normalizeCode(it) }.filter { it.isNotEmpty() }.toSet()
            )
        )
    }

    override fun revokeUnverifiedActivation(blockedCode: String) {
        val normalized = LicenseManager.normalizeCode(blockedCode)
        val current = loadBlob()
        val mergedUsed = if (normalized.isNotEmpty()) current.usedCodes + normalized else current.usedCodes
        saveBlob(
            current.copy(
                isActivated = false,
                activationCode = "",
                activatedAtEpochMs = 0L,
                usedCodes = mergedUsed
            )
        )
    }

    companion object {
        private const val TAG = "LicenseStorage"
        private const val PREFS_NAME = "sounding_license_prefs"
        private const val KEY_FALLBACK_DEVICE_ID = "license_fallback_device_id"
        private const val KEY_ENCRYPTED_LICENSE_BLOB = "license_encrypted_blob_v2"
        private const val KEY_LEGACY_FIRST_LAUNCH_EPOCH_MS = "trial_first_launch_epoch_ms"
        private const val KEY_LEGACY_MAX_OBSERVED_EPOCH_MS = "trial_max_observed_epoch_ms"
        private const val KEY_LEGACY_IS_ACTIVATED = "license_is_activated"
        private const val KEY_LEGACY_ACTIVATED_CODE = "license_activated_code"
        private const val KEY_LEGACY_ACTIVATED_AT_EPOCH_MS = "license_activated_at_epoch_ms"
        private const val KEY_LEGACY_USED_CODES = "license_used_codes"
    }
}

class LicenseManager(
    private val storage: LicenseStorage,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun getDeviceId(): String = storage.getDeviceId()

    fun refreshState(): LicenseState {
        val deviceId = storage.getDeviceId()
        val now = nowProvider()
        var firstLaunch = storage.getFirstLaunchEpochMs()
        if (firstLaunch <= 0L) {
            firstLaunch = now
            storage.setFirstLaunchEpochMs(firstLaunch)
        }

        val computedTrialEnd = firstLaunch + TRIAL_DURATION_MS
        val storedTrialEnd = storage.getTrialEndsAtEpochMs()
        val trialEndEpochMs = if (storedTrialEnd > 0L) {
            minOf(storedTrialEnd, computedTrialEnd)
        } else {
            storage.setTrialEndsAtEpochMs(computedTrialEnd)
            computedTrialEnd
        }

        val previousMax = storage.getMaxObservedEpochMs()
        val effectiveNow = maxOf(now, previousMax, firstLaunch)
        if (effectiveNow > previousMax) {
            storage.setMaxObservedEpochMs(effectiveNow)
        }

        val isActivated = storage.isActivated()
        val activatedCode = normalizeCode(storage.getActivatedCode())
        val activatedAt = storage.getActivatedAtEpochMs()
        val usedCodes = buildSet {
            addAll(storage.getUsedCodes())
            if (isActivated && activatedCode.isNotEmpty()) {
                add(activatedCode)
            }
        }

        if (isActivated) {
            return LicenseState(
                deviceId = deviceId,
                isActivated = true,
                activatedCode = activatedCode,
                activatedAtEpochMs = activatedAt,
                firstLaunchEpochMs = firstLaunch,
                trialEndEpochMs = trialEndEpochMs,
                remainingTrialDays = 0,
                isTrialExpired = false,
                usedCodes = usedCodes
            )
        }

        val remainingMs = (trialEndEpochMs - effectiveNow).coerceAtLeast(0L)
        val isExpired = effectiveNow >= trialEndEpochMs
        val remainingDays = if (isExpired) {
            0
        } else {
            ceil(remainingMs.toDouble() / MILLIS_PER_DAY.toDouble())
                .toInt()
                .coerceIn(1, TRIAL_DURATION_DAYS)
        }

        return LicenseState(
            deviceId = deviceId,
            isActivated = false,
            activatedCode = "",
            activatedAtEpochMs = 0L,
            firstLaunchEpochMs = firstLaunch,
            trialEndEpochMs = trialEndEpochMs,
            remainingTrialDays = remainingDays,
            isTrialExpired = isExpired,
            usedCodes = usedCodes
        )
    }

    /**
     * Merges authoritative ANDROID_ID license data from Firestore device_licenses/{deviceId}:
     * - Always keeps the EARLIEST firstInstalledAt and EARLIEST trialEndsAt so reinstalls,
     *   clearing app data, or switching namauser@shiftA/B/C never reset the 60-day trial.
     * - Restores permanent activation if this ANDROID_ID was previously activated in cloud.
     */
    fun mergeRemoteDeviceLicense(
        remoteFirstInstalledAt: Long,
        remoteTrialEndsAt: Long = 0L,
        remoteIsActivated: Boolean,
        remoteActivationCode: String,
        remoteActivatedAtEpochMs: Long
    ): LicenseState {
        val localState = refreshState()
        if (remoteFirstInstalledAt > 0L && remoteFirstInstalledAt < localState.firstLaunchEpochMs) {
            storage.setFirstLaunchEpochMs(remoteFirstInstalledAt)
        }
        val effectiveRemoteTrialEnd = when {
            remoteTrialEndsAt > 0L && remoteFirstInstalledAt > 0L ->
                minOf(remoteTrialEndsAt, remoteFirstInstalledAt + TRIAL_DURATION_MS)
            remoteTrialEndsAt > 0L -> remoteTrialEndsAt
            remoteFirstInstalledAt > 0L -> remoteFirstInstalledAt + TRIAL_DURATION_MS
            else -> 0L
        }
        if (effectiveRemoteTrialEnd > 0L && effectiveRemoteTrialEnd < localState.trialEndEpochMs) {
            storage.setTrialEndsAtEpochMs(effectiveRemoteTrialEnd)
        }

        val normalizedRemoteCode = normalizeCode(remoteActivationCode)
        if (remoteIsActivated && normalizedRemoteCode in VALID_ACTIVATION_CODES && !localState.isActivated) {
            val mergedUsed = localState.usedCodes + normalizedRemoteCode
            val effectiveAt = remoteActivatedAtEpochMs.takeIf { it > 0L } ?: nowProvider()
            storage.persistActivation(
                code = normalizedRemoteCode,
                activatedAtEpochMs = effectiveAt,
                usedCodes = mergedUsed
            )
        }
        return refreshState()
    }

    /**
     * Revokes a local offline activation if server verification discovers that the code
     * was already bound to a different ANDROID_ID in activation_codes/{code}.
     */
    fun revokeUnverifiedOfflineActivation(blockedCode: String): LicenseState {
        storage.revokeUnverifiedActivation(blockedCode)
        return refreshState()
    }

    fun preValidateCode(rawInput: String): LicenseActivationResult? {
        val normalized = normalizeCode(rawInput)
        if (normalized.isEmpty()) return LicenseActivationResult.EmptyCode

        val currentState = refreshState()
        if (normalized in currentState.usedCodes) {
            return LicenseActivationResult.AlreadyUsed(normalized)
        }
        if (normalized !in VALID_ACTIVATION_CODES) {
            return LicenseActivationResult.InvalidCode(normalized)
        }
        if (currentState.isActivated) {
            return LicenseActivationResult.AlreadyActivated(
                activeCode = currentState.activatedCode,
                state = currentState
            )
        }
        return null
    }

    fun activateCode(rawInput: String): LicenseActivationResult {
        val rejection = preValidateCode(rawInput)
        if (rejection != null) return rejection

        val normalized = normalizeCode(rawInput)
        val currentState = refreshState()
        val now = nowProvider()
        val updatedUsedCodes = currentState.usedCodes + normalized
        storage.persistActivation(
            code = normalized,
            activatedAtEpochMs = now,
            usedCodes = updatedUsedCodes
        )

        val newState = refreshState()
        return LicenseActivationResult.Success(
            code = normalized,
            state = newState
        )
    }

    fun markCodeUsedLocally(rawCode: String) {
        val normalized = normalizeCode(rawCode)
        if (normalized.isEmpty()) return
        val updated = storage.getUsedCodes() + normalized
        storage.persistUsedCodes(updated)
    }

    companion object {
        const val TRIAL_DURATION_DAYS = 60
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val TRIAL_DURATION_MS = TRIAL_DURATION_DAYS.toLong() * MILLIS_PER_DAY

        val VALID_ACTIVATION_CODES: Set<String> = linkedSetOf(
            "SPTF-8472",
            "SPTF-3916",
            "SPTF-6259",
            "SPTF-5184",
            "SPTF-9347",
            "SPTF-2763",
            "SPTF-7528",
            "SPTF-4691"
        )

        fun normalizeCode(raw: String): String = raw.trim().uppercase(Locale.US)
    }
}
