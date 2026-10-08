package com.example.chat.ui.call

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.call.ActiveCallState
import com.example.chat.call.ZegoCallManager
import com.example.chat.data.ChatRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppCallOverlay(
    modifier: Modifier = Modifier
) {
    val callState by ZegoCallManager.activeCallState.collectAsState()
    val context = LocalContext.current
    var showMoreSheet by remember { mutableStateOf(false) }

    val state = callState ?: ActiveCallState()

    // Main WhatsApp dark background (#0B141A)
    // If it is a video call and camera is active, use a semi-transparent scrim so the video stream shows through
    val bgScrim = if (state.isVideo && state.isCameraOn) {
        Color(0x880B141A)
    } else {
        Color(0xFF0B141A)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScrim)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER ROW
            CallHeaderRow(
                state = state,
                onMinimize = {
                    val activity = (context as? Activity) ?: ZegoCallManager.findActivity(context)
                    if (activity != null) {
                        ZegoCallManager.minimizeCall(activity)
                    }
                },
                onAddPerson = {
                    Toast.makeText(context, "Group calling coming soon", Toast.LENGTH_SHORT).show()
                }
            )

            // CENTER AVATAR
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CallCenterAvatar(
                    name = state.targetName.ifBlank { "Member" },
                    photoUrl = state.targetPhotoUrl,
                    isConnected = state.isConnected
                )
            }

            // BOTTOM CONTROL PANEL (WhatsApp rounded card with two rows of 3 buttons)
            CallControlPanel(
                state = state,
                onToggleSpeaker = { ZegoCallManager.toggleSpeaker() },
                onToggleVideo = { ZegoCallManager.toggleCamera() },
                onToggleMute = { ZegoCallManager.toggleMicrophone() },
                onOpenMore = { showMoreSheet = true },
                onShare = {
                    Toast.makeText(context, "Screen sharing coming soon", Toast.LENGTH_SHORT).show()
                },
                onEndCall = { ZegoCallManager.endCall() }
            )
        }

        // MORE BOTTOM SHEET
        if (showMoreSheet) {
            CallMoreBottomSheet(
                state = state,
                onDismiss = { showMoreSheet = false },
                onMinimizeToChat = {
                    showMoreSheet = false
                    val activity = (context as? Activity) ?: ZegoCallManager.findActivity(context)
                    if (activity != null) {
                        ZegoCallManager.minimizeCall(activity)
                    }
                }
            )
        }
    }
}

@Composable
private fun CallHeaderRow(
    state: ActiveCallState,
    onMinimize: () -> Unit,
    onAddPerson: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: collapse/minimize button
        IconButton(
            onClick = onMinimize,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Center: Contact name + status / live duration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            Text(
                text = state.targetName.ifBlank { "Member" },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (state.isConnected) state.formattedDuration else state.statusText,
                color = Color(0xFF8696A0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        // Right: Add person button (placeholder)
        IconButton(
            onClick = onAddPerson,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Person",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CallCenterAvatar(
    name: String,
    photoUrl: String?,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // Outer pulsing halo ring when connected
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0x2200A884))
            )
        }

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0x33FFFFFF), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2C34))
                    .border(2.dp, Color(0x3300A884), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color(0xFF00A884),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CallControlPanel(
    state: ActiveCallState,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenMore: () -> Unit,
    onShare: () -> Unit,
    onEndCall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1F2C34).copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ROW 1: Speaker, Video, Mute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallRoundButton(
                    icon = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    label = "Speaker",
                    isActive = state.isSpeakerOn,
                    onClick = onToggleSpeaker
                )
                CallRoundButton(
                    icon = if (state.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = "Video",
                    isActive = state.isCameraOn,
                    onClick = onToggleVideo
                )
                CallRoundButton(
                    icon = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (state.isMicMuted) "Unmute" else "Mute",
                    isActive = state.isMicMuted,
                    activeColor = Color(0xFFE53935),
                    onClick = onToggleMute
                )
            }

            // ROW 2: More, Share, End call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallRoundButton(
                    icon = Icons.Default.MoreHoriz,
                    label = "More",
                    isActive = false,
                    onClick = onOpenMore
                )
                CallRoundButton(
                    icon = Icons.Default.ScreenShare,
                    label = "Share",
                    isActive = false,
                    onClick = onShare
                )
                CallRoundButton(
                    icon = Icons.Default.CallEnd,
                    label = "End",
                    isEndCall = true,
                    onClick = onEndCall
                )
            }
        }
    }
}

@Composable
private fun CallRoundButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    isEndCall: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp)
    ) {
        val buttonBg = when {
            isEndCall -> Color(0xFFE53935)
            isActive && activeColor == Color.White -> Color.White
            isActive -> activeColor
            else -> Color(0xFF2A3942)
        }
        val iconTint = when {
            isEndCall -> Color.White
            isActive && activeColor == Color.White -> Color(0xFF111B21)
            isActive -> Color.White
            else -> Color.White
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFE9EDEF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallMoreBottomSheet(
    state: ActiveCallState,
    onDismiss: () -> Unit,
    onMinimizeToChat: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var quickMessageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1F2C34),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "In-Call Options",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // NOISE CANCELLATION TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2A3942))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300A884)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Noise Cancellation",
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Noise Cancellation",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Filter ambient sounds with Zego ANS",
                            color = Color(0xFF8696A0),
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = state.isNoiseCancellationOn,
                    onCheckedChange = { isChecked ->
                        ZegoCallManager.setNoiseCancellation(isChecked)
                        Toast.makeText(
                            context,
                            if (isChecked) "Noise cancellation enabled" else "Noise cancellation disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00A884),
                        uncheckedThumbColor = Color(0xFF8696A0),
                        uncheckedTrackColor = Color(0xFF111B21)
                    )
                )
            }

            // SEND MESSAGE (Quick chat or return to chat)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2A3942))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300A884)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Send message",
                                tint = Color(0xFF00A884),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Send Message",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Chat without ending the call",
                                color = Color(0xFF8696A0),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Button to minimize to chat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF00A884))
                            .clickable(onClick = onMinimizeToChat)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Open Chat",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Inline quick message input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickMessageText,
                        onValueChange = { quickMessageText = it },
                        placeholder = {
                            Text("Type quick reply…", color = Color(0xFF8696A0), fontSize = 13.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A884),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1F2C34),
                            unfocusedContainerColor = Color(0xFF1F2C34)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = quickMessageText.trim()
                            val myUid = ZegoCallManager.currentUserId
                            if (text.isNotBlank() && state.targetUid.isNotBlank() && myUid != null) {
                                isSending = true
                                coroutineScope.launch {
                                    val repo = ChatRepository(context)
                                    val chatId = ChatRepository.getDirectChatId(myUid, state.targetUid)
                                    repo.sendMessage(
                                        chatId = chatId,
                                        senderId = myUid,
                                        text = text
                                    )
                                    quickMessageText = ""
                                    isSending = false
                                    Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = quickMessageText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (quickMessageText.isNotBlank()) Color(0xFF00A884) else Color(0x442A3942)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
