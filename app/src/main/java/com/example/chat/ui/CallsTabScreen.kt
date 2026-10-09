package com.example.chat.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.call.CallPermissionHelper
import com.example.chat.call.CallPermissionRationaleDialog
import com.example.chat.call.ZegoCallManager
import com.example.chat.call.ZegoConfigRequiredDialog
import com.example.chat.model.ChatSummary
import com.example.chat.model.ParticipantInfo
import com.example.ui.components.isAppInDarkMode

@Composable
fun CallsTabScreen(
    viewModel: ChatViewModel,
    onBackToApp: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val myUsername = currentUser?.username?.ifBlank { null }
        ?: com.example.chat.data.ChatPreferences(context).lastUsername
        ?: ""
    val chatSummaries by viewModel.chatsList.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()

    LaunchedEffect(chatSummaries) {
        val myUid = currentUser?.uid.orEmpty()
        chatSummaries.forEach { summary ->
            val otherUid = summary.participantIds.firstOrNull { it != myUid }
            val otherInfo = summary.getOtherParticipant(myUid)
            if (!otherUid.isNullOrBlank() && !otherInfo?.photoUrl.isNullOrBlank()) {
                ZegoCallManager.cacheAvatarUrl(otherUid, otherInfo?.photoUrl)
            }
        }
    }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showZegoConfigDialog by remember { mutableStateOf(false) }
    var pendingCallAction by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var hasPermissions by remember { mutableStateOf(CallPermissionHelper.hasCallPermissions(context)) }

    // (intentionally removed: no persistent button is created here anymore —
    // ZegoCallManager.startCall() now builds a fresh, properly-attached
    // button at the moment of each call attempt instead)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            pendingCallAction?.let { (uid, username, isVideo) ->
                pendingCallAction = null
                if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                    Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                    return@let
                }
                if (!ZegoCallManager.isConfigured()) {
                    val reason = ZegoCallManager.getConfigurationError() ?: "Zego call service is not configured"
                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                    showZegoConfigDialog = true
                } else {
                    val errorReason = ZegoCallManager.startCall(context, uid, username, isVideo)
                    if (errorReason == null) {
                        Toast.makeText(context, "Calling @$username…", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Camera & Microphone permissions are needed to place calls", Toast.LENGTH_LONG).show()
        }
    }

    fun handleCallPress(targetUid: String, targetUsername: String, isVideo: Boolean) {
        if (!hasPermissions) {
            pendingCallAction = Triple(targetUid, targetUsername, isVideo)
            showPermissionRationale = true
        } else {
            if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                return
            }
            if (!ZegoCallManager.isConfigured()) {
                val reason = ZegoCallManager.getConfigurationError() ?: "Zego call service is not configured"
                Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                showZegoConfigDialog = true
            } else {
                val errorReason = ZegoCallManager.startCall(context, targetUid, targetUsername, isVideo)
                if (errorReason == null) {
                    Toast.makeText(context, "Calling @$targetUsername…", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showPermissionRationale) {
        CallPermissionRationaleDialog(
            onDismiss = {
                showPermissionRationale = false
                pendingCallAction = null
            },
            onConfirm = {
                showPermissionRationale = false
                permissionLauncher.launch(CallPermissionHelper.getRequiredCallPermissions().toTypedArray())
            },
            accentColor = accentColor
        )
    }

    if (showZegoConfigDialog) {
        ZegoConfigRequiredDialog(
            onDismiss = { showZegoConfigDialog = false },
            accentColor = accentColor
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Calls Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0x221E293B) else Color(0x33FFFFFF),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToApp,
                    modifier = Modifier.testTag("calls_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Text(
                    text = "Calls",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.openSearch() },
                    modifier = Modifier.testTag("calls_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find Member to Call",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Call Status & Service Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (ZegoCallManager.isConfigured()) Icons.Default.CheckCircle else Icons.Default.Call,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ZegoCallManager.isConfigured()) "ZegoCloud Ready" else "Calls Ready for Setup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (ZegoCallManager.isConfigured())
                                    "Encrypted voice and video calling active with FCM offline push"
                                else
                                    "Tap to view ZegoCloud AppID and AppSign setup instructions",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Permission Card (if permissions missing)
            if (!hasPermissions) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enable Audio & Video Permissions",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Grant microphone, camera, and notification permissions to receive and place calls seamlessly.",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(CallPermissionHelper.getRequiredCallPermissions().toTypedArray())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Recent Real Call History Section
            if (callHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Calls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(callHistory, key = { it.id }) { log ->
                    val other = log.getOtherParticipant(currentUid)
                    val otherUid = if (log.callerId == currentUid) log.calleeId else log.callerId
                    val isCaller = (log.callerId == currentUid)
                    val isVideo = (log.type == "video")
                    val isMissed = (log.status == "missed" || log.status == "no_answer")
                    val isDeclined = (log.status == "declined")

                    val statusLabel = when {
                        log.status == "connected" -> {
                            val secs = log.durationSeconds ?: 0L
                            if (secs > 0) "${secs / 60}:${String.format("%02d", secs % 60)}" else "Connected"
                        }
                        isMissed -> if (isCaller) "No answer" else "Missed"
                        isDeclined -> "Declined"
                        else -> log.status
                    }

                    val dateLabel = log.startedAt?.toDate()?.let {
                        java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(it)
                    } ?: ""

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!other?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = other?.photoUrl,
                                    contentDescription = other?.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (other?.displayName?.ifBlank { other.username } ?: "U").take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = other?.displayName?.ifBlank { other.username } ?: "Member",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMissed && !isCaller) Color(0xFFEF4444) else if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = if (isMissed && !isCaller) Color(0xFFEF4444) else accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCaller) "Outgoing • $statusLabel • $dateLabel" else "Incoming • $statusLabel • $dateLabel",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val targetUsername = other?.username ?: other?.displayName ?: ""
                                    handleCallPress(otherUid, targetUsername, isVideo)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Call Back",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Call Section Header
            item {
                Text(
                    text = "Quick Call Members",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // List of member contacts from chat summaries
            val myUid = currentUser?.uid ?: ""
            val memberContacts = chatSummaries.mapNotNull { summary ->
                summary.getOtherParticipant(myUid)?.let { info ->
                    val otherUid = summary.participantIds.firstOrNull { it != myUid } ?: ""
                    Pair(otherUid, info)
                }
            }

            if (memberContacts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Recent Members",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Search for fellow growers to start a voice or video call.",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.openSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Find Members", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                items(memberContacts, key = { it.first }) { (otherUid, info) ->
                    CallContactItem(
                        participant = info,
                        isDark = isDark,
                        accentColor = accentColor,
                        onVoiceCall = { handleCallPress(otherUid, info.username, false) },
                        onVideoCall = { handleCallPress(otherUid, info.username, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CallContactItem(
    participant: ParticipantInfo,
    isDark: Boolean,
    accentColor: Color,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!participant.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = participant.photoUrl,
                    contentDescription = participant.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (participant.displayName.ifBlank { participant.username }).take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.displayName.ifBlank { participant.username },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${participant.username}",
                    fontSize = 12.sp,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Voice Call Action
            IconButton(
                onClick = onVoiceCall,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Video Call Action
            IconButton(
                onClick = onVideoCall,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
