package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.AppNotification
import com.example.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_BOOKINGS_ID = "channel_bookings"
    const val CHANNEL_REMINDERS_ID = "channel_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bookingChannel = NotificationChannel(
                CHANNEL_BOOKINGS_ID,
                "Booking Confirmations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications sent when a booking is saved or updated"
                setShowBadge(true)
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Task & Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled visits, deliveries, or custom tasks"
                setShowBadge(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(bookingChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postSystemNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_BOOKINGS_ID,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        badgeCount: Int = 1
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, return gracefully
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setNumber(badgeCount)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Fallback gracefully if system blocks notification
        }
    }

    fun scheduleAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("notificationId", notificationId)
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun postBookingConfirmation(
        context: Context,
        repository: NotificationRepository,
        scope: CoroutineScope,
        farmerName: String,
        serviceType: String,
        serialNo: String
    ) {
        val title = "Booking Confirmed! 🌾"
        val message = "Booking #$serialNo saved for $farmerName ($serviceType)."

        scope.launch(Dispatchers.IO) {
            val id = repository.insertNotification(
                AppNotification(
                    title = title,
                    message = message,
                    type = "BOOKING"
                )
            )
            postSystemNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_BOOKINGS_ID,
                notificationId = id.toInt()
            )
        }
    }

    fun postReminderNotification(
        context: Context,
        repository: NotificationRepository,
        scope: CoroutineScope,
        title: String,
        message: String,
        triggerAtMillis: Long = System.currentTimeMillis()
    ) {
        val fullTitle = if (title.startsWith("Reminder")) title else "Reminder ⏰: $title"
        val now = System.currentTimeMillis()

        scope.launch(Dispatchers.IO) {
            val id = repository.insertNotification(
                AppNotification(
                    title = fullTitle,
                    message = message,
                    timestamp = triggerAtMillis,
                    type = "REMINDER"
                )
            )

            if (triggerAtMillis <= now + 1000L) {
                postSystemNotification(
                    context = context,
                    title = fullTitle,
                    message = message,
                    channelId = CHANNEL_REMINDERS_ID,
                    notificationId = id.toInt()
                )
            } else {
                scheduleAlarmNotification(
                    context = context,
                    notificationId = id.toInt(),
                    title = fullTitle,
                    message = message,
                    triggerAtMillis = triggerAtMillis
                )
            }
        }
    }
}
