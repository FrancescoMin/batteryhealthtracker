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

            val health = batteryManager.getIntProperty(10) // BATTERY_PROPERTY_STATE_OF_HEALTH
            var cycleCount = batteryManager.getIntProperty(7) // BATTERY_PROPERTY_CYCLE_COUNT

            if (health == Int.MIN_VALUE || cycleCount == Int.MIN_VALUE) {
                // fallback to intent
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
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
        } catch (e: SecurityException) {
            // Missing BATTERY_STATS permission granted via ADB
            Log.e("BatteryWorker", "Missing BATTERY_STATS permission", e)
            Result.failure() // Stop retrying since it's a permanent error until adb grant
        } catch (e: Exception) {
            Log.e("BatteryWorker", "Error saving battery data", e)
            Result.retry() // temporary issue, maybe room DB locked
        }
    }
}
