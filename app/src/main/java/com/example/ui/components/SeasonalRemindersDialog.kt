package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SeasonalTask
import com.example.data.SeasonalTaskRepository
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

private val MONTH_NAMES = DateFormatSymbols().months.take(12)
private val SHORT_MONTH_NAMES = DateFormatSymbols().shortMonths.take(12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalRemindersDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by SeasonalTaskRepository.tasks.collectAsState()

    var taskToEdit by remember { mutableStateOf<SeasonalTask?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<SeasonalTask?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val isDark = isAppInDarkMode()
    val seasonalAccent = getSectionAccentColor("Seasonal Reminders")
    val seasonalBgBrush = remember(isDark, seasonalAccent) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF0D1B2A),
                    seasonalAccent.copy(alpha = 0.05f),
                    Color(0xFF060911)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    seasonalAccent.copy(alpha = 0.035f),
                    Color(0xFFF1F5F9),
                    Color(0xFFFFFFFF)
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(seasonalBgBrush)
                .testTag("seasonal_reminders_dialog")
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .glassCardBackground(
                                isDark = isDark,
                                accentColor = seasonalAccent,
                                shape = CircleShape
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("seasonal_reminders_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = seasonalAccent
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(seasonalAccent.copy(alpha = if (isDark) 0.25f else 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Park,
                                        contentDescription = null,
                                        tint = seasonalAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Seasonal Reminders",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tasks.count { it.isEnabled }} active • ${tasks.size} annual tasks",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("seasonal_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Seeds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .widthIn(min = 220.dp, max = 260.dp)
                                    .fillMaxWidth(0.65f)
                                    .height(48.dp)
                                    .testTag("seasonal_reminders_done_button")
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Info Banner
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCardBackground(
                                cornerRadius = 16.dp,
                                accentColor = seasonalAccent,
                                isDark = isDark
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = seasonalAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Set annual dates for pruning, grafting, spraying, fertilizing, or harvest. Alarms automatically recur every year and survive device reboots.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent Horizontal Add Task Button
                    Button(
                        onClick = {
                            taskToEdit = SeasonalTask(category = "Custom Task")
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("seasonal_add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Task",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section Heading
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Orchard Schedule (${tasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Task List
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No seasonal tasks configured",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Add Task' or 'Reset Seeds' to get started",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                SeasonalTaskCard(
                                    task = task,
                                    isDark = isDark,
                                    seasonalAccent = seasonalAccent,
                                    onEdit = {
                                        taskToEdit = task
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        taskToDelete = task
                                    },
                                    onToggleEnabled = { isEnabled ->
                                        scope.launch {
                                            val updated = task.copy(isEnabled = isEnabled)
                                            SeasonalTaskRepository.addOrUpdateTask(updated, context)
                                        }
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit/Add Task Dialog
    if (showEditDialog && taskToEdit != null) {
        SeasonalTaskEditDialog(
            task = taskToEdit!!,
            onDismiss = {
                showEditDialog = false
                taskToEdit = null
            },
            onSave = { updatedTask ->
                scope.launch {
                    SeasonalTaskRepository.addOrUpdateTask(updatedTask, context)
                    Toast.makeText(context, "Seasonal reminder saved!", Toast.LENGTH_SHORT).show()
                }
                showEditDialog = false
                taskToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Seasonal Task?") },
            text = { Text("Are you sure you want to remove '${taskToDelete?.title}' from your annual schedule?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = taskToDelete?.id
                        taskToDelete = null
                        if (id != null) {
                            scope.launch {
                                SeasonalTaskRepository.deleteTask(id, context)
                                Toast.makeText(context, "Task removed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("seasonal_delete_confirm_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to Default Seeds?") },
            text = { Text("This will restore the 5 default orchard categories (Pruning, Grafting, Spraying, Fertilizing, Harvest). Your current edits will be replaced.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        scope.launch {
                            SeasonalTaskRepository.saveTasks(SeasonalTask.DEFAULT_SEEDS, context)
                            Toast.makeText(context, "Restored default seasonal categories!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("seasonal_reset_confirm_button")
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SeasonalTaskCard(
    task: SeasonalTask,
    isDark: Boolean,
    seasonalAccent: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val hasDate = task.reminderMonth != null && task.reminderMonth in 1..12 &&
            task.reminderDay != null && task.reminderDay in 1..31

    val dateFormatted = if (hasDate) {
        val monthStr = MONTH_NAMES[(task.reminderMonth!! - 1).coerceIn(0, 11)]
        "$monthStr ${task.reminderDay}"
    } else {
        "No date set"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                cornerRadius = 18.dp,
                accentColor = seasonalAccent,
                isDark = isDark
            )
            .clickable { onEdit() }
            .testTag("seasonal_task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Chip + Title
                Column(modifier = Modifier.weight(1f)) {
                    if (task.category.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDark) seasonalAccent.copy(alpha = 0.22f)
                                    else seasonalAccent.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) seasonalAccent.copy(alpha = 0.45f) else seasonalAccent.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = task.title.ifBlank { task.category.ifBlank { "Untitled Task" } },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (task.isEnabled) {
                            if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Switch + Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = task.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = seasonalAccent,
                            uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                            uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("seasonal_task_switch_${task.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            tint = seasonalAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Notes
            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.notes,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date & Recurrence Metadata Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (hasDate) {
                                if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f)
                                else Color(0xFFF1F5F9).copy(alpha = 0.95f)
                            } else {
                                if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f)
                                else Color(0xFFF8FAFC).copy(alpha = 0.9f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (hasDate) {
                                if (isDark) seasonalAccent.copy(alpha = 0.4f) else seasonalAccent.copy(alpha = 0.3f)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (hasDate) Icons.Default.Alarm else Icons.Default.AlarmOff,
                        contentDescription = null,
                        tint = if (hasDate) seasonalAccent else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasDate) {
                            if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        }
                    )
                }

                if (task.recurring && hasDate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = "Yearly Recurring",
                            tint = seasonalAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Repeats yearly",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonalTaskEditDialog(
    task: SeasonalTask,
    onDismiss: () -> Unit,
    onSave: (SeasonalTask) -> Unit
) {
    var title by remember { mutableStateOf(task.title.ifBlank { task.category }) }
    var category by remember { mutableStateOf(task.category) }
    var notes by remember { mutableStateOf(task.notes) }
    var selectedMonth by remember { mutableStateOf(task.reminderMonth) }
    var selectedDay by remember { mutableStateOf(task.reminderDay) }
    var isRecurring by remember { mutableStateOf(task.recurring) }
    var isEnabled by remember { mutableStateOf(task.isEnabled) }

    var showMonthPicker by remember { mutableStateOf(false) }

    val presetCategories = listOf(
        "Dormant Pruning",
        "Grafting Window",
        "Spray Schedule",
        "Fertilizing",
        "Harvest",
        "Irrigation",
        "Thinning",
        "Cover Crop"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .testTag("seasonal_task_edit_dialog"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (task.id.startsWith("seed_")) "Edit Seasonal Task" else "Seasonal Task",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Category selection chips
                item {
                    Text(
                        text = "Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetCategories.forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = {
                                    category = cat
                                    if (title.isBlank() || presetCategories.contains(title)) {
                                        title = cat
                                    }
                                },
                                label = { Text(cat, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Task Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Dormant Oil Spray (HMO)") },
                        shape = RoundedCornerShape(14.dp),
                        colors = elevatedInputFieldColors(accentColor = getSectionAccentColor("Seasonal Reminders")),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seasonal_title_input")
                    )
                }

                // Annual Timing / Date Selector (Month + Day)
                item {
                    Text(
                        text = "Annual Timing (Month & Day)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val dateText = if (selectedMonth != null && selectedDay != null) {
                        "${MONTH_NAMES[(selectedMonth!! - 1).coerceIn(0, 11)]} $selectedDay (Every Year)"
                    } else {
                        "Tap to set reminder date"
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMonthPicker = !showMonthPicker }
                            .testTag("seasonal_date_selector_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = dateText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (selectedMonth != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (selectedMonth != null) "Yearly alarm fires automatically" else "No annual date selected",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (selectedMonth != null && selectedDay != null) {
                                TextButton(
                                    onClick = {
                                        selectedMonth = null
                                        selectedDay = null
                                    }
                                ) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Inline Month & Day Picker if open
                if (showMonthPicker) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "1. Select Month",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (m in 1..12) {
                                        val isSel = selectedMonth == m
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable {
                                                    selectedMonth = m
                                                    if (selectedDay == null) selectedDay = 1
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = SHORT_MONTH_NAMES[m - 1],
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (selectedMonth != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "2. Select Day in ${MONTH_NAMES[(selectedMonth!! - 1).coerceIn(0, 11)]}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val maxDays = Calendar.getInstance().apply {
                                        set(Calendar.MONTH, (selectedMonth!! - 1).coerceIn(0, 11))
                                    }.getActualMaximum(Calendar.DAY_OF_MONTH)

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (d in 1..maxDays) {
                                            val isSel = selectedDay == d
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSel) MaterialTheme.colorScheme.secondary
                                                        else MaterialTheme.colorScheme.surface
                                                    )
                                                    .clickable { selectedDay = d }
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$d",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes Field
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Timing Details & Notes") },
                        placeholder = { Text("e.g. Apply 2% HMO spray right after pruning when daytime temperature stays above 5°C.") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = elevatedInputFieldColors(accentColor = getSectionAccentColor("Seasonal Reminders")),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seasonal_notes_input")
                    )
                }

                // Toggles: Recurring & Enabled
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Recurring Annual Alarm", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Recompute and alert every year on this date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    modifier = Modifier.testTag("seasonal_recurring_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Enable Reminder", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Turn alarm active/inactive without deleting", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it },
                                    modifier = Modifier.testTag("seasonal_enabled_switch")
                                )
                            }
                        }
                    }
                }

                // Action Buttons (Cancel / Save)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val updated = task.copy(
                                    category = category.trim(),
                                    title = title.trim().ifBlank { category.trim().ifBlank { "Seasonal Task" } },
                                    notes = notes.trim(),
                                    reminderMonth = selectedMonth,
                                    reminderDay = selectedDay,
                                    recurring = isRecurring,
                                    isEnabled = isEnabled,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(updated)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("seasonal_save_task_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Task")
                        }
                    }
                }
            }
        }
    }
}
}
