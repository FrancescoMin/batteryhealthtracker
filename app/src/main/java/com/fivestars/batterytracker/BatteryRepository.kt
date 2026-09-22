package com.fivestars.batterytracker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

enum class BmsSyncStatus {
    SYNCED,
    GOOD,
    CALIBRATION_RECOMMENDED
}

data class BatterySnapshot(
    val cycleCount: Int?,
    val healthPercentage: Int?,
    val currentCapacityMah: Double?,
    val designCapacityMah: Double?,
    val batteryLevelPercentage: Int?,
    val source: String,
    val isShizukuUsed: Boolean,
    val qMaxMah: Int? = null,
    val isDualBattery: Boolean? = null,
    val cell0VoltageMv: Int? = null,
    val cell1VoltageMv: Int? = null,
    val vbatUvMv: Int? = null,
    val rawSohPercentage: Float? = null,
    val rawFccMah: Int? = null,
    val batteryType: String? = null,
    val manuDate: String? = null,
    val firstUsageDate: String? = null,
    val batteryAgeMonths: Int? = null,
    val isAuthentic: Boolean? = null,
    val remainingCapacityMah: Double? = null,
    val chargingPowerWatts: Double? = null,
    val chargingProtocol: String? = null,
    val batteryTemperatureCelsius: Double? = null,
    val internalResistanceMohm: Double? = null,
    val voltageOcvMv: Int? = null,
    val cellBalanceDeltaMv: Int? = null,
    val cellBalanceStatus: String? = null,
    val bmsSyncStatus: BmsSyncStatus? = null,
    val cyclesSinceLastCalibration: Int? = null,
    val chipSoc: Int? = null,
    val isTrueFullCharge: Boolean? = null,
    val saturationStatus: String? = null,
    val tempCompensatedCapacityMah: Double? = null,
    val isHardwareSafe: Boolean? = null,
    val safetyFaultDetails: String? = null
)

class BatteryRepository(private val context: Context) {

    private val tag = "BatteryRepository"
    private val preferences = BatteryPreferences(context)

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isShizukuPermissionGranted(): Boolean {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun getBatterySnapshot(): BatterySnapshot = withContext(Dispatchers.IO) {
        val currentLevel = getBatteryLevelPercentage()

        // 1. Priorità: Oplus Sysfs tramite Shizuku (lettura diretta BMS / ColorOS)
        if (isShizukuPermissionGranted()) {
            val oplusSnapshot = readFromOplusSysfs(currentLevel)
            if (oplusSnapshot != null) {
                Log.d(tag, "Acquisizione completata da Oplus Sysfs: $oplusSnapshot")
                return@withContext oplusSnapshot
            }
        }

        // 2. Fallback: API standard BatteryManager di Android
        val standardSnapshot = readFromBatteryManager(currentLevel)
        Log.d(tag, "Acquisizione da BatteryManager standard: $standardSnapshot")
        return@withContext standardSnapshot
    }

    private fun readFromOplusSysfs(batteryLevel: Int?): BatterySnapshot? {
        return try {
            val fccStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_fcc")
            val designStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/design_capacity")
            val ccStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_cc")
            val sohStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_soh")

            val rawSoh = sohStr?.toIntOrNull()
            val cycles = ccStr?.toIntOrNull()
            val fcc = fccStr?.toDoubleOrNull()
            val rawDesign = designStr?.toDoubleOrNull()

            // Determinazione della capacità nominale (Rated Capacity IEC 61960):
            // 1. Se l'utente ha impostato una capacità manuale o scelto un preset, usa quel valore
            val userRated = preferences.getCustomRatedCapacity()
            val ratedDesign = if (userRated != null && userRated > 0) {
                userRated
            } else {
                // Rilevamento automatico:
                // a) Verifica se il modello hardware rilevato da Build.MODEL è presente nei preset
                val detectedPreset = OplusDevicePresets.detectDevicePreset()
                if (detectedPreset != null) {
                    detectedPreset.ratedMah
                } else when {
                    // b) Mappatura euristica basata sul valore registrato in design_capacity dal chip
                    rawDesign != null && rawDesign in 7400.0..7650.0 -> 7290.0 // Oppo Find X9 Pro (tipica 7500 / nominale 7290)
                    rawDesign != null && rawDesign in 7200.0..7399.0 -> 7150.0 // OnePlus 15 (tipica 7300 / nominale 7150)
                    rawDesign != null && rawDesign in 6950.0..7100.0 -> 6840.0 // Find X9 (7025 / 6840) / Realme GT 8 Pro (7000 / 6850)
                    rawDesign != null && rawDesign in 6600.0..6800.0 -> 6490.0 // Reno 16 Cina (6700 / 6490)
                    rawDesign != null && rawDesign in 6400.0..6599.0 -> 6310.0 // GT 7 Pro EU/Cina (6500 / 6310) / Reno 15 (6500 / 6335)
                    rawDesign != null && rawDesign in 6100.0..6300.0 -> 6060.0 // Reno 14 Pro / 15 Pro (6200 / 6060)
                    rawDesign != null && rawDesign in 5900.0..6099.0 -> 5840.0 // Reno 14 / OnePlus 13 / Reno 16 EU (5820-5840)
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

            // Calcolo della salute reale permanente:
            // SOH = (Capacità reale FCC / Capacità nominale Rated) * 100
            val effectiveHealth = if (fcc != null && fcc > 0) {
                val calculatedHealth = Math.round((fcc / ratedDesign) * 100.0).toInt().coerceIn(1, 100)
                calculatedHealth
            } else {
                rawSoh
            }

            // --- 1. Capacità Chimica Assoluta Qmax ---
            val headLine = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_log_head")
            val contentLine = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_log_content")
            val qMaxMah = if (!headLine.isNullOrEmpty() && !contentLine.isNullOrEmpty()) {
                val heads = headLine.split(',')
                val values = contentLine.split(',')
                val qIdx = heads.indexOf("batt_qmax")
                if (qIdx != -1 && qIdx < values.size) {
                    values[qIdx].trim().toIntOrNull()?.let { normalizeQmax(it, fcc?.toInt()) }
                } else null
            } else null

            // --- 2. Rilevamento Doppia Cella (SuperVOOC) e Voltaggi Singole Celle ---
            val agingData = executeShizukuCommand("cat /sys/class/oplus_chg/battery/aging_ffc_data")
            val isDual = when (agingData?.split(',')?.getOrNull(1)?.trim()) {
                "2" -> true
                "1" -> false
                else -> null
            }

            val bccParms = executeShizukuCommand("cat /sys/class/oplus_chg/battery/bcc_parms")
            var cell0Volt: Int? = null
            var cell1Volt: Int? = null
            var bccCurrent: Int? = null
            if (!bccParms.isNullOrEmpty()) {
                val parts = bccParms.split(',').map { it.trim() }
                cell0Volt = parts.getOrNull(6)?.toIntOrNull()?.takeIf { it > 0 }
                bccCurrent = parts.getOrNull(8)?.toIntOrNull()
                cell1Volt = parts.getOrNull(11)?.toIntOrNull()?.takeIf { it > 0 }
            }
            if (cell0Volt == null || cell0Volt == 0) {
                cell0Volt = executeShizukuCommand("cat /sys/class/oplus_chg/battery/gauge_vbat")?.toIntOrNull()
                    ?: executeShizukuCommand("cat /sys/class/oplus_chg/battery/batt_volt")?.toIntOrNull()
            }

            // --- 3. Tensione Minima di Spegnimento vbat_uv ---
            val vbatUv = executeShizukuCommand("cat /sys/class/oplus_chg/battery/vbat_uv")?.toIntOrNull()

            // --- 4. Raw SOH & Raw FCC per Batterie al Silicio-Carbonio ---
            val battType = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_type")
            var rawSohPercentage: Float? = null
            var rawFccMah: Int? = null

            try {
                val coeffList = readTermCoefficients(battType)
                if (coeffList.isNotEmpty() && vbatUv != null) {
                    val match = coeffList.find { it.first == vbatUv }
                    if (match != null) {
                        val sohOffset = match.third
                        val factor = 1 + sohOffset.toFloat() / 100f
                        val baseSoh = effectiveHealth ?: 100
                        val computedRawSoh = baseSoh.toFloat() / factor
                        rawSohPercentage = Math.round(computedRawSoh * 10f) / 10f

                        val fccOffset = match.second
                        if (fcc != null) {
                            rawFccMah = (fcc - (fccOffset * computedRawSoh.toInt() / 100)).toInt()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(tag, "Term coeff non disponibili o inaccessibili senza root: ${e.message}")
            }

            // --- 5. Dati Esclusivi Oppo: Date, Età, Autenticità, Remaining mAh ---
            val manuDate = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_manu_date")?.takeIf { it.isNotBlank() }
            val firstUsageDate = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_first_usage_date")?.takeIf { it.isNotBlank() }

            var batteryAgeMonths: Int? = null
            val refDateStr = manuDate ?: firstUsageDate
            if (!refDateStr.isNullOrEmpty()) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val parsed = sdf.parse(refDateStr)
                    if (parsed != null) {
                        val diffMs = System.currentTimeMillis() - parsed.time
                        if (diffMs > 0) {
                            batteryAgeMonths = (diffMs / (1000L * 60 * 60 * 24 * 30.4375)).toInt()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(tag, "Errore parsing data produzione: $refDateStr", e)
                }
            }

            val authStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/authenticate")
            val isAuthentic = authStr?.trim() == "1" || authStr?.trim()?.equals("true", ignoreCase = true) == true

            val rmStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/battery_rm")
            val remainingMah = rmStr?.toDoubleOrNull()

            // --- 6. Potenza in Watt, Protocollo SuperVOOC, Temperatura con Allarme ---
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            val rawStatus = executeShizukuCommand("cat /sys/class/power_supply/battery/status")?.trim()
            val isPlugged = (plugged > 0) || rawStatus.equals("Charging", ignoreCase = true) || rawStatus.equals("Full", ignoreCase = true)

            val vMv = cell0Volt ?: 4000
            val rawCur = bccCurrent ?: run {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                if (cur != 0) {
                    if (Math.abs(cur) > 10000) cur / 1000 else cur
                } else null
            }

            // Normalizzazione corrente (mA):
            // Nei driver Oppo/Oplus (bcc_parms e current_now), la convenzione hardware è:
            // negativo durante la ricarica, positivo durante la scarica.
            // Invertiamo il segno (-rawCur) per allinearlo alla fisica standard:
            // > 0 durante la ricarica (energia netta in entrata nella cella)
            // < 0 durante la scarica (energia netta assorbita dal dispositivo)
            val currentMa = if (rawCur != null) -rawCur else null

            val chargingPowerWatts = if (currentMa != null && vMv > 0) {
                Math.round(((vMv.toDouble() * currentMa.toDouble()) / 1_000_000.0) * 10.0) / 10.0
            } else null

            val voocIng = executeShizukuCommand("cat /sys/class/oplus_chg/battery/voocchg_ing")?.trim()
            val ppsIng = executeShizukuCommand("cat /sys/class/oplus_chg/battery/ppschg_ing")?.trim()
            val fastChgType = executeShizukuCommand("cat /sys/class/oplus_chg/usb/fast_chg_type")?.trim()
            val chargingProtocol = when {
                !isPlugged -> {
                    if (currentMa != null && currentMa < -20) "In Scarica" else "Standby"
                }
                voocIng == "1" || fastChgType?.toIntOrNull()?.let { it > 0 } == true -> "SuperVOOC"
                ppsIng == "1" -> "USB-PD / PPS"
                currentMa != null && currentMa > 20 -> "Carica Standard"
                else -> "Standby"
            }

            val tempStr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/batt_temp")
            var tempCelsius = tempStr?.toDoubleOrNull()?.let { if (it > 200.0) it / 10.0 else it }
            if (tempCelsius == null) {
                val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -999) ?: -999
                if (rawTemp != -999) {
                    tempCelsius = rawTemp / 10.0
                }
            }

            // --- 7. Resistenza Interna / ESR (Punto 1) ---
            val ocvStr = executeShizukuCommand("cat /sys/class/power_supply/battery/voltage_ocv")
            val rawOcv = ocvStr?.toIntOrNull()
            val voltageOcvMv = if (rawOcv != null) {
                if (rawOcv > 100_000) rawOcv / 1000 else rawOcv
            } else null

            val vNowStr = executeShizukuCommand("cat /sys/class/power_supply/battery/voltage_now")
            val rawVnow = vNowStr?.toIntOrNull()
            val vNowMv = if (rawVnow != null && rawVnow > 0) {
                if (rawVnow > 100_000) rawVnow / 1000 else rawVnow
            } else cell0Volt

            val curNowStr = executeShizukuCommand("cat /sys/class/power_supply/battery/current_now")
            val rawCurNow = curNowStr?.toIntOrNull()
            val curMa = if (rawCurNow != null && rawCurNow != 0) {
                if (Math.abs(rawCurNow) > 10000) rawCurNow / 1000 else rawCurNow
            } else currentMa

            var internalResistanceMohm: Double? = null
            if (voltageOcvMv != null && vNowMv != null && curMa != null && Math.abs(curMa) >= 40) {
                val deltaV = Math.abs(voltageOcvMv - vNowMv)
                val esr = (deltaV.toDouble() / Math.abs(curMa).toDouble()) * 1000.0
                if (esr in 10.0..3000.0) {
                    internalResistanceMohm = Math.round(esr * 10.0) / 10.0
                }
            }

            // --- 8. Analisi Bilanciamento Celle (Punto 3) ---
            val cellBalanceDeltaMv: Int?
            val cellBalanceStatus: String?
            if (isDual == true && cell0Volt != null && cell1Volt != null && cell1Volt > 0) {
                val delta = Math.abs(cell0Volt - cell1Volt)
                cellBalanceDeltaMv = delta
                cellBalanceStatus = when {
                    delta < 15 -> "Optimal"
                    delta <= 40 -> "Normal"
                    else -> "Imbalanced"
                }
            } else {
                // Architettura a singola cella (1S, es. Reno 14)
                cellBalanceDeltaMv = 0
                cellBalanceStatus = "SingleCell"
            }

            // --- 9. Calibrazione & Rilevamento Deriva BMS (Punto 2) ---
            var bmsSyncStatus: BmsSyncStatus? = null
            var cyclesSinceLastCalibration: Int? = null
            if (cycles != null && cycles > 0) {
                if (batteryLevel == 100) {
                    preferences.setLastCalibrationCycle(cycles)
                    cyclesSinceLastCalibration = 0
                    bmsSyncStatus = BmsSyncStatus.SYNCED
                } else {
                    val lastCycle = preferences.getLastCalibrationCycle()
                    if (lastCycle != null) {
                        val diff = cycles - lastCycle
                        cyclesSinceLastCalibration = if (diff >= 0) diff else 0
                    } else {
                        preferences.setLastCalibrationCycle(cycles)
                        cyclesSinceLastCalibration = 0
                    }
                    bmsSyncStatus = when {
                        cyclesSinceLastCalibration <= 15 -> BmsSyncStatus.SYNCED
                        cyclesSinceLastCalibration <= 30 -> BmsSyncStatus.GOOD
                        else -> BmsSyncStatus.CALIBRATION_RECOMMENDED
                    }
                }
            }

            // --- 10. Saturazione Reale: True Full Charge vs Display 100% (Punto 4) ---
            val chipSoc = executeShizukuCommand("cat /sys/class/oplus_chg/battery/chip_soc")?.toIntOrNull()

            val isTrueFullCharge: Boolean?
            val saturationStatus: String?
            when {
                isPlugged && batteryLevel == 100 -> {
                    if (rawStatus.equals("Full", ignoreCase = true) || (chipSoc != null && chipSoc >= 100 && (currentMa == null || Math.abs(currentMa) <= 80))) {
                        isTrueFullCharge = true
                        saturationStatus = "SATURATED"
                    } else {
                        isTrueFullCharge = false
                        saturationStatus = "CV_TAPERING"
                    }
                }
                isPlugged -> {
                    isTrueFullCharge = false
                    saturationStatus = "CHARGING"
                }
                currentMa != null && currentMa < -20 -> {
                    isTrueFullCharge = false
                    saturationStatus = "DISCHARGING"
                }
                else -> {
                    isTrueFullCharge = false
                    saturationStatus = "STANDBY"
                }
            }

            // --- 11. Compensazione Termica della Capacità a 25°C (Standard IEC 61960 - Punto 5) ---
            val tempCompensatedCapacityMah: Double? = if (fcc != null && fcc > 0 && tempCelsius != null) {
                val deltaT = tempCelsius - 25.0
                // Coefficiente termico standard per celle Li-ion / Silicio-Carbonio: 0.6% per °C (0.006)
                val comp = fcc / (1.0 + 0.006 * deltaT)
                Math.round(comp * 10.0) / 10.0
            } else null

            // --- 12. Flag di Protezione e Sicurezza Hardware BMS (Punto 6) ---
            val shortCHwStatus = executeShizukuCommand("cat /sys/class/oplus_chg/battery/short_c_hw_status")?.toIntOrNull()
            val shortIcOtpStatus = executeShizukuCommand("cat /sys/class/oplus_chg/battery/short_ic_otp_status")?.toIntOrNull()
            val subboardTempErr = executeShizukuCommand("cat /sys/class/oplus_chg/battery/subboard_temp_err")?.toIntOrNull()

            val faults = mutableListOf<String>()
            if (shortCHwStatus != null && shortCHwStatus != 0) faults.add("ShortCircuit($shortCHwStatus)")
            if (shortIcOtpStatus != null && shortIcOtpStatus != 0) faults.add("OTP_OverHeat($shortIcOtpStatus)")
            if (subboardTempErr != null && subboardTempErr != 0) faults.add("SubboardTempErr($subboardTempErr)")

            val isHardwareSafe = faults.isEmpty()
            val safetyFaultDetails = if (isHardwareSafe) "OK" else faults.joinToString(", ")

            // Verifica soglia di sicurezza termica (> 42°C)
            if (tempCelsius != null && tempCelsius >= 42.0) {
                NotificationHelper.showOverheatNotification(context, tempCelsius)
            }

            Log.d(tag, "Oplus Sysfs: FCC=$fcc mAh, Qmax=$qMaxMah mAh, Dual=$isDual, Cell0=$cell0Volt mV, Cell1=$cell1Volt mV, ESR=$internalResistanceMohm mOhm, Sync=$bmsSyncStatus, TrueFull=$isTrueFullCharge, SatStatus=$saturationStatus, TempComp=$tempCompensatedCapacityMah, Safe=$isHardwareSafe")

            if (effectiveHealth != null || cycles != null || fcc != null) {
                BatterySnapshot(
                    cycleCount = cycles,
                    healthPercentage = effectiveHealth,
                    currentCapacityMah = fcc,
                    designCapacityMah = ratedDesign,
                    batteryLevelPercentage = batteryLevel,
                    source = "Oplus Sysfs (Shizuku)",
                    isShizukuUsed = true,
                    qMaxMah = qMaxMah,
                    isDualBattery = isDual,
                    cell0VoltageMv = cell0Volt,
                    cell1VoltageMv = cell1Volt,
                    vbatUvMv = vbatUv,
                    rawSohPercentage = rawSohPercentage,
                    rawFccMah = rawFccMah,
                    batteryType = battType,
                    manuDate = manuDate,
                    firstUsageDate = firstUsageDate,
                    batteryAgeMonths = batteryAgeMonths,
                    isAuthentic = isAuthentic,
                    remainingCapacityMah = remainingMah,
                    chargingPowerWatts = chargingPowerWatts,
                    chargingProtocol = chargingProtocol,
                    batteryTemperatureCelsius = tempCelsius,
                    internalResistanceMohm = internalResistanceMohm,
                    voltageOcvMv = voltageOcvMv,
                    cellBalanceDeltaMv = cellBalanceDeltaMv,
                    cellBalanceStatus = cellBalanceStatus,
                    bmsSyncStatus = bmsSyncStatus,
                    cyclesSinceLastCalibration = cyclesSinceLastCalibration,
                    chipSoc = chipSoc,
                    isTrueFullCharge = isTrueFullCharge,
                    saturationStatus = saturationStatus,
                    tempCompensatedCapacityMah = tempCompensatedCapacityMah,
                    isHardwareSafe = isHardwareSafe,
                    safetyFaultDetails = safetyFaultDetails
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore nella lettura da Oplus Sysfs tramite Shizuku", e)
            null
        }
    }

    private fun normalizeQmax(rawQ: Int, fcc: Int?): Int {
        var q = rawQ
        val ref = fcc ?: 20000
        while (q >= ref * 2) {
            q /= 10
        }
        return q
    }

    private fun readTermCoefficients(battType: String?): List<Triple<Int, Int, Int>> {
        val primaryPath = if (!battType.isNullOrEmpty()) {
            "/proc/device-tree/soc/oplus,mms_gauge/$battType/deep_spec,term_coeff"
        } else null
        val fallbackPath = "/proc/device-tree/soc/oplus,mms_gauge/deep_spec,term_coeff"

        val cmd = if (primaryPath != null) {
            "base64 $primaryPath 2>/dev/null || base64 $fallbackPath 2>/dev/null"
        } else {
            "base64 $fallbackPath 2>/dev/null"
        }

        val b64 = executeShizukuCommand(cmd)?.replace("\n", "")?.replace("\r", "")?.trim()
        if (b64.isNullOrEmpty()) return emptyList()

        return try {
            val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
            val buffer = java.nio.ByteBuffer.wrap(bytes)
            val list = mutableListOf<Triple<Int, Int, Int>>()
            while (buffer.remaining() >= 12) {
                val vbatUv = buffer.int
                val fccOffset = buffer.int
                val sohOffset = buffer.int
                list.add(Triple(vbatUv, fccOffset, sohOffset))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun executeShizukuCommand(cmd: String): String? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
            process.waitFor()
            output?.trim()
        } catch (e: Exception) {
            Log.e(tag, "Errore esecuzione comando Shizuku: $cmd", e)
            null
        }
    }

    private fun readFromBatteryManager(batteryLevel: Int?): BatterySnapshot {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        // Lettura cicli: ID 7 (BATTERY_PROPERTY_CYCLE_COUNT) o Intent ACTION_BATTERY_CHANGED
        var cycleCount: Int? = null
        try {
            val rawCycles = batteryManager.getIntProperty(7) // BATTERY_PROPERTY_CYCLE_COUNT
            if (rawCycles >= 0) {
                cycleCount = rawCycles
            }
        } catch (e: Exception) {
            Log.d(tag, "Impossibile leggere BATTERY_PROPERTY_CYCLE_COUNT da BatteryManager", e)
        }

        // Lettura SOH: ID 10 (BATTERY_PROPERTY_STATE_OF_HEALTH)
        var healthPercentage: Int? = null
        try {
            val rawHealth = batteryManager.getIntProperty(10) // BATTERY_PROPERTY_STATE_OF_HEALTH
            if (rawHealth in 1..100) {
                healthPercentage = rawHealth
            }
        } catch (e: Exception) {
            Log.d(tag, "Impossibile leggere BATTERY_PROPERTY_STATE_OF_HEALTH", e)
        }

        // Se i cicli non sono ancora disponibili, proviamo dal broadcast intent
        if (cycleCount == null) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null && Build.VERSION.SDK_INT >= 34) {
                val intentCycles = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                if (intentCycles >= 0) {
                    cycleCount = intentCycles
                }
            }
        }

        // Capacità attuale in µAh -> convertita in mAh
        var capacityMah: Double? = null
        try {
            val chargeCounterUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (chargeCounterUah > 0) {
                capacityMah = chargeCounterUah / 1000.0
            }
        } catch (e: Exception) {
            Log.d(tag, "Impossibile leggere BATTERY_PROPERTY_CHARGE_COUNTER", e)
        }

        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -999) ?: -999
        val tempC = if (rawTemp != -999) rawTemp / 10.0 else null
        val tempComp = if (capacityMah != null && tempC != null) {
            val deltaT = tempC - 25.0
            Math.round((capacityMah / (1.0 + 0.006 * deltaT)) * 10.0) / 10.0
        } else null

        val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL
        val isTrueFull = if (batteryLevel == 100) isFull else false
        val satStatus = when {
            batteryLevel == 100 && isFull -> "SATURATED"
            batteryLevel == 100 -> "CV_TAPERING"
            statusInt == BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
            statusInt == BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            else -> "STANDBY"
        }

        return BatterySnapshot(
            cycleCount = cycleCount,
            healthPercentage = healthPercentage,
            currentCapacityMah = capacityMah,
            designCapacityMah = null,
            batteryLevelPercentage = batteryLevel,
            source = "BatteryManager",
            isShizukuUsed = false,
            batteryTemperatureCelsius = tempC,
            isTrueFullCharge = isTrueFull,
            saturationStatus = satStatus,
            tempCompensatedCapacityMah = tempComp,
            isHardwareSafe = true,
            safetyFaultDetails = "OK"
        )
    }

    fun getBatteryLevelPercentage(): Int? {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level in 0..100) level else null
        } catch (e: Exception) {
            null
        }
    }
}
