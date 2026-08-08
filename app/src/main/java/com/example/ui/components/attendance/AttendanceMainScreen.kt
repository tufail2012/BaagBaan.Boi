package com.example.ui.components.attendance

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
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AttendanceViewScreen.HOME) }
    val selectedWorker by viewModel.selectedWorker.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

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
                    }
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
