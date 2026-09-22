package com.fivestars.batterytracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batteryData: BatteryData): Long

    @Query("SELECT * FROM battery_records WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActiveBatteryData(): Flow<List<BatteryData>>

    @Query("SELECT * FROM battery_records WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getAllTrashedBatteryData(): Flow<List<BatteryData>>

    @Query("SELECT * FROM battery_records WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllActiveBatteryDataSync(): List<BatteryData>

    @Query("SELECT * FROM battery_records WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBatteryData(): Flow<BatteryData?>

    @Query("UPDATE battery_records SET isDeleted = 1 WHERE id = :id")
    suspend fun moveToTrash(id: Int)

    @Query("UPDATE battery_records SET isDeleted = 1 WHERE id IN (:ids)")
    suspend fun moveMultipleToTrash(ids: List<Int>)

    @Query("UPDATE battery_records SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Query("UPDATE battery_records SET isDeleted = 0 WHERE id IN (:ids)")
    suspend fun restoreMultipleFromTrash(ids: List<Int>)

    @Query("DELETE FROM battery_records WHERE id = :id")
    suspend fun deletePermanently(id: Int)

    @Query("DELETE FROM battery_records WHERE id IN (:ids)")
    suspend fun deleteMultiplePermanently(ids: List<Int>)

    @Query("DELETE FROM battery_records WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM battery_records")
    suspend fun deleteAll()
}
