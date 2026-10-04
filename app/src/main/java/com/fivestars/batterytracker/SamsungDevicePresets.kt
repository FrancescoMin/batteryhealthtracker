package com.fivestars.batterytracker

import android.os.Build

object SamsungDevicePresets {

    private val RAW_PRESETS = listOf(
        // --- GALAXY TAB S9 SERIES ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S9",
            regionVariant = "Wi-Fi & 5G",
            typicalMah = 8400,
            ratedMah = 8160.0,
            codeNames = listOf("SM-X710", "SM-X716B", "SM-X716N", "SM-X718U", "SM_X716B", "SM_X710")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S9+",
            regionVariant = "Wi-Fi & 5G",
            typicalMah = 10090,
            ratedMah = 9800.0,
            codeNames = listOf("SM-X810", "SM-X816B", "SM-X816N", "SM-X818U", "SM_X816B", "SM_X810")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S9 Ultra",
            regionVariant = "Wi-Fi & 5G",
            typicalMah = 11200,
            ratedMah = 10880.0,
            codeNames = listOf("SM-X910", "SM-X916B", "SM-X916N", "SM-X918U", "SM_X916B", "SM_X910")
        ),

        // --- GALAXY TAB S8 SERIES ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S8",
            regionVariant = null,
            typicalMah = 8000,
            ratedMah = 7760.0,
            codeNames = listOf("SM-X700", "SM-X706B", "SM-X706N", "SM-X706U")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S8+",
            regionVariant = null,
            typicalMah = 10090,
            ratedMah = 9800.0,
            codeNames = listOf("SM-X800", "SM-X806B", "SM-X806N", "SM-X806U")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Tab S8 Ultra",
            regionVariant = null,
            typicalMah = 11200,
            ratedMah = 10880.0,
            codeNames = listOf("SM-X900", "SM-X906B", "SM-X906N", "SM-X906U")
        ),

        // --- GALAXY S24 SERIES ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S24 Ultra",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4855.0,
            codeNames = listOf("SM-S928B", "SM-S928U", "SM-S9280", "SM_S928B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S24+",
            regionVariant = null,
            typicalMah = 4900,
            ratedMah = 4755.0,
            codeNames = listOf("SM-S926B", "SM-S926U", "SM-S9260", "SM_S926B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S24",
            regionVariant = null,
            typicalMah = 4000,
            ratedMah = 3880.0,
            codeNames = listOf("SM-S921B", "SM-S921U", "SM-S9210", "SM_S921B")
        ),

        // --- GALAXY S23 SERIES ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S23 Ultra",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4855.0,
            codeNames = listOf("SM-S918B", "SM-S918U", "SM-S9180", "SM_S918B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S23+",
            regionVariant = null,
            typicalMah = 4700,
            ratedMah = 4565.0,
            codeNames = listOf("SM-S916B", "SM-S916U", "SM-S9160", "SM_S916B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy S23",
            regionVariant = null,
            typicalMah = 3900,
            ratedMah = 3785.0,
            codeNames = listOf("SM-S911B", "SM-S911U", "SM-S9110", "SM_S911B")
        ),

        // --- GALAXY Z FOLD & FLIP ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Z Fold 6",
            regionVariant = null,
            typicalMah = 4400,
            ratedMah = 4273.0,
            codeNames = listOf("SM-F956B", "SM-F956U", "SM-F9560", "SM_F956B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Z Flip 6",
            regionVariant = null,
            typicalMah = 4000,
            ratedMah = 3887.0,
            codeNames = listOf("SM-F741B", "SM-F741U", "SM-F7410", "SM_F741B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Z Fold 5",
            regionVariant = null,
            typicalMah = 4400,
            ratedMah = 4270.0,
            codeNames = listOf("SM-F946B", "SM-F946U", "SM-F9460")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy Z Flip 5",
            regionVariant = null,
            typicalMah = 3700,
            ratedMah = 3591.0,
            codeNames = listOf("SM-F731B", "SM-F731U", "SM-F7310")
        ),

        // --- GALAXY A SERIES ---
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy A55 5G",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4905.0,
            codeNames = listOf("SM-A556B", "SM-A556E", "SM-A5560", "SM_A556B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy A54 5G",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4905.0,
            codeNames = listOf("SM-A546B", "SM-A546E", "SM-A5460", "SM_A546B")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy A35 5G",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4905.0,
            codeNames = listOf("SM-A356B", "SM-A356E", "SM-A3560")
        ),
        DeviceBatteryPreset(
            brand = "Samsung",
            modelName = "Galaxy A34 5G",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4905.0,
            codeNames = listOf("SM-A346B", "SM-A346E", "SM-A3460")
        )
    )

    val ALL_PRESETS: List<DeviceBatteryPreset> = RAW_PRESETS.sortedBy { it.displayName.lowercase() }

    fun detectDevicePreset(model: String = Build.MODEL): DeviceBatteryPreset? {
        val upper = model.uppercase().replace("_", "-")
        return ALL_PRESETS.firstOrNull { preset ->
            preset.codeNames.any { code ->
                val normCode = code.uppercase().replace("_", "-")
                upper.contains(normCode)
            }
        }
    }
}
