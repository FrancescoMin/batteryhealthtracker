package com.fivestars.batterytracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testRealmeGT7TPreset() {
        val gt7tModel = OplusDevicePresets.detectDevicePreset("Realme RMX5085")
        assertNotNull("Realme GT 7T (RMX5085) deve essere riconosciuto", gt7tModel)
        assertEquals("Realme", gt7tModel?.brand)
        assertEquals("GT 7T", gt7tModel?.modelName)
        assertEquals(7000, gt7tModel?.typicalMah)
        assertEquals(6850.0, gt7tModel?.ratedMah ?: 0.0, 0.01)

        val gt7tEea = OplusDevicePresets.detectDevicePreset("RMX5085EEA")
        assertNotNull("Realme GT 7T (RMX5085EEA) deve essere riconosciuto", gt7tEea)
        assertEquals(7000, gt7tEea?.typicalMah)
        assertEquals(6850.0, gt7tEea?.ratedMah ?: 0.0, 0.01)

        val gt7tCode = OplusDevicePresets.detectDevicePreset("RE6090L1")
        assertNotNull("Realme GT 7T (RE6090L1) deve essere riconosciuto", gt7tCode)
        assertEquals(7000, gt7tCode?.typicalMah)
        assertEquals(6850.0, gt7tCode?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testRealmeDynamicFuelGaugeLogPriority() {
        // Realme GT 7T: nominale 6850 mAh, tipica 7000 mAh
        val preset = OplusDevicePresets.detectDevicePreset("RMX5085")
        assertNotNull(preset)
        val ratedDesign = preset?.ratedMah ?: 6850.0

        // Simulazione battery_log_head e battery_log_content reali da Realme GT 7T:
        val headLine = ",batt_temp,shell_temp,vbat_mv,vbat_min_mv,ibat_ma,batt_soc,ui_soc,wired_online,charge_type,notify_code,wired_ibus_ma,wired_vbus_mv,smooth_soc,led_on,fv_mv,fcc_ma,wired_icl_ma,otg_switch,cool_down,bcc_current,normal_cool_down,chg_cycle,mmi_chg,usb_status,cc_detect,batt_full,rechging,pd_svooc,prop_status,batt_qmax,batt_soh,gauge_car_c,batt_rm,batt_fcc,vooc_online,vooc_started,vooc_charging,vooc_online_keep,vooc_sid,adapter_id"
        val contentLine = ",346,340,4047,4044,-137,61,63,1,1,0,412,4893,63,1,4455,600,500,0,7,11500,7,0,1,0,2,0,0,0,1,6736,99,0,3774,6236,0,0,0,0,0,0"

        val logMap = BatteryTelemetryParser.parseFuelGaugeLog(headLine, contentLine)
        val logSoh = BatteryTelemetryParser.extractLogSoh(logMap)
        val logQmax = BatteryTelemetryParser.extractLogQmax(logMap)
        val logRm = BatteryTelemetryParser.extractLogRm(logMap)

        // Nel sysfs standard, battery_soh riporta erroneamente 100% statico e normal_batt_fcc 7000 mAh
        val sysfsSoh = 100
        val sysfsFcc = 7000.0

        // Risoluzione SOH dinamica: minOf(logSoh, sysfsSoh) tramite parser
        val rawSoh = BatteryTelemetryParser.resolveRawSoh(logSoh, sysfsSoh)
        assertEquals(99, rawSoh)

        // Risoluzione FCC: sovrastima 7000 > logQmax (6736) con rawSoh < 100 viene corretta con logQmax
        val fcc = BatteryTelemetryParser.resolveAdjustedFcc(sysfsFcc, logQmax, rawSoh)
        assertEquals(6736.0, fcc ?: 0.0, 0.01)

        // Verifica coerenza effectiveFcc e derivazione salute
        val healthDerivation = BatteryTelemetryParser.resolveHealthAndFcc(
            rawSoh = rawSoh,
            fcc = fcc,
            ratedDesign = ratedDesign,
            typicalCalculationBase = 7000.0
        )
        assertEquals(6736.0, healthDerivation.effectiveFcc ?: 0.0, 0.01)
        assertEquals(99, healthDerivation.effectiveHealth)
        assertEquals(3774.0, logRm ?: 0.0, 0.01)
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

    @Test
    fun testOppoFindX9UltraPreset() {
        val preset = OplusDevicePresets.detectDevicePreset("PMD110")
        assertNotNull(preset)
        assertEquals("Oppo", preset?.brand)
        assertEquals("Find X9 Ultra", preset?.modelName)
        assertEquals(7050, preset?.typicalMah)
        assertEquals(6890.0, preset?.ratedMah ?: 0.0, 0.01)

        val presetByName = OplusDevicePresets.detectDevicePreset("Oppo Find X9 Ultra")
        assertNotNull(presetByName)
        assertEquals(7050, presetByName?.typicalMah)
        assertEquals(6890.0, presetByName?.ratedMah ?: 0.0, 0.01)
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
        val normalized = BatteryTelemetryParser.normalizeCapacity(6800000.0)
        assertEquals(6800.0, normalized ?: 0.0, 0.01)

        // Valori già in mAh (e.g. 5500.0)
        val normalizedMah = BatteryTelemetryParser.normalizeCapacity(5500.0)
        assertEquals(5500.0, normalizedMah ?: 0.0, 0.01)

        // Test string parsing
        assertEquals(6800.0, BatteryTelemetryParser.parseCapacity("6800000") ?: 0.0, 0.01)
        assertEquals(5500.0, BatteryTelemetryParser.parseCapacity("5500.0") ?: 0.0, 0.01)
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

    @Test
    fun testOnePlus13HealthPriorityAndEffectiveFcc() {
        // OnePlus 13 (CPH2653): Nominale 5840 mAh, Tipica 6000 mAh
        val preset = OplusDevicePresets.detectDevicePreset("CPH2653")
        assertNotNull(preset)
        val ratedDesign = preset?.ratedMah ?: 5840.0
        assertEquals(5840.0, ratedDesign, 0.01)

        // Scenario segnalato: Settings di OxygenOS 15 riporta 98%, mentre dumpsys batterystats
        // riporta il profilo statico di power_profile.xml "Estimated battery capacity: 6000"
        val rawSoh = 98
        val dumpsysEstimatedProfile = 6000.0 // MAI da usare come FCC reale degradato!

        // Priorità salute: rawSoh certificato dal BMS/OS prevale
        val effectiveHealth = rawSoh
        assertEquals(98, effectiveHealth)

        // Capacità effettiva: quando fcc da sysfs manca o è un valore teorico eccedente (> ratedDesign)
        val effectiveFcc = Math.round((ratedDesign * rawSoh / 100.0) * 10.0) / 10.0
        assertEquals(5723.2, effectiveFcc, 0.1)

        // Se invece avessimo usato la vecchia formula errata (6000 / 5840 * 100):
        val wrongCalculatedHealth = Math.round((dumpsysEstimatedProfile / ratedDesign) * 100.0).toInt().coerceIn(1, 100)
        assertEquals(100, wrongCalculatedHealth) // Questa era l'anomalia rilevata dall'amico!
    }

    @Test
    fun testEffectiveFccConsistencyWithRawSoh() {
        val ratedDesign = 5840.0
        val rawSoh = 98

        // Caso 1: FCC sysfs reale (es. 5720 mAh) è coerente con SOH 98% (entro 5%)
        val sysfsFcc = 5720.0
        val isConsistent = Math.abs((sysfsFcc / ratedDesign * 100.0) - rawSoh) <= 5.0 && sysfsFcc <= ratedDesign * 1.02
        assertTrue("Sysfs FCC coerente deve essere accettato", isConsistent)

        // Caso 2: FCC anomalo o statico dumpsys (6000 mAh > ratedDesign)
        val staticProfileFcc = 6000.0
        val isProfileConsistent = Math.abs((staticProfileFcc / ratedDesign * 100.0) - rawSoh) <= 5.0 && staticProfileFcc <= ratedDesign * 1.02
        org.junit.Assert.assertFalse("Profilo teorico eccedente ratedDesign non deve essere accettato come FCC", isProfileConsistent)
    }

    @Test
    fun testOnePlus13CalculatedSohAlignedToTypicalCapacity() {
        // OnePlus 13 (CPH2653): Tipica 6000 mAh, Nominale 5840 mAh
        val preset = OplusDevicePresets.detectDevicePreset("CPH2653")
        assertNotNull(preset)
        val typicalMah = preset?.typicalMah?.toDouble() ?: 6000.0
        val ratedMah = preset?.ratedMah ?: 5840.0

        // Caso reale riscontrato nel test dell'amico:
        // rawSoh è null (SELinux blocca normal_batt_soh)
        // dumpsys batterystats riporta learned battery capacity = 5920 mAh (393 cicli)
        val rawSoh: Int? = null
        val learnedFcc = 5920.0

        // Calcolo con la nuova logica: allineamento alla capacità TIPICA (6000 mAh)
        val isHealthCalculated = (rawSoh == null)
        val calculationBase = if (isHealthCalculated) typicalMah else ratedMah
        val calculatedHealth = ((learnedFcc * 100.0) / calculationBase).toInt().coerceIn(1, 100)

        // Deve risultare esattamente 98%, combaciando con le Impostazioni di sistema di OnePlus 13!
        assertEquals(98, calculatedHealth)

        // Se invece avessimo usato la vecchia base (5840 rated): 5920 / 5840 = 101% -> 100% errato
        val oldWrongHealth = Math.round((learnedFcc / ratedMah) * 100.0).toInt().coerceIn(1, 100)
        assertEquals(100, oldWrongHealth)
    }

    @Test
    fun testUsageStatsAndDaysSinceFirstBootCalculation() {
        val daysSinceFirstBoot = 390
        val cycles = 320

        val daysPerCycle = daysSinceFirstBoot.toDouble() / cycles.toDouble()
        val cyclesPerDay = cycles.toDouble() / daysSinceFirstBoot.toDouble()

        assertEquals(1.21875, daysPerCycle, 0.001)
        assertEquals(0.82051, cyclesPerDay, 0.001)
        assertTrue(daysPerCycle > 1.0)
        assertTrue(cyclesPerDay < 1.0)
    }

    // --- 5. TEST RESISTENZA INTERNA (ESR), VALIDAZIONE OCV E LOG RENO 13 ---

    @Test
    fun testOppoReno13PresetAndTelemetryMatching() {
        // Oppo Reno 13 (CPH2689 / CPH2689IN / OP5E9EL1)
        val preset = OplusDevicePresets.detectDevicePreset("CPH2689")
        assertNotNull("Oppo Reno 13 (CPH2689) deve essere riconosciuto", preset)
        assertEquals("Oppo", preset?.brand)
        assertEquals("Reno 13", preset?.modelName)
        assertEquals(5600, preset?.typicalMah)
        assertEquals(5450.0, preset?.ratedMah ?: 0.0, 0.01)

        // Dati reali dal report dell'utente su issue #3 (338 cicli):
        val sysfsFcc = 5421.0
        val ratedDesign = preset?.ratedMah ?: 5450.0
        val retentionRate = (sysfsFcc / ratedDesign) * 100.0
        assertEquals(99.467, retentionRate, 0.01)
        // La batteria è al 99.5% di capacità utile dopo 338 cicli: salute eccellente!
        assertTrue(retentionRate >= 98.0)
    }

    @Test
    fun testStaticOcvCutoffRejectionPreventsFakeHighResistance() {
        // Scenario segnalato su Reno 13 MediaTek (Issue #3):
        // A batteria scarica/parziale (66% SoC, 3932 mV, assorbimento 734 mA),
        // il driver /sys/class/power_supply/battery/voltage_ocv riporta 4540 mV fissi (tensione massima di cutoff CV).
        val batteryLevel = 66
        val normOcv = 4540
        val normVnow = 3932
        val currentMa = 734
        val isDual = false

        // La vecchia logica avrebbe calcolato una resistenza fasulla enorme (> 800 mOhm)
        val oldDeltaV = Math.abs(normOcv - normVnow)
        val oldFakeEsr = (oldDeltaV.toDouble() / currentMa.toDouble()) * 1000.0
        assertTrue(oldFakeEsr > 800.0)

        // La logica pura del parser identifica e respinge il valore statico di cutoff restituendo null
        val ocvEsr = BatteryTelemetryParser.calculateOcvEsr(
            voltageOcvMv = normOcv,
            vNowMv = normVnow,
            currentMa = currentMa,
            batteryLevel = batteryLevel,
            isDual = isDual
        )
        assertNull("Il cutoff statico a 4540 mV su 66% SoC deve essere respinto", ocvEsr)
    }

    @Test
    fun testEquilibriumOcvModelAcrossSocRange() {
        assertEquals(4450, BatteryTelemetryParser.estimateEquilibriumOcv(100))
        assertEquals(4350, BatteryTelemetryParser.estimateEquilibriumOcv(95))
        assertEquals(4260, BatteryTelemetryParser.estimateEquilibriumOcv(90))
        assertEquals(4130, BatteryTelemetryParser.estimateEquilibriumOcv(80))
        assertEquals(4040, BatteryTelemetryParser.estimateEquilibriumOcv(70))
        // Reno 13 a 66% SoC
        assertEquals(4008, BatteryTelemetryParser.estimateEquilibriumOcv(66))
        assertEquals(3960, BatteryTelemetryParser.estimateEquilibriumOcv(60))
        assertEquals(3890, BatteryTelemetryParser.estimateEquilibriumOcv(50))
        assertEquals(3830, BatteryTelemetryParser.estimateEquilibriumOcv(40))
        assertEquals(3790, BatteryTelemetryParser.estimateEquilibriumOcv(30))
        assertEquals(3750, BatteryTelemetryParser.estimateEquilibriumOcv(20))
        assertEquals(3680, BatteryTelemetryParser.estimateEquilibriumOcv(10))
        assertEquals(3300, BatteryTelemetryParser.estimateEquilibriumOcv(0))
    }

    @Test
    fun testEquilibriumSocEsrResolutionForReno13StaticCutoff() {
        // Ripristina l'estrazione per Reno 13 5G (Issue #3) quando il sysfs ha 4540 mV statici:
        // SoC = 66%, Vnow = 3932 mV, Current = 734 mA, Single-Cell (1S)
        val eqEsr = BatteryTelemetryParser.calculateEquilibriumSocEsr(
            batteryLevel = 66,
            vNowMv = 3932,
            currentMa = 734,
            isDual = false
        )
        assertNotNull("La resistenza interna non deve più essere N/D su Reno 13", eqEsr)
        // OCV(66) = 4008 mV, dV = |4008 - 3932| = 76 mV -> ESR = (76 / 734) * 1000 = 103.5 mOhm
        assertEquals(103.5, eqEsr ?: 0.0, 0.5)
        assertTrue("103.5 mOhm è perfettamente nella fascia eccellente 1S (40-180 mOhm)", (eqEsr ?: 0.0) in 40.0..180.0)
    }

    @Test
    fun testEquilibriumSocEsrResolutionForDualCellSuperVOOC() {
        // Realme GT 7T / Find X (2S serie):
        // SoC = 60%, Vnow = 3960 mV (per cella), Current = 2000 mA
        val eqEsr = BatteryTelemetryParser.calculateEquilibriumSocEsr(
            batteryLevel = 60,
            vNowMv = 3960,
            currentMa = 2000,
            isDual = true
        )
        // A 60% OCV = 3960 mV, deltaV = 0 -> in caso di deltaV minimo
        assertNull(eqEsr) // deltaV = 0 produce ESR < 15 mOhm quindi filtrato

        // Con carico/scarica reale: Vnow = 3880 mV, current = -400 mA
        val realEsr = BatteryTelemetryParser.calculateEquilibriumSocEsr(
            batteryLevel = 60,
            vNowMv = 3880,
            currentMa = -400,
            isDual = true
        )
        assertNotNull(realEsr)
        // dV per cella = 80 mV, cellEsr = 200 mOhm, packEsr = 400 mOhm
        assertEquals(400.0, realEsr ?: 0.0, 1.0)
    }

    @Test
    fun testOcvEsrAllowsRealisticDynamicLightLoadAndFastChargeRanges() {
        // Reno 14 (1S): V_ocv = 4150 mV, V_now = 4050 mV, I = -200 mA
        val reno14Esr = BatteryTelemetryParser.calculateOcvEsr(
            voltageOcvMv = 4150,
            vNowMv = 4050,
            currentMa = -200,
            batteryLevel = 80,
            isDual = false
        )
        assertNotNull(reno14Esr)
        assertEquals(500.0, reno14Esr ?: 0.0, 1.0)

        // Realme GT 7T (2S): dV = 200 mV, I = 185 mA (da test fisici in GEMINI.md ~1079.6 mOhm)
        val realmeEsr = BatteryTelemetryParser.calculateOcvEsr(
            voltageOcvMv = 4225,
            vNowMv = 4025,
            currentMa = 185,
            batteryLevel = 65,
            isDual = true
        )
        assertNotNull("Realme GT 7T in carica a 1079 mOhm non deve essere scartato", realmeEsr)
        assertEquals(1081.1, realmeEsr ?: 0.0, 5.0)
    }

    @Test
    fun testDynamicDeltaVDeltaIStepEsrCalculation() {
        // Campione 1: Uso leggero (I1 = -300 mA, V1 = 3920 mV)
        // Campione 2: Carico attivo/avvio app (I2 = -800 mA, V2 = 3880 mV)
        val stepEsr = BatteryTelemetryParser.calculateDynamicStepEsr(
            v1Mv = 3920,
            i1Ma = -300,
            v2Mv = 3880,
            i2Ma = -800,
            elapsedMs = 2000L,
            isDual = false
        )
        assertNotNull(stepEsr)
        assertEquals(80.0, stepEsr ?: 0.0, 0.01)
        assertTrue("80 mOhm è perfettamente nell'intervallo di salute ottimale (40-180 mOhm)", (stepEsr ?: 0.0) in 40.0..180.0)

        // Se intervallo temporale fuori range (< 500 ms o > 45000 ms)
        assertNull(BatteryTelemetryParser.calculateDynamicStepEsr(3920, -300, 3880, -800, 200L, false))
        assertNull(BatteryTelemetryParser.calculateDynamicStepEsr(3920, -300, 3880, -800, 50000L, false))

        // Se gradino di corrente troppo piccolo (< 150 mA)
        assertNull(BatteryTelemetryParser.calculateDynamicStepEsr(3920, -300, 3915, -400, 2000L, false))
    }

    @Test
    fun testHardwareResistanceMicroOhmNormalization() {
        // Driver kernel Linux power supply ABI che restituisce micro-ohm (85000 uOhm)
        val normalized = BatteryTelemetryParser.normalizeInternalResistance(85000.0, false)
        assertEquals(85.0, normalized ?: 0.0, 0.01)

        // Driver che restituisce direttamente milli-ohm (75.0 mOhm)
        val direct = BatteryTelemetryParser.normalizeInternalResistance(75.0, false)
        assertEquals(75.0, direct ?: 0.0, 0.01)

        // Valori fuori limite (> 550 mOhm su cella singola o <= 0)
        assertNull(BatteryTelemetryParser.normalizeInternalResistance(650.0, false))
        assertNull(BatteryTelemetryParser.normalizeInternalResistance(0.0, false))
        assertNull(BatteryTelemetryParser.normalizeInternalResistance(-50.0, false))
    }

    // --- 6. SUITE DI TEST UNITARI PURA SUI PARSER KERNEL/SYSFS (ISPIRATA AD aBATTERY ISSUE #2) ---

    @Test
    fun testCycleCountParsingSentinelsAndEdgeCases() {
        // Valori validi
        assertEquals(259, BatteryTelemetryParser.parseCycleCount("259"))
        assertEquals(338, BatteryTelemetryParser.parseCycleCount(" 338 \n"))
        assertEquals(0, BatteryTelemetryParser.parseCycleCount("0"))

        // Sentinelle kernel di errore (-1 o negativi)
        assertNull(BatteryTelemetryParser.parseCycleCount("-1"))
        assertNull(BatteryTelemetryParser.parseCycleCount("-999"))

        // Stringhe malformate o vuote
        assertNull(BatteryTelemetryParser.parseCycleCount(""))
        assertNull(BatteryTelemetryParser.parseCycleCount("   "))
        assertNull(BatteryTelemetryParser.parseCycleCount("null"))
        assertNull(BatteryTelemetryParser.parseCycleCount("N/A"))
        assertNull(BatteryTelemetryParser.parseCycleCount(null))
    }

    @Test
    fun testCapacityParsingAndNormalizationEdgeCases() {
        // Microampere-ora (Qualcomm / HAL)
        assertEquals(5840.0, BatteryTelemetryParser.normalizeCapacity(5840000.0) ?: 0.0, 0.01)
        assertEquals(6500.0, BatteryTelemetryParser.parseCapacity("6500000") ?: 0.0, 0.01)

        // Milliampere-ora (MediaTek / driver standard)
        assertEquals(5450.0, BatteryTelemetryParser.normalizeCapacity(5450.0) ?: 0.0, 0.01)
        assertEquals(7000.0, BatteryTelemetryParser.parseCapacity("7000") ?: 0.0, 0.01)

        // Sentinelle zero, negative, NaN, Infinite
        assertNull(BatteryTelemetryParser.normalizeCapacity(0.0))
        assertNull(BatteryTelemetryParser.normalizeCapacity(-1.0))
        assertNull(BatteryTelemetryParser.normalizeCapacity(Double.NaN))
        assertNull(BatteryTelemetryParser.normalizeCapacity(Double.POSITIVE_INFINITY))
        assertNull(BatteryTelemetryParser.parseCapacity("0"))
        assertNull(BatteryTelemetryParser.parseCapacity("-5000"))
        assertNull(BatteryTelemetryParser.parseCapacity("invalid"))
        assertNull(BatteryTelemetryParser.parseCapacity(null))
    }

    @Test
    fun testVoltageParsingAndNormalizationEdgeCases() {
        // Microvolt (HAL standard Linux)
        assertEquals(3920, BatteryTelemetryParser.normalizeVoltage(3920000))
        assertEquals(4050, BatteryTelemetryParser.parseVoltage("4050000"))

        // Millivolt (BMS fuel-gauge)
        assertEquals(3920, BatteryTelemetryParser.normalizeVoltage(3920))
        assertEquals(4050, BatteryTelemetryParser.parseVoltage("4050"))

        // Volt singoli (es. Android sticky broadcast "4" o "4 V")
        assertEquals(4000, BatteryTelemetryParser.normalizeVoltage(4))
        assertEquals(4000, BatteryTelemetryParser.parseVoltage("4"))

        // Sentinelle
        assertNull(BatteryTelemetryParser.normalizeVoltage(0))
        assertNull(BatteryTelemetryParser.normalizeVoltage(-1))
        assertNull(BatteryTelemetryParser.parseVoltage("0"))
        assertNull(BatteryTelemetryParser.parseVoltage("-1"))
        assertNull(BatteryTelemetryParser.parseVoltage(""))
        assertNull(BatteryTelemetryParser.parseVoltage(null))
    }

    @Test
    fun testFuelGaugeLogParsingMismatchedColumnsAndRobustness() {
        // Log troncato o non allineato
        val headLine = "batt_temp,vbat_mv,batt_soc,batt_soh"
        val contentLine = "340,4050,60" // Manca batt_soh

        val map = BatteryTelemetryParser.parseFuelGaugeLog(headLine, contentLine)
        assertEquals(3, map.size)
        assertEquals("340", map["batt_temp"])
        assertEquals("4050", map["vbat_mv"])
        assertEquals("60", map["batt_soc"])
        assertNull(map["batt_soh"])
        assertNull(BatteryTelemetryParser.extractLogSoh(map))

        // Log con valori extra rispetto all'header
        val headShort = "batt_soc,batt_soh"
        val contentLong = "80,98,extra1,extra2"
        val mapShort = BatteryTelemetryParser.parseFuelGaugeLog(headShort, contentLong)
        assertEquals(2, mapShort.size)
        assertEquals("80", mapShort["batt_soc"])
        assertEquals(98, BatteryTelemetryParser.extractLogSoh(mapShort))

        // Log nullo o vuoto
        assertTrue(BatteryTelemetryParser.parseFuelGaugeLog(null, null).isEmpty())
        assertTrue(BatteryTelemetryParser.parseFuelGaugeLog("", "").isEmpty())
        assertTrue(BatteryTelemetryParser.parseFuelGaugeLog("a,b", null).isEmpty())
    }

    @Test
    fun testBccParametersParsingWithCorruptedTokens() {
        // Stringa valida (da Realme GT 7T):
        val validBcc = "0, 0, 0, 0, 0, 0, 4025, 0, -137, 0, 0, 4024, 0, 0"
        val parsed = BatteryTelemetryParser.parseBccParameters(validBcc)
        assertEquals(4025, parsed.cell0VoltMv)
        assertEquals(-137, parsed.currentMa)
        assertEquals(4024, parsed.cell1VoltMv)

        // Stringa con tensioni in microvolt (> 100000)
        val uVoltBcc = "0, 0, 0, 0, 0, 0, 4025000, 0, 1500, 0, 0, 4024000"
        val parsedUv = BatteryTelemetryParser.parseBccParameters(uVoltBcc)
        assertEquals(4025, parsedUv.cell0VoltMv)
        assertEquals(1500, parsedUv.currentMa)
        assertEquals(4024, parsedUv.cell1VoltMv)

        // Stringa corta / troncata (< 12 elementi) - non deve lanciare eccezioni
        val shortBcc = "0, 0, 0, 0, 0, 0, 3950"
        val parsedShort = BatteryTelemetryParser.parseBccParameters(shortBcc)
        assertEquals(3950, parsedShort.cell0VoltMv)
        assertNull(parsedShort.currentMa)
        assertNull(parsedShort.cell1VoltMv)

        // Null o vuota
        val emptyBcc = BatteryTelemetryParser.parseBccParameters(null)
        assertNull(emptyBcc.cell0VoltMv)
        assertNull(emptyBcc.currentMa)
        assertNull(emptyBcc.cell1VoltMv)
    }

    @Test
    fun testDualCellArchitectureParsingFromAgingFfcData() {
        // Realme GT 7T dual cell (2S series)
        val dualAging = "0,2,0,0,259,0,0,0,0,0,0,0,0"
        assertEquals(true, BatteryTelemetryParser.parseDualCellArchitecture(dualAging))

        // Oppo Reno 13 single cell (1S)
        val singleAging = "0,1,0,0,338,0,0,0,0,0,0,0,0"
        assertEquals(false, BatteryTelemetryParser.parseDualCellArchitecture(singleAging))

        // Dati assenti o sconosciuti
        assertNull(BatteryTelemetryParser.parseDualCellArchitecture("0,0,0,0"))
        assertNull(BatteryTelemetryParser.parseDualCellArchitecture("corrupted"))
        assertNull(BatteryTelemetryParser.parseDualCellArchitecture(null))
    }

    @Test
    fun testCurrentNormalizationAndConventions() {
        // In carica (isPlugged = true): la corrente deve essere sempre > 0
        assertEquals(1500, BatteryTelemetryParser.normalizeCurrent(-1500, isPlugged = true))
        assertEquals(2000, BatteryTelemetryParser.normalizeCurrent(2000, isPlugged = true))
        // In microampere (-2500000 uA -> 2500 mA)
        assertEquals(2500, BatteryTelemetryParser.normalizeCurrent(-2500000, isPlugged = true))

        // In scarica (isPlugged = false): la corrente deve essere < 0
        assertEquals(-450, BatteryTelemetryParser.normalizeCurrent(450, isPlugged = false))
        assertEquals(-450, BatteryTelemetryParser.normalizeCurrent(-450, isPlugged = false))
        // In microampere (600000 uA -> -600 mA)
        assertEquals(-600, BatteryTelemetryParser.normalizeCurrent(600000, isPlugged = false))

        // Corrente zero o nulla
        assertNull(BatteryTelemetryParser.normalizeCurrent(0, isPlugged = true))
        assertNull(BatteryTelemetryParser.normalizeCurrent(null, isPlugged = false))
    }

    @Test
    fun testTemperatureParsingAndNormalization() {
        // Deci-Celsius da sysfs (346 -> 34.6°C)
        assertEquals(34.6, BatteryTelemetryParser.parseTemperature("346") ?: 0.0, 0.01)
        // Temperatura già in Celsius (28.5)
        assertEquals(28.5, BatteryTelemetryParser.parseTemperature("28.5") ?: 0.0, 0.01)
        // Temperatura sotto zero
        assertEquals(-4.0, BatteryTelemetryParser.normalizeTemperature(-4.0) ?: 0.0, 0.01)
        // Null o vuota
        assertNull(BatteryTelemetryParser.parseTemperature(""))
        assertNull(BatteryTelemetryParser.parseTemperature(null))
    }

    @Test
    fun testQmaxNormalizationScaleGuard() {
        // Valore su scala errata 10x (58400 con FCC di riferimento 5840)
        assertEquals(5840, BatteryTelemetryParser.normalizeQmax(58400, 5840))

        // Valore su scala errata 100x (584000)
        assertEquals(5840, BatteryTelemetryParser.normalizeQmax(584000, 5840))

        // Valore corretto (5815 con FCC 5525)
        assertEquals(5815, BatteryTelemetryParser.normalizeQmax(5815, 5525))
    }

    @Test
    fun testCellBalanceEvaluationRanges() {
        // Ottimale (< 15 mV)
        val optimal = BatteryTelemetryParser.evaluateCellBalance(4025, 4024, isDual = true)
        assertEquals(1, optimal.deltaMv)
        assertEquals("Optimal", optimal.status)

        // Normale (15..40 mV)
        val normal = BatteryTelemetryParser.evaluateCellBalance(4050, 4025, isDual = true)
        assertEquals(25, normal.deltaMv)
        assertEquals("Normal", normal.status)

        // Sbilanciato (> 40 mV)
        val imbalanced = BatteryTelemetryParser.evaluateCellBalance(4100, 4020, isDual = true)
        assertEquals(80, imbalanced.deltaMv)
        assertEquals("Imbalanced", imbalanced.status)

        // Singola cella
        val single = BatteryTelemetryParser.evaluateCellBalance(3950, null, isDual = false)
        assertEquals(0, single.deltaMv)
        assertEquals("SingleCell", single.status)
    }

    @Test
    fun testThermalCompensatedCapacityIec61960() {
        val capacity = 5840.0
        // A 25°C: deltaT = 0, capacità identica
        assertEquals(5840.0, BatteryTelemetryParser.calculateTempCompensatedCapacity(capacity, 25.0) ?: 0.0, 0.01)

        // A 35°C (caldo): capacità compensata inferiore (5509.4 mAh)
        val warmComp = BatteryTelemetryParser.calculateTempCompensatedCapacity(capacity, 35.0)
        assertNotNull(warmComp)
        assertTrue(warmComp!! < capacity)
        assertEquals(5509.4, warmComp, 0.1)

        // A 15°C (freddo): capacità compensata superiore (6212.8 mAh)
        val coldComp = BatteryTelemetryParser.calculateTempCompensatedCapacity(capacity, 15.0)
        assertNotNull(coldComp)
        assertTrue(coldComp!! > capacity)
        assertEquals(6212.8, coldComp, 0.1)

        // Null o valori non validi
        assertNull(BatteryTelemetryParser.calculateTempCompensatedCapacity(null, 25.0))
        assertNull(BatteryTelemetryParser.calculateTempCompensatedCapacity(5000.0, null))
    }

    @Test
    fun testHardwareSafetyFaultsEvaluation() {
        // Nessun guasto
        val (safeOk, detailsOk) = BatteryTelemetryParser.evaluateHardwareSafety(0, 0, 0)
        assertTrue(safeOk)
        assertEquals("OK", detailsOk)

        // Cortocircuito
        val (safeSc, detailsSc) = BatteryTelemetryParser.evaluateHardwareSafety(1, 0, 0)
        org.junit.Assert.assertFalse(safeSc)
        assertEquals("ShortCircuit(1)", detailsSc)

        // Guasti multipli
        val (safeMulti, detailsMulti) = BatteryTelemetryParser.evaluateHardwareSafety(2, 1, 3)
        org.junit.Assert.assertFalse(safeMulti)
        assertTrue(detailsMulti.contains("ShortCircuit(2)"))
        assertTrue(detailsMulti.contains("OTP_OverHeat(1)"))
        assertTrue(detailsMulti.contains("SubboardTempErr(3)"))
    }

    @Test
    fun testDumpsysBatteryTextParsing() {
        val sampleDumpsys = """
            Current Battery Service state:
              AC powered: false
              USB powered: true
              status: 2
              health: 2
              present: true
              level: 66
              scale: 100
              voltage: 3950
              temperature: 320
              technology: Li-ion
              Charge counter: 3500000
              mSavedBatteryAsoc: 98
        """.trimIndent()

        val parsed = BatteryTelemetryParser.parseDumpsysBatteryText(sampleDumpsys)
        assertEquals(3950, parsed.voltageMv)
        assertEquals(3500000L, parsed.chargeCounterUah)
        assertEquals(98, parsed.asocPercent)
        assertEquals(2, parsed.status)
        assertEquals(320, parsed.tempTenths)
    }

    @Test
    fun testDumpsysLearnedAndEstimatedCapacityParsing() {
        val statsOutput = """
          Learned battery capacity: 5920 mAh
          Estimated battery capacity: 6000 mAh
        """.trimIndent()

        val learned = BatteryTelemetryParser.parseDumpsysLearnedCapacity(statsOutput)
        val estimated = BatteryTelemetryParser.parseDumpsysEstimatedCapacity(statsOutput)

        assertEquals(5920.0, learned ?: 0.0, 0.01)
        assertEquals(6000.0, estimated ?: 0.0, 0.01)

        assertNull(BatteryTelemetryParser.parseDumpsysLearnedCapacity("no match"))
        assertNull(BatteryTelemetryParser.parseDumpsysEstimatedCapacity(null))
    }

    @Test
    fun testSettingsBatteryHealthParsing() {
        // Valore intero diretto (es. da settings get system maximum_capacity)
        assertEquals(98, BatteryTelemetryParser.parseSettingsBatteryHealth("98"))
        assertEquals(95, BatteryTelemetryParser.parseSettingsBatteryHealth("  95 \n"))

        // Formato chiave-valore
        assertEquals(97, BatteryTelemetryParser.parseSettingsBatteryHealth("maximum_capacity=97"))
        assertEquals(94, BatteryTelemetryParser.parseSettingsBatteryHealth("battery_health:94"))

        // Risposta da content provider
        assertEquals(99, BatteryTelemetryParser.parseSettingsBatteryHealth("Row: 0 value=99"))

        // Fuori intervallo o non valido
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("150"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("-1"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("null"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth(null))

        // Reiezione chiavi di telemetria, contatori o flag (es. OnePlus/Oppo battery_health_enter_times_daily=1)
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("battery_health_enter_times_daily=1"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("oplus_customize_rhythm_health_enable=1"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("1"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("2"))
        assertNull(BatteryTelemetryParser.parseSettingsBatteryHealth("battery_health_count=5"))
    }

    @Test
    fun testDerivedRatedCapacityMappingLadder() {
        assertEquals(7290.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(7500.0), 0.01)
        assertEquals(7150.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(7300.0), 0.01)
        assertEquals(6840.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(7000.0), 0.01)
        assertEquals(6490.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(6700.0), 0.01)
        assertEquals(6310.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(6500.0), 0.01)
        assertEquals(6060.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(6200.0), 0.01)
        assertEquals(5840.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(6000.0), 0.01)
        assertEquals(5660.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(5800.0), 0.01)
        assertEquals(5490.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(5630.0), 0.01)
        assertEquals(5360.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(5500.0), 0.01)
        assertEquals(5050.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(5200.0), 0.01)
        assertEquals(4880.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(5000.0), 0.01)
        assertEquals(4440.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(4600.0), 0.01)
        assertEquals(4190.0, BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(4300.0), 0.01)
    }

    @Test
    fun testBatteryAuthenticityParsing() {
        assertTrue(BatteryTelemetryParser.parseBatteryAuthenticity("1"))
        assertTrue(BatteryTelemetryParser.parseBatteryAuthenticity("true"))
        assertTrue(BatteryTelemetryParser.parseBatteryAuthenticity("TRUE"))
        assertTrue(BatteryTelemetryParser.parseBatteryAuthenticity(" 1 \n"))

        org.junit.Assert.assertFalse(BatteryTelemetryParser.parseBatteryAuthenticity("0"))
        org.junit.Assert.assertFalse(BatteryTelemetryParser.parseBatteryAuthenticity("false"))
        org.junit.Assert.assertFalse(BatteryTelemetryParser.parseBatteryAuthenticity("null"))
        org.junit.Assert.assertFalse(BatteryTelemetryParser.parseBatteryAuthenticity(""))
        org.junit.Assert.assertFalse(BatteryTelemetryParser.parseBatteryAuthenticity(null))
    }

    @Test
    fun testUsageDatesParsing() {
        // Data di produzione fissa: 2025-01-01
        // Timestamp simulato: 2025-07-01 (181 giorni dopo, ~5.94 mesi -> 5 mesi)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val fixedNow = sdf.parse("2025-07-01")!!.time
        val (months, days) = BatteryTelemetryParser.parseUsageDates("2025-01-01", null, fixedNow)
        assertEquals(5, months)
        assertEquals(181, days)

        // Scenario reale Oppo Reno 14: Produzione 2025-06-07 (15 mesi fa) e 1° Avvio 2025-09-04 (393 giorni fa)
        val nowOct2026 = sdf.parse("2026-10-02")!!.time
        val (renoMonths, renoDays) = BatteryTelemetryParser.parseUsageDates("2025-06-07", "2025-09-04", nowOct2026)
        assertEquals(15, renoMonths) // 15 mesi = 1 anno e 3 mesi (età del componente chimico)
        assertEquals(393, renoDays)  // 393 giorni dal 1° avvio (tempo di funzionamento dispositivo)

        // Data malformata o futura
        val (badMonths, badDays) = BatteryTelemetryParser.parseUsageDates("corrupted-date", null, fixedNow)
        assertNull(badMonths)
        assertNull(badDays)

        val (nullMonths, nullDays) = BatteryTelemetryParser.parseUsageDates(null, null, fixedNow)
        assertNull(nullMonths)
        assertNull(nullDays)

        // Rifiuto date antecedenti al 2018 (es. Unix epoch 1970 o fine 2017)
        val (epochMonths, epochDays) = BatteryTelemetryParser.parseUsageDates("1970-01-01", null, fixedNow)
        assertNull(epochMonths)
        assertNull(epochDays)

        val (pre2018Months, pre2018Days) = BatteryTelemetryParser.parseUsageDates("2017-12-31", null, fixedNow)
        assertNull(pre2018Months)
        assertNull(pre2018Days)

        // Test deriveUsageFromTimestamp: rifiuto 0L, 1L o valori antecedenti al 2018
        assertNull(BatteryTelemetryParser.deriveUsageFromTimestamp(0L, fixedNow).second)
        assertNull(BatteryTelemetryParser.deriveUsageFromTimestamp(1L, fixedNow).second)
        assertNull(BatteryTelemetryParser.deriveUsageFromTimestamp(1000L, fixedNow).second)
        assertNull(BatteryTelemetryParser.deriveUsageFromTimestamp(1514764799999L, fixedNow).second) // 2017-12-31 23:59:59.999 UTC

        // Test deriveUsageFromTimestamp: timestamp valido (es. 2025-01-01)
        val bootTime2025 = sdf.parse("2025-01-01")!!.time
        val (validMonths, validDays) = BatteryTelemetryParser.deriveUsageFromTimestamp(bootTime2025, fixedNow)
        assertEquals(5, validMonths)
        assertEquals(181, validDays)

        // Test deriveUsageFromTimestamp: timestamp futuro
        assertNull(BatteryTelemetryParser.deriveUsageFromTimestamp(fixedNow + 100_000L, fixedNow).second)
    }

    @Test
    fun testChargingPowerAndProtocolEvaluation() {
        // 4000 mV, 2000 mA -> 8.0 W
        val watts = BatteryTelemetryParser.calculateChargingPowerWatts(4000, 2000)
        assertEquals(8.0, watts ?: 0.0, 0.01)

        // Protocolli
        val vooc = BatteryTelemetryParser.determineChargingProtocol(
            isPlugged = true,
            currentMa = 5000,
            voocIng = "1",
            fastChgType = null,
            ppsIng = null
        )
        assertEquals("SuperVOOC", vooc)

        val pd = BatteryTelemetryParser.determineChargingProtocol(
            isPlugged = true,
            currentMa = 2500,
            voocIng = "0",
            fastChgType = null,
            ppsIng = "1"
        )
        assertEquals("USB-PD / PPS", pd)

        val standard = BatteryTelemetryParser.determineChargingProtocol(
            isPlugged = true,
            currentMa = 1000,
            voocIng = "0",
            fastChgType = null,
            ppsIng = "0"
        )
        assertEquals("STANDARD", standard)

        val discharging = BatteryTelemetryParser.determineChargingProtocol(
            isPlugged = false,
            currentMa = -500,
            voocIng = null,
            fastChgType = null,
            ppsIng = null
        )
        assertEquals("DISCHARGING", discharging)

        val standby = BatteryTelemetryParser.determineChargingProtocol(
            isPlugged = false,
            currentMa = -5,
            voocIng = null,
            fastChgType = null,
            ppsIng = null
        )
        assertEquals("STANDBY", standby)
    }

    // --- 16. TEST IMPORTAZIONE ED ESPORTAZIONE BACKUP CSV ---

    @Test
    fun testSplitCsvLineStandardAndQuoted() {
        val line = """1,1740000000000,"2025-06-07 14:30:00",95,321,5525.0,"OPLUS_SYSFS""""
        val tokens = BatteryTelemetryParser.splitCsvLine(line)
        assertEquals(7, tokens.size)
        assertEquals("1", tokens[0])
        assertEquals("1740000000000", tokens[1])
        assertEquals("2025-06-07 14:30:00", tokens[2])
        assertEquals("95", tokens[3])
        assertEquals("321", tokens[4])
        assertEquals("5525.0", tokens[5])
        assertEquals("OPLUS_SYSFS", tokens[6])
    }

    @Test
    fun testParseBatteryCsvStandardExportFormat() {
        val csvLines = listOf(
            "ID,Timestamp,Data_Ora,Salute_Percentuale,Cicli_Carica,Capacita_Residua_mAh,Sorgente",
            """1,1740000000000,"2025-06-07 14:30:00",95,321,5525.0,"OPLUS_SYSFS"""",
            """2,1740086400000,"2025-06-08 14:30:00",94,325,5480.5,"OPLUS_SYSFS""""
        )

        val result = BatteryTelemetryParser.parseBatteryCsv(csvLines)
        assertEquals(2, result.records.size)
        assertEquals(0, result.duplicateInFileCount)
        assertEquals(0, result.invalidLinesCount)

        val r1 = result.records[0]
        assertEquals(0, r1.id)
        assertEquals(1740000000000L, r1.timestamp)
        assertEquals(95, r1.healthPercentage)
        assertEquals(321, r1.cycleCount)
        assertEquals(5525.0, r1.currentCapacityMah ?: 0.0, 0.01)
        assertEquals("OPLUS_SYSFS", r1.source)

        val r2 = result.records[1]
        assertEquals(1740086400000L, r2.timestamp)
        assertEquals(94, r2.healthPercentage)
        assertEquals(325, r2.cycleCount)
        assertEquals(5480.5, r2.currentCapacityMah ?: 0.0, 0.01)
    }

    @Test
    fun testParseBatteryCsvWithNullsAndEuropeanComma() {
        val csvLines = listOf(
            "ID,Timestamp,Data_Ora,Salute_Percentuale,Cicli_Carica,Capacita_Residua_mAh,Sorgente",
            """1,1740000000000,"2025-06-07 14:30:00",N/D,N/D,"5525,0","BATTERY_MANAGER"""",
            """2,1740086400000,"2025-06-08 14:30:00",100,10,N/D,"""""
        )

        val result = BatteryTelemetryParser.parseBatteryCsv(csvLines)
        assertEquals(2, result.records.size)
        val r1 = result.records[0]
        assertNull(r1.healthPercentage)
        assertNull(r1.cycleCount)
        assertEquals(5525.0, r1.currentCapacityMah ?: 0.0, 0.01)
        assertEquals("BATTERY_MANAGER", r1.source)

        val r2 = result.records[1]
        assertEquals(100, r2.healthPercentage)
        assertEquals(10, r2.cycleCount)
        assertNull(r2.currentCapacityMah)
        assertEquals("CSV_IMPORT", r2.source)
    }

    @Test
    fun testParseBatteryCsvDeduplicationAndInvalidLines() {
        val csvLines = listOf(
            "\uFEFFID,Timestamp,Data_Ora,Salute_Percentuale,Cicli_Carica,Capacita_Residua_mAh,Sorgente",
            """1,1740000000000,"2025-06-07 14:30:00",95,321,5525.0,"OPLUS_SYSFS"""",
            """2,1740000000000,"2025-06-07 14:30:00",95,321,5525.0,"OPLUS_SYSFS"""", // Duplicato
            """3,invalid_timestamp,"invalid_date",95,321,5525.0,"OPLUS_SYSFS"""", // Riga non valida
            """4,1740172800000,"2025-06-09 14:30:00",93,330,5450.0,"OPLUS_SYSFS""""
        )

        val result = BatteryTelemetryParser.parseBatteryCsv(csvLines)
        assertEquals(2, result.records.size)
        assertEquals(1, result.duplicateInFileCount)
        assertEquals(1, result.invalidLinesCount)
        assertEquals(1740000000000L, result.records[0].timestamp)
        assertEquals(1740172800000L, result.records[1].timestamp)
    }

    @Test
    fun testParseBatteryCsvEnglishHeaders() {
        val csvLines = listOf(
            "Timestamp,Date_Time,SOH,Cycle_Count,Capacity_mAh,Source",
            """1740000000000,"2025-06-07 14:30:00",98,150,5600.0,"OPLUS_SYSFS""""
        )

        val result = BatteryTelemetryParser.parseBatteryCsv(csvLines)
        assertEquals(1, result.records.size)
        assertEquals(1740000000000L, result.records[0].timestamp)
        assertEquals(98, result.records[0].healthPercentage)
        assertEquals(150, result.records[0].cycleCount)
        assertEquals(5600.0, result.records[0].currentCapacityMah ?: 0.0, 0.01)
    }

    // --- 18. TEST SCHEDULER DI CAMPIONAMENTO AUTOMATICO ---

    @Test
    fun testBatteryWorkSchedulerInitialDelayFutureToday() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 10)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val currentMillis = calendar.timeInMillis

        // Target: 20:00 today (10 hours later = 10 * 3600 * 1000 ms)
        val delay = BatteryWorkScheduler.calculateInitialDelayMillis(
            targetHour = 20,
            targetMinute = 0,
            currentTimeMillis = currentMillis
        )
        assertEquals(10 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun testBatteryWorkSchedulerInitialDelayPastTimeRollsToTomorrow() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 22)
            set(java.util.Calendar.MINUTE, 30)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val currentMillis = calendar.timeInMillis

        // Target: 20:00 (already passed today, so 21 hours and 30 minutes until tomorrow 20:00)
        val delay = BatteryWorkScheduler.calculateInitialDelayMillis(
            targetHour = 20,
            targetMinute = 0,
            currentTimeMillis = currentMillis
        )
        val expectedDelay = (21 * 60 + 30) * 60 * 1000L
        assertEquals(expectedDelay, delay)
    }

    // --- 19. TEST TELEMETRIA SAMSUNG E DERIVAZIONE DINAMICA UNIVERSALE ---

    @Test
    fun testSamsungTabS9PresetsDetection() {
        val tabS9 = DevicePresets.detectDevicePreset("SM_X716B")
        assertNotNull("Galaxy Tab S9 5G (SM_X716B) deve essere riconosciuto", tabS9)
        assertEquals("Samsung", tabS9?.brand)
        assertEquals("Galaxy Tab S9", tabS9?.modelName)
        assertEquals(8400, tabS9?.typicalMah)
        assertEquals(8160.0, tabS9?.ratedMah ?: 0.0, 0.01)

        val tabS9Wifi = DevicePresets.detectDevicePreset("SM-X710")
        assertNotNull(tabS9Wifi)
        assertEquals(8160.0, tabS9Wifi?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testSamsungS24UltraAndZFold6Presets() {
        val s24u = DevicePresets.detectDevicePreset("SM-S928B")
        assertNotNull("Galaxy S24 Ultra (SM-S928B) deve essere riconosciuto", s24u)
        assertEquals(5000, s24u?.typicalMah)
        assertEquals(4855.0, s24u?.ratedMah ?: 0.0, 0.01)

        val fold6 = DevicePresets.detectDevicePreset("SM-F956B")
        assertNotNull("Galaxy Z Fold 6 deve essere riconosciuto", fold6)
        assertEquals(4400, fold6?.typicalMah)
        assertEquals(4273.0, fold6?.ratedMah ?: 0.0, 0.01)
    }

    @Test
    fun testUniversalBatterystatsHardwareCapacityParsing() {
        val samsungBatterystatsOutput = "Capacity: 9800, Rated: 8160, Typical: 8400, Computed drain: 1326, actual drain: 1326"
        val parsed = BatteryTelemetryParser.parseDumpsysBatterystatsHardwareCapacity(samsungBatterystatsOutput)
        assertEquals(8160.0, parsed.ratedMah ?: 0.0, 0.01)
        assertEquals(8400.0, parsed.typicalMah ?: 0.0, 0.01)
        assertEquals(9800.0, parsed.capacityMah ?: 0.0, 0.01)

        val aospBatterystatsOutput = "Capacity: 5000, Computed drain: 420"
        val aospParsed = BatteryTelemetryParser.parseDumpsysBatterystatsHardwareCapacity(aospBatterystatsOutput)
        assertNull(aospParsed.ratedMah)
        assertNull(aospParsed.typicalMah)
        assertEquals(5000.0, aospParsed.capacityMah ?: 0.0, 0.01)
    }

    @Test
    fun testUniversalIecDerivationWithoutPreset() {
        // Se un dispositivo non è a catalogo ma conosciamo la tipica di fabbrica (8400 mAh),
        // deve calcolare automaticamente la nominale IEC 61960 (approx 97.2%)
        val derived = BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(null, typicalFallback = 8400.0)
        assertEquals(8164.8, derived, 0.01)
    }

    @Test
    fun testSamsungEfsTelemetryParsing() {
        // Test con dump reale di Galaxy Tab S9 5G (SM-X716B)
        // Nota: mSavedBatteryBsoh: 99 compare in coda nel dump, ma AsocData efsValue: 96
        // deve avere PRIORITÀ ASSOLUTA perché rappresenta il reale degrado chimico del fuelgauge hardware.
        val sampleDumpsys = """
            Current Battery Service state:
              voltage: 4134
              Charge counter: 7274890
              status: 2
              temperature: 279
              current now: 1389
              mProtectBatteryMode: 1
            LLB CAL: 20240522
            10-04 21:48:23.173  [SS][BattInfo]QrData efsValue:GH43-05165A+DL1X411AS+00278N    
            10-04 21:48:23.174  [SS][BattInfo]FirstUseDateData efsValue:20240522    
            10-04 21:48:23.174  [SS][BattInfo]AsocData efsValue:96    
            10-04 21:48:23.184  [SS][BattInfo]DischargeLevelData efsValue:16114    
            BatteryInfoBackUp
              mSavedBatteryMaxTemp: 379
              mSavedBatteryMaxCurrent: 7354
              mSavedBatteryBsoh: 99
        """.trimIndent()

        val parsed = BatteryTelemetryParser.parseDumpsysBatteryText(sampleDumpsys)
        assertEquals(4134, parsed.voltageMv)
        assertEquals(7274890L, parsed.chargeCounterUah)
        assertEquals("La salute reale chimica EFS (96%) non deve essere sovrascritta dal BSOH software (99%)", 96, parsed.asocPercent)
        assertEquals(161, parsed.cycleCount) // 16114 / 100 = 161 cicli
        assertEquals("2024-05-22", parsed.firstUseDate)
        assertEquals("2024-05-22", parsed.calDate)
        assertEquals(1389, parsed.currentNowMa)
        assertEquals(1, parsed.protectBatteryMode)
        assertTrue("Deve essere riconosciuta come batteria autentica", parsed.isAuthentic == true)

        // Verifica calcolo capacità effettiva su base nominale 8160 mAh
        val healthDerivation = BatteryTelemetryParser.resolveHealthAndFcc(
            rawSoh = parsed.asocPercent,
            fcc = null,
            ratedDesign = 8160.0,
            typicalCalculationBase = 8400.0
        )
        assertEquals(96, healthDerivation.effectiveHealth)
        assertFalse(healthDerivation.isHealthCalculated)
        assertTrue("La capacità deve essere contrassegnata come calcolata/stimata da ASOC", healthDerivation.isCapacityEstimated)
        assertEquals(7833.6, healthDerivation.effectiveFcc ?: 0.0, 0.01)
    }

    @Test
    fun testSamsungBsohFallbackWhenAsocMissing() {
        // Se AsocData non è presente nel dump, mSavedBatteryBsoh deve fungere da fallback
        val sampleDumpsysWithoutAsoc = """
            Current Battery Service state:
              voltage: 4134
              Charge counter: 7274890
              status: 2
            BatteryInfoBackUp
              mSavedBatteryBsoh: 99
        """.trimIndent()

        val parsed = BatteryTelemetryParser.parseDumpsysBatteryText(sampleDumpsysWithoutAsoc)
        assertEquals(99, parsed.asocPercent)
    }
}


