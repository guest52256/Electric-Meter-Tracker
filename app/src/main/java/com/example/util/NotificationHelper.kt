package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "meter_usage_alerts"
    private const val CHANNEL_NAME = "Electricity Usage Alerts"
    private const val CHANNEL_DESC = "Notifications for 100+ units electric usage alerts"

    private const val ONGOING_CHANNEL_ID = "meter_tracker_ongoing"
    private const val ONGOING_NOTIFICATION_ID = 1001

    fun showOngoingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                "App Ongoing Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows quick actions while the app is active"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Base intent to just open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Add Bill Cycle action
        val billCycleIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_ADD_BILL_CYCLE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val billCyclePendingIntent = PendingIntent.getActivity(
            context, 1, billCycleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Add Reading action
        val readingIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_ADD_READING"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val readingPendingIntent = PendingIntent.getActivity(
            context, 2, readingIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Developer Info action
        val devInfoIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_DEV_INFO"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val devInfoPendingIntent = PendingIntent.getActivity(
            context, 3, devInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Electric Meter Tracker")
            .setContentText("Quick actions available")
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Add Bill Cycle", billCyclePendingIntent)
            .addAction(0, "Add Reading", readingPendingIntent)
            .addAction(0, "Dev Info", devInfoPendingIntent)
            .build()
            
        try {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun hideOngoingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendHighUsageNotification(
        context: Context,
        meterName: String,
        unitsUsed: Double,
        currentReading: Double,
        previousBillReading: Double
    ) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val unitsInt = if (unitsUsed % 1.0 == 0.0) unitsUsed.toInt().toString() else "%.1f".format(unitsUsed)
            val title = "⚠️ Electricity Usage Alert (100+ Units)"
            val contentText = "$meterName: $unitsInt units used since last bill! (Current: $currentReading, Bill: $previousBillReading)"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("⚠️ Electricity Usage Alert: 100 or more units have been used since the last bill reading.\n\nMeter: $meterName\nUnits Consumed: $unitsInt Units\nCurrent Reading: $currentReading\nPrevious Bill Reading: $previousBillReading")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted or restricted
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
