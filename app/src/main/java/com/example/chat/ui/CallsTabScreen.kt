package com.example.chat.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.chat.model.CallLogItem
import com.example.chat.model.ChatTab
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInDarkMode
import com.example.ui.components.isGlassSupported
import com.example.ui.components.liquidGlassNav
import com.google.firebase.Timestamp
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CallsTabScreen(
    viewModel: ChatViewModel,
    onBackToApp: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val contentColor = glassContentColor()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val myUsername = currentUser?.username?.ifBlank { null }
        ?: com.example.chat.data.ChatPreferences(context).lastUsername?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: "user_${currentUid.take(6)}"
    val chatSummaries by viewModel.chatsList.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()

    LaunchedEffect(chatSummaries, currentUid) {
        val myUid = currentUid
        if (myUid.isBlank()) return@LaunchedEffect
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
    var showOverflowMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            pendingCallAction?.let { (uid, username, isVideo) ->
                pendingCallAction = null
                if (ZegoCallManager.getCredentialsError() != null) {
                    showZegoConfigDialog = true
                    return@let
                }
                if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                    Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                    return@let
                }
                val safeName = username.ifBlank { "Member" }
                val errorReason = ZegoCallManager.startCall(context, uid, safeName, isVideo)
                if (errorReason == null) {
                    Toast.makeText(context, "Calling @$safeName…", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Camera & Microphone permissions are needed to place calls", Toast.LENGTH_LONG).show()
        }
    }

    fun handleCallPress(targetUid: String, targetUsername: String, isVideo: Boolean) {
        if (targetUid.isBlank() || targetUid == currentUid) {
            Toast.makeText(context, "Cannot call yourself", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasPermissions) {
            pendingCallAction = Triple(targetUid, targetUsername, isVideo)
            showPermissionRationale = true
        } else {
            if (ZegoCallManager.getCredentialsError() != null) {
                showZegoConfigDialog = true
                return
            }
            if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                return
            }
            val safeName = targetUsername.ifBlank { "Member" }
            val errorReason = ZegoCallManager.startCall(context, targetUid, safeName, isVideo)
            if (errorReason == null) {
                Toast.makeText(context, "Calling @$safeName…", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
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
            accentColor = contentColor
        )
    }

    if (showZegoConfigDialog) {
        ZegoConfigRequiredDialog(
            onDismiss = { showZegoConfigDialog = false },
            accentColor = contentColor
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top bar (no back arrow, no Surface background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calls",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.openSearch() },
                    modifier = Modifier.testTag("calls_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("calls_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        val menuShape = RoundedCornerShape(20.dp)
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .clip(menuShape)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = if (isDark) 0.82f else 0.88f
                                    )
                                )
                                .border(0.5.dp, contentColor.copy(alpha = 0.15f), menuShape)
                                .callsGlass(backdrop, menuShape)
                                .padding(vertical = 8.dp)
                        ) {
                            CallsMenuItem(
                                text = "Clear call log",
                                contentColor = contentColor,
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showToast("Coming soon")
                                }
                            )
                            CallsMenuItem(
                                text = "Scheduled calls",
                                contentColor = contentColor,
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.showToast("Coming soon")
                                }
                            )
                            CallsMenuItem(
                                text = "Settings",
                                contentColor = contentColor,
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.selectTab(ChatTab.SETTINGS)
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("calls_list_column"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 2. Four quick action circles
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallQuickActionCircle(
                            icon = Icons.Default.Call,
                            label = "Call",
                            contentColor = contentColor,
                            backdrop = backdrop,
                            testTag = "calls_quick_call",
                            onClick = { viewModel.openSearch() }
                        )
                        CallQuickActionCircle(
                            icon = Icons.Default.Event,
                            label = "Schedule",
                            contentColor = contentColor,
                            backdrop = backdrop,
                            testTag = "calls_quick_schedule",
                            onClick = { viewModel.showToast("Coming soon") }
                        )
                        CallQuickActionCircle(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            contentColor = contentColor,
                            backdrop = backdrop,
                            testTag = "calls_quick_keypad",
                            onClick = { viewModel.showToast("Coming soon") }
                        )
                        CallQuickActionCircle(
                            icon = Icons.Default.FavoriteBorder,
                            label = "Favourites",
                            contentColor = contentColor,
                            backdrop = backdrop,
                            testTag = "calls_quick_favourites",
                            onClick = { viewModel.showToast("Coming soon") }
                        )
                    }
                }

                // 3. "Recent" section header
                item {
                    Text(
                        text = "Recent",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
                    )
                }

                if (callHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent calls",
                                fontSize = 15.sp,
                                color = contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(callHistory, key = { it.id }) { log ->
                        CallHistoryRow(
                            log = log,
                            currentUid = currentUid,
                            contentColor = contentColor,
                            onCallClick = { otherUid, targetUsername, isVideo ->
                                handleCallPress(otherUid, targetUsername, isVideo)
                            }
                        )
                    }
                }
            }
        }

        // 4. Floating call button at bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(56.dp)
                .callsGlass(backdrop, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { viewModel.openSearch() }
                .testTag("calls_fab_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddIcCall,
                contentDescription = "New call",
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun CallQuickActionCircle(
    icon: ImageVector,
    label: String,
    contentColor: Color,
    backdrop: Backdrop?,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .callsGlass(backdrop, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun CallHistoryRow(
    log: CallLogItem,
    currentUid: String,
    contentColor: Color,
    onCallClick: (otherUid: String, targetUsername: String, isVideo: Boolean) -> Unit
) {
    val other = log.getOtherParticipant(currentUid)
    val otherUid = if (log.callerId == currentUid) log.calleeId else log.callerId
    val isCaller = (log.callerId == currentUid)
    val isVideo = (log.type == "video")
    val isMissed = (log.status == "missed" || log.status == "no_answer")
    val isMissedIncoming = isMissed && !isCaller
    val missedRed = Color(0xFFEF4444)

    val displayName = other?.displayName?.ifBlank { other.username } ?: "Member"
    val targetUsername = other?.username ?: other?.displayName ?: ""
    val dateLabel = remember(log.startedAt) {
        formatCallLogTimestamp(log.startedAt)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallClick(otherUid, targetUsername, isVideo) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!other?.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = other?.photoUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMissedIncoming) missedRed else contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCaller) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = if (isCaller) "Outgoing" else "Incoming",
                    tint = if (isMissedIncoming) missedRed else contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
                if (dateLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateLabel,
                        fontSize = 13.sp,
                        color = contentColor.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = { onCallClick(otherUid, targetUsername, isVideo) },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = if (isVideo) "Video Call" else "Voice Call",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CallsMenuItem(
    text: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

private fun formatCallLogTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)

    return when {
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> {
            "Today, $timeStr"
        }
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday, $timeStr"
        }
        else -> {
            SimpleDateFormat("MMMM d, h:mm a", Locale.getDefault()).format(date)
        }
    }
}

@Composable
private fun Modifier.callsGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape = RoundedCornerShape(percent = 50)
): Modifier {
    return if (backdrop != null && isGlassSupported()) {
        this.liquidGlassNav(shape = shape, backdrop = backdrop)
    } else {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(0.5.dp, glassContentColor().copy(alpha = 0.15f), shape)
    }
}

