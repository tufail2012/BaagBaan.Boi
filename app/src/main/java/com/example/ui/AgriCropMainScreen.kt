package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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
import com.example.ui.components.BookingConfirmationOverlay
import com.example.ui.components.PaymentRemindersDialog
import com.example.ui.components.BusinessInfoDialog
import com.example.ui.components.MessageTemplateManagerScreen
import com.example.ui.components.AgriDashboardScreen
import com.example.data.BusinessInfoRepository
import com.example.data.MessageTemplateRepository
import com.example.security.AppLockManager
import com.example.ui.components.security.AppLockScreen
import com.example.ui.components.security.SettingsScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.ClearCredentialException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriCropMainScreen(
    viewModel: CropViewModel,
    attendanceViewModel: AttendanceViewModel,
    notificationViewModel: NotificationViewModel? = null,
    appLockManager: AppLockManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { com.example.data.AppDatabase.getDatabase(context, coroutineScope) }
    val auth = com.example.util.SafeFirebase.getAuth(context)
    var currentUser by remember { mutableStateOf(auth?.currentUser) }
    var isAttendanceActive by remember { mutableStateOf(false) }
    var isGardenPlanningActive by remember { mutableStateOf(false) }
    var isDashboardActive by remember { mutableStateOf(false) }
    var isSettingsActive by remember { mutableStateOf(false) }

    val effectiveAppLockManager = remember { appLockManager ?: AppLockManager.getInstance(context.applicationContext) }
    val isAppLockEnabled by effectiveAppLockManager.isAppLockEnabled.collectAsState()
    val isAppLocked by effectiveAppLockManager.isLocked.collectAsState()

    val gardenPlanningViewModel: GardenPlanningViewModel = remember {
        val repository = com.example.data.GardenPlanningRepository(
            dao = db.gardenPlanningDao(),
            farmerContactDao = db.farmerContactDao(),
            recycleBinDao = db.recycleBinDao()
        )
        GardenPlanningViewModel(repository)
    }
    var isLoginActive by remember { mutableStateOf(currentUser == null) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showContactDirectoryDialog by remember { mutableStateOf(false) }
    var showPaymentRemindersDialog by remember { mutableStateOf(false) }
    val showPaymentRemindersFromVm by viewModel.showPaymentRemindersDialog.collectAsState()
    var showSeasonalRemindersDialog by remember { mutableStateOf(false) }
    val showSeasonalRemindersFromVm by viewModel.showSeasonalRemindersDialog.collectAsState()
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    val showInventoryFromVm by viewModel.showInventoryDialog.collectAsState()
    var showThemePreferencesDialog by remember { mutableStateOf(false) }
    var showBusinessInfoDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var isMessageTemplatesActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BusinessInfoRepository.startListening(context)
        MessageTemplateRepository.startListening(context)
    }

    val notifications by (notificationViewModel?.notifications?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val unreadCount by (notificationViewModel?.unreadCount?.collectAsState() ?: remember { mutableStateOf(0) })

    val selectedService by viewModel.selectedService.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        com.example.ui.AppThemeMode.SYSTEM -> isSystemDark
        com.example.ui.AppThemeMode.LIGHT -> false
        com.example.ui.AppThemeMode.DARK, com.example.ui.AppThemeMode.AMOLED -> true
    }
    val accentColorHex by viewModel.accentColorHex.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGlobalSearchActive by viewModel.isGlobalSearchActive.collectAsState()

    val selectedPruningSubTab by viewModel.selectedPruningSubTab.collectAsState()
    val selectedRootstockSubTab by viewModel.selectedRootstockSubTab.collectAsState()
    val selectedGenevaOption by viewModel.selectedGenevaOption.collectAsState()
    val filteredCropRecords by viewModel.filteredRecords.collectAsState()
    val cropRecordsCount = filteredCropRecords.size

    val snackbarHostState = remember { SnackbarHostState() }

    val userDashboardViewModel = remember { UserDashboardViewModel() }
    val dashboardUserMsg by userDashboardViewModel.userMessage.collectAsState()

    LaunchedEffect(currentUser) {
        userDashboardViewModel.refreshUser()
    }

    val performLogout: () -> Unit = {
        auth?.signOut()
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: ClearCredentialException) {
                e.printStackTrace()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                currentUser = null
                isLoginActive = true
            }
        }
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
            performLogout()
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

    if (showPaymentRemindersDialog || showPaymentRemindersFromVm) {
        PaymentRemindersDialog(
            onDismiss = {
                showPaymentRemindersDialog = false
                viewModel.dismissPaymentReminders()
            }
        )
    }

    if (showSeasonalRemindersDialog || showSeasonalRemindersFromVm) {
        com.example.ui.components.SeasonalRemindersDialog(
            onDismiss = {
                showSeasonalRemindersDialog = false
                viewModel.dismissSeasonalReminders()
            }
        )
    }

    if (showRecycleBinDialog) {
        com.example.ui.components.RecycleBinDialog(
            onDismissRequest = { showRecycleBinDialog = false },
            db = db,
            isDark = isDark
        )
    }

    if (showInventoryDialog || showInventoryFromVm) {
        com.example.ui.components.InventoryManagementDialog(
            onDismissRequest = {
                showInventoryDialog = false
                viewModel.dismissInventoryManagement()
            },
            db = db,
            isDark = isDark,
            viewModel = viewModel
        )
    }

    if (showThemePreferencesDialog) {
        com.example.ui.components.ThemeColoursDialog(
            themeMode = themeMode,
            selectedColorHex = accentColorHex,
            onSelectThemeMode = { mode -> viewModel.setThemeMode(context, mode) },
            onSelectColorHex = { hex -> viewModel.setAccentColorHex(context, hex) },
            onDismissRequest = { showThemePreferencesDialog = false }
        )
    }

    if (showBusinessInfoDialog) {
        BusinessInfoDialog(
            onDismiss = { showBusinessInfoDialog = false }
        )
    }

    if (showQrScannerDialog) {
        com.example.ui.components.QrScannerDialog(
            onDismissRequest = { showQrScannerDialog = false },
            onQrScanned = { rawQr ->
                showQrScannerDialog = false
                viewModel.handleDeepLinkString(rawQr) { errMsg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errMsg)
                    }
                }
            }
        )
    }

    val selectedDetailCropRecord by viewModel.selectedDetailCropRecord.collectAsState()
    val selectedDetailGardenEntry by viewModel.selectedDetailGardenEntry.collectAsState()

    selectedDetailCropRecord?.let { record ->
        com.example.ui.components.BookingRecordDetailDialog(
            record = record,
            onDismiss = { viewModel.dismissCropRecordDetail() },
            onEdit = { rec ->
                viewModel.dismissCropRecordDetail()
                viewModel.loadRecordForEditing(rec)
                isSettingsActive = false
                isDashboardActive = false
                isAttendanceActive = false
                isMessageTemplatesActive = false
                viewModel.closeGlobalSearch()
            },
            onDelete = { rec ->
                viewModel.dismissCropRecordDetail()
                viewModel.deleteRecord(rec)
            },
            onUpdateRecord = { updatedRec ->
                viewModel.openCropRecordDetail(updatedRec)
                viewModel.updateRecordSync(updatedRec)
            }
        )
    }

    selectedDetailGardenEntry?.let { entry ->
        com.example.ui.components.GardenBookingRecordDetailDialog(
            entry = entry,
            viewModel = gardenPlanningViewModel,
            isDark = isDark,
            onDismiss = { viewModel.dismissGardenEntryDetail() },
            onEdit = { edited ->
                viewModel.dismissGardenEntryDetail()
                gardenPlanningViewModel.loadEntryForEdit(edited)
                viewModel.selectServiceCategory("Garden Planning")
                isSettingsActive = false
                isDashboardActive = false
                isAttendanceActive = false
                isMessageTemplatesActive = false
                viewModel.closeGlobalSearch()
            }
        )
    }

    val currentRootScreen = when {
        isLoginActive -> "LOGIN"
        isMessageTemplatesActive -> "TEMPLATES"
        isSettingsActive -> "SETTINGS"
        isDashboardActive -> "DASHBOARD"
        isAttendanceActive -> "ATTENDANCE"
        isGlobalSearchActive -> "SEARCH"
        else -> "MAIN"
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentRootScreen,
        transitionSpec = {
            if (initialState == "LOGIN") {
                (fadeIn(animationSpec = tween(450)) + scaleIn(initialScale = 0.95f, animationSpec = tween(450)))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            } else if (targetState == "LOGIN") {
                fadeIn(animationSpec = tween(300))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            } else {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.97f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            }
        },
        label = "RootScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { targetScreen ->
        when (targetScreen) {
            "LOGIN" -> {
                LoginScreen(
                    onLoginSuccess = { userEmail ->
                        currentUser = auth?.currentUser
                        userDashboardViewModel.refreshUser()
                        isLoginActive = false
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao(), db.gardenPlanningDao())
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
            }
            "SETTINGS" -> {
                SettingsScreen(
                    appLockManager = effectiveAppLockManager,
                    themeMode = themeMode,
                    currentUserEmail = currentUser?.email,
                    currentUserPhotoUrl = currentUser?.photoUrl?.toString(),
                    onOpenThemeDialog = { showThemePreferencesDialog = true },
                    onNavigateToAccounts = {
                        isSettingsActive = false
                        isLoginActive = true
                    },
                    onLogout = {
                        isSettingsActive = false
                        performLogout()
                    },
                    onNavigateToBackupRestore = { showBackupRestoreDialog = true },
                    onOpenRecycleBin = { showRecycleBinDialog = true },
                    onNavigateToBusinessInfo = { showBusinessInfoDialog = true },
                    onNavigateToMessageTemplates = { isMessageTemplatesActive = true },
                    onBack = { isSettingsActive = false },
                    modifier = modifier
                )
            }
            "TEMPLATES" -> {
                MessageTemplateManagerScreen(
                    onNavigateBack = { isMessageTemplatesActive = false },
                    modifier = modifier
                )
            }
            "DASHBOARD" -> {
                AgriDashboardScreen(
                    viewModel = viewModel,
                    userDashboardViewModel = userDashboardViewModel,
                    gardenPlanningViewModel = gardenPlanningViewModel,
                    currentUserEmail = currentUser?.email,
                    onBack = { isDashboardActive = false },
                    onNavigateToCategory = { category ->
                        viewModel.selectServiceCategory(category)
                        gardenPlanningViewModel.resetToNewEntry()
                        isDashboardActive = false
                    },
                    onNavigateToSettings = { isSettingsActive = true },
                    modifier = modifier
                )
            }
            "ATTENDANCE" -> {
                AttendanceMainScreen(
                    viewModel = attendanceViewModel,
                    onNavigateBackToMain = { isAttendanceActive = false },
                    themeMode = themeMode,
                    selectedColorHex = accentColorHex,
                    onSelectThemeMode = { mode -> viewModel.setThemeMode(context, mode) },
                    onSelectColorHex = { hex -> viewModel.setAccentColorHex(context, hex) },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { newQuery -> viewModel.setSearchQuery(newQuery) },
                    isSearchActive = isGlobalSearchActive,
                    onSearchActiveChange = { active -> if (active) viewModel.openGlobalSearch() else viewModel.closeGlobalSearch() },
                    onToggleSearch = { viewModel.openGlobalSearch() },
                    onNavigateToAttendance = { isAttendanceActive = true },
                    onNavigateToBookings = { viewModel.selectServiceCategory("Bookings") },
                    onNavigateToBackupRestore = { showBackupRestoreDialog = true },
                    onNavigateToContactDirectory = { showContactDirectoryDialog = true },
                    onNavigateToPaymentReminders = { showPaymentRemindersDialog = true },
                    onNavigateToSeasonalReminders = { showSeasonalRemindersDialog = true },
                    onNavigateToInventory = { showInventoryDialog = true },
                    onOpenRecycleBin = { showRecycleBinDialog = true },
                    onNavigateToDashboard = { isDashboardActive = true },
                    onNavigateToLogin = { isLoginActive = true },
                    onNavigateToGardenPlanning = {
                        isAttendanceActive = false
                        viewModel.selectServiceCategory("Garden Planning")
                    },
                    unreadNotificationCount = unreadCount,
                    onOpenNotifications = { showNotificationCenter = true },
                    currentUserEmail = currentUser?.email,
                    currentUserPhotoUrl = currentUser?.photoUrl?.toString(),
                    onLogout = performLogout,
                    onManualSync = {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao(), db.gardenPlanningDao())
                        }
                    },
                    onNavigateToSettings = { isSettingsActive = true },
                    modifier = modifier
                )
            }
            "SEARCH" -> {
                GlobalSearchResultsScreen(
                    viewModel = viewModel,
                    gardenPlanningViewModel = gardenPlanningViewModel,
                    onBack = { viewModel.closeGlobalSearch() },
                    modifier = modifier
                )
            }
            else -> {
                val displayHeaderTitle = when {
                    selectedService.equals("Imported", ignoreCase = true) -> "Imported Plants"
                    selectedService.equals("Rootstocks", ignoreCase = true) -> "Imported Rootstocks"
                    selectedService.equals("Bookings", ignoreCase = true) -> "Bookings"
                    selectedService.equals("Attendance", ignoreCase = true) -> "Worker Attendance"
                    selectedService.equals("Garden Planning", ignoreCase = true) || selectedService.equals("Garden", ignoreCase = true) -> "Garden Planning"
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
                                onNavigateToPaymentReminders = {
                                    showPaymentRemindersDialog = true
                                },
                                onNavigateToSeasonalReminders = {
                                    showSeasonalRemindersDialog = true
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
                                onNavigateToGardenPlanning = {
                                    viewModel.selectServiceCategory("Garden Planning")
                                    gardenPlanningViewModel.resetToNewEntry()
                                },
                                onNavigateToSettings = {
                                    isSettingsActive = true
                                },
                                onNavigateToBusinessInfo = {
                                    showBusinessInfoDialog = true
                                },
                                onNavigateToMessageTemplates = {
                                    isMessageTemplatesActive = true
                                },
                                onNavigateToQrScanner = {
                                    showQrScannerDialog = true
                                },
                                unreadNotificationCount = unreadCount,
                                onOpenNotifications = {
                                    showNotificationCenter = true
                                },
                                currentUserEmail = currentUser?.email,
                                currentUserPhotoUrl = currentUser?.photoUrl?.toString(),
                                onLogout = performLogout,
                                onManualSync = {
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao(), db.gardenPlanningDao())
                                    }
                                },
                                onBack = if (isAttendanceActive) ({ isAttendanceActive = false }) else if (selectedService.equals("Attendance", ignoreCase = true)) ({ viewModel.selectServiceCategory("Local Plants") }) else null
                            )

                            // Segmented toggle header (New Entry / Records) - shown only for Crop Services
                            if (!selectedService.equals("Bookings", ignoreCase = true) && !selectedService.equals("Attendance", ignoreCase = true) && !selectedService.equals("Garden Planning", ignoreCase = true) && !selectedService.equals("Garden", ignoreCase = true)) {
                                AgriSegmentedControl(
                                    selectedMode = viewMode,
                                    onModeSelected = { viewModel.setViewMode(it) },
                                    recordsLabel = "Records ($cropRecordsCount)"
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
                    val mainTabs = remember {
                        listOf(
                            "Local Plants",
                            "Imported",
                            "Rootstocks",
                            "Site Visit",
                            "Pruning",
                            "Garden Planning"
                        )
                    }

                    val initialTabIndex = remember {
                        val idx = mainTabs.indexOfFirst { it.equals(selectedService, ignoreCase = true) }
                        if (idx >= 0) idx else 0
                    }

                    val hazeState = remember { HazeState() }

                    val pagerState = rememberPagerState(
                        initialPage = initialTabIndex,
                        pageCount = { mainTabs.size }
                    )

                    // Synchronize tab when user finishes swiping to a new page
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage in mainTabs.indices) {
                            val targetCategory = mainTabs[pagerState.currentPage]
                            if (!selectedService.equals(targetCategory, ignoreCase = true)) {
                                viewModel.selectServiceCategory(targetCategory)
                                if (targetCategory.equals("Garden Planning", ignoreCase = true)) {
                                    gardenPlanningViewModel.resetToNewEntry()
                                }
                            }
                        }
                    }

                    // Synchronize pager when tab is selected from outside (e.g. bottom nav, dashboard, search)
                    LaunchedEffect(selectedService) {
                        val targetIndex = mainTabs.indexOfFirst { it.equals(selectedService, ignoreCase = true) }
                        if (targetIndex >= 0 && pagerState.currentPage != targetIndex && !pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage(
                                page = targetIndex,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState)
                        ) {
                            when {
                                selectedService.equals("Bookings", ignoreCase = true) -> {
                                    UserBookingsSection(viewModel = userDashboardViewModel)
                                }
                                selectedService.equals("Attendance", ignoreCase = true) -> {
                                    UserAttendanceSection(viewModel = userDashboardViewModel)
                                }
                                else -> {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        key = { mainTabs.getOrElse(it) { "$it" } }
                                    ) { page ->
                                        val tabCategory = mainTabs.getOrElse(page) { "Local Plants" }
                                        if (tabCategory.equals("Garden Planning", ignoreCase = true)) {
                                            com.example.ui.components.GardenPlanningScreen(
                                                viewModel = gardenPlanningViewModel,
                                                onBack = null,
                                                showHeader = false,
                                                isDark = isDark,
                                                themeMode = themeMode,
                                                selectedColorHex = accentColorHex,
                                                onSelectThemeMode = { mode -> viewModel.setThemeMode(context, mode) },
                                                onSelectColorHex = { hex -> viewModel.setAccentColorHex(context, hex) },
                                                searchQuery = searchQuery,
                                                onSearchQueryChange = { newQuery -> viewModel.setSearchQuery(newQuery) },
                                                isSearchActive = isGlobalSearchActive,
                                                onSearchActiveChange = { active -> if (active) viewModel.openGlobalSearch() else viewModel.closeGlobalSearch() },
                                                onToggleSearch = { viewModel.openGlobalSearch() },
                                                onNavigateToAttendance = { isAttendanceActive = true },
                                                onNavigateToBookings = { viewModel.selectServiceCategory("Bookings") },
                                                onNavigateToBackupRestore = { showBackupRestoreDialog = true },
                                                onNavigateToContactDirectory = { showContactDirectoryDialog = true },
                                                onNavigateToPaymentReminders = { showPaymentRemindersDialog = true },
                                                onNavigateToSeasonalReminders = { showSeasonalRemindersDialog = true },
                                                onNavigateToInventory = { showInventoryDialog = true },
                                                onOpenRecycleBin = { showRecycleBinDialog = true },
                                                onNavigateToDashboard = { isDashboardActive = true },
                                                onNavigateToLogin = { isLoginActive = true },
                                                onNavigateToGardenPlanning = { viewModel.selectServiceCategory("Garden Planning") },
                                                unreadNotificationCount = unreadCount,
                                                onOpenNotifications = { showNotificationCenter = true },
                                                currentUserEmail = currentUser?.email,
                                                currentUserPhotoUrl = currentUser?.photoUrl?.toString(),
                                                onLogout = performLogout,
                                                onManualSync = {
                                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        com.example.data.FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao(), db.gardenPlanningDao())
                                                    }
                                                },
                                                onNavigateToSettings = { isSettingsActive = true },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            when (viewMode) {
                                                0 -> FarmerFormScreen(viewModel = viewModel)
                                                else -> FarmerRecordsScreen(viewModel = viewModel)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Standalone floating pill navigation bar hovering above the bottom edge with Liquid Glass material
                        AgriBottomNav(
                            selectedCategory = selectedService,
                            onCategorySelected = { category ->
                                val targetIndex = mainTabs.indexOfFirst { it.equals(category, ignoreCase = true) }
                                viewModel.selectServiceCategory(category)
                                if (category.equals("Garden Planning", ignoreCase = true) || category.equals("Garden", ignoreCase = true)) {
                                    gardenPlanningViewModel.resetToNewEntry()
                                }
                                if (targetIndex >= 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = targetIndex,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                }
                            },
                            hazeState = hazeState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // Full-screen booking confirmation overlay
                        BookingConfirmationOverlay()
                    }
                }
            }
        }
    }

    // Production-ready App Lock authentication overlay when enabled and locked
    if (isAppLockEnabled && isAppLocked) {
        AppLockScreen(
            appLockManager = effectiveAppLockManager,
            modifier = Modifier.fillMaxSize()
        )
    }
}
}

