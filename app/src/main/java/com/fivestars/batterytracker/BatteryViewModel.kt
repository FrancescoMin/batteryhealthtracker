package com.fivestars.batterytracker

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val allBatteryData: Flow<List<BatteryData>> = database.batteryDao().getAllBatteryData()
    val latestBatteryData: Flow<BatteryData?> = database.batteryDao().getLatestBatteryData()

    private val workManager = WorkManager.getInstance(application)

    // Observers WorkInfo to find out when the next periodic work is scheduled
    val nextWorkScheduleTime: Flow<Long?> = workManager.getWorkInfosForUniqueWorkFlow("DailyBatteryCheck").map { workInfos ->
        val workInfo = workInfos.firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        workInfo?.nextScheduleTimeMillis
    }

    fun getCurrentBatteryStats(context: Context): Pair<Int, Int> {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Use property 10 for BATTERY_PROPERTY_STATE_OF_HEALTH, 7 for BATTERY_PROPERTY_CYCLE_COUNT
            val health = batteryManager.getIntProperty(10)
            var cycleCount = batteryManager.getIntProperty(7)

            if (health == Int.MIN_VALUE || cycleCount == Int.MIN_VALUE) {
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                if (cycleCount == Int.MIN_VALUE && intent != null) {
                    cycleCount = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                }
            }

            Pair(health, cycleCount)
        } catch (e: Exception) {
            Log.e("BatteryTracker", "Permission denied or other error getting stats", e)
            Pair(-1, -1) // Return invalid data instead of crashing if BATTERY_STATS is not granted via adb yet
        }
    }

    fun saveCurrentBatteryData(context: Context) {
        viewModelScope.launch {
            val stats = getCurrentBatteryStats(context)
            // Don't save if it crashed due to permissions.
            if (stats.first != -1 && stats.second != -1) {
                val data = BatteryData(
                    timestamp = System.currentTimeMillis(),
                    healthStatus = stats.first,
                    cycleCount = stats.second
                )
                database.batteryDao().insert(data)
            }
        }
    }
}
