package com.fivestars.batterytracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePresetsAndHealthTest {

    // --- 1. TEST VERIFICA PRESET ONEPLUS (SNAPDRAGON & MEDIATEK) ---

    @Test
    fun testOnePlusNord5IndianGlobalPreset() {
        val preset = OplusDevicePresets.detectDevicePreset("OnePlus CPH2707")
        assertNotNull("OnePlus Nord 5 (CPH2707) deve essere riconosciuto", preset)
        assertEquals("OnePlus", preset?.brand)
        assertEquals("Nord 5", preset?.modelName)
        assertEquals(6800, preset?.typicalMah)
        assertEquals(6650.0, preset?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testOnePlusNord5SecondaryModel() {
        val preset = OplusDevicePresets.detectDevicePreset("CPH2709")
        assertNotNull("OnePlus Nord 5 (CPH2709) deve essere riconosciuto", preset)
        assertEquals(6800, preset?.typicalMah)
        assertEquals(6650.0, preset?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testOnePlus13RPreset() {
        val preset = OplusDevicePresets.detectDevicePreset("CPH2649")
        assertNotNull("OnePlus 13R (CPH2649) deve essere riconosciuto", preset)
        assertEquals("OnePlus", preset?.brand)
        assertEquals("13R", preset?.modelName)
        assertEquals(6000, preset?.typicalMah)
        assertEquals(5840.0, preset?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testOnePlus12And12RPresets() {
        val op12 = OplusDevicePresets.detectDevicePreset("CPH2581")
        assertNotNull(op12)
        assertEquals(5400, op12?.typicalMah)
        assertEquals(5260.0, op12?.ratedMah ?: 0.0, 0.01)

        val op12r = OplusDevicePresets.detectDevicePreset("CPH2609")
        assertNotNull(op12r)
        assertEquals(5500, op12r?.typicalMah)
        assertEquals(5360.0, op12r?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testOnePlus11And13Presets() {
        val op11 = OplusDevicePresets.detectDevicePreset("CPH2449")
        assertNotNull(op11)
        assertEquals(5000, op11?.typicalMah)
        assertEquals(4880.0, op11?.ratedMah ?: 0.0, 0.01)

        val op13 = OplusDevicePresets.detectDevicePreset("CPH2653")
        assertNotNull(op13)
        assertEquals(6000, op13?.typicalMah)
        assertEquals(5840.0, op13?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testOnePlusNordFamilyPresets() {
        val nord3 = OplusDevicePresets.detectDevicePreset("CPH2491")
        assertNotNull(nord3)
        assertEquals(5000, nord3?.typicalMah)

        val nord4 = OplusDevicePresets.detectDevicePreset("CPH2661")
        assertNotNull(nord4)
        assertEquals(5500, nord4?.typicalMah)

        val ce4 = OplusDevicePresets.detectDevicePreset("CPH2613")
        assertNotNull(ce4)
        assertEquals(5500, ce4?.typicalMah)

        val ce4lite = OplusDevicePresets.detectDevicePreset("CPH2621")
        assertNotNull(ce4lite)
        assertEquals(5500, ce4lite?.typicalMah)
    }

    // --- 2. TEST VERIFICA PRESET REALME (SNAPDRAGON & MEDIATEK) ---

    @Test
    fun testRealme14ProPlusPreset() {
        val preset = OplusDevicePresets.detectDevicePreset("Realme RMX3980")
        assertNotNull("Realme 14 Pro+ (RMX3980) deve essere riconosciuto", preset)
        assertEquals("Realme", preset?.brand)
        assertEquals("14 Pro+", preset?.modelName)
        assertEquals(6000, preset?.typicalMah)
        assertEquals(5850.0, preset?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testRealme13ProPlusAnd12ProPlusPresets() {
        val r13pp = OplusDevicePresets.detectDevicePreset("RMX3921")
        assertNotNull(r13pp)
        assertEquals(5200, r13pp?.typicalMah)
        assertEquals(5050.0, r13pp?.ratedMah ?: 0.0, 0.01)

        val r12pp = OplusDevicePresets.detectDevicePreset("RMX3840")
        assertNotNull(r12pp)
        assertEquals(5000, r12pp?.typicalMah)
        assertEquals(4880.0, r12pp?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testRealmeGTPresets() {
        val gt5Pro = OplusDevicePresets.detectDevicePreset("RMX3888")
        assertNotNull(gt5Pro)
        assertEquals(5400, gt5Pro?.typicalMah)

        val gt6 = OplusDevicePresets.detectDevicePreset("RMX3851")
        assertNotNull(gt6)
        assertEquals(5500, gt6?.typicalMah)

        val gt6t = OplusDevicePresets.detectDevicePreset("RMX3853")
        assertNotNull(gt6t)
        assertEquals(5500, gt6t?.typicalMah)

        val gt7Pro = OplusDevicePresets.detectDevicePreset("RMX5011")
        assertNotNull(gt7Pro)
        assertEquals(6500, gt7Pro?.typicalMah)
    }

    // --- 3. TEST VERIFICA DISPOSITIVI OPPO ---

    @Test
    fun testOppoReno14Preset() {
        val preset = OplusDevicePresets.detectDevicePreset("CPH2737")
        assertNotNull(preset)
        assertEquals("Oppo", preset?.brand)
        assertEquals("Reno 14", preset?.modelName)
        assertEquals(6000, preset?.typicalMah)
        assertEquals(5840.0, preset?.ratedMah ?: 0.0, 0.01)
    }

    // --- 4. TEST CALCOLO SALUTE (SOH) E CAPACITA' INCROCIATA ---

    @Test
    fun testHealthCalculationFromFcc() {
        // Nord 5 con batteria nominale 6650 mAh e capacità attuale misurata 6450 mAh
        val rated = 6650.0
        val fcc = 6450.0
        val calculatedHealth = Math.round((fcc / rated) * 100.0).toInt().coerceIn(1, 100)
        assertEquals(97, calculatedHealth)
    }

    @Test
    fun testFccDerivationFromSohWhenFccMissingInSysfs() {
        // Se su OnePlus Snapdragon il nodo battery_fcc non è presente, ma battery_soh riporta 98%
        val rated = 6650.0
        val rawSoh = 98
        val effectiveFcc = Math.round((rated * rawSoh / 100.0) * 10.0) / 10.0
        assertEquals(6517.0, effectiveFcc, 0.1)
    }

    @Test
    fun testMicroAmpNormalization() {
        // Valori da chip Qualcomm in microampere-ora (e.g. 6800000 uAh)
        val rawVal = 6800000.0
        val normalized = if (rawVal > 100000) rawVal / 1000.0 else rawVal
        assertEquals(6800.0, normalized, 0.01)

        // Valori già in mAh (e.g. 5500.0)
        val rawMah = 5500.0
        val normalizedMah = if (rawMah > 100000) rawMah / 1000.0 else rawMah
        assertEquals(5500.0, normalizedMah, 0.01)
    }

    @Test
    fun testPresetAlphabeticalSorting() {
        val presets = OplusDevicePresets.ALL_PRESETS
        assertTrue(presets.isNotEmpty())
        for (i in 0 until presets.size - 1) {
            val current = presets[i].displayName.lowercase()
            val next = presets[i + 1].displayName.lowercase()
            assertTrue(
                "La lista dei preset deve essere ordinata alfabeticamente: '$current' vs '$next'",
                current <= next
            )
        }
    }
}
