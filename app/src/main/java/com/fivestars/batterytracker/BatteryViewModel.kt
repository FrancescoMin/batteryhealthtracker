package com.fivestars.batterytracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val allBatteryData: Flow<List<BatteryData>> = database.batteryDao().getAllBatteryData()
    val latestBatteryData: Flow<BatteryData?> = database.batteryDao().getLatestBatteryData()
}
