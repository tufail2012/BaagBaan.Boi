package com.example.chat.ui.bubbles

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.chat.model.ChatMessage
import com.example.chat.voice.VoicePlayer

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "File"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * 1. Image & Video Message Bubble
 */
@Composable
fun ImageVideoMessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean,
    uploadProgress: Float? = null,
    onRetryUpload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showFullScreen by remember { mutableStateOf(false) }
    val isVideo = message.messageType == "video"

    Column(
        modifier = Modifier
            .widthIn(min = 180.dp, max = 280.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF1B2831) else Color(0xFFE2E8F0))
                .clickable {
                    if (!message.mediaUrl.isNullOrBlank()) {
                        showFullScreen = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (!message.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = message.thumbnailUrl ?: message.mediaUrl,
                    contentDescription = if (isVideo) "Video preview" else "Image preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Upload Progress Overlay
            if (uploadProgress != null && uploadProgress in 0f..0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        color = accentColor,
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Upload Retry Overlay
            if (onRetryUpload != null && message.status == "failed") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.60f))
                        .clickable { onRetryUpload() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry upload",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Failed • Tap to retry",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Optional Caption text
        if (message.text.isNotBlank() && message.text != "📷 Photo" && message.text != "🎥 Video") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.text,
                color = if (isOwn) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }

    if (showFullScreen && !message.mediaUrl.isNullOrBlank()) {
        FullScreenMediaViewer(
            mediaUrl = message.mediaUrl,
            isVideo = isVideo,
            caption = message.text.takeIf { it != "📷 Photo" && it != "🎥 Video" },
            onDismiss = { showFullScreen = false }
        )
    }
}

/**
 * 2. Document Message Bubble
 */
@Composable
fun DocumentMessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean,
    uploadProgress: Float? = null,
    onRetryUpload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fileName = message.fileName ?: "Document"
    val sizeText = formatFileSize(message.fileSize)

    Surface(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .clickable {
                if (!message.mediaUrl.isNullOrBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        shape = RoundedCornerShape(10.dp),
        color = if (isOwn) Color.Black.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1A262E) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                if (uploadProgress != null && uploadProgress in 0f..0.99f) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (onRetryUpload != null && message.status == "failed") {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOwn) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sizeText,
                    fontSize = 11.sp,
                    color = if (isOwn) Color.White.copy(alpha = 0.70f) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open file",
                tint = if (isOwn) Color.White.copy(alpha = 0.70f) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 3. Voice Note Message Bubble (WhatsApp style with play/pause, duration, and waveform)
 */
@Composable
fun VoiceNoteMessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean,
    uploadProgress: Float? = null,
    onRetryUpload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackState by VoicePlayer.playbackState.collectAsState()
    val isThisPlaying = playbackState.activeMessageId == message.id && playbackState.isPlaying
    val progress = if (playbackState.activeMessageId == message.id) playbackState.progress else 0f

    val totalDurationSec = message.durationSeconds ?: 0L
    val durationText = if (playbackState.activeMessageId == message.id && playbackState.totalDurationMs > 0) {
        val currSec = playbackState.currentPositionMs / 1000
        val m = currSec / 60
        val s = currSec % 60
        String.format(java.util.Locale.getDefault(), "%d:%02d", m, s)
    } else {
        val m = totalDurationSec / 60
        val s = totalDurationSec % 60
        String.format(java.util.Locale.getDefault(), "%d:%02d", m, s)
    }

    val waveformAmplitudes = remember(message.waveform) {
        if (!message.waveform.isNullOrEmpty()) {
            message.waveform
        } else {
            // Default visually pleasant fallback waveform
            listOf(20, 45, 60, 30, 70, 85, 40, 65, 90, 50, 35, 75, 45, 60, 80, 40, 30, 20)
        }
    }

    Column(
        modifier = Modifier.widthIn(min = 200.dp, max = 260.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play/Pause / Upload button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable {
                        if (onRetryUpload != null && message.status == "failed") {
                            onRetryUpload()
                        } else if (!message.mediaUrl.isNullOrBlank()) {
                            VoicePlayer.play(context, message.id, message.mediaUrl)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uploadProgress != null && uploadProgress in 0f..0.99f) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (onRetryUpload != null && message.status == "failed") {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isThisPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform bars with playback scrub progress
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clickable {
                            // Tap to scrub
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveformAmplitudes.forEachIndexed { index, amp ->
                        val barFraction = (index.toFloat() / waveformAmplitudes.size.toFloat())
                        val hasPlayed = barFraction <= progress
                        val barHeight = (amp / 100f * 22f).coerceIn(4f, 24f).dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (hasPlayed) accentColor
                                    else if (isOwn) Color.White.copy(alpha = 0.40f)
                                    else if (isDark) Color(0xFF64748B)
                                    else Color(0xFF94A3B8)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOwn) Color.White.copy(alpha = 0.75f) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    )

                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4. Location Message Bubble
 */
@Composable
fun LocationMessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val lat = message.latitude ?: 0.0
    val lng = message.longitude ?: 0.0
    val address = message.locationAddress?.ifBlank { "Location coordinates: $lat, $lng" }
        ?: "Location: $lat, $lng"

    Surface(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .clickable {
                try {
                    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(address)})")
                    val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open maps: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isOwn) Color.Black.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1A262E) else Color(0xFFE2E8F0))
    ) {
        Column {
            // Simulated Map Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(if (isDark) Color(0xFF1E3A4C) else Color(0xFFC7D2FE)),
                contentAlignment = Alignment.Center
            ) {
                // Map grid graphics
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location pin",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to open in Google Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                }
            }

            // Location address footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOwn) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A))
                )
            }
        }
    }
}

/**
 * 5. Contact Card Message Bubble
 */
@Composable
fun ContactMessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val contactName = message.contactName ?: "Contact"
    val contactPhone = message.contactPhone ?: ""

    Surface(
        modifier = Modifier.widthIn(min = 200.dp, max = 260.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isOwn) Color.Black.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1A262E) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = contactName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOwn) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contactPhone.isNotBlank()) {
                        Text(
                            text = contactPhone,
                            fontSize = 12.sp,
                            color = if (isOwn) Color.White.copy(alpha = 0.70f) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Message or Add Contact
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.Contacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.NAME, contactName)
                                putExtra(ContactsContract.Intents.Insert.PHONE, contactPhone)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not add contact: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Save Contact",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Save Contact",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }
    }
}

/**
 * Fullscreen Media Viewer Dialog
 */
@Composable
fun FullScreenMediaViewer(
    mediaUrl: String,
    isVideo: Boolean,
    caption: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = mediaUrl,
                contentDescription = "Full-screen media",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top bar with close button and external open
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open external player: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in player",
                        tint = Color.White
                    )
                }
            }

            // Caption overlay at bottom
            if (!caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
