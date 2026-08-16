package com.example.ui.components.attendance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.Worker
import com.example.ui.AttendanceViewModel

enum class AttendanceViewScreen {
    HOME,
    DAILY_MARKING,
    WORKER_DETAIL
}

@Composable
fun AttendanceMainScreen(
    viewModel: AttendanceViewModel,
    onNavigateBackToMain: () -> Unit,
    themeMode: com.example.ui.AppThemeMode = com.example.ui.AppThemeMode.SYSTEM,
    selectedColorHex: String = "#D32F2F",
    onSelectThemeMode: (com.example.ui.AppThemeMode) -> Unit = {},
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
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AttendanceViewScreen.HOME) }
    val selectedWorker by viewModel.selectedWorker.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    BackHandler {
        when (currentScreen) {
            AttendanceViewScreen.DAILY_MARKING,
            AttendanceViewScreen.WORKER_DETAIL -> {
                currentScreen = AttendanceViewScreen.HOME
            }
            AttendanceViewScreen.HOME -> {
                onNavigateBackToMain()
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            AttendanceViewScreen.HOME -> {
                AttendanceHomeScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBackToMain,
                    onOpenDailyMarking = {
                        currentScreen = AttendanceViewScreen.DAILY_MARKING
                    },
                    onSelectWorker = { worker ->
                        viewModel.setSelectedWorker(worker)
                        currentScreen = AttendanceViewScreen.WORKER_DETAIL
                    },
                    themeMode = themeMode,
                    selectedColorHex = selectedColorHex,
                    onSelectThemeMode = onSelectThemeMode,
                    onSelectColorHex = onSelectColorHex,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = onSearchActiveChange,
                    onToggleSearch = onToggleSearch,
                    onNavigateToAttendance = onNavigateToAttendance,
                    onNavigateToBookings = onNavigateToBookings,
                    onNavigateToBackupRestore = onNavigateToBackupRestore,
                    onNavigateToContactDirectory = onNavigateToContactDirectory,
                    onNavigateToPaymentReminders = onNavigateToPaymentReminders,
                    onNavigateToDashboard = onNavigateToDashboard,
                    onNavigateToInventory = onNavigateToInventory,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                    unreadNotificationCount = unreadNotificationCount,
                    onOpenNotifications = onOpenNotifications,
                    currentUserEmail = currentUserEmail,
                    currentUserPhotoUrl = currentUserPhotoUrl,
                    onLogout = onLogout,
                    onManualSync = onManualSync,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            AttendanceViewScreen.DAILY_MARKING -> {
                DailyMarkingScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = AttendanceViewScreen.HOME
                    }
                )
            }
            AttendanceViewScreen.WORKER_DETAIL -> {
                selectedWorker?.let { worker ->
                    WorkerDetailCalendarScreen(
                        viewModel = viewModel,
                        worker = worker,
                        onNavigateBack = {
                            currentScreen = AttendanceViewScreen.HOME
                        }
                    )
                } ?: run {
                    LaunchedEffect(Unit) {
                        currentScreen = AttendanceViewScreen.HOME
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
