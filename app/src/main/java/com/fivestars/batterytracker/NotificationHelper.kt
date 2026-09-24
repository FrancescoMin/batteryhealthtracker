package com.fivestars.batterytracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "battery_sampling_channel"
    private const val CHANNEL_NAME = "Monitoraggio Batteria"
    private const val CHANNEL_DESC = "Notifiche per campionamento manuale e automatico dello stato della batteria"

    const val CHANNEL_OVERHEAT_ID = "battery_overheat_channel"
    private const val CHANNEL_OVERHEAT_NAME = "Allarme Temperatura Batteria"
    private const val CHANNEL_OVERHEAT_DESC = "Avvisi di sicurezza per surriscaldamento della batteria durante la ricarica rapida o uso intenso"

    private const val NOTIFICATION_ID = 1001
    private const val NOTIFICATION_OVERHEAT_ID = 1002
    private const val NOTIFICATION_FULL_CHARGE_ID = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val samplingChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }

            val overheatChannel = NotificationChannel(
                CHANNEL_OVERHEAT_ID,
                CHANNEL_OVERHEAT_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_OVERHEAT_DESC
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(samplingChannel)
            notificationManager.createNotificationChannel(overheatChannel)
        }
    }

    fun showOverheatNotification(context: Context, tempCelsius: Double) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚠️ Allarme Temperatura Batteria: ${String.format(Locale.US, "%.1f°C", tempCelsius)}"
        val text = "La temperatura ha superato la soglia di sicurezza (42°C). Rimuovi la cover o scollega temporaneamente la ricarica SuperVOOC per preservare la salute della cella."

        val notification = NotificationCompat.Builder(context, CHANNEL_OVERHEAT_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_OVERHEAT_ID, notification)
        } catch (e: SecurityException) {
            // Permesso notifiche non ancora concesso
        }
    }

    fun showSamplingNotification(context: Context, isManual: Boolean, snapshot: BatterySnapshot, isFullChargeTrigger: Boolean = false) {
        createNotificationChannel(context)

        // Verifica permessi per Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isFullChargeTrigger -> "Ricarica 100% Completata • Campionamento Eseguito"
            isManual -> "Campionamento Manuale Eseguito"
            else -> "Campionamento Automatico (24h)"
        }

        val triggerReason = when {
            isFullChargeTrigger -> "Campionamento automatico al distacco del caricatore con batteria al 100%"
            isManual -> "Rilevazione manuale avviata tramite pulsante nell'app"
            else -> "Rilevazione periodica automatica programmata (ogni 24h)"
        }

        val healthStr = snapshot.healthPercentage?.let { "$it%" } ?: "N/D"
        val cyclesStr = snapshot.cycleCount?.let { "$it" } ?: "N/D"
        val capStr = snapshot.currentCapacityMah?.let { String.format(Locale.US, "%.0f mAh", it) } ?: "N/D"

        val summaryText = "Salute: $healthStr | Cicli: $cyclesStr | $capStr"
        val expandedText = "$triggerReason.\n\n• Salute SOH: $healthStr\n• Cicli di carica: $cyclesStr\n• Capacità residua: $capStr\n• Sorgente dati: ${snapshot.source}"

        val notifId = if (isFullChargeTrigger) NOTIFICATION_FULL_CHARGE_ID else NOTIFICATION_ID

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Permesso notifiche non ancora concesso
        }
    }
}
