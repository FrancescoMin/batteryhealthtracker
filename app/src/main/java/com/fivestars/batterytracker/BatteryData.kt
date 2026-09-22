package com.fivestars.batterytracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_records")
data class BatteryData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val cycleCount: Int?,
    val healthPercentage: Int?,
    val currentCapacityMah: Double?,
    val source: String,
    val isDeleted: Boolean = false
)
