package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic password hashing utility using PBKDF2-HMAC-SHA256 with a unique
 * 16-byte random salt per account and constant-time digest comparison.
 *
 * Stored format: pbkdf2_sha256$<iterations>$<hex_salt>$<hex_derived_key>
 * Plaintext passwords are never stored in SharedPreferences or Firestore.
 */
object PasswordHashUtil {

    private const val ALGORITHM_PRIMARY = "PBKDF2WithHmacSHA256"
    private const val ALGORITHM_FALLBACK = "PBKDF2WithHmacSHA1"
    private const val PREFIX_SHA256 = "pbkdf2_sha256"
    private const val PREFIX_SHA1 = "pbkdf2_sha1"
    private const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    fun hashPassword(password: String, iterations: Int = DEFAULT_ITERATIONS): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val safeIterations = iterations.coerceIn(10_000, 300_000)
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val (prefix, derived) = deriveKey(password, salt, safeIterations, preferSha256 = true)
        return "$prefix$$safeIterations$${bytesToHex(salt)}$${bytesToHex(derived)}"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isEmpty() || storedHash.isBlank()) return false
        val parts = storedHash.trim().split('$')
        if (parts.size != 4) return false

        val prefix = parts[0]
        if (prefix != PREFIX_SHA256 && prefix != PREFIX_SHA1) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations !in 10_000..500_000) return false

        val salt = hexToBytes(parts[2]) ?: return false
        val expectedHash = hexToBytes(parts[3]) ?: return false
        if (salt.size < 8 || expectedHash.size < 16) return false

        return runCatching {
            val algorithm = if (prefix == PREFIX_SHA256) ALGORITHM_PRIMARY else ALGORITHM_FALLBACK
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.size * 8)
            try {
                val computed = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
                MessageDigest.isEqual(computed, expectedHash)
            } finally {
                spec.clearPassword()
            }
        }.getOrDefault(false)
    }

    fun isValidStoredHash(hash: String): Boolean {
        if (hash.isBlank()) return false
        val parts = hash.trim().split('$')
        if (parts.size != 4) return false
        val prefix = parts[0]
        if (prefix != PREFIX_SHA256 && prefix != PREFIX_SHA1) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations !in 10_000..500_000) return false
        val salt = hexToBytes(parts[2]) ?: return false
        val dk = hexToBytes(parts[3]) ?: return false
        return salt.size >= 8 && dk.size >= 16
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        preferSha256: Boolean
    ): Pair<String, ByteArray> {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH_BITS)
        return try {
            if (preferSha256) {
                runCatching {
                    PREFIX_SHA256 to SecretKeyFactory.getInstance(ALGORITHM_PRIMARY)
                        .generateSecret(spec).encoded
                }.getOrElse {
                    PREFIX_SHA1 to SecretKeyFactory.getInstance(ALGORITHM_FALLBACK)
                        .generateSecret(spec).encoded
                }
            } else {
                PREFIX_SHA1 to SecretKeyFactory.getInstance(ALGORITHM_FALLBACK)
                    .generateSecret(spec).encoded
            }
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hex[i * 2].digitToIntOrNull(16) ?: return null
            val lo = hex[i * 2 + 1].digitToIntOrNull(16) ?: return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
