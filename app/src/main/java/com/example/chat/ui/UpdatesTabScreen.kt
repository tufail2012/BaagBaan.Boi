package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.model.ParticipantInfo
import com.example.chat.model.StatusItem
import com.example.chat.model.UserStatusGroup
import com.example.ui.components.glassContentColor
import com.google.firebase.Timestamp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val STATUS_BG_HEX_PALETTE = listOf(
    "#0F766E", // Deep Teal
    "#1D4ED8", // Royal Blue
    "#6D28D9", // Deep Purple
    "#BE123C", // Crimson Rose
    "#15803D", // Forest Green
    "#B45309", // Warm Amber
    "#1E293B", // Slate Dark
    "#4338CA"  // Indigo
)

internal fun parseStatusBackgroundColor(hex: String): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = when (cleaned.length) {
            6 -> ("FF$cleaned").toLong(16)
            8 -> cleaned.toLong(16)
            else -> 0xFF0F766EL
        }
        Color(value)
    } catch (_: Exception) {
        Color(0xFF0F766E)
    }
}

internal fun formatStatusRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val date = timestamp.toDate()
    val nowMs = System.currentTimeMillis()
    val diffMs = nowMs - date.time
    if (diffMs < 60_000L) return "Just now"
    val diffMins = diffMs / 60_000L
    if (diffMins < 60L) return "${diffMins}m ago"

    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val isToday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (isToday) return "Today, ${timeFmt.format(date)}"
    return "Yesterday, ${timeFmt.format(date)}"
}

@Composable
fun UpdatesTabScreen(
    viewModel: ChatViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    topPadding: Dp = 76.dp,
    onOpenComposer: () -> Unit,
    onOpenViewer: (UserStatusGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentColor = glassContentColor()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val myStatuses by viewModel.myStatuses.collectAsState()
    val contactGroups by viewModel.contactStatusGroups.collectAsState()
    val viewedStatusIds by viewModel.viewedStatusIds.collectAsState()
    val nicknames by viewModel.nicknames.collectAsState()

    // Refresh statuses on open and periodically prune expired statuses
    LaunchedEffect(Unit) {
        viewModel.refreshStatuses()
        while (true) {
            delay(15_000L)
            viewModel.refreshStatuses()
        }
    }

    val (unviewedGroups, viewedGroups) = remember(contactGroups, viewedStatusIds, currentUid) {
        val unviewed = mutableListOf<UserStatusGroup>()
        val viewed = mutableListOf<UserStatusGroup>()
        for (group in contactGroups) {
            val hasUnseen = group.statuses.any { status ->
                status.id !in viewedStatusIds && currentUid !in status.viewerIds
            }
            if (hasUnseen) {
                unviewed.add(group)
            } else {
                viewed.add(group)
            }
        }
        unviewed to viewed
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("updates_list_column"),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = 120.dp + navBarBottom
            )
        ) {
            // Section label: Status
            item {
                Text(
                    text = "Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                )
            }

            // My status row
            item {
                val hasMyStatus = myStatuses.isNotEmpty()
                val latestMine = myStatuses.lastOrNull()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (hasMyStatus) {
                                onOpenViewer(
                                    UserStatusGroup(
                                        userId = currentUid,
                                        username = currentUser?.username.orEmpty(),
                                        displayName = currentUser?.displayName?.ifBlank { currentUser?.username }.orEmpty(),
                                        photoUrl = currentUser?.photoUrl,
                                        statuses = myStatuses,
                                        latestTimestamp = latestMine?.createdAt
                                    )
                                )
                            } else {
                                onOpenComposer()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("my_status_row")
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasMyStatus) {
                            StatusRingAvatar(
                                photoUrl = currentUser?.photoUrl,
                                fallbackText = (currentUser?.displayName?.ifBlank { currentUser?.username } ?: "M"),
                                segmentCount = myStatuses.size,
                                ringColor = accentColor,
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        } else {
                            AvatarCircle(
                                photoUrl = currentUser?.photoUrl,
                                fallbackText = (currentUser?.displayName?.ifBlank { currentUser?.username } ?: "M"),
                                size = 52.dp,
                                accentColor = accentColor
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add status",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasMyStatus) {
                                val countLabel = if (myStatuses.size == 1) "1 update" else "${myStatuses.size} updates"
                                "$countLabel • ${formatStatusRelativeTime(latestMine?.createdAt)}"
                            } else {
                                "Tap to add status update"
                            },
                            fontSize = 13.sp,
                            color = contentColor.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasMyStatus) {
                        IconButton(
                            onClick = onOpenComposer,
                            modifier = Modifier.testTag("my_status_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Add another status",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (unviewedGroups.isEmpty() && viewedGroups.isEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent updates",
                            fontSize = 15.sp,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                if (unviewedGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent updates",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                        )
                    }
                    items(unviewedGroups, key = { "unviewed_${it.userId}" }) { group ->
                        val resolvedName = remember(group, nicknames) {
                            viewModel.displayNameFor(
                                group.userId,
                                ParticipantInfo(
                                    uid = group.userId,
                                    username = group.username,
                                    displayName = group.displayName,
                                    photoUrl = group.photoUrl
                                )
                            )
                        }
                        StatusGroupRow(
                            group = group,
                            displayName = resolvedName,
                            ringColor = accentColor,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = { onOpenViewer(group) }
                        )
                    }
                }

                if (viewedGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Viewed updates",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                        )
                    }
                    items(viewedGroups, key = { "viewed_${it.userId}" }) { group ->
                        val resolvedName = remember(group, nicknames) {
                            viewModel.displayNameFor(
                                group.userId,
                                ParticipantInfo(
                                    uid = group.userId,
                                    username = group.username,
                                    displayName = group.displayName,
                                    photoUrl = group.photoUrl
                                )
                            )
                        }
                        StatusGroupRow(
                            group = group,
                            displayName = resolvedName,
                            ringColor = contentColor.copy(alpha = 0.35f),
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = { onOpenViewer(group) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusGroupRow(
    group: UserStatusGroup,
    displayName: String,
    ringColor: Color,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val latest = group.statuses.lastOrNull()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("status_group_row_${group.userId}")
    ) {
        StatusRingAvatar(
            photoUrl = group.photoUrl,
            fallbackText = displayName,
            segmentCount = group.statuses.size,
            ringColor = ringColor,
            contentColor = contentColor,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatStatusRelativeTime(group.latestTimestamp ?: latest?.createdAt),
                fontSize = 13.sp,
                color = contentColor.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusRingAvatar(
    photoUrl: String?,
    fallbackText: String,
    segmentCount: Int,
    ringColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.6.dp.toPx()
            val count = segmentCount.coerceAtLeast(1)
            if (count == 1) {
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                val gapDegrees = 10f
                val sweep = (360f - gapDegrees * count) / count
                for (i in 0 until count) {
                    val start = -90f + i * (sweep + gapDegrees) + gapDegrees / 2f
                    drawArc(
                        color = ringColor,
                        startAngle = start,
                        sweepAngle = sweep.coerceAtLeast(6f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
        AvatarCircle(
            photoUrl = photoUrl,
            fallbackText = fallbackText,
            size = 46.dp,
            accentColor = accentColor
        )
    }
}

@Composable
private fun AvatarCircle(
    photoUrl: String?,
    fallbackText: String,
    size: Dp,
    accentColor: Color
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = fallbackText,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.trim().take(1).uppercase().ifEmpty { "U" },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accentColor
            )
        }
    }
}

@Composable
internal fun UpdatesTopHeaderOverlay(
    viewModel: ChatViewModel,
    backdrop: Backdrop?,
    onHeaderHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = glassContentColor()
    val currentUser by viewModel.currentUser.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .onSizeChanged { onHeaderHeightChanged(it.height) }
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    ) {
        // Left glass pill: Updates icon + "Updates" title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .chatHeaderGlass(backdrop)
                .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Updates",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        // Right side: profile button opening Settings screen
        Box(
            modifier = Modifier
                .size(50.dp)
                .chatHeaderGlass(backdrop, CircleShape)
                .clip(CircleShape)
                .clickable { viewModel.openSettings() }
                .testTag("updates_profile_button"),
            contentAlignment = Alignment.Center
        ) {
            val photo = currentUser?.photoUrl
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
internal fun UpdatesFloatingStatusButton(
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val contentColor = glassContentColor()
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 104.dp)
            .size(56.dp)
            .chatHeaderGlass(backdrop, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("updates_fab_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "New status update",
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
internal fun TextStatusComposerOverlay(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPosting by viewModel.isPostingStatus.collectAsState()
    var statusText by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    val selectedHex = STATUS_BG_HEX_PALETTE[colorIndex % STATUS_BG_HEX_PALETTE.size]
    val bgColor = remember(selectedHex) { parseStatusBackgroundColor(selectedHex) }
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        if (!isPosting) onDismiss()
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("text_status_composer_screen")
    ) {
        // Top bar: Close and Color cycle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = { if (!isPosting) onDismiss() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .testTag("status_composer_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { colorIndex = (colorIndex + 1) % STATUS_BG_HEX_PALETTE.size },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .testTag("status_composer_color_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change background colour",
                    tint = Color.White
                )
            }
        }

        // Centered text field
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 88.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = statusText,
                onValueChange = { if (it.length <= 700) statusText = it },
                cursorBrush = SolidColor(Color.White),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = if (statusText.length < 120) 28.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (statusText.length < 120) 36.sp else 28.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("status_composer_input"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (statusText.isEmpty()) {
                            Text(
                                text = "Type a status...",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Bottom bar: color swatches and Send button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.22f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                STATUS_BG_HEX_PALETTE.forEachIndexed { idx, hex ->
                    val swatchColor = parseStatusBackgroundColor(hex)
                    val isSelected = idx == (colorIndex % STATUS_BG_HEX_PALETTE.size)
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 28.dp else 22.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .clickable { colorIndex = idx }
                    )
                }
            }

            val canSend = statusText.trim().isNotEmpty() && !isPosting
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Color(0xFF22C55E) else Color.White.copy(alpha = 0.25f))
                    .clickable(enabled = canSend) {
                        viewModel.postTextStatus(statusText.trim(), selectedHex) { success ->
                            if (success) onDismiss()
                        }
                    }
                    .testTag("status_composer_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post status",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusViewerOverlay(
    group: UserStatusGroup,
    currentUid: String,
    displayName: String,
    accentColor: Color,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = group.statuses
    if (statuses.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var currentIndex by remember(group.userId, statuses.size) {
        mutableIntStateOf(0)
    }
    val safeIndex = currentIndex.coerceIn(0, (statuses.size - 1).coerceAtLeast(0))
    val currentStatus: StatusItem = statuses[safeIndex]
    val bgColor = remember(currentStatus.backgroundColor) {
        parseStatusBackgroundColor(currentStatus.backgroundColor)
    }
    val progress = remember(currentStatus.id) { Animatable(0f) }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(currentStatus.id) {
        if (group.userId != currentUid) {
            viewModel.markStatusViewed(currentStatus.id)
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        if (safeIndex < statuses.size - 1) {
            currentIndex = safeIndex + 1
        } else {
            onDismiss()
        }
    }

    var containerWidthPx by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { containerWidthPx = it.width }
            .pointerInput(safeIndex, statuses.size, containerWidthPx) {
                detectTapGestures { offset ->
                    val threshold = if (containerWidthPx > 0) containerWidthPx * 0.35f else 300f
                    if (offset.x < threshold) {
                        if (safeIndex > 0) {
                            currentIndex = safeIndex - 1
                        }
                    } else {
                        if (safeIndex < statuses.size - 1) {
                            currentIndex = safeIndex + 1
                        } else {
                            onDismiss()
                        }
                    }
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("status_viewer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Segmented progress bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                statuses.forEachIndexed { idx, _ ->
                    val barProgress = when {
                        idx < safeIndex -> 1f
                        idx == safeIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header row: Back button, avatar, name + timestamp, and delete button (for own status)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("status_viewer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                AvatarCircle(
                    photoUrl = group.photoUrl,
                    fallbackText = displayName,
                    size = 40.dp,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (group.userId == currentUid) "My status" else displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatStatusRelativeTime(currentStatus.createdAt),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                if (group.userId == currentUid) {
                    IconButton(
                        onClick = {
                            val idToDelete = currentStatus.id
                            val remainingCount = statuses.size - 1
                            viewModel.deleteStatus(idToDelete)
                            if (remainingCount <= 0) {
                                onDismiss()
                            } else if (safeIndex >= remainingCount) {
                                currentIndex = (remainingCount - 1).coerceAtLeast(0)
                            }
                        },
                        modifier = Modifier.testTag("status_viewer_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete status",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Centered status text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 96.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentStatus.text,
                color = Color.White,
                fontSize = if (currentStatus.text.length < 120) 28.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = if (currentStatus.text.length < 120) 36.sp else 28.sp
            )
        }
    }
}
