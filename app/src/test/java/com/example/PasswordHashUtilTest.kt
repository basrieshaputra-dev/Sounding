package com.example

import com.example.data.local.PasswordHashUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHashUtilTest {

    @Test
    fun hashAndVerify_matchesOriginalPasswordAndRejectsWrongPassword() {
        val raw = "Plant@2026"
        val storedHash = PasswordHashUtil.hashPassword(raw)

        assertTrue(PasswordHashUtil.isValidStoredHash(storedHash))
        assertTrue(storedHash.startsWith("pbkdf2_sha256$"))
        assertFalse(storedHash.contains(raw))

        assertTrue(PasswordHashUtil.verifyPassword(raw, storedHash))
        assertFalse(PasswordHashUtil.verifyPassword("WrongPass!", storedHash))
        assertFalse(PasswordHashUtil.verifyPassword("", storedHash))
        assertFalse(PasswordHashUtil.verifyPassword(raw.lowercase(), storedHash))
    }

    @Test
    fun hashPassword_usesUniqueRandomSaltEachTime() {
        val h1 = PasswordHashUtil.hashPassword("secret123")
        val h2 = PasswordHashUtil.hashPassword("secret123")

        assertNotEquals(h1, h2)
        assertTrue(PasswordHashUtil.verifyPassword("secret123", h1))
        assertTrue(PasswordHashUtil.verifyPassword("secret123", h2))
    }

    @Test
    fun verifyPassword_rejectsMalformedOrTamperedHash() {
        val valid = PasswordHashUtil.hashPassword("operator99")
        val tampered = valid.dropLast(1) + if (valid.last() == 'a') 'b' else 'a'

        assertFalse(PasswordHashUtil.verifyPassword("operator99", tampered))
        assertFalse(PasswordHashUtil.verifyPassword("operator99", ""))
        assertFalse(PasswordHashUtil.verifyPassword("operator99", "plain_text_password"))
        assertFalse(PasswordHashUtil.verifyPassword("operator99", "pbkdf2_sha256\$100\$00\$00"))
        assertFalse(PasswordHashUtil.isValidStoredHash("pbkdf2_sha256\$100\$abcd\$ef01"))
    }
}
