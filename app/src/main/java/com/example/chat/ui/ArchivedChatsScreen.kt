package com.example.chat.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInDarkMode
import com.kyant.backdrop.Backdrop

@Composable
fun ArchivedChatsScreen(
    viewModel: ChatViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    selectedChatIds: Set<String> = emptySet(),
    onUpdateSelection: (Set<String>) -> Unit = {},
    topPadding: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val contentColor = glassContentColor()
    val archivedChats by viewModel.archivedChatsList.collectAsState()
    val chatLocalStates by viewModel.chatLocalStates.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = topPadding)
    ) {
        Text(
            text = "These chats stay archived until you move them back.",
            fontSize = 13.sp,
            color = contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (archivedChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No archived chats",
                    fontSize = 15.sp,
                    color = contentColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("archived_chats_list_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(archivedChats, key = { it.chatId }) { chat ->
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
                                val updated = if (isSelected) {
                                    selectedChatIds - chat.chatId
                                } else {
                                    selectedChatIds + chat.chatId
                                }
                                onUpdateSelection(updated)
                            } else {
                                viewModel.openChatFromSummary(chat)
                            }
                        },
                        onLongClick = {
                            val updated = if (isSelected) {
                                selectedChatIds - chat.chatId
                            } else {
                                selectedChatIds + chat.chatId
                            }
                            onUpdateSelection(updated)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
internal fun ArchivedChatsTopHeaderOverlay(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    backdrop: Backdrop?,
    selectedChatIds: Set<String>,
    onUpdateSelection: (Set<String>) -> Unit,
    onClearSelection: () -> Unit,
    onHeaderHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val contentColor = glassContentColor()
    val archivedChats by viewModel.archivedChatsList.collectAsState()
    val chatLocalStates by viewModel.chatLocalStates.collectAsState()
    var showOverflowMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .onSizeChanged { onHeaderHeightChanged(it.height) }
    ) {
        if (selectedChatIds.isNotEmpty()) {
            ChatSelectionBar(
                selectedChatIds = selectedChatIds,
                allSelectableChatIds = archivedChats.map { it.chatId }.toSet(),
                chatLocalStates = chatLocalStates,
                viewModel = viewModel,
                backdrop = backdrop,
                isDark = isDark,
                contentColor = contentColor,
                onUpdateSelection = onUpdateSelection,
                onClearSelection = onClearSelection
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            ) {
                // Left glass pill: back arrow + "Archived" title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .chatHeaderGlass(backdrop)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { onBack() }
                        .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 8.dp)
                        .testTag("archived_back_pill")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Archived",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                // Right glass pill: 3-dot button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(50.dp)
                        .chatHeaderGlass(backdrop)
                        .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("archived_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
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
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), menuShape)
                                    .border(0.5.dp, contentColor.copy(alpha = 0.15f), menuShape)
                                    .padding(vertical = 8.dp)
                            ) {
                                SelectionMenuItem(
                                    text = "Select all",
                                    contentColor = contentColor,
                                    onClick = {
                                        showOverflowMenu = false
                                        onUpdateSelection(archivedChats.map { it.chatId }.toSet())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
