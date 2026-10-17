package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.call.ZegoCallManager
import com.example.chat.data.ChatItemLocalState
import com.example.chat.data.ChatList
import com.example.chat.model.ChatSummary
import com.example.chat.model.ChatTab
import com.example.chat.model.ParticipantInfo
import com.example.chat.ui.call.MinimizedCallBar
import com.example.ui.components.glassContentColor
import com.example.ui.components.glassIndicatorColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.google.firebase.Timestamp
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ChatsListScreen(
    viewModel: ChatViewModel,
    onBackToApp: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val allChats by viewModel.allChatsList.collectAsState()
    val chats by viewModel.chatsList.collectAsState()
    val archivedChats by viewModel.archivedChatsList.collectAsState()
    val chatLocalStates by viewModel.chatLocalStates.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val customLists by viewModel.customLists.collectAsState()
    val isLoading by viewModel.isChatsLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid ?: ""
    val activeCall by ZegoCallManager.activeCallState.collectAsState()

    var selectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var showListSheet by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<ChatList?>(null) }

    val unreadCount = remember(allChats, chatLocalStates) {
        allChats.count { chatLocalStates[it.chatId]?.markedUnread == true }
    }
    val favouritesCount = remember(allChats, chatLocalStates) {
        allChats.count { chatLocalStates[it.chatId]?.favourite == true }
    }

    // Keep selection clean if chats list updates and removes selected items
    LaunchedEffect(chats) {
        if (selectedChatIds.isNotEmpty()) {
            val visibleIds = chats.map { it.chatId }.toSet()
            val remaining = selectedChatIds.intersect(visibleIds)
            if (remaining.size != selectedChatIds.size) {
                selectedChatIds = remaining
            }
        }
    }

    BackHandler(enabled = selectedChatIds.isNotEmpty()) {
        selectedChatIds = emptySet()
    }

    val contentColor = glassContentColor()

    if (showListSheet) {
        ChatListSheet(
            allChats = allChats,
            currentUid = currentUid,
            editingList = editingList,
            backdrop = backdrop,
            onDismiss = {
                showListSheet = false
                editingList = null
            },
            onCreateList = { name, chatIds ->
                viewModel.createList(name, chatIds)
            },
            onUpdateList = { id, name, chatIds ->
                viewModel.updateList(id, name, chatIds)
            },
            onDeleteList = { id ->
                viewModel.deleteList(id)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (selectedChatIds.isNotEmpty()) {
            ChatSelectionBar(
                selectedChatIds = selectedChatIds,
                allSelectableChatIds = chats.map { it.chatId }.toSet(),
                chatLocalStates = chatLocalStates,
                viewModel = viewModel,
                backdrop = backdrop,
                isDark = isDark,
                contentColor = contentColor,
                onUpdateSelection = { selectedChatIds = it },
                onClearSelection = { selectedChatIds = emptySet() }
            )
        } else {
            // Top App Bar: glass pills, icons and text black or white by theme only
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            ) {
                // Left glass pill: chat icon + "Chats" title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .chatHeaderGlass(backdrop)
                        .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chats",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                // Right side: profile pill (no text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .chatHeaderGlass(backdrop, CircleShape)
                            .clip(CircleShape)
                            .clickable { viewModel.selectTab(ChatTab.SETTINGS) }
                            .testTag("chat_list_profile_button"),
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

            // Expanded search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .chatHeaderGlass(backdrop, RoundedCornerShape(percent = 50))
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { viewModel.openSearch() }
                        .testTag("open_user_search_button"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search users",
                            fontSize = 16.sp,
                            color = contentColor.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
            }

            // Glass filter row
            ChatFilterRow(
                selectedFilter = selectedFilter,
                unreadCount = unreadCount,
                favouritesCount = favouritesCount,
                customLists = customLists,
                backdrop = backdrop,
                contentColor = contentColor,
                onSelectFilter = { viewModel.selectFilter(it) },
                onAddListClick = {
                    editingList = null
                    showListSheet = true
                },
                onEditCustomList = { list ->
                    editingList = list
                    showListSheet = true
                }
            )
        }

        // Active Minimized Call Bar
        activeCall?.let { call ->
            MinimizedCallBar(
                state = call,
                accentColor = accentColor,
                isDark = isDark
            )
        }

        // Content
        val filterEmptyLabel: String? = when (val f = selectedFilter) {
            ChatFilter.All -> if (chats.isEmpty() && allChats.isNotEmpty()) "No chats in All" else null
            ChatFilter.Unread -> if (chats.isEmpty()) "No chats in Unread" else null
            ChatFilter.Favourites -> if (chats.isEmpty()) "No chats in Favourites" else null
            ChatFilter.Groups -> if (chats.isEmpty()) "No chats in Groups" else null
            is ChatFilter.Custom -> if (chats.isEmpty()) {
                val listName = customLists.find { it.id == f.listId }?.name ?: "list"
                "No chats in $listName"
            } else null
        }

        if (isLoading && allChats.isEmpty() && archivedChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (filterEmptyLabel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)
                    .padding(horizontal = 32.dp)
                    .testTag("empty_filter_state"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filterEmptyLabel,
                    fontSize = 15.sp,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        } else if (allChats.isEmpty() && archivedChats.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "No chats yet",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Search a username to start a conversation with fellow members.",
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.openSearch() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.testTag("empty_state_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Find Members",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chats_list_column"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 110.dp + navBarBottom
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (selectedFilter == ChatFilter.All && archivedChats.isNotEmpty()) {
                    item(key = "archived_header_row") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openArchived() }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("archived_chats_row")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(contentColor.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archived",
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Archived (${archivedChats.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }

                items(chats, key = { it.chatId }) { chat ->
                    val other = chat.getOtherParticipant(currentUid)
                    val isSelected = chat.chatId in selectedChatIds
                    val localState = chatLocalStates[chat.chatId]
                    ChatListItemRow(
                        chat = chat,
                        other = other,
                        accentColor = accentColor,
                        isDark = isDark,
                        contentColor = contentColor,
                        isSelected = isSelected,
                        localState = localState,
                        onClick = {
                            if (selectedChatIds.isNotEmpty()) {
                                selectedChatIds = if (isSelected) {
                                    selectedChatIds - chat.chatId
                                } else {
                                    selectedChatIds + chat.chatId
                                }
                            } else {
                                viewModel.openChatFromSummary(chat)
                            }
                        },
                        onLongClick = {
                            selectedChatIds = if (isSelected) {
                                selectedChatIds - chat.chatId
                            } else {
                                selectedChatIds + chat.chatId
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChatSelectionBar(
    selectedChatIds: Set<String>,
    allSelectableChatIds: Set<String>,
    chatLocalStates: Map<String, ChatItemLocalState>,
    viewModel: ChatViewModel,
    backdrop: Backdrop?,
    isDark: Boolean,
    contentColor: Color,
    onUpdateSelection: (Set<String>) -> Unit,
    onClearSelection: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMuteDialog by remember { mutableStateOf(false) }

    val allPinned = selectedChatIds.isNotEmpty() &&
        selectedChatIds.all { chatLocalStates[it]?.pinned == true }
    val allMuted = selectedChatIds.isNotEmpty() &&
        selectedChatIds.all { chatLocalStates[it]?.isMuted() == true }
    val allArchived = selectedChatIds.isNotEmpty() &&
        selectedChatIds.all { chatLocalStates[it]?.archived == true }
    val anyUnread = selectedChatIds.any { chatLocalStates[it]?.markedUnread == true }
    val allFavourite = selectedChatIds.isNotEmpty() &&
        selectedChatIds.all { chatLocalStates[it]?.favourite == true }

    // Selection top bar: one glass pill across the full width
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .chatHeaderGlass(backdrop)
                .padding(horizontal = 6.dp)
        ) {
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("selection_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Clear selection",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "${selectedChatIds.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // Pin / Unpin (diagonal slash when all selected are pinned)
            IconButton(
                onClick = {
                    viewModel.togglePinChats(selectedChatIds)
                    onClearSelection()
                },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("selection_pin_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if (allPinned) "Unpin" else "Pin",
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (allPinned) {
                                Modifier.drawWithContent {
                                    drawContent()
                                    drawLine(
                                        color = contentColor,
                                        start = Offset(size.width * 0.14f, size.height * 0.14f),
                                        end = Offset(size.width * 0.86f, size.height * 0.86f),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Delete
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("selection_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Mute (plain bell) / Unmute (bell with diagonal line)
            IconButton(
                onClick = {
                    if (allMuted) {
                        viewModel.unmuteChats(selectedChatIds)
                        onClearSelection()
                    } else {
                        showMuteDialog = true
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("selection_mute_button")
            ) {
                Icon(
                    imageVector = if (allMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = if (allMuted) "Unmute" else "Mute",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Archive / Unarchive
            IconButton(
                onClick = {
                    if (allArchived) {
                        viewModel.unarchiveChats(selectedChatIds)
                    } else {
                        viewModel.archiveChats(selectedChatIds)
                    }
                    onClearSelection()
                },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("selection_archive_button")
            ) {
                Icon(
                    imageVector = if (allArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = if (allArchived) "Unarchive" else "Archive",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3-dot overflow menu
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("selection_more_button")
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
                            .chatHeaderGlass(backdrop, menuShape)
                            .padding(vertical = 8.dp)
                    ) {
                        SelectionMenuItem(
                            text = "Add chat shortcut",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.showToast("Coming soon")
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = if (anyUnread) "Mark as read" else "Mark as unread",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                if (anyUnread) {
                                    viewModel.markChatsAsRead(selectedChatIds)
                                } else {
                                    viewModel.markChatsAsUnread(selectedChatIds)
                                }
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = "Select all",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                onUpdateSelection(allSelectableChatIds)
                            }
                        )
                        SelectionMenuItem(
                            text = "Lock chat",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.showToast("Coming soon")
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = if (allFavourite) "Remove from favourites" else "Add to favourites",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.toggleFavouriteChats(selectedChatIds)
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = "Add to list",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.showToast("Coming soon")
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = "Clear chat",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.showToast("Coming soon")
                                onClearSelection()
                            }
                        )
                        SelectionMenuItem(
                            text = "Block",
                            contentColor = contentColor,
                            onClick = {
                                showOverflowMenu = false
                                viewModel.showToast("Coming soon")
                                onClearSelection()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete chat?",
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideDeletedChatsLocally(selectedChatIds)
                        showDeleteDialog = false
                        onClearSelection()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = contentColor
                    )
                }
            }
        )
    }

    if (showMuteDialog) {
        var selectedOption by remember { mutableIntStateOf(0) }
        val options = listOf("8 hours", "1 week", "Always")
        AlertDialog(
            onDismissRequest = { showMuteDialog = false },
            title = {
                Text(
                    text = "Mute message notifications",
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    options.forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = index }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedOption == index,
                                onClick = { selectedOption = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = contentColor,
                                    unselectedColor = contentColor.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                color = contentColor
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val mutedUntil = when (selectedOption) {
                            0 -> now + 8L * 60L * 60L * 1000L
                            1 -> now + 7L * 24L * 60L * 60L * 1000L
                            else -> Long.MAX_VALUE
                        }
                        viewModel.muteChats(selectedChatIds, mutedUntil)
                        showMuteDialog = false
                        onClearSelection()
                    }
                ) {
                    Text(
                        text = "OK",
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showMuteDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = contentColor
                    )
                }
            }
        )
    }
}

@Composable
internal fun SelectionMenuItem(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatListItemRow(
    chat: ChatSummary,
    other: ParticipantInfo?,
    accentColor: Color,
    isDark: Boolean,
    contentColor: Color,
    isSelected: Boolean,
    localState: ChatItemLocalState?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = other?.displayName?.ifBlank { other.username } ?: "Member"
    val username = other?.username ?: ""
    val lastMsg = chat.lastMessage
    val formattedTime = remember(lastMsg?.timestamp) {
        formatTimestamp(lastMsg?.timestamp)
    }
    val isPinned = localState?.pinned == true
    val isMuted = localState?.isMuted() == true
    val isMarkedUnread = localState?.markedUnread == true

    Surface(
        shape = RoundedCornerShape(0.dp),
        color = if (isSelected) contentColor.copy(alpha = 0.10f) else Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("chat_item_${chat.chatId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Avatar or 48dp selection circle
                if (isSelected) {
                    val tickColor = if (isDark) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(contentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = tickColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (!other?.photoUrl.isNullOrBlank()) {
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
                            .background(accentColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (formattedTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (username.isNotEmpty()) {
                                Text(
                                    text = "@$username",
                                    fontSize = 12.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Text(
                                text = lastMsg?.text ?: "No messages yet",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isMuted || isPinned || isMarkedUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isMuted) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = "Muted",
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isPinned) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isMarkedUnread) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(contentColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.6.dp,
                color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
        else -> {
            SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date)
        }
    }
}

private data class FilterChipDescriptor(
    val key: String,
    val label: String,
    val filter: ChatFilter,
    val testTag: String,
    val onLongClick: (() -> Unit)? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatFilterRow(
    selectedFilter: ChatFilter,
    unreadCount: Int,
    favouritesCount: Int,
    customLists: List<ChatList>,
    backdrop: Backdrop?,
    contentColor: Color,
    onSelectFilter: (ChatFilter) -> Unit,
    onAddListClick: () -> Unit,
    onEditCustomList: (ChatList) -> Unit
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val capsuleShape = RoundedCornerShape(percent = 50)
    val indicatorColor = glassIndicatorColor().copy(alpha = 0.55f)

    val chips = remember(unreadCount, favouritesCount, customLists) {
        buildList {
            add(
                FilterChipDescriptor(
                    key = "all",
                    label = "All",
                    filter = ChatFilter.All,
                    testTag = "filter_chip_all"
                )
            )
            add(
                FilterChipDescriptor(
                    key = "unread",
                    label = if (unreadCount > 0) "Unread $unreadCount" else "Unread",
                    filter = ChatFilter.Unread,
                    testTag = "filter_chip_unread"
                )
            )
            add(
                FilterChipDescriptor(
                    key = "favourites",
                    label = if (favouritesCount > 0) "Favourites $favouritesCount" else "Favourites",
                    filter = ChatFilter.Favourites,
                    testTag = "filter_chip_favourites"
                )
            )
            add(
                FilterChipDescriptor(
                    key = "groups",
                    label = "Groups",
                    filter = ChatFilter.Groups,
                    testTag = "filter_chip_groups"
                )
            )
            for (list in customLists) {
                add(
                    FilterChipDescriptor(
                        key = "custom_${list.id}",
                        label = list.name,
                        filter = ChatFilter.Custom(list.id),
                        testTag = "filter_chip_custom_${list.id}",
                        onLongClick = { onEditCustomList(list) }
                    )
                )
            }
        }
    }

    val selectedKey = when (selectedFilter) {
        ChatFilter.All -> "all"
        ChatFilter.Unread -> "unread"
        ChatFilter.Favourites -> "favourites"
        ChatFilter.Groups -> "groups"
        is ChatFilter.Custom -> "custom_${selectedFilter.listId}"
    }

    val chipBounds = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val targetBounds = chipBounds[selectedKey]

    val pillSpring = remember {
        spring<Float>(
            stiffness = 500f,
            dampingRatio = 0.85f
        )
    }

    val animatedX by animateFloatAsState(
        targetValue = (targetBounds?.first ?: 0).toFloat(),
        animationSpec = pillSpring,
        label = "chatFilterPillX"
    )
    val animatedWidth by animateFloatAsState(
        targetValue = (targetBounds?.second ?: 0).toFloat(),
        animationSpec = pillSpring,
        label = "chatFilterPillWidth"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag("chat_filter_row")
    ) {
        // Layer 1: Glass capsule backgrounds for each chip and the + circle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chips.forEach { chip ->
                val isSelected = chip.key == selectedKey
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .chatHeaderGlass(backdrop = backdrop, shape = capsuleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chip.label,
                        color = Color.Transparent,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .chatHeaderGlass(backdrop = backdrop, shape = CircleShape)
            )
        }

        // Layer 2: Animated selection pill behind the selected chip's text
        if (targetBounds != null && animatedWidth > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedX.roundToInt(), 0) }
                    .width(with(density) { animatedWidth.toDp() })
                    .height(40.dp)
                    .clip(capsuleShape)
                    .background(indicatorColor, capsuleShape)
                    .border(0.5.dp, contentColor.copy(alpha = 0.25f), capsuleShape)
            )
        }

        // Layer 3: Interactive foreground labels and + icon button
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chips.forEach { chip ->
                val isSelected = chip.key == selectedKey
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .onGloballyPositioned { coords ->
                            val x = coords.positionInParent().x.roundToInt()
                            val w = coords.size.width
                            val current = chipBounds[chip.key]
                            if (current == null || current.first != x || current.second != w) {
                                chipBounds[chip.key] = x to w
                            }
                        }
                        .clip(capsuleShape)
                        .combinedClickable(
                            onClick = { onSelectFilter(chip.filter) },
                            onLongClick = chip.onLongClick
                        )
                        .padding(horizontal = 16.dp)
                        .testTag(chip.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chip.label,
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAddListClick)
                    .testTag("filter_chip_add_list"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New list",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
