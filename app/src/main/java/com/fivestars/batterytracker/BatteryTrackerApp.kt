package com.fivestars.batterytracker

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BatteryTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupDailyWork()
    }

    private fun setupDailyWork() {
        val constraints = Constraints.Builder()
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<BatteryWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyBatteryCheck",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
}
