package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task Reminder ⏰"
        val message = intent.getStringExtra("message") ?: "You have a scheduled task reminder."
        val notificationId = intent.getIntExtra("notificationId", (System.currentTimeMillis() % 100000).toInt())

        NotificationHelper.postSystemNotification(
            context = context,
            title = title,
            message = message,
            channelId = NotificationHelper.CHANNEL_REMINDERS_ID,
            notificationId = notificationId
        )
    }
}
