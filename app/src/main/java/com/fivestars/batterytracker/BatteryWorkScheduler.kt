package com.fivestars.batterytracker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BatteryWorkScheduler {

    private const val TAG = "BatteryWorkScheduler"
    const val WORK_NAME = "PeriodicBatteryCheck"
    private const val LEGACY_WORK_NAME = "DailyBatteryCheck"

    /**
     * Programma o aggiorna il campionamento automatico della batteria con WorkManager.
     *
     * @param context Context dell'applicazione
     * @param isEnabled true se il campionamento è attivo, false per disattivarlo e cancellare il work
     * @param intervalHours intervallo di ripetizione (24h, 48h, 168h)
     * @param targetHour ora del giorno desiderata (0..23)
     * @param targetMinute minuto dell'ora desiderato (0..59)
     * @param forceReschedule se true, forza la cancellazione e la riprogrammazione immediata (es. modifica dalle Impostazioni)
     */
    fun schedule(
        context: Context,
        isEnabled: Boolean,
        intervalHours: Long,
        targetHour: Int,
        targetMinute: Int,
        forceReschedule: Boolean = false
    ) {
        val workManager = WorkManager.getInstance(context)

        // Cancella eventuali residui del vecchio worker hardcodato a 24h
        try {
            workManager.cancelUniqueWork(LEGACY_WORK_NAME)
        } catch (_: Exception) {}

        if (!isEnabled) {
            Log.d(TAG, "Campionamento automatico disabilitato dall'utente. Cancellazione work '$WORK_NAME'.")
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val safeIntervalHours = if (intervalHours in listOf(24L, 48L, 168L)) intervalHours else 24L
        val safeHour = targetHour.coerceIn(0, 23)
        val safeMinute = targetMinute.coerceIn(0, 59)

        val initialDelayMillis = calculateInitialDelayMillis(safeHour, safeMinute)

        Log.d(
            TAG,
            "Pianificazione campionamento: ogni ${safeIntervalHours}h alle ${String.format("%02d:%02d", safeHour, safeMinute)}. " +
                    "Ritardo iniziale: ${initialDelayMillis / (1000 * 60)} min. ForceReschedule: $forceReschedule"
        )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<BatteryWorker>(safeIntervalHours, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        val policy = if (forceReschedule) {
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            policy,
            periodicWork
        )
    }

    /**
     * Calcola il ritardo in millisecondi tra l'orario corrente e il prossimo orario target specificato.
     * Se l'orario per oggi è già trascorso, programma per il giorno successivo.
     */
    fun calculateInitialDelayMillis(
        targetHour: Int,
        targetMinute: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now) || timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - currentTimeMillis).coerceAtLeast(0L)
    }

    /**
     * Inizializza il worker all'avvio dell'applicazione leggendo le impostazioni correnti.
     * Utilizza ExistingPeriodicWorkPolicy.KEEP per non interrompere un ciclo già schedulato.
     */
    fun initAtStartup(context: Context) {
        val prefs = BatteryPreferences(context)
        val config = prefs.getSamplingConfig()
        schedule(
            context = context,
            isEnabled = config.isEnabled,
            intervalHours = config.intervalHours,
            targetHour = config.targetHour,
            targetMinute = config.targetMinute,
            forceReschedule = false
        )
    }
}
