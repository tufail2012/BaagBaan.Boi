package com.example.chat.call

data class ActiveCallState(
    val callId: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val targetPhotoUrl: String? = null,
    val isVideo: Boolean = false,
    val isConnected: Boolean = false,
    val durationSeconds: Long = 0L,
    val isMicMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isNoiseCancellationOn: Boolean = true,
    val isMinimized: Boolean = false,
    val statusText: String = "Connecting…"
) {
    val formattedDuration: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return if (mins >= 60) {
                val hours = mins / 60
                val remMins = mins % 60
                String.format("%d:%02d:%02d", hours, remMins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }
}
