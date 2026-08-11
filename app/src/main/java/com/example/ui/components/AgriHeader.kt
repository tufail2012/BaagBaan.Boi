package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.collectAsState
import com.example.data.FirestoreSyncManager
import com.example.data.SyncState
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.ui.AppThemeMode

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun AgriHeader(
    title: String,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    selectedColorHex: String = "#D32F2F",
    onSelectColorHex: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToContactDirectory: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToGardenPlanning: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    currentUserEmail: String? = null,
    currentUserPhotoUrl: String? = null,
    onLogout: () -> Unit = {},
    onManualSync: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authUser = remember(currentUserEmail) { com.example.util.SafeFirebase.getAuth(context)?.currentUser }
    val effectivePhotoUrl = currentUserPhotoUrl ?: authUser?.photoUrl?.toString()

    var menuExpanded by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSyncDetailsDialog by remember { mutableStateOf(false) }

    val syncState by FirestoreSyncManager.syncState.collectAsState()
    val lastSyncedTime by FirestoreSyncManager.lastSyncedTime.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val isDark = isAppInDarkMode()
    val focusRequester = remember { FocusRequester() }

    val isAttendanceScreen = title.equals("Worker Attendance", ignoreCase = true) || title.equals("Attendance", ignoreCase = true)
    val activeSearchMode = (isSearchActive || searchQuery.isNotEmpty()) && !isAttendanceScreen

    LaunchedEffect(isSearchActive) {
        if (isSearchActive && !isAttendanceScreen) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = if (isAttendanceScreen) 12.dp else 16.dp,
                vertical = 6.dp
            ),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else Color.Transparent
        ),
        tonalElevation = 2.dp,
        shape = if (isAttendanceScreen) RoundedCornerShape(12.dp) else CircleShape
    ) {
        if (activeSearchMode) {
            // Global Search Bar active in Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        onSearchActiveChange(false)
                        onSearchQueryChange("")
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Close Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search farmer name, serial no., contact no...",
                            fontSize = 12.sp,
                            color = if (isDark) Color.Gray else Color.DarkGray
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = if (isDark) Color.White else Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD),
                        focusedContainerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF8F9FA),
                        unfocusedContainerColor = if (isDark) Color(0xFF181818) else Color(0xFFF1F3F5),
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .focusRequester(focusRequester)
                        .testTag("global_header_search_input")
                )

                // Profile Avatar menu button inside search bar header
                Box {
                    HeaderProfileAvatar(
                        photoUrl = effectivePhotoUrl,
                        currentUserEmail = currentUserEmail,
                        isDark = isDark,
                        onClick = { menuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        OverflowMenuContent(
                            themeMode = themeMode,
                            currentUserEmail = currentUserEmail,
                            tagSuffix = "",
                            onDismiss = { menuExpanded = false },
                            onNavigateToDashboard = onNavigateToDashboard,
                            onNavigateToInventory = onNavigateToInventory,
                            onNavigateToAttendance = onNavigateToAttendance,
                            onNavigateToContactDirectory = onNavigateToContactDirectory,
                            onNavigateToBackupRestore = onNavigateToBackupRestore,
                            onNavigateToLogin = onNavigateToLogin,
                            onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                            onLogout = onLogout,
                            onOpenThemeDialog = { showThemeDialog = true },
                            onOpenRecycleBin = onOpenRecycleBin
                        )
                    }
                }
            }
        } else {
            // Standard Header with Title and Search Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left App Logo Badge & Title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBack != null || isAttendanceScreen) {
                        IconButton(
                            onClick = { onBack?.invoke() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val headerBadgeIcon = when (title.lowercase()) {
                        "imported", "imported plants" -> Icons.Default.LocalShipping
                        "rootstocks", "imported rootstocks" -> Icons.Default.Spa
                        "site visit" -> Icons.Outlined.Assignment
                        "pruning" -> Icons.Default.ContentCut
                        "bookings" -> Icons.Default.PlaylistAddCheck
                        "garden planning", "garden" -> Icons.Default.Park
                        "attendance", "worker attendance" -> Icons.Default.CalendarToday
                        else -> Icons.Outlined.LocalFlorist
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerBadgeIcon,
                            contentDescription = "$title Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isAttendanceScreen) {
                    // Right Action Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("header_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification Center",
                                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onSearchActiveChange(true)
                                onToggleSearch()
                            },
                            modifier = Modifier.testTag("header_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Records",
                                tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Profile Avatar Menu containing Theme & Account options
                        Box {
                            HeaderProfileAvatar(
                                photoUrl = effectivePhotoUrl,
                                currentUserEmail = currentUserEmail,
                                isDark = isDark,
                                onClick = { menuExpanded = true }
                            )

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                OverflowMenuContent(
                                    themeMode = themeMode,
                                    currentUserEmail = currentUserEmail,
                                    tagSuffix = "_2",
                                    onDismiss = { menuExpanded = false },
                                    onNavigateToDashboard = onNavigateToDashboard,
                                    onNavigateToInventory = onNavigateToInventory,
                                    onNavigateToAttendance = onNavigateToAttendance,
                                    onNavigateToContactDirectory = onNavigateToContactDirectory,
                                    onNavigateToBackupRestore = onNavigateToBackupRestore,
                                    onNavigateToLogin = onNavigateToLogin,
                                    onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                                    onLogout = onLogout,
                                    onOpenThemeDialog = { showThemeDialog = true },
                                    onOpenRecycleBin = onOpenRecycleBin
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeColoursDialog(
            themeMode = themeMode,
            selectedColorHex = selectedColorHex,
            onSelectThemeMode = onSelectThemeMode,
            onSelectColorHex = onSelectColorHex,
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showSyncDetailsDialog) {
        SyncStatusDialog(
            syncState = syncState,
            lastSyncedTime = lastSyncedTime,
            onDismiss = { showSyncDetailsDialog = false },
            onManualSync = {
                onManualSync?.invoke()
            }
        )
    }
}

@Composable
private fun SyncStatusDialog(
    syncState: SyncState,
    lastSyncedTime: Long,
    onDismiss: () -> Unit,
    onManualSync: () -> Unit
) {
    val isDark = isAppInDarkMode()
    val formattedTime = remember(lastSyncedTime) {
        if (lastSyncedTime == 0L) "Not synced yet"
        else {
            val diffMs = System.currentTimeMillis() - lastSyncedTime
            if (diffMs < 30_000) "Just now"
            else if (diffMs < 60_000) "Less than a minute ago"
            else {
                val diffMins = diffMs / 60_000
                if (diffMins < 60) "$diffMins min ago"
                else {
                    val sdf = java.text.SimpleDateFormat("MMM d, hh:mm a", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(lastSyncedTime))
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (syncState) {
                                SyncState.SYNCED -> Color(0xFFE8F5E9)
                                SyncState.SYNCING -> Color(0xFFE1F5FE)
                                SyncState.OFFLINE -> Color(0xFFFFF3E0)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (syncState) {
                            SyncState.SYNCED -> Icons.Default.CloudDone
                            SyncState.SYNCING -> Icons.Default.CloudSync
                            SyncState.OFFLINE -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        tint = when (syncState) {
                            SyncState.SYNCED -> Color(0xFF2E7D32)
                            SyncState.SYNCING -> Color(0xFF0288D1)
                            SyncState.OFFLINE -> Color(0xFFE65100)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Cloud Sync Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (syncState) {
                        SyncState.SYNCED -> Color(0xFFE8F5E9)
                        SyncState.SYNCING -> Color(0xFFE1F5FE)
                        SyncState.OFFLINE -> Color(0xFFFFF3E0)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Real-Time Sync",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (syncState) {
                                    SyncState.SYNCED -> "Firestore Live & Synced"
                                    SyncState.SYNCING -> "Synchronizing with Cloud..."
                                    SyncState.OFFLINE -> "Offline / Local Mode"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (syncState) {
                                    SyncState.SYNCED -> Color(0xFF2E7D32)
                                    SyncState.SYNCING -> Color(0xFF0288D1)
                                    SyncState.OFFLINE -> Color(0xFFE65100)
                                }
                            )
                        }

                        if (syncState == SyncState.SYNCED) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Synced:",
                        fontSize = 13.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                Text(
                    text = "All crop bookings, worker records, attendance logs, and financial advance payments are saved locally and automatically synced with Google Firebase Firestore.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isDark) Color.Gray else Color.DarkGray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onManualSync()
                    onDismiss()
                },
                enabled = syncState != SyncState.SYNCING
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sync Now",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun OverflowMenuContent(
    themeMode: AppThemeMode,
    currentUserEmail: String?,
    tagSuffix: String = "",
    onDismiss: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToContactDirectory: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToGardenPlanning: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    // === GROUP 1: Main (no header) ===
    // 1. Dashboard
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToDashboard()
        },
        modifier = Modifier.testTag("dashboard_menu_item$tagSuffix")
    )

    // 2. Inventory
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Inventory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToInventory()
        },
        modifier = Modifier.testTag("inventory_menu_item$tagSuffix")
    )

    // 3. Attendance
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Attendance",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToAttendance()
        },
        modifier = Modifier.testTag("attendance_menu_item$tagSuffix")
    )

    // === DIVIDER 1 ===
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    // === GROUP 2: PROFILE & DATA ===
    Text(
        text = "PROFILE & DATA",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
    )

    // 4. Contact Directory
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Contact Directory",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToContactDirectory()
        },
        modifier = Modifier.testTag("contact_directory_menu_item$tagSuffix")
    )

    // 5. Data Backup & Restore
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Data Backup & Restore",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToBackupRestore()
        },
        modifier = Modifier.testTag("backup_restore_menu_item$tagSuffix")
    )

    // 6. Accounts
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Accounts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (currentUserEmail != null) {
                        Text(
                            text = currentUserEmail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        onClick = {
            onDismiss()
            onNavigateToLogin()
        },
        modifier = Modifier.testTag("accounts_menu_item$tagSuffix")
    )

    // === DIVIDER 2 ===
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    // === GROUP 3: Bottom group (no header) ===
    // 7. Recycle Bin
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Recycle Bin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        onClick = {
            onDismiss()
            onOpenRecycleBin()
        },
        modifier = Modifier.testTag("recycle_bin_menu_item$tagSuffix")
    )

    // 8. Theme Preferences
    val currentModeLabel = when (themeMode) {
        AppThemeMode.SYSTEM -> "System"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.DARK -> "Dark"
        AppThemeMode.AMOLED -> "AMOLED"
    }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Theme Preferences",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "($currentModeLabel)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        onClick = {
            onDismiss()
            onOpenThemeDialog()
        },
        modifier = Modifier.testTag("theme_preference_menu_item$tagSuffix")
    )

    // 9. Logout
    if (currentUserEmail != null) {
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = {
                onDismiss()
                onLogout()
            },
            modifier = Modifier.testTag("logout_menu_item$tagSuffix")
        )
    }
}

@Composable
private fun HeaderProfileAvatar(
    photoUrl: String?,
    currentUserEmail: String?,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(38.dp)
            .testTag("overflow_menu_button")
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile Options",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        } else if (!currentUserEmail.isNullOrBlank()) {
            val initial = currentUserEmail.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

