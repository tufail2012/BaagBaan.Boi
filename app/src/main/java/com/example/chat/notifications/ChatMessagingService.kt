package com.example.chat.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.MainActivity
import com.example.chat.data.ChatListLocalPrefs
import com.example.chat.data.ChatPreferences
import com.example.chat.data.ChatRepository
import com.example.util.SafeFirebase
import com.google.firebase.messaging.RemoteMessage
import im.zego.zpns_android_plugin_fcm.FCMMessageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatMessagingService : FCMMessageService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun handleIntent(intent: Intent?) {
        val extras = intent?.extras
        if (intent?.action == "com.google.firebase.MESSAGING_EVENT" &&
            extras != null &&
            !extras.getString("chatId").isNullOrBlank()
        ) {
            onMessageReceived(RemoteMessage(extras))
            return
        }
        super.handleIntent(intent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        val appContext = applicationContext
        val prefs = ChatPreferences(appContext)
        val uid = SafeFirebase.getAuth(appContext)?.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            serviceScope.launch {
                val result = ChatRepository(appContext).saveFcmToken(uid, token)
                if (result.isFailure) {
                    prefs.pendingFcmToken = token
                } else if (prefs.pendingFcmToken == token) {
                    prefs.pendingFcmToken = null
                }
            }
        } else {
            prefs.pendingFcmToken = token
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // If message.data does not contain "chatId", forward untouched to Zego's FCM handler and return immediately.
        val chatId = message.data["chatId"]?.trim()
        if (chatId.isNullOrEmpty()) {
            super.onMessageReceived(message)
            return
        }

        try {
            // a. If the app is in the foreground (ProcessLifecycleOwner state is at least STARTED), do nothing.
            val isForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
            if (isForeground) {
                return
            }

            val appContext = applicationContext

            // c. If ChatPreferences(context).isNotificationsEnabled is false, stop.
            if (!ChatPreferences(appContext).isNotificationsEnabled) {
                return
            }

            // d. If ChatListLocalPrefs(context) says this chat is muted (isMuted()), stop.
            val chatState = ChatListLocalPrefs(appContext).loadAll()[chatId]
            if (chatState?.isMuted() == true) {
                return
            }

            // e. If POST_NOTIFICATIONS is not granted (Android 13+), stop.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    return
                }
            }

            ensureChatNotificationChannel()

            val isLocked = chatState?.locked == true

            // f. Title and text come from message.notification (redacted when chat is locked).
            val title = if (isLocked) {
                "New message"
            } else {
                message.notification?.title
                    ?: message.data["title"]
                    ?: "New message"
            }
            val body = if (isLocked) {
                ""
            } else {
                message.notification?.body
                    ?: message.data["body"]
                    ?: ""
            }

            val notificationId = chatId.hashCode()

            // g. Tap action: PendingIntent to MainActivity with extra "open_chat_id" = chatId.
            val tapIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_chat_id", chatId)
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val smallIconRes = appContext.applicationInfo.icon.let {
                if (it != 0) it else android.R.drawable.sym_action_chat
            }

            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_CHATS)
                .setContentIntent(pendingIntent)

            if (body.isNotEmpty()) {
                builder
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }

            val notification = builder.build()

            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle incoming FCM chat message: ${e.message}", e)
        }
    }

    private fun ensureChatNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                setShowBadge(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "ChatMessagingService"
        const val CHANNEL_ID = "chat_messages"
        const val CHANNEL_NAME = "Chat messages"
        private const val GROUP_KEY_CHATS = "chats"
    }
}
