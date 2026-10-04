package com.fivestars.batterytracker

import android.os.Build

/**
 * Unified device battery presets registry combining OPlus ecosystem and Samsung Galaxy presets.
 */
object DevicePresets {

    val ALL_PRESETS: List<DeviceBatteryPreset> = (OplusDevicePresets.ALL_PRESETS + SamsungDevicePresets.ALL_PRESETS)
        .sortedBy { it.displayName.lowercase() }

    fun detectDevicePreset(model: String = Build.MODEL): DeviceBatteryPreset? {
        return OplusDevicePresets.detectDevicePreset(model) ?: SamsungDevicePresets.detectDevicePreset(model)
    }
}
