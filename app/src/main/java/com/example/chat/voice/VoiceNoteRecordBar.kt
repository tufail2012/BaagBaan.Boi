package com.example.chat.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.roundToInt

@Composable
fun VoiceNoteRecordBar(
    voiceRecorder: VoiceRecorder,
    onVoiceNoteRecorded: (File, List<Int>, Long) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by voiceRecorder.recordingState.collectAsState()
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val cancelThresholdPx = -160f // Drag 160px left to cancel

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Microphone enabled! Hold to record voice note.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancelRecording()
        }
    }

    val isRecording = recordingState.isRecording
    val isCancelTriggered = dragOffsetX < cancelThresholdPx

    // Pulsing animation for recording red dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRecording) {
            // Live recording bar replacing the text field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing red dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Timer (0:04)
                val totalSec = recordingState.durationSeconds
                val m = totalSec / 60
                val s = totalSec % 60
                Text(
                    text = String.format(java.util.Locale.getDefault(), "%d:%02d", m, s),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Live dynamic audio waveform bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val amps = recordingState.amplitudes.takeLast(16)
                    amps.forEach { amp ->
                        val h = (amp / 100f * 18f).coerceIn(4f, 20f).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h)
                                .clip(RoundedCornerShape(1.dp))
                                .background(accentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Slide to cancel text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset { IntOffset(dragOffsetX.roundToInt().coerceAtMost(0), 0) }
                ) {
                    Icon(
                        imageVector = if (isCancelTriggered) Icons.Default.Delete else Icons.Default.ChevronLeft,
                        contentDescription = "Cancel",
                        tint = if (isCancelTriggered) Color(0xFFEF4444) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isCancelTriggered) "Release to cancel" else "Slide to cancel",
                        fontSize = 12.sp,
                        color = if (isCancelTriggered) Color(0xFFEF4444) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        // The Mic Button (Hold to Record, Release to Send)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isRecording) (if (isCancelTriggered) Color(0xFFEF4444) else accentColor) else accentColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@detectDragGestures
                            }

                            dragOffsetX = 0f
                            val started = voiceRecorder.startRecording()
                            if (!started) {
                                Toast.makeText(context, "Could not start recording", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDragEnd = {
                            if (voiceRecorder.recordingState.value.isRecording) {
                                if (dragOffsetX < cancelThresholdPx) {
                                    // User dragged past cancel threshold
                                    voiceRecorder.cancelRecording()
                                    Toast.makeText(context, "Voice note cancelled", Toast.LENGTH_SHORT).show()
                                } else {
                                    val duration = voiceRecorder.recordingState.value.durationSeconds
                                    val result = voiceRecorder.stopRecording()
                                    if (result != null) {
                                        val (file, amps) = result
                                        onVoiceNoteRecorded(file, amps, duration)
                                    } else {
                                        Toast.makeText(context, "Hold to record longer voice note", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            voiceRecorder.cancelRecording()
                            dragOffsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Hold to record voice note",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
