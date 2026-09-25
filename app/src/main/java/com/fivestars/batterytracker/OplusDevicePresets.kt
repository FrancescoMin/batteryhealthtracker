package com.fivestars.batterytracker

import android.os.Build

data class DeviceBatteryPreset(
    val brand: String,
    val modelName: String,
    val regionVariant: String? = null,
    val typicalMah: Int,
    val ratedMah: Double,
    val codeNames: List<String> = emptyList()
) {
    val displayName: String
        get() = if (regionVariant.isNullOrBlank()) {
            "$brand $modelName"
        } else {
            "$brand $modelName ($regionVariant)"
        }
}

object OplusDevicePresets {

    private val RAW_PRESETS = listOf(
        // --- ONEPLUS ---
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "11",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4880.0,
            codeNames = listOf("CPH2449", "CPH2451")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "12",
            regionVariant = null,
            typicalMah = 5400,
            ratedMah = 5260.0,
            codeNames = listOf("CPH2581", "CPH2579")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "12R",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("CPH2609", "CPH2611")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "13",
            regionVariant = null,
            typicalMah = 6000,
            ratedMah = 5840.0,
            codeNames = listOf("PJZ110", "CPH2653", "CPH2655")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "15",
            regionVariant = "Global & Cina",
            typicalMah = 7300,
            ratedMah = 7150.0,
            codeNames = listOf("CPH2799", "PLK110")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord 4",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("CPH2661", "CPH2663")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord 5",
            regionVariant = "India / Global",
            typicalMah = 6800,
            ratedMah = 6650.0,
            codeNames = listOf("CPH2707", "CPH2709")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord 5",
            regionVariant = "EU / UK",
            typicalMah = 5200,
            ratedMah = 5200.0,
            codeNames = listOf("CPH2709_EU")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "13R",
            regionVariant = null,
            typicalMah = 6000,
            ratedMah = 5840.0,
            codeNames = listOf("CPH2649", "CPH2645", "CPH2647")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord 3",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4880.0,
            codeNames = listOf("CPH2491", "CPH2493")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord CE 4",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("CPH2613")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Nord CE 4 Lite",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("CPH2621", "CPH2619")
        ),
        DeviceBatteryPreset(
            brand = "OnePlus",
            modelName = "Open",
            regionVariant = null,
            typicalMah = 4805,
            ratedMah = 4680.0,
            codeNames = listOf("CPH2551")
        ),

        // --- OPPO FIND SERIES ---
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find N3",
            regionVariant = null,
            typicalMah = 4805,
            ratedMah = 4680.0,
            codeNames = listOf("CPH2499")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find N3 Flip",
            regionVariant = null,
            typicalMah = 4300,
            ratedMah = 4190.0,
            codeNames = listOf("CPH2519")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find X7 Ultra",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4860.0,
            codeNames = listOf("PHY110")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find X8",
            regionVariant = null,
            typicalMah = 5630,
            ratedMah = 5490.0,
            codeNames = listOf("PKB110", "CPH2651")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find X8 Pro",
            regionVariant = null,
            typicalMah = 5910,
            ratedMah = 5770.0,
            codeNames = listOf("PKC110", "CPH2659")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find X9",
            regionVariant = "Global & Cina",
            typicalMah = 7025,
            ratedMah = 6840.0,
            codeNames = listOf("PMB110", "CPH2751")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Find X9 Pro",
            regionVariant = "Global & Cina",
            typicalMah = 7500,
            ratedMah = 7290.0,
            codeNames = listOf("PMC110", "CPH2759")
        ),

        // --- OPPO RENO SERIES ---
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 10 Pro",
            regionVariant = null,
            typicalMah = 4600,
            ratedMah = 4440.0,
            codeNames = listOf("CPH2525")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 11 Pro",
            regionVariant = null,
            typicalMah = 4600,
            ratedMah = 4440.0,
            codeNames = listOf("CPH2607")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 12",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4880.0,
            codeNames = listOf("CPH2625")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 12 Pro",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4880.0,
            codeNames = listOf("CPH2629")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 14",
            regionVariant = "Global/EU",
            typicalMah = 6000,
            ratedMah = 5840.0,
            codeNames = listOf("CPH2737", "CPH2739")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 14 Pro",
            regionVariant = null,
            typicalMah = 6200,
            ratedMah = 6060.0,
            codeNames = listOf("CPH2741")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 15",
            regionVariant = "Global/EU",
            typicalMah = 6500,
            ratedMah = 6335.0,
            codeNames = listOf("CPH2761")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 15 Pro",
            regionVariant = null,
            typicalMah = 6200,
            ratedMah = 6040.0,
            codeNames = listOf("CPH2765")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 16",
            regionVariant = "Global/EU",
            typicalMah = 6000,
            ratedMah = 5820.0,
            codeNames = listOf("CPH2781")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 16",
            regionVariant = "Cina",
            typicalMah = 6700,
            ratedMah = 6490.0,
            codeNames = listOf("PKT110")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 16 Pro",
            regionVariant = "Cina",
            typicalMah = 7000,
            ratedMah = 6840.0,
            codeNames = listOf("PRB110")
        ),
        DeviceBatteryPreset(
            brand = "Oppo",
            modelName = "Reno 16 Pro",
            regionVariant = "Global/EU",
            typicalMah = 6000,
            ratedMah = 5820.0,
            codeNames = listOf("CPH2785")
        ),

        // --- REALME ---
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "12 Pro+",
            regionVariant = null,
            typicalMah = 5000,
            ratedMah = 4880.0,
            codeNames = listOf("RMX3840", "RMX3841")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "13 Pro+",
            regionVariant = null,
            typicalMah = 5200,
            ratedMah = 5050.0,
            codeNames = listOf("RMX3921", "RMX3920")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "14 Pro+",
            regionVariant = null,
            typicalMah = 6000,
            ratedMah = 5850.0,
            codeNames = listOf("RMX3980", "RMX3981")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 5 Pro",
            regionVariant = null,
            typicalMah = 5400,
            ratedMah = 5260.0,
            codeNames = listOf("RMX3888")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 6",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("RMX3851", "RMX3850")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 6T",
            regionVariant = null,
            typicalMah = 5500,
            ratedMah = 5360.0,
            codeNames = listOf("RMX3853")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 7 Pro",
            regionVariant = "Global/EU",
            typicalMah = 6500,
            ratedMah = 6310.0,
            codeNames = listOf("RMX5011", "RMX5012")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 7 Pro",
            regionVariant = "Cina",
            typicalMah = 6500,
            ratedMah = 6310.0,
            codeNames = listOf("RMX5010")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 7 Pro",
            regionVariant = "India",
            typicalMah = 5800,
            ratedMah = 5660.0,
            codeNames = listOf("RMX5011_IN")
        ),
        DeviceBatteryPreset(
            brand = "Realme",
            modelName = "GT 8 Pro",
            regionVariant = "Global & Cina",
            typicalMah = 7000,
            ratedMah = 6850.0,
            codeNames = listOf("RMX5080", "RMX5081")
        )
    )

    // Lista ordinata rigorosamente in ordine alfabetico per displayName
    val ALL_PRESETS: List<DeviceBatteryPreset> = RAW_PRESETS.sortedBy { it.displayName.lowercase() }

    /**
     * Rileva se il dispositivo corrente corrisponde a uno dei preset noti
     */
    fun detectDevicePreset(model: String = Build.MODEL): DeviceBatteryPreset? {
        val upper = model.uppercase()
        return ALL_PRESETS.firstOrNull { preset ->
            preset.codeNames.any { code -> upper.contains(code.uppercase()) }
        }
    }
}
