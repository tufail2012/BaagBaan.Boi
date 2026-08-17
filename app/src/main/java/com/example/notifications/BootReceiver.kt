package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SeasonalTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Device boot / update broadcast received: action=$action. Rescheduling pending alarms...")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Reschedule regular task and booking reminders from Room
                    val database = AppDatabase.getDatabase(appContext)
                    val now = System.currentTimeMillis()
                    val notifications = database.notificationDao().getAllNotifications().firstOrNull() ?: emptyList()

                    var regularCount = 0
                    notifications.filter { it.timestamp > now && !it.isRead }.forEach { item ->
                        NotificationHelper.scheduleAlarmNotification(
                            context = appContext,
                            notificationId = item.id.toInt(),
                            title = item.title,
                            message = item.message,
                            triggerAtMillis = item.timestamp
                        )
                        regularCount++
                    }
                    Log.i(TAG, "Rescheduled $regularCount pending standard reminders from Room database.")

                    // 2. Reschedule annual recurring Seasonal Tasks
                    SeasonalTaskRepository.startListening(appContext)
                    SeasonalTaskRepository.rescheduleAllAlarms(appContext)
                    Log.i(TAG, "Rescheduled all configured seasonal orchard alarms successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after device boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
