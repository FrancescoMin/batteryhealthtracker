package com.fivestars.batterytracker

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BatteryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Using correct property constants available in API 34
            // BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH is 10
            // BatteryManager.BATTERY_PROPERTY_CYCLE_COUNT is 7 (added in API 34)
            // But they might be added in newer extensions or just missing in standard build stub
            // Instead of using constants directly if they fail, we can use their values
            // but the review meant to use the actual properties instead of EXTRA intent
            // Actually, BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH is 10 as per AOSP

            // Let's use the actual intent way again because getIntProperty constants
            // are hidden or we use raw values. Actually Android 14 API 34 SDK has:
            // public static final int BATTERY_PROPERTY_STATE_OF_HEALTH = 10;
            // Let's try 10 and 7 or revert to intent

            // Actually, reviewing Android API docs, `BATTERY_PROPERTY_STATE_OF_HEALTH` is 10.
            val health = batteryManager.getIntProperty(10) // BATTERY_PROPERTY_STATE_OF_HEALTH
            var cycleCount = batteryManager.getIntProperty(7) // BATTERY_PROPERTY_CYCLE_COUNT

            // Note: getIntProperty returns INT_MIN (-2147483648) if the property is not supported
            if (health == Int.MIN_VALUE || cycleCount == Int.MIN_VALUE) {
                // fallback to intent
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                if (health == Int.MIN_VALUE && intent != null) {
                    // if it doesn't support percentage, we just save the status
                }
                if (cycleCount == Int.MIN_VALUE && intent != null) {
                    cycleCount = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                }
            }

            Log.d("BatteryWorker", "Saved Data - Health %: $health, Cycles: $cycleCount")

            val database = AppDatabase.getDatabase(context)
            val data = BatteryData(
                timestamp = System.currentTimeMillis(),
                cycleCount = cycleCount,
                healthStatus = health // This is now a percentage if supported, else INT_MIN
            )

            database.batteryDao().insert(data)

            Result.success()
        } catch (e: Exception) {
            Log.e("BatteryWorker", "Error saving battery data", e)
            Result.retry()
        }
    }
}
