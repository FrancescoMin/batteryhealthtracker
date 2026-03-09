package com.fivestars.batterytracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_data")
data class BatteryData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val cycleCount: Int,
    val healthStatus: Int
)
