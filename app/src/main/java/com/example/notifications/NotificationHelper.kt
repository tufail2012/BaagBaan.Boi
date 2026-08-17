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

import android.net.Uri
import java.util.Calendar
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_BOOKINGS_ID = "channel_bookings"
    const val CHANNEL_REMINDERS_ID = "channel_reminders"
    const val CHANNEL_SEASONAL_ID = "channel_seasonal_reminders"

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

            val seasonalChannel = NotificationChannel(
                CHANNEL_SEASONAL_ID,
                "Seasonal Orchard Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Annual recurring reminders for pruning, grafting, spraying, fertilizing and harvest"
                setShowBadge(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(bookingChannel)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(seasonalChannel)
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
        badgeCount: Int = 1,
        deepLinkUri: Uri? = null,
        extraFlagKey: String? = null,
        extraFlagValue: Boolean = false
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
            if (deepLinkUri != null) {
                data = deepLinkUri
            }
            if (extraFlagKey != null) {
                putExtra(extraFlagKey, extraFlagValue)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

        val appLockPrefs = com.example.security.AppLockPreferences(context)
        val shouldProtect = appLockPrefs.isAppLockEnabled && appLockPrefs.protectNotifications

        val displayTitle = if (shouldProtect) "AgriCrop Security Alert" else title
        val displayMessage = if (shouldProtect) "New notification content hidden while app is locked" else message

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(displayTitle)
            .setContentText(displayMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayMessage))
            .setVisibility(if (shouldProtect) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
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

    fun computeNextSeasonalTriggerMillis(
        month: Int,
        day: Int,
        hour: Int = 9,
        minute: Int = 0
    ): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            val maxDayInMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDayInMonth))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun scheduleAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        triggerAtMillis: Long,
        isSeasonal: Boolean = false,
        taskId: String? = null,
        month: Int? = null,
        day: Int? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("notificationId", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("isSeasonal", isSeasonal)
            if (taskId != null) putExtra("taskId", taskId)
            if (month != null) putExtra("month", month)
            if (day != null) putExtra("day", day)
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
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cancelAlarmNotification(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
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
