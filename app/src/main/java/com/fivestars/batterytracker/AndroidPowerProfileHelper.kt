package com.fivestars.batterytracker

import android.content.Context

/**
 * Universal helper that queries Android's internal PowerProfile.
 * Reads battery.capacity compiled into /system/framework/framework-res.apk (res/xml/power_profile.xml).
 * Standardized across all certified Android OEMs (Samsung, Google, Xiaomi, Motorola, BBK, Sony, etc.).
 * Operates without requiring Shizuku, ADB, or Root privileges.
 */
object AndroidPowerProfileHelper {

    fun getPowerProfileCapacity(context: Context): Double? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val result = method.invoke(powerProfile)
            when (result) {
                is Double -> if (result > 500.0) result else null
                is Number -> if (result.toDouble() > 500.0) result.toDouble() else null
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
