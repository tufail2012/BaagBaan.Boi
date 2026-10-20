package com.example.chat.ui

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.example.ui.components.isGlassSupported
import com.example.ui.components.navLiquidGlass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.TopHeaderScrollScrim
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.chat.data.ChatWallpaperStore
import com.example.chat.model.ChatMessage
import kotlinx.coroutines.launch
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var showGroupInfo by remember { mutableStateOf(false) }
    var moreButtonBounds by remember { mutableStateOf(Rect.Zero) }

    BackHandler {
        when {
            showGroupInfo -> {
                showGroupInfo = false
            }
            showMoreMenu -> {
                showMoreMenu = false
            }
            else -> {
                onBack()
            }
        }
    }

    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val myUsername = currentUser?.username?.ifBlank { null }
        ?: com.example.chat.data.ChatPreferences(context).lastUsername?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: "user_${currentUid.take(6)}"
    val recipient by viewModel.activeRecipient.collectAsState()
    val nicknames by viewModel.nicknames.collectAsState()
    val recipientPresence by viewModel.recipientPresence.collectAsState()
    val recipientTyping by viewModel.recipientTyping.collectAsState()
    val showOnlineStatus by viewModel.showOnlineStatus.collectAsState()

    var currentTimeTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            currentTimeTick = System.currentTimeMillis()
        }
    }

    val isBlockedByMeState by viewModel.isActiveChatBlockedByMe.collectAsState()
    val isBlockedByOtherState by viewModel.isActiveChatBlockedByOther.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val userProfiles by viewModel.userProfiles.collectAsState()
    val contactUserIds by viewModel.contactUserIds.collectAsState()
    val editingMessageId by viewModel.editingMessageId.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()
    val allChatsList by viewModel.allChatsList.collectAsState()
    val chatsList by viewModel.chatsList.collectAsState()
    val groupChats by viewModel.groupChats.collectAsState()
    val lockedChatsList by viewModel.lockedChatsList.collectAsState()
    val archivedChatsList by viewModel.archivedChatsList.collectAsState()
    val activeChatSummary = remember(
        activeChatId,
        allChatsList,
        chatsList,
        groupChats,
        lockedChatsList,
        archivedChatsList
    ) {
        allChatsList.firstOrNull { it.chatId == activeChatId }
            ?: groupChats.firstOrNull { it.chatId == activeChatId }
            ?: chatsList.firstOrNull { it.chatId == activeChatId }
            ?: lockedChatsList.firstOrNull { it.chatId == activeChatId }
            ?: archivedChatsList.firstOrNull { it.chatId == activeChatId }
    }
    val isGroup = activeChatSummary?.isGroup == true
    val otherUid = activeChatSummary?.participantIds?.firstOrNull { it != currentUid }
        ?.ifBlank { null }
        ?: recipient?.uid.orEmpty()
    val isBlockedByMe = !isGroup && (
        isBlockedByMeState ||
            (otherUid.isNotEmpty() && otherUid in blockedUserIds) ||
            (currentUid.isNotEmpty() && activeChatSummary?.blockedBy?.contains(currentUid) == true)
    )
    val isBlockedByOther = !isGroup && (
        isBlockedByOtherState ||
            (otherUid.isNotEmpty() && activeChatSummary?.blockedBy?.contains(otherUid) == true) ||
            (currentUid.isNotEmpty() && recipientPresence?.blockedUsers?.contains(currentUid) == true)
    )
    val isChatBlocked = !isGroup && (isBlockedByMe || isBlockedByOther)

    val canSeeRecipientLastSeen = remember(
        recipientPresence,
        showOnlineStatus,
        currentUser,
        contactUserIds,
        isChatBlocked
    ) {
        !isChatBlocked && viewModel.canSeeUserLastSeen(recipientPresence)
    }

    val isRecipientOnline = remember(recipientPresence, currentTimeTick, isChatBlocked, canSeeRecipientLastSeen) {
        if (isChatBlocked || !canSeeRecipientLastSeen) {
            false
        } else {
            val p = recipientPresence
            p != null && com.example.chat.model.isUserConsideredOnline(p.isOnline, p.lastSeenAt)
        }
    }

    val recipientStatusText: String? = remember(
        recipientPresence,
        canSeeRecipientLastSeen,
        isRecipientOnline,
        currentTimeTick,
        isChatBlocked
    ) {
        if (isChatBlocked || !canSeeRecipientLastSeen) {
            null
        } else if (isRecipientOnline) {
            "online"
        } else if (recipientPresence?.lastSeenAt != null) {
            com.example.chat.model.formatLastSeenCompact(recipientPresence?.lastSeenAt)
        } else if (recipientPresence != null) {
            "offline"
        } else {
            null
        }
    }
    val displayStatusText: String? = if (recipientTyping && canSeeRecipientLastSeen) "typing..." else recipientStatusText

    val presenceHidden = isChatBlocked || !canSeeRecipientLastSeen
    val ringColor: Color = when {
        recipientPresence == null || presenceHidden -> Color(0xFF94A3B8) // grey: loading or hidden
        isRecipientOnline -> Color(0xFF22C55E) // green: online
        else -> Color(0xFFEF4444) // red: offline
    }
    val recipientVisiblePhotoUrl = remember(
        otherUid,
        recipient?.photoUrl,
        recipientPresence,
        userProfiles,
        contactUserIds,
        isChatBlocked
    ) {
        if (isChatBlocked) null
        else viewModel.visiblePhotoUrl(otherUid, recipient?.photoUrl, recipientPresence ?: userProfiles[otherUid])
    }
    val recipientVisibleAbout = remember(
        otherUid,
        recipientPresence,
        userProfiles,
        contactUserIds,
        isChatBlocked
    ) {
        if (isChatBlocked) null
        else viewModel.visibleAbout(otherUid, recipientPresence ?: userProfiles[otherUid])
    }
    val messages by viewModel.messages.collectAsState()
    val readReceipts by viewModel.readReceipts.collectAsState()
    val ghostModeEnabled by viewModel.ghostModeEnabled.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()

    val listState = rememberLazyListState()
    val chatGlass = com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop()
    val glassOn = com.example.ui.components.isGlassSupported()
    val chatHaze = remember { dev.chrisbanes.haze.HazeState() }
    val headerScrolled by remember { derivedStateOf { listState.canScrollBackward } }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (headerScrolled) 1f else 0f,
        animationSpec = tween(150),
        label = "chatHeaderScrim"
    )
    val headerShape = RoundedCornerShape(percent = 50)
    val composerShape = RoundedCornerShape(28.dp)

    @Composable
    fun glassModifier(shape: CornerBasedShape): Modifier =
        if (glassOn) Modifier.navLiquidGlass(shape = shape, backdrop = chatGlass)
        else Modifier
            .clip(shape)
            .background(if (isDark) Color(0x991E293B) else Color(0xB3FFFFFF))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedCallForAction by remember { mutableStateOf<ChatMessage?>(null) }
    val activeCall by ZegoCallManager.activeCallState.collectAsState()

    var wallpaperRevision by remember { mutableIntStateOf(0) }
    var wallpaperFile by remember(activeChatId) {
        mutableStateOf<java.io.File?>(
            activeChatId?.let { ChatWallpaperStore.get(context, currentUid, it) }
        )
    }
    val wallpaperScope = rememberCoroutineScope()
    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = activeChatId
        if (uri != null && id != null) {
            wallpaperScope.launch {
                val saved = ChatWallpaperStore.save(context, currentUid, id, uri)
                if (saved != null) {
                    wallpaperFile = saved
                    wallpaperRevision++
                }
            }
        }
    }

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
                if (ZegoCallManager.getCredentialsError() != null) {
                    showZegoConfigDialog = true
                    return@let
                }
                if (!ZegoCallManager.ensureInitialized(context, currentUid, myUsername)) {
                    Toast.makeText(context, "Call service not ready yet — please try again in a moment", Toast.LENGTH_SHORT).show()
                    return@let
                }
                val targetName = rec.username.ifBlank { rec.displayName.ifBlank { "Member" } }
                val errorReason = ZegoCallManager.startCall(context, otherUid, targetName, isVideo)
                if (errorReason == null) {
                    Toast.makeText(context, "Calling @$targetName…", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Camera & Microphone permissions are needed to place calls", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isChatBlocked) {
        if (isChatBlocked) {
            voiceRecorder.cancelRecording()
            showAttachmentSheet = false
            viewModel.cancelEditingMessage()
        }
    }

    fun initiateCall(isVideo: Boolean) {
        if (isChatBlocked) {
            Toast.makeText(context, "You can't call this contact while blocked", Toast.LENGTH_SHORT).show()
            return
        }
        val rec = recipient ?: return
        if (!hasCallPermissions) {
            pendingIsVideoCall = isVideo
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
            val targetName = rec.username.ifBlank { rec.displayName.ifBlank { "Member" } }
            val errorReason = try {
                ZegoCallManager.startCall(context, otherUid, targetName, isVideo)
            } catch (e: Throwable) {
                Log.e("ChatScreen", "startCall threw unexpectedly", e)
                "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
            }
            if (errorReason == null) {
                Toast.makeText(context, "Calling @$targetName…", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Call failed: $errorReason", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(otherUid, recipientVisiblePhotoUrl) {
        ZegoCallManager.cacheAvatarUrl(otherUid, recipientVisiblePhotoUrl)
    }

    val effectiveDisplayName = remember(otherUid, recipient, nicknames) {
        viewModel.displayNameFor(otherUid, recipient)
    }
    val currentNickname = nicknames[otherUid]?.trim().orEmpty()
    val originalDisplayName = remember(recipient) {
        recipient?.displayName?.ifBlank { recipient?.username } ?: "Member"
    }

    // Profile Dialog Stub
    if (showProfileDialog && recipient != null) {
        var isEditingNickname by remember { mutableStateOf(false) }
        var nicknameDraft by remember(currentNickname, originalDisplayName) {
            mutableStateOf(if (currentNickname.isNotEmpty()) currentNickname else originalDisplayName)
        }
        val titleTextColor = if (isDark) Color.White else Color(0xFF0F172A)
        val secondaryNoteColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        AlertDialog(
            onDismissRequest = {
                isEditingNickname = false
                showProfileDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!recipientVisiblePhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recipientVisiblePhotoUrl,
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
                    Column(modifier = Modifier.weight(1f)) {
                        if (!isEditingNickname) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = effectiveDisplayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        nicknameDraft = if (currentNickname.isNotEmpty()) currentNickname else originalDisplayName
                                        isEditingNickname = true
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("edit_nickname_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit nickname",
                                        tint = secondaryNoteColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (currentNickname.isNotEmpty()) {
                                Text(
                                    text = "Original name: $originalDisplayName",
                                    fontSize = 11.sp,
                                    color = secondaryNoteColor
                                )
                            }
                            Text(
                                text = "@${recipient?.username}",
                                fontSize = 13.sp,
                                color = accentColor
                            )
                        } else {
                            OutlinedTextField(
                                value = nicknameDraft,
                                onValueChange = { if (it.length <= 30) nicknameDraft = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 15.sp,
                                    color = titleTextColor
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = secondaryNoteColor,
                                    cursorColor = accentColor,
                                    focusedTextColor = titleTextColor,
                                    unfocusedTextColor = titleTextColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nickname_input_field")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Only you can see this name.",
                                fontSize = 11.sp,
                                color = secondaryNoteColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.saveNickname(otherUid, nicknameDraft)
                                        isEditingNickname = false
                                    },
                                    modifier = Modifier.testTag("save_nickname_button")
                                ) {
                                    Text("Save", color = accentColor)
                                }
                                if (currentNickname.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            viewModel.saveNickname(otherUid, "")
                                            isEditingNickname = false
                                        },
                                        modifier = Modifier.testTag("reset_nickname_button")
                                    ) {
                                        Text("Reset", color = secondaryNoteColor)
                                    }
                                }
                            }
                            Text(
                                text = "@${recipient?.username}",
                                fontSize = 13.sp,
                                color = accentColor
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    if (!recipientVisibleAbout.isNullOrBlank()) {
                        Text(
                            text = "About",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = recipientVisibleAbout,
                            fontSize = 14.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.testTag("profile_dialog_about_text")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                TextButton(onClick = {
                    isEditingNickname = false
                    showProfileDialog = false
                }) {
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

    selectedCallForAction?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedCallForAction = null },
            title = { Text("Call options") },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.deleteMessageForMe(msg.id)
                        selectedCallForAction = null
                    }) { Text("Delete for me") }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCallForAction = null }) { Text("Cancel") }
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
            .imePadding()
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            var overlayHeightPx by remember { mutableIntStateOf(0) }
            val overlayHeight = with(LocalDensity.current) { overlayHeightPx.toDp() }
            var composerHeightPx by remember { mutableIntStateOf(0) }
            val composerHeight = with(LocalDensity.current) { composerHeightPx.toDp() }

            // Layer 1: messages. This Box is what the glass records.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (glassOn) Modifier.layerBackdrop(chatGlass) else Modifier)
                    .hazeSource(state = chatHaze)
                    .background(if (isDark) Color(0xFF0B141A) else Color(0xFFF1F5F9))
            ) {
                wallpaperFile?.let { file ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(file)
                            .memoryCacheKey("chat_wp_${file.absolutePath}_$wallpaperRevision")
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().testTag("conversation_wallpaper")
                    )
                    // readability scrim so bubbles and text stay legible on any photo
                    Box(
                        Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f))
                    )
                    // status bar readability on a wallpaper
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .height(statusTop + 24.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)))
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversation_messages_column"),
                    contentPadding = PaddingValues(
                        top = overlayHeight + 8.dp,
                        bottom = composerHeight + 12.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
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
                                },
                                onLongPress = { selectedCallForAction = msg }
                            )
                        } else {
                            val senderDisplayName = if (isGroup && !isOwnMessage && !isSameSenderAsPrev) {
                                viewModel.displayNameFor(
                                    msg.senderId,
                                    activeChatSummary?.participantInfo?.get(msg.senderId)
                                )
                            } else {
                                null
                            }
                            MessageBubble(
                                message = msg,
                                isOwn = isOwnMessage,
                                isGroup = isGroup,
                                senderName = senderDisplayName,
                                readReceipts = readReceipts,
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

            if (scrimAlpha > 0f) {
                TopHeaderScrollScrim(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer { alpha = scrimAlpha },
                    isScrolling = true,
                    hazeState = chatHaze,
                    accentColor = accentColor,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    customHeight = statusTop + overlayHeight,
                    pageColorOverride = when {
                        isAmoled -> Color.Black
                        isDark -> Color(0xFF0B141A)
                        else -> Color(0xFFF1F5F9)
                    }
                )
            }

            // Layer 2: floating overlay. It is a SIBLING of layer 1, never a
            // child, so it can never sample itself.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = statusTop)
                    .onGloballyPositioned { overlayHeightPx = it.size.height }
                    .zIndex(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                activeCall?.let { call ->
                    MinimizedCallBar(state = call, accentColor = accentColor, isDark = isDark)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 10.dp, top = 0.dp, bottom = 8.dp)
                        .height(56.dp)
                ) {
                    // 1) BACK: glass circle at the left edge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .then(glassModifier(CircleShape))
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("conversation_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    // 2) NAME: wide glass pill with avatar, name and status (username is in the profile dialog)
                    val groupDisplayTitle = remember(activeChatSummary, currentUid) {
                        activeChatSummary?.displayTitle(currentUid) ?: "Group"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .then(glassModifier(headerShape))
                            .clip(headerShape)
                            .clickable {
                                if (isGroup) {
                                    showGroupInfo = true
                                } else {
                                    showProfileDialog = true
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        if (isGroup) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = groupDisplayTitle.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        } else {
                            // Recipient avatar with status ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(width = 2.dp, color = ringColor, shape = CircleShape)
                                    .padding(2.dp)
                            ) {
                                if (!recipientVisiblePhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = recipientVisiblePhotoUrl,
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
                                            text = effectiveDisplayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGroup) groupDisplayTitle else effectiveDisplayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isGroup) {
                                val memberCount = activeChatSummary?.participantIds?.size ?: 0
                                Text(
                                    text = "$memberCount members",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (displayStatusText != null) {
                                val textMeasurer = rememberTextMeasurer()
                                val statusFontWeight = if (recipientTyping || isRecipientOnline) FontWeight.Medium else FontWeight.Normal
                                val statusStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                                    fontSize = 11.sp,
                                    fontWeight = statusFontWeight
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isRecipientOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                        val availableWidthPx = constraints.maxWidth
                                        val fittedStatusText = remember(
                                            displayStatusText,
                                            availableWidthPx,
                                            statusStyle
                                        ) {
                                            if (availableWidthPx <= 0) {
                                                displayStatusText
                                            } else {
                                                val fullWidthPx = textMeasurer.measure(
                                                    text = displayStatusText,
                                                    style = statusStyle,
                                                    maxLines = 1,
                                                    softWrap = false
                                                ).size.width
                                                if (fullWidthPx <= availableWidthPx) {
                                                    displayStatusText
                                                } else {
                                                    val shortText = displayStatusText.removePrefix("last seen ")
                                                    shortText
                                                }
                                            }
                                        }
                                        Text(
                                            text = fittedStatusText,
                                            fontSize = 11.sp,
                                            color = if (recipientTyping || isRecipientOnline) Color(0xFF22C55E) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            fontWeight = statusFontWeight,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3) ACTIONS: small glass pill with voice call (direct chats only) and more menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(56.dp)
                            .then(glassModifier(headerShape))
                            .clip(headerShape)
                            .padding(horizontal = 2.dp)
                    ) {
                        if (!isGroup) {
                            IconButton(
                                onClick = { initiateCall(false) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("conversation_voice_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Voice call",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showMoreMenu = !showMoreMenu },
                            modifier = Modifier
                                .size(44.dp)
                                .onGloballyPositioned { coords ->
                                    moreButtonBounds = coords.boundsInWindow()
                                }
                                .testTag("conversation_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Layer 3: floating bottom message composer overlay (or blocked banner)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .onSizeChanged { composerHeightPx = it.height },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isChatBlocked) {
                    val bannerShape = RoundedCornerShape(22.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(glassModifier(bannerShape))
                            .clip(bannerShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("chat_blocked_banner")
                    ) {
                        Text(
                            text = "You can't send messages to this contact. Unblock to continue.",
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        if (isBlockedByMe && otherUid.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.unblockUser(otherUid) },
                                modifier = Modifier.testTag("chat_banner_unblock_button")
                            ) {
                                Text(
                                    text = "Unblock",
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // (a) editing banner: its own small glass pill ABOVE the composer pill
                    if (editingMessageId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .then(glassModifier(RoundedCornerShape(percent = 50)))
                                .padding(start = 16.dp, end = 4.dp)
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

                    // (b) the composer: three separate glass pieces
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!recordingState.isRecording) {
                            // LEFT: a 56.dp glass CIRCLE with the "+" (Icons.Default.Add) icon
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .then(glassModifier(CircleShape)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Attach",
                                        tint = accentColor
                                    )
                                }
                            }

                            // MIDDLE: the message pill, containing ONLY the TextField
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .then(glassModifier(composerShape)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextField(
                                    value = messageInput,
                                    onValueChange = { viewModel.updateMessageInput(it) },
                                    placeholder = {
                                        Text(
                                            "Message...",
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("conversation_message_input"),
                                    shape = RectangleShape,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        cursorColor = accentColor,
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
                            }
                        }

                        // RIGHT: a 56.dp circle for send or mic
                        if (messageInput.isNotBlank() && !recordingState.isRecording) {
                            IconButton(
                                onClick = {
                                    if (!isSending) {
                                        if (editingMessageId != null) viewModel.saveEditedMessage() else viewModel.sendMessage()
                                    }
                                },
                                enabled = !isSending,
                                modifier = Modifier
                                    .size(56.dp)
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
                            VoiceNoteRecordBar(
                                voiceRecorder = voiceRecorder,
                                onVoiceNoteRecorded = { file, waveform, duration ->
                                    viewModel.uploadAndSendVoiceNote(file, waveform, duration)
                                },
                                accentColor = accentColor,
                                isDark = isDark,
                                containerModifier = glassModifier(CircleShape),
                                modifier = if (recordingState.isRecording) Modifier.fillMaxWidth() else Modifier.size(56.dp)
                            )
                        }
                    }
                }
            }

            // Layer 4: Sibling three-dot overflow glass panel (drawn inside the same screen, outside layerBackdrop)
            val menuTransitionState = remember { MutableTransitionState(false) }
            LaunchedEffect(showMoreMenu) {
                menuTransitionState.targetState = showMoreMenu
            }
            if (menuTransitionState.currentState || menuTransitionState.targetState) {
                val density = LocalDensity.current
                var containerWindowOffset by remember { mutableStateOf(Offset.Zero) }
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                var panelHeightPx by remember { mutableIntStateOf(0) }

                val menuWidthDp = 220.dp
                val menuWidthPx = with(density) { menuWidthDp.roundToPx() }
                val gapPx = with(density) { 8.dp.roundToPx() }
                val edgeMarginPx = with(density) { 10.dp.roundToPx() }

                val anchorRight = (moreButtonBounds.right - containerWindowOffset.x).roundToInt()
                val anchorBottom = (moreButtonBounds.bottom - containerWindowOffset.y).roundToInt()

                val minX = edgeMarginPx
                val maxX = if (containerSize.width > 0) {
                    (containerSize.width - menuWidthPx - edgeMarginPx).coerceAtLeast(minX)
                } else {
                    Int.MAX_VALUE
                }
                val xPx = if (moreButtonBounds.width > 0f && containerSize.width > 0) {
                    (anchorRight - menuWidthPx).coerceIn(minX, maxX)
                } else if (containerSize.width > 0) {
                    maxX
                } else {
                    minX
                }

                val minY = edgeMarginPx
                val maxY = if (panelHeightPx > 0 && containerSize.height > 0) {
                    (containerSize.height - panelHeightPx - edgeMarginPx).coerceAtLeast(minY)
                } else {
                    Int.MAX_VALUE
                }
                val fallbackTopPx = with(density) { (statusTop + 60.dp).roundToPx() }
                val yPx = if (moreButtonBounds.height > 0f) {
                    (anchorBottom + gapPx).coerceIn(minY, maxY)
                } else {
                    fallbackTopPx
                }

                val menuShape = RoundedCornerShape(20.dp)
                val menuContentColor = if (isDark) Color.White else Color(0xFF0F172A)
                val menuBorderColor = menuContentColor.copy(alpha = 0.15f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                        .onGloballyPositioned { coords ->
                            containerWindowOffset = coords.positionInWindow()
                            containerSize = coords.size
                        }
                ) {
                    // Full-screen transparent clickable scrim under the panel (no dim, no ripple)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showMoreMenu = false }
                            )
                            .testTag("conversation_more_menu_outside_dismiss")
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = menuTransitionState,
                        enter = fadeIn(animationSpec = tween(durationMillis = 150)) +
                            scaleIn(
                                initialScale = 0.92f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(durationMillis = 150)
                            ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                            scaleOut(
                                targetScale = 0.92f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(durationMillis = 150)
                            ),
                        modifier = Modifier.offset { IntOffset(xPx, yPx) }
                    ) {
                        Column(
                            modifier = Modifier
                                .width(menuWidthDp)
                                .onSizeChanged { panelHeightPx = it.height }
                                .then(glassModifier(menuShape))
                                .border(0.5.dp, menuBorderColor, menuShape)
                                .clip(menuShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                                .padding(vertical = 8.dp)
                                .testTag("conversation_overflow_glass_menu")
                        ) {
                            if (isGroup) {
                                ConversationOverflowMenuItem(
                                    text = "Group info",
                                    contentColor = menuContentColor,
                                    testTag = "conversation_menu_group_info",
                                    onClick = {
                                        showMoreMenu = false
                                        showGroupInfo = true
                                    }
                                )
                                ConversationOverflowMenuItem(
                                    text = "Wallpaper",
                                    contentColor = menuContentColor,
                                    testTag = "menu_wallpaper",
                                    onClick = {
                                        showMoreMenu = false
                                        wallpaperPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                if (wallpaperFile != null) {
                                    ConversationOverflowMenuItem(
                                        text = "Reset wallpaper",
                                        contentColor = menuContentColor,
                                        onClick = {
                                            showMoreMenu = false
                                            activeChatId?.let { ChatWallpaperStore.clear(context, currentUid, it) }
                                            wallpaperFile = null
                                            wallpaperRevision++
                                        }
                                    )
                                }
                            } else {
                                ConversationOverflowMenuItem(
                                    text = "Video call",
                                    contentColor = menuContentColor,
                                    leadingIcon = {
                                        VideoCallIcon(
                                            tint = menuContentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    testTag = "conversation_video_call_button",
                                    onClick = {
                                        showMoreMenu = false
                                        initiateCall(true)
                                    }
                                )
                                ConversationOverflowMenuItem(
                                    text = "View profile",
                                    contentColor = menuContentColor,
                                    onClick = {
                                        showMoreMenu = false
                                        showProfileDialog = true
                                    }
                                )
                                ConversationOverflowMenuItem(
                                    text = "Wallpaper",
                                    contentColor = menuContentColor,
                                    testTag = "menu_wallpaper",
                                    onClick = {
                                        showMoreMenu = false
                                        wallpaperPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                if (wallpaperFile != null) {
                                    ConversationOverflowMenuItem(
                                        text = "Reset wallpaper",
                                        contentColor = menuContentColor,
                                        onClick = {
                                            showMoreMenu = false
                                            activeChatId?.let { ChatWallpaperStore.clear(context, currentUid, it) }
                                            wallpaperFile = null
                                            wallpaperRevision++
                                        }
                                    )
                                }
                                if (otherUid.isNotEmpty()) {
                                    ConversationOverflowMenuItem(
                                        text = if (isBlockedByMe) "Unblock" else "Block",
                                        contentColor = menuContentColor,
                                        testTag = "conversation_menu_block_toggle",
                                        onClick = {
                                            showMoreMenu = false
                                            if (isBlockedByMe) {
                                                viewModel.unblockUser(otherUid)
                                            } else {
                                                viewModel.blockUser(otherUid, recipient)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Full-screen Group Info Box inside ChatScreen (no AlertDialog or popup window)
            if (showGroupInfo && isGroup) {
                var showEditGroupNameDialog by remember { mutableStateOf(false) }
                var showDeleteGroupConfirmDialog by remember { mutableStateOf(false) }
                BackHandler {
                    when {
                        showEditGroupNameDialog -> showEditGroupNameDialog = false
                        showDeleteGroupConfirmDialog -> showDeleteGroupConfirmDialog = false
                        else -> showGroupInfo = false
                    }
                }
                val groupTitle = activeChatSummary?.displayTitle(currentUid) ?: "Group"
                val isCreator = currentUid.isNotEmpty() && activeChatSummary?.createdBy == currentUid
                val memberIds = activeChatSummary?.participantIds.orEmpty()
                val participantInfoMap = activeChatSummary?.participantInfo.orEmpty()
                val bgColor = when {
                    isAmoled -> Color.Black
                    isDark -> Color(0xFF0B141A)
                    else -> Color(0xFFF1F5F9)
                }
                val primaryTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                val cardBgColor = if (isDark) Color(0xFF1E293B) else Color.White

                if (showEditGroupNameDialog) {
                    var nameDraft by remember(activeChatSummary?.name, groupTitle) {
                        mutableStateOf(activeChatSummary?.name?.takeIf { it.isNotBlank() } ?: groupTitle)
                    }
                    AlertDialog(
                        onDismissRequest = { showEditGroupNameDialog = false },
                        title = {
                            Text(
                                text = "Edit group name",
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { if (it.length <= 40) nameDraft = it },
                                singleLine = true,
                                label = { Text("Group name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = secondaryTextColor,
                                    cursorColor = accentColor,
                                    focusedTextColor = primaryTextColor,
                                    unfocusedTextColor = primaryTextColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_group_name_input")
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val id = activeChatId
                                    val trimmed = nameDraft.trim().take(40)
                                    if (id != null && trimmed.isNotEmpty()) {
                                        viewModel.renameGroup(id, trimmed)
                                        showEditGroupNameDialog = false
                                    }
                                },
                                enabled = nameDraft.trim().isNotEmpty(),
                                modifier = Modifier.testTag("edit_group_name_save_button")
                            ) {
                                Text(
                                    text = "Save",
                                    color = if (nameDraft.trim().isNotEmpty()) accentColor else secondaryTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showEditGroupNameDialog = false },
                                modifier = Modifier.testTag("edit_group_name_cancel_button")
                            ) {
                                Text("Cancel", color = secondaryTextColor)
                            }
                        }
                    )
                }

                if (showDeleteGroupConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteGroupConfirmDialog = false },
                        title = {
                            Text(
                                text = "Delete group",
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        },
                        text = {
                            Text(
                                text = "Delete this group for everyone? All messages will be removed.",
                                color = primaryTextColor
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val id = activeChatId
                                    showDeleteGroupConfirmDialog = false
                                    showGroupInfo = false
                                    if (id != null) {
                                        viewModel.deleteGroup(id)
                                    }
                                    onBack()
                                },
                                modifier = Modifier.testTag("delete_group_confirm_button")
                            ) {
                                Text(
                                    text = "Delete",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteGroupConfirmDialog = false },
                                modifier = Modifier.testTag("delete_group_cancel_button")
                            ) {
                                Text("Cancel", color = secondaryTextColor)
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(30f)
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .testTag("group_info_screen")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Top bar: back button and "Group info" title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            IconButton(
                                onClick = { showGroupInfo = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("group_info_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = primaryTextColor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Group info",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        // Body: big group avatar circle, group name, "${participantIds.size} members", then list of members
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item(key = "group_info_header") {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = groupTitle.take(1).uppercase(),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = groupTitle,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${memberIds.size} members",
                                        fontSize = 14.sp,
                                        color = secondaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }

                            items(memberIds.size, key = { idx -> memberIds[idx] }) { idx ->
                                val memberUid = memberIds[idx]
                                val pInfo = participantInfoMap[memberUid]
                                val isMe = memberUid == currentUid
                                val baseDisplayName = if (isMe) {
                                    currentUser?.displayName?.ifBlank { currentUser?.username }
                                        ?: pInfo?.displayName?.ifBlank { pInfo.username }
                                        ?: "You"
                                } else {
                                    viewModel.displayNameFor(memberUid, pInfo)
                                }
                                val memberTitle = if (isMe) "$baseDisplayName (You)" else baseDisplayName
                                val usernameStr = if (isMe) {
                                    currentUser?.username?.ifBlank { pInfo?.username.orEmpty() }.orEmpty()
                                } else {
                                    pInfo?.username.orEmpty()
                                }
                                val photoUrl = if (isMe) {
                                    currentUser?.photoUrl ?: pInfo?.photoUrl
                                } else {
                                    viewModel.visiblePhotoUrl(memberUid, pInfo?.photoUrl, userProfiles[memberUid])
                                }
                                val memberAbout = if (isMe) {
                                    currentUser?.about?.trim()?.ifEmpty { null }
                                } else {
                                    viewModel.visibleAbout(memberUid, userProfiles[memberUid])
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardBgColor,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("group_member_row_$memberUid")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        if (!photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = baseDisplayName,
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
                                                    text = baseDisplayName.take(1).uppercase(),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = memberTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primaryTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (usernameStr.isNotBlank()) {
                                                Text(
                                                    text = "@$usernameStr",
                                                    fontSize = 13.sp,
                                                    color = secondaryTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (!memberAbout.isNullOrBlank()) {
                                                Text(
                                                    text = memberAbout,
                                                    fontSize = 12.sp,
                                                    color = secondaryTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom actions: creator sees "Edit group name" and "Delete group" above "Leave group"
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (isCreator) {
                                androidx.compose.material3.Button(
                                    onClick = { showEditGroupNameDialog = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = cardBgColor,
                                        contentColor = primaryTextColor
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .testTag("edit_group_name_button")
                                ) {
                                    Text(
                                        text = "Edit group name",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryTextColor
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = { showDeleteGroupConfirmDialog = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.14f),
                                        contentColor = Color(0xFFEF4444)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .testTag("delete_group_button")
                                ) {
                                    Text(
                                        text = "Delete group",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            androidx.compose.material3.Button(
                                onClick = {
                                    val id = activeChatId
                                    showGroupInfo = false
                                    if (id != null) {
                                        viewModel.leaveGroup(id)
                                    } else {
                                        onBack()
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("leave_group_button")
                            ) {
                                Text(
                                    text = "Leave group",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
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
    isGroup: Boolean = false,
    senderName: String? = null,
    readReceipts: Boolean,
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
                    if (!isOwn && !senderName.isNullOrBlank()) {
                        Text(
                            text = senderName,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
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
                            if (isOwn && !isGroup) {
                                MessageStatusTicks(
                                    status = if (!readReceipts && message.status == "read") "delivered" else message.status
                                )
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
private fun ConversationOverflowMenuItem(
    text: String,
    contentColor: Color,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemShape = RoundedCornerShape(12.dp)
    val pressedHighlight = contentColor.copy(alpha = 0.12f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .height(48.dp)
            .clip(itemShape)
            .background(if (isPressed) pressedHighlight else Color.Transparent, itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 14.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogBubble(
    message: ChatMessage,
    currentUid: String,
    accentColor: Color,
    isDark: Boolean,
    onCallBack: (isVideo: Boolean) -> Unit,
    onLongPress: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
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
                .combinedClickable(
                    onClick = {
                        if (!isCaller && (isMissed || isDeclined)) {
                            onCallBack(isVideo)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
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


