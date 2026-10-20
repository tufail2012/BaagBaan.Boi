package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.chat.model.ChatUser
import com.example.chat.model.ParticipantInfo
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode

@Composable
fun CreateGroupOverlay(
    viewModel: ChatViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val allChats by viewModel.allChatsList.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val nicknames by viewModel.nicknames.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    val selectedMembers = remember { mutableStateMapOf<String, ChatUser>() }

    DisposableEffect(Unit) {
        viewModel.updateSearchQuery("")
        onDispose {
            viewModel.updateSearchQuery("")
        }
    }

    BackHandler {
        if (!isCreating) {
            onDismiss()
        }
    }

    val directContacts = remember(allChats, currentUid, blockedUserIds) {
        allChats
            .filter { !it.isGroup }
            .mapNotNull { chat ->
                val otherUid = chat.participantIds.firstOrNull { it != currentUid }.orEmpty()
                if (otherUid.isBlank() || otherUid in blockedUserIds) return@mapNotNull null
                val info = chat.getOtherParticipant(currentUid)
                ChatUser(
                    uid = otherUid,
                    username = info?.username.orEmpty(),
                    displayName = info?.displayName?.ifBlank { info.username }.orEmpty(),
                    photoUrl = info?.photoUrl
                )
            }
            .distinctBy { it.uid }
    }

    val filteredSearchResults = remember(searchResults, currentUid, blockedUserIds) {
        searchResults.filter { it.uid.isNotBlank() && it.uid != currentUid && it.uid !in blockedUserIds }
    }

    val bgColor = when {
        isAmoled -> Color.Black
        isDark -> Color(0xFF0B141A)
        else -> Color(0xFFF8FAFC)
    }
    val textColor = if (isDark || isAmoled) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardColor = if (isDark || isAmoled) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark || isAmoled) Color(0xFF334155) else Color(0xFFCBD5E1)

    val canCreate = groupName.trim().isNotEmpty() && selectedMembers.isNotEmpty() && !isCreating

    fun toggleMember(user: ChatUser) {
        if (selectedMembers.containsKey(user.uid)) {
            selectedMembers.remove(user.uid)
        } else {
            selectedMembers[user.uid] = user
        }
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
            .testTag("create_group_overlay")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar: Close X on left, "New group" title, Create button on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { if (!isCreating) onDismiss() },
                    modifier = Modifier.testTag("create_group_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }

                Text(
                    text = "New group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Button(
                    onClick = {
                        if (!canCreate) return@Button
                        isCreating = true
                        viewModel.createGroup(
                            name = groupName,
                            members = selectedMembers.values.toList()
                        ) { chatId ->
                            isCreating = false
                            if (chatId != null) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = canCreate,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White,
                        disabledContainerColor = accentColor.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("create_group_confirm_button")
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "Create",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Group name input with max 40 characters and counter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { if (it.length <= 40) groupName = it },
                    label = { Text("Group name") },
                    placeholder = { Text("Enter group name") },
                    singleLine = true,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${groupName.length}/40",
                                fontSize = 12.sp,
                                color = subTextColor,
                                modifier = Modifier.testTag("create_group_name_counter")
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = accentColor,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = subTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_name_input")
                )
            }

            // Selected members chips
            if (selectedMembers.isNotEmpty()) {
                val chipScroll = rememberScrollState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScroll)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("create_group_selected_chips")
                ) {
                    selectedMembers.values.forEach { member ->
                        val chipName = remember(member, nicknames) {
                            viewModel.displayNameFor(
                                member.uid,
                                ParticipantInfo(
                                    uid = member.uid,
                                    username = member.username,
                                    displayName = member.displayName,
                                    photoUrl = member.photoUrl
                                )
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(accentColor.copy(alpha = 0.16f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
                                .clickable { selectedMembers.remove(member.uid) }
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                                .testTag("selected_member_chip_${member.uid}")
                        ) {
                            Text(
                                text = chipName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $chipName",
                                tint = subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("create_group_members_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "direct_contacts_header") {
                    Text(
                        text = "Direct contacts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subTextColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                if (directContacts.isEmpty()) {
                    item(key = "direct_contacts_empty") {
                        Text(
                            text = "No direct contacts yet. Search by username below.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(directContacts, key = { "contact_${it.uid}" }) { contact ->
                        val resolvedName = remember(contact, nicknames) {
                            viewModel.displayNameFor(
                                contact.uid,
                                ParticipantInfo(
                                    uid = contact.uid,
                                    username = contact.username,
                                    displayName = contact.displayName,
                                    photoUrl = contact.photoUrl
                                )
                            )
                        }
                        val isSelected = selectedMembers.containsKey(contact.uid)
                        CreateGroupMemberRow(
                            user = contact,
                            displayName = resolvedName,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            cardColor = cardColor,
                            testTag = "create_group_contact_${contact.uid}",
                            onToggle = { toggleMember(contact) }
                        )
                    }
                }

                item(key = "username_search_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Add by username",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Search by username...",
                                    color = subTextColor,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = accentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_group_user_search_input")
                        )
                    }
                }

                if (isSearching) {
                    item(key = "search_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else if (searchQuery.trim().isNotEmpty() && filteredSearchResults.isEmpty()) {
                    item(key = "search_empty") {
                        Text(
                            text = "No users found matching \"$searchQuery\"",
                            fontSize = 13.sp,
                            color = subTextColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else if (searchQuery.trim().isNotEmpty()) {
                    items(filteredSearchResults, key = { "search_${it.uid}" }) { user ->
                        val resolvedName = remember(user, nicknames) {
                            viewModel.displayNameFor(
                                user.uid,
                                ParticipantInfo(
                                    uid = user.uid,
                                    username = user.username,
                                    displayName = user.displayName,
                                    photoUrl = user.photoUrl
                                )
                            )
                        }
                        val isSelected = selectedMembers.containsKey(user.uid)
                        CreateGroupMemberRow(
                            user = user,
                            displayName = resolvedName,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            cardColor = cardColor,
                            testTag = "create_group_search_result_${user.uid}",
                            onToggle = { toggleMember(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupMemberRow(
    user: ChatUser,
    displayName: String,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    subTextColor: Color,
    cardColor: Color,
    testTag: String,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        if (!user.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.photoUrl,
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
                    .background(accentColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.trim().take(1).uppercase().ifEmpty { "M" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.username.isNotBlank()) {
                Text(
                    text = "@${user.username}",
                    fontSize = 12.sp,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                uncheckedColor = subTextColor
            )
        )
    }
}
