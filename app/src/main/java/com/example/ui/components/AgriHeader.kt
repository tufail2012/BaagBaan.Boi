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
    val sectionAccent = accentColor ?: getSectionAccentColor(title, customPaletteColor = parsedPaletteColor)
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
                            .widthIn(min = 230.dp, max = 285.dp)
                            .frostedLiquidGlassMenuBackground(
                                hazeState = hazeState,
                                isDark = isDark,
                                themeMode = themeMode,
                                accentColor = getSectionAccentColor("Profile", customPaletteColor = parsedPaletteColor),
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
                                    .widthIn(min = 230.dp, max = 285.dp)
                                    .frostedLiquidGlassMenuBackground(
                                        hazeState = hazeState,
                                        isDark = isDark,
                                        themeMode = themeMode,
                                        accentColor = getSectionAccentColor("Profile", customPaletteColor = parsedPaletteColor),
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
    val itemTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

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
                    tint = getSectionAccentColor("Dashboard"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToDashboard()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("dashboard_menu_item$tagSuffix")
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
                    tint = getSectionAccentColor("Inventory", customPaletteColor = parsedPaletteColor),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Inventory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToInventory()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("inventory_menu_item$tagSuffix")
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
                    tint = getSectionAccentColor("Attendance"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Attendance",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToAttendance()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("attendance_menu_item$tagSuffix")
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
                    tint = getSectionAccentColor("Contact Directory"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Contact Directory",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToContactDirectory()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("contact_directory_menu_item$tagSuffix")
    )

    // 5. Payment Reminder
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = getSectionAccentColor("Payment Reminder"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Payment Reminder",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToPaymentReminders()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("payment_reminders_menu_item$tagSuffix")
    )

    // Seasonal Reminders
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = null,
                    tint = getSectionAccentColor("Seasonal Reminders"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Seasonal Reminders",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToSeasonalReminders()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("seasonal_reminders_menu_item$tagSuffix")
    )

    // 6. Scan QR
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = getSectionAccentColor("Scan QR"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scan QR",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToQrScanner()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("scan_qr_menu_item$tagSuffix")
    )

    // Divider before Settings with subtle glowing accent tint
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        color = (parsedPaletteColor ?: getSectionAccentColor("Profile")).copy(alpha = if (isDark) 0.28f else 0.18f)
    )

    // 6. Settings
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = getSectionAccentColor("Settings"),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = itemTextColor
                )
            }
        },
        onClick = {
            onDismiss()
            onNavigateToSettings?.invoke()
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
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
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Solid opaque base layer ensures 100% complete obscurity of underlying text and form shapes
    val solidBaseColor = when {
        isAmoled -> Color(0xFF000000) // AMOLED pure black for pixels off
        isDark -> Color(0xFF0C0B0F)   // Deep black charcoal matching reference screenshot
        else -> Color(0xFFF8FAFC)
    }

    // Frosted liquid glass gradient: provides specular top reflection, rich accent light transmission, and deep ambient tone
    val liquidGlassGradient = Brush.verticalGradient(
        colors = when {
            isAmoled -> listOf(
                Color.White.copy(alpha = 0.28f),
                Color(0xFF141216),
                accentColor.copy(alpha = 0.18f),
                Color(0xFF000000) // Pure black for AMOLED
            )
            isDark -> listOf(
                Color.White.copy(alpha = 0.28f),
                Color(0xFF242127),
                accentColor.copy(alpha = 0.15f),
                Color(0xFF0C0B0F) // Deep glossy tone matching screenshot
            )
            else -> listOf(
                Color.White,
                Color(0xFFFFFFFF),
                accentColor.copy(alpha = 0.22f),
                Color(0xFFEDF2F7)
            )
        }
    )

    // Thin bright 1dp glass rim with specular top highlight and accent refraction
    val glassRimBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.40f), // Crisp specular top rim
                Color.White.copy(alpha = 0.15f), // Clear glass sides
                accentColor.copy(alpha = 0.25f), // Accent color refraction
                Color.White.copy(alpha = 0.05f)  // Soft bottom rim
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f), // Crisp bright top rim
                Color.White.copy(alpha = 0.45f), // Translucent sides
                accentColor.copy(alpha = 0.30f), // Soft accent diffusion
                Color.White.copy(alpha = 0.40f)  // Subtle bottom rim
            )
        }
    )

    return this
        // 1. Soft floating 3D elevation shadow
        .shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.50f) else Color(0x30000000),
            ambientColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.30f) else Color(0x14000000)
        )
        .clip(shape)
        // 2. Base solid opaque barrier guaranteeing 100% obscurity of underlying text/shapes
        .background(color = solidBaseColor, shape = shape)
        // 3. Frosted liquid glass gradient with light transmission and vibrant accent diffusion
        .background(brush = liquidGlassGradient, shape = shape)
        // 4. Subtle top inner specular reflection / edge sheen
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightHeight = 1.5.dp.toPx()
            val margin = 10.dp.toPx()

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.55f else 0.80f),
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

