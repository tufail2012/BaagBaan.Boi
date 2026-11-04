package com.example.chat.call

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.IRtcEngineEventHandler.RtcStats
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Media-only wrapper around the Agora RTC engine (1-to-1 voice or video).
 * No signaling here. Who calls whom is handled elsewhere (Firestore, added in a later phase).
 */
object AgoraRtcManager {
    private const val TAG = "AgoraRtcManager"

    private var engine: RtcEngine? = null
    private var appContext: Context? = null
    private var localPreview: SurfaceView? = null

    private val _joined = MutableStateFlow(false)
    val joined: StateFlow<Boolean> = _joined.asStateFlow()

    private val _remoteUids = MutableStateFlow<Set<Int>>(emptySet())
    val remoteUids: StateFlow<Set<Int>> = _remoteUids.asStateFlow()

    private val _lastError = MutableStateFlow(0)
    val lastError: StateFlow<Int> = _lastError.asStateFlow()

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i(TAG, "joined channel=$channel localUid=$uid")
            _joined.value = true
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(TAG, "remote user joined uid=$uid")
            _remoteUids.update { it + uid }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.i(TAG, "remote user offline uid=$uid reason=$reason")
            _remoteUids.update { it - uid }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.i(TAG, "left channel")
            _joined.value = false
            _remoteUids.value = emptySet()
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error code=$err")
            _lastError.value = err
        }
    }

    /** Creates the engine once. Safe to call many times. Returns true if the engine exists. */
    fun init(context: Context, appId: String): Boolean {
        if (engine != null) return true
        if (appId.isBlank()) {
            Log.w(TAG, "AGORA_APP_ID is blank, cannot init")
            return false
        }
        return try {
            val app = context.applicationContext
            appContext = app
            val config = RtcEngineConfig().apply {
                mContext = app
                mAppId = appId
                mEventHandler = eventHandler
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            }
            engine = RtcEngine.create(config)
            engine?.enableAudio()
            engine?.enableVideo()
            engine?.setEnableSpeakerphone(false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "RtcEngine.create failed: ${e.message}", e)
            engine = null
            false
        }
    }

    /**
     * Joins a 1-to-1 channel. channelName must be 1 to 64 characters: letters, digits, '_' or '-'.
     * Token is empty because App Certificate is off during testing.
     */
    fun join(channelName: String, uid: Int, withVideo: Boolean): Boolean {
        val rtc = engine ?: return false
        return try {
            if (withVideo) {
                rtc.enableVideo()
                rtc.startPreview()
            } else {
                rtc.disableVideo()
            }
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishMicrophoneTrack = true
                publishCameraTrack = withVideo
                autoSubscribeAudio = true
                autoSubscribeVideo = true
            }
            val ret = rtc.joinChannel("", channelName, uid, options)
            Log.i(TAG, "joinChannel channel=$channelName ret=$ret")
            ret == 0
        } catch (e: Exception) {
            Log.e(TAG, "joinChannel failed: ${e.message}", e)
            false
        }
    }

    fun setupLocalView(view: SurfaceView) {
        val rtc = engine ?: return
        localPreview = view
        rtc.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    fun setupRemoteView(remoteUid: Int, view: SurfaceView) {
        val rtc = engine ?: return
        rtc.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
    }

    fun setMicMuted(muted: Boolean) {
        engine?.muteLocalAudioStream(muted)
    }

    fun setCameraOff(off: Boolean) {
        engine?.muteLocalVideoStream(off)
    }

    fun switchCamera() {
        engine?.switchCamera()
    }

    fun setSpeakerphone(on: Boolean) {
        engine?.setEnableSpeakerphone(on)
    }

    /** Leaves the channel but keeps the engine for the next call. */
    fun leave() {
        val rtc = engine ?: return
        try {
            rtc.stopPreview()
            rtc.leaveChannel()
        } catch (e: Exception) {
            Log.w(TAG, "leave failed: ${e.message}")
        }
        localPreview = null
        _joined.value = false
        _remoteUids.value = emptySet()
    }

    /** Full teardown. Call on logout or app shutdown. */
    fun release() {
        leave()
        try {
            RtcEngine.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "destroy failed: ${e.message}")
        }
        engine = null
        appContext = null
    }

    /** Creates an Agora renderer SurfaceView for the UI layer. */
    fun createRendererView(context: Context): SurfaceView =
        SurfaceView(context)
}
