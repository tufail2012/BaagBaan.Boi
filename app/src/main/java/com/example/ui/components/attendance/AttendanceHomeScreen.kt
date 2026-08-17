package com.example.ui.components.attendance

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.components.BrandedPullToRefreshBox
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceStatus
import com.example.data.Worker
import com.example.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.ui.components.AgriHeader

@Composable
fun AttendanceHomeScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onOpenDailyMarking: () -> Unit,
    onSelectWorker: (Worker) -> Unit,
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
    onNavigateToSeasonalReminders: () -> Unit = {},
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
    onNavigateToSettings: (() -> Unit)? = null
) {
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val monthRecords by viewModel.attendanceForSelectedMonth.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<Worker?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val monthDisplayName = remember(selectedMonthYear) {
        try {
            val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
            val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = sdfIn.parse(selectedMonthYear)
            if (date != null) sdfOut.format(date) else selectedMonthYear
        } catch (_: Exception) {
            selectedMonthYear
        }
    }

    Scaffold(
        topBar = {
            Column {
                AgriHeader(
                    title = "Worker Attendance",
                    themeMode = themeMode,
                    onSelectThemeMode = onSelectThemeMode,
                    selectedColorHex = selectedColorHex,
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
                    onNavigateToSeasonalReminders = onNavigateToSeasonalReminders,
                    onNavigateToDashboard = onNavigateToDashboard,
                    onNavigateToInventory = onNavigateToInventory,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToGardenPlanning = onNavigateToGardenPlanning,
                    onNavigateToSettings = onNavigateToSettings,
                    unreadNotificationCount = unreadNotificationCount,
                    onOpenNotifications = onOpenNotifications,
                    currentUserEmail = currentUserEmail,
                    currentUserPhotoUrl = currentUserPhotoUrl,
                    onLogout = onLogout,
                    onManualSync = onManualSync,
                    onBack = onNavigateBack
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Worker Roster & Summary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onOpenDailyMarking,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("daily_marking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Today", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddWorkerDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_worker_fab")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Worker")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Month Selector Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Month Range:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.setSelectedMonthYear(getPreviousMonth(selectedMonthYear))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = monthDisplayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.setSelectedMonthYear(getNextMonth(selectedMonthYear))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (activeWorkers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Active Workers Registered",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add workers to your roster to start tracking daily attendance.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showAddWorkerDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Worker")
                            }
                        }
                    }
                }
            } else {
                val attendanceHomeListState = rememberLazyListState()
                attendanceHomeListState.rememberScrollHapticFeedback()

                BrandedPullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        if (isRefreshing) return@BrandedPullToRefreshBox
                        isRefreshing = true
                        scope.launch {
                            try {
                                delay(500)
                            } catch (_: Exception) {
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = attendanceHomeListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Roster (${activeWorkers.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                TextButton(onClick = { showAddWorkerDialog = true }) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Worker", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        items(activeWorkers, key = { it.workerId }) { worker ->
                            val workerRecords = monthRecords.filter { it.workerId == worker.workerId }
                            val presentCount = workerRecords.count { it.status == AttendanceStatus.PRESENT }
                            val absentCount = workerRecords.count { it.status == AttendanceStatus.ABSENT }

                            WorkerSummaryCard(
                                worker = worker,
                                presentCount = presentCount,
                                absentCount = absentCount,
                                onClick = { onSelectWorker(worker) },
                                onEditClick = { workerToEdit = worker }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { name, phone, dailyRate, advancePaid ->
                viewModel.addWorker(name, phone, dailyRate, advancePaid)
                showAddWorkerDialog = false
            }
        )
    }

    workerToEdit?.let { worker ->
        EditWorkerDialog(
            worker = worker,
            onDismiss = { workerToEdit = null },
            onConfirm = { updatedWorker ->
                viewModel.updateWorker(updatedWorker)
                workerToEdit = null
            },
            onDeactivate = {
                viewModel.deactivateWorker(worker.workerId, worker.name)
                workerToEdit = null
            }
        )
    }
}

@Composable
fun WorkerSummaryCard(
    worker: Worker,
    presentCount: Int,
    absentCount: Int,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val totalEarnings = presentCount * worker.dailyRate
    val remainingBalance = totalEarnings - worker.advancePaid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("worker_card_${worker.workerId}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (worker.phoneNumber.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = worker.phoneNumber,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (worker.dailyRate > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Rate: ₹${worker.dailyRate.toInt()}/day",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Present count badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "$presentCount Present",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Absent count badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "$absentCount Absent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (worker.dailyRate > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Earned: ₹${totalEarnings.toInt()} • Due: ₹${if (remainingBalance > 0) remainingBalance.toInt() else 0}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Worker",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, dailyRate: Double, advancePaid: Double) -> Unit
) {
    val prefix = "+91 "
    var name by remember { mutableStateOf("") }
    var phoneTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = prefix,
                selection = TextRange(prefix.length)
            )
        )
    }
    var phone by remember { mutableStateOf(prefix) }
    var dailyRateText by remember { mutableStateOf("") }
    var advanceText by remember { mutableStateOf("") }
    var isSavingWorker by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Worker", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Worker Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input")
                )
                OutlinedTextField(
                    value = phoneTextFieldValue,
                    onValueChange = { newValue ->
                        val rawText = newValue.text
                        var cleanDigits = if (rawText.startsWith(prefix)) {
                            rawText.substring(prefix.length).filter { it.isDigit() }
                        } else {
                            rawText.removePrefix("+91").removePrefix("+").filter { it.isDigit() }
                        }
                        if (cleanDigits.length > 10) {
                            cleanDigits = cleanDigits.take(10)
                        }
                        val formattedText = prefix + cleanDigits
                        val targetSelStart = maxOf(prefix.length, minOf(newValue.selection.start, formattedText.length))
                        val targetSelEnd = maxOf(prefix.length, minOf(newValue.selection.end, formattedText.length))
                        phoneTextFieldValue = TextFieldValue(
                            text = formattedText,
                            selection = TextRange(targetSelStart, targetSelEnd)
                        )
                        phone = formattedText
                    },
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_phone_input")
                )
                OutlinedTextField(
                    value = dailyRateText,
                    onValueChange = { dailyRateText = it },
                    label = { Text("Add Rate (Per-day Wage ₹) *") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_rate_input")
                )
                OutlinedTextField(
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    label = { Text("Initial Advance / Installment Paid (Optional ₹)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_advance_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && !isSavingWorker) {
                        isSavingWorker = true
                        coroutineScope.launch {
                            try {
                                val rate = dailyRateText.toDoubleOrNull() ?: 0.0
                                val advance = advanceText.toDoubleOrNull() ?: 0.0
                                onConfirm(name, phone, rate, advance)
                            } catch (t: Throwable) {
                                android.util.Log.e("AddWorker", "Error adding worker", t)
                            } finally {
                                isSavingWorker = false
                            }
                        }
                    }
                },
                enabled = !isSavingWorker && name.isNotBlank(),
                modifier = Modifier.testTag("add_worker_confirm_button")
            ) {
                if (isSavingWorker) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Worker")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditWorkerDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onConfirm: (Worker) -> Unit,
    onDeactivate: () -> Unit
) {
    val context = LocalContext.current
    val prefix = "+91 "
    val initialPhone = remember(worker.phoneNumber) {
        val raw = worker.phoneNumber
        if (raw.isBlank()) {
            prefix
        } else if (!raw.startsWith("+91")) {
            var digits = raw.replace("[^0-9]".toRegex(), "")
            if (digits.startsWith("91") && digits.length > 10) {
                digits = digits.substring(digits.length - 10)
            }
            if (digits.length > 10) digits = digits.takeLast(10)
            if (digits.isNotEmpty()) "+91 $digits" else prefix
        } else {
            raw
        }
    }
    var name by remember { mutableStateOf(worker.name) }
    var phoneTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialPhone,
                selection = TextRange(initialPhone.length)
            )
        )
    }
    var phone by remember { mutableStateOf(initialPhone) }
    var dailyRateText by remember { mutableStateOf(if (worker.dailyRate > 0) worker.dailyRate.toInt().toString() else "") }
    var advanceText by remember { mutableStateOf(if (worker.advancePaid > 0) worker.advancePaid.toInt().toString() else "") }
    var showConfirmDeactivate by remember { mutableStateOf(false) }
    var isSavingWorker by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    if (showConfirmDeactivate) {
        AlertDialog(
            onDismissRequest = { showConfirmDeactivate = false },
            title = { Text("Deactivate Worker", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to deactivate '${worker.name}'? They will be removed from active daily marking, but their past attendance records will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeactivate = false
                        onDeactivate()
                    }
                ) {
                    Text("Deactivate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeactivate = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Worker Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Worker Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneTextFieldValue,
                        onValueChange = { newValue ->
                            val rawText = newValue.text
                            var cleanDigits = if (rawText.startsWith(prefix)) {
                                rawText.substring(prefix.length).filter { it.isDigit() }
                            } else {
                                rawText.removePrefix("+91").removePrefix("+").filter { it.isDigit() }
                            }
                            if (cleanDigits.length > 10) {
                                cleanDigits = cleanDigits.take(10)
                            }
                            val formattedText = prefix + cleanDigits
                            val targetSelStart = maxOf(prefix.length, minOf(newValue.selection.start, formattedText.length))
                            val targetSelEnd = maxOf(prefix.length, minOf(newValue.selection.end, formattedText.length))
                            phoneTextFieldValue = TextFieldValue(
                                text = formattedText,
                                selection = TextRange(targetSelStart, targetSelEnd)
                            )
                            phone = formattedText
                        },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
                                    if (cleanPhone.isNotBlank()) {
                                        val targetPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$targetPhone"))
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("whatsapp_worker_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "WhatsApp Chat",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_worker_phone_input")
                    )
                    OutlinedTextField(
                        value = dailyRateText,
                        onValueChange = { dailyRateText = it },
                        label = { Text("Daily Wage Rate (₹/day)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = advanceText,
                        onValueChange = { advanceText = it },
                        label = { Text("Total Advance Paid (₹)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { showConfirmDeactivate = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Deactivate Worker", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && !isSavingWorker) {
                            isSavingWorker = true
                            coroutineScope.launch {
                                try {
                                    val rate = dailyRateText.toDoubleOrNull() ?: 0.0
                                    val advance = advanceText.toDoubleOrNull() ?: 0.0
                                    onConfirm(
                                        worker.copy(
                                            name = name.trim(),
                                            phoneNumber = phone.trim(),
                                            dailyRate = rate,
                                            advancePaid = advance
                                        )
                                    )
                                } catch (t: Throwable) {
                                    android.util.Log.e("EditWorker", "Error updating worker", t)
                                } finally {
                                    isSavingWorker = false
                                }
                            }
                        }
                    },
                    enabled = !isSavingWorker && name.isNotBlank()
                ) {
                    if (isSavingWorker) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getPreviousMonth(yearMonth: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonth) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, -1)
        sdf.format(cal.time)
    } catch (_: Exception) {
        yearMonth
    }
}

private fun getNextMonth(yearMonth: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonth) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, 1)
        sdf.format(cal.time)
    } catch (_: Exception) {
        yearMonth
    }
}
