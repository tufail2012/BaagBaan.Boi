package com.example.chat.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.chat.call.CallPermissionHelper
import com.example.chat.call.CallPermissionRationaleDialog
import com.example.chat.call.ZegoCallManager
import com.example.chat.call.ZegoConfigRequiredDialog
import com.example.chat.ui.call.MinimizedCallBar
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import coil.compose.AsyncImage
import com.example.chat.model.ChatMessage
import com.example.chat.model.ParticipantInfo
import com.example.chat.ui.attachments.AttachmentBottomSheet
import com.example.chat.ui.bubbles.ContactMessageBubble
import com.example.chat.ui.bubbles.DocumentMessageBubble
import com.example.chat.ui.bubbles.ImageVideoMessageBubble
import com.example.chat.ui.bubbles.LocationMessageBubble
import com.example.chat.ui.bubbles.VoiceNoteMessageBubble
import com.example.chat.voice.VoiceNoteRecordBar
import com.example.chat.voice.VoicePlayer
import com.example.chat.voice.VoiceRecorder
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val myUsername = currentUser?.username?.ifBlank { null }
        ?: com.example.chat.data.ChatPreferences(context).lastUsername
        ?: ""
    val recipient by viewModel.activeRecipient.collectAsState()
    val recipientPresence by viewModel.recipientPresence.collectAsState()
    val showOnlineStatus by viewModel.showOnlineStatus.collectAsState()
    val recipientStatusText: String? = if (showOnlineStatus && recipientPresence?.showOnlineStatus != false) {
        when {
            recipientPresence?.isOnline == true -> "online"
            recipientPresence?.lastSeenAt != null -> com.example.chat.model.formatLastSeen(recipientPresence?.lastSeenAt)
            else -> null
        }
    } else null
    val messages by viewModel.messages.collectAsState()
    val ghostModeEnabled by viewModel.ghostModeEnabled.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()

    val listState = rememberLazyListState()
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    val editingMessageId by viewModel.editingMessageId.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chatsList by viewModel.chatsList.collectAsState()
    val activeChatSummary = chatsList.firstOrNull { it.chatId == activeChatId }
    val otherUid = activeChatSummary?.participantIds?.firstOrNull { it != currentUid } ?: ""
    val activeCall by ZegoCallManager.activeCallState.collectAsState()

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showZegoConfigDialog by remember { mutableStateOf(false) }
    var pendingIsVideoCall by remember { mutableStateOf<Boolean?>(null) }
    var hasCallPermissions by remember { mutableStateOf(CallPermissionHelper.hasCallPermissions(context)) }

    // Attachment bottom sheet & upload progress
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uploadProgressMap by viewModel.uploadProgress.collectAsState()
    val failedUploadsMap by viewModel.failedUploads.collectAsState()

    // Voice note recording
    val voiceRecorder = remember { VoiceRecorder(context) }
    val recordingState by voiceRecorder.recordingState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            VoicePlayer.stop()
            voiceRecorder.cancelRecording()
        }
    }

    // Attachment Launchers
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val mime = context.contentResolver.getType(uri) ?: ""
            val type = if (mime.startsWith("video")) "video" else "image"
            viewModel.uploadAndSendMedia(type, uri, context)
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.uploadAndSendMedia("image", cameraImageUri!!, context)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraImageUri = uri
                takePhotoLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAndSendMedia("document", uri, context)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "Fetching current location...", Toast.LENGTH_SHORT).show()
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            val lat = loc.latitude
                            val lng = loc.longitude
                            var addressStr = "Lat: $lat, Lng: $lng"
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(lat, lng, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    addressStr = addr.getAddressLine(0) ?: addressStr
                                }
                            } catch (_: Exception) {}
                            viewModel.sendLocation(lat, lng, addressStr)
                        } else {
                            Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Could not get location: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission is required to share location", Toast.LENGTH_LONG).show()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                try {
                    context.contentResolver.query(
                        contactUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Contact" else "Contact"
                            val phone = if (phoneIndex != -1) cursor.getString(phoneIndex) ?: "" else ""
                            viewModel.sendContact(name, phone)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not read contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // (intentionally removed: no persistent button is created here anymore —
    // ZegoCallManager.startCall() now builds a fresh, properly-attached
    // button at the moment of each call attempt instead)

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCallPermissions = results.values.all { it }
        if (hasCallPermissions) {
            val isVideo = pendingIsVideoCall ?: false
            pendingIsVideoCall = null
            recipient?.let { rec ->
                if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                    Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                    return@let
                }
                if (!ZegoCallManager.isConfigured()) {
                    val reason = ZegoCallManager.getConfigurationError() ?: "Zego call service is not configured"
                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                    showZegoConfigDialog = true
                } else {
                    val errorReason = ZegoCallManager.startCall(context, otherUid, rec.username, isVideo)
                    if (errorReason == null) {
                        Toast.makeText(context, "Calling @${rec.username}…", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Camera & Microphone permissions are needed to place calls", Toast.LENGTH_LONG).show()
        }
    }

    fun initiateCall(isVideo: Boolean) {
        val rec = recipient ?: return
        if (!hasCallPermissions) {
            pendingIsVideoCall = isVideo
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
                val errorReason = try {
                    ZegoCallManager.startCall(context, otherUid, rec.username, isVideo)
                } catch (e: Throwable) {
                    Log.e("ChatScreen", "startCall threw unexpectedly", e)
                    "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
                }
                if (errorReason == null) {
                    Toast.makeText(context, "Calling @${rec.username}…", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(otherUid, recipient?.photoUrl) {
        ZegoCallManager.cacheAvatarUrl(otherUid, recipient?.photoUrl)
    }

    // Profile Dialog Stub
    if (showProfileDialog && recipient != null) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!recipient?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recipient?.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = recipient?.displayName?.ifBlank { recipient?.username } ?: "Member",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${recipient?.username}",
                            fontSize = 13.sp,
                            color = accentColor
                        )
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "Baagbaan Boi Verified Member.\nDirect encrypted messaging active.",
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ghost",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Messages vanish from both sides once seen",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ghostModeEnabled,
                            onCheckedChange = { viewModel.toggleGhostMode(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = accentColor)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }

    selectedMessageForAction?.let { msg ->
        val isOwnMsg = msg.senderId == currentUid
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("Message options") },
            text = {
                Column {
                    if (isOwnMsg && !msg.isDeleted) {
                        TextButton(onClick = {
                            viewModel.startEditingMessage(msg)
                            selectedMessageForAction = null
                        }) { Text("Edit") }
                        TextButton(onClick = {
                            viewModel.deleteMessageForEveryone(msg.id)
                            selectedMessageForAction = null
                        }) { Text("Delete for everyone") }
                    }
                    TextButton(onClick = {
                        viewModel.deleteMessageForMe(msg.id)
                        selectedMessageForAction = null
                    }) { Text("Delete for me") }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) { Text("Cancel") }
            }
        )
    }

    if (showPermissionRationale) {
        CallPermissionRationaleDialog(
            onDismiss = {
                showPermissionRationale = false
                pendingIsVideoCall = null
            },
            onConfirm = {
                showPermissionRationale = false
                callPermissionLauncher.launch(CallPermissionHelper.getRequiredCallPermissions().toTypedArray())
            },
            accentColor = accentColor
        )
    }

    if (showZegoConfigDialog) {
        val actualErrorString = ZegoCallManager.getConfigurationError()
            ?: ZegoCallManager.getCredentialsError()
            ?: "Call service is not ready or configured"
        ZegoConfigRequiredDialog(
            onDismiss = { showZegoConfigDialog = false },
            accentColor = accentColor,
            reason = actualErrorString
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        // Active minimized call bar
        activeCall?.let { call ->
            MinimizedCallBar(
                state = call,
                accentColor = accentColor,
                isDark = isDark
            )
        }

        // Conversation Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0x331E293B) else Color(0x44FFFFFF),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("conversation_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showProfileDialog = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    // Recipient Avatar
                    if (!recipient?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recipient?.photoUrl,
                            contentDescription = recipient?.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (recipient?.displayName?.ifBlank { recipient?.username } ?: "U").take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = recipient?.displayName?.ifBlank { recipient?.username } ?: "Member",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${recipient?.username}",
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (recipientStatusText != null) {
                            Text(
                                text = recipientStatusText,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { initiateCall(true) },
                    modifier = Modifier.testTag("conversation_video_call_button")
                ) {
                    VideoCallIcon(
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { initiateCall(false) },
                    modifier = Modifier.testTag("conversation_voice_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice call",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { showProfileDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF0B141A) else Color(0xFFF1F5F9))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("conversation_messages_column"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                    val isOwnMessage = msg.senderId == currentUid
                    val prev = messages.getOrNull(index - 1)
                    val next = messages.getOrNull(index + 1)

                    val isNewDay = prev == null || !isSameDay(prev.timestamp, msg.timestamp)
                    val isSameSenderAsNext = next != null &&
                        next.senderId == msg.senderId &&
                        isSameDay(msg.timestamp, next.timestamp) &&
                        minutesBetween(msg.timestamp, next.timestamp) < 3
                    val isSameSenderAsPrev = prev != null &&
                        prev.senderId == msg.senderId &&
                        !isNewDay &&
                        minutesBetween(prev.timestamp, msg.timestamp) < 3

                    if (isNewDay) {
                        DateSeparator(date = msg.timestamp, isDark = isDark)
                    }

                    Spacer(modifier = Modifier.height(if (isSameSenderAsPrev) 1.dp else 6.dp))

                    if (msg.messageType == "call") {
                        CallLogBubble(
                            message = msg,
                            currentUid = currentUid,
                            accentColor = accentColor,
                            isDark = isDark,
                            onCallBack = { isVideo ->
                                initiateCall(isVideo)
                            }
                        )
                    } else {
                        MessageBubble(
                            message = msg,
                            isOwn = isOwnMessage,
                            accentColor = accentColor,
                            isDark = isDark,
                            showTail = !isSameSenderAsNext,
                            uploadProgress = uploadProgressMap[msg.id],
                            onRetryUpload = failedUploadsMap[msg.id],
                            onLongPress = { selectedMessageForAction = msg }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }

        // Bottom Message Composer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0x331E293B) else Color(0x77FFFFFF),
            tonalElevation = 2.dp
        ) {
            if (editingMessageId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editing message",
                        fontSize = 12.sp,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.cancelEditingMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel edit", tint = accentColor)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (!recordingState.isRecording) {
                    // "+" Attachment Button
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = accentColor
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { viewModel.updateMessageInput(it) },
                        placeholder = {
                            Text("Message...", color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), fontSize = 14.sp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conversation_message_input"),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                            focusedContainerColor = if (isDark) Color(0x441E293B) else Color(0x99FFFFFF),
                            unfocusedContainerColor = if (isDark) Color(0x221E293B) else Color(0x66FFFFFF),
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInput.isNotBlank() && !isSending) {
                                    if (editingMessageId != null) viewModel.saveEditedMessage() else viewModel.sendMessage()
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (messageInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (!isSending) {
                                    if (editingMessageId != null) viewModel.saveEditedMessage() else viewModel.sendMessage()
                                }
                            },
                            enabled = !isSending,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .testTag("conversation_send_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // Voice note mic button (hold to record)
                        VoiceNoteRecordBar(
                            voiceRecorder = voiceRecorder,
                            onVoiceNoteRecorded = { file, waveform, duration ->
                                viewModel.uploadAndSendVoiceNote(file, waveform, duration)
                            },
                            accentColor = accentColor,
                            isDark = isDark,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                } else {
                    // Full-width live voice recording bar
                    VoiceNoteRecordBar(
                        voiceRecorder = voiceRecorder,
                        onVoiceNoteRecorded = { file, waveform, duration ->
                            viewModel.uploadAndSendVoiceNote(file, waveform, duration)
                        },
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Attachment Bottom Sheet
        if (showAttachmentSheet) {
            AttachmentBottomSheet(
                sheetState = attachmentSheetState,
                onDismiss = { showAttachmentSheet = false },
                onPickGallery = {
                    galleryPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onTakePhoto = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            cameraImageUri = uri
                            takePhotoLauncher.launch(uri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickDocument = {
                    documentPickerLauncher.launch(arrayOf("*/*"))
                },
                onShareLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onPickContact = {
                    val contactIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(contactIntent)
                },
                isDark = isDark
            )
        }
    }
}

@Composable
private fun DateSeparator(date: Timestamp?, isDark: Boolean) {
    val label = remember(date) {
        if (date == null) return@remember ""
        val cal = Calendar.getInstance().apply { time = date.toDate() }
        val now = Calendar.getInstance()
        when {
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> "Today"
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date.toDate())
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date.toDate())
        }
    }
    if (label.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            tonalElevation = 1.dp
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean,
    showTail: Boolean,
    uploadProgress: Float? = null,
    onRetryUpload: (() -> Unit)? = null,
    onLongPress: () -> Unit
) {
    val bubbleColor = if (isOwn) {
        accentColor
    } else {
        if (isDark) Color(0xFF1E293B) else Color.White
    }

    val textColor = if (isOwn) {
        Color.White
    } else {
        if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }

    val timeColor = if (isOwn) {
        Color.White.copy(alpha = 0.75f)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    val tailCorner = 4.dp
    val roundCorner = 18.dp
    val bubbleShape = if (isOwn) {
        RoundedCornerShape(
            topStart = roundCorner, topEnd = roundCorner,
            bottomStart = roundCorner, bottomEnd = if (showTail) tailCorner else roundCorner
        )
    } else {
        RoundedCornerShape(
            topStart = roundCorner, topEnd = roundCorner,
            bottomStart = if (showTail) tailCorner else roundCorner, bottomEnd = roundCorner
        )
    }

    val timeFormatted = remember(message.timestamp) {
        message.timestamp?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: ""
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box {
            if (showTail) {
                Canvas(
                    modifier = Modifier
                        .size(10.dp)
                        .align(if (isOwn) Alignment.BottomEnd else Alignment.BottomStart)
                        .offset(x = if (isOwn) 6.dp else (-6).dp)
                ) {
                    val path = Path().apply {
                        if (isOwn) {
                            moveTo(0f, 0f)
                            lineTo(size.width, size.height * 0.45f)
                            lineTo(0f, size.height)
                            close()
                        } else {
                            moveTo(size.width, 0f)
                            lineTo(0f, size.height * 0.45f)
                            lineTo(size.width, size.height)
                            close()
                        }
                    }
                    drawPath(path, color = bubbleColor)
                }
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                tonalElevation = if (isOwn) 2.dp else 1.dp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(bubbleShape)
                    .combinedClickable(onClick = {}, onLongClick = onLongPress)
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isDeleted) {
                        Text(
                            text = "This message was deleted",
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        when (message.messageType) {
                            "image", "video" -> {
                                ImageVideoMessageBubble(
                                    message = message,
                                    isOwn = isOwn,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    uploadProgress = uploadProgress,
                                    onRetryUpload = onRetryUpload
                                )
                            }
                            "document" -> {
                                DocumentMessageBubble(
                                    message = message,
                                    isOwn = isOwn,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    uploadProgress = uploadProgress,
                                    onRetryUpload = onRetryUpload
                                )
                            }
                            "voice" -> {
                                VoiceNoteMessageBubble(
                                    message = message,
                                    isOwn = isOwn,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    uploadProgress = uploadProgress,
                                    onRetryUpload = onRetryUpload
                                )
                            }
                            "location" -> {
                                LocationMessageBubble(
                                    message = message,
                                    isOwn = isOwn,
                                    accentColor = accentColor,
                                    isDark = isDark
                                )
                            }
                            "contact" -> {
                                ContactMessageBubble(
                                    message = message,
                                    isOwn = isOwn,
                                    accentColor = accentColor,
                                    isDark = isDark
                                )
                            }
                            else -> {
                                Text(
                                    text = message.text,
                                    color = textColor,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    if (timeFormatted.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (message.isEdited && !message.isDeleted) {
                                Text(text = "edited", color = timeColor, fontSize = 9.sp, fontStyle = FontStyle.Italic)
                            }
                            Text(text = timeFormatted, color = timeColor, fontSize = 10.sp)
                            if (isOwn) {
                                MessageStatusTicks(status = message.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return false
    val ca = Calendar.getInstance().apply { time = a.toDate() }
    val cb = Calendar.getInstance().apply { time = b.toDate() }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun minutesBetween(a: Timestamp?, b: Timestamp?): Long {
    if (a == null || b == null) return Long.MAX_VALUE
    return kotlin.math.abs(b.toDate().time - a.toDate().time) / 60000L
}

@Composable
private fun MessageStatusTicks(
    status: String,
    modifier: Modifier = Modifier
) {
    val tickColor = when (status) {
        "read" -> Color(0xFF34D399)
        "delivered" -> Color.White.copy(alpha = 0.80f)
        else -> Color.White.copy(alpha = 0.60f)
    }
    val isDouble = status == "delivered" || status == "read"
    val strokeWidthPx = with(LocalDensity.current) { 1.6.dp.toPx() }

    Canvas(
        modifier = modifier.size(width = if (isDouble) 18.dp else 11.dp, height = 11.dp)
    ) {
        fun drawCheck(offsetXPx: Float) {
            val w = 11.dp.toPx()
            val h = 11.dp.toPx()
            val path = Path().apply {
                moveTo(offsetXPx + w * 0.08f, h * 0.55f)
                lineTo(offsetXPx + w * 0.38f, h * 0.85f)
                lineTo(offsetXPx + w * 0.95f, h * 0.18f)
            }
            drawPath(
                path = path,
                color = tickColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        drawCheck(0f)
        if (isDouble) drawCheck(7.dp.toPx())
    }
}

@Composable
private fun VideoCallIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyWidth = w * 0.62f
        val cornerRadius = h * 0.18f

        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(bodyWidth, h * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )

        val lensPath = Path().apply {
            moveTo(bodyWidth + w * 0.02f, h * 0.30f)
            lineTo(w, h * 0.12f)
            lineTo(w, h * 0.88f)
            lineTo(bodyWidth + w * 0.02f, h * 0.70f)
            close()
        }
        drawPath(path = lensPath, color = tint)
    }
}

@Composable
private fun CallLogBubble(
    message: ChatMessage,
    currentUid: String,
    accentColor: Color,
    isDark: Boolean,
    onCallBack: (isVideo: Boolean) -> Unit
) {
    val isCaller = message.callCallerId == currentUid || (message.callCallerId.isBlank() && message.senderId == currentUid)
    val isVideo = message.callType == "video"
    val isMissed = message.callOutcome == "missed" || message.callOutcome == "no_answer"
    val isDeclined = message.callOutcome == "declined"
    val isConnected = message.callOutcome == "connected"

    // Primary label text
    val titleText = when {
        isConnected -> if (isVideo) "Video call" else "Voice call"
        isMissed -> if (isCaller) {
            "No answer"
        } else {
            if (isVideo) "Missed video call" else "Missed voice call"
        }
        isDeclined -> if (isCaller) "Declined" else "Call declined"
        else -> if (isVideo) "Video call" else "Voice call"
    }

    // Subtitle / duration
    val subtitleText = when {
        isConnected && message.callDurationSeconds > 0L -> {
            val mins = message.callDurationSeconds / 60
            val secs = message.callDurationSeconds % 60
            String.format("%d:%02d", mins, secs)
        }
        !isCaller && (isMissed || isDeclined) -> "Tap to call back"
        else -> null
    }

    val iconColor = when {
        isMissed || isDeclined -> Color(0xFFEF4444) // Red for missed/declined
        else -> accentColor
    }

    val bubbleBg = if (isDark) Color(0xFF1E293B) else Color.White
    val titleColor = if (isMissed && !isCaller) {
        Color(0xFFEF4444)
    } else {
        if (isDark) Color.White else Color(0xFF0F172A)
    }

    val timeFormatted = remember(message.timestamp) {
        message.timestamp?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleBg,
            tonalElevation = 2.dp,
            modifier = Modifier
                .widthIn(min = 220.dp, max = 310.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    if (!isCaller && (isMissed || isDeclined)) {
                        onCallBack(isVideo)
                    }
                }
                .testTag("call_log_bubble_${message.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        VideoCallIcon(tint = iconColor, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (subtitleText != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            color = if (subtitleText.startsWith("Tap")) accentColor else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = if (subtitleText.startsWith("Tap")) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }

                if (timeFormatted.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeFormatted,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Bottom)
                    )
                }
            }
        }
    }
}


