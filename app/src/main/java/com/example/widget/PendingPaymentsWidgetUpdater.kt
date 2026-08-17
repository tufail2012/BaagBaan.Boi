package com.example.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.AgriApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PendingPaymentsWidgetUpdater {
    private const val TAG = "PendingPaymentsWidget"

    fun triggerUpdate(context: Context? = null) {
        val targetContext = context ?: try {
            AgriApplication.appContext
        } catch (e: Exception) {
            null
        } ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(targetContext)
                val glanceIds = manager.getGlanceIds(PendingPaymentsWidget::class.java)
                if (glanceIds.isNotEmpty()) {
                    Log.d(TAG, "Updating ${glanceIds.size} widget instances...")
                    PendingPaymentsWidget().updateAll(targetContext)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update Glance widget: ${e.message}", e)
            }
        }
    }
}
