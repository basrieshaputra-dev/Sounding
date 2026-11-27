package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object OperatorIdentityFormatter {
    private val INVALID_USERNAME_REGEX = Regex("[^a-z0-9._-]")
    private val VALID_ACCOUNT_REGEX = Regex("^[a-z0-9._-]{1,64}$")

    /**
     * Normalizes raw account/username input into lowercase with no spaces,
     * stripping any legacy "@shift..." suffix and keeping only [a-z0-9._-].
     */
    fun sanitizeUsername(raw: String): String {
        val beforeAt = raw.substringBefore("@")
        return beforeAt
            .lowercase(Locale.US)
            .replace(INVALID_USERNAME_REGEX, "")
            .take(64)
    }

    /**
     * Ensures any string is normalized into a valid lowercase account name,
     * falling back to "operator" if empty.
     */
    fun ensureValidAccountName(raw: String): String {
        val clean = sanitizeUsername(raw)
        return clean.ifEmpty { "operator" }
    }

    fun isValidAccountName(value: String): Boolean = VALID_ACCOUNT_REGEX.matches(value.trim())
}

data class VerifiedOperatorAccount(
    val accountName: String,
    val email: String,
    val passwordHash: String = "",
    val isVerified: Boolean = true,
    val verifiedAtEpochMs: Long = 0L
)

data class PendingEmailVerification(
    val accountName: String,
    val email: String,
    val passwordHash: String = "",
    val sentAtEpochMs: Long = 0L
)

data class OperatorSession(
    val activeAccountName: String = "",
    val activeOperatorEmail: String = "",
    val isLoggedIn: Boolean = false,
    val explicitlyLoggedOut: Boolean = false,
    val savedAccountNames: List<String> = emptyList(),
    val verifiedAccounts: List<VerifiedOperatorAccount> = emptyList(),
    val pendingVerification: PendingEmailVerification? = null
) {
    val hasActiveSession: Boolean get() = isLoggedIn && activeAccountName.isNotBlank()

    fun findVerifiedAccount(accountName: String): VerifiedOperatorAccount? {
        val clean = OperatorIdentityFormatter.sanitizeUsername(accountName)
        if (clean.isEmpty()) return null
        return verifiedAccounts.firstOrNull {
            it.isVerified && it.accountName.equals(clean, ignoreCase = true)
        }
    }

    fun findVerifiedAccountByEmail(email: String): VerifiedOperatorAccount? {
        val cleanEmail = email.trim().lowercase(Locale.US)
        if (cleanEmail.isEmpty()) return null
        return verifiedAccounts.firstOrNull {
            it.isVerified && it.email.equals(cleanEmail, ignoreCase = true)
        }
    }

    fun isAccountVerified(accountName: String): Boolean = findVerifiedAccount(accountName) != null
}

class OperatorPreferences(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences? = runCatching {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure { err ->
        Log.e(TAG, "Failed opening operator SharedPreferences", err)
    }.getOrNull()

    private val deviceKeyScope: String by lazy {
        runCatching {
            val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "local_device_scope"
            "op_cred_$androidId"
        }.getOrDefault("op_cred_local_device_scope")
    }

    private fun safeGetString(key: String, default: String? = null): String? {
        return runCatching { prefs?.getString(key, default) }.onFailure { err ->
            Log.w(TAG, "Corrupted string preference for key=$key; using default", err)
        }.getOrDefault(default)
    }

    private fun safeGetBoolean(key: String, default: Boolean): Boolean {
        return runCatching { prefs?.getBoolean(key, default) ?: default }.onFailure { err ->
            Log.w(TAG, "Corrupted boolean preference for key=$key; using default", err)
        }.getOrDefault(default)
    }

    /**
     * Synchronously commits session & credential updates to disk so closing the app,
     * swiping from recents, or preview reloads never drop active login state.
     */
    private fun safeEdit(block: SharedPreferences.Editor.() -> Unit) {
        runCatching {
            val editor = prefs?.edit() ?: return
            editor.block()
            val committed = runCatching { editor.commit() }.getOrDefault(false)
            if (!committed) {
                editor.apply()
            }
        }.onFailure { err ->
            Log.w(TAG, "Failed saving operator SharedPreferences", err)
        }
    }

    private fun encodeSensitiveJson(rawJson: String): String {
        val encrypted = SecureLicenseCipher.encrypt(rawJson, deviceKeyScope)
        return encrypted.ifBlank { rawJson }
    }

    private fun decodeSensitiveJson(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        val trimmed = stored.trim()
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            // Backward-compatible migration from plain JSON stored by earlier app versions
            return trimmed
        }
        return SecureLicenseCipher.decrypt(trimmed, deviceKeyScope)
    }

    fun loadSession(): OperatorSession {
        return runCatching {
            val rawActive = safeGetString(KEY_ACTIVE_ACCOUNT_NAME, null)
                ?: safeGetString(KEY_LEGACY_ACTIVE_OPERATOR_ID, null)
                ?: safeGetString(KEY_LEGACY_ACTIVE_USERNAME, "")
            val normalizedActive = OperatorIdentityFormatter.sanitizeUsername(rawActive.orEmpty())
            val saved = loadSavedAccountNames()
            val verified = loadVerifiedAccounts()
            val pending = loadPendingVerification()
            val activeEmail = verified.firstOrNull {
                it.accountName.equals(normalizedActive, ignoreCase = true)
            }?.email ?: safeGetString(KEY_ACTIVE_OPERATOR_EMAIL, "").orEmpty().trim().lowercase(Locale.US)

            val explicitlyLoggedOut = safeGetBoolean(KEY_USER_EXPLICITLY_LOGGED_OUT, false)
            val hasExplicitFlag = runCatching { prefs?.contains(KEY_IS_LOGGED_IN) == true }.getOrDefault(false)
            val persistedLoggedIn = if (hasExplicitFlag) {
                safeGetBoolean(KEY_IS_LOGGED_IN, false)
            } else {
                // Seamless migration for sessions saved before KEY_IS_LOGGED_IN was added
                normalizedActive.isNotBlank() && !explicitlyLoggedOut
            }
            val effectiveLoggedIn = persistedLoggedIn && normalizedActive.isNotBlank() && !explicitlyLoggedOut

            // Ensure non-empty active session migrates KEY_IS_LOGGED_IN persistently
            if (effectiveLoggedIn && !hasExplicitFlag) {
                safeEdit {
                    putBoolean(KEY_IS_LOGGED_IN, true)
                    putBoolean(KEY_USER_EXPLICITLY_LOGGED_OUT, false)
                    putString(KEY_ACTIVE_ACCOUNT_NAME, normalizedActive)
                    if (activeEmail.isNotBlank()) {
                        putString(KEY_ACTIVE_OPERATOR_EMAIL, activeEmail)
                    }
                }
            }

            // One-time silent cleanup of legacy shift-era keys so stale formats never interfere
            if (prefs != null && (prefs.contains(KEY_LEGACY_ACTIVE_SHIFT) || prefs.contains(KEY_LEGACY_SHIFT_GROUP))) {
                safeEdit {
                    remove(KEY_LEGACY_ACTIVE_SHIFT)
                    remove(KEY_LEGACY_SHIFT_GROUP)
                }
            }

            OperatorSession(
                activeAccountName = if (effectiveLoggedIn) normalizedActive else "",
                activeOperatorEmail = if (effectiveLoggedIn) activeEmail else "",
                isLoggedIn = effectiveLoggedIn,
                explicitlyLoggedOut = explicitlyLoggedOut,
                savedAccountNames = saved,
                verifiedAccounts = verified,
                pendingVerification = pending
            )
        }.onFailure { err ->
            Log.e(TAG, "Failed loading OperatorSession; returning empty fallback", err)
        }.getOrDefault(OperatorSession())
    }

    fun savePendingVerification(
        rawUsername: String,
        email: String,
        passwordHash: String = "",
        sentAtEpochMs: Long = System.currentTimeMillis()
    ): OperatorSession {
        return runCatching {
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
            val cleanEmail = email.trim().lowercase(Locale.US)
            if (cleanAccount.isEmpty() || cleanEmail.isEmpty()) return loadSession()

            val obj = JSONObject().apply {
                put("accountName", cleanAccount)
                put("email", cleanEmail)
                put("passwordHash", passwordHash)
                put("sentAtEpochMs", sentAtEpochMs)
            }
            safeEdit {
                putString(KEY_PENDING_VERIFICATION_JSON, encodeSensitiveJson(obj.toString()))
            }
            loadSession()
        }.onFailure { err ->
            Log.w(TAG, "Failed saving pending verification", err)
        }.getOrElse { loadSession() }
    }

    fun clearPendingVerification(): OperatorSession {
        safeEdit {
            remove(KEY_PENDING_VERIFICATION_JSON)
        }
        return loadSession()
    }

    fun saveVerifiedAccount(
        rawUsername: String,
        email: String,
        passwordHash: String = "",
        verifiedAtEpochMs: Long = System.currentTimeMillis()
    ): OperatorSession {
        return runCatching {
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
            val cleanEmail = email.trim().lowercase(Locale.US)
            if (cleanAccount.isEmpty() || cleanEmail.isEmpty()) return loadSession()

            val existingVerified = loadVerifiedAccounts()
            val previousMatch = existingVerified.firstOrNull {
                it.accountName.equals(cleanAccount, ignoreCase = true)
            }
            val effectiveHash = passwordHash.ifBlank { previousMatch?.passwordHash.orEmpty() }
            val updatedVerified = buildList {
                add(
                    VerifiedOperatorAccount(
                        accountName = cleanAccount,
                        email = cleanEmail,
                        passwordHash = effectiveHash,
                        isVerified = true,
                        verifiedAtEpochMs = verifiedAtEpochMs
                    )
                )
                existingVerified.forEach { prev ->
                    if (!prev.accountName.equals(cleanAccount, ignoreCase = true)) {
                        add(prev)
                    }
                }
            }.take(MAX_SAVED_ACCOUNTS)

            val verifiedJson = serializeVerifiedAccounts(updatedVerified)
            val backupJson = serializeAccountEmailBackup(updatedVerified)

            val existingNames = loadSavedAccountNames()
            val mergedNames = buildList {
                add(cleanAccount)
                existingNames.forEach { prev ->
                    if (!prev.equals(cleanAccount, ignoreCase = true)) add(prev)
                }
            }.take(MAX_SAVED_ACCOUNTS)

            val namesJson = JSONArray().apply {
                mergedNames.forEach { put(it) }
            }.toString()

            safeEdit {
                putString(KEY_ACTIVE_ACCOUNT_NAME, cleanAccount)
                putString(KEY_ACTIVE_OPERATOR_EMAIL, cleanEmail)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putBoolean(KEY_USER_EXPLICITLY_LOGGED_OUT, false)
                putString(KEY_SAVED_ACCOUNT_NAMES_JSON, namesJson)
                putString(KEY_VERIFIED_ACCOUNTS_JSON, encodeSensitiveJson(verifiedJson))
                putString(KEY_ACCOUNT_EMAIL_BACKUP_JSON, backupJson)
                remove(KEY_PENDING_VERIFICATION_JSON)
            }

            OperatorSession(
                activeAccountName = cleanAccount,
                activeOperatorEmail = cleanEmail,
                isLoggedIn = true,
                explicitlyLoggedOut = false,
                savedAccountNames = mergedNames,
                verifiedAccounts = updatedVerified,
                pendingVerification = null
            )
        }.onFailure { err ->
            Log.w(TAG, "Failed saving verified account", err)
        }.getOrElse { loadSession() }
    }

    /**
     * Caches a verified user profile retrieved from Firestore into local storage without
     * changing the currently active account yet (so password can be verified first).
     */
    fun cacheVerifiedAccountFromCloud(
        rawUsername: String,
        email: String,
        passwordHash: String,
        verifiedAtEpochMs: Long
    ): OperatorSession {
        return runCatching {
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
            val cleanEmail = email.trim().lowercase(Locale.US)
            if (cleanAccount.isEmpty() || cleanEmail.isEmpty()) return loadSession()

            val existingVerified = loadVerifiedAccounts()
            val previousMatch = existingVerified.firstOrNull {
                it.accountName.equals(cleanAccount, ignoreCase = true)
            }
            val effectiveHash = passwordHash.ifBlank { previousMatch?.passwordHash.orEmpty() }
            val updatedVerified = buildList {
                add(
                    VerifiedOperatorAccount(
                        accountName = cleanAccount,
                        email = cleanEmail,
                        passwordHash = effectiveHash,
                        isVerified = true,
                        verifiedAtEpochMs = verifiedAtEpochMs
                    )
                )
                existingVerified.forEach { prev ->
                    if (!prev.accountName.equals(cleanAccount, ignoreCase = true)) {
                        add(prev)
                    }
                }
            }.take(MAX_SAVED_ACCOUNTS)

            val existingNames = loadSavedAccountNames()
            val mergedNames = buildList {
                add(cleanAccount)
                existingNames.forEach { prev ->
                    if (!prev.equals(cleanAccount, ignoreCase = true)) add(prev)
                }
            }.take(MAX_SAVED_ACCOUNTS)

            val namesJson = JSONArray().apply {
                mergedNames.forEach { put(it) }
            }.toString()

            safeEdit {
                putString(KEY_SAVED_ACCOUNT_NAMES_JSON, namesJson)
                putString(KEY_VERIFIED_ACCOUNTS_JSON, encodeSensitiveJson(serializeVerifiedAccounts(updatedVerified)))
                putString(KEY_ACCOUNT_EMAIL_BACKUP_JSON, serializeAccountEmailBackup(updatedVerified))
            }
            loadSession()
        }.onFailure { err ->
            Log.w(TAG, "Failed caching verified account from cloud", err)
        }.getOrElse { loadSession() }
    }

    /**
     * Updates the salted PBKDF2 password hash for an already-verified local account
     * (used when migrating a legacy verified account or syncing a newly reset password).
     */
    fun updateVerifiedAccountPasswordHash(
        rawUsername: String,
        passwordHash: String
    ): OperatorSession {
        return runCatching {
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
            if (cleanAccount.isEmpty() || passwordHash.isBlank()) return loadSession()

            val existingVerified = loadVerifiedAccounts()
            val updatedVerified = existingVerified.map { acc ->
                if (acc.accountName.equals(cleanAccount, ignoreCase = true)) {
                    acc.copy(passwordHash = passwordHash, isVerified = true)
                } else {
                    acc
                }
            }
            safeEdit {
                putString(KEY_VERIFIED_ACCOUNTS_JSON, encodeSensitiveJson(serializeVerifiedAccounts(updatedVerified)))
                putString(KEY_ACCOUNT_EMAIL_BACKUP_JSON, serializeAccountEmailBackup(updatedVerified))
            }
            loadSession()
        }.onFailure { err ->
            Log.w(TAG, "Failed updating verified account passwordHash", err)
        }.getOrElse { loadSession() }
    }

    fun saveActiveAccount(rawUsername: String): OperatorSession {
        return runCatching {
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
            if (cleanAccount.isEmpty()) return loadSession()

            val verified = loadVerifiedAccounts()
            val matchedEmail = verified.firstOrNull {
                it.accountName.equals(cleanAccount, ignoreCase = true)
            }?.email.orEmpty()

            val existing = loadSavedAccountNames()
            val merged = buildList {
                add(cleanAccount)
                existing.forEach { prev ->
                    if (!prev.equals(cleanAccount, ignoreCase = true)) {
                        add(prev)
                    }
                }
            }.take(MAX_SAVED_ACCOUNTS)

            val json = JSONArray().apply {
                merged.forEach { put(it) }
            }.toString()

            safeEdit {
                putString(KEY_ACTIVE_ACCOUNT_NAME, cleanAccount)
                putString(KEY_ACTIVE_OPERATOR_EMAIL, matchedEmail)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putBoolean(KEY_USER_EXPLICITLY_LOGGED_OUT, false)
                putString(KEY_SAVED_ACCOUNT_NAMES_JSON, json)
            }

            OperatorSession(
                activeAccountName = cleanAccount,
                activeOperatorEmail = matchedEmail,
                isLoggedIn = true,
                explicitlyLoggedOut = false,
                savedAccountNames = merged,
                verifiedAccounts = verified,
                pendingVerification = loadPendingVerification()
            )
        }.onFailure { err ->
            Log.w(TAG, "Failed saving active account", err)
        }.getOrElse { loadSession() }
    }

    /**
     * Explicit user-initiated logout: clears active session flags while preserving
     * verifiedAccounts locally so the operator can sign back in with Username + Password.
     */
    fun logoutSession(): OperatorSession {
        safeEdit {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_USER_EXPLICITLY_LOGGED_OUT, true)
            remove(KEY_ACTIVE_ACCOUNT_NAME)
            remove(KEY_LEGACY_ACTIVE_OPERATOR_ID)
            remove(KEY_ACTIVE_OPERATOR_EMAIL)
        }
        return loadSession()
    }

    fun clearActiveAccount(): OperatorSession = logoutSession()

    fun removeSavedAccountName(accountName: String): OperatorSession {
        return runCatching {
            val clean = OperatorIdentityFormatter.sanitizeUsername(accountName)
            val filtered = loadSavedAccountNames().filterNot { it.equals(clean, ignoreCase = true) }
            val json = JSONArray().apply {
                filtered.forEach { put(it) }
            }.toString()
            safeEdit {
                putString(KEY_SAVED_ACCOUNT_NAMES_JSON, json)
            }
            loadSession()
        }.onFailure { err ->
            Log.w(TAG, "Failed removing saved account name", err)
        }.getOrElse { loadSession() }
    }

    /**
     * Removes a saved verified account entry from this device's quick-select list and email backup.
     * Cloud account data in Firebase remains untouched; if the account is not currently logged in,
     * any stale active_account_name fallback pointing to it is also cleared.
     */
    fun removeVerifiedAccount(rawAccountName: String): OperatorSession {
        return runCatching {
            val clean = OperatorIdentityFormatter.sanitizeUsername(rawAccountName)
            if (clean.isEmpty()) return loadSession()

            val currentSession = loadSession()
            val filteredVerified = loadVerifiedAccounts()
                .filterNot { it.accountName.equals(clean, ignoreCase = true) }
            val filteredNames = loadSavedAccountNames()
                .filterNot { it.equals(clean, ignoreCase = true) }
            val namesJson = JSONArray().apply {
                filteredNames.forEach { put(it) }
            }.toString()

            val clearStaleActive = !currentSession.hasActiveSession &&
                safeGetString(KEY_ACTIVE_ACCOUNT_NAME, "")
                    .orEmpty()
                    .equals(clean, ignoreCase = true)

            safeEdit {
                putString(KEY_SAVED_ACCOUNT_NAMES_JSON, namesJson)
                putString(
                    KEY_VERIFIED_ACCOUNTS_JSON,
                    encodeSensitiveJson(serializeVerifiedAccounts(filteredVerified))
                )
                putString(
                    KEY_ACCOUNT_EMAIL_BACKUP_JSON,
                    serializeAccountEmailBackup(filteredVerified)
                )
                if (clearStaleActive) {
                    remove(KEY_ACTIVE_ACCOUNT_NAME)
                    remove(KEY_ACTIVE_OPERATOR_EMAIL)
                }
            }
            loadSession()
        }.onFailure { err ->
            Log.w(TAG, "Failed removing verified account from device", err)
        }.getOrElse { loadSession() }
    }

    private fun serializeVerifiedAccounts(accounts: List<VerifiedOperatorAccount>): String {
        return JSONArray().apply {
            accounts.forEach { acc ->
                put(
                    JSONObject().apply {
                        put("accountName", acc.accountName)
                        put("email", acc.email)
                        put("passwordHash", acc.passwordHash)
                        put("isVerified", acc.isVerified)
                        put("verifiedAtEpochMs", acc.verifiedAtEpochMs)
                    }
                )
            }
        }.toString()
    }

    /**
     * Non-secret backup of username -> email mappings (never stores passwords or hashes)
     * so account discovery and Forgot Password remain resilient even if Keystore resets.
     */
    private fun serializeAccountEmailBackup(accounts: List<VerifiedOperatorAccount>): String {
        return JSONArray().apply {
            accounts.forEach { acc ->
                put(
                    JSONObject().apply {
                        put("accountName", acc.accountName)
                        put("email", acc.email)
                        put("isVerified", acc.isVerified)
                        put("verifiedAtEpochMs", acc.verifiedAtEpochMs)
                    }
                )
            }
        }.toString()
    }

    private fun loadSavedAccountNames(): List<String> {
        val raw = safeGetString(KEY_SAVED_ACCOUNT_NAMES_JSON, null)
            ?: safeGetString(KEY_LEGACY_SAVED_OPERATOR_IDS_JSON, null)
            ?: safeGetString(KEY_LEGACY_SAVED_USERNAMES_JSON, null)
            ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val seen = LinkedHashSet<String>()
            for (i in 0 until arr.length()) {
                val clean = OperatorIdentityFormatter.sanitizeUsername(arr.optString(i, ""))
                if (clean.isNotEmpty()) {
                    seen.add(clean)
                }
            }
            seen.toList()
        }.onFailure { err ->
            Log.w(TAG, "Corrupted saved account names JSON; using empty list", err)
        }.getOrDefault(emptyList())
    }

    private fun parseVerifiedAccountsJson(rawJson: String?): List<VerifiedOperatorAccount> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            val seen = LinkedHashSet<String>()
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawAccount = obj.optString("accountName", "").ifBlank {
                        obj.optString("operatorId", "").ifBlank { obj.optString("username", "") }
                    }
                    val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawAccount)
                    val email = obj.optString("email", "").trim().lowercase(Locale.US)
                    val passwordHash = obj.optString("passwordHash", "").trim()
                    val isVerified = obj.optBoolean("isVerified", true)
                    val verifiedAt = obj.optLong("verifiedAtEpochMs", 0L)
                    if (cleanAccount.isNotEmpty() && email.isNotEmpty() && seen.add(cleanAccount)) {
                        add(
                            VerifiedOperatorAccount(
                                accountName = cleanAccount,
                                email = email,
                                passwordHash = passwordHash,
                                isVerified = isVerified,
                                verifiedAtEpochMs = verifiedAt
                            )
                        )
                    }
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "Corrupted verified accounts JSON; skipping entry", err)
        }.getOrDefault(emptyList())
    }

    private fun loadVerifiedAccounts(): List<VerifiedOperatorAccount> {
        val rawStored = safeGetString(KEY_VERIFIED_ACCOUNTS_JSON, null)
        val primary = parseVerifiedAccountsJson(decodeSensitiveJson(rawStored))
        val backupRaw = safeGetString(KEY_ACCOUNT_EMAIL_BACKUP_JSON, null)
        val backup = parseVerifiedAccountsJson(backupRaw)

        val merged = LinkedHashMap<String, VerifiedOperatorAccount>()
        primary.forEach { acc -> merged[acc.accountName.lowercase(Locale.US)] = acc }
        backup.forEach { acc ->
            val key = acc.accountName.lowercase(Locale.US)
            if (!merged.containsKey(key)) {
                merged[key] = acc
            }
        }

        // Last-resort fallback: preserve active account + email mapping if both lists were empty
        val activeName = OperatorIdentityFormatter.sanitizeUsername(
            safeGetString(KEY_ACTIVE_ACCOUNT_NAME, "").orEmpty()
        )
        val activeEmail = safeGetString(KEY_ACTIVE_OPERATOR_EMAIL, "").orEmpty().trim().lowercase(Locale.US)
        if (activeName.isNotEmpty() && activeEmail.contains("@") && !merged.containsKey(activeName)) {
            merged[activeName] = VerifiedOperatorAccount(
                accountName = activeName,
                email = activeEmail,
                passwordHash = "",
                isVerified = true,
                verifiedAtEpochMs = 0L
            )
        }

        return merged.values.toList().take(MAX_SAVED_ACCOUNTS)
    }

    private fun loadPendingVerification(): PendingEmailVerification? {
        val rawStored = safeGetString(KEY_PENDING_VERIFICATION_JSON, null) ?: return null
        val raw = decodeSensitiveJson(rawStored) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val rawAccount = obj.optString("accountName", "").ifBlank {
                obj.optString("username", "").ifBlank { obj.optString("operatorId", "") }
            }
            val cleanAccount = OperatorIdentityFormatter.sanitizeUsername(rawAccount)
            val email = obj.optString("email", "").trim().lowercase(Locale.US)
            if (cleanAccount.isEmpty() || email.isEmpty()) return null
            val passwordHash = obj.optString("passwordHash", "").trim()
            val sentAt = obj.optLong("sentAtEpochMs", 0L)
            PendingEmailVerification(
                accountName = cleanAccount,
                email = email,
                passwordHash = passwordHash,
                sentAtEpochMs = sentAt
            )
        }.onFailure { err ->
            Log.w(TAG, "Corrupted pending verification JSON; clearing stale entry", err)
            safeEdit { remove(KEY_PENDING_VERIFICATION_JSON) }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "OperatorPreferences"
        private const val PREFS_NAME = "sounding_operator_session_prefs"
        private const val KEY_ACTIVE_ACCOUNT_NAME = "active_account_name"
        private const val KEY_ACTIVE_OPERATOR_EMAIL = "active_operator_email"
        private const val KEY_IS_LOGGED_IN = "session_is_logged_in"
        private const val KEY_USER_EXPLICITLY_LOGGED_OUT = "session_explicit_logout"
        private const val KEY_SAVED_ACCOUNT_NAMES_JSON = "saved_account_names_json"
        private const val KEY_VERIFIED_ACCOUNTS_JSON = "verified_operator_accounts_json"
        private const val KEY_ACCOUNT_EMAIL_BACKUP_JSON = "verified_account_email_backup_json"
        private const val KEY_PENDING_VERIFICATION_JSON = "pending_email_verification_json"
        private const val KEY_LEGACY_ACTIVE_OPERATOR_ID = "active_operator_id"
        private const val KEY_LEGACY_ACTIVE_USERNAME = "active_operator_username"
        private const val KEY_LEGACY_ACTIVE_SHIFT = "active_operator_shift"
        private const val KEY_LEGACY_SHIFT_GROUP = "active_shift_group"
        private const val KEY_LEGACY_SAVED_OPERATOR_IDS_JSON = "saved_operator_ids_json"
        private const val KEY_LEGACY_SAVED_USERNAMES_JSON = "saved_operator_usernames_json"
        private const val MAX_SAVED_ACCOUNTS = 12
    }
}
