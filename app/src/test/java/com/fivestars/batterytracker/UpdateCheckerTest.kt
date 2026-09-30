package com.fivestars.batterytracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testSameVersionsAreEqual() {
        assertEquals(0, UpdateChecker.compareVersions("1.5", "1.5"))
        assertEquals(0, UpdateChecker.compareVersions("v1.5", "1.5"))
        assertEquals(0, UpdateChecker.compareVersions("v1.5.0", "1.5"))
        assertEquals(0, UpdateChecker.compareVersions("1.5", "v1.5.0"))
    }

    @Test
    fun testNewerMajorVersionDetected() {
        assertTrue(UpdateChecker.compareVersions("2.0", "1.5") > 0)
        assertTrue(UpdateChecker.compareVersions("v2.0", "1.5") > 0)
    }

    @Test
    fun testNewerMinorVersionDetected() {
        assertTrue(UpdateChecker.compareVersions("1.6", "1.5") > 0)
        assertTrue(UpdateChecker.compareVersions("v1.6", "1.5") > 0)
        assertTrue(UpdateChecker.compareVersions("1.10", "1.9") > 0)
    }

    @Test
    fun testNewerPatchVersionDetected() {
        assertTrue(UpdateChecker.compareVersions("1.5.1", "1.5") > 0)
        assertTrue(UpdateChecker.compareVersions("v1.5.2", "v1.5.1") > 0)
    }

    @Test
    fun testOlderVersionDetected() {
        assertTrue(UpdateChecker.compareVersions("1.4", "1.5") < 0)
        assertTrue(UpdateChecker.compareVersions("v1.4.9", "1.5") < 0)
        assertTrue(UpdateChecker.compareVersions("1.5", "1.6") < 0)
    }

    @Test
    fun testVersionWithPrefixAndSuffix() {
        assertTrue(UpdateChecker.compareVersions("release-1.6", "1.5") > 0)
        assertTrue(UpdateChecker.compareVersions("v1.6-alpha", "1.5") > 0)
    }
}
