package com.example.chat.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.zegocloud.uikit.prebuilt.call.invite.internal.CallInviteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class OutgoingCallNotificationService : Service() {

    companion object {
        private const val TAG = "OutgoingCallService"
        const val CHANNEL_ID = "outgoing_call_channel"
        const val NOTIFICATION_ID = 9021

        private const val ACTION_START = "com.example.chat.call.ACTION_START"
        private const val ACTION_UPDATE = "com.example.chat.call.ACTION_UPDATE"
        private const val ACTION_STOP = "com.example.chat.call.ACTION_STOP"
        const val ACTION_HANG_UP = "com.example.chat.call.ACTION_HANG_UP"

        private const val EXTRA_TARGET_NAME = "extra_target_name"
        private const val EXTRA_STATUS_TEXT = "extra_status_text"
        private const val EXTRA_PHOTO_URL = "extra_photo_url"
        private const val EXTRA_IS_VIDEO = "extra_is_video"

        fun start(
            context: Context,
            targetName: String,
            statusText: String,
            photoUrl: String?,
            isVideo: Boolean
        ) {
            val intent = Intent(context, OutgoingCallNotificationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_NAME, targetName)
                putExtra(EXTRA_STATUS_TEXT, statusText)
                putExtra(EXTRA_PHOTO_URL, photoUrl)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start OutgoingCallNotificationService: ${e.message}", e)
            }
        }

        fun update(
            context: Context,
            targetName: String,
            statusText: String,
            photoUrl: String?,
            isVideo: Boolean
        ) {
            val intent = Intent(context, OutgoingCallNotificationService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TARGET_NAME, targetName)
                putExtra(EXTRA_STATUS_TEXT, statusText)
                putExtra(EXTRA_PHOTO_URL, photoUrl)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            try {
                context.startService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to update OutgoingCallNotificationService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OutgoingCallNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to stop OutgoingCallNotificationService: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var imageLoadingJob: Job? = null
    private var cachedAvatarBitmap: Bitmap? = null
    private var lastPhotoUrl: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundNotification()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HANG_UP -> {
                Log.i(TAG, "User clicked Hang Up action from notification")
                ZegoCallManager.endCall()
                stopForegroundNotification()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE -> {
                val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: "Calling…"
                val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "Calling…"
                val photoUrl = intent.getStringExtra(EXTRA_PHOTO_URL)
                val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

                handleShowNotification(targetName, statusText, photoUrl, isVideo)
            }
        }
        return START_NOT_STICKY
    }

    private fun handleShowNotification(
        targetName: String,
        statusText: String,
        photoUrl: String?,
        isVideo: Boolean
    ) {
        val notification = buildNotification(targetName, statusText, cachedAvatarBitmap, isVideo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Asynchronously load avatar photo if URL changed and available
        if (!photoUrl.isNullOrBlank() && photoUrl != lastPhotoUrl) {
            lastPhotoUrl = photoUrl
            imageLoadingJob?.cancel()
            imageLoadingJob = serviceScope.launch {
                val bitmap = loadBitmapFromUrl(photoUrl)
                if (bitmap != null) {
                    cachedAvatarBitmap = bitmap
                    val updatedNotification = buildNotification(targetName, statusText, bitmap, isVideo)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, updatedNotification)
                }
            }
        }
    }

    private fun buildNotification(
        targetName: String,
        statusText: String,
        avatarBitmap: Bitmap?,
        isVideo: Boolean
    ): Notification {
        // Tapping notification reopens call screen
        val contentIntent = Intent(this, CallInviteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("page", "page_call")
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hang up action intent
        val hangUpIntent = Intent(this, OutgoingCallNotificationService::class.java).apply {
            action = ACTION_HANG_UP
        }
        val pendingHangUpIntent = PendingIntent.getService(
            this,
            1,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isVideo) "Outgoing Video Call: $targetName" else "Outgoing Voice Call: $targetName"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingContentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline / End", pendingHangUpIntent)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        return builder.build()
    }

    private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.getInputStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Active Call Notification"
            val desc = "Shows persistent status while placing an outgoing call"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundNotification() {
        imageLoadingJob?.cancel()
        imageLoadingJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        stopForegroundNotification()
        super.onDestroy()
    }
}
