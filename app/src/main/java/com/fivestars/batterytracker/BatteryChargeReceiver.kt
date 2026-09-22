package com.fivestars.batterytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryChargeReceiver : BroadcastReceiver() {

    private val tag = "BatteryChargeReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d(tag, "Cavo di ricarica scollegato. Verifica livello batteria per auto-campionamento...")

            // Leggi il livello attuale della batteria
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else -1

            Log.d(tag, "Livello batteria al distacco: $batteryPct%")

            // Se il telefono era al 100%, esegui il campionamento automatico a fine ricarica
            if (batteryPct >= 100) {
                Log.d(tag, "Batteria carica al 100%! Avvio auto-campionamento...")
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = BatteryRepository(context)
                        val snapshot = repository.getBatterySnapshot()

                        val database = AppDatabase.getDatabase(context)
                        val record = BatteryData(
                            timestamp = System.currentTimeMillis(),
                            cycleCount = snapshot.cycleCount,
                            healthPercentage = snapshot.healthPercentage,
                            currentCapacityMah = snapshot.currentCapacityMah,
                            source = "${snapshot.source} (Fine Ricarica)"
                        )
                        database.batteryDao().insert(record)

                        // Notifica all'utente l'avvenuto campionamento
                        NotificationHelper.showSamplingNotification(
                            context = context,
                            isManual = false,
                            snapshot = snapshot,
                            isFullChargeTrigger = true
                        )
                        Log.d(tag, "Auto-campionamento completato e salvato con successo.")
                    } catch (e: Exception) {
                        Log.e(tag, "Errore durante l'auto-campionamento a fine ricarica", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
