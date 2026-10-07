package com.fivestars.batterytracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.fivestars.batterytracker.action.REFRESH_WIDGET"
        private const val TAG = "BatteryWidgetProvider"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore durante l'aggiornamento asincrono dei widget", e)
                    }
                }
            }
        }

        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val repository = BatteryRepository(context)
            val snapshot = repository.getBatterySnapshot()

            val latestDbRecord = try {
                AppDatabase.getDatabase(context).batteryDao().getAllActiveBatteryDataSync().firstOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Impossibile recuperare record recente dal database per il widget", e)
                null
            }

            // 1. SOH %
            val soh = snapshot.healthPercentage ?: latestDbRecord?.healthPercentage
            val sohStr = if (soh != null) "$soh%" else "--%"
            val sohColor = when {
                soh == null -> 0xFF00E5FF.toInt()
                soh >= 90 -> 0xFF00E5FF.toInt()   // Ciano brillante
                soh >= 80 -> 0xFFFFB300.toInt()   // Ambra
                else -> 0xFFEF4444.toInt()        // Rosso degradazione
            }
            val sohSub = if (snapshot.isHealthCalculated) {
                context.getString(R.string.card_health_sub)
            } else {
                context.getString(R.string.card_health_sub_bms)
            }

            // 2. Cicli di ricarica
            val cycles = snapshot.cycleCount ?: latestDbRecord?.cycleCount
            val cyclesStr = if (cycles != null) "$cycles" else "--"
            val cyclesSub = context.getString(R.string.card_cycles_sub)

            // 3. Wattaggio Live
            val watts = snapshot.chargingPowerWatts
            val isCharging = watts != null && watts > 0.0
            val isDischarging = watts != null && watts < -0.05
            val wattStr = when {
                watts == null -> "0.0 W"
                isCharging -> "+%.1f W".format(Locale.US, watts)
                else -> "%.1f W".format(Locale.US, watts)
            }
            val wattColor = when {
                isCharging -> 0xFF00E676.toInt()       // Verde smeraldo brillante per ricarica
                isDischarging -> 0xFFFFB300.toInt()    // Ambra per assorbimento/scarica
                else -> 0xFF94A3B8.toInt()             // Grigio neutro per standby
            }
            val wattSub = when {
                isCharging -> context.getString(R.string.widget_charging)
                isDischarging -> context.getString(R.string.widget_discharging)
                else -> context.getString(R.string.diag_standby)
            }

            // 4. Temperatura Cella
            val tempC = snapshot.batteryTemperatureCelsius
            val tempStr = if (tempC != null) "%.1f°C".format(Locale.US, tempC) else "--°C"
            val tempColor = when {
                tempC == null -> 0xFF38BDF8.toInt()
                tempC > 42.0 -> 0xFFEF4444.toInt()     // Allarme rosso surriscaldamento
                tempC >= 38.0 -> 0xFFFB923C.toInt()    // Arancione caldo
                else -> 0xFF38BDF8.toInt()             // Celeste ottimale
            }
            val tempSub = when {
                tempC == null -> context.getString(R.string.not_available)
                tempC > 42.0 -> "Allarme"
                tempC >= 38.0 -> "Caldo"
                else -> "Ottimale"
            }

            // Livello batteria e timestamp
            val level = snapshot.batteryLevelPercentage ?: repository.getBatteryLevelPercentage()
            val levelPrefix = if (level != null) "⚡ $level%" else "⚡"
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = timeFormat.format(Date())
            val updatedFullStr = context.getString(R.string.widget_updated_format, timeStr)

            // PendingIntent per aprire l'app principale al tocco
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // PendingIntent per il pulsante di aggiornamento manuale
            val refreshIntent = Intent(context, BatteryWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // --- Layout Compatto (2x1) ---
            val viewsCompact = RemoteViews(context.packageName, R.layout.widget_battery_compact).apply {
                setOnClickPendingIntent(R.id.widget_compact_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_compact_btn_refresh, refreshPendingIntent)

                setTextViewText(R.id.widget_compact_title, "$levelPrefix • ${context.getString(R.string.app_name)}")
                setTextViewText(R.id.widget_compact_updated, timeStr)

                setTextViewText(R.id.widget_compact_soh_val, sohStr)
                setTextColor(R.id.widget_compact_soh_val, sohColor)

                setTextViewText(R.id.widget_compact_cycles_val, cyclesStr)

                setTextViewText(R.id.widget_compact_watt_val, wattStr)
                setTextColor(R.id.widget_compact_watt_val, wattColor)
                setInt(R.id.widget_compact_watt_icon, "setColorFilter", wattColor)

                setTextViewText(R.id.widget_compact_temp_val, tempStr)
                setTextColor(R.id.widget_compact_temp_val, tempColor)
                setInt(R.id.widget_compact_temp_icon, "setColorFilter", tempColor)
            }

            // --- Layout Espanso (2x2) ---
            val viewsExpanded = RemoteViews(context.packageName, R.layout.widget_battery_expanded).apply {
                setOnClickPendingIntent(R.id.widget_expanded_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_expanded_btn_refresh, refreshPendingIntent)

                setTextViewText(R.id.widget_expanded_title, "${context.getString(R.string.app_name)}  $levelPrefix")
                setTextViewText(R.id.widget_expanded_updated, updatedFullStr)

                // Card SOH
                setTextViewText(R.id.widget_expanded_soh_val, sohStr)
                setTextColor(R.id.widget_expanded_soh_val, sohColor)
                setTextViewText(R.id.widget_expanded_soh_sub, sohSub)

                // Card Cicli
                setTextViewText(R.id.widget_expanded_cycles_val, cyclesStr)
                setTextViewText(R.id.widget_expanded_cycles_sub, cyclesSub)

                // Card Watt
                setTextViewText(R.id.widget_expanded_watt_val, wattStr)
                setTextColor(R.id.widget_expanded_watt_val, wattColor)
                setInt(R.id.widget_expanded_watt_icon, "setColorFilter", wattColor)
                setTextViewText(R.id.widget_expanded_watt_sub, wattSub)

                // Card Temp
                setTextViewText(R.id.widget_expanded_temp_val, tempStr)
                setTextColor(R.id.widget_expanded_temp_val, tempColor)
                setInt(R.id.widget_expanded_temp_icon, "setColorFilter", tempColor)
                setTextViewText(R.id.widget_expanded_temp_sub, tempSub)
            }

            // Responsive Layout per Android 12+ (minSdk 34)
            val viewMapping = mapOf(
                SizeF(130f, 60f) to viewsCompact,
                SizeF(180f, 110f) to viewsExpanded
            )
            val responsiveViews = RemoteViews(viewMapping)

            for (appWidgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(appWidgetId, responsiveViews)
            }
            Log.d(TAG, "Aggiornamento completato con successo per ${appWidgetIds.size} widget.")
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.e(TAG, "Errore in onUpdate widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")
        when (action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_BOOT_COMPLETED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore in onReceive widget refresh", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
