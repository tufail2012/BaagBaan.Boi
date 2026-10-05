package com.example.chat.call

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.BuildConfig
import com.zegocloud.uikit.internal.ZegoUIKitLanguage
import com.zegocloud.uikit.prebuilt.call.config.ZegoNotificationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser

object ZegoCallManager {
    private const val TAG = "ZegoCallManager"

    @Volatile
    var isInitialized: Boolean = false
        private set

    var currentUserId: String? = null
        private set

    var currentUserName: String? = null
        private set

    fun getAppId(): Long = try {
        BuildConfig.ZEGO_APP_ID.toString().trim().toLongOrNull() ?: 0L
    } catch (_: Throwable) { 0L }

    fun getAppSign(): String = try {
        BuildConfig.ZEGO_APP_SIGN.toString().trim()
    } catch (_: Throwable) { "" }

    fun getCredentialsError(): String? {
        val appId = getAppId()
        val appSign = getAppSign()
        return when {
            appId == 0L && appSign.isBlank() -> "Zego not configured: AppID is 0 and AppSign is missing"
            appId == 0L -> "Zego not configured: AppID is 0"
            appSign.isBlank() -> "Zego not configured: AppSign is missing"
            else -> null
        }
    }

    fun getConfigurationError(): String? {
        val credsError = getCredentialsError()
        if (credsError != null) return credsError
        if (!isInitialized) {
            return "Zego service not logged in: Call invitation service is not initialized for the current user yet"
        }
        return null
    }

    fun isConfigured(): Boolean {
        return getConfigurationError() == null
    }

    fun ensureInitialized(context: Context, uid: String, username: String): Boolean {
        if (isInitialized && currentUserId == uid) {
            return true
        }
        if (uid.isBlank() || username.isBlank()) {
            Log.w(TAG, "Cannot ensureInitialized: uid or username is blank (uid='$uid', username='$username')")
            return false
        }
        val application = (context.applicationContext as? Application)
            ?: (context as? Application)
            ?: com.example.AgriApplication.instance
        init(application, uid, username)
        return isInitialized
    }

    fun init(application: Application, userId: String, userName: String) {
        val credsError = getCredentialsError()
        if (credsError != null) {
            Log.w(TAG, "$credsError (ZEGO_APP_ID=${getAppId()}). Please set in gradle.properties or .env.")
            return
        }
        if (isInitialized && currentUserId == userId) {
            Log.d(TAG, "Already initialized for user $userId")
            return
        }
        if (isInitialized) {
            unInit()
        }
        try {
            val config = ZegoUIKitPrebuiltCallInvitationConfig().apply {
                translationText = ZegoTranslationText(ZegoUIKitLanguage.ENGLISH)
                notificationConfig = ZegoNotificationConfig().apply {
                    channelID = "zego_call_invitation"
                    channelName = "Call Invitation"
                    channelDesc = "Incoming call invitation notifications"
                }
                showDeclineButton = true
            }
            ZegoUIKitPrebuiltCallInvitationService.init(
                application,
                getAppId(),
                getAppSign(),
                userId,
                userName,
                config
            )
            isInitialized = true
            currentUserId = userId
            currentUserName = userName
            Log.i(TAG, "ZegoUIKitPrebuiltCallInvitationService initialized for user: $userName ($userId)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Zego call service: ${e.message}", e)
        }
    }

    fun unInit() {
        try {
            if (isInitialized) {
                ZegoUIKitPrebuiltCallInvitationService.unInit()
                isInitialized = false
                currentUserId = null
                currentUserName = null
                Log.i(TAG, "ZegoUIKitPrebuiltCallInvitationService uninitialized")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Zego unInit: ${e.message}", e)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun startCall(
        button: ZegoSendCallInvitationButton?,
        targetUserId: String,
        targetUserName: String,
        isVideo: Boolean
    ): Boolean {
        if (button == null) {
            Log.w(TAG, "Cannot start call: ZegoSendCallInvitationButton reference is null")
            return false
        }
        return try {
            button.setIsVideoCall(isVideo)
            button.setResourceID("zego_call")
            button.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
            button.performClick()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to performClick on ZegoSendCallInvitationButton: ${e.message}", e)
            false
        }
    }

    @Deprecated("Prefer passing the attached ZegoSendCallInvitationButton")
    fun startCall(
        context: Context,
        targetUserId: String,
        targetUserName: String,
        isVideo: Boolean,
        timeoutSeconds: Int = 60,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        val configError = getConfigurationError()
        if (configError != null) {
            Log.w(TAG, "Cannot start call: $configError")
            onResult(false, configError)
            return
        }

        val activity = findActivity(context) ?: (context as? Activity)
        if (activity == null) {
            val err = "Call failed: Activity context not available"
            Log.e(TAG, err)
            onResult(false, err)
            return
        }

        try {
            val button = ZegoSendCallInvitationButton(activity)
            button.setIsVideoCall(isVideo)
            button.setResourceID("zego_call")
            button.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
            button.performClick()
            onResult(true, "Calling @$targetUserName…")
        } catch (e: Throwable) {
            val err = "Call failed: ${e.message ?: "Unknown error"}"
            Log.e(TAG, err, e)
            onResult(false, err)
        }
    }
}
