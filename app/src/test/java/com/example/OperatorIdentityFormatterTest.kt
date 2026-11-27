package com.example

import com.example.data.firebase.EmailVerificationAuthHelper
import com.example.data.local.OperatorIdentityFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorIdentityFormatterTest {

    @Test
    fun sanitizeUsername_lowercasesStripsSpacesSymbolsAndLegacyShiftSuffixes() {
        assertEquals("basri", OperatorIdentityFormatter.sanitizeUsername("  Basri  "))
        assertEquals("basrisyaputra", OperatorIdentityFormatter.sanitizeUsername("Basri Syaputra"))
        assertEquals("arif_01.ok", OperatorIdentityFormatter.sanitizeUsername("Arif_01.OK!@#"))
        assertEquals("rizal", OperatorIdentityFormatter.sanitizeUsername("Rizal@shiftB"))
        assertEquals("basri", OperatorIdentityFormatter.sanitizeUsername("basri@shiftC"))
        assertEquals("", OperatorIdentityFormatter.sanitizeUsername("   !!!  "))
    }

    @Test
    fun ensureValidAccountName_alwaysProducesValidLowercaseAccount() {
        assertEquals("basri", OperatorIdentityFormatter.ensureValidAccountName("basri@shiftC"))
        assertEquals("basri", OperatorIdentityFormatter.ensureValidAccountName("Basri"))
        assertEquals("operator", OperatorIdentityFormatter.ensureValidAccountName("   "))
        assertTrue(OperatorIdentityFormatter.isValidAccountName("basri"))
        assertTrue(OperatorIdentityFormatter.isValidAccountName("arif.01_ok"))
        assertFalse(OperatorIdentityFormatter.isValidAccountName("Basri"))
        assertFalse(OperatorIdentityFormatter.isValidAccountName("basri@shiftC"))
        assertFalse(OperatorIdentityFormatter.isValidAccountName("basri syaputra"))
        assertFalse(OperatorIdentityFormatter.isValidAccountName(""))
    }

    @Test
    fun emailVerificationHelper_validatesEmailFormatCleanly() {
        assertEquals("basri@permatagroup.co.id", EmailVerificationAuthHelper.normalizeEmail("  Basri@PermataGroup.co.id  "))
        assertTrue(EmailVerificationAuthHelper.isValidEmail("Basri@PermataGroup.co.id"))
        assertTrue(EmailVerificationAuthHelper.isValidEmail("operator_01@gmail.com"))
        assertFalse(EmailVerificationAuthHelper.isValidEmail("bukan-email"))
        assertFalse(EmailVerificationAuthHelper.isValidEmail("a..b@domain.com"))
        assertFalse(EmailVerificationAuthHelper.isValidEmail(""))
    }
}
