package com.example.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.data.ChatRepository
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode

@Composable
fun ChatSettingsScreen(
    viewModel: ChatViewModel,
    onBackToApp: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val currentUser by viewModel.currentUser.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val isSubmitting by viewModel.isSubmittingUsername.collectAsState()
    val serverError by viewModel.usernameError.collectAsState()

    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var newUsernameInput by remember { mutableStateOf("") }
    val cleanedNewUsername = remember(newUsernameInput) { newUsernameInput.trim().lowercase() }
    val isValidNew = remember(cleanedNewUsername) { ChatRepository.isValidUsername(cleanedNewUsername) }

    if (showChangeUsernameDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    showChangeUsernameDialog = false
                    viewModel.clearUsernameError()
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Username", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a new unique handle (3–20 lowercase letters, numbers, or underscores).",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newUsernameInput,
                        onValueChange = { input ->
                            val filtered = input.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
                            if (filtered.length <= 20) {
                                newUsernameInput = filtered
                                viewModel.clearUsernameError()
                            }
                        },
                        leadingIcon = {
                            Text("@", fontWeight = FontWeight.Bold, color = accentColor)
                        },
                        trailingIcon = {
                            if (cleanedNewUsername.isNotEmpty()) {
                                Icon(
                                    imageVector = if (isValidNew) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isValidNew) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValidNew && !isSubmitting) {
                                    viewModel.changeUsername(cleanedNewUsername) { success ->
                                        if (success) showChangeUsernameDialog = false
                                    }
                                }
                            }
                        )
                    )

                    AnimatedVisibility(
                        visible = serverError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = serverError ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isValidNew && !isSubmitting) {
                            viewModel.changeUsername(cleanedNewUsername) { success ->
                                if (success) showChangeUsernameDialog = false
                            }
                        }
                    },
                    enabled = isValidNew && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeUsernameDialog = false
                        viewModel.clearUsernameError()
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBackToApp,
                modifier = Modifier.testTag("chat_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Chat Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0x331E293B) else Color(0x77FFFFFF),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (!currentUser?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentUser?.photoUrl,
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.displayName?.ifBlank { currentUser?.username } ?: "U").take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.displayName?.ifBlank { currentUser?.username } ?: "User",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "@${currentUser?.username ?: ""}",
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        newUsernameInput = currentUser?.username ?: ""
                        showChangeUsernameDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.testTag("change_username_button")
                ) {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Username")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Notifications Section
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0x331E293B) else Color(0x77FFFFFF),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Message Notifications",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Show in-app banners for new messages",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor
                    ),
                    modifier = Modifier.testTag("notifications_toggle_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Info Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0x331E293B) else Color(0x77FFFFFF),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Direct Messaging Security",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Firestore rules protect all direct chats. Only you and the recipient can read messages.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
