package com.fivestars.batterytracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class BatteryWidgetTelemetryTest {

    @Test
    fun testSohFormattingAndColorLogic() {
        fun formatSoh(soh: Int?): Pair<String, Int> {
            val sohStr = if (soh != null) "$soh%" else "--%"
            val sohColor = when {
                soh == null -> 0xFF00E5FF.toInt()
                soh >= 90 -> 0xFF00E5FF.toInt()
                soh >= 80 -> 0xFFFFB300.toInt()
                else -> 0xFFEF4444.toInt()
            }
            return Pair(sohStr, sohColor)
        }

        val prime = formatSoh(99)
        assertEquals("99%", prime.first)
        assertEquals(0xFF00E5FF.toInt(), prime.second)

        val degraded = formatSoh(84)
        assertEquals("84%", degraded.first)
        assertEquals(0xFFFFB300.toInt(), degraded.second)

        val critical = formatSoh(76)
        assertEquals("76%", critical.first)
        assertEquals(0xFFEF4444.toInt(), critical.second)

        val missing = formatSoh(null)
        assertEquals("--%", missing.first)
    }

    @Test
    fun testCycleCountFormatting() {
        fun formatCycles(cycles: Int?): String = if (cycles != null) "$cycles" else "--"

        assertEquals("321", formatCycles(321))
        assertEquals("0", formatCycles(0))
        assertEquals("--", formatCycles(null))
    }

    @Test
    fun testLiveWattageFormattingAndColorLogic() {
        fun formatWattage(watts: Double?): Pair<String, Int> {
            val isCharging = watts != null && watts > 0.0
            val isDischarging = watts != null && watts < -0.05
            val wattStr = when {
                watts == null -> "0.0 W"
                isCharging -> "+%.1f W".format(Locale.US, watts)
                else -> "%.1f W".format(Locale.US, watts)
            }
            val wattColor = when {
                isCharging -> 0xFF00E676.toInt()
                isDischarging -> 0xFFFFB300.toInt()
                else -> 0xFF94A3B8.toInt()
            }
            return Pair(wattStr, wattColor)
        }

        // Fast charging (SuperVOOC / PD / VOOC)
        val fastChg = formatWattage(67.48)
        assertEquals("+67.5 W", fastChg.first)
        assertEquals(0xFF00E676.toInt(), fastChg.second)

        // Standard discharging under active screen load
        val discharging = formatWattage(-2.34)
        assertEquals("-2.3 W", discharging.first)
        assertEquals(0xFFFFB300.toInt(), discharging.second)

        // Standby / zero
        val idle = formatWattage(0.0)
        assertEquals("0.0 W", idle.first)
        assertEquals(0xFF94A3B8.toInt(), idle.second)

        val missing = formatWattage(null)
        assertEquals("0.0 W", missing.first)
        assertEquals(0xFF94A3B8.toInt(), missing.second)
    }

    @Test
    fun testCellTemperatureFormattingAndAlertThresholds() {
        fun formatTemperature(tempC: Double?): Pair<String, Int> {
            val tempStr = if (tempC != null) "%.1f°C".format(Locale.US, tempC) else "--°C"
            val tempColor = when {
                tempC == null -> 0xFF38BDF8.toInt()
                tempC > 42.0 -> 0xFFEF4444.toInt()
                tempC >= 38.0 -> 0xFFFB923C.toInt()
                else -> 0xFF38BDF8.toInt()
            }
            return Pair(tempStr, tempColor)
        }

        // Normal operating temperature
        val normal = formatTemperature(31.24)
        assertEquals("31.2°C", normal.first)
        assertEquals(0xFF38BDF8.toInt(), normal.second)

        // Warm during intensive charge
        val warm = formatTemperature(39.5)
        assertEquals("39.5°C", warm.first)
        assertEquals(0xFFFB923C.toInt(), warm.second)

        // Critical safety threshold (> 42°C)
        val hot = formatTemperature(44.1)
        assertEquals("44.1°C", hot.first)
        assertEquals(0xFFEF4444.toInt(), hot.second)

        val missing = formatTemperature(null)
        assertEquals("--°C", missing.first)
    }

    @Test
    fun testStandardBatteryManagerWattageCalculation() {
        // 4000 mV and 2500 mA (charging) -> 10.0 W
        val chargingWatts = BatteryTelemetryParser.calculateChargingPowerWatts(4000, 2500)
        assertEquals(10.0, chargingWatts ?: 0.0, 0.01)

        // 3850 mV and -650 mA (discharging) -> -2.5 W
        val dischargingWatts = BatteryTelemetryParser.calculateChargingPowerWatts(3850, -650)
        assertEquals(-2.5, dischargingWatts ?: 0.0, 0.01)
    }
}
