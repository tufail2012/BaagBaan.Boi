package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class ReminderBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task Reminder ⏰"
        val message = intent.getStringExtra("message") ?: "You have a scheduled task reminder."
        val notificationId = intent.getIntExtra("notificationId", (System.currentTimeMillis() % 100000).toInt())
        val isSeasonal = intent.getBooleanExtra("isSeasonal", false)
        val taskId = intent.getStringExtra("taskId")
        val month = if (intent.hasExtra("month")) intent.getIntExtra("month", -1) else null
        val day = if (intent.hasExtra("day")) intent.getIntExtra("day", -1) else null

        Log.d(TAG, "onReceive triggered: id=$notificationId, title=$title, isSeasonal=$isSeasonal, month=$month, day=$day")

        val channelId = if (isSeasonal) {
            NotificationHelper.CHANNEL_SEASONAL_ID
        } else {
            NotificationHelper.CHANNEL_REMINDERS_ID
        }

        val deepLinkUri = if (isSeasonal) Uri.parse("baagbaanboi://seasonal") else null
        val extraFlagKey = if (isSeasonal) "OPEN_SEASONAL_REMINDERS" else null

        NotificationHelper.postSystemNotification(
            context = context,
            title = title,
            message = message,
            channelId = channelId,
            notificationId = notificationId,
            deepLinkUri = deepLinkUri,
            extraFlagKey = extraFlagKey,
            extraFlagValue = true
        )

        // If this is a recurring annual seasonal task, auto-reschedule for next year
        if (isSeasonal && month != null && month in 1..12 && day != null && day in 1..31) {
            val nextYearTrigger = NotificationHelper.computeNextSeasonalTriggerMillis(month, day)
            Log.d(TAG, "Auto-rescheduling yearly seasonal task ($title) for next occurrence: $nextYearTrigger")
            NotificationHelper.scheduleAlarmNotification(
                context = context,
                notificationId = notificationId,
                title = title,
                message = message,
                triggerAtMillis = nextYearTrigger,
                isSeasonal = true,
                taskId = taskId,
                month = month,
                day = day
            )
        }
    }
}

