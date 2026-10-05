package com.rtgrowth.admin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rtgrowth.admin.R
import com.rtgrowth.admin.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_ALERTS = "rt_growth_alerts"
    const val CHANNEL_MESSAGES = "rt_growth_messages"
    const val CHANNEL_SERVICE = "rt_growth_service"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Transaction & Task Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Deposit, Withdraw, Recharge and Task alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Support Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct user support messages"
                enableVibration(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Background Engine Sync",
                NotificationManager.IMPORTANCE_LOW
            )

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, isChat: Boolean = false) {
        val channelId = if (isChat) CHANNEL_MESSAGES else CHANNEL_ALERTS
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
