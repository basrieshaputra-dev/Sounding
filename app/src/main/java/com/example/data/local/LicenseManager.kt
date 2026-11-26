package com.example.data.local

import android.content.Context
import java.util.Locale
import kotlin.math.ceil

data class LicenseState(
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
    data class InvalidCode(val code: String) : LicenseActivationResult()
    data class AlreadyActivated(val activeCode: String, val state: LicenseState) : LicenseActivationResult()
}

interface LicenseStorage {
    fun getFirstLaunchEpochMs(): Long
    fun setFirstLaunchEpochMs(epochMs: Long)
    fun getMaxObservedEpochMs(): Long
    fun setMaxObservedEpochMs(epochMs: Long)
    fun isActivated(): Boolean
    fun getActivatedCode(): String
    fun getActivatedAtEpochMs(): Long
    fun getUsedCodes(): Set<String>
    fun persistActivation(code: String, activatedAtEpochMs: Long, usedCodes: Set<String>)
    fun persistUsedCodes(usedCodes: Set<String>)
}

class SharedPreferencesLicenseStorage(context: Context) : LicenseStorage {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getFirstLaunchEpochMs(): Long = prefs.getLong(KEY_FIRST_LAUNCH_EPOCH_MS, 0L)

    override fun setFirstLaunchEpochMs(epochMs: Long) {
        prefs.edit()
            .putLong(KEY_FIRST_LAUNCH_EPOCH_MS, epochMs)
            .commit()
    }

    override fun getMaxObservedEpochMs(): Long = prefs.getLong(KEY_MAX_OBSERVED_EPOCH_MS, 0L)

    override fun setMaxObservedEpochMs(epochMs: Long) {
        prefs.edit()
            .putLong(KEY_MAX_OBSERVED_EPOCH_MS, epochMs)
            .commit()
    }

    override fun isActivated(): Boolean = prefs.getBoolean(KEY_IS_ACTIVATED, false)

    override fun getActivatedCode(): String = prefs.getString(KEY_ACTIVATED_CODE, "").orEmpty()

    override fun getActivatedAtEpochMs(): Long = prefs.getLong(KEY_ACTIVATED_AT_EPOCH_MS, 0L)

    override fun getUsedCodes(): Set<String> {
        val stored = prefs.getStringSet(KEY_USED_CODES, emptySet()).orEmpty()
        return stored.map { LicenseManager.normalizeCode(it) }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    override fun persistActivation(code: String, activatedAtEpochMs: Long, usedCodes: Set<String>) {
        val copy = HashSet<String>(usedCodes)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putString(KEY_ACTIVATED_CODE, code)
            .putLong(KEY_ACTIVATED_AT_EPOCH_MS, activatedAtEpochMs)
            .putStringSet(KEY_USED_CODES, copy)
            .commit()
    }

    override fun persistUsedCodes(usedCodes: Set<String>) {
        val copy = HashSet<String>(usedCodes)
        prefs.edit()
            .putStringSet(KEY_USED_CODES, copy)
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "sounding_license_prefs"
        private const val KEY_FIRST_LAUNCH_EPOCH_MS = "trial_first_launch_epoch_ms"
        private const val KEY_MAX_OBSERVED_EPOCH_MS = "trial_max_observed_epoch_ms"
        private const val KEY_IS_ACTIVATED = "license_is_activated"
        private const val KEY_ACTIVATED_CODE = "license_activated_code"
        private const val KEY_ACTIVATED_AT_EPOCH_MS = "license_activated_at_epoch_ms"
        private const val KEY_USED_CODES = "license_used_codes"
    }
}

class LicenseManager(
    private val storage: LicenseStorage,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun refreshState(): LicenseState {
        val now = nowProvider()
        var firstLaunch = storage.getFirstLaunchEpochMs()
        if (firstLaunch <= 0L) {
            firstLaunch = now
            storage.setFirstLaunchEpochMs(firstLaunch)
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
        val trialEndEpochMs = firstLaunch + TRIAL_DURATION_MS

        if (isActivated) {
            return LicenseState(
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

        val elapsedMs = (effectiveNow - firstLaunch).coerceAtLeast(0L)
        val isExpired = elapsedMs >= TRIAL_DURATION_MS
        val remainingMs = (TRIAL_DURATION_MS - elapsedMs).coerceAtLeast(0L)
        val remainingDays = if (isExpired) {
            0
        } else {
            ceil(remainingMs.toDouble() / MILLIS_PER_DAY.toDouble())
                .toInt()
                .coerceIn(1, TRIAL_DURATION_DAYS)
        }

        return LicenseState(
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

    fun activateCode(rawInput: String): LicenseActivationResult {
        val normalized = normalizeCode(rawInput)
        if (normalized.isEmpty()) {
            return LicenseActivationResult.EmptyCode
        }

        val currentState = refreshState()

        // Always reject any code that has already been claimed on this device
        if (normalized in currentState.usedCodes) {
            return LicenseActivationResult.AlreadyUsed(normalized)
        }

        // Reject any code not in the authorized single-use list
        if (normalized !in VALID_ACTIVATION_CODES) {
            return LicenseActivationResult.InvalidCode(normalized)
        }

        // If already permanently activated, keep existing license without burning another code
        if (currentState.isActivated) {
            return LicenseActivationResult.AlreadyActivated(
                activeCode = currentState.activatedCode,
                state = currentState
            )
        }

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
