package com.fivestars.batterytracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batteryData: BatteryData)

    @Query("SELECT * FROM battery_data ORDER BY timestamp DESC")
    fun getAllBatteryData(): Flow<List<BatteryData>>

    @Query("SELECT * FROM battery_data ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBatteryData(): Flow<BatteryData?>
}
