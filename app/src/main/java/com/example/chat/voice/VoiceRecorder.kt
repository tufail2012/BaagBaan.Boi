package com.example.chat.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.io.File

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Long = 0L,
    val amplitudes: List<Int> = emptyList(),
    val currentAmplitude: Int = 0,
    val isCancelled: Boolean = false
)

class VoiceRecorder(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var tickerJob: Job? = null
    private var startTimeMs: Long = 0L

    private val _recordingState = MutableStateFlow(VoiceRecordingState())
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    fun startRecording(): Boolean {
        try {
            stopCurrentSession(deleteFile = true)

            val cacheDir = context.cacheDir
            val voiceDir = File(cacheDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()

            _recordingState.value = VoiceRecordingState(
                isRecording = true,
                durationSeconds = 0L,
                amplitudes = emptyList(),
                currentAmplitude = 0
            )

            startSampling()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording: ${e.message}", e)
            stopCurrentSession(deleteFile = true)
            return false
        }
    }

    private fun startSampling() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _recordingState.value.isRecording) {
                delay(100L)
                val recorder = mediaRecorder ?: break
                val rawAmp = try {
                    recorder.maxAmplitude
                } catch (_: Exception) {
                    0
                }
                // Normalize 0..32767 down to 0..100
                val normalized = (rawAmp / 327).coerceIn(4, 100)
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000L

                _recordingState.update { current ->
                    val updatedAmps = (current.amplitudes + normalized).takeLast(40)
                    current.copy(
                        durationSeconds = elapsed,
                        amplitudes = updatedAmps,
                        currentAmplitude = normalized
                    )
                }
            }
        }
    }

    /**
     * Stops recording and returns the recorded audio file and sampled waveform.
     * Returns null if recording was shorter than 1 second.
     */
    fun stopRecording(): Pair<File, List<Int>>? {
        val file = currentOutputFile
        val finalAmps = _recordingState.value.amplitudes
        val duration = _recordingState.value.durationSeconds

        stopCurrentSession(deleteFile = false)

        if (file == null || !file.exists() || file.length() == 0L || duration < 1L) {
            file?.delete()
            return null
        }

        return Pair(file, finalAmps)
    }

    fun cancelRecording() {
        stopCurrentSession(deleteFile = true)
        _recordingState.update { it.copy(isRecording = false, isCancelled = true) }
    }

    private fun stopCurrentSession(deleteFile: Boolean) {
        tickerJob?.cancel()
        tickerJob = null

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        if (deleteFile) {
            currentOutputFile?.delete()
            currentOutputFile = null
        }

        _recordingState.update {
            it.copy(isRecording = false)
        }
    }

    companion object {
        private const val TAG = "VoiceRecorder"
    }
}
