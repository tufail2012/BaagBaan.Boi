package com.example.chat.call

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.BuildConfig
import com.zegocloud.uikit.plugin.adapter.plugins.signaling.ZegoSignalingPluginNotificationConfig
import com.zegocloud.uikit.plugin.common.PluginCallbackListener
import com.zegocloud.uikit.plugin.invitation.ZegoInvitationType
import com.zegocloud.uikit.prebuilt.call.core.CallInvitationServiceImpl
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
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

    fun getConfigurationError(): String? {
        val appId = getAppId()
        val appSign = getAppSign()
        return when {
            appId == 0L && appSign.isBlank() -> "Zego not configured: AppID is 0 and AppSign is missing"
            appId == 0L -> "Zego not configured: AppID is 0"
            appSign.isBlank() -> "Zego not configured: AppSign is missing"
            else -> null
        }
    }

    fun isConfigured(): Boolean {
        return getConfigurationError() == null
    }

    fun init(application: Application, userId: String, userName: String) {
        val configError = getConfigurationError()
        if (configError != null) {
            Log.w(TAG, "$configError (ZEGO_APP_ID=${getAppId()}). Please set in gradle.properties or .env.")
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
            val config = ZegoUIKitPrebuiltCallInvitationConfig()
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

        val activity = findActivity(context) ?: CallInvitationServiceImpl.getInstance().topActivity
        if (activity == null) {
            val err = "Call failed: Activity context not available"
            Log.e(TAG, err)
            onResult(false, err)
            return
        }

        try {
            val invitees = listOf(ZegoUIKitUser(targetUserId, targetUserName))
            val type = if (isVideo) ZegoInvitationType.VIDEO_CALL else ZegoInvitationType.VOICE_CALL
            val notificationConfig = ZegoSignalingPluginNotificationConfig().apply {
                resourceID = "zego_call"
            }

            val maskedSign = getAppSign().take(6)
            Log.d(TAG, "ZEGO credentials check: AppID=${getAppId()}, AppSignPrefix=$maskedSign..., isUserLoggedIn=$isInitialized (userId=$currentUserId)")

            CallInvitationServiceImpl.getInstance().sendInvitationWithUIChange(
                activity,
                invitees,
                type,
                "", // customData
                timeoutSeconds,
                null, // callID
                notificationConfig,
                object : PluginCallbackListener {
                    override fun callback(result: Map<String, Any?>?) {
                        val code = (result?.get("code") as? Number)?.toInt() ?: -1
                        val message = (result?.get("message") as? String).orEmpty()
                        val errorInvitees = result?.get("errorInvitees") as? List<*>

                        val isSuccess = code == 0 && errorInvitees.isNullOrEmpty()
                        val statusMessage = when {
                            isSuccess -> "Calling @$targetUserName…"
                            !errorInvitees.isNullOrEmpty() -> "Call failed: User @$targetUserName is offline or unavailable"
                            message.isNotBlank() -> "Call failed: $message (code $code)"
                            else -> "Call failed with error code $code"
                        }

                        Log.d(TAG, "Zego call invitation callback: code=$code, message=$message, isSuccess=$isSuccess")
                        activity.runOnUiThread {
                            onResult(isSuccess, statusMessage)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            val err = "Call failed: ${e.message ?: "Unknown error"}"
            Log.e(TAG, err, e)
            onResult(false, err)
        }
    }
}
