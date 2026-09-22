package com.fivestars.batterytracker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BatteryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = BatteryRepository(context)
            val snapshot = repository.getBatterySnapshot()

            Log.d("BatteryWorker", "Campionamento eseguito: $snapshot")

            val database = AppDatabase.getDatabase(context)
            val record = BatteryData(
                timestamp = System.currentTimeMillis(),
                cycleCount = snapshot.cycleCount,
                healthPercentage = snapshot.healthPercentage,
                currentCapacityMah = snapshot.currentCapacityMah,
                source = snapshot.source
            )

            database.batteryDao().insert(record)
            Log.d("BatteryWorker", "Record salvato con successo in Room")

            // Mostra la notifica di sistema per il campionamento automatico
            NotificationHelper.showSamplingNotification(context, isManual = false, snapshot)

            Result.success()
        } catch (e: Exception) {
            Log.e("BatteryWorker", "Errore durante il monitoraggio periodico della batteria", e)
            Result.retry()
        }
    }
}
