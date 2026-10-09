package com.example.chat.call

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import coil.load
import coil.transform.CircleCropTransformation
import com.example.BuildConfig
import com.example.MainActivity
import com.zegocloud.uikit.ZegoUIKit
import com.zegocloud.uikit.components.audiovideo.ZegoAvatarViewProvider
import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayout
import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayoutPictureInPictureConfig
import com.zegocloud.uikit.internal.ZegoUIKitLanguage
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.config.ZegoBottomMenuBarConfig
import com.zegocloud.uikit.prebuilt.call.config.ZegoCallDurationConfig
import com.zegocloud.uikit.prebuilt.call.config.ZegoNotificationConfig
import com.zegocloud.uikit.prebuilt.call.config.ZegoTopMenuBarConfig
import com.zegocloud.uikit.prebuilt.call.core.CallInvitationServiceImpl
import com.zegocloud.uikit.prebuilt.call.core.basic.provider.ZegoCallRoomForegroundProvider
import com.zegocloud.uikit.prebuilt.call.core.invite.PrebuiltCallRepository
import com.zegocloud.uikit.prebuilt.call.core.invite.ZegoCallInvitationData
import com.zegocloud.uikit.prebuilt.call.event.BackPressEvent
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import com.zegocloud.uikit.prebuilt.call.invite.internal.CallInviteActivity
import com.zegocloud.uikit.prebuilt.call.invite.internal.CallStateListener
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoUIKitPrebuiltCallConfigProvider
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoAudioOutputDevice
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.entity.ZIMCallUserStateChangeInfo
import im.zego.zim.enums.ZIMCallUserState
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.constants.ZegoANSMode
import java.lang.ref.WeakReference
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ZegoCallManager {
    private const val TAG = "ZegoCallManager"

    @Volatile
    var isInitialized: Boolean = false
        private set

    var currentUserId: String? = null
        private set

    var currentUserName: String? = null
        private set

    @Volatile
    private var initializedAtMs: Long = 0L

    private const val SETTLE_DELAY_MS = 1500L

    private val avatarUrlCache = mutableMapOf<String, String>()

    // ACTIVE CALL STATE FOR IN-CALL UI & MINIMIZED BAR
    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private var applicationContext: Context? = null

    private var topActivityRef: WeakReference<Activity>? = null

    fun getTopActivity(): Activity? = topActivityRef?.get()

    private val zimEventHandler = object : ZIMEventHandler() {
        override fun onCallUserStateChanged(
            zim: ZIM,
            info: ZIMCallUserStateChangeInfo,
            callID: String
        ) {
            val currentCall = _activeCallState.value ?: return
            if (currentCall.isConnected) return

            for (user in info.callUserList ?: emptyList()) {
                if (user.userID == currentCall.targetUid) {
                    Log.d(TAG, "ZIM onCallUserStateChanged: user=${user.userID}, state=${user.state}")
                    if (user.state == ZIMCallUserState.RECEIVED) {
                        _activeCallState.update { it?.copy(statusText = "Ringing…") }
                        applicationContext?.let { ctx ->
                            OutgoingCallNotificationService.update(
                                context = ctx,
                                targetName = currentCall.targetName,
                                statusText = "Ringing…",
                                photoUrl = currentCall.targetPhotoUrl,
                                isVideo = currentCall.isVideo
                            )
                        }
                    }
                }
            }
        }
    }

    fun cacheAvatarUrl(uid: String, photoUrl: String?) {
        if (uid.isNotBlank() && !photoUrl.isNullOrBlank()) {
            avatarUrlCache[uid] = photoUrl
        }
    }

    fun getAppId(): Long = try {
        BuildConfig.ZEGO_APP_ID.toString().trim().toLongOrNull() ?: 0L
    } catch (_: Throwable) { 0L }

    fun getAppSign(): String = try {
        BuildConfig.ZEGO_APP_SIGN.toString().replace("\\s+".toRegex(), "").trim()
    } catch (_: Throwable) { "" }

    fun getCredentialsError(): String? {
        val appId = getAppId()
        val appSign = getAppSign()
        if (appId <= 0L && appSign.isBlank()) {
            return "Zego not configured: AppID is 0 and AppSign is missing"
        }
        if (appId <= 0L) {
            return "Zego not configured: AppID must be a positive number, got $appId"
        }
        if (appSign.isBlank()) {
            return "Zego not configured: AppSign is missing"
        }
        if (appSign.length != 64) {
            return "AppSign is malformed: expected 64 hex characters, got ${appSign.length} characters"
        }
        if (!appSign.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            return "AppSign is malformed: contains non-hex characters"
        }
        return null
    }

    fun getConfigurationError(): String? {
        val credsError = getCredentialsError()
        if (credsError != null) return credsError
        if (!isInitialized) {
            return "Zego service not logged in: Call invitation service is not initialized for the current user yet"
        }
        val sinceInit = SystemClock.elapsedRealtime() - initializedAtMs
        if (sinceInit < SETTLE_DELAY_MS) {
            return "Zego call service is still starting up — please try again in a moment"
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
        Log.w(TAG, "Zego just initialized for $uid — asking caller to retry so the SDK can settle")
        return false
    }

    fun init(application: Application, userId: String, userName: String) {
        applicationContext = application.applicationContext
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
                Log.d(TAG, "ZEGO_INIT_CONFIG_MARKER_V2 — translationText set, build timestamp: 2026-10-05T05:59:09-07:00")
                notificationConfig = ZegoNotificationConfig().apply {
                    channelID = "zego_call_invitation"
                    channelName = "Call Invitation"
                    channelDesc = "Incoming call invitation notifications"
                }
                showDeclineButton = true

                // WhatsApp dark background on ringing screens
                incomingCallBackground = ColorDrawable(Color.parseColor("#0B141A"))
                outgoingCallBackground = ColorDrawable(Color.parseColor("#0B141A"))

                // Customize the in-call experience
                provider = object : ZegoUIKitPrebuiltCallConfigProvider {
                    override fun requireConfig(callInvitationData: ZegoCallInvitationData): ZegoUIKitPrebuiltCallConfig {
                        return try {
                            val callConfig = ZegoUIKitPrebuiltCallInvitationConfig.generateDefaultConfig(callInvitationData)

                            val otherUser = if (callInvitationData.inviter?.userID == currentUserId) {
                                callInvitationData.invitees?.firstOrNull()
                            } else {
                                callInvitationData.inviter
                            }
                            val isOutgoing = (callInvitationData.inviter?.userID == currentUserId)
                            val targetUid = otherUser?.userID ?: (_activeCallState.value?.targetUid ?: "")
                            val targetName = otherUser?.userName ?: (_activeCallState.value?.targetName ?: "Member")
                            val photoUrl = avatarUrlCache[targetUid] ?: _activeCallState.value?.targetPhotoUrl
                            val isVideo = (callInvitationData.type == 1)

                            _activeCallState.value = ActiveCallState(
                                callId = callInvitationData.callID ?: "",
                                targetUid = targetUid,
                                targetName = targetName,
                                targetPhotoUrl = photoUrl,
                                isVideo = isVideo,
                                isOutgoing = isOutgoing,
                                callerId = callInvitationData.inviter?.userID ?: currentUserId.orEmpty(),
                                calleeId = if (isOutgoing) targetUid else currentUserId.orEmpty(),
                                statusText = if (isOutgoing) "Calling…" else "Incoming call…",
                                isNoiseCancellationOn = true
                            )

                            // WhatsApp PIP layout style
                            if (callConfig.layout == null) {
                                callConfig.layout = ZegoLayout()
                            }
                            callConfig.layout.config = ZegoLayoutPictureInPictureConfig().apply {
                                largeViewBackgroundColor = Color.parseColor("#0B141A")
                                smallViewBackgroundColor = Color.parseColor("#111B21")
                            }

                            // Hide built-in SDK bars so our WhatsApp Compose overlay has complete control
                            if (callConfig.topMenuBarConfig == null) {
                                callConfig.topMenuBarConfig = ZegoTopMenuBarConfig()
                            }
                            callConfig.topMenuBarConfig.isVisible = false

                            if (callConfig.bottomMenuBarConfig == null) {
                                callConfig.bottomMenuBarConfig = ZegoBottomMenuBarConfig()
                            }
                            callConfig.bottomMenuBarConfig.buttons = emptyList()
                            callConfig.bottomMenuBarConfig.maxCount = 0

                            if (callConfig.durationConfig == null) {
                                callConfig.durationConfig = ZegoCallDurationConfig()
                            }
                            callConfig.durationConfig.isVisible = false

                            // Attach WhatsApp-style full screen overlay via roomForegroundProvider
                            callConfig.roomForegroundProvider = ZegoCallRoomForegroundProvider { ctx ->
                                createWhatsAppCallView(ctx)
                            }

                            // Avatar view provider for underlying audio/video container
                            callConfig.avatarViewProvider = object : ZegoAvatarViewProvider {
                                override fun onUserIDUpdated(parent: ViewGroup, uiKitUser: ZegoUIKitUser): View {
                                    val imageView = ImageView(parent.context)
                                    val pUrl = avatarUrlCache[uiKitUser.userID]
                                    if (!pUrl.isNullOrBlank()) {
                                        imageView.load(pUrl) {
                                            transformations(CircleCropTransformation())
                                        }
                                    }
                                    return imageView
                                }
                            }

                            callConfig.leaveCallListener = ZegoUIKitPrebuiltCallFragment.LeaveCallListener {
                                endCallInternal()
                            }

                            callConfig
                        } catch (e: Throwable) {
                            Log.e(TAG, "Exception inside requireConfig, falling back to default", e)
                            com.example.util.CrashReporter.recordExplicitCrash(e, "ZegoCallManager.requireConfig")
                            ZegoUIKitPrebuiltCallInvitationConfig.generateDefaultConfig(callInvitationData)
                        }
                    }
                }
            }

            ZegoUIKitPrebuiltCallInvitationService.init(
                application,
                getAppId(),
                getAppSign(),
                userId,
                userName,
                config
            )

            // Track top activity for system-level minimize & back-press routing
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    topActivityRef = WeakReference(activity)
                }
                override fun onActivityPaused(activity: Activity) {
                    if (topActivityRef?.get() == activity) {
                        topActivityRef = null
                    }
                }
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    topActivityRef = WeakReference(activity)
                }
                override fun onActivityStarted(activity: Activity) {
                    topActivityRef = WeakReference(activity)
                }
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (topActivityRef?.get() == activity) {
                        topActivityRef = null
                    }
                }
            })

            // Intercept system Back press on the Call screen to minimize the call rather than drop it
            try {
                ZegoUIKitPrebuiltCallService.events.callEvents.setBackPressEvent(object : BackPressEvent {
                    override fun onBackPressed(): Boolean {
                        val act = getTopActivity()
                        if (act != null) {
                            minimizeCall(act)
                            return true
                        }
                        return false
                    }
                })
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register callEvents.setBackPressEvent: ${e.message}")
            }

            // Setup listeners for call state, audio devices, and mute status
            setupStateListeners()

            // Register ZIM signaling event handler for real-time invitation delivery acknowledgment
            try {
                ZegoSignalingPlugin.getInstance().registerZIMEventHandler(zimEventHandler)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register ZIMEventHandler: ${e.message}")
            }

            isInitialized = true
            currentUserId = userId
            currentUserName = userName
            initializedAtMs = SystemClock.elapsedRealtime()
            Log.i(TAG, "ZegoUIKitPrebuiltCallInvitationService initialized for user: $userName ($userId)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Zego call service: ${e.message}", e)
        }
    }

    private fun setupStateListeners() {
        try {
            CallInvitationServiceImpl.getInstance().addCallStateListener(object : CallStateListener {
                override fun onStateChanged(before: Int, after: Int) {
                    Log.d(TAG, "CallStateListener onStateChanged: before=$before, after=$after")
                    when (after) {
                        PrebuiltCallRepository.CONNECTED -> {
                            onCallConnected()
                        }
                        PrebuiltCallRepository.NONE_HANG_UP -> {
                            endCallWithOutcome("connected") // was connected and hung up normally
                        }
                        PrebuiltCallRepository.NONE_REJECTED -> {
                            endCallWithOutcome("declined")
                        }
                        PrebuiltCallRepository.NONE_CALL_NO_REPLY -> {
                            endCallWithOutcome("no_answer")
                        }
                        PrebuiltCallRepository.NONE_CANCELED -> {
                            endCallWithOutcome("missed")
                        }
                        PrebuiltCallRepository.NONE_RECEIVE_MISSED -> {
                            endCallWithOutcome("missed")
                        }
                        PrebuiltCallRepository.NONE -> {
                            endCallInternal()
                        }
                    }
                }
            })

            ZegoUIKit.addMicrophoneStateListener { user, isOn ->
                if (user?.userID == currentUserId) {
                    _activeCallState.update { it?.copy(isMicMuted = !isOn) }
                }
            }

            ZegoUIKit.addCameraStateListener { user, isOn ->
                if (user?.userID == currentUserId) {
                    _activeCallState.update {
                        it?.copy(
                            isCameraOn = isOn,
                            isVideo = if (isOn) true else (it?.isVideo ?: false)
                        )
                    }
                }
            }

            ZegoUIKit.addAudioOutputDeviceChangedListener { device ->
                val isSpeaker = (device == ZegoAudioOutputDevice.SPEAKER)
                _activeCallState.update { it?.copy(isSpeakerOn = isSpeaker) }
            }

            ZegoUIKit.addUserUpdateListener(object : com.zegocloud.uikit.service.defines.ZegoUserUpdateListener {
                override fun onUserJoined(list: List<ZegoUIKitUser>?) {
                    if (list != null && list.any { it.userID != currentUserId } && _activeCallState.value?.isConnected != true) {
                        onCallConnected()
                    }
                }
                override fun onUserLeft(list: List<ZegoUIKitUser>?) {}
            })
        } catch (e: Throwable) {
            Log.w(TAG, "Could not register Zego listeners: ${e.message}")
        }
    }

    private fun onCallConnected() {
        _activeCallState.update { it?.copy(isConnected = true, statusText = "00:00") }
        // Keep ongoing notification alive throughout connected call to preserve foreground protection
        applicationContext?.let { ctx ->
            val call = _activeCallState.value
            OutgoingCallNotificationService.update(
                context = ctx,
                targetName = call?.targetName ?: "Member",
                statusText = "Ongoing call • 00:00",
                photoUrl = call?.targetPhotoUrl,
                isVideo = call?.isVideo ?: false
            )
        }
        durationJob?.cancel()
        durationJob = managerScope.launch {
            var elapsed = 0L
            while (isActive) {
                delay(1000L)
                elapsed++
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                _activeCallState.update { it?.copy(durationSeconds = elapsed, statusText = formatted) }
                applicationContext?.let { ctx ->
                    val call = _activeCallState.value
                    OutgoingCallNotificationService.update(
                        context = ctx,
                        targetName = call?.targetName ?: "Member",
                        statusText = "Ongoing call • $formatted",
                        photoUrl = call?.targetPhotoUrl,
                        isVideo = call?.isVideo ?: false
                    )
                }
            }
        }
    }

    private fun endCallWithOutcome(suggestedOutcome: String) {
        val call = _activeCallState.value
        val outcome = if (call?.isConnected == true) {
            "connected"
        } else {
            suggestedOutcome
        }
        val duration = call?.durationSeconds ?: 0L
        val callerId = call?.callerId?.ifBlank { currentUserId.orEmpty() } ?: currentUserId.orEmpty()
        val calleeId = call?.calleeId?.ifBlank { call.targetUid } ?: call?.targetUid.orEmpty()
        val callType = if (call?.isVideo == true) "video" else "voice"

        applicationContext?.let { ctx ->
            OutgoingCallNotificationService.stop(ctx)
            if (callerId.isNotBlank() && calleeId.isNotBlank()) {
                managerScope.launch(Dispatchers.IO) {
                    try {
                        val repo = com.example.chat.data.ChatRepository(ctx)
                        val callerPhoto = avatarUrlCache[callerId]
                        val calleePhoto = avatarUrlCache[calleeId] ?: call?.targetPhotoUrl

                        val callerInfo = com.example.chat.model.ParticipantInfo(
                            uid = callerId,
                            username = if (callerId == currentUserId) currentUserName.orEmpty() else "",
                            displayName = if (callerId == currentUserId) currentUserName.orEmpty() else "",
                            photoUrl = callerPhoto
                        )
                        val calleeInfo = com.example.chat.model.ParticipantInfo(
                            uid = calleeId,
                            username = if (calleeId == call?.targetUid) call.targetName else "",
                            displayName = if (calleeId == call?.targetUid) call.targetName else "",
                            photoUrl = calleePhoto
                        )

                        repo.recordCallEvent(
                            callerId = callerId,
                            calleeId = calleeId,
                            callType = callType,
                            callOutcome = outcome,
                            durationSeconds = duration,
                            callerInfo = callerInfo,
                            calleeInfo = calleeInfo
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to record call event: ${e.message}", e)
                    }
                }
            }
        }

        durationJob?.cancel()
        durationJob = null
        _activeCallState.value = null
    }

    private fun endCallInternal() {
        endCallWithOutcome("ended")
    }

    fun createWhatsAppCallView(context: Context): View {
        return ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                com.example.chat.ui.call.WhatsAppCallOverlay()
            }
        }
    }

    fun toggleMicrophone() {
        val uid = currentUserId ?: return
        val currentMuted = _activeCallState.value?.isMicMuted ?: false
        val newMicOn = currentMuted
        ZegoUIKit.turnMicrophoneOn(uid, newMicOn)
        _activeCallState.update { it?.copy(isMicMuted = !newMicOn) }
    }

    fun toggleCamera(context: Context? = null) {
        val uid = currentUserId ?: return
        val currentCamOn = _activeCallState.value?.isCameraOn ?: false
        val newCamOn = !currentCamOn
        val ctx = context ?: applicationContext
        if (newCamOn && ctx != null) {
            val hasCamPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasCamPermission) {
                val act = (ctx as? Activity) ?: getTopActivity()
                if (act != null) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        act,
                        arrayOf(android.Manifest.permission.CAMERA),
                        1011
                    )
                }
                return
            }
        }
        try {
            ZegoUIKit.turnCameraOn(uid, newCamOn)
        } catch (e: Throwable) {
            Log.e(TAG, "turnCameraOn failed: ${e.message}", e)
        }
        _activeCallState.update {
            it?.copy(
                isCameraOn = newCamOn,
                isVideo = if (newCamOn) true else (it.isVideo)
            )
        }
    }

    fun toggleSpeaker() {
        val currentSpeaker = _activeCallState.value?.isSpeakerOn ?: false
        val newSpeaker = !currentSpeaker
        ZegoUIKit.setAudioOutputToSpeaker(newSpeaker)
        _activeCallState.update { it?.copy(isSpeakerOn = newSpeaker) }
    }

    fun setNoiseCancellation(enable: Boolean) {
        try {
            val engine = ZegoExpressEngine.getEngine()
            engine?.enableANS(enable)
            engine?.enableTransientANS(enable)
            if (enable) {
                engine?.setANSMode(ZegoANSMode.AI)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not toggle ANS: ${e.message}")
        }
        _activeCallState.update { it?.copy(isNoiseCancellationOn = enable) }
    }

    fun minimizeCall(activity: Activity) {
        try {
            val intent = Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            _activeCallState.update { it?.copy(isMinimized = true) }
        } catch (e: Throwable) {
            Log.e(TAG, "Error minimizing call activity to MainActivity, falling back to moveTaskToBack", e)
            try {
                activity.moveTaskToBack(true)
                _activeCallState.update { it?.copy(isMinimized = true) }
            } catch (t: Throwable) {
                Log.e(TAG, "moveTaskToBack failed", t)
            }
        }
    }

    fun restoreCall(context: Context) {
        try {
            val intent = Intent(context, CallInviteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("page", "page_call")
            }
            context.startActivity(intent)
            _activeCallState.update { it?.copy(isMinimized = false) }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore call", e)
        }
    }

    fun endCall() {
        try {
            CallInvitationServiceImpl.getInstance().endCall()
        } catch (e: Throwable) {
            Log.w(TAG, "CallInvitationServiceImpl.endCall failed: ${e.message}")
        }
        try {
            ZegoUIKit.leaveRoom()
        } catch (_: Throwable) {}
        endCallInternal()
    }

    fun unInit() {
        try {
            endCallInternal()
            if (isInitialized) {
                try {
                    ZegoSignalingPlugin.getInstance().unregisterZIMEventHandler(zimEventHandler)
                } catch (_: Throwable) {}
                ZegoUIKitPrebuiltCallInvitationService.unInit()
                isInitialized = false
                currentUserId = null
                currentUserName = null
                initializedAtMs = 0L
                Log.i(TAG, "ZegoUIKitPrebuiltCallInvitationService uninitialized")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Zego unInit: ${e.message}", e)
        }
    }

    fun findActivity(context: Context): Activity? {
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
        isVideo: Boolean
    ): String? {
        val configError = getConfigurationError()
        if (configError != null) {
            Log.w(TAG, "Cannot start call: $configError")
            return configError
        }

        val activity = findActivity(context) ?: (context as? Activity)
        if (activity == null) {
            val err = "Activity context not available (context type: ${context.javaClass.name})"
            Log.e(TAG, "Call failed: $err")
            return err
        }

        // Initialize active call state for initial dial
        _activeCallState.value = ActiveCallState(
            targetUid = targetUserId,
            targetName = targetUserName,
            targetPhotoUrl = avatarUrlCache[targetUserId],
            isVideo = isVideo,
            isOutgoing = true,
            callerId = currentUserId.orEmpty(),
            calleeId = targetUserId,
            statusText = "Calling…",
            isNoiseCancellationOn = true
        )

        return try {
            val button = ZegoSendCallInvitationButton(activity)
            button.setIsVideoCall(isVideo)
            button.setResourceID("zego_call")
            button.setInvitees(listOf(ZegoUIKitUser(targetUserId, targetUserName)))
            val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            root.addView(button, android.view.ViewGroup.LayoutParams(0, 0))
            button.performClick()

            // Launch ongoing notification for the outgoing call
            val photoUrl = avatarUrlCache[targetUserId]
            OutgoingCallNotificationService.start(
                context = activity.applicationContext,
                targetName = targetUserName,
                statusText = "Calling…",
                photoUrl = photoUrl,
                isVideo = isVideo
            )

            null
        } catch (e: Throwable) {
            endCallInternal()
            val err = "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
            Log.e(TAG, "Failed to start call: $err", e)
            com.example.util.CrashReporter.recordExplicitCrash(e, "ZegoCallManager.startCall")
            err
        }
    }
}
