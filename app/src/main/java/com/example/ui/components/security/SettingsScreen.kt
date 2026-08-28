package com.example.ui.components.security

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.getSectionAccentColor
import com.example.ui.components.glassCardBackground
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLockManager
import com.example.security.LockAfterDuration
import com.example.security.UnlockMethod
import com.example.ui.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appLockManager: AppLockManager,
    themeMode: AppThemeMode,
    currentUserEmail: String? = null,
    currentUserPhotoUrl: String? = null,
    onOpenThemeDialog: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit,
    onOpenRecycleBin: () -> Unit = {},
    onNavigateToBusinessInfo: () -> Unit = {},
    onNavigateToMessageTemplates: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isAppLockEnabled by appLockManager.isAppLockEnabled.collectAsState()
    val unlockMethod by appLockManager.currentUnlockMethod.collectAsState()
    val lockAfterDuration by appLockManager.lockAfterDuration.collectAsState()
    val hideInRecentApps by appLockManager.hideInRecentApps.collectAsState()
    val protectNotifications by appLockManager.protectNotifications.collectAsState()

    var showSetupSheet by remember { mutableStateOf(false) }
    var isChangeMethodFlow by remember { mutableStateOf(false) }
    var showLockAfterSheet by remember { mutableStateOf(false) }
    var showDisableAuthDialog by remember { mutableStateOf(false) }
    var showChangeMethodAuthDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showLogoutDialog -> showLogoutDialog = false
            showDisableAuthDialog -> showDisableAuthDialog = false
            showChangeMethodAuthDialog -> showChangeMethodAuthDialog = false
            showLockAfterSheet -> showLockAfterSheet = false
            showSetupSheet -> showSetupSheet = false
            else -> onBack()
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out from ${currentUserEmail ?: "your account"}?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        text = "Logout",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Lock After Bottom Sheet
    if (showLockAfterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLockAfterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDarkDialog = MaterialTheme.colorScheme.surface.let {
                        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
                    }

                    Text(
                        text = "Lock After",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showLockAfterSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkDialog) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LockAfterDuration.entries.forEach { option ->
                    val isSelected = option == lockAfterDuration
                    val isDarkDialog = MaterialTheme.colorScheme.surface.let {
                        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
                    }
                    Surface(
                        onClick = {
                            appLockManager.updateLockAfterDuration(option)
                            showLockAfterSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            if (isDarkDialog) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    if (isDarkDialog) Color.White else MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isDarkDialog) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // App Lock Setup Flow Sheet (for initial enable or change method)
    if (showSetupSheet) {
        AppLockSetupSheet(
            isChangeMethodFlow = isChangeMethodFlow,
            onCompleteSetup = { method, credential ->
                if (isChangeMethodFlow) {
                    appLockManager.updateUnlockMethod(method, credential)
                } else {
                    appLockManager.enableAppLock(method, credential)
                    if (activity != null) {
                        appLockManager.applySecureWindowFlag(activity)
                    }
                }
                showSetupSheet = false
                isChangeMethodFlow = false
            },
            onDismiss = {
                showSetupSheet = false
                isChangeMethodFlow = false
            }
        )
    }

    // Verification Dialog before Disabling App Lock
    if (showDisableAuthDialog) {
        AuthenticateVerificationDialog(
            title = "Disable App Lock?",
            subtitle = "Authenticate to turn off App Lock.",
            appLockManager = appLockManager,
            onSuccess = {
                showDisableAuthDialog = false
                appLockManager.disableAppLock()
                if (activity != null) {
                    appLockManager.applySecureWindowFlag(activity)
                }
            },
            onDismiss = {
                showDisableAuthDialog = false
            }
        )
    }

    // Verification Dialog before Changing Unlock Method
    if (showChangeMethodAuthDialog) {
        AuthenticateVerificationDialog(
            title = "Change Unlock Method",
            subtitle = "Authenticate to configure a new unlock method.",
            appLockManager = appLockManager,
            onSuccess = {
                showChangeMethodAuthDialog = false
                isChangeMethodFlow = true
                showSetupSheet = true
            },
            onDismiss = {
                showChangeMethodAuthDialog = false
            }
        )
    }

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val settingsAccent = getSectionAccentColor("Settings")
    val isAmoled = themeMode == AppThemeMode.AMOLED || (isDark && MaterialTheme.colorScheme.background == Color(0xFF000000))
    val settingsBgBrush = remember(isDark, isAmoled, settingsAccent) {
        if (isAmoled) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF000000),
                    Color(0xFF09090B),
                    settingsAccent.copy(alpha = 0.05f),
                    Color(0xFF000000)
                )
            )
        } else if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF0D1B2A),
                    settingsAccent.copy(alpha = 0.05f),
                    Color(0xFF060911)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    settingsAccent.copy(alpha = 0.035f),
                    Color(0xFFF1F5F9),
                    Color(0xFFFFFFFF)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(settingsBgBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar - Wide Pill-Shaped Glass Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .glassCardBackground(
                        isDark = isDark,
                        accentColor = settingsAccent,
                        shape = CircleShape,
                        themeMode = themeMode
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = settingsAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(settingsAccent.copy(alpha = if (isDark) 0.25f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = settingsAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Appearance, Account, Security & Storage",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ==========================================
                // SECTION 1: APPEARANCE
                // ==========================================
                item {
                    Text(
                        text = "Appearance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = Color(0xFF8B5CF6),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val modeLabel = when (themeMode) {
                                AppThemeMode.SYSTEM -> "System Default"
                                AppThemeMode.LIGHT -> "Light Theme"
                                AppThemeMode.DARK -> "Dark Theme"
                                AppThemeMode.AMOLED -> "AMOLED Pure Black"
                            }

                            SettingsNavigationRow(
                                icon = Icons.Default.Palette,
                                title = "Theme & Preferences",
                                subtitle = modeLabel,
                                onClick = onOpenThemeDialog,
                                testTag = "settings_theme_row"
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 2: ACCOUNT
                // ==========================================
                item {
                    Text(
                        text = "Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = Color(0xFF3B82F6),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Accounts Row
                            SettingsNavigationRow(
                                icon = Icons.Default.AccountCircle,
                                title = "Accounts",
                                subtitle = if (!currentUserEmail.isNullOrBlank()) currentUserEmail else "Sign in / Manage accounts",
                                onClick = onNavigateToAccounts,
                                testTag = "settings_accounts_row"
                            )

                            // Logout Row (if logged in)
                            if (!currentUserEmail.isNullOrBlank()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                SettingsNavigationRow(
                                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                                    title = "Logout",
                                    subtitle = "Sign out of $currentUserEmail",
                                    onClick = { showLogoutDialog = true },
                                    isDestructive = true,
                                    testTag = "settings_logout_row"
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION: BUSINESS IDENTITY
                // ==========================================
                item {
                    Text(
                        text = "Business Identity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = Color(0xFF10B981),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsNavigationRow(
                                icon = Icons.Default.Storefront,
                                title = "Business Info",
                                subtitle = "Shared business name, address, contacts & bank details",
                                onClick = onNavigateToBusinessInfo,
                                testTag = "settings_business_info_row"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            SettingsNavigationRow(
                                icon = Icons.Default.ReceiptLong,
                                title = "Message Templates",
                                subtitle = "Customize WhatsApp & SMS format strings and dynamic placeholders",
                                onClick = onNavigateToMessageTemplates,
                                testTag = "settings_message_templates_row"
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 3: SECURITY & PRIVACY
                // ==========================================
                item {
                    Text(
                        text = "Security & Privacy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = Color(0xFFE11D48),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 1. App Lock Switch Row
                            val masterLockIconTint = if (isAppLockEnabled) {
                                Color.White
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAppLockEnabled) {
                                                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                    else MaterialTheme.colorScheme.primary
                                                } else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = masterLockIconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "App Lock",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAppLockEnabled) "App is protected with ${when (unlockMethod) {
                                                UnlockMethod.BIOMETRIC -> "Fingerprint"
                                                UnlockMethod.PIN -> "PIN"
                                                UnlockMethod.PATTERN -> "Pattern"
                                                UnlockMethod.PASSWORD -> "Password"
                                            }}" else "Protect this app from unauthorized access",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isAppLockEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            isChangeMethodFlow = false
                                            showSetupSheet = true
                                        } else {
                                            showDisableAuthDialog = true
                                        }
                                    },
                                    modifier = Modifier.testTag("app_lock_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Sub-options when App Lock is ENABLED
                            AnimatedVisibility(
                                visible = isAppLockEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Dynamic Method Title and Icon based strictly on configured unlockMethod
                                    val currentMethodName = when (unlockMethod) {
                                        UnlockMethod.BIOMETRIC -> "Fingerprint"
                                        UnlockMethod.PIN -> "PIN"
                                        UnlockMethod.PATTERN -> "Pattern"
                                        UnlockMethod.PASSWORD -> "Password"
                                    }

                                    val methodIcon = when (unlockMethod) {
                                        UnlockMethod.BIOMETRIC -> Icons.Default.Fingerprint
                                        UnlockMethod.PIN -> Icons.Default.Pin
                                        UnlockMethod.PATTERN -> Icons.Default.Pattern
                                        UnlockMethod.PASSWORD -> Icons.Default.Password
                                    }

                                    // 2. Unlock Method Display Row
                                    SettingsNavigationRow(
                                        icon = methodIcon,
                                        title = "Unlock Method",
                                        subtitle = currentMethodName,
                                        onClick = { showChangeMethodAuthDialog = true },
                                        testTag = "settings_unlock_method"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3. Change Unlock Method Row
                                    SettingsNavigationRow(
                                        icon = Icons.Default.Key,
                                        title = "Change Unlock Method",
                                        subtitle = "Configure Fingerprint, PIN, Pattern, or Password",
                                        onClick = { showChangeMethodAuthDialog = true },
                                        testTag = "settings_change_unlock_method"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4. Lock After Row
                                    SettingsNavigationRow(
                                        icon = Icons.Default.Timer,
                                        title = "Lock After",
                                        subtitle = lockAfterDuration.label,
                                        onClick = { showLockAfterSheet = true },
                                        testTag = "settings_lock_after"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Other Security Options
                                    Text(
                                        text = "Other Security Options",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    // Hide Content in Recent Apps Toggle
                                    SettingsToggleRow(
                                        icon = Icons.Default.VisibilityOff,
                                        title = "Hide Content in Recent Apps",
                                        subtitle = "Prevent sensitive app content from appearing in recent-apps preview",
                                        checked = hideInRecentApps,
                                        onCheckedChange = { checked ->
                                            appLockManager.updateHideInRecentApps(checked)
                                            if (activity != null) {
                                                appLockManager.applySecureWindowFlag(activity)
                                            }
                                        },
                                        testTag = "hide_in_recents_switch"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Protect Notifications Toggle
                                    SettingsToggleRow(
                                        icon = Icons.Default.NotificationsOff,
                                        title = "Protect Notifications",
                                        subtitle = "Hide sensitive information while the app is locked",
                                        checked = protectNotifications,
                                        onCheckedChange = { checked ->
                                            appLockManager.updateProtectNotifications(checked)
                                        },
                                        testTag = "protect_notifications_switch"
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Disable App Lock Button
                                    OutlinedButton(
                                        onClick = { showDisableAuthDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("disable_app_lock_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text("Disable App Lock", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 4: DATA & STORAGE
                // ==========================================
                item {
                    Text(
                        text = "Data & Storage",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = Color(0xFF06B6D4),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Data Backup & Restore
                            SettingsNavigationRow(
                                icon = Icons.Default.CloudSync,
                                title = "Data Backup & Restore",
                                subtitle = "Local JSON & Excel database backups",
                                onClick = onNavigateToBackupRestore,
                                testTag = "settings_backup_row"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // 2. Recycle Bin
                            SettingsNavigationRow(
                                icon = Icons.Default.Delete,
                                title = "Recycle Bin",
                                subtitle = "Restore or permanently delete removed records",
                                onClick = onOpenRecycleBin,
                                testTag = "settings_recycle_bin_row"
                            )
                        }
                    }
                }

                // ==========================================
                // SECURITY ENCRYPTION NOTE
                // ==========================================
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 20.dp,
                                accentColor = getSectionAccentColor("Settings"),
                                isDark = isDark,
                                themeMode = themeMode
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Device-Bound Cryptographic Salt & SHA-256",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Credentials and security preferences are hashed with unique on-device cryptographic salts and stored securely on your local device.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val iconTint = if (isDestructive) {
        if (isDark) Color(0xFFFFB4AB) else Color.White
    } else {
        Color.White
    }
    val containerBg = if (isDestructive) {
        if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.error
    } else {
        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.primary
    }
    val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val iconTint = if (checked) {
        Color.White
    } else {
        if (isDark) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val boxBg = if (checked) {
        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(boxBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
