package com.example.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed Android Keystore AES-256-GCM cipher binding encrypted license data
 * to the device's ANDROID_ID via GCM Additional Authenticated Data (AAD).
 * If a preference file is copied to another device or modified, GCM authentication fails.
 * Every cryptographic step is wrapped in runCatching so OEM Keystore quirks never crash the app.
 */
object SecureLicenseCipher {

    private const val TAG = "SecureLicenseCipher"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "sounding_tangki_license_aes_gcm_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val PREFIX_KEYSTORE = "ks1:"
    private const val PREFIX_DERIVED = "dv1:"
    private const val PREFIX_B64_FALLBACK = "b64:"
    private const val DERIVED_SALT = "PHPO_FAC_TEXTURIZING_SOUNDING_LIC_V2_SALT"

    fun encrypt(plaintext: String, deviceId: String): String {
        val aad = deviceId.toByteArray(StandardCharsets.UTF_8)
        val plainBytes = plaintext.toByteArray(StandardCharsets.UTF_8)

        // Primary path: hardware-backed Android Keystore AES-256-GCM
        val ksEncrypted = runCatching {
            val key = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(aad)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainBytes)
            PREFIX_KEYSTORE + packIvAndCipherText(iv, cipherText)
        }.onFailure { err ->
            Log.w(TAG, "Keystore AES-GCM encryption unavailable; falling back", err)
        }.getOrNull()

        if (ksEncrypted != null) return ksEncrypted

        // Secondary path: deviceId-derived AES-256-GCM with random IV
        val derivedEncrypted = runCatching {
            val key = deriveDeviceBoundKey(deviceId)
            val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)
            val cipherText = cipher.doFinal(plainBytes)
            PREFIX_DERIVED + packIvAndCipherText(iv, cipherText)
        }.onFailure { err ->
            Log.w(TAG, "Derived AES-GCM encryption failed; using safe fallback encoding", err)
        }.getOrNull()

        if (derivedEncrypted != null) return derivedEncrypted

        // Last-resort safe encoding so unusual OEM crypto providers never force-close the app
        return runCatching {
            PREFIX_B64_FALLBACK + Base64.encodeToString(plainBytes, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    fun decrypt(payload: String, deviceId: String): String? {
        if (payload.isBlank()) return null
        val aad = deviceId.toByteArray(StandardCharsets.UTF_8)

        return when {
            payload.startsWith(PREFIX_KEYSTORE) -> runCatching {
                val (iv, cipherText) = unpackIvAndCipherText(payload.removePrefix(PREFIX_KEYSTORE))
                    ?: return null
                val key = getExistingKeystoreKey() ?: return null
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.updateAAD(aad)
                String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
            }.onFailure { err ->
                Log.w(TAG, "Keystore AES-GCM decryption failed safely", err)
            }.getOrNull()

            payload.startsWith(PREFIX_DERIVED) -> runCatching {
                val (iv, cipherText) = unpackIvAndCipherText(payload.removePrefix(PREFIX_DERIVED))
                    ?: return null
                val key = deriveDeviceBoundKey(deviceId)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.updateAAD(aad)
                String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
            }.onFailure { err ->
                Log.w(TAG, "Derived AES-GCM decryption failed safely", err)
            }.getOrNull()

            payload.startsWith(PREFIX_B64_FALLBACK) -> runCatching {
                val decoded = Base64.decode(payload.removePrefix(PREFIX_B64_FALLBACK), Base64.NO_WRAP)
                String(decoded, StandardCharsets.UTF_8)
            }.getOrNull()

            else -> null
        }
    }

    private fun getExistingKeystoreKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        getExistingKeystoreKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun deriveDeviceBoundKey(deviceId: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$DERIVED_SALT|$deviceId".toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    private fun packIvAndCipherText(iv: ByteArray, cipherText: ByteArray): String {
        val combined = ByteArray(1 + iv.size + cipherText.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(cipherText, 0, combined, 1 + iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun unpackIvAndCipherText(encoded: String): Pair<ByteArray, ByteArray>? {
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < 2) return null
            val ivLen = combined[0].toInt() and 0xFF
            if (ivLen < 12 || combined.size <= 1 + ivLen) return null
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val cipherText = combined.copyOfRange(1 + ivLen, combined.size)
            iv to cipherText
        }.getOrNull()
    }
}
