package com.example.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat.data.ChatRepository
import com.example.chat.model.formatPrivacyVisibilityLabel
import com.example.chat.model.formatStatusPrivacyLabel
import com.example.ui.components.isAppInDarkMode
import com.kyant.backdrop.Backdrop

@Composable
fun ChatSettingsScreen(
    viewModel: ChatViewModel,
    onBackToApp: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    privacyOpen: Boolean = false,
    onPrivacyOpenChange: (Boolean) -> Unit = {},
    privacyTopPadding: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val contentColor = if (isDark) Color.White else Color(0xFF0F172A)
    val screenBackground = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val currentUser by viewModel.currentUser.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val showOnlineStatus by viewModel.showOnlineStatus.collectAsState()
    val readReceipts by viewModel.readReceipts.collectAsState()
    val blockedContacts by viewModel.blockedContacts.collectAsState()
    val directContacts by viewModel.directContacts.collectAsState()
    val nicknames by viewModel.nicknames.collectAsState()
    val isSubmitting by viewModel.isSubmittingUsername.collectAsState()
    val serverError by viewModel.usernameError.collectAsState()

    BackHandler(enabled = privacyOpen) {
        onPrivacyOpenChange(false)
    }

    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var newUsernameInput by remember { mutableStateOf("") }
    val cleanedNewUsername = remember(newUsernameInput) { newUsernameInput.trim().lowercase() }
    val isValidNew = remember(cleanedNewUsername) { ChatRepository.isValidUsername(cleanedNewUsername) }

    var showEditAboutDialog by remember { mutableStateOf(false) }
    var aboutDraft by remember { mutableStateOf("") }

    var activePrivacyDialog by remember { mutableStateOf<String?>(null) } // "lastSeen" | "photo" | "about" | "status" | "groups"
    var statusModeDraft by remember { mutableStateOf("contacts") }
    var statusSelectedIdsDraft by remember { mutableStateOf<Set<String>>(emptySet()) }

    if (showEditAboutDialog) {
        AlertDialog(
            onDismissRequest = { showEditAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Write a short bio or status line (max 139 characters).",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aboutDraft,
                        onValueChange = { input ->
                            if (input.length <= 139) {
                                aboutDraft = input
                            } else {
                                aboutDraft = input.take(139)
                            }
                        },
                        placeholder = { Text("Hey there! I am using Baagbaan.") },
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_about_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${aboutDraft.length}/139",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAbout(aboutDraft) {
                            showEditAboutDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.testTag("save_about_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditAboutDialog = false },
                    modifier = Modifier.testTag("cancel_about_button")
                ) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        )
    }

    when (activePrivacyDialog) {
        "lastSeen" -> {
            val currentVal = (currentUser?.lastSeenVisibility ?: if (showOnlineStatus) "everyone" else "nobody")
                .trim().lowercase()
            PrivacyOptionsDialog(
                title = "Last seen and online",
                options = listOf(
                    "everyone" to "Everyone",
                    "contacts" to "My contacts",
                    "nobody" to "Nobody"
                ),
                selectedValue = if (currentVal in setOf("everyone", "contacts", "nobody")) currentVal else "everyone",
                accentColor = accentColor,
                isDark = isDark,
                onSelect = { selected ->
                    viewModel.setLastSeenVisibility(selected)
                    activePrivacyDialog = null
                },
                onDismiss = { activePrivacyDialog = null }
            )
        }
        "photo" -> {
            val currentVal = (currentUser?.photoVisibility ?: "everyone").trim().lowercase()
            PrivacyOptionsDialog(
                title = "Profile photo",
                options = listOf(
                    "everyone" to "Everyone",
                    "contacts" to "My contacts",
                    "nobody" to "Nobody"
                ),
                selectedValue = if (currentVal in setOf("everyone", "contacts", "nobody")) currentVal else "everyone",
                accentColor = accentColor,
                isDark = isDark,
                onSelect = { selected ->
                    viewModel.setPhotoVisibility(selected)
                    activePrivacyDialog = null
                },
                onDismiss = { activePrivacyDialog = null }
            )
        }
        "about" -> {
            val currentVal = (currentUser?.aboutVisibility ?: "everyone").trim().lowercase()
            PrivacyOptionsDialog(
                title = "About",
                options = listOf(
                    "everyone" to "Everyone",
                    "contacts" to "My contacts",
                    "nobody" to "Nobody"
                ),
                selectedValue = if (currentVal in setOf("everyone", "contacts", "nobody")) currentVal else "everyone",
                accentColor = accentColor,
                isDark = isDark,
                onSelect = { selected ->
                    viewModel.setAboutVisibility(selected)
                    activePrivacyDialog = null
                },
                onDismiss = { activePrivacyDialog = null }
            )
        }
        "groups" -> {
            val currentVal = (currentUser?.groupsVisibility ?: "everyone").trim().lowercase()
            PrivacyOptionsDialog(
                title = "Groups",
                options = listOf(
                    "everyone" to "Everyone",
                    "contacts" to "My contacts"
                ),
                selectedValue = if (currentVal in setOf("everyone", "contacts")) currentVal else "everyone",
                accentColor = accentColor,
                isDark = isDark,
                onSelect = { selected ->
                    viewModel.setGroupsVisibility(selected)
                    activePrivacyDialog = null
                },
                onDismiss = { activePrivacyDialog = null }
            )
        }
        "status" -> {
            AlertDialog(
                onDismissRequest = { activePrivacyDialog = null },
                title = {
                    Text("Status privacy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Who can see my status updates",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val statusOptions = listOf(
                            "contacts" to "My contacts",
                            "except" to "My contacts except...",
                            "only" to "Only share with..."
                        )
                        statusOptions.forEach { (value, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val prev = statusModeDraft
                                        statusModeDraft = value
                                        if (prev != value) {
                                            statusSelectedIdsDraft = when (value) {
                                                "except" -> currentUser?.statusExceptIds.orEmpty().toSet()
                                                "only" -> currentUser?.statusOnlyIds.orEmpty().toSet()
                                                else -> emptySet()
                                            }
                                        }
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("status_privacy_option_$value")
                            ) {
                                RadioButton(
                                    selected = statusModeDraft == value,
                                    onClick = {
                                        val prev = statusModeDraft
                                        statusModeDraft = value
                                        if (prev != value) {
                                            statusSelectedIdsDraft = when (value) {
                                                "except" -> currentUser?.statusExceptIds.orEmpty().toSet()
                                                "only" -> currentUser?.statusOnlyIds.orEmpty().toSet()
                                                else -> emptySet()
                                            }
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }

                        if (statusModeDraft == "except" || statusModeDraft == "only") {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = if (isDark) Color(0x3394A3B8) else Color(0x220F172A))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (statusModeDraft == "except") {
                                    "Hide status from selected contacts:"
                                } else {
                                    "Only share status with selected contacts:"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (directContacts.isEmpty()) {
                                Text(
                                    text = "No direct contacts yet.",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                ) {
                                    directContacts.forEach { contact ->
                                        val contactName = remember(contact, nicknames) {
                                            viewModel.displayNameFor(contact.uid, contact)
                                        }
                                        val checked = contact.uid in statusSelectedIdsDraft
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    statusSelectedIdsDraft = if (checked) {
                                                        statusSelectedIdsDraft - contact.uid
                                                    } else {
                                                        statusSelectedIdsDraft + contact.uid
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("status_privacy_contact_${contact.uid}")
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { isChecked ->
                                                    statusSelectedIdsDraft = if (isChecked) {
                                                        statusSelectedIdsDraft + contact.uid
                                                    } else {
                                                        statusSelectedIdsDraft - contact.uid
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = contactName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (contact.username.isNotBlank()) {
                                                    Text(
                                                        text = "@${contact.username}",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val exceptIds = if (statusModeDraft == "except") {
                                statusSelectedIdsDraft.toList()
                            } else {
                                currentUser?.statusExceptIds.orEmpty()
                            }
                            val onlyIds = if (statusModeDraft == "only") {
                                statusSelectedIdsDraft.toList()
                            } else {
                                currentUser?.statusOnlyIds.orEmpty()
                            }
                            viewModel.setStatusPrivacy(
                                visibility = statusModeDraft,
                                exceptIds = exceptIds,
                                onlyIds = onlyIds
                            )
                            activePrivacyDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.testTag("save_status_privacy_button")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activePrivacyDialog = null }) {
                        Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }
                }
            )
        }
    }

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
                        text = "Enter a new unique handle (3-20 lowercase letters, numbers, or underscores).",
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        if (privacyOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = privacyTopPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val lastSeenValueLabel = formatPrivacyVisibilityLabel(
                    currentUser?.lastSeenVisibility ?: if (showOnlineStatus) "everyone" else "nobody"
                )
                val photoValueLabel = formatPrivacyVisibilityLabel(currentUser?.photoVisibility ?: "everyone")
                val aboutValueLabel = formatPrivacyVisibilityLabel(currentUser?.aboutVisibility ?: "everyone")
                val statusValueLabel = formatStatusPrivacyLabel(currentUser?.statusVisibility ?: "contacts")
                val groupsValueLabel = formatPrivacyVisibilityLabel(currentUser?.groupsVisibility ?: "everyone")

                PrivacySelectionRowCard(
                    icon = Icons.Default.Visibility,
                    title = "Last seen and online",
                    valueLabel = lastSeenValueLabel,
                    isDark = isDark,
                    contentColor = contentColor,
                    testTag = "privacy_row_last_seen",
                    onClick = { activePrivacyDialog = "lastSeen" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrivacySelectionRowCard(
                    icon = Icons.Default.AccountCircle,
                    title = "Profile photo",
                    valueLabel = photoValueLabel,
                    isDark = isDark,
                    contentColor = contentColor,
                    testTag = "privacy_row_profile_photo",
                    onClick = { activePrivacyDialog = "photo" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrivacySelectionRowCard(
                    icon = Icons.Default.Info,
                    title = "About",
                    valueLabel = aboutValueLabel,
                    isDark = isDark,
                    contentColor = contentColor,
                    testTag = "privacy_row_about",
                    onClick = { activePrivacyDialog = "about" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrivacySelectionRowCard(
                    icon = Icons.Default.Update,
                    title = "Status",
                    valueLabel = statusValueLabel,
                    isDark = isDark,
                    contentColor = contentColor,
                    testTag = "privacy_row_status",
                    onClick = {
                        val mode = (currentUser?.statusVisibility ?: "contacts").trim().lowercase()
                            .let { if (it in setOf("contacts", "except", "only")) it else "contacts" }
                        statusModeDraft = mode
                        statusSelectedIdsDraft = when (mode) {
                            "except" -> currentUser?.statusExceptIds.orEmpty().toSet()
                            "only" -> currentUser?.statusOnlyIds.orEmpty().toSet()
                            else -> emptySet()
                        }
                        activePrivacyDialog = "status"
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrivacySelectionRowCard(
                    icon = Icons.Default.Group,
                    title = "Groups",
                    valueLabel = groupsValueLabel,
                    isDark = isDark,
                    contentColor = contentColor,
                    testTag = "privacy_row_groups",
                    onClick = { activePrivacyDialog = "groups" }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Read receipts toggle
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
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
                                .background(contentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read receipts",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = contentColor
                            )
                            Text(
                                text = "If off, others will not see your blue ticks, and you will not see theirs.",
                                fontSize = 12.sp,
                                color = contentColor.copy(alpha = 0.65f),
                                lineHeight = 16.sp
                            )
                        }

                        Switch(
                            checked = readReceipts,
                            onCheckedChange = { viewModel.setReadReceipts(it) },
                            modifier = Modifier.testTag("read_receipts_toggle_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                BlockedContactsCard(
                    blockedContacts = blockedContacts,
                    nicknames = nicknames,
                    viewModel = viewModel,
                    isDark = isDark,
                    contentColor = contentColor,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.navigationBarsPadding().height(110.dp))
            }
        } else {
            Column(
                modifier = Modifier
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
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            tonalElevation = 0.dp,
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

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color(0x3394A3B8) else Color(0x220F172A))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            aboutDraft = currentUser?.about.orEmpty()
                            showEditAboutDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("profile_about_row")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        val aboutText = currentUser?.about?.trim().orEmpty()
                        Text(
                            text = aboutText.ifEmpty { "Hey there! I am using Baagbaan." },
                            fontSize = 14.sp,
                            color = if (aboutText.isEmpty()) {
                                if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (isDark) Color.White else Color(0xFF0F172A)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit About",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Notifications Section
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
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
                    modifier = Modifier.testTag("notifications_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Row
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onPrivacyOpenChange(true) }
                .testTag("privacy_settings_row")
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
                        .background(contentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = contentColor
                    )
                    Text(
                        text = "Online status, read receipts, blocked contacts",
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.65f),
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Privacy",
                    tint = contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BlockedContactsCard(
            blockedContacts = blockedContacts,
            nicknames = nicknames,
            viewModel = viewModel,
            isDark = isDark,
            contentColor = contentColor,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Security Info Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            tonalElevation = 0.dp,
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

        Spacer(modifier = Modifier.navigationBarsPadding().height(110.dp))
            }
        }
    }
}

@Composable
private fun PrivacySelectionRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueLabel: String,
    isDark: Boolean,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
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
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = valueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PrivacyOptionsDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    accentColor: Color,
    isDark: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 6.dp)
                            .testTag("privacy_option_$value")
                    ) {
                        RadioButton(
                            selected = selectedValue == value,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun BlockedContactsCard(
    blockedContacts: List<com.example.chat.model.ParticipantInfo>,
    nicknames: Map<String, String>,
    viewModel: ChatViewModel,
    isDark: Boolean,
    contentColor: Color,
    accentColor: Color
) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("blocked_contacts_section")
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
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blocked contacts",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = contentColor
                    )
                    Text(
                        text = if (blockedContacts.isEmpty()) {
                            "No blocked contacts"
                        } else {
                            "${blockedContacts.size} blocked"
                        },
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.65f)
                    )
                }
            }
        }

        if (blockedContacts.isNotEmpty()) {
            blockedContacts.forEach { contact ->
                Spacer(modifier = Modifier.height(8.dp))
                val resolvedName = remember(contact, nicknames) {
                    viewModel.displayNameFor(contact.uid, contact)
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("blocked_contact_row_${contact.uid}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (!contact.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = contact.photoUrl,
                                contentDescription = resolvedName,
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
                                    text = resolvedName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = contentColor
                            )
                            if (contact.username.isNotBlank()) {
                                Text(
                                    text = "@${contact.username}",
                                    fontSize = 12.sp,
                                    color = contentColor.copy(alpha = 0.65f)
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.unblockUser(contact.uid) },
                            modifier = Modifier.testTag("settings_unblock_button_${contact.uid}")
                        ) {
                            Text(
                                text = "Unblock",
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun PrivacyTopHeaderOverlay(
    onBack: () -> Unit,
    backdrop: Backdrop?,
    onHeaderHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val contentColor = if (isDark) Color.White else Color(0xFF0F172A)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .onSizeChanged { onHeaderHeightChanged(it.height) }
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onBack)
                .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 8.dp)
                .testTag("privacy_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Privacy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

