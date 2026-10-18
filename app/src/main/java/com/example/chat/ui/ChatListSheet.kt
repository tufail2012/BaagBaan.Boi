package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.data.ChatList
import com.example.chat.model.ChatSummary
import com.example.ui.components.glassContentColor
import com.example.ui.components.glassIndicatorColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.example.ui.components.isGlassSupported
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

private const val SHEET_BLUR_RADIUS_DP = 16f

@Composable
internal fun ChatListSheet(
    allChats: List<ChatSummary>,
    currentUid: String,
    editingList: ChatList?,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onCreateList: (name: String, chatIds: List<String>) -> Boolean,
    onUpdateList: (id: String, name: String, chatIds: List<String>) -> Boolean,
    onDeleteList: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val contentColor = glassContentColor()
    val indicatorColor = glassIndicatorColor()
    val surfaceColor = MaterialTheme.colorScheme.surface

    var listName by remember(editingList) { mutableStateOf(editingList?.name ?: "") }
    var selectedChatIds by remember(editingList) {
        mutableStateOf(editingList?.chatIds?.toSet() ?: emptySet())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isEditMode = editingList != null
    val canSubmit = listName.trim().isNotEmpty() && selectedChatIds.isNotEmpty()
    val panelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val scrimAlpha = if (isDark || isAmoled) 0.35f else 0.25f
    val sheetTintAlpha = if (isDark || isAmoled) 0.6f else 0.7f
    val sheetTintColor = if (isAmoled) Color(0xFF0A0A0A) else surfaceColor
    val blurPx = with(density) { SHEET_BLUR_RADIUS_DP.dp.toPx() }
    val useGlass = backdrop != null && isGlassSupported()

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Full-screen scrim behind the sheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .testTag("chat_list_sheet_scrim")
        )

        // Bottom glass panel reading chatBackdrop as a sibling (outside layerBackdrop)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (useGlass && backdrop != null) {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { panelShape },
                                effects = {
                                    vibrancy()
                                    blur(blurPx)
                                },
                                highlight = null,
                                shadow = null,
                                onDrawSurface = {
                                    drawRect(sheetTintColor.copy(alpha = sheetTintAlpha))
                                }
                            )
                            .clip(panelShape)
                    } else {
                        Modifier
                            .clip(panelShape)
                            .background(surfaceColor.copy(alpha = 0.9f), panelShape)
                    }
                )
                .border(0.5.dp, contentColor.copy(alpha = 0.18f), panelShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("chat_list_sheet")
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(contentColor.copy(alpha = 0.28f))
            )

            // Top row: close (X) icon on the left, and title "New list" / "Edit list" centred
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .testTag("chat_list_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = if (isEditMode) "Edit list" else "New list",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Label "List name"
            Text(
                text = "List name",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = listName,
                onValueChange = { input ->
                    if (input.length <= 30) {
                        listName = input
                    } else {
                        listName = input.take(30)
                    }
                },
                placeholder = {
                    Text(
                        text = "Example: Work, Friends",
                        color = contentColor.copy(alpha = 0.45f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = contentColor,
                    unfocusedBorderColor = contentColor.copy(alpha = 0.25f),
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = contentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_list_name_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Any list you create becomes a filter at the top of your Chats tab.",
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.65f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Label "Add chats"
            Text(
                text = "Add chats",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (allChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chats available",
                        fontSize = 14.sp,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp)
                        .testTag("chat_list_sheet_checklist"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(allChats, key = { it.chatId }) { chat ->
                        val other = chat.getOtherParticipant(currentUid)
                        val displayName = other?.displayName?.ifBlank { other.username } ?: "Member"
                        val isChecked = chat.chatId in selectedChatIds

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedChatIds = if (isChecked) {
                                        selectedChatIds - chat.chatId
                                    } else {
                                        selectedChatIds + chat.chatId
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("chat_list_sheet_row_${chat.chatId}")
                        ) {
                            if (!other?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = other?.photoUrl,
                                    contentDescription = displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(contentColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = contentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedChatIds = if (checked) {
                                        selectedChatIds + chat.chatId
                                    } else {
                                        selectedChatIds - chat.chatId
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = contentColor,
                                    uncheckedColor = contentColor.copy(alpha = 0.5f),
                                    checkmarkColor = indicatorColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fixed bottom full-width "Create list" or "Save" button
            val buttonShape = RoundedCornerShape(percent = 50)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .alpha(if (canSubmit) 1f else 0.4f)
                    .clip(buttonShape)
                    .background(indicatorColor.copy(alpha = 0.55f), buttonShape)
                    .border(0.5.dp, contentColor.copy(alpha = 0.2f), buttonShape)
                    .clickable(enabled = canSubmit) {
                        val orderedIds = allChats
                            .map { it.chatId }
                            .filter { it in selectedChatIds }
                            .ifEmpty { selectedChatIds.toList() }
                        val success = if (editingList != null) {
                            onUpdateList(editingList.id, listName, orderedIds)
                        } else {
                            onCreateList(listName, orderedIds)
                        }
                        if (success) {
                            onDismiss()
                        }
                    }
                    .testTag("chat_list_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditMode) "Save" else "Create list",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }

            if (editingList != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_list_delete_button")
                ) {
                    Text(
                        text = "Delete list",
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && editingList != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete this list?",
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteList(editingList.id)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_delete_list_button")
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
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = contentColor
                    )
                }
            }
        )
    }
}
