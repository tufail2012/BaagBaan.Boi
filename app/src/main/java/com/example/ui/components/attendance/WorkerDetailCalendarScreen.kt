package com.example.ui.components.attendance

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.CountUpText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdvancePayment
import com.example.data.AttendanceStatus
import com.example.data.Worker
import com.example.ui.AttendanceViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.glassCardBackground
import com.example.ui.components.frostedGlassChrome
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun WorkerDetailCalendarScreen(
    viewModel: AttendanceViewModel,
    worker: Worker,
    onNavigateBack: () -> Unit
) {
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val monthRecords by viewModel.attendanceForSelectedMonth.collectAsState()
    val advancePayments by viewModel.selectedWorkerAdvancePayments.collectAsState()
    val todayDate = viewModel.getTodayString()

    var selectedMarkMode by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var showDigitalReceipt by remember { mutableStateOf(false) }

    val workerMonthRecords = monthRecords.filter { it.workerId == worker.workerId }

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

    val calendarDays = remember(selectedMonthYear) {
        getCalendarDaysForMonth(selectedMonthYear)
    }

    val presentDays = workerMonthRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentDays = workerMonthRecords.count { it.status == AttendanceStatus.ABSENT }

    val isDark = isSystemInDarkTheme()
    val workerHazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .frostedGlassChrome(
                        hazeState = workerHazeState,
                        isDark = isDark,
                        accentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(percent = 50)
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
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("worker_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Attendance History & Calendar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = workerHazeState)
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Worker Payroll & Attendance Summary Section
            WorkerPayrollSummarySection(
                worker = worker,
                presentDays = presentDays,
                absentDays = absentDays,
                advancePayments = advancePayments,
                todayDateStr = todayDate,
                onRecordAdvance = { amount, date ->
                    viewModel.recordAdvancePayment(worker, amount, date)
                },
                onDeleteAdvance = { payment ->
                    viewModel.deleteAdvancePayment(payment)
                },
                onUpdateRate = { newRate ->
                    viewModel.updateWorkerRate(worker, newRate)
                },
                onShowDigitalReceipt = {
                    showDigitalReceipt = true
                }
            )

            if (showDigitalReceipt) {
                DigitalReceiptDialog(
                    worker = worker,
                    presentDays = presentDays,
                    absentDays = absentDays,
                    advancePayments = advancePayments,
                    onDismiss = { showDigitalReceipt = false }
                )
            }

            // Month Navigation Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.setSelectedMonthYear(getPreviousMonth(selectedMonthYear))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = monthDisplayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(
                            onClick = {
                                viewModel.setSelectedMonthYear(getNextMonth(selectedMonthYear))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Monthly Stats Chips (Selectable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMarkMode = AttendanceStatus.PRESENT }
                                .testTag("indicator_days_present"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMarkMode == AttendanceStatus.PRESENT) {
                                Color(0xFFE8F5E9)
                            } else {
                                Color(0xFFE8F5E9).copy(alpha = 0.5f)
                            },
                            border = if (selectedMarkMode == AttendanceStatus.PRESENT) {
                                BorderStroke(2.dp, Color(0xFF2E7D32))
                            } else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$presentDays",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    if (selectedMarkMode == AttendanceStatus.PRESENT) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Days Present",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedMarkMode == AttendanceStatus.PRESENT) FontWeight.Bold else FontWeight.Normal,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMarkMode = AttendanceStatus.ABSENT }
                                .testTag("indicator_days_absent"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMarkMode == AttendanceStatus.ABSENT) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            },
                            border = if (selectedMarkMode == AttendanceStatus.ABSENT) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$absentDays",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    if (selectedMarkMode == AttendanceStatus.ABSENT) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Days Absent",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedMarkMode == AttendanceStatus.ABSENT) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedMarkMode == AttendanceStatus.PRESENT)
                            "Mode: Marking Present • Tap calendar dates to mark Present"
                        else
                            "Mode: Marking Absent • Tap calendar dates to mark Absent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedMarkMode == AttendanceStatus.PRESENT)
                            Color(0xFF2E7D32)
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of week header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        userScrollEnabled = false
                    ) {
                        items(calendarDays) { calDay ->
                            if (calDay.dateStr == null) {
                                // Blank space before first day of month
                                Box(modifier = Modifier.aspectRatio(1f))
                            } else {
                                val record = workerMonthRecords.find { it.date == calDay.dateStr }
                                val status = record?.status
                                val isToday = calDay.dateStr == todayDate
                                val isFuture = calDay.dateStr > todayDate

                                val greenBg = Color(0xFFE8F5E9)
                                val greenText = Color(0xFF1B5E20)
                                val greenDot = Color(0xFF2E7D32)

                                val redBg = MaterialTheme.colorScheme.errorContainer
                                val redText = MaterialTheme.colorScheme.error
                                val redDot = MaterialTheme.colorScheme.error

                                val cellBgColor = when {
                                    status == AttendanceStatus.PRESENT -> greenBg
                                    status == AttendanceStatus.ABSENT -> redBg
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }

                                val cellTextColor = when {
                                    status == AttendanceStatus.PRESENT -> greenText
                                    status == AttendanceStatus.ABSENT -> redText
                                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isFuture) {
                                            if (status == selectedMarkMode) {
                                                viewModel.deleteAttendanceRecord(worker.workerId, calDay.dateStr)
                                            } else {
                                                viewModel.setAttendanceStatus(worker.workerId, calDay.dateStr, selectedMarkMode)
                                            }
                                        },
                                    color = cellBgColor,
                                    shape = RoundedCornerShape(8.dp),
                                    border = when {
                                        isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        status == AttendanceStatus.PRESENT -> BorderStroke(1.dp, greenDot)
                                        status == AttendanceStatus.ABSENT -> BorderStroke(1.dp, redDot)
                                        else -> null
                                    }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${calDay.dayOfMonth}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isToday || status != null) FontWeight.Bold else FontWeight.Normal,
                                                color = cellTextColor
                                            )
                                            if (status == AttendanceStatus.PRESENT) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(greenDot)
                                                )
                                            } else if (status == AttendanceStatus.ABSENT) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(redDot)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(label = "Present", color = Color(0xFF2E7D32))
                        LegendItem(label = "Absent", color = MaterialTheme.colorScheme.error)
                        LegendItem(label = "Unmarked", color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class CalendarDay(
    val dayOfMonth: Int,
    val dateStr: String?
)

private fun getCalendarDaysForMonth(yearMonthStr: String): List<CalendarDay> {
    val list = mutableListOf<CalendarDay>()
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val parts = yearMonthStr.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1

        cal.set(year, month, 1)

        // Monday = 2 in Java Calendar, Sunday = 1. Let's align so Monday = 0
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek += 7

        // Pad blank days
        for (i in 0 until firstDayOfWeek) {
            list.add(CalendarDay(0, null))
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDays) {
            cal.set(year, month, day)
            val dateStr = sdf.format(cal.time)
            list.add(CalendarDay(day, dateStr))
        }
    } catch (_: Exception) {}
    return list
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

private fun formatDateToDDMMYYYY(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        if (dateStr.contains("-")) {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                "$day/$month/$year"
            } else dateStr
        } else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun WorkerPayrollSummarySection(
    worker: Worker,
    presentDays: Int,
    absentDays: Int,
    advancePayments: List<AdvancePayment>,
    todayDateStr: String,
    onRecordAdvance: (amount: Double, date: String) -> Unit,
    onDeleteAdvance: (payment: AdvancePayment) -> Unit,
    onUpdateRate: (newRate: Double) -> Unit,
    onShowDigitalReceipt: () -> Unit
) {
    val context = LocalContext.current
    var advanceInputText by remember { mutableStateOf("") }
    var advanceDateText by remember { mutableStateOf(formatDateToDDMMYYYY(todayDateStr)) }
    var rateInputText by remember { mutableStateOf(if (worker.dailyRate > 0) worker.dailyRate.toInt().toString() else "") }
    var showEditRateDialog by remember { mutableStateOf(false) }
    var isSavingAdvance by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    fun showDatePicker() {
        val cal = Calendar.getInstance()
        try {
            if (advanceDateText.contains("/")) {
                val parts = advanceDateText.split("/")
                if (parts.size == 3) {
                    cal.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.YEAR, parts[2].toInt())
                }
            }
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDay = String.format(Locale.US, "%02d", dayOfMonth)
                val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                advanceDateText = "$formattedDay/$formattedMonth/$year"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val dailyRate = worker.dailyRate
    val totalEarnings = presentDays * dailyRate
    val advancePaid = worker.advancePaid
    val remainingBalance = totalEarnings - advancePaid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("worker_payroll_summary_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Title Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Payroll & Wage Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Worker: ${worker.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Square-shaped containers side-by-side for Digital Receipt format and Edit Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Digital Receipt Square Container
                Surface(
                    onClick = onShowDigitalReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("digital_receipt_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Digital Receipt",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Digital Receipt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "View / Export PDF",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Edit Rate Square Container
                Surface(
                    onClick = { showEditRateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("edit_rate_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Wage Rate",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Wage Rate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (dailyRate > 0) "₹${dailyRate.toInt()}/day" else "Set Rate",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Summary Grid Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Daily Wage Rate
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Daily Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (dailyRate > 0) "₹${dailyRate.toInt()}/day" else "Not Set",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("${presentDays} Days Present", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Total Earnings
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Total Earnings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CountUpText(
                            targetValue = totalEarnings,
                            formatter = { "₹${it.toInt()}" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (dailyRate > 0) "${presentDays} × ₹${dailyRate.toInt()}" else "Rate required",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Advances Paid
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Advances Received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CountUpText(
                            targetValue = advancePaid,
                            formatter = { "₹${it.toInt()}" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Total Advances", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Remaining Balance Due
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Remaining Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CountUpText(
                            targetValue = if (remainingBalance > 0) remainingBalance else 0.0,
                            formatter = { "₹${it.toInt()}" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (remainingBalance > 0) "Earnings - Advances" else "Fully Settled",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Field to Record Advance Payment / Installments Received with Date
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Record Advance / Installment Received",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = advanceDateText,
                            onValueChange = { advanceDateText = it },
                            label = { Text("Date", fontSize = 10.sp) },
                            placeholder = { Text("Select Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            leadingIcon = {
                                IconButton(
                                    onClick = { showDatePicker() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("record_advance_date_input")
                        )

                        OutlinedTextField(
                            value = advanceInputText,
                            onValueChange = { advanceInputText = it },
                            label = { Text("Amount (₹)", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("record_advance_input")
                        )

                        Button(
                            onClick = {
                                val addedAmt = advanceInputText.toDoubleOrNull() ?: 0.0
                                if (addedAmt > 0 && advanceDateText.isNotBlank() && !isSavingAdvance) {
                                    isSavingAdvance = true
                                    coroutineScope.launch {
                                        try {
                                            onRecordAdvance(addedAmt, advanceDateText)
                                            advanceInputText = ""
                                        } catch (t: Throwable) {
                                            android.util.Log.e("WorkerDetail", "Error saving advance", t)
                                        } finally {
                                            isSavingAdvance = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSavingAdvance,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_advance_button")
                        ) {
                            if (isSavingAdvance) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Recorded Advance Payments History
                    if (advancePayments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Advance Payment Records (${advancePayments.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            advancePayments.forEach { payment ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = payment.date,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "₹${payment.amount.toInt()}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )

                                            IconButton(
                                                onClick = { onDeleteAdvance(payment) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("delete_advance_${payment.paymentId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Advance",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditRateDialog) {
        AlertDialog(
            onDismissRequest = { showEditRateDialog = false },
            title = { Text("Update Per-Day Wage Rate", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter per-day wage rate for ${worker.name}:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = rateInputText,
                        onValueChange = { rateInputText = it },
                        label = { Text("Daily Rate (₹/day)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_rate_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = rateInputText.toDoubleOrNull() ?: 0.0
                        onUpdateRate(rate)
                        showEditRateDialog = false
                    },
                    modifier = Modifier.testTag("save_rate_button")
                ) {
                    Text("Save Rate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
