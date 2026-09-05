package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

import androidx.compose.material.icons.filled.AccountCircle
import com.example.ui.theme.getSectionAccentColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun AgriHeader(
    title: String,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    accentColor: Color? = null,
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
    onNavigateToPaymentReminders: () -> Unit = {},
    onNavigateToSeasonalReminders: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    onOpenThemeDialog: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToGardenPlanning: () -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToBusinessInfo: () -> Unit = {},
    onNavigateToMessageTemplates: () -> Unit = {},
    onNavigateToQrScanner: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    currentUserEmail: String? = null,
    currentUserPhotoUrl: String? = null,
    onLogout: () -> Unit = {},
    onManualSync: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authUser = remember(currentUserEmail) { com.example.util.SafeFirebase.getAuth(context)?.currentUser }
    val effectivePhotoUrl = currentUserPhotoUrl ?: authUser?.photoUrl?.toString()

    var menuExpanded by remember { mutableStateOf(false) }
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

    val parsedPaletteColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            null
        }
    }
    val sectionAccent = accentColor ?: parsedPaletteColor ?: MaterialTheme.colorScheme.primary
    val animatedAccentColor by animateColorAsState(
        targetValue = sectionAccent,
        animationSpec = tween(durationMillis = 280),
        label = "HeaderAccentColor"
    )

    val isAttendanceScreen = title.equals("Worker Attendance", ignoreCase = true) || title.equals("Attendance", ignoreCase = true)
    val activeSearchMode = (isSearchActive || searchQuery.isNotEmpty()) && !isAttendanceScreen

    LaunchedEffect(isSearchActive) {
        if (isSearchActive && !isAttendanceScreen) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val headerShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = 16.dp,
                vertical = 6.dp
            )
            .frostedGlassChrome(
                isDark = isDark,
                accentColor = animatedAccentColor,
                shape = headerShape
            )
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
                        tint = animatedAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { onSearchQueryChange(capitalizeWordsNaturally(it)) },
                    placeholder = {
                        Text(
                            "Search farmer name, serial no., contact no...",
                            fontSize = 12.sp,
                            color = if (isDark) Color.Gray else Color.DarkGray
                        )
                    },
                    keyboardOptions = AppDefaultWordKeyboardOptions,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = animatedAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = if (isDark) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            VoiceSearchIconButton(
                                onQueryChange = { recognizedText ->
                                    onSearchQueryChange(capitalizeWordsNaturally(recognizedText))
                                },
                                accentColor = animatedAccentColor,
                                isDark = isDark,
                                buttonSize = 34.dp,
                                iconSize = 18.dp,
                                testTag = "global_header_voice_search_button"
                            )
                        }
                    },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedAccentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedContainerColor = when {
                            themeMode == AppThemeMode.AMOLED -> Color(0xFF000000)
                            isDark -> Color(0xFF1E293B)
                            else -> Color(0xFFFFFFFF)
                        },
                        unfocusedContainerColor = when {
                            themeMode == AppThemeMode.AMOLED -> Color(0xFF000000)
                            isDark -> Color(0xFF1E293B)
                            else -> Color(0xFFFFFFFF)
                        },
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
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
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 250.dp, max = 310.dp)
                            .padding(vertical = 4.dp)
                            .frostedLiquidGlassMenuBackground(
                                hazeState = hazeState,
                                isDark = isDark,
                                themeMode = themeMode,
                                accentColor = animatedAccentColor,
                                shape = RoundedCornerShape(22.dp)
                            ),
                        shape = RoundedCornerShape(22.dp),
                        containerColor = Color.Transparent,
                        border = null,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        OverflowMenuContent(
                            themeMode = themeMode,
                            currentUserEmail = currentUserEmail,
                            tagSuffix = "",
                            parsedPaletteColor = parsedPaletteColor,
                            hazeState = hazeState,
                            onDismiss = { menuExpanded = false },
                            onNavigateToDashboard = onNavigateToDashboard,
                            onNavigateToInventory = onNavigateToInventory,
                            onNavigateToAttendance = onNavigateToAttendance,
                            onNavigateToContactDirectory = onNavigateToContactDirectory,
                            onNavigateToPaymentReminders = onNavigateToPaymentReminders,
                            onNavigateToSeasonalReminders = onNavigateToSeasonalReminders,
                            onNavigateToBackupRestore = onNavigateToBackupRestore,
                            onNavigateToLogin = onNavigateToLogin,
                            onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToBusinessInfo = onNavigateToBusinessInfo,
                            onNavigateToMessageTemplates = onNavigateToMessageTemplates,
                            onNavigateToQrScanner = onNavigateToQrScanner,
                            onLogout = onLogout,
                            onOpenThemeDialog = onOpenThemeDialog,
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

                    val isAppLogo = when (title.lowercase()) {
                        "agricrop", "baagbaan boi", "home", "dashboard" -> true
                        "local", "local plants", "imported", "imported plants", "rootstocks", "imported rootstocks", "site visit", "pruning", "bookings", "garden planning", "garden", "attendance", "worker attendance" -> false
                        else -> false
                    }

                    val headerBadgeIcon = when (title.lowercase()) {
                        "local", "local plants" -> Icons.Outlined.LocalFlorist
                        "imported", "imported plants" -> Icons.Default.LocalShipping
                        "rootstocks", "imported rootstocks" -> Icons.Default.Spa
                        "site visit" -> Icons.Outlined.Assignment
                        "pruning" -> Icons.Default.ContentCut
                        "bookings" -> Icons.Default.PlaylistAddCheck
                        "garden planning", "garden" -> Icons.Default.Park
                        "attendance", "worker attendance" -> Icons.Default.CalendarToday
                        else -> null
                    }

                    if (isAppLogo || headerBadgeIcon == null) {
                        AppBrandLogo(
                            size = 38.dp,
                            shape = RoundedCornerShape(10.dp),
                            elevation = 1.dp,
                            contentDescription = "App Logo"
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(animatedAccentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = headerBadgeIcon,
                                contentDescription = "$title Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                                            containerColor = animatedAccentColor,
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
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 310.dp)
                                    .padding(vertical = 4.dp)
                                    .frostedLiquidGlassMenuBackground(
                                        hazeState = hazeState,
                                        isDark = isDark,
                                        themeMode = themeMode,
                                        accentColor = animatedAccentColor,
                                        shape = RoundedCornerShape(22.dp)
                                    ),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = Color.Transparent,
                                border = null,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                OverflowMenuContent(
                                    themeMode = themeMode,
                                    currentUserEmail = currentUserEmail,
                                    tagSuffix = "_2",
                                    parsedPaletteColor = parsedPaletteColor,
                                    hazeState = hazeState,
                                    onDismiss = { menuExpanded = false },
                                    onNavigateToDashboard = onNavigateToDashboard,
                                    onNavigateToInventory = onNavigateToInventory,
                                    onNavigateToAttendance = onNavigateToAttendance,
                                    onNavigateToContactDirectory = onNavigateToContactDirectory,
                                    onNavigateToPaymentReminders = onNavigateToPaymentReminders,
                                    onNavigateToSeasonalReminders = onNavigateToSeasonalReminders,
                                    onNavigateToBackupRestore = onNavigateToBackupRestore,
                                    onNavigateToLogin = onNavigateToLogin,
                                    onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                                    onNavigateToSettings = onNavigateToSettings,
                                    onNavigateToBusinessInfo = onNavigateToBusinessInfo,
                                    onNavigateToMessageTemplates = onNavigateToMessageTemplates,
                                    onNavigateToQrScanner = onNavigateToQrScanner,
                                    onLogout = onLogout,
                                    onOpenThemeDialog = onOpenThemeDialog,
                                    onOpenRecycleBin = onOpenRecycleBin
                                )
                            }
                        }
                    }
                }
            }
        }
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
    parsedPaletteColor: Color? = null,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToContactDirectory: () -> Unit,
    onNavigateToPaymentReminders: () -> Unit = {},
    onNavigateToSeasonalReminders: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToGardenPlanning: () -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToBusinessInfo: () -> Unit = {},
    onNavigateToMessageTemplates: () -> Unit = {},
    onNavigateToQrScanner: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED || (themeMode == AppThemeMode.SYSTEM && isAppInAmoledMode())
    val itemTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val menuAccent = parsedPaletteColor ?: MaterialTheme.colorScheme.primary

    // 1. Dashboard
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.Dashboard,
                title = "Dashboard",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToDashboard()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_menu_item$tagSuffix")
    )

    // 2. Inventory
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.Inventory2,
                title = "Inventory",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToInventory()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_menu_item$tagSuffix")
    )

    // 3. Attendance
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.EventAvailable,
                title = "Attendance",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToAttendance()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_menu_item$tagSuffix")
    )

    // 4. Contact Directory
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.Contacts,
                title = "Contact Directory",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToContactDirectory()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_directory_menu_item$tagSuffix")
    )

    // 5. Payment Reminder
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.ReceiptLong,
                title = "Payment Reminder",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToPaymentReminders()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_reminders_menu_item$tagSuffix")
    )

    // 6. Seasonal Reminders
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.Park,
                title = "Seasonal Reminders",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToSeasonalReminders()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seasonal_reminders_menu_item$tagSuffix")
    )

    // 7. Scan QR
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.QrCodeScanner,
                title = "Scan QR",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToQrScanner()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_qr_menu_item$tagSuffix")
    )

    // Divider before Settings with subtle neutral line (no gradient, no border)
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    )

    // 8. Settings
    DropdownMenuItem(
        text = {
            FrostedMenuItemContent(
                icon = Icons.Default.Settings,
                title = "Settings",
                accentColor = menuAccent,
                textColor = itemTextColor,
                isDark = isDark
            )
        },
        onClick = {
            onDismiss()
            onNavigateToSettings?.invoke()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_menu_item$tagSuffix")
    )
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

/**
 * True Frosted Liquid Glass Menu Background Modifier for the Profile Menu.
 * Rendering concept based on reference image and LiquidGlass specification:
 * BACKDROP → BLUR/SATURATION → TRANSLUCENT GLASS → EDGE SHEEN → CONTENT
 *
 * - Real backdrop blur of the content behind the menu via Haze.
 * - Background colors softly visible and diffused through the glass.
 * - Very transparent glass surface, NOT grey/blue/opaque.
 * - Subtle accent color diffusion and tactile micro-grain.
 * - Thin bright 1dp glass rim with specular top highlight.
 * - Very subtle top inner specular reflection.
 * - Soft floating 3D elevation shadow.
 * - Text and icons remain perfectly sharp inside.
 * - Explicit HazeStyle.backgroundColor maintained in every theme to prevent crashes.
 */
@Composable
fun Modifier.frostedLiquidGlassMenuBackground(
    hazeState: HazeState,
    isDark: Boolean = isAppInDarkMode(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: Color = Color(0xFF10B981),
    shape: Shape = RoundedCornerShape(22.dp)
): Modifier {
    val isAmoled = themeMode == AppThemeMode.AMOLED || (themeMode == AppThemeMode.SYSTEM && isAppInAmoledMode())

    // 1:1 match with Bottom Navigation reflective glass rim brush
    val glassRimBrush = Brush.linearGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.40f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.24f), // Reflective edge sheen
                Color.White.copy(alpha = 0.12f), // Clear lateral glass sides
                accentColor.copy(alpha = 0.16f), // Ambient edge reflection
                Color.White.copy(alpha = 0.08f)  // Soft specular bottom return
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.28f), // Reflective edge sheen in light mode
                Color.White.copy(alpha = 0.50f), // Clear lateral glass sides
                accentColor.copy(alpha = 0.18f), // Ambient edge reflection
                Color.White.copy(alpha = 0.40f)  // Soft specular bottom return
            )
        }
    )

    // 1:1 match with Bottom Navigation HazeStyle: blurRadius 64.dp, tints 0.05f, noiseFactor 0.08f
    val hazeStyle = HazeStyle(
        backgroundColor = if (isAmoled) {
            Color.Black.copy(alpha = 0.35f)
        } else if (isDark) {
            Color(0xFF16141D).copy(alpha = 0.28f)
        } else {
            Color.White.copy(alpha = 0.25f)
        },
        blurRadius = 64.dp,
        tints = listOf(
            HazeTint(
                color = accentColor.copy(alpha = 0.05f)
            )
        ),
        noiseFactor = 0.08f
    )

    return this
        // 1. Soft floating drop shadow with subtle ambient halo matching Bottom Navigation
        .shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = if (isAmoled) Color.Black.copy(alpha = 0.55f) else if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x22000000),
            ambientColor = if (isDark || isAmoled) accentColor.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.10f)
        )
        .clip(shape)
        // 2. Real optical backdrop blur via Haze
        .hazeEffect(state = hazeState, style = hazeStyle)
        // 3. Uniform reflective glass body wash matching Bottom Navigation
        .background(
            brush = when {
                isAmoled -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),          // Specular top glass reflection
                            Color(0xFF100E14).copy(alpha = 0.25f),    // Translucent dark glass
                            accentColor.copy(alpha = 0.08f),          // Uniform reflective color sheen
                            Color(0xFF000000).copy(alpha = 0.35f)     // Pure black AMOLED foundation
                        )
                    )
                }
                isDark -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),          // Specular top glass reflection
                            Color(0xFF221F2B).copy(alpha = 0.32f),    // Translucent dark charcoal glass
                            accentColor.copy(alpha = 0.10f),          // Uniform reflective color sheen
                            Color(0xFF14121A).copy(alpha = 0.35f)     // Dark charcoal gray foundation (non-pure-black)
                        )
                    )
                }
                else -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),          // Specular top glass reflection
                            Color.White.copy(alpha = 0.25f),          // Translucent light glass
                            accentColor.copy(alpha = 0.10f),          // Uniform reflective color sheen
                            Color.White.copy(alpha = 0.35f)           // Base foundation
                        )
                    )
                }
            },
            shape = shape
        )
        // 4. Soft noise grain overlay and dual-tone reflective specular top highlight matching Bottom Navigation
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightHeight = 1.5.dp.toPx()
            val margin = 12.dp.toPx()

            // Soft procedural micro-grain overlay for tactile frosted noisy blur
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.08f else 0.10f
            )

            // Top specular shine with blended reflective color sheen
            val highlightWhiteAlpha = if (isDark || isAmoled) 0.38f else 0.70f
            val sheenAccentAlpha = if (isDark || isAmoled) 0.18f else 0.22f

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = sheenAccentAlpha),
                        Color.White.copy(alpha = highlightWhiteAlpha),
                        accentColor.copy(alpha = sheenAccentAlpha),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 1.dp.toPx()),
                size = Size(w - (margin * 2), highlightHeight)
            )
        }
        // 5. Thin bright 1dp glass rim
        .border(
            border = BorderStroke(width = 1.dp, brush = glassRimBrush),
            shape = shape
        )
}

/**
 * Frosted Liquid Glass Modifier for settings and profile menu items:
 * Dashboard, Inventory, Attendance, Contact Directory, Payment Reminder, Seasonal Reminders, etc.
 *
 * Provides a soft, translucent glassy pill with subtle optical blur via Haze,
 * delicate specular rim lighting, item accent refraction, and a soft 3D elevation.
 */
@Composable
fun Modifier.frostedLiquidGlassMenuItem(
    hazeState: HazeState? = null,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(14.dp)
): Modifier {
    val hazeStyle = if (hazeState != null) {
        HazeStyle(
            backgroundColor = if (isDark || isAmoled) Color(0xFF14121A).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.45f),
            blurRadius = 16.dp,
            tints = listOf(
                HazeTint(
                    color = if (isDark || isAmoled) {
                        accentColor.copy(alpha = 0.10f)
                    } else {
                        Color.White.copy(alpha = 0.50f)
                    }
                )
            ),
            noiseFactor = 0.20f
        )
    } else null

    // Subtle 1dp glass rim with top specular sheen and accent refraction
    val glassRimBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.12f),
                accentColor.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.05f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.45f),
                accentColor.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.30f)
            )
        }
    )

    // Soft translucent liquid glass gradient letting background hints come through gently
    val glassGradient = if (isDark || isAmoled) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color(0xFF221F28).copy(alpha = 0.35f),
                accentColor.copy(alpha = 0.10f),
                Color(0xFF100F14).copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.60f),
                accentColor.copy(alpha = 0.10f),
                Color(0xFFF8EEEA).copy(alpha = 0.60f)
            )
        )
    }

    return this
        .shadow(
            elevation = 2.dp,
            shape = shape,
            spotColor = if (isDark || isAmoled) accentColor.copy(alpha = 0.18f) else Color(0x15000000),
            ambientColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.25f) else Color(0x0A000000)
        )
        .clip(shape)
        .then(
            if (hazeState != null && hazeStyle != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .background(brush = glassGradient, shape = shape)
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightHeight = 1.2.dp.toPx()
            val margin = 8.dp.toPx()

            // Soft procedural micro-grain overlay for tactile frosted noisy blur
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.12f else 0.15f
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.40f else 0.75f),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 0.8.dp.toPx()),
                size = Size(w - (margin * 2), highlightHeight)
            )
        }
        .border(
            border = BorderStroke(width = 1.dp, brush = glassRimBrush),
            shape = shape
        )
}

/**
 * Clean Backdrop Blur Modifier strictly for the Profile Menu.
 * - Proper optical backdrop blur via Haze with generous blur radius (40.dp).
 * - Flat translucent neutral surface without gradient colors.
 * - Borderless: completely removes borders and gradient rims.
 * - Soft neutral elevation shadow for floating depth.
 */
@Composable
fun Modifier.profileMenuBackdropBlur(
    hazeState: HazeState,
    isDark: Boolean = isAppInDarkMode(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(22.dp)
): Modifier = this.frostedLiquidGlassMenuBackground(
    hazeState = hazeState,
    isDark = isDark,
    themeMode = themeMode,
    accentColor = accentColor,
    shape = shape
)

/**
 * Clean Profile Menu Item Modifier without individual card/container backgrounds or borders.
 */
@Composable
fun Modifier.profileMenuItem(
    isDark: Boolean,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this

/**
 * Profile Menu item row:
 * [ ICON in circular translucent accent backdrop ]   Caption
 * No individual cards, no borders, no trailing arrow/chevron.
 * For Dark Mode: subtle colored circular palette-style background behind it, icon itself is WHITE.
 * For Light Mode: subtle colored circular background, icon uses accent color.
 */
@Composable
private fun FrostedMenuItemContent(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    textColor: Color,
    isDark: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Subtle colored circular palette-style background behind the icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    color = accentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) Color.White else accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

