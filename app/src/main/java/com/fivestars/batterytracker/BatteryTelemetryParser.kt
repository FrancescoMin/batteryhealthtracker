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
        val tempTenths: Int? = null
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
        if (deltaI < 150) return null
        val deltaV = Math.abs(v2Mv - v1Mv)
        val stepEsr = (deltaV.toDouble() / deltaI.toDouble()) * 1000.0
        val maxLimit = if (isDual == true) 900.0 else 550.0
        return if (stepEsr in 15.0..maxLimit) {
            Math.round(stepEsr * 10.0) / 10.0
        } else null
    }

    /**
     * Fallback calculation of ESR using OCV with anti-static cutoff and scale guards.
     */
    fun calculateOcvEsr(
        voltageOcvMv: Int,
        vNowMv: Int,
        currentMa: Int,
        batteryLevel: Int?,
        isDual: Boolean?
    ): Double? {
        if (Math.abs(currentMa) < 120) return null
        val normOcv = if (voltageOcvMv > 5000 && vNowMv <= 4600) voltageOcvMv / 2 else voltageOcvMv
        val normVnow = if (vNowMv > 5000 && voltageOcvMv <= 4600) vNowMv / 2 else vNowMv

        val isStaticOcvCutoff = (batteryLevel ?: 100) < 95 && normOcv >= 4420
        val deltaV = Math.abs(normOcv - normVnow)
        val maxAllowableDeltaV = if (isDual == true) 300 else 150

        if (!isStaticOcvCutoff && deltaV <= maxAllowableDeltaV) {
            val esr = (deltaV.toDouble() / Math.abs(currentMa).toDouble()) * 1000.0
            val maxLimit = if (isDual == true) 900.0 else 550.0
            if (esr in 20.0..maxLimit) {
                return Math.round(esr * 10.0) / 10.0
            }
        }
        return null
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
     * Heuristic rated capacity mapping for modern OPlus batteries.
     */
    fun deriveRatedCapacityFromRawDesign(rawDesign: Double?): Double {
        return when {
            rawDesign != null && rawDesign in 7400.0..7650.0 -> 7290.0 // Oppo Find X9 Pro (tipica 7500 / nominale 7290)
            rawDesign != null && rawDesign in 7200.0..7399.0 -> 7150.0 // OnePlus 15 (tipica 7300 / nominale 7150)
            rawDesign != null && rawDesign in 6950.0..7100.0 -> 6840.0 // Find X9 (7025 / 6840) / Realme GT 8 Pro (7000 / 6850)
            rawDesign != null && rawDesign in 6600.0..6800.0 -> 6490.0 // Reno 16 Cina (6700 / 6490) / OnePlus Nord 5 (6800 / 6650)
            rawDesign != null && rawDesign in 6400.0..6599.0 -> 6310.0 // GT 7 Pro EU/Cina (6500 / 6310) / Reno 15 (6500 / 6335)
            rawDesign != null && rawDesign in 6100.0..6300.0 -> 6060.0 // Reno 14 Pro / 15 Pro (6200 / 6060)
            rawDesign != null && rawDesign in 5900.0..6099.0 -> 5840.0 // Reno 14 / OnePlus 13 / Reno 16 EU (5820-5840) / Realme 14 Pro+ (6000 / 5850)
            rawDesign != null && rawDesign in 5750.0..5899.0 -> 5660.0 // GT 7 Pro India (5800 / 5660)
            rawDesign != null && rawDesign in 5550.0..5749.0 -> 5490.0 // Find X8 (5630 / 5490)
            rawDesign != null && rawDesign in 5350.0..5549.0 -> 5360.0 // OnePlus 12R / Nord 4 / GT 6 (5500 / 5360)
            rawDesign != null && rawDesign in 5150.0..5349.0 -> 5050.0 // Realme 13 Pro+ (5200 / 5050)
            rawDesign != null && rawDesign in 4900.0..5149.0 -> 4880.0 // OnePlus 11 / Reno 12 / Find X7 Ultra (4860-4880)
            rawDesign != null && rawDesign in 4500.0..4700.0 -> 4440.0 // Reno 10 Pro / Reno 11 Pro (4600 / 4440)
            rawDesign != null && rawDesign in 4200.0..4400.0 -> 4190.0 // Find N3 Flip (4300 / 4190)
            rawDesign != null && rawDesign > 0 -> Math.round(rawDesign * 0.97333 * 10.0) / 10.0
            else -> 5840.0
        }
    }

    /**
     * Parses dumpsys battery plain-text output.
     */
    fun parseDumpsysBatteryText(output: String?): DumpsysParsedData {
        if (output.isNullOrBlank()) return DumpsysParsedData()

        var voltage: Int? = null
        var chargeCounter: Long? = null
        var asoc: Int? = null
        var status: Int? = null
        var temp: Int? = null

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("voltage:", ignoreCase = true) -> {
                    voltage = trimmed.substringAfter(":").trim().toIntOrNull()
                }
                trimmed.startsWith("Charge counter:", ignoreCase = true) -> {
                    chargeCounter = trimmed.substringAfter(":").trim().toLongOrNull()
                }
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
            tempTenths = temp
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
        val directInt = output.trim().toIntOrNull()
        if (directInt != null) {
            return if (directInt in 1..100) directInt else null
        }
        val match = Regex("""(?:value|health|soh|capacity)[:=]\s*(\d{1,3})""", RegexOption.IGNORE_CASE).find(output)
            ?: Regex("""=(\d{1,3})""").find(output)
            ?: Regex("""(?<![-+0-9])(100|[1-9]\d?)(?![-+0-9])""").find(output)
        val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (parsed != null && parsed in 1..100) parsed else null
    }
}
