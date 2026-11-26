package com.example

import com.example.data.local.LicenseActivationResult
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseManagerTest {

    private class InMemoryLicenseStorage : LicenseStorage {
        private var firstLaunch: Long = 0L
        private var maxObserved: Long = 0L
        private var activated: Boolean = false
        private var activatedCode: String = ""
        private var activatedAt: Long = 0L
        private var usedCodes: Set<String> = emptySet()

        override fun getFirstLaunchEpochMs(): Long = firstLaunch
        override fun setFirstLaunchEpochMs(epochMs: Long) {
            firstLaunch = epochMs
        }

        override fun getMaxObservedEpochMs(): Long = maxObserved
        override fun setMaxObservedEpochMs(epochMs: Long) {
            maxObserved = epochMs
        }

        override fun isActivated(): Boolean = activated
        override fun getActivatedCode(): String = activatedCode
        override fun getActivatedAtEpochMs(): Long = activatedAt
        override fun getUsedCodes(): Set<String> = usedCodes

        override fun persistActivation(code: String, activatedAtEpochMs: Long, usedCodes: Set<String>) {
            activated = true
            activatedCode = code
            activatedAt = activatedAtEpochMs
            this.usedCodes = usedCodes.toSet()
        }

        override fun persistUsedCodes(usedCodes: Set<String>) {
            this.usedCodes = usedCodes.toSet()
        }

        fun resetActivationKeepingUsedCodes() {
            activated = false
            activatedCode = ""
            activatedAt = 0L
        }
    }

    @Test
    fun containsExactEightAuthorizedActivationCodes() {
        val expected = setOf(
            "SPTF-8472",
            "SPTF-3916",
            "SPTF-6259",
            "SPTF-5184",
            "SPTF-9347",
            "SPTF-2763",
            "SPTF-7528",
            "SPTF-4691"
        )
        assertEquals(expected, LicenseManager.VALID_ACTIVATION_CODES)
        assertEquals(8, LicenseManager.VALID_ACTIVATION_CODES.size)
    }

    @Test
    fun tracks60DayTrialExpirationAndPreventsClockRollback() {
        val storage = InMemoryLicenseStorage()
        val day0 = 1_700_000_000_000L
        var currentNow = day0
        val manager = LicenseManager(storage) { currentNow }

        // Day 0: 60 days remaining, not expired
        val initial = manager.refreshState()
        assertFalse(initial.isActivated)
        assertFalse(initial.isTrialExpired)
        assertEquals(60, initial.remainingTrialDays)

        // Day 59.5: 1 day remaining, still active
        currentNow = day0 + (59L * LicenseManager.MILLIS_PER_DAY) + (12L * 3_600_000L)
        val almostDone = manager.refreshState()
        assertFalse(almostDone.isTrialExpired)
        assertEquals(1, almostDone.remainingTrialDays)

        // Day 60 exact: expired!
        currentNow = day0 + (60L * LicenseManager.MILLIS_PER_DAY)
        val expired = manager.refreshState()
        assertTrue(expired.isTrialExpired)
        assertEquals(0, expired.remainingTrialDays)

        // Rolling system clock backwards cannot bypass expired 60-day trial
        currentNow = day0 + (5L * LicenseManager.MILLIS_PER_DAY)
        val afterRollback = manager.refreshState()
        assertTrue(afterRollback.isTrialExpired)
        assertEquals(0, afterRollback.remainingTrialDays)
    }

    @Test
    fun activatesPermanentlyWithUnusedValidCodeAndNeverExpiresAgain() {
        val storage = InMemoryLicenseStorage()
        val day0 = 1_700_000_000_000L
        var currentNow = day0
        val manager = LicenseManager(storage) { currentNow }

        // Expire trial after 65 days
        manager.refreshState()
        currentNow = day0 + (65L * LicenseManager.MILLIS_PER_DAY)
        assertTrue(manager.refreshState().isTrialExpired)

        // Lowercase + whitespace input still normalizes to SPTF-8472
        val result = manager.activateCode("  sptf-8472  ")
        assertTrue(result is LicenseActivationResult.Success)
        val success = result as LicenseActivationResult.Success
        assertEquals("SPTF-8472", success.code)
        assertTrue(success.state.isActivated)
        assertFalse(success.state.isTrialExpired)
        assertTrue("SPTF-8472" in success.state.usedCodes)

        // Even 10 years later, license remains permanently active and trial expired screen never returns
        currentNow = day0 + (3650L * LicenseManager.MILLIS_PER_DAY)
        val yearsLater = manager.refreshState()
        assertTrue(yearsLater.isActivated)
        assertFalse(yearsLater.isTrialExpired)
        assertEquals("SPTF-8472", yearsLater.activatedCode)
    }

    @Test
    fun rejectsAlreadyUsedCodeAndInvalidCode() {
        val storage = InMemoryLicenseStorage()
        val day0 = 1_700_000_000_000L
        val manager = LicenseManager(storage) { day0 }

        // Blank code rejected
        assertTrue(manager.activateCode("   ") is LicenseActivationResult.EmptyCode)

        // Unknown / unlisted code rejected
        val invalidResult = manager.activateCode("SPTF-0000")
        assertTrue(invalidResult is LicenseActivationResult.InvalidCode)
        assertFalse(manager.refreshState().isActivated)

        // Claim first valid code
        val firstClaim = manager.activateCode("SPTF-3916")
        assertTrue(firstClaim is LicenseActivationResult.Success)
        assertTrue("SPTF-3916" in storage.getUsedCodes())

        // Re-submitting the same code is rejected as AlreadyUsed
        val duplicateSameDevice = manager.activateCode("SPTF-3916")
        assertTrue(duplicateSameDevice is LicenseActivationResult.AlreadyUsed)

        // Even if activation flag were somehow reset, usedCodes history permanently blocks reusing SPTF-3916
        storage.resetActivationKeepingUsedCodes()
        val reuseAttempt = manager.activateCode("sptf-3916")
        assertTrue(reuseAttempt is LicenseActivationResult.AlreadyUsed)
        assertFalse(manager.refreshState().isActivated)

        // A different unused valid code from the list still works once
        val secondCode = manager.activateCode("SPTF-4691")
        assertTrue(secondCode is LicenseActivationResult.Success)
        assertEquals(setOf("SPTF-3916", "SPTF-4691"), storage.getUsedCodes())
    }
}
