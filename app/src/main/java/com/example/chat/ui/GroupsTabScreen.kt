package com.example.chat.ui


import com.example.ui.components.ThemedIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.model.ChatSummary
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInDarkMode
import com.kyant.backdrop.Backdrop

@Composable
fun GroupsTabScreen(
    viewModel: ChatViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    topPadding: Dp = 76.dp,
    onOpenCreateGroup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val contentColor = glassContentColor()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid?.ifBlank { null }
        ?: com.example.util.SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
    val groupChats by viewModel.groupChats.collectAsState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        if (groupChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = topPadding, bottom = 120.dp + navBarBottom)
                    .padding(horizontal = 32.dp)
                    .testTag("groups_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No groups yet. Create one to chat with several people together.",
                    fontSize = 15.sp,
                    color = contentColor.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("groups_list_column"),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = 120.dp + navBarBottom
                )
            ) {
                items(groupChats, key = { it.chatId }) { group ->
                    GroupListRow(
                        chat = group,
                        currentUid = currentUid,
                        currentUserDisplayName = currentUser?.displayName?.ifBlank { currentUser?.username }.orEmpty(),
                        accentColor = accentColor,
                        contentColor = contentColor,
                        isDark = isDark,
                        onClick = { viewModel.openChatFromSummary(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupListRow(
    chat: ChatSummary,
    currentUid: String,
    currentUserDisplayName: String,
    accentColor: Color,
    contentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val groupTitle = remember(chat.name, chat.type, currentUid) {
        chat.displayTitle(currentUid)
    }
    val lastMsg = chat.lastMessage
    val subtitle = remember(lastMsg, chat.participantInfo, currentUid, currentUserDisplayName) {
        if (lastMsg == null || lastMsg.isDeleted || (currentUid.isNotEmpty() && currentUid in lastMsg.deletedFor) || lastMsg.text.isBlank()) {
            "No messages yet"
        } else {
            val senderInfo = chat.participantInfo[lastMsg.senderId]
            val senderName = senderInfo?.displayName?.trim()?.ifBlank { null }
                ?: senderInfo?.username?.trim()?.ifBlank { null }
                ?: if (lastMsg.senderId == currentUid && currentUserDisplayName.isNotBlank()) {
                    currentUserDisplayName
                } else {
                    "Member"
                }
            "$senderName: ${lastMsg.text}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("group_item_${chat.chatId}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = groupTitle.trim().take(1).uppercase().ifEmpty { "G" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = groupTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            thickness = 0.6.dp,
            color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
        )
    }
}

@Composable
internal fun GroupsTopHeaderOverlay(
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
        // Title row: icon + title, no glass
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(0.dp)
        ) {
            ThemedIcon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Groups",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .chatHeaderGlass(backdrop, CircleShape)
                .clip(CircleShape)
                .clickable { viewModel.openSettings() }
                .testTag("groups_profile_button"),
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
                ThemedIcon(
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
internal fun GroupsFloatingCreateButton(
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
            .testTag("groups_fab_button"),
        contentAlignment = Alignment.Center
    ) {
        ThemedIcon(
            imageVector = Icons.Default.Add,
            contentDescription = "New group",
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}
