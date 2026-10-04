package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.model.ChatMessage
import com.example.chat.model.ParticipantInfo
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

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
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid ?: ""
    val recipient by viewModel.activeRecipient.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()

    val listState = rememberLazyListState()
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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
                Text(
                    text = "Baagbaan Boi Verified Member.\nDirect encrypted messaging active.",
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
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
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("conversation_messages_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isOwnMessage = msg.senderId == currentUid
                MessageBubble(
                    message = msg,
                    isOwn = isOwnMessage,
                    accentColor = accentColor,
                    isDark = isDark
                )
            }
        }

        // Bottom Message Composer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0x331E293B) else Color(0x77FFFFFF),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
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
                                viewModel.sendMessage()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank() && !isSending) {
                            viewModel.sendMessage()
                        }
                    },
                    enabled = messageInput.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (messageInput.isNotBlank() && !isSending) accentColor else accentColor.copy(alpha = 0.3f)
                        )
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
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color,
    isDark: Boolean
) {
    val bubbleColor = if (isOwn) {
        accentColor
    } else {
        if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
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

    val bubbleShape = if (isOwn) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
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
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            tonalElevation = if (isOwn) 2.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(bubbleShape)
                .testTag("message_bubble_${message.id}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                if (timeFormatted.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = timeFormatted,
                            color = timeColor,
                            fontSize = 10.sp
                        )
                        if (isOwn) {
                            MessageStatusTicks(status = message.status)
                        }
                    }
                }
            }
        }
    }
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
