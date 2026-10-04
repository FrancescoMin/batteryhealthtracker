package com.fivestars.batterytracker

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pure, isolated parsing and electrochemical normalization engine for battery telemetry.
 *
 * This object contains zero Android runtime dependencies (no Context, no BatteryManager, no Shizuku IPC)
 * ensuring 100% JVM testability across all sysfs drivers, BMS log streams, dumpsys dumps, and OEM edge cases.
 */
object BatteryTelemetryParser {

    data class BccParameters(
        val cell0VoltMv: Int?,
        val currentMa: Int?,
        val cell1VoltMv: Int?
    )

    data class CellBalanceResult(
        val deltaMv: Int,
        val status: String
    )

    data class HealthDerivation(
        val effectiveHealth: Int?,
        val isHealthCalculated: Boolean,
        val effectiveFcc: Double?,
        val displayDesignCapacity: Double
    )

    data class DumpsysParsedData(
        val voltageMv: Int? = null,
        val chargeCounterUah: Long? = null,
        val asocPercent: Int? = null,
        val status: Int? = null,
        val tempTenths: Int? = null,
        val currentNowMa: Int? = null,
        val cycleCount: Int? = null,
        val firstUseDate: String? = null,
        val calDate: String? = null,
        val qrData: String? = null,
        val protectBatteryMode: Int? = null,
        val isAuthentic: Boolean? = null
    )

    data class BatterystatsCapacityData(
        val ratedMah: Double? = null,
        val typicalMah: Double? = null,
        val capacityMah: Double? = null
    )

    data class CsvParseResult(
        val records: List<BatteryData>,
        val duplicateInFileCount: Int,
        val invalidLinesCount: Int
    )

    /**
     * Parses the dynamic CSV fuel-gauge log header and content rows from OPlus kernel
     * (/sys/class/oplus_chg/battery/battery_log_head and battery_log_content).
     */
    fun parseFuelGaugeLog(headLine: String?, contentLine: String?): Map<String, String> {
        if (headLine.isNullOrBlank() || contentLine.isNullOrBlank()) return emptyMap()
        val heads = headLine.split(',').map { it.trim() }
        val values = contentLine.split(',').map { it.trim() }
        return heads.zip(values).filter { it.first.isNotEmpty() }.toMap()
    }

    fun extractLogSoh(logMap: Map<String, String>): Int? {
        return logMap["batt_soh"]?.toIntOrNull()?.takeIf { it in 1..100 }
    }

    fun extractLogQmax(logMap: Map<String, String>): Double? {
        return logMap["batt_qmax"]?.toDoubleOrNull()?.takeIf { it > 1000.0 }
    }

    fun extractLogRm(logMap: Map<String, String>): Double? {
        return logMap["batt_rm"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    fun extractLogFcc(logMap: Map<String, String>): Double? {
        return logMap["batt_fcc"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    /**
     * Normalizes capacity values from microampere-hours (uAh) or milliampere-hours (mAh).
     * Rejects sentinels (<= 0, null, NaN).
     */
    fun normalizeCapacity(rawMahOrUah: Double?): Double? {
        if (rawMahOrUah == null || rawMahOrUah <= 0.0 || rawMahOrUah.isNaN() || rawMahOrUah.isInfinite()) {
            return null
        }
        return if (rawMahOrUah > 100_000.0) rawMahOrUah / 1000.0 else rawMahOrUah
    }

    fun parseCapacity(rawStr: String?): Double? {
        return rawStr?.trim()?.toDoubleOrNull()?.let { normalizeCapacity(it) }
    }

    /**
     * Normalizes voltage values from microvolts (uV) or millivolts (mV).
     * Rejects non-positive sentinels (<= 0, null).
     */
    fun normalizeVoltage(rawMvOrUv: Int?): Int? {
        if (rawMvOrUv == null || rawMvOrUv <= 0) return null
        return if (rawMvOrUv > 100_000) rawMvOrUv / 1000 else rawMvOrUv
    }

    fun parseVoltage(rawStr: String?): Int? {
        return rawStr?.trim()?.toIntOrNull()?.let { normalizeVoltage(it) }
    }

    /**
     * Parses cycle count integer, rejecting negative sentinel codes (e.g. -1).
     */
    fun parseCycleCount(rawStr: String?): Int? {
        val count = rawStr?.trim()?.toIntOrNull() ?: return null
        return if (count >= 0) count else null
    }

    /**
     * Normalizes Qmax register to prevent 10x or 100x scale mismatch anomalies.
     */
    fun normalizeQmax(rawQ: Int, fcc: Int?): Int {
        var q = rawQ
        val ref = fcc ?: 20000
        while (q >= ref * 2 && q > 0) {
            q /= 10
        }
        return q
    }

    /**
     * Identifies series dual-cell architecture (SuperVOOC) from aging_ffc_data.
     * Index 1 == "2" denotes 2S dual-cell series, "1" denotes 1S single-cell.
     */
    fun parseDualCellArchitecture(agingData: String?): Boolean? {
        val token = agingData?.split(',')?.getOrNull(1)?.trim()
        return when (token) {
            "2" -> true
            "1" -> false
            else -> null
        }
    }

    /**
     * Parses BCC (Battery Charge Controller) parameters CSV stream.
     * Index 6: cell0Volt (mV or uV)
     * Index 8: bccCurrent (mA)
     * Index 11: cell1Volt (mV or uV)
     */
    fun parseBccParameters(bccParms: String?): BccParameters {
        if (bccParms.isNullOrBlank()) return BccParameters(null, null, null)
        val parts = bccParms.split(',').map { it.trim() }
        val cell0 = parts.getOrNull(6)?.toIntOrNull()?.takeIf { it > 0 }
        val current = parts.getOrNull(8)?.toIntOrNull()
        val cell1 = parts.getOrNull(11)?.toIntOrNull()?.takeIf { it > 0 }
        return BccParameters(
            cell0VoltMv = normalizeVoltage(cell0),
            currentMa = current,
            cell1VoltMv = normalizeVoltage(cell1)
        )
    }

    /**
     * Normalizes electric current to standard convention:
     * > 0 during charge (net power into cell)
     * < 0 during discharge (net power consumption)
     */
    fun normalizeCurrent(rawCur: Int?, isPlugged: Boolean): Int? {
        if (rawCur == null || rawCur == 0) return null
        val normalized = if (Math.abs(rawCur) > 10_000) rawCur / 1000 else rawCur
        return when {
            isPlugged && normalized < 0 -> -normalized
            !isPlugged && normalized > 0 -> -normalized
            else -> normalized
        }
    }

    /**
     * Normalizes temperature from deci-Celsius (e.g. 346 for 34.6°C) or raw Celsius.
     */
    fun normalizeTemperature(rawTemp: Double?): Double? {
        if (rawTemp == null || rawTemp.isNaN() || rawTemp.isInfinite()) return null
        return if (rawTemp > 200.0) rawTemp / 10.0 else rawTemp
    }

    fun parseTemperature(rawStr: String?): Double? {
        return rawStr?.trim()?.toDoubleOrNull()?.let { normalizeTemperature(it) }
    }

    /**
     * Normalizes internal resistance from micro-ohms (uOhm) or milli-ohms (mOhm).
     * Validates against realistic electrochemical boundaries.
     */
    fun normalizeInternalResistance(rawRes: Double?, isDual: Boolean?): Double? {
        if (rawRes == null || rawRes <= 0.0 || rawRes.isNaN() || rawRes.isInfinite()) return null
        val normalizedRes = if (rawRes > 1000.0) rawRes / 1000.0 else rawRes
        val maxLimit = if (isDual == true) 900.0 else 550.0
        return if (normalizedRes in 15.0..maxLimit) {
            Math.round(normalizedRes * 10.0) / 10.0
        } else null
    }

    /**
     * Estimates the thermodynamic equilibrium Open Circuit Voltage (OCV in mV)
     * as a function of State of Charge (SoC % in [0, 100]) for high-density
     * Lithium-ion / Silicon-Carbon battery cells (nominal 3.85V-3.91V, cutoff 4.45V-4.50V).
     */
    fun estimateEquilibriumOcv(batteryLevel: Int): Int {
        val lvl = batteryLevel.coerceIn(0, 100)
        return when {
            lvl >= 100 -> 4450
            lvl >= 95 -> 4350 + ((lvl - 95) * 100) / 5
            lvl >= 90 -> 4260 + ((lvl - 90) * 90) / 5
            lvl >= 80 -> 4130 + ((lvl - 80) * 130) / 10
            lvl >= 70 -> 4040 + ((lvl - 70) * 90) / 10
            lvl >= 60 -> 3960 + ((lvl - 60) * 80) / 10
            lvl >= 50 -> 3890 + ((lvl - 50) * 70) / 10
            lvl >= 40 -> 3830 + ((lvl - 40) * 60) / 10
            lvl >= 30 -> 3790 + ((lvl - 30) * 40) / 10
            lvl >= 20 -> 3750 + ((lvl - 20) * 40) / 10
            lvl >= 10 -> 3680 + ((lvl - 10) * 70) / 10
            else -> 3300 + (lvl * 380) / 10
        }
    }

    /**
     * Calculates dynamic step ESR via pulse differential response: R = |dV| / |dI|
     */
    fun calculateDynamicStepEsr(
        v1Mv: Int,
        i1Ma: Int,
        v2Mv: Int,
        i2Ma: Int,
        elapsedMs: Long,
        isDual: Boolean?
    ): Double? {
        if (elapsedMs !in 500L..45_000L) return null
        val deltaI = Math.abs(i2Ma - i1Ma)
        if (deltaI < 120) return null
        val deltaV = Math.abs(v2Mv - v1Mv)
        val stepEsr = (deltaV.toDouble() / deltaI.toDouble()) * 1000.0
        val packStepEsr = if (isDual == true && v1Mv <= 4600 && v2Mv <= 4600) stepEsr * 2.0 else stepEsr
        val maxLimit = 3000.0
        return if (packStepEsr in 15.0..maxLimit) {
            Math.round(packStepEsr * 10.0) / 10.0
        } else null
    }

    /**
     * Fallback calculation of ESR using sysfs OCV with anti-static cutoff and scale guards.
     */
    fun calculateOcvEsr(
        voltageOcvMv: Int,
        vNowMv: Int,
        currentMa: Int,
        batteryLevel: Int?,
        isDual: Boolean?
    ): Double? {
        if (Math.abs(currentMa) < 40) return null
        val normOcv = if (voltageOcvMv > 5000 && vNowMv <= 4600) voltageOcvMv / 2 else voltageOcvMv
        val normVnow = if (vNowMv > 5000 && voltageOcvMv <= 4600) vNowMv / 2 else vNowMv

        val deltaV = Math.abs(normOcv - normVnow)
        val isStaticOcvCutoff = (batteryLevel ?: 100) < 90 && normOcv >= 4420 && deltaV > 250
        val maxAllowableDeltaV = if (isDual == true) 700 else 450

        if (!isStaticOcvCutoff && deltaV <= maxAllowableDeltaV) {
            val esr = (deltaV.toDouble() / Math.abs(currentMa).toDouble()) * 1000.0
            val maxLimit = 3000.0
            if (esr in 15.0..maxLimit) {
                return Math.round(esr * 10.0) / 10.0
            }
        }
        return null
    }

    /**
     * Calculates internal DC resistance based on the thermodynamic equilibrium OCV curve.
     * Used when the kernel sysfs node is a static float cutoff (e.g. 4540 mV on MediaTek Dimensity chips)
     * or when direct OCV registers are uncalibrated or absent.
     */
    fun calculateEquilibriumSocEsr(
        batteryLevel: Int,
        vNowMv: Int,
        currentMa: Int,
        isDual: Boolean?
    ): Double? {
        if (Math.abs(currentMa) < 40) return null
        val lvl = batteryLevel.coerceIn(1, 100)
        val eqOcv = estimateEquilibriumOcv(lvl)
        val normV = if (vNowMv > 5000 && eqOcv <= 4600) vNowMv / 2 else vNowMv
        val deltaV = Math.abs(eqOcv - normV)
        val cellEsr = (deltaV.toDouble() / Math.abs(currentMa).toDouble()) * 1000.0
        val packEsr = if (isDual == true && vNowMv <= 4600) cellEsr * 2.0 else cellEsr
        val maxLimit = 3000.0
        return if (packEsr in 15.0..maxLimit) {
            Math.round(packEsr * 10.0) / 10.0
        } else null
    }

    /**
     * Evaluates cell voltage balance for series dual-cell setups.
     */
    fun evaluateCellBalance(cell0Mv: Int?, cell1Mv: Int?, isDual: Boolean?): CellBalanceResult {
        if (isDual == true && cell0Mv != null && cell1Mv != null && cell1Mv > 0) {
            val delta = Math.abs(cell0Mv - cell1Mv)
            val status = when {
                delta < 15 -> "Optimal"
                delta <= 40 -> "Normal"
                else -> "Imbalanced"
            }
            return CellBalanceResult(delta, status)
        }
        return CellBalanceResult(0, "SingleCell")
    }

    /**
     * Compensates battery capacity to 25°C standard reference according to IEC 61960.
     */
    fun calculateTempCompensatedCapacity(capacityMah: Double?, tempCelsius: Double?): Double? {
        if (capacityMah == null || capacityMah <= 0.0 || capacityMah.isNaN() || capacityMah.isInfinite() ||
            tempCelsius == null || tempCelsius.isNaN() || tempCelsius.isInfinite()
        ) {
            return null
        }
        val deltaT = tempCelsius - 25.0
        val comp = capacityMah / (1.0 + 0.006 * deltaT)
        return Math.round(comp * 10.0) / 10.0
    }

    /**
     * Parses authentic battery status string.
     */
    fun parseBatteryAuthenticity(authStr: String?): Boolean {
        val trimmed = authStr?.trim() ?: return false
        return trimmed == "1" || trimmed.equals("true", ignoreCase = true)
    }

    // Timestamp minimo accettabile: 1° gennaio 2018 00:00:00 UTC (1514764800000L)
    const val MIN_VALID_BOOT_TIMESTAMP_MS = 1514764800000L
    const val MAX_VALID_BOOT_DAYS = 4000

    /**
     * Parses manufacturing date (yyyy-MM-dd) and first usage date.
     * Computes:
     * - component age in months (prioritizing manuDate for physical cell age, fallback to firstUsageDate)
     * - days since first usage (prioritizing firstUsageDate for device runtime, fallback to manuDate)
     * Enforces strict sanity checks: rejects dates prior to 2018-01-01 or in the future.
     */
    fun parseUsageDates(
        manuDate: String?,
        firstUsageDate: String?,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<Int?, Int?> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }

        fun parseDate(str: String?): Long? {
            if (str.isNullOrBlank()) return null
            return try {
                val parsed = sdf.parse(str)
                if (parsed != null && parsed.time in MIN_VALID_BOOT_TIMESTAMP_MS..nowMs) {
                    parsed.time
                } else null
            } catch (_: Exception) {
                null
            }
        }

        val manuTime = parseDate(manuDate)
        val usageTime = parseDate(firstUsageDate)

        // Età effettiva del componente: la cella chimica esiste dalla sua produzione
        val compTime = manuTime ?: usageTime
        val ageMonths = compTime?.let {
            val diffMs = nowMs - it
            if (diffMs > 0) {
                (diffMs / (1000L * 60 * 60 * 24 * 30.4375)).toInt()
            } else null
        }

        // Giorni dal 1° avvio del dispositivo
        val activeTime = usageTime ?: manuTime
        val usageDays = activeTime?.let {
            val diffMs = nowMs - it
            if (diffMs > 0) {
                val days = (diffMs / (1000L * 60 * 60 * 24L)).toInt()
                if (days in 0..MAX_VALID_BOOT_DAYS) days else null
            } else null
        }

        return Pair(ageMonths, usageDays)
    }

    /**
     * Derives usage age in months and days from a millisecond timestamp (e.g. ro.runtime.firstboot or firstInstallTime).
     * Rejects timestamps before 2018-01-01, in the future, or durations exceeding MAX_VALID_BOOT_DAYS.
     */
    fun deriveUsageFromTimestamp(
        bootMs: Long?,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<Int?, Int?> {
        if (bootMs == null || bootMs < MIN_VALID_BOOT_TIMESTAMP_MS || bootMs > nowMs) {
            return Pair(null, null)
        }
        val diffMs = nowMs - bootMs
        if (diffMs <= 0) return Pair(null, null)
        val days = (diffMs / (1000L * 60 * 60 * 24L)).toInt()
        if (days !in 0..MAX_VALID_BOOT_DAYS) return Pair(null, null)
        val months = (diffMs / (1000L * 60 * 60 * 24 * 30.4375)).toInt()
        return Pair(months, days)
    }

    /**
     * Computes charging power in Watts from voltage (mV) and current (mA).
     */
    fun calculateChargingPowerWatts(vMv: Int?, currentMa: Int?): Double? {
        if (vMv == null || vMv <= 0 || currentMa == null) return null
        return Math.round(((vMv.toDouble() * currentMa.toDouble()) / 1_000_000.0) * 10.0) / 10.0
    }

    /**
     * Determines active charging protocol.
     */
    fun determineChargingProtocol(
        isPlugged: Boolean,
        currentMa: Int?,
        voocIng: String?,
        fastChgType: String?,
        ppsIng: String?
    ): String {
        return when {
            !isPlugged -> {
                if (currentMa != null && currentMa < -20) "DISCHARGING" else "STANDBY"
            }
            voocIng?.trim() == "1" || fastChgType?.trim()?.toIntOrNull()?.let { it > 0 } == true -> "SuperVOOC"
            ppsIng?.trim() == "1" -> "USB-PD / PPS"
            currentMa != null && currentMa > 20 -> "STANDARD"
            else -> "STANDBY"
        }
    }

    /**
     * Evaluates hardware safety faults reported by BMS IC.
     */
    fun evaluateHardwareSafety(
        shortCHwStatus: Int?,
        shortIcOtpStatus: Int?,
        subboardTempErr: Int?
    ): Pair<Boolean, String> {
        val faults = mutableListOf<String>()
        if (shortCHwStatus != null && shortCHwStatus != 0) faults.add("ShortCircuit($shortCHwStatus)")
        if (shortIcOtpStatus != null && shortIcOtpStatus != 0) faults.add("OTP_OverHeat($shortIcOtpStatus)")
        if (subboardTempErr != null && subboardTempErr != 0) faults.add("SubboardTempErr($subboardTempErr)")
        val isSafe = faults.isEmpty()
        return Pair(isSafe, if (isSafe) "OK" else faults.joinToString(", "))
    }

    /**
     * Resolves raw SOH between fuel-gauge dynamic log and sysfs node.
     */
    fun resolveRawSoh(logSoh: Int?, sysfsSoh: Int?): Int? {
        return when {
            logSoh != null && sysfsSoh != null -> minOf(logSoh, sysfsSoh)
            logSoh != null -> logSoh
            else -> sysfsSoh
        }
    }

    /**
     * Adjusts FCC against Qmax overestimation when physical degradation is present.
     */
    fun resolveAdjustedFcc(rawFcc: Double?, logQmax: Double?, rawSoh: Int?): Double? {
        if (logQmax != null) {
            if (rawFcc == null || (rawSoh != null && rawSoh < 100 && rawFcc > logQmax)) {
                return logQmax
            }
        }
        return rawFcc
    }

    /**
     * Derives final effective State of Health and Full Charge Capacity.
     */
    fun resolveHealthAndFcc(
        rawSoh: Int?,
        fcc: Double?,
        ratedDesign: Double,
        typicalCalculationBase: Double
    ): HealthDerivation {
        var isHealthCalculated = false
        var calculationBaseUsed = ratedDesign

        val effectiveHealth = when {
            rawSoh != null && rawSoh in 1..100 -> {
                isHealthCalculated = false
                rawSoh
            }
            fcc != null && fcc > 0 -> {
                isHealthCalculated = true
                calculationBaseUsed = typicalCalculationBase
                ((fcc * 100.0) / typicalCalculationBase).toInt().coerceIn(1, 100)
            }
            else -> null
        }

        val effectiveFcc = when {
            rawSoh != null && rawSoh in 1..100 && ratedDesign > 0 -> {
                if (fcc != null && fcc > 0 && Math.abs((fcc / ratedDesign * 100.0) - rawSoh) <= 5.0 && fcc <= ratedDesign * 1.02) {
                    fcc
                } else {
                    Math.round((ratedDesign * rawSoh / 100.0) * 10.0) / 10.0
                }
            }
            fcc != null && fcc > 0 -> fcc
            else -> null
        }

        val displayDesignCapacity = if (isHealthCalculated) calculationBaseUsed else ratedDesign

        return HealthDerivation(
            effectiveHealth = effectiveHealth,
            isHealthCalculated = isHealthCalculated,
            effectiveFcc = effectiveFcc,
            displayDesignCapacity = displayDesignCapacity
        )
    }

    /**
     * Universal rated capacity mapping and heuristic IEC 61960 derivation.
     * If a known raw design capacity is provided, matches the known vendor curve.
     * If an unlisted typical capacity is provided (e.g. from PowerProfile or dumpsys),
     * automatically derives the IEC 61960 rated capacity (approx 97.2% of typical).
     */
    fun deriveRatedCapacityFromRawDesign(rawDesign: Double?, typicalFallback: Double? = null): Double {
        val base = rawDesign ?: typicalFallback
        return when {
            base != null && base in 7400.0..7650.0 -> 7290.0 // Oppo Find X9 Pro (tipica 7500 / nominale 7290)
            base != null && base in 7200.0..7399.0 -> 7150.0 // OnePlus 15 (tipica 7300 / nominale 7150)
            base != null && base in 6950.0..7100.0 -> 6840.0 // Find X9 (7025 / 6840) / Realme GT 8 Pro (7000 / 6850)
            base != null && base in 6600.0..6800.0 -> 6490.0 // Reno 16 Cina (6700 / 6490) / OnePlus Nord 5 (6800 / 6650)
            base != null && base in 6400.0..6599.0 -> 6310.0 // GT 7 Pro EU/Cina (6500 / 6310) / Reno 15 (6500 / 6335)
            base != null && base in 6100.0..6300.0 -> 6060.0 // Reno 14 Pro / 15 Pro (6200 / 6060)
            base != null && base in 5900.0..6099.0 -> 5840.0 // Reno 14 / OnePlus 13 / Reno 16 EU (5820-5840) / Realme 14 Pro+ (6000 / 5850)
            base != null && base in 5750.0..5899.0 -> 5660.0 // GT 7 Pro India (5800 / 5660)
            base != null && base in 5550.0..5749.0 -> 5490.0 // Find X8 (5630 / 5490)
            base != null && base in 5350.0..5549.0 -> 5360.0 // OnePlus 12R / Nord 4 / GT 6 (5500 / 5360)
            base != null && base in 5150.0..5349.0 -> 5050.0 // Realme 13 Pro+ (5200 / 5050)
            base != null && base in 4900.0..5149.0 -> 4880.0 // OnePlus 11 / Reno 12 / Find X7 Ultra (4860-4880)
            base != null && base in 4500.0..4700.0 -> 4440.0 // Reno 10 Pro / Reno 11 Pro (4600 / 4440)
            base != null && base in 4200.0..4400.0 -> 4190.0 // Find N3 Flip (4300 / 4190)
            base != null && base > 500.0 -> Math.round(base * 0.972 * 10.0) / 10.0
            else -> 5840.0
        }
    }

    /**
     * Parses dumpsys battery plain-text output with universal and Samsung EFS support.
     */
    fun parseDumpsysBatteryText(output: String?): DumpsysParsedData {
        if (output.isNullOrBlank()) return DumpsysParsedData()

        var voltage: Int? = null
        var chargeCounter: Long? = null
        var asoc: Int? = null
        var status: Int? = null
        var temp: Int? = null
        var currentNow: Int? = null
        var cycleCount: Int? = null
        var firstUseDate: String? = null
        var calDate: String? = null
        var qrData: String? = null
        var protectBatteryMode: Int? = null
        var isAuthentic: Boolean? = null

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("voltage:", ignoreCase = true) -> {
                    voltage = trimmed.substringAfter(":").trim().toIntOrNull()
                }
                trimmed.startsWith("Charge counter:", ignoreCase = true) ||
                trimmed.startsWith("charge counter:", ignoreCase = true) -> {
                    chargeCounter = trimmed.substringAfter(":").trim().toLongOrNull()
                }
                trimmed.startsWith("current now:", ignoreCase = true) -> {
                    currentNow = trimmed.substringAfter(":").trim().toIntOrNull()
                }
                trimmed.startsWith("mProtectBatteryMode:", ignoreCase = true) -> {
                    protectBatteryMode = trimmed.substringAfter(":").trim().toIntOrNull()
                }
                trimmed.startsWith("LLB CAL:", ignoreCase = true) -> {
                    val rawCal = trimmed.substringAfter(":").trim()
                    if (rawCal.length == 8 && rawCal.all { it.isDigit() }) {
                        calDate = "${rawCal.substring(0, 4)}-${rawCal.substring(4, 6)}-${rawCal.substring(6, 8)}"
                    }
                }
                trimmed.contains("[SS][BattInfo]FirstUseDateData", ignoreCase = true) -> {
                    val rawDate = trimmed.substringAfter("efsValue:").trim()
                    if (rawDate.length >= 8 && rawDate.take(8).all { it.isDigit() }) {
                        firstUseDate = "${rawDate.substring(0, 4)}-${rawDate.substring(4, 6)}-${rawDate.substring(6, 8)}"
                    }
                }
                trimmed.contains("[SS][BattInfo]DischargeLevelData", ignoreCase = true) -> {
                    val rawCycles = trimmed.substringAfter("efsValue:").trim().toIntOrNull()
                    if (rawCycles != null && rawCycles > 0) {
                        cycleCount = rawCycles / 100
                    }
                }
                trimmed.contains("[SS][BattInfo]QrData", ignoreCase = true) -> {
                    val qr = trimmed.substringAfter("efsValue:").trim()
                    if (qr.isNotEmpty()) {
                        qrData = qr
                        if (qr.contains("GH43-", ignoreCase = true) || qr.length >= 10) {
                            isAuthentic = true
                        }
                    }
                }
                trimmed.contains("[SS][BattInfo]AsocData", ignoreCase = true) -> {
                    val v = trimmed.substringAfter("efsValue:").trim().toIntOrNull()
                    if (v != null && v in 1..100) {
                        asoc = v
                    }
                }
                trimmed.startsWith("mSavedBatteryBsoh:", ignoreCase = true) ||
                trimmed.startsWith("mSavedBatteryAsoc:", ignoreCase = true) ||
                trimmed.startsWith("mBatteryHealth:", ignoreCase = true) ||
                trimmed.startsWith("mMaximumCapacity:", ignoreCase = true) ||
                trimmed.startsWith("mBatteryStateOfHealth:", ignoreCase = true) ||
                trimmed.startsWith("battery_health:", ignoreCase = true) ||
                trimmed.startsWith("maximum_capacity:", ignoreCase = true) ||
                trimmed.startsWith("battery_soh:", ignoreCase = true) ||
                trimmed.startsWith("asoc:", ignoreCase = true) ||
                trimmed.startsWith("health_percent:", ignoreCase = true) ||
                trimmed.startsWith("State of Health:", ignoreCase = true) -> {
                    val v = trimmed.substringAfter(":").trim().toIntOrNull()
                    if (v != null && v in 1..100) {
                        asoc = v
                    }
                }
                trimmed.startsWith("status:", ignoreCase = true) -> {
                    status = trimmed.substringAfter(":").trim().toIntOrNull()
                }
                trimmed.startsWith("temperature:", ignoreCase = true) -> {
                    temp = trimmed.substringAfter(":").trim().toIntOrNull()
                }
            }
        }

        return DumpsysParsedData(
            voltageMv = voltage,
            chargeCounterUah = chargeCounter,
            asocPercent = asoc,
            status = status,
            tempTenths = temp,
            currentNowMa = currentNow,
            cycleCount = cycleCount,
            firstUseDate = firstUseDate,
            calDate = calDate,
            qrData = qrData,
            protectBatteryMode = protectBatteryMode,
            isAuthentic = isAuthentic
        )
    }

    /**
     * Parses hardware rated, typical, and capacity numbers from 'dumpsys batterystats'.
     * Samsung One UI explicitly reports: "Capacity: 9800, Rated: 8160, Typical: 8400".
     * Standard AOSP reports: "Capacity: 5000".
     */
    fun parseDumpsysBatterystatsHardwareCapacity(output: String?): BatterystatsCapacityData {
        if (output.isNullOrBlank()) return BatterystatsCapacityData()
        var rated: Double? = null
        var typical: Double? = null
        var capacity: Double? = null

        val ratedMatch = Regex("""Rated:\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(output)
        if (ratedMatch != null) rated = ratedMatch.groupValues[1].toDoubleOrNull()

        val typicalMatch = Regex("""Typical:\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(output)
        if (typicalMatch != null) typical = typicalMatch.groupValues[1].toDoubleOrNull()

        val capMatch = Regex("""Capacity:\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(output)
        if (capMatch != null) capacity = capMatch.groupValues[1].toDoubleOrNull()

        return BatterystatsCapacityData(
            ratedMah = rated?.takeIf { it > 500.0 },
            typicalMah = typical?.takeIf { it > 500.0 },
            capacityMah = capacity?.takeIf { it > 500.0 }
        )
    }

    /**
     * Parses 'learned battery capacity' from dumpsys batterystats output.
     */
    fun parseDumpsysLearnedCapacity(output: String?): Double? {
        if (output.isNullOrBlank()) return null
        val match = Regex("""learned battery capacity:\s*(\d+)""", RegexOption.IGNORE_CASE).find(output)
        val rawLearned = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return if (rawLearned != null && rawLearned > 0) {
            normalizeCapacity(rawLearned)
        } else null
    }

    /**
     * Parses 'Estimated battery capacity' from dumpsys batterystats output.
     */
    fun parseDumpsysEstimatedCapacity(output: String?): Double? {
        if (output.isNullOrBlank()) return null
        val match = Regex("""Estimated battery capacity:\s*(\d+)""", RegexOption.IGNORE_CASE).find(output)
        val rawEst = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return if (rawEst != null && rawEst > 0) {
            normalizeCapacity(rawEst)
        } else null
    }

    /**
     * Parses battery health percentage from settings get or content query outputs.
     */
    fun parseSettingsBatteryHealth(output: String?): Int? {
        if (output.isNullOrBlank()) return null
        val lower = output.lowercase(Locale.ROOT)
        // Rejects analytics, count, state, or flag entries (e.g. battery_health_enter_times_daily=1)
        if (lower.contains("times") || lower.contains("count") || lower.contains("enable") ||
            lower.contains("switch") || lower.contains("daily") || lower.contains("state") ||
            lower.contains("status") || lower.contains("mode") || lower.contains("flag") ||
            lower.contains("level")
        ) {
            return null
        }
        val directInt = output.trim().toIntOrNull()
        if (directInt != null) {
            return if (directInt in 30..100) directInt else null
        }
        val match = Regex("""(?:value|health|soh|capacity)[:=]\s*(\d{1,3})""", RegexOption.IGNORE_CASE).find(output)
            ?: Regex("""=(\d{1,3})""").find(output)
            ?: Regex("""(?<![-+0-9])(100|[1-9]\d?)(?![-+0-9])""").find(output)
        val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (parsed != null && parsed in 30..100) parsed else null
    }

    /**
     * Splits a single CSV row into tokens, respecting double-quoted fields.
     */
    fun splitCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim().removeSurrounding("\"").trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\"").trim())
        return tokens
    }

    /**
     * Parses battery records from a sequence of CSV lines.
     * Supports standard export format:
     * ID,Timestamp,Data_Ora,Salute_Percentuale,Cicli_Carica,Capacita_Residua_mAh,Sorgente
     * As well as flexible header matching across multiple languages and column orders.
     */
    fun parseBatteryCsv(lines: Sequence<String>): CsvParseResult {
        val iterator = lines.iterator()
        if (!iterator.hasNext()) {
            return CsvParseResult(emptyList(), 0, 0)
        }

        var timestampIdx = -1
        var dateIdx = -1
        var healthIdx = -1
        var cyclesIdx = -1
        var capacityIdx = -1
        var sourceIdx = -1

        val records = mutableListOf<BatteryData>()
        val seenTimestamps = mutableSetOf<Long>()
        var duplicatesInFile = 0
        var invalidLines = 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        fun mapHeader(headerTokens: List<String>) {
            headerTokens.forEachIndexed { index, rawToken ->
                val token = rawToken.lowercase(Locale.ROOT)
                when {
                    token.contains("timestamp") || token.contains("epoch") -> timestampIdx = index
                    token.contains("salut") || token.contains("health") || token.contains("soh") -> healthIdx = index
                    token.contains("cicl") || token.contains("cycl") -> cyclesIdx = index
                    token.contains("capacit") || token.contains("mah") || token.contains("fcc") -> capacityIdx = index
                    token.contains("sorgent") || token.contains("sourc") || token.contains("origen") -> sourceIdx = index
                    token.contains("data") || token.contains("date") || token.contains("fech") -> dateIdx = index
                }
            }
        }

        fun parseLine(line: String) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return
            val tokens = splitCsvLine(trimmed)
            if (tokens.isEmpty()) return

            var timestamp: Long? = if (timestampIdx in tokens.indices) {
                tokens[timestampIdx].toLongOrNull()
            } else null

            // Fallback timestamp from formatted date string if timestamp column is missing or <= 0
            if ((timestamp == null || timestamp <= 0) && dateIdx in tokens.indices) {
                val dateStr = tokens[dateIdx]
                try {
                    timestamp = dateFormat.parse(dateStr)?.time
                } catch (_: Exception) {}
            }

            if (timestamp == null || timestamp <= 0) {
                invalidLines++
                return
            }

            val health = if (healthIdx in tokens.indices) {
                tokens[healthIdx].toIntOrNull()?.takeIf { it in 1..150 }
            } else null

            val cycles = if (cyclesIdx in tokens.indices) {
                tokens[cyclesIdx].toIntOrNull()?.takeIf { it >= 0 }
            } else null

            val capacity = if (capacityIdx in tokens.indices) {
                tokens[capacityIdx].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
            } else null

            val source = if (sourceIdx in tokens.indices) {
                val rawSrc = tokens[sourceIdx]
                if (rawSrc.isNotBlank() && rawSrc != "N/D" && rawSrc != "null") rawSrc else "CSV_IMPORT"
            } else "CSV_IMPORT"

            if (seenTimestamps.contains(timestamp)) {
                duplicatesInFile++
            } else {
                seenTimestamps.add(timestamp)
                records.add(
                    BatteryData(
                        id = 0,
                        timestamp = timestamp,
                        cycleCount = cycles,
                        healthPercentage = health,
                        currentCapacityMah = capacity,
                        source = source,
                        isDeleted = false
                    )
                )
            }
        }

        val firstLine = iterator.next().removePrefix("\uFEFF").trim()
        if (firstLine.isNotEmpty()) {
            val firstTokens = splitCsvLine(firstLine)
            val isHeader = firstTokens.any { token ->
                val lower = token.lowercase(Locale.ROOT)
                lower.contains("id") || lower.contains("timestamp") || lower.contains("salut") ||
                lower.contains("health") || lower.contains("cicl") || lower.contains("cycl") ||
                lower.contains("capacit") || lower.contains("data") || lower.contains("date")
            }

            if (isHeader) {
                mapHeader(firstTokens)
            } else {
                // Not a header: set fallback columns according to standard 7-col format
                // ID, Timestamp, Data_Ora, Salute, Cicli, Capacita, Sorgente
                if (firstTokens.size >= 6) {
                    timestampIdx = 1
                    dateIdx = 2
                    healthIdx = 3
                    cyclesIdx = 4
                    capacityIdx = 5
                    sourceIdx = 6
                }
                parseLine(firstLine)
            }
        }

        while (iterator.hasNext()) {
            val line = iterator.next().trim()
            if (line.isNotEmpty()) {
                parseLine(line)
            }
        }

        return CsvParseResult(
            records = records.sortedBy { it.timestamp },
            duplicateInFileCount = duplicatesInFile,
            invalidLinesCount = invalidLines
        )
    }

    fun parseBatteryCsv(lines: List<String>): CsvParseResult = parseBatteryCsv(lines.asSequence())
}
