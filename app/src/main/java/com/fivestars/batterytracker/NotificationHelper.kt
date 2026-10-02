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
    const val CHANNEL_OVERHEAT_ID = "battery_overheat_channel"

    private const val NOTIFICATION_ID = 1001
    private const val NOTIFICATION_OVERHEAT_ID = 1002
    private const val NOTIFICATION_FULL_CHARGE_ID = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val samplingChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_sampling_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_sampling_desc)
            }

            val overheatChannel = NotificationChannel(
                CHANNEL_OVERHEAT_ID,
                context.getString(R.string.notif_channel_overheat_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_overheat_desc)
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

        val formattedTemp = String.format(Locale.US, "%.1f°C", tempCelsius)
        val title = context.getString(R.string.notif_overheat_title, formattedTemp)
        val text = context.getString(R.string.notif_overheat_text)

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
            isFullChargeTrigger -> context.getString(R.string.notif_sampling_full_title)
            isManual -> context.getString(R.string.notif_sampling_manual_title)
            else -> context.getString(R.string.notif_sampling_auto_title)
        }

        val triggerReason = when {
            isFullChargeTrigger -> context.getString(R.string.notif_sampling_full_reason)
            isManual -> context.getString(R.string.notif_sampling_manual_reason)
            else -> context.getString(R.string.notif_sampling_auto_reason)
        }

        val notAvailable = context.getString(R.string.not_available)
        val healthStr = snapshot.healthPercentage?.let { "$it%" } ?: notAvailable
        val cyclesStr = snapshot.cycleCount?.let { "$it" } ?: notAvailable
        val capStr = snapshot.currentCapacityMah?.let { String.format(Locale.US, "%.0f mAh", it) } ?: notAvailable

        val summaryText = context.getString(R.string.notif_sampling_summary, healthStr, cyclesStr, capStr)
        val expandedText = context.getString(R.string.notif_sampling_expanded, triggerReason, healthStr, cyclesStr, capStr, snapshot.source)

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
