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
    val isHealthCalculated: Boolean = false,
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
    val daysSinceFirstUsage: Int? = null,
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

    companion object {
        @Volatile private var lastVoltageMv: Int? = null
        @Volatile private var lastCurrentMa: Int? = null
        @Volatile private var lastSampleTimeMs: Long = 0L
        @Volatile private var cachedDynamicEsr: Double? = null

        fun resetEsrCache() {
            lastVoltageMv = null
            lastCurrentMa = null
            lastSampleTimeMs = 0L
            cachedDynamicEsr = null
        }
    }

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

    private var isRootChecked = false
    private var isRootAvailableCache = false

    fun isRootAvailable(): Boolean {
        if (isRootChecked) return isRootAvailableCache
        return try {
            val suPaths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            val suBinaryExists = suPaths.any { java.io.File(it).exists() } || try {
                val whichProcess = Runtime.getRuntime().exec(arrayOf("which", "su"))
                val whichExit = whichProcess.waitFor()
                whichExit == 0
            } catch (_: Throwable) {
                false
            }
            if (!suBinaryExists) {
                isRootChecked = true
                isRootAvailableCache = false
                return false
            }
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
            process.waitFor()
            val hasRoot = output?.contains("uid=0") == true
            if (hasRoot) {
                isRootChecked = true
                isRootAvailableCache = true
            }
            hasRoot
        } catch (_: Throwable) {
            isRootChecked = true
            isRootAvailableCache = false
            false
        }
    }

    suspend fun getBatterySnapshot(): BatterySnapshot = withContext(Dispatchers.IO) {
        val currentLevel = getBatteryLevelPercentage()

        // 1. Priorità: Sysfs / HAL tramite Shizuku o Root (lettura diretta BMS / ColorOS)
        if (isShizukuPermissionGranted() || isRootAvailable()) {
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
            // Decodifica dinamica della telemetria live del fuel-gauge BMS (batt_soh, batt_qmax, batt_rm, batt_fcc)
            // Questo registro è comune a tutti i dispositivi Realme, Oppo e OnePlus (modulo oplus_chg_battery).
            val headLine = querySysfs(
                "/sys/class/oplus_chg/battery/battery_log_head",
                "/sys/class/power_supply/battery/battery_log_head"
            )
            val contentLine = querySysfs(
                "/sys/class/oplus_chg/battery/battery_log_content",
                "/sys/class/power_supply/battery/battery_log_content"
            )
            val oplusLogMap = BatteryTelemetryParser.parseFuelGaugeLog(headLine, contentLine)
            val logSoh = BatteryTelemetryParser.extractLogSoh(oplusLogMap)
            val logQmax = BatteryTelemetryParser.extractLogQmax(oplusLogMap)
            val logRm = BatteryTelemetryParser.extractLogRm(oplusLogMap)

            val rawFccVal = querySysfs(
                "/sys/class/oplus_chg/battery/normal_batt_fcc",
                "/sys/class/oplus_chg/battery/sub_batt_fcc",
                "/sys/class/oplus_chg/battery/batt_fcc",
                "/sys/class/oplus_chg/battery/battery_fcc",
                "/sys/class/oplus_chg/battery/fcc",
                "/sys/class/power_supply/battery/charge_full",
                "/sys/class/power_supply/bms/charge_full",
                "/sys/class/power_supply/battery/batt_fcc",
                "/sys/class/power_supply/battery/battery_fcc",
                "/sys/class/power_supply/bms/batt_fcc"
            )
            var fcc = BatteryTelemetryParser.parseCapacity(rawFccVal)

            val rawDesignVal = querySysfs(
                "/sys/class/oplus_chg/battery/design_capacity",
                "/sys/class/power_supply/battery/charge_full_design",
                "/sys/class/power_supply/bms/charge_full_design",
                "/sys/class/power_supply/battery/design_capacity"
            )
            val rawDesign = BatteryTelemetryParser.parseCapacity(rawDesignVal)

            val rawCyclesVal = querySysfs(
                "/sys/class/oplus_chg/battery/battery_cycle",
                "/sys/class/oplus_chg/battery/battery_cc",
                "/sys/class/oplus_chg/battery/cycle_count",
                "/sys/class/power_supply/battery/cycle_count",
                "/sys/class/power_supply/bms/cycle_count",
                "/sys/class/power_supply/battery/battery_cycle",
                "/sys/class/power_supply/battery/charge_cycle"
            )
            val parsedCycles = BatteryTelemetryParser.parseCycleCount(rawCyclesVal)
            val cycles = if (parsedCycles != null) {
                parsedCycles
            } else {
                val bmCycles = try {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val c = bm.getIntProperty(7)
                    if (c >= 0) c else null
                } catch (_: Exception) {
                    null
                }
                bmCycles ?: run {
                    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    if (intent != null && Build.VERSION.SDK_INT >= 34) {
                        val intentCycles = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                        if (intentCycles >= 0) intentCycles else null
                    } else null
                }
            }

            val sysfsSoh = querySysfs(
                "/sys/class/oplus_chg/battery/normal_batt_soh",
                "/sys/class/oplus_chg/battery/sub_batt_soh",
                "/sys/class/oplus_chg/battery/batt_soh",
                "/sys/class/oplus_chg/battery/battery_soh",
                "/sys/class/oplus_chg/battery/soh",
                "/sys/class/power_supply/battery/battery_soh",
                "/sys/class/power_supply/battery/soh",
                "/sys/class/power_supply/bms/soh",
                "/sys/class/power_supply/battery/state_of_health",
                "/sys/class/power_supply/bms/state_of_health",
                "/sys/class/power_supply/battery/health_pct",
                "/sys/class/power_supply/battery/asoc",
                "/sys/class/power_supply/bms/asoc",
                "/proc/oplus_battery/soh",
                "/proc/oplus_battery/batt_soh",
                "/proc/oplus_battery/battery_soh"
            )?.toIntOrNull()?.takeIf { it in 1..100 }

            var rawSoh = BatteryTelemetryParser.resolveRawSoh(logSoh, sysfsSoh)

            // Se logQmax è disponibile e fcc è assente o un valore teorico sovrastimato (> logQmax a fronte di degrado), usiamo logQmax come reale FCC
            fcc = BatteryTelemetryParser.resolveAdjustedFcc(fcc, logQmax, rawSoh)

            var isDumpsysFallbackUsed = false

            // Se rawSoh non è nei percorsi standard di sysfs (es. restrizioni SELinux su OxygenOS),
            // interroghiamo Settings Provider / Content Provider di ColorOS/OxygenOS (es. Maximum capacity nelle impostazioni)
            if (rawSoh == null) {
                rawSoh = readSettingsBatteryHealth()
                if (rawSoh != null) {
                    isDumpsysFallbackUsed = true
                }
            }

            // Se né FCC né SOH sono ancora completi, tentiamo il recupero tramite il servizio Android HAL 'dumpsys battery' e 'dumpsys batterystats'
            val dumpsysInfo = if (fcc == null || rawSoh == null) {
                readDumpsysBatteryInfo().also {
                    if (it.asocPercent != null || it.learnedCapacityMah != null || it.estimatedCapacityMah != null || it.voltageMv != null || it.chargeCounterUah != null) {
                        isDumpsysFallbackUsed = true
                    }
                }
            } else null

            if (rawSoh == null && dumpsysInfo?.asocPercent != null && dumpsysInfo.asocPercent in 1..100) {
                rawSoh = dumpsysInfo.asocPercent
            }

            // Controllo finale SOH tramite API Android 14+ BATTERY_PROPERTY_STATE_OF_HEALTH (ID 10)
            if (rawSoh == null) {
                try {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val bmSoh = bm.getIntProperty(10)
                    if (bmSoh in 1..100) {
                        rawSoh = bmSoh
                    }
                } catch (_: Exception) {}
            }

            // FCC da dumpsys: SOLO se proviene da 'learned battery capacity' (reale degradato appreso dal BMS),
            // MAI da 'Estimated battery capacity' che è il profilo statico teorico di power_profile.xml (es. 6000 mAh su OP13)
            if (fcc == null && dumpsysInfo?.learnedCapacityMah != null && dumpsysInfo.learnedCapacityMah > 0) {
                fcc = dumpsysInfo.learnedCapacityMah
            }

            // Determinazione della capacità nominale (Rated Capacity IEC 61960):
            // 1. Se l'utente ha impostato una capacità manuale o scelto un preset, usa quel valore
            val userRated = preferences.getCustomRatedCapacity()
            val detectedPreset = OplusDevicePresets.detectDevicePreset()
            val presetRated = detectedPreset?.ratedMah
            val presetTypical = detectedPreset?.typicalMah?.toDouble()

            val ratedDesign = if (userRated != null && userRated > 0) {
                userRated
            } else {
                // Rilevamento automatico:
                // a) Verifica se il modello hardware rilevato da Build.MODEL è presente nei preset
                if (presetRated != null) {
                    presetRated
                } else {
                    BatteryTelemetryParser.deriveRatedCapacityFromRawDesign(rawDesign)
                }
            }

            // Calcolo della salute reale permanente:
            // Priorità ASSOLUTA alla salute certificata dal BMS hardware / Settings OS / ASOC (rawSoh)
            val typicalCalculationBase = userRated ?: presetTypical ?: rawDesign ?: ratedDesign
            val healthDerivation = BatteryTelemetryParser.resolveHealthAndFcc(
                rawSoh = rawSoh,
                fcc = fcc,
                ratedDesign = ratedDesign,
                typicalCalculationBase = typicalCalculationBase
            )
            var isHealthCalculated = healthDerivation.isHealthCalculated
            var effectiveHealth = healthDerivation.effectiveHealth
            var effectiveFcc = healthDerivation.effectiveFcc
            var calculationBaseUsed = healthDerivation.displayDesignCapacity

            // Stima a saturazione: se a piena carica (100% o status Full), il contatore coulombiano rappresenta l'FCC effettivo
            if (effectiveHealth == null && dumpsysInfo?.chargeCounterUah != null && (batteryLevel == 100 || dumpsysInfo.status == 5)) {
                val fullCoulomb = dumpsysInfo.chargeCounterUah / 1000.0
                if (fullCoulomb > 1000.0) {
                    calculationBaseUsed = typicalCalculationBase
                    effectiveHealth = ((fullCoulomb * 100.0) / typicalCalculationBase).toInt().coerceIn(1, 100)
                    effectiveFcc = fullCoulomb
                    isHealthCalculated = true
                    if (fcc == null) fcc = fullCoulomb
                }
            }

            // Capacità di riferimento da mostrare nella UI (designCapacityMah nel BatterySnapshot)
            val displayDesignCapacity = if (isHealthCalculated) calculationBaseUsed else ratedDesign

            // --- 1. Capacità Chimica Assoluta Qmax ---
            val qMaxMah = logQmax?.toInt()?.let { BatteryTelemetryParser.normalizeQmax(it, (effectiveFcc ?: fcc)?.toInt()) }
                ?: oplusLogMap["batt_qmax"]?.toIntOrNull()?.let { BatteryTelemetryParser.normalizeQmax(it, (effectiveFcc ?: fcc)?.toInt()) }

            // --- 2. Rilevamento Doppia Cella (SuperVOOC) e Voltaggi Singole Celle ---
            val agingData = querySysfs(
                "/sys/class/oplus_chg/battery/aging_ffc_data",
                "/sys/class/power_supply/battery/aging_ffc_data"
            )
            val isDual = BatteryTelemetryParser.parseDualCellArchitecture(agingData)

            val bccParms = querySysfs(
                "/sys/class/oplus_chg/battery/bcc_parms",
                "/sys/class/power_supply/battery/bcc_parms"
            )
            val bccParsed = BatteryTelemetryParser.parseBccParameters(bccParms)
            var cell0Volt = bccParsed.cell0VoltMv
            val bccCurrent = bccParsed.currentMa
            var cell1Volt = bccParsed.cell1VoltMv

            if (cell0Volt == null || cell0Volt == 0) {
                val rawV = querySysfs(
                    "/sys/class/oplus_chg/battery/gauge_vbat",
                    "/sys/class/oplus_chg/battery/batt_volt",
                    "/sys/class/power_supply/battery/voltage_now",
                    "/sys/class/power_supply/battery/batt_vol",
                    "/sys/class/power_supply/bms/voltage_now"
                )
                cell0Volt = BatteryTelemetryParser.parseVoltage(rawV)
            }
            if (cell0Volt == null || cell0Volt == 0) {
                cell0Volt = BatteryTelemetryParser.normalizeVoltage(dumpsysInfo?.voltageMv)
            }
            if (cell1Volt == null || cell1Volt == 0) {
                val rawV1 = querySysfs(
                    "/sys/class/oplus_chg/battery/cell1_volt",
                    "/sys/class/power_supply/battery/cell1_volt"
                )
                cell1Volt = BatteryTelemetryParser.parseVoltage(rawV1)
            }

            // --- 3. Tensione Minima di Spegnimento vbat_uv ---
            val vbatUv = querySysfs(
                "/sys/class/oplus_chg/battery/vbat_uv",
                "/sys/class/power_supply/battery/vbat_uv"
            )?.toIntOrNull()

            // --- 4. Raw SOH & Raw FCC per Batterie al Silicio-Carbonio ---
            val battType = querySysfs(
                "/sys/class/oplus_chg/battery/battery_type",
                "/sys/class/power_supply/battery/battery_type"
            )
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
                        val refFcc = effectiveFcc ?: fcc
                        if (refFcc != null) {
                            rawFccMah = (refFcc - (fccOffset * computedRawSoh.toInt() / 100)).toInt()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(tag, "Term coeff non disponibili o inaccessibili senza root: ${e.message}")
            }

            // --- 5. Dati Esclusivi Oppo: Date, Età, Autenticità, Remaining mAh ---
            val manuDate = querySysfs(
                "/sys/class/oplus_chg/battery/battery_manu_date",
                "/sys/class/power_supply/battery/battery_manu_date",
                "/sys/class/power_supply/battery/manu_date"
            )?.takeIf { it.isNotBlank() }
            val firstUsageDate = querySysfs(
                "/sys/class/oplus_chg/battery/battery_first_usage_date",
                "/sys/class/power_supply/battery/battery_first_usage_date",
                "/sys/class/power_supply/battery/first_usage_date"
            )?.takeIf { it.isNotBlank() }

            val (parsedAgeMonths, parsedDays) = BatteryTelemetryParser.parseUsageDates(manuDate, firstUsageDate)
            var batteryAgeMonths = parsedAgeMonths
            var daysSinceFirstUsage = parsedDays

            if (daysSinceFirstUsage == null) {
                try {
                    val pInfo = context.packageManager.getPackageInfo("android", 0)
                    val firstBootMs = pInfo.firstInstallTime
                    if (firstBootMs > 0) {
                        val diffMs = System.currentTimeMillis() - firstBootMs
                        if (diffMs > 0) {
                            daysSinceFirstUsage = (diffMs / (1000L * 60 * 60 * 24L)).toInt()
                            if (batteryAgeMonths == null) {
                                batteryAgeMonths = (diffMs / (1000L * 60 * 60 * 24 * 30.4375)).toInt()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val authStr = querySysfs(
                "/sys/class/oplus_chg/battery/authenticate",
                "/sys/class/power_supply/battery/authenticate",
                "/sys/class/power_supply/battery/authentic"
            )
            val isAuthentic = BatteryTelemetryParser.parseBatteryAuthenticity(authStr)

            val rmRaw = querySysfs(
                "/sys/class/oplus_chg/battery/battery_rm",
                "/sys/class/oplus_chg/battery/rm",
                "/sys/class/power_supply/battery/charge_now",
                "/sys/class/power_supply/bms/charge_now"
            )
            var remainingMah = BatteryTelemetryParser.parseCapacity(rmRaw)
            if (remainingMah == null && logRm != null) {
                remainingMah = logRm
            }
            if (remainingMah == null && dumpsysInfo?.chargeCounterUah != null) {
                val dumpsysRm = dumpsysInfo.chargeCounterUah / 1000.0
                if (dumpsysRm > 0) remainingMah = dumpsysRm
            }

            // --- 6. Potenza in Watt, Protocollo SuperVOOC, Temperatura con Allarme ---
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            val rawStatus = querySysfs(
                "/sys/class/power_supply/battery/status",
                "/sys/class/oplus_chg/battery/status"
            )?.trim()
            val isPlugged = (plugged > 0) || rawStatus.equals("Charging", ignoreCase = true) || rawStatus.equals("Full", ignoreCase = true)

            val vMv = cell0Volt ?: 4000

            // Campionamento unificato della corrente (mA) sincronizzato per Potenza ed ESR
            // Priorità:
            // 1. Sysfs Linux kernel (/sys/class/power_supply/battery/current_now o batt_current)
            // 2. bccCurrent (da bcc_parms) se != 0
            // 3. Android BatteryManager HAL BATTERY_PROPERTY_CURRENT_NOW
            val rawSysfsCur = querySysfs(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/bms/current_now",
                "/sys/class/oplus_chg/battery/batt_current"
            )?.toIntOrNull()?.takeIf { it != 0 }

            val rawCur = rawSysfsCur
                ?: bccCurrent?.takeIf { it != 0 }
                ?: run {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    if (cur != 0) cur else null
                }

            // Normalizzazione corrente (mA):
            // Convenzione standard fisica:
            // > 0 durante la ricarica (energia netta in entrata nella cella)
            // < 0 durante la scarica (energia netta assorbita dal dispositivo)
            val currentMa = BatteryTelemetryParser.normalizeCurrent(rawCur, isPlugged)
            val chargingPowerWatts = BatteryTelemetryParser.calculateChargingPowerWatts(vMv, currentMa)

            val voocIng = querySysfs(
                "/sys/class/oplus_chg/battery/voocchg_ing",
                "/sys/class/power_supply/battery/voocchg_ing"
            )
            val ppsIng = querySysfs(
                "/sys/class/oplus_chg/battery/ppschg_ing",
                "/sys/class/power_supply/battery/ppschg_ing"
            )
            val fastChgType = querySysfs(
                "/sys/class/oplus_chg/usb/fast_chg_type",
                "/sys/class/power_supply/usb/fast_chg_type"
            )
            val chargingProtocol = BatteryTelemetryParser.determineChargingProtocol(
                isPlugged = isPlugged,
                currentMa = currentMa,
                voocIng = voocIng,
                fastChgType = fastChgType,
                ppsIng = ppsIng
            )

            val tempRaw = querySysfs(
                "/sys/class/oplus_chg/battery/batt_temp",
                "/sys/class/power_supply/battery/temp",
                "/sys/class/power_supply/bms/temp"
            )
            var tempCelsius = BatteryTelemetryParser.parseTemperature(tempRaw)
            if (tempCelsius == null && dumpsysInfo?.tempTenths != null) {
                tempCelsius = dumpsysInfo.tempTenths / 10.0
            }
            if (tempCelsius == null) {
                val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -999) ?: -999
                if (rawTemp != -999) {
                    tempCelsius = rawTemp / 10.0
                }
            }

            // --- 7. Resistenza Interna Dinamica DC / ESR (Punto 1) ---
            val vNowStr = querySysfs(
                "/sys/class/power_supply/battery/voltage_now",
                "/sys/class/power_supply/bms/voltage_now"
            )
            val vNowMv = BatteryTelemetryParser.parseVoltage(vNowStr) ?: cell0Volt

            var internalResistanceMohm: Double? = null

            // Priorità 1: Lettura diretta del registro di resistenza interna hardware dal BMS / sysfs
            val rawResistanceStr = querySysfs(
                "/sys/class/power_supply/battery/resistance",
                "/sys/class/power_supply/battery/resistance_now",
                "/sys/class/power_supply/bms/resistance",
                "/sys/class/power_supply/bms/resistance_now",
                "/sys/class/oplus_chg/battery/battery_resistance",
                "/sys/class/oplus_chg/battery/resistance",
                "/sys/class/oplus_chg/battery/rbatt",
                "/sys/class/oplus_chg/battery/r_cell",
                "/sys/class/oplus_chg/battery/batt_res"
            )
            val rawRes = rawResistanceStr?.toDoubleOrNull()
            if (rawRes != null && rawRes > 0) {
                internalResistanceMohm = BatteryTelemetryParser.normalizeInternalResistance(rawRes, isDual)
            }

            // Priorità 2: Calcolo Elettrochimico Dinamico a Impulso / Step (ΔV / ΔI tra campioni successivi)
            val nowMs = System.currentTimeMillis()
            if (internalResistanceMohm == null && vNowMv != null && currentMa != null) {
                val prevV = lastVoltageMv
                val prevI = lastCurrentMa
                val elapsedMs = nowMs - lastSampleTimeMs
                if (prevV != null && prevI != null) {
                    val stepEsr = BatteryTelemetryParser.calculateDynamicStepEsr(
                        v1Mv = prevV,
                        i1Ma = prevI,
                        v2Mv = vNowMv,
                        i2Ma = currentMa,
                        elapsedMs = elapsedMs,
                        isDual = isDual
                    )
                    if (stepEsr != null) {
                        internalResistanceMohm = stepEsr
                        cachedDynamicEsr = stepEsr
                    }
                }
            }

            // Aggiorna lo storico del campione temporale per il calcolo differenziale ΔV/ΔI
            if (vNowMv != null && currentMa != null) {
                lastVoltageMv = vNowMv
                lastCurrentMa = currentMa
                lastSampleTimeMs = nowMs
            }

            // Priorità 3: Fallback con OCV (|V_ocv - V_now| / |I|), MA con validazione anti-static e anti-desync
            val ocvStr = querySysfs(
                "/sys/class/power_supply/battery/voltage_ocv",
                "/sys/class/power_supply/bms/voltage_ocv",
                "/sys/class/oplus_chg/battery/voltage_ocv"
            )
            val voltageOcvMv = BatteryTelemetryParser.parseVoltage(ocvStr)

            if (internalResistanceMohm == null && voltageOcvMv != null && vNowMv != null && currentMa != null) {
                val ocvEsr = BatteryTelemetryParser.calculateOcvEsr(
                    voltageOcvMv = voltageOcvMv,
                    vNowMv = vNowMv,
                    currentMa = currentMa,
                    batteryLevel = batteryLevel,
                    isDual = isDual
                )
                if (ocvEsr != null) {
                    internalResistanceMohm = ocvEsr
                    cachedDynamicEsr = ocvEsr
                }
            }

            // Se nessun calcolo istantaneo è stato possibile ma disponiamo di un ESR dinamico recente verificato
            if (internalResistanceMohm == null && cachedDynamicEsr != null) {
                internalResistanceMohm = cachedDynamicEsr
            }

            // --- 8. Analisi Bilanciamento Celle (Punto 3) ---
            val cellBalance = BatteryTelemetryParser.evaluateCellBalance(cell0Volt, cell1Volt, isDual)
            val cellBalanceDeltaMv = cellBalance.deltaMv
            val cellBalanceStatus = cellBalance.status

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
            val chipSoc = querySysfs(
                "/sys/class/oplus_chg/battery/chip_soc",
                "/sys/class/power_supply/battery/chip_soc"
            )?.toIntOrNull()

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
            val refCapacity = effectiveFcc ?: fcc
            val tempCompensatedCapacityMah = BatteryTelemetryParser.calculateTempCompensatedCapacity(refCapacity, tempCelsius)

            // --- 12. Flag di Protezione e Sicurezza Hardware BMS (Punto 6) ---
            val shortCHwStatus = querySysfs(
                "/sys/class/oplus_chg/battery/short_c_hw_status",
                "/sys/class/power_supply/battery/short_c_hw_status"
            )?.toIntOrNull()
            val shortIcOtpStatus = querySysfs(
                "/sys/class/oplus_chg/battery/short_ic_otp_status",
                "/sys/class/power_supply/battery/short_ic_otp_status"
            )?.toIntOrNull()
            val subboardTempErr = querySysfs(
                "/sys/class/oplus_chg/battery/subboard_temp_err",
                "/sys/class/power_supply/battery/subboard_temp_err"
            )?.toIntOrNull()

            val (isHardwareSafe, safetyFaultDetails) = BatteryTelemetryParser.evaluateHardwareSafety(
                shortCHwStatus,
                shortIcOtpStatus,
                subboardTempErr
            )

            // Verifica soglia di sicurezza termica (> 42°C)
            if (tempCelsius != null && tempCelsius >= 42.0) {
                NotificationHelper.showOverheatNotification(context, tempCelsius)
            }

            Log.d(tag, "Oplus Sysfs: FCC=${effectiveFcc ?: fcc} mAh, Qmax=$qMaxMah mAh, Dual=$isDual, Cell0=$cell0Volt mV, Cell1=$cell1Volt mV, ESR=$internalResistanceMohm mOhm, Sync=$bmsSyncStatus, TrueFull=$isTrueFullCharge, SatStatus=$saturationStatus, TempComp=$tempCompensatedCapacityMah, Safe=$isHardwareSafe")

            val snapshotSource = when {
                lastPrivilegedSource == "ROOT" && !isDumpsysFallbackUsed -> "Oplus Sysfs (Root)"
                lastPrivilegedSource == "ROOT" && isDumpsysFallbackUsed -> "Android HAL / dumpsys (Root)"
                isDumpsysFallbackUsed -> "Android HAL / dumpsys (Shizuku)"
                else -> "Oplus Sysfs (Shizuku)"
            }

            if (effectiveHealth != null || cycles != null || (effectiveFcc ?: fcc) != null || cell0Volt != null || remainingMah != null) {
                DiagnosticLogger.log(
                    tag = "SNAPSHOT_DONE",
                    command = "getBatterySnapshot()",
                    result = "Health=$effectiveHealth%, FCC=${effectiveFcc ?: fcc} mAh, BaseDesign=$displayDesignCapacity mAh, Cycles=$cycles, Source=$snapshotSource, Calculated=$isHealthCalculated",
                    isSuccess = true
                )
                BatterySnapshot(
                    cycleCount = cycles,
                    healthPercentage = effectiveHealth,
                    currentCapacityMah = effectiveFcc ?: fcc,
                    designCapacityMah = displayDesignCapacity,
                    batteryLevelPercentage = batteryLevel,
                    source = snapshotSource,
                    isShizukuUsed = true,
                    isHealthCalculated = isHealthCalculated,
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
                    daysSinceFirstUsage = daysSinceFirstUsage,
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

    private fun normalizeQmax(rawQ: Int, fcc: Int?): Int = BatteryTelemetryParser.normalizeQmax(rawQ, fcc)

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

    private var lastPrivilegedSource: String = "SHIZUKU"

    private fun querySysfs(vararg paths: String): String? {
        if (paths.isEmpty()) return null
        val pathListStr = paths.joinToString(" ")
        val loopCmd = """
            for p in $pathListStr; do
                if [ -f "${'$'}p" ]; then
                    v=${'$'}(cat "${'$'}p" 2>/dev/null)
                    if [ -n "${'$'}v" ] && [ "${'$'}v" != "0" ] && [ "${'$'}v" != "-1" ] && [ "${'$'}v" != "null" ]; then
                        echo "${'$'}v"
                        exit 0
                    fi
                fi
            done
        """.trimIndent()
        val (result, source) = executePrivilegedCommand(loopCmd, multiLine = false)
        if (!result.isNullOrEmpty() && result != "0" && result != "-1") {
            lastPrivilegedSource = source
            return result
        }
        return null
    }

    private fun executePrivilegedCommand(cmd: String, multiLine: Boolean = false): Pair<String?, String> {
        if (isShizukuPermissionGranted()) {
            val shizukuRes = executeShizukuCommand(cmd, multiLine)
            if (!shizukuRes.isNullOrEmpty() &&
                !shizukuRes.contains("Permission denied", ignoreCase = true) &&
                !shizukuRes.equals("No such file or directory", ignoreCase = true)
            ) {
                return Pair(shizukuRes, "SHIZUKU")
            }
        }
        if (isRootAvailable()) {
            val rootRes = executeRootCommand(cmd, multiLine)
            if (!rootRes.isNullOrEmpty() &&
                !rootRes.contains("Permission denied", ignoreCase = true) &&
                !rootRes.equals("No such file or directory", ignoreCase = true)
            ) {
                return Pair(rootRes, "ROOT")
            }
        }
        return Pair(null, "NONE")
    }

    private fun executeShizukuCommand(cmd: String, multiLine: Boolean = false): String? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val output = BufferedReader(InputStreamReader(process.inputStream)).use {
                if (multiLine) it.readText() else it.readLine()
            }
            process.waitFor()
            val trimmed = output?.trim()
            DiagnosticLogger.log(
                tag = "SHIZUKU",
                command = cmd,
                result = trimmed,
                isSuccess = !trimmed.isNullOrEmpty() && !trimmed.contains("Permission denied", ignoreCase = true)
            )
            trimmed
        } catch (e: Exception) {
            Log.e(tag, "Errore esecuzione comando Shizuku: $cmd", e)
            DiagnosticLogger.log("SHIZUKU", cmd, "Error: ${e.message}", false)
            null
        }
    }

    private fun executeRootCommand(cmd: String, multiLine: Boolean = false): String? {
        if (!isRootAvailable()) return null
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use {
                if (multiLine) it.readText() else it.readLine()
            }
            process.waitFor()
            val trimmed = output?.trim()
            DiagnosticLogger.log(
                tag = "ROOT",
                command = cmd,
                result = trimmed,
                isSuccess = !trimmed.isNullOrEmpty() && !trimmed.contains("Permission denied", ignoreCase = true)
            )
            trimmed
        } catch (e: Exception) {
            Log.e(tag, "Errore esecuzione comando Root: $cmd", e)
            DiagnosticLogger.log("ROOT", cmd, "Error: ${e.message}", false)
            null
        }
    }

    private fun readSettingsBatteryHealth(): Int? {
        val cmd = """
            for k in battery_health maximum_capacity battery_maximum_capacity oplus_battery_soh oplus_battery_health oplus_battery_maximum_capacity oplus_customize_battery_soh battery_soh; do
                for ns in system global secure; do
                    v=$(settings get ${'$'}ns ${'$'}k 2>/dev/null)
                    if [ "${'$'}v" != "null" ] && [ -n "${'$'}v" ] && [ "${'$'}v" -ge 1 ] && [ "${'$'}v" -le 100 ] 2>/dev/null; then
                        echo "${'$'}v"
                        exit 0
                    fi
                done
            done
            for uri in content://com.oplus.battery.provider/battery_health content://com.oplus.battery.provider/maximum_capacity content://com.oplus.battery.provider/battery_soh content://com.oplus.battery/battery_health; do
                res=$(content query --uri ${'$'}uri 2>/dev/null)
                if [ -n "${'$'}res" ] && [ "${'$'}res" != "No result" ]; then
                    echo "${'$'}res"
                    exit 0
                fi
            done
            for ns in system global secure; do
                res=$(settings list ${'$'}ns 2>/dev/null | grep -iE '^(.*(soh|maximum_capacity|battery_health).*)=' | head -n 1)
                if [ -n "${'$'}res" ]; then
                    echo "${'$'}res"
                    exit 0
                fi
            done
        """.trimIndent()

        val (output, _) = executePrivilegedCommand(cmd, multiLine = false)
        val resultHealth = BatteryTelemetryParser.parseSettingsBatteryHealth(output)

        DiagnosticLogger.log(
            tag = "SETTINGS_SOH",
            command = "readSettingsBatteryHealth()",
            result = resultHealth?.let { "$it%" } ?: "Not found in settings/providers",
            isSuccess = resultHealth != null
        )
        return resultHealth
    }

    private data class DumpsysBatteryInfo(
        val voltageMv: Int? = null,
        val chargeCounterUah: Long? = null,
        val asocPercent: Int? = null,
        val learnedCapacityMah: Double? = null,
        val estimatedCapacityMah: Double? = null,
        val status: Int? = null,
        val tempTenths: Int? = null
    )

    private fun readDumpsysBatteryInfo(): DumpsysBatteryInfo {
        val (output, source) = executePrivilegedCommand("dumpsys battery", multiLine = true)
        if (output.isNullOrEmpty()) return DumpsysBatteryInfo()
        lastPrivilegedSource = source

        val parsed = BatteryTelemetryParser.parseDumpsysBatteryText(output)

        var learnedCapacity: Double? = null
        var estimatedCapacity: Double? = null
        try {
            val (learnedOutput, _) = executePrivilegedCommand("dumpsys batterystats 2>/dev/null | grep -m 1 -i 'learned battery capacity'", multiLine = false)
            learnedCapacity = BatteryTelemetryParser.parseDumpsysLearnedCapacity(learnedOutput)
            val (statsOutput, _) = executePrivilegedCommand("dumpsys batterystats 2>/dev/null | grep -m 1 -i 'Estimated battery capacity'", multiLine = false)
            estimatedCapacity = BatteryTelemetryParser.parseDumpsysEstimatedCapacity(statsOutput)
        } catch (_: Exception) {}

        val info = DumpsysBatteryInfo(
            voltageMv = parsed.voltageMv,
            chargeCounterUah = parsed.chargeCounterUah,
            asocPercent = parsed.asocPercent,
            learnedCapacityMah = learnedCapacity,
            estimatedCapacityMah = estimatedCapacity,
            status = parsed.status,
            tempTenths = parsed.tempTenths
        )
        DiagnosticLogger.log(
            tag = "DUMPSYS_INFO",
            command = "readDumpsysBatteryInfo()",
            result = "V=${parsed.voltageMv}mV, CC=${parsed.chargeCounterUah}uAh, ASOC=${parsed.asocPercent}%, Learned=${learnedCapacity}mAh, EstProfile=${estimatedCapacity}mAh, Temp=${parsed.tempTenths?.let { it / 10.0 }}C",
            isSuccess = parsed.voltageMv != null || parsed.chargeCounterUah != null || learnedCapacity != null || parsed.asocPercent != null
        )
        return info
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

        val userRated = preferences.getCustomRatedCapacity()
        val detectedPreset = OplusDevicePresets.detectDevicePreset()
        val presetTypical = detectedPreset?.typicalMah?.toDouble()
        val ratedDesign = userRated ?: detectedPreset?.ratedMah
        val calculationBase = userRated ?: presetTypical ?: ratedDesign

        var effectiveHealth = healthPercentage
        var isHealthCalculated = false
        if (effectiveHealth == null && capacityMah != null && capacityMah > 1000.0 && calculationBase != null && calculationBase > 0) {
            if (batteryLevel == 100 || isFull) {
                effectiveHealth = ((capacityMah * 100.0) / calculationBase).toInt().coerceIn(1, 100)
                isHealthCalculated = true
            }
        }
        val displayDesign = if (isHealthCalculated && calculationBase != null) calculationBase else ratedDesign

        DiagnosticLogger.log(
            tag = "BATTERY_MANAGER",
            command = "readFromBatteryManager()",
            result = "Cycles=$cycleCount, Health=$effectiveHealth%, Cap=${capacityMah}mAh, Design=${displayDesign}mAh",
            isSuccess = true
        )

        val bmDaysSinceFirstUsage = try {
            val pInfo = context.packageManager.getPackageInfo("android", 0)
            val firstBootMs = pInfo.firstInstallTime
            if (firstBootMs > 0) {
                val diffMs = System.currentTimeMillis() - firstBootMs
                if (diffMs > 0) (diffMs / (1000L * 60 * 60 * 24L)).toInt() else null
            } else null
        } catch (_: Exception) { null }

        return BatterySnapshot(
            cycleCount = cycleCount,
            healthPercentage = effectiveHealth,
            currentCapacityMah = capacityMah,
            designCapacityMah = displayDesign,
            batteryLevelPercentage = batteryLevel,
            source = "BatteryManager",
            isShizukuUsed = false,
            isHealthCalculated = isHealthCalculated,
            daysSinceFirstUsage = bmDaysSinceFirstUsage,
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
