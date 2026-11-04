package com.example.chat.call

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.chat.data.ChatRepository
import com.example.chat.model.ParticipantInfo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
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

/**
 * Coordinates 1-to-1 voice and video calls using Firestore signaling (/calls/{callId})
 * and AgoraRtcManager for real-time audio/video media.
 */
object AgoraCallCoordinator {
    private const val TAG = "AgoraCallCoordinator"
    private const val RING_TIMEOUT_MS = 45_000L
    private const val STALE_CALL_THRESHOLD_MS = 60_000L

    @Volatile
    var isInitialized: Boolean = false
        private set

    var currentUserId: String? = null
        private set

    var currentUserName: String? = null
        private set

    private var appContext: Context? = null
    private val avatarUrlCache = mutableMapOf<String, String>()

    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private var remoteWatchJob: Job? = null

    private var incomingCallsListener: ListenerRegistration? = null
    private var activeCallDocListener: ListenerRegistration? = null

    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore unavailable: ${e.message}")
            null
        }

    fun cacheAvatarUrl(uid: String, photoUrl: String?) {
        if (uid.isNotBlank() && !photoUrl.isNullOrBlank()) {
            avatarUrlCache[uid] = photoUrl
        }
    }

    fun getAppId(): String = try {
        BuildConfig.AGORA_APP_ID.replace("\\s+".toRegex(), "").trim()
    } catch (_: Throwable) {
        ""
    }

    fun getCredentialsError(): String? {
        val appId = getAppId()
        if (appId.isBlank()) {
            return "Agora not configured: AGORA_APP_ID is missing"
        }
        return null
    }

    fun getConfigurationError(): String? {
        val credsError = getCredentialsError()
        if (credsError != null) return credsError
        if (!isInitialized) {
            return "Agora call service is not initialized for the current user yet"
        }
        return null
    }

    fun isConfigured(): Boolean = getConfigurationError() == null

    fun ensureInitialized(context: Context, uid: String, username: String): Boolean {
        if (getCredentialsError() != null) return false
        if (uid.isBlank()) {
            Log.w(TAG, "Cannot ensureInitialized: uid is blank")
            return false
        }
        if (isInitialized && currentUserId == uid) return true
        val safeName = username.ifBlank { "user_${uid.take(6)}" }
        init(context.applicationContext, uid, safeName)
        return isInitialized
    }

    fun init(application: Application, userId: String, userName: String) {
        init(application.applicationContext, userId, userName)
    }

    fun init(context: Context, userId: String, userName: String) {
        val app = context.applicationContext
        appContext = app
        if (userId.isBlank()) {
            Log.w(TAG, "init skipped: userId is blank")
            return
        }
        val credsError = getCredentialsError()
        if (credsError != null) {
            Log.w(TAG, "$credsError. Check the AGORA_APP_ID secret.")
            return
        }
        if (isInitialized && currentUserId == userId) {
            return
        }
        if (isInitialized) {
            unInit()
        }

        val engineOk = AgoraRtcManager.init(app, getAppId())
        if (!engineOk) {
            Log.e(TAG, "AgoraRtcManager.init failed")
            return
        }

        currentUserId = userId
        currentUserName = userName.ifBlank { "user_${userId.take(6)}" }
        isInitialized = true

        startObservingRemoteUids()
        listenForIncomingCalls(userId)
        Log.i(TAG, "AgoraCallCoordinator initialized for user=$userName ($userId)")
    }

    private fun startObservingRemoteUids() {
        remoteWatchJob?.cancel()
        remoteWatchJob = scope.launch {
            var hadRemotePeer = false
            AgoraRtcManager.remoteUids.collect { uids ->
                val current = _activeCallState.value
                if (current == null) {
                    hadRemotePeer = false
                    return@collect
                }
                if (uids.isNotEmpty()) {
                    hadRemotePeer = true
                    if (!current.isConnected) {
                        onCallConnected()
                    }
                } else if (hadRemotePeer && current.isConnected) {
                    hadRemotePeer = false
                    finishCallLocally(
                        callId = current.callId,
                        outcome = "connected",
                        updateFirestoreStatus = "ended"
                    )
                }
            }
        }
    }

    private fun listenForIncomingCalls(myUid: String) {
        incomingCallsListener?.remove()
        val firestore = db ?: return
        incomingCallsListener = firestore.collection("calls")
            .whereEqualTo("calleeId", myUid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Incoming calls listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                for (doc in docs) {
                    val callId = doc.id
                    if (_activeCallState.value != null) continue

                    val createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    if (System.currentTimeMillis() - createdAtMs > STALE_CALL_THRESHOLD_MS) {
                        continue
                    }

                    val callerId = doc.getString("callerId").orEmpty()
                    if (callerId.isBlank() || callerId == myUid) continue
                    val callerName = doc.getString("callerName")?.ifBlank { "Member" } ?: "Member"
                    val callerPhoto = doc.getString("callerPhotoUrl")?.takeIf { it.isNotBlank() }
                        ?: avatarUrlCache[callerId]
                    val isVideo = doc.getBoolean("isVideo") ?: false

                    cacheAvatarUrl(callerId, callerPhoto)

                    _activeCallState.value = ActiveCallState(
                        callId = callId,
                        targetUid = callerId,
                        targetName = callerName,
                        targetPhotoUrl = callerPhoto,
                        isVideo = isVideo,
                        isConnected = false,
                        isOutgoing = false,
                        callerId = callerId,
                        calleeId = myUid,
                        isMicMuted = false,
                        isCameraOn = isVideo,
                        isSpeakerOn = isVideo,
                        statusText = "Incoming call…"
                    )

                    watchActiveCallDoc(callId)
                    break
                }
            }
    }

    fun startCall(
        context: Context,
        targetUserId: String,
        targetUserName: String,
        isVideo: Boolean
    ): String? {
        val myUid = currentUserId
        if (myUid.isNullOrBlank()) {
            return "Call service is not logged in"
        }
        if (targetUserId.isBlank() || targetUserId == myUid) {
            return "Cannot place a call to yourself"
        }
        val configError = getConfigurationError()
        if (configError != null) {
            return configError
        }
        if (_activeCallState.value != null) {
            return "Another call is already in progress"
        }
        val firestore = db ?: return "Firestore is not available"

        val callId = UUID.randomUUID().toString().replace("-", "")
        val safeTargetName = targetUserName.ifBlank { "Member" }
        val targetPhoto = avatarUrlCache[targetUserId]
        val myName = currentUserName?.ifBlank { "Member" } ?: "Member"
        val myPhoto = avatarUrlCache[myUid].orEmpty()

        _activeCallState.value = ActiveCallState(
            callId = callId,
            targetUid = targetUserId,
            targetName = safeTargetName,
            targetPhotoUrl = targetPhoto,
            isVideo = isVideo,
            isConnected = false,
            isOutgoing = true,
            callerId = myUid,
            calleeId = targetUserId,
            isMicMuted = false,
            isCameraOn = isVideo,
            isSpeakerOn = isVideo,
            statusText = "Calling…"
        )

        val callData = hashMapOf(
            "callId" to callId,
            "channelName" to callId,
            "callerId" to myUid,
            "callerName" to myName,
            "callerPhotoUrl" to myPhoto,
            "calleeId" to targetUserId,
            "calleeName" to safeTargetName,
            "calleePhotoUrl" to (targetPhoto ?: ""),
            "isVideo" to isVideo,
            "status" to "ringing",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("calls").document(callId)
            .set(callData)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create call doc $callId: ${e.message}", e)
                if (_activeCallState.value?.callId == callId) {
                    finishCallLocally(callId, outcome = "failed", updateFirestoreStatus = null)
                }
            }

        AgoraRtcManager.setSpeakerphone(isVideo)
        val joinedOk = AgoraRtcManager.join(callId, 0, isVideo)
        if (!joinedOk) {
            finishCallLocally(callId, outcome = "failed", updateFirestoreStatus = "canceled")
            return "Failed to join Agora media channel"
        }

        watchActiveCallDoc(callId)
        startRingingTimeout(callId)

        OutgoingCallNotificationService.start(
            context = context.applicationContext,
            targetName = safeTargetName,
            statusText = "Calling…",
            photoUrl = targetPhoto,
            isVideo = isVideo
        )

        return null
    }

    fun acceptIncomingCall() {
        val current = _activeCallState.value ?: return
        if (current.isOutgoing || current.isConnected || current.callId.isBlank()) return
        val callId = current.callId

        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null

        db?.collection("calls")?.document(callId)?.update(
            mapOf(
                "status" to "accepted",
                "connectedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        AgoraRtcManager.setSpeakerphone(current.isVideo)
        AgoraRtcManager.join(callId, 0, current.isVideo)
        onCallConnected()
    }

    fun acceptCall() = acceptIncomingCall()

    fun declineIncomingCall() {
        val current = _activeCallState.value ?: return
        val callId = current.callId
        finishCallLocally(
            callId = callId,
            outcome = "declined",
            updateFirestoreStatus = "declined"
        )
    }

    fun declineCall() = declineIncomingCall()

    fun endCall() {
        val current = _activeCallState.value ?: return
        val callId = current.callId
        val wasConnected = current.isConnected
        val nextStatus = when {
            wasConnected -> "ended"
            current.isOutgoing -> "canceled"
            else -> "declined"
        }
        val outcome = when {
            wasConnected -> "connected"
            current.isOutgoing -> "missed"
            else -> "declined"
        }
        finishCallLocally(
            callId = callId,
            outcome = outcome,
            updateFirestoreStatus = nextStatus
        )
    }

    fun endActiveCallForShutdown() {
        if (_activeCallState.value != null) {
            endCall()
        }
    }

    private fun watchActiveCallDoc(callId: String) {
        activeCallDocListener?.remove()
        val firestore = db ?: return
        activeCallDocListener = firestore.collection("calls").document(callId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Active call listener error for $callId: ${error.message}")
                    return@addSnapshotListener
                }
                val current = _activeCallState.value ?: return@addSnapshotListener
                if (current.callId != callId) return@addSnapshotListener
                if (doc == null || !doc.exists()) {
                    finishCallLocally(callId, outcome = if (current.isConnected) "connected" else "ended", updateFirestoreStatus = null)
                    return@addSnapshotListener
                }

                when (doc.getString("status").orEmpty()) {
                    "ringing" -> {
                        if (current.isOutgoing && !current.isConnected && current.statusText != "Ringing…") {
                            _activeCallState.update { it?.copy(statusText = "Ringing…") }
                            appContext?.let { ctx ->
                                OutgoingCallNotificationService.update(
                                    context = ctx,
                                    targetName = current.targetName,
                                    statusText = "Ringing…",
                                    photoUrl = current.targetPhotoUrl,
                                    isVideo = current.isVideo
                                )
                            }
                        }
                    }
                    "accepted" -> {
                        if (!current.isConnected) {
                            onCallConnected()
                        }
                    }
                    "declined" -> {
                        finishCallLocally(callId, outcome = "declined", updateFirestoreStatus = null)
                    }
                    "canceled" -> {
                        finishCallLocally(callId, outcome = "missed", updateFirestoreStatus = null)
                    }
                    "missed" -> {
                        finishCallLocally(callId, outcome = "no_answer", updateFirestoreStatus = null)
                    }
                    "ended" -> {
                        val outcome = if (current.isConnected) "connected" else "ended"
                        finishCallLocally(callId, outcome = outcome, updateFirestoreStatus = null)
                    }
                }
            }
    }

    private fun startRingingTimeout(callId: String) {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = scope.launch {
            delay(RING_TIMEOUT_MS)
            val current = _activeCallState.value ?: return@launch
            if (current.callId == callId && !current.isConnected) {
                finishCallLocally(
                    callId = callId,
                    outcome = "no_answer",
                    updateFirestoreStatus = "missed"
                )
            }
        }
    }

    private fun onCallConnected() {
        val current = _activeCallState.value ?: return
        if (current.isConnected) return

        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null

        _callDurationSeconds.value = 0L
        _activeCallState.update {
            it?.copy(
                isConnected = true,
                statusText = "00:00",
                durationSeconds = 0L
            )
        }

        appContext?.let { ctx ->
            val updated = _activeCallState.value
            OutgoingCallNotificationService.update(
                context = ctx,
                targetName = updated?.targetName ?: "Member",
                statusText = "Ongoing call",
                photoUrl = updated?.targetPhotoUrl,
                isVideo = updated?.isVideo ?: false,
                connectedAtMillis = System.currentTimeMillis()
            )
        }

        durationJob?.cancel()
        durationJob = scope.launch {
            var elapsed = 0L
            while (isActive) {
                delay(1000L)
                elapsed++
                _callDurationSeconds.value = elapsed
                _activeCallState.update { it?.copy(durationSeconds = elapsed) }
            }
        }
    }

    private fun finishCallLocally(
        callId: String,
        outcome: String,
        updateFirestoreStatus: String?
    ) {
        val snapshot = _activeCallState.value
        val duration = _callDurationSeconds.value

        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        durationJob?.cancel()
        durationJob = null
        activeCallDocListener?.remove()
        activeCallDocListener = null

        _activeCallState.value = null
        _callDurationSeconds.value = 0L

        AgoraRtcManager.leave()
        appContext?.let { OutgoingCallNotificationService.stop(it) }

        if (!updateFirestoreStatus.isNullOrBlank() && callId.isNotBlank()) {
            db?.collection("calls")?.document(callId)?.update(
                mapOf(
                    "status" to updateFirestoreStatus,
                    "endedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Only the caller records the call event message in the chat history to avoid duplicates.
        val myUid = currentUserId.orEmpty()
        if (snapshot != null && snapshot.callerId == myUid && snapshot.calleeId.isNotBlank() && outcome != "failed") {
            val ctx = appContext
            if (ctx != null) {
                val effectiveOutcome = if (snapshot.isConnected) "connected" else outcome
                val callType = if (snapshot.isVideo) "video" else "voice"
                val callerName = currentUserName.orEmpty()
                val calleeName = snapshot.targetName
                val callerInfo = ParticipantInfo(
                    uid = snapshot.callerId,
                    username = callerName,
                    displayName = callerName,
                    photoUrl = avatarUrlCache[snapshot.callerId]
                )
                val calleeInfo = ParticipantInfo(
                    uid = snapshot.calleeId,
                    username = calleeName,
                    displayName = calleeName,
                    photoUrl = avatarUrlCache[snapshot.calleeId] ?: snapshot.targetPhotoUrl
                )
                scope.launch(Dispatchers.IO) {
                    try {
                        ChatRepository(ctx).recordCallEvent(
                            callerId = snapshot.callerId,
                            calleeId = snapshot.calleeId,
                            callType = callType,
                            callOutcome = effectiveOutcome,
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
    }

    fun toggleMicrophone() {
        val currentMuted = _activeCallState.value?.isMicMuted ?: false
        val newMuted = !currentMuted
        AgoraRtcManager.setMicMuted(newMuted)
        _activeCallState.update { it?.copy(isMicMuted = newMuted) }
    }

    fun toggleCamera(context: Context? = null) {
        val currentCamOn = _activeCallState.value?.isCameraOn ?: false
        val newCamOn = !currentCamOn
        val ctx = context ?: appContext
        if (newCamOn && ctx != null) {
            val hasCamPermission = ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasCamPermission) {
                val act = findActivity(ctx)
                if (act != null) {
                    ActivityCompat.requestPermissions(
                        act,
                        arrayOf(android.Manifest.permission.CAMERA),
                        1011
                    )
                }
                return
            }
        }
        AgoraRtcManager.setCameraOff(!newCamOn)
        _activeCallState.update {
            it?.copy(
                isCameraOn = newCamOn,
                isVideo = if (newCamOn) true else it.isVideo
            )
        }
    }

    fun switchCamera() {
        AgoraRtcManager.switchCamera()
    }

    fun toggleSpeaker() {
        val currentSpeaker = _activeCallState.value?.isSpeakerOn ?: false
        val newSpeaker = !currentSpeaker
        AgoraRtcManager.setSpeakerphone(newSpeaker)
        _activeCallState.update { it?.copy(isSpeakerOn = newSpeaker) }
    }

    fun setNoiseCancellation(enable: Boolean) {
        _activeCallState.update { it?.copy(isNoiseCancellationOn = enable) }
    }

    fun setSmoothing(enabled: Boolean, strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        _activeCallState.update {
            it?.copy(
                isSmoothOn = enabled,
                smoothStrength = clamped
            )
        }
    }

    fun minimizeCall(activity: Activity? = null) {
        _activeCallState.update { it?.copy(isMinimized = true) }
    }

    fun restoreCall(context: Context? = null) {
        _activeCallState.update { it?.copy(isMinimized = false) }
    }

    fun unInit() {
        incomingCallsListener?.remove()
        incomingCallsListener = null
        activeCallDocListener?.remove()
        activeCallDocListener = null
        remoteWatchJob?.cancel()
        remoteWatchJob = null
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        durationJob?.cancel()
        durationJob = null

        if (_activeCallState.value != null) {
            endCall()
        } else {
            AgoraRtcManager.leave()
        }

        isInitialized = false
        currentUserId = null
        currentUserName = null
        Log.i(TAG, "AgoraCallCoordinator uninitialized")
    }

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
