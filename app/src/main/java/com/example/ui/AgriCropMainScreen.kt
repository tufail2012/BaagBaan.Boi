package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.example.ui.components.AgriBottomNav
import com.example.ui.components.AgriHeader
import com.example.ui.components.AgriSegmentedControl
import com.example.ui.components.FarmerFormScreen
import com.example.ui.components.FarmerRecordsScreen
import com.example.ui.components.GlobalSearchResultsScreen
import com.example.ui.components.PruningSubTabs
import com.example.ui.components.RootstockSubTabs

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.example.ui.components.attendance.AttendanceMainScreen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.UserAttendanceSection
import com.example.ui.components.UserBookingsSection

import com.example.ui.components.LoginScreen
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.ContactDirectoryDialog
import com.example.ui.components.AgriDashboardScreen
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriCropMainScreen(
    viewModel: CropViewModel,
    attendanceViewModel: AttendanceViewModel,
    notificationViewModel: NotificationViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { com.example.data.AppDatabase.getDatabase(context, coroutineScope) }
    val auth = com.example.util.SafeFirebase.getAuth(context)
    var currentUser by remember { mutableStateOf(auth?.currentUser) }
    var isAttendanceActive by remember { mutableStateOf(false) }
    var isDashboardActive by remember { mutableStateOf(false) }
    var isLoginActive by remember { mutableStateOf(currentUser == null) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showContactDirectoryDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }

    val notifications by (notificationViewModel?.notifications?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val unreadCount by (notificationViewModel?.unreadCount?.collectAsState() ?: remember { mutableStateOf(0) })

    val selectedService by viewModel.selectedService.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val accentColorHex by viewModel.accentColorHex.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGlobalSearchActive by viewModel.isGlobalSearchActive.collectAsState()

    val selectedPruningSubTab by viewModel.selectedPruningSubTab.collectAsState()
    val selectedRootstockSubTab by viewModel.selectedRootstockSubTab.collectAsState()
    val selectedGenevaOption by viewModel.selectedGenevaOption.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val userDashboardViewModel = remember { UserDashboardViewModel() }
    val dashboardUserMsg by userDashboardViewModel.userMessage.collectAsState()

    LaunchedEffect(currentUser) {
        userDashboardViewModel.refreshUser()
    }

    LaunchedEffect(dashboardUserMsg) {
        dashboardUserMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            userDashboardViewModel.clearUserMessage()
        }
    }

    LaunchedEffect(Unit) {
        val user = auth?.currentUser
        if (user != null && !user.isEmailVerified) {
            auth?.signOut()
            currentUser = null
            isLoginActive = true
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (showNotificationCenter && notificationViewModel != null) {
        NotificationCenterSheet(
            notifications = notifications,
            unreadCount = unreadCount,
            onDismiss = { showNotificationCenter = false },
            onMarkAsRead = { notificationViewModel.markAsRead(it) },
            onMarkAllAsRead = { notificationViewModel.markAllAsRead() },
            onDeleteNotification = { notificationViewModel.deleteNotification(it) },
            onClearAll = { notificationViewModel.deleteAllNotifications() },
            onScheduleReminder = { title, message, triggerAtMillis ->
                notificationViewModel.sendReminder(context, title, message, triggerAtMillis)
            }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            onDismiss = { showBackupRestoreDialog = false }
        )
    }

    if (showContactDirectoryDialog) {
        ContactDirectoryDialog(
            onDismiss = { showContactDirectoryDialog = false }
        )
    }

    if (showRecycleBinDialog) {
        com.example.ui.components.RecycleBinDialog(
            onDismissRequest = { showRecycleBinDialog = false },
            db = db,
            isDark = themeMode == com.example.ui.AppThemeMode.DARK || themeMode == com.example.ui.AppThemeMode.AMOLED
        )
    }

    if (showInventoryDialog) {
        com.example.ui.components.InventoryManagementDialog(
            onDismissRequest = { showInventoryDialog = false },
            db = db,
            isDark = themeMode == com.example.ui.AppThemeMode.DARK || themeMode == com.example.ui.AppThemeMode.AMOLED
        )
    }

    if (isLoginActive) {
        LoginScreen(
            onLoginSuccess = { userEmail ->
                currentUser = auth?.currentUser
                userDashboardViewModel.refreshUser()
                isLoginActive = false
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao())
                    } catch (e: Exception) {
                        // Silent fail pattern matching onCreate / manual sync
                    }
                }
            },
            onContinueAsGuest = {
                isLoginActive = false
            },
            modifier = modifier
        )
    } else if (isDashboardActive) {
        AgriDashboardScreen(
            viewModel = viewModel,
            userDashboardViewModel = userDashboardViewModel,
            currentUserEmail = currentUser?.email,
            onBack = { isDashboardActive = false },
            onNavigateToCategory = { category ->
                viewModel.selectServiceCategory(category)
                isDashboardActive = false
            },
            modifier = modifier
        )
    } else if (isAttendanceActive) {
        AttendanceMainScreen(
            viewModel = attendanceViewModel,
            onNavigateBackToMain = { isAttendanceActive = false },
            modifier = modifier
        )
    } else if (isGlobalSearchActive) {
        GlobalSearchResultsScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeGlobalSearch() },
            modifier = modifier
        )
    } else {
        val displayHeaderTitle = when {
            selectedService.equals("Imported", ignoreCase = true) -> "Imported Plants"
            selectedService.equals("Rootstocks", ignoreCase = true) -> "Imported Rootstocks"
            selectedService.equals("Bookings", ignoreCase = true) -> "Bookings"
            selectedService.equals("Attendance", ignoreCase = true) -> "Worker Attendance"
            else -> selectedService
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    AgriHeader(
                        title = displayHeaderTitle,
                        themeMode = themeMode,
                        selectedColorHex = accentColorHex,
                        onSelectThemeMode = { mode -> viewModel.setThemeMode(context, mode) },
                        onSelectColorHex = { hex -> viewModel.setAccentColorHex(context, hex) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { newQuery ->
                            viewModel.setSearchQuery(newQuery)
                        },
                        isSearchActive = isGlobalSearchActive,
                        onSearchActiveChange = { active ->
                            if (active) viewModel.openGlobalSearch() else viewModel.closeGlobalSearch()
                        },
                        onToggleSearch = {
                            viewModel.openGlobalSearch()
                        },
                        onNavigateToAttendance = {
                            isAttendanceActive = true
                        },
                        onNavigateToBookings = {
                            viewModel.selectServiceCategory("Bookings")
                        },
                        onNavigateToBackupRestore = {
                            showBackupRestoreDialog = true
                        },
                        onNavigateToContactDirectory = {
                            showContactDirectoryDialog = true
                        },
                        onNavigateToInventory = {
                            showInventoryDialog = true
                        },
                        onOpenRecycleBin = {
                            showRecycleBinDialog = true
                        },
                        onNavigateToDashboard = {
                            isDashboardActive = true
                        },
                        onNavigateToLogin = {
                            isLoginActive = true
                        },
                        unreadNotificationCount = unreadCount,
                        onOpenNotifications = {
                            showNotificationCenter = true
                        },
                        currentUserEmail = currentUser?.email,
                        onLogout = {
                            auth?.signOut()
                            currentUser = null
                            isLoginActive = true
                        },
                        onManualSync = {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao())
                            }
                        }
                    )

                    // Segmented toggle header (New Entry / Records) - shown only for Crop Services
                    if (!selectedService.equals("Bookings", ignoreCase = true) && !selectedService.equals("Attendance", ignoreCase = true)) {
                        AgriSegmentedControl(
                            selectedMode = viewMode,
                            onModeSelected = { viewModel.setViewMode(it) }
                        )
                    }

                    // Dedicated Sub-Tabs for Pruning & Rootstocks
                    if (selectedService.equals("Pruning", ignoreCase = true)) {
                        PruningSubTabs(
                            selectedSubTab = selectedPruningSubTab,
                            onSelectSubTab = { viewModel.selectPruningSubTab(it) }
                        )
                    } else if (selectedService.equals("Rootstocks", ignoreCase = true)) {
                        RootstockSubTabs(
                            selectedSubTab = selectedRootstockSubTab,
                            selectedGenevaOption = selectedGenevaOption,
                            onSelectSubTab = { subTab, genevaOpt ->
                                viewModel.selectRootstockSubTab(subTab, genevaOpt)
                            }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when {
                    selectedService.equals("Bookings", ignoreCase = true) -> {
                        UserBookingsSection(viewModel = userDashboardViewModel)
                    }
                    selectedService.equals("Attendance", ignoreCase = true) -> {
                        UserAttendanceSection(viewModel = userDashboardViewModel)
                    }
                    else -> {
                        when (viewMode) {
                            0 -> FarmerFormScreen(viewModel = viewModel)
                            else -> FarmerRecordsScreen(viewModel = viewModel)
                        }
                    }
                }

                // Standalone floating pill navigation bar hovering above the bottom edge
                AgriBottomNav(
                    selectedCategory = selectedService,
                    onCategorySelected = { category ->
                        viewModel.selectServiceCategory(category)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

