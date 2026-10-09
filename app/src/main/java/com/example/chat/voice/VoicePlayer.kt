package com.example.chat.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0,
    val progress: Float = 0f
)

object VoicePlayer {
    private const val TAG = "VoicePlayer"
    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    fun play(context: Context, messageId: String, audioUrl: String) {
        val currentState = _playbackState.value

        // If this message is already playing, toggle pause
        if (currentState.activeMessageId == messageId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
                return
            } else {
                resume()
                return
            }
        }

        // Switching to a new voice note or starting fresh
        stop()

        _playbackState.value = VoicePlaybackState(
            activeMessageId = messageId,
            isPlaying = false,
            isBuffering = true
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    val duration = mp.duration
                    _playbackState.update {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            totalDurationMs = duration
                        )
                    }
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopProgressTracker()
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            progress = 0f,
                            currentPositionMs = 0
                        )
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize playback for $audioUrl: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            stopProgressTracker()
            _playbackState.update { it.copy(isPlaying = false) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pause MediaPlayer: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _playbackState.update { it.copy(isPlaying = true) }
            startProgressTracker()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resume MediaPlayer: ${e.message}")
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        try {
            val total = player.duration
            if (total > 0) {
                val targetMs = (total * progress.coerceIn(0f, 1f)).toInt()
                player.seekTo(targetMs)
                _playbackState.update {
                    it.copy(
                        currentPositionMs = targetMs,
                        progress = progress.coerceIn(0f, 1f)
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to seek: ${e.message}")
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }
        _playbackState.value = VoicePlaybackState()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                delay(100L)
                val player = mediaPlayer ?: break
                val current = player.currentPosition
                val total = player.duration
                if (total > 0) {
                    val progress = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = current,
                            totalDurationMs = total,
                            progress = progress
                        )
                    }
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
