package com.fivestars.batterytracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatterySohWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_SOH_WIDGET = "com.fivestars.batterytracker.action.REFRESH_SOH_WIDGET"
        private const val TAG = "BatterySohWidgetProvider"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, BatterySohWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore durante l'aggiornamento dei widget SOH 1x1", e)
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
                Log.w(TAG, "Impossibile recuperare record recente dal database per il widget SOH", e)
                null
            }

            // Salute reale (SOH %) dal chip BMS o dal record recente
            val soh = snapshot.healthPercentage ?: latestDbRecord?.healthPercentage
            val sohStr = if (soh != null) "$soh%" else "--%"
            val sohColor = when {
                soh == null -> 0xFF00E5FF.toInt()
                soh >= 90 -> 0xFF00E5FF.toInt()   // Ciano brillante
                soh >= 80 -> 0xFFFFB300.toInt()   // Ambra
                else -> 0xFFEF4444.toInt()        // Rosso degradazione
            }

            // PendingIntent per aprire l'applicazione al tocco sul widget 1x1
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_battery_soh_1x1).apply {
                setOnClickPendingIntent(R.id.widget_soh_root, openAppPendingIntent)
                setTextViewText(R.id.widget_soh_val, sohStr)
                setTextColor(R.id.widget_soh_val, sohColor)
                setInt(R.id.widget_soh_icon, "setColorFilter", sohColor)
            }

            for (appWidgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            Log.d(TAG, "Widget SOH 1x1 aggiornati con successo: SOH=$sohStr per ${appWidgetIds.size} widget.")
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.e(TAG, "Errore in onUpdate widget SOH", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.d(TAG, "onReceive SOH widget action: $action")
        when (action) {
            ACTION_REFRESH_SOH_WIDGET,
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
                        Log.e(TAG, "Errore in onReceive SOH widget refresh", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
