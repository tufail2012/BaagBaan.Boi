package com.example.chat.call

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.BuildConfig
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

    fun isConfigured(): Boolean {
        return getAppId() != 0L && getAppSign().isNotBlank()
    }

    fun init(application: Application, userId: String, userName: String) {
        if (!isConfigured()) {
            Log.w(TAG, "Zego AppID/AppSign not configured (ZEGO_APP_ID=${getAppId()}). Please set in gradle.properties or .env.")
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

    fun startCall(
        context: Context,
        targetUserId: String,
        targetUserName: String,
        isVideo: Boolean
    ): Boolean {
        if (!isConfigured()) {
            return false
        }
        return try {
            val button = com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton(context)
            button.setIsVideoCall(isVideo)
            button.setResourceID("zego_call")
            button.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
            button.performClick()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start call: ${e.message}", e)
            false
        }
    }
}
