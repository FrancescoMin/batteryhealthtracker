package com.fivestars.batterytracker

import android.app.Application

class BatteryTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        BatteryWorkScheduler.initAtStartup(this)
    }
}
