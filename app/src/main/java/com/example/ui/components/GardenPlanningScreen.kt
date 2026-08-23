package com.example.ui.components

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.components.BrandedPullToRefreshBox

import androidx.compose.material3.LocalTextStyle

import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.GardenPlanningEntry
import com.example.ui.GardenPlanningViewModel
import com.example.util.MessagePreviewComponent
import com.example.util.ReceiptData
import com.example.util.ReceiptGenerator
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Data model for installment payment records
data class GardenInstallment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val paymentMode: String = "Cash",
    val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val note: String = ""
)

fun parseGardenInstallments(jsonStr: String, currentPaid: Double = 0.0, bookingDate: String = ""): List<GardenInstallment> {
    if (jsonStr.isBlank()) {
        if (currentPaid > 0) {
            return listOf(
                GardenInstallment(
                    id = "init_1",
                    amount = currentPaid,
                    date = bookingDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                    paymentMode = "Cash",
                    note = "Initial Advance Payment"
                )
            )
        }
        return emptyList()
    }
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<GardenInstallment>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                GardenInstallment(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    amount = obj.optDouble("amount", 0.0),
                    paymentMode = obj.optString("paymentMode", "Cash"),
                    date = obj.optString("date", ""),
                    note = obj.optString("note", "")
                )
            )
        }
        list
    } catch (e: Exception) {
        if (currentPaid > 0) {
            listOf(
                GardenInstallment(
                    id = "init_1",
                    amount = currentPaid,
                    date = bookingDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                    paymentMode = "Cash",
                    note = "Initial Advance Payment"
                )
            )
        } else emptyList()
    }
}

fun serializeGardenInstallments(list: List<GardenInstallment>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("amount", item.amount)
        obj.put("paymentMode", item.paymentMode)
        obj.put("date", item.date)
        obj.put("note", item.note)
        array.put(obj)
    }
    return array.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenPlanningScreen(
    viewModel: GardenPlanningViewModel,
    onBack: (() -> Unit)? = null,
    isDark: Boolean = isAppInDarkMode(),
    showHeader: Boolean = true,
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
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()

    val userMessage by viewModel.userMessage.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val filteredEntries by viewModel.filteredEntries.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.userMessage.value = null
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("User Profile")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Baagbaan Horticulturist Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Garden Planning Module Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: Connected & Verified", fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Notifications")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Garden Planning System Alerts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("• All garden planning entries are saved locally & backed up.", fontSize = 12.sp)
                    Text("• Generate digital receipts & share via WhatsApp or SMS anytime.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Consistent Main App Header
            if (showHeader) {
                AgriHeader(
                title = "Garden Planning",
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
                unreadNotificationCount = unreadNotificationCount,
                onOpenNotifications = onOpenNotifications,
                currentUserEmail = currentUserEmail,
                currentUserPhotoUrl = currentUserPhotoUrl,
                onLogout = onLogout,
                onManualSync = onManualSync,
                onNavigateToSettings = onNavigateToSettings,
                onBack = null
            )
            }

            // Unified Segmented Control matching the app design system
            val isEditing = viewModel.editingEntryId.collectAsState().value != null
            AgriSegmentedControl(
                selectedMode = selectedTabIndex,
                onModeSelected = { viewModel.selectedTabIndex.value = it },
                newEntryLabel = if (isEditing) "Edit Entry" else "New Entry",
                recordsLabel = "Records (${allEntries.size})"
            )

            when (selectedTabIndex) {
                0 -> {
                    GardenPlanningFormTab(
                        viewModel = viewModel,
                        isDark = isDark,
                        onSaved = { viewModel.selectedTabIndex.value = 1 }
                    )
                }
                1 -> {
                    GardenPlanningRecordsTab(
                        viewModel = viewModel,
                        entries = filteredEntries,
                        isDark = isDark,
                        onEdit = { entry ->
                            viewModel.loadEntryForEdit(entry)
                            viewModel.selectedTabIndex.value = 0
                        },
                        onAddNewEntry = {
                            viewModel.clearForm()
                            viewModel.selectedTabIndex.value = 0
                        }
                    )
                }
            }
        }
    }
}

enum class LastEditedField { AREA, TOTAL_PLANTS, NONE }

@Composable
fun GardenPlanningFormTab(
    viewModel: GardenPlanningViewModel,
    isDark: Boolean,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val textFieldShape = RoundedCornerShape(16.dp)
    val pillShape = RoundedCornerShape(24.dp)

    var lastEdited by remember { mutableStateOf(LastEditedField.NONE) }

    val serialNumber by viewModel.serialNumber.collectAsState()
    val isSerialLocked by viewModel.isSerialLocked.collectAsState()
    val farmerName by viewModel.farmerName.collectAsState()
    val farmerAddress by viewModel.farmerAddress.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()
    val totalKanalArea by viewModel.totalKanalArea.collectAsState()
    val plantsPerKanal by viewModel.plantsPerKanal.collectAsState()
    val totalPlants by viewModel.totalPlants.collectAsState()
    val costPerPlant by viewModel.costPerPlant.collectAsState()
    val plantVariety by viewModel.plantVariety.collectAsState()
    val rootStock by viewModel.rootStock.collectAsState()
    val saplingAge by viewModel.saplingAge.collectAsState()
    val feathers by viewModel.feathers.collectAsState()
    val plantOrigin by viewModel.plantOrigin.collectAsState()
    val amountPaid by viewModel.amountPaid.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val bookingDate by viewModel.bookingDate.collectAsState()
    val expectedDelivery by viewModel.expectedDelivery.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val editingEntryId by viewModel.editingEntryId.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    // Contact Number +91 Prefix Formatting & Selection
    val prefix = "+91 "
    var contactTextFieldValue by remember {
        val initialText = if (contactNumber.isBlank()) prefix else contactNumber
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(if (initialText.startsWith(prefix)) maxOf(prefix.length, initialText.length) else initialText.length)
            )
        )
    }

    LaunchedEffect(contactNumber) {
        val targetText = if (contactNumber.isBlank()) prefix else contactNumber
        if (targetText != contactTextFieldValue.text) {
            val cursorIndex = if (targetText.startsWith(prefix)) maxOf(prefix.length, targetText.length) else targetText.length
            contactTextFieldValue = TextFieldValue(
                text = targetText,
                selection = TextRange(cursorIndex)
            )
        }
    }

    var bookingDateTFV by remember {
        mutableStateOf(TextFieldValue(text = bookingDate, selection = TextRange(bookingDate.length)))
    }
    LaunchedEffect(bookingDate) {
        if (bookingDate != bookingDateTFV.text) {
            bookingDateTFV = TextFieldValue(text = bookingDate, selection = TextRange(bookingDate.length))
        }
    }

    var expectedDeliveryTFV by remember {
        mutableStateOf(TextFieldValue(text = expectedDelivery, selection = TextRange(expectedDelivery.length)))
    }
    LaunchedEffect(expectedDelivery) {
        if (expectedDelivery != expectedDeliveryTFV.text) {
            expectedDeliveryTFV = TextFieldValue(text = expectedDelivery, selection = TextRange(expectedDelivery.length))
        }
    }

    // Contact Picker logic
    val processSelectedContact: (Uri) -> Unit = { contactUri ->
        try {
            var contactId: String? = null
            var displayName: String? = null

            context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY).let {
                        if (it < 0) cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME) else it
                    }
                    if (idIdx >= 0) contactId = cursor.getString(idIdx)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                }
            }

            if (contactId.isNullOrEmpty()) {
                contactId = try { ContentUris.parseId(contactUri).toString() } catch (e: Exception) { contactUri.lastPathSegment }
            }

            var phoneNum: String? = null
            if (!contactId.isNullOrEmpty()) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (phoneCursor.moveToNext() && numIdx >= 0) {
                        phoneNum = phoneCursor.getString(numIdx)
                    }
                }
            }

            if (!displayName.isNullOrBlank()) {
                viewModel.farmerName.value = displayName!!
            }
            if (!phoneNum.isNullOrBlank()) {
                val cleanDigits = phoneNum!!.replace("[^0-9]".toRegex(), "")
                val tenDigits = if (cleanDigits.length > 10) cleanDigits.takeLast(10) else cleanDigits
                val formatted = prefix + tenDigits
                viewModel.contactNumber.value = formatted
            }
        } catch (e: Exception) {
            Log.e("GardenPlanning", "Error picking contact", e)
            Toast.makeText(context, "Failed to read contact details", Toast.LENGTH_SHORT).show()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri -> if (uri != null) processSelectedContact(uri) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try { contactPickerLauncher.launch(null) } catch (e: Exception) {
                Toast.makeText(context, "Unable to open contacts picker", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Contacts permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    val launchContactPicker = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try { contactPickerLauncher.launch(null) } catch (e: Exception) {
                Toast.makeText(context, "Unable to open contacts", Toast.LENGTH_SHORT).show()
            }
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val speechToTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val current = viewModel.notes.value
                val updated = if (current.isBlank()) spokenText else "$current $spokenText"
                viewModel.notes.value = updated
            }
        }
    }

    val launchSpeechToText = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak notes or inspection remarks...")
            }
            speechToTextLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("GardenPlanning", "Speech recognizer not found", e)
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("GardenPlanning", "Failed to launch speech recognizer", e)
            Toast.makeText(context, "Unable to open voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechToText()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    // Date Pickers state & handler
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var datePickerInitialStr by remember { mutableStateOf("") }
    var datePickerOnSelected by remember { mutableStateOf<(String) -> Unit>({}) }

    val openDatePicker = { isBookingDate: Boolean ->
        datePickerInitialStr = if (isBookingDate) bookingDate else expectedDelivery
        datePickerOnSelected = { selected ->
            if (isBookingDate) {
                bookingDateTFV = TextFieldValue(text = selected, selection = TextRange(selected.length))
                viewModel.bookingDate.value = selected
            } else {
                expectedDeliveryTFV = TextFieldValue(text = selected, selection = TextRange(selected.length))
                viewModel.expectedDelivery.value = selected
            }
        }
        showDatePickerDialog = true
    }

    // Computed total cost
    val calculatedCost = viewModel.calculateTotalCost()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalCostFormatted = currencyFormat.format(calculatedCost)

    val scrollState = rememberScrollState()
    scrollState.rememberScrollHapticFeedback()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Serial Number field with Lock / Save / Refresh icons matching FarmerFormScreen
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { 
                if (!isSerialLocked) {
                    viewModel.updateSerialNumber(it)
                }
            },
            readOnly = isSerialLocked,
            label = { Text("Serial No. (Garden Planning) *") },
            placeholder = { Text("Type serial number (e.g. GP-1001)") },
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_serial_number_input"),
            colors = elevatedInputFieldColors(isDark = isDark),
            leadingIcon = {
                Icon(
                    imageVector = if (isSerialLocked) Icons.Default.Lock else Icons.Default.ConfirmationNumber,
                    contentDescription = if (isSerialLocked) "Locked" else "Serial Number",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isSerialLocked) {
                        IconButton(
                            onClick = { viewModel.lockSerialNumber() },
                            modifier = Modifier.testTag("save_serial_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Serial Number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.resetSerialNumber() },
                        modifier = Modifier.testTag("new_serial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Serial",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        // Section Header: FARMER DETAILS
        Text(
            text = "FARMER DETAILS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Farmer Name
        OutlinedTextField(
            value = farmerName,
            onValueChange = { viewModel.farmerName.value = capitalizeWordsNaturally(it) },
            label = { Text("Farmer Name *") },
            placeholder = { Text("e.g. Mohammad Abdullah") },
            shape = textFieldShape,
            singleLine = true,
            keyboardOptions = AppDefaultWordKeyboardOptions,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_farmer_name_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Farmer Address
        OutlinedTextField(
            value = farmerAddress,
            onValueChange = { viewModel.farmerAddress.value = capitalizeWordsNaturally(it) },
            label = { Text("Farmer Address *") },
            placeholder = { Text("e.g. Village Green Valley, Sector 4") },
            shape = textFieldShape,
            singleLine = false,
            maxLines = 2,
            keyboardOptions = AppDefaultWordKeyboardOptions,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_farmer_address_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Contact Number Field Pattern (+91 Prefix + Contact Picker)
        OutlinedTextField(
            value = contactTextFieldValue,
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

                val updatedValue = TextFieldValue(
                    text = formattedText,
                    selection = TextRange(targetSelStart, targetSelEnd)
                )

                contactTextFieldValue = updatedValue
                viewModel.contactNumber.value = formattedText
            },
            label = { Text("Contact Number *") },
            placeholder = { Text("e.g. 9876543210") },
            shape = textFieldShape,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { launchContactPicker() },
                    modifier = Modifier.testTag("contacts_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "Contact Picker",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        if (contactNumber.isEmpty() || !contactNumber.startsWith(prefix)) {
                            val initialText = prefix
                            viewModel.contactNumber.value = initialText
                            contactTextFieldValue = TextFieldValue(
                                text = initialText,
                                selection = TextRange(prefix.length)
                            )
                        }
                    }
                }
                .testTag("garden_contact_number_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Section Header: GARDEN PLANNING SPECIFICATION
        Text(
            text = "GARDEN PLANNING SPECIFICATION",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Row 1: Plant Variety (left) | Rootstock (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Plant Variety
            OutlinedTextField(
                value = plantVariety,
                onValueChange = { viewModel.plantVariety.value = capitalizeWordsNaturally(it) },
                label = { Text("Plant Variety", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. Gala") },
                shape = textFieldShape,
                singleLine = true,
                keyboardOptions = AppDefaultWordKeyboardOptions,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LocalFlorist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_plant_variety_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // Rootstock
            OutlinedTextField(
                value = rootStock,
                onValueChange = { viewModel.rootStock.value = capitalizeWordsNaturally(it) },
                label = { Text("Rootstock", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. M9") },
                shape = textFieldShape,
                singleLine = true,
                keyboardOptions = AppDefaultWordKeyboardOptions,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_root_stock_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // Row 2: Sapling Age (left) | Plant Origin (right)
        var ageDropdownExpanded by remember { mutableStateOf(false) }
        val saplingAgeOptions = listOf("1 Year", "2 Years", "3 Years", "4 Years", "Grafted / Budded")

        var originDropdownExpanded by remember { mutableStateOf(false) }
        val plantOriginOptions = listOf("Local Plants", "Imported Plants")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sapling Age Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = saplingAge,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sapling Age", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("Select Age") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { ageDropdownExpanded = !ageDropdownExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Sapling Age")
                        }
                    },
                    shape = textFieldShape,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ageDropdownExpanded = true }
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("garden_sapling_age_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
                DropdownMenu(
                    expanded = ageDropdownExpanded,
                    onDismissRequest = { ageDropdownExpanded = false }
                ) {
                    saplingAgeOptions.forEach { age ->
                        DropdownMenuItem(
                            text = { Text(age) },
                            onClick = {
                                viewModel.saplingAge.value = age
                                ageDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Plant Origin Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = plantOrigin,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Plant Origin", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("Select Origin") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Yard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { originDropdownExpanded = !originDropdownExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Plant Origin")
                        }
                    },
                    shape = textFieldShape,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { originDropdownExpanded = true }
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("garden_plant_origin_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
                DropdownMenu(
                    expanded = originDropdownExpanded,
                    onDismissRequest = { originDropdownExpanded = false }
                ) {
                    plantOriginOptions.forEach { originOption ->
                        DropdownMenuItem(
                            text = { Text(originOption) },
                            onClick = {
                                viewModel.plantOrigin.value = originOption
                                originDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Feathers (Full-Width Standard Text Specification Field)
        OutlinedTextField(
            value = feathers,
            onValueChange = { viewModel.feathers.value = it },
            label = { Text("Feathers", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text("Branches / shoots (e.g. 3, 3F, 5A, 2-3, 3+)") },
            shape = textFieldShape,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Nature,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_feathers_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Section Header: COST & QUANTITY DETAILS
        Text(
            text = "COST & QUANTITY DETAILS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 3-Field Row: Total Kanal Area, Plants per Kanal, Total Plants
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Field 1: Total Kanal Area
            OutlinedTextField(
                value = totalKanalArea,
                onValueChange = { newArea ->
                    viewModel.totalKanalArea.value = newArea
                    lastEdited = LastEditedField.AREA
                    val area = newArea.toDoubleOrNull()
                    val density = plantsPerKanal.toDoubleOrNull()
                    if (newArea.isBlank()) {
                        viewModel.totalPlants.value = ""
                    } else if (density == null || density <= 0) {
                        viewModel.totalPlants.value = "—"
                    } else if (area != null) {
                        val calcPlants = Math.round(area * density).toInt()
                        viewModel.totalPlants.value = if (calcPlants > 0) calcPlants.toString() else "0"
                    } else {
                        viewModel.totalPlants.value = "—"
                    }
                    viewModel.recalculatePaymentStatus()
                },
                label = { Text("Kanal Area *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. 1.2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = textFieldShape,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_kanal_area_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // Field 2: Plants per Kanal
            OutlinedTextField(
                value = plantsPerKanal,
                onValueChange = { newDensityStr ->
                    viewModel.plantsPerKanal.value = newDensityStr
                    val density = newDensityStr.toDoubleOrNull()
                    if (lastEdited == LastEditedField.TOTAL_PLANTS) {
                        // Recompute Kanal Area = totalPlants / plantsPerKanal (2 decimals), leave Total Plants untouched
                        val plants = totalPlants.toDoubleOrNull()
                        if (totalPlants.isBlank()) {
                            viewModel.totalKanalArea.value = ""
                        } else if (density == null || density <= 0) {
                            viewModel.totalKanalArea.value = "—"
                        } else if (plants != null) {
                            val calcArea = plants / density
                            val formattedKanal = if (calcArea % 1.0 == 0.0) {
                                calcArea.toInt().toString()
                            } else {
                                String.format(java.util.Locale.US, "%.2f", calcArea)
                            }
                            viewModel.totalKanalArea.value = formattedKanal
                        } else {
                            viewModel.totalKanalArea.value = "—"
                        }
                    } else {
                        // lastEdited == AREA or NONE (default): recompute Total Plants = round(kanalArea * plantsPerKanal), leave Kanal Area untouched
                        val area = totalKanalArea.toDoubleOrNull()
                        if (totalKanalArea.isBlank()) {
                            viewModel.totalPlants.value = ""
                        } else if (density == null || density <= 0) {
                            viewModel.totalPlants.value = "—"
                        } else if (area != null) {
                            val calcPlants = Math.round(area * density).toInt()
                            viewModel.totalPlants.value = if (calcPlants > 0) calcPlants.toString() else "0"
                        } else {
                            viewModel.totalPlants.value = "—"
                        }
                    }
                    viewModel.recalculatePaymentStatus()
                },
                label = { Text("Plants/Kanal *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. 100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = textFieldShape,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_plants_per_kanal_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // Field 3: Total Plants
            OutlinedTextField(
                value = totalPlants,
                onValueChange = { newPlantsStr ->
                    viewModel.totalPlants.value = newPlantsStr
                    lastEdited = LastEditedField.TOTAL_PLANTS
                    val plants = newPlantsStr.toDoubleOrNull()
                    val density = plantsPerKanal.toDoubleOrNull()
                    if (newPlantsStr.isBlank()) {
                        viewModel.totalKanalArea.value = ""
                    } else if (density == null || density <= 0) {
                        viewModel.totalKanalArea.value = "—"
                    } else if (plants != null) {
                        val calcArea = plants / density
                        val formattedKanal = if (calcArea % 1.0 == 0.0) {
                            calcArea.toInt().toString()
                        } else {
                            String.format(java.util.Locale.US, "%.2f", calcArea)
                        }
                        viewModel.totalKanalArea.value = formattedKanal
                    } else {
                        viewModel.totalKanalArea.value = "—"
                    }
                    viewModel.recalculatePaymentStatus()
                },
                label = { Text("Total Plants *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. 120") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = textFieldShape,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_total_plants_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // Field 4: Unit Price per Plant
        OutlinedTextField(
            value = costPerPlant,
            onValueChange = { 
                viewModel.costPerPlant.value = it
                viewModel.recalculatePaymentStatus()
            },
            label = { Text("Unit Price per Plant *") },
            placeholder = { Text("e.g. 150") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = textFieldShape,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_cost_per_plant_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Computed Total Cost & Summary Box (displayed when total plants and unit price are filled)
        val areaVal = totalKanalArea.toDoubleOrNull()
        val plantsVal = plantsPerKanal.toDoubleOrNull()
        val totalPVal = totalPlants.toDoubleOrNull() ?: (if (areaVal != null && plantsVal != null) areaVal * plantsVal else null)
        val costVal = costPerPlant.toDoubleOrNull()
        val isAllCostFieldsFilled = totalPVal != null && totalPVal > 0 && costVal != null && costVal > 0

        if (isAllCostFieldsFilled) {
            val calcTotalCost = totalPVal!! * costVal!!
            val calculatedTotalPlants = Math.round(totalPVal).toInt()
            val formattedTotalPlants = NumberFormat.getIntegerInstance(Locale("en", "IN")).format(calculatedTotalPlants)
            val formattedTotalCost = currencyFormat.format(calcTotalCost)

            val areaDisplay = areaVal?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "N/A"
            val plantsDisplay = plantsVal?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "N/A"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevated3dShadow(shape = RoundedCornerShape(16.dp), isDark = isDark)
                    .testTag("garden_total_cost_card"),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF22242B) else Color(0xFFF8F9FA),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF373A45) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "TOTAL COST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$areaDisplay Kanals × $plantsDisplay/Kanal = $formattedTotalPlants Total Plants",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = formattedTotalCost,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }

        // Section Header: PAYMENT STATUS
        Text(
            text = "PAYMENT STATUS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Dynamic Payment Status Option Badges (Non-interactive, auto-calculated)
        val paymentStatusOptions = listOf("Pending", "Advance Paid", "Fully Paid")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paymentStatusOptions.forEach { statusOption ->
                val isSelected = paymentStatus.equals(statusOption, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF4A4D58) else Color(0xFFD0D0D0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .boundedFormFieldRipple(shape = RoundedCornerShape(24.dp))
                        .testTag("garden_payment_status_$statusOption")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Auto-calculated & Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = statusOption,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDark) Color.White else Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }

        // Amount Paid Input Field
        OutlinedTextField(
            value = amountPaid,
            onValueChange = { viewModel.onAmountPaidChanged(it) },
            label = { Text("Amount Paid (₹) *") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = pillShape,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = pillShape)
                .elevated3dShadow(shape = pillShape, isDark = isDark)
                .testTag("garden_amount_paid_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Calculated Payment Summary Box (Matching Local Plants tab)
        val amountPaidDouble = viewModel.calculateAmountPaid()
        val remainingBalance = viewModel.calculateRemainingBalance()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .elevated3dShadow(shape = RoundedCornerShape(16.dp), isDark = isDark),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF22242B) else Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF373A45) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(calculatedCost.toLong())}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount Paid:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(amountPaidDouble.toLong())}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32))
                }

                HorizontalDivider(color = if (isDark) Color(0xFF373A45) else Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Balance:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(remainingBalance.toLong())}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Section Title: SCHEDULE & DATES (Matching Local Plants tab)
        Text(
            text = "SCHEDULE & DATES",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = bookingDateTFV,
                onValueChange = { newVal ->
                    val formatted = formatAutoSlashDate(bookingDateTFV.text, newVal.text)
                    val newPos = if (formatted.length > bookingDateTFV.text.length && formatted.endsWith("/")) {
                        formatted.length
                    } else if (newVal.selection.end <= formatted.length) {
                        newVal.selection.end
                    } else {
                        formatted.length
                    }
                    bookingDateTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                    viewModel.bookingDate.value = formatted
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp),
                label = { Text("Booking Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("DD/MM/YYYY") },
                shape = textFieldShape,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { openDatePicker(true) },
                        modifier = Modifier.size(36.dp).testTag("garden_booking_date_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_booking_date_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            OutlinedTextField(
                value = expectedDeliveryTFV,
                onValueChange = { newVal ->
                    val formatted = formatAutoSlashDate(expectedDeliveryTFV.text, newVal.text)
                    val newPos = if (formatted.length > expectedDeliveryTFV.text.length && formatted.endsWith("/")) {
                        formatted.length
                    } else if (newVal.selection.end <= formatted.length) {
                        newVal.selection.end
                    } else {
                        formatted.length
                    }
                    expectedDeliveryTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                    viewModel.expectedDelivery.value = formatted
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp),
                label = { Text("Expected Delivery", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("DD/MM/YYYY") },
                shape = textFieldShape,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { openDatePicker(false) },
                        modifier = Modifier.size(36.dp).testTag("garden_expected_delivery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("garden_expected_delivery_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // Section Header: SPECIAL INSTRUCTIONS / NOTES
        Text(
            text = "SPECIAL INSTRUCTIONS / NOTES",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = capitalizeWordsNaturally(it) },
            label = { Text("Notes / Inspection Remarks") },
            placeholder = { Text("Enter any special instructions or land conditions...") },
            minLines = 2,
            maxLines = 4,
            shape = textFieldShape,
            keyboardOptions = AppDefaultWordKeyboardOptions,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            launchSpeechToText()
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.testTag("garden_notes_voice_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_notes_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Message Preview Component
        val previewMsg = viewModel.getGeneratedPreviewMessage()
        MessagePreviewComponent(
            selectedTemplate = selectedTemplate,
            onSelectTemplate = { viewModel.selectedTemplate.value = it },
            generatedMessage = previewMsg,
            isDark = isDark
        )

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Save Booking Entry
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            val success = viewModel.saveEntrySync()
                            isSaving = false
                            if (success) {
                                onSaved()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("garden_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingEntryId != null) "Update Booking Entry" else "Save Booking Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            // 2 & 3. Share via WhatsApp & Share via SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (contactNumber.isBlank()) {
                            Toast.makeText(context, "Please enter farmer's contact phone number first", Toast.LENGTH_SHORT).show()
                        } else {
                            val cleanPhone = contactNumber.replace("[^0-9]".toRegex(), "")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(previewMsg)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("send_text_via_whatsapp_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share via WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = {
                        if (contactNumber.isBlank()) {
                            Toast.makeText(context, "Please enter farmer's contact phone number first", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("smsto:${contactNumber}")
                                putExtra("sms_body", previewMsg)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Messaging app not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("send_text_via_sms_button")
                ) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share via SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // 4. Send Digital Receipt Image
            OutlinedButton(
                onClick = {
                    val area = totalKanalArea.toDoubleOrNull() ?: 0.0
                    val plants = plantsPerKanal.toDoubleOrNull()?.toInt() ?: plantsPerKanal.toIntOrNull() ?: 0
                    val totalPlants = Math.round(area * plants).toInt()
                    val totalAmount = calculatedCost

                    val cost = costPerPlant.toDoubleOrNull() ?: 0.0
                    val receiptData = ReceiptData(
                        serialNumber = serialNumber,
                        bookingDate = bookingDate,
                        farmerName = farmerName,
                        contactNumber = contactNumber,
                        address = farmerAddress,
                        orchardLocation = farmerAddress,
                        serviceCategory = "Garden Planning",
                        plantVariety = plantVariety.ifBlank { "Apple Plants" },
                        quantity = totalPlants.toString(),
                        totalAmount = totalAmount,
                        amountPaid = amountPaidDouble,
                        remainingBalance = remainingBalance,
                        paymentStatus = paymentStatus,
                        expectedDelivery = expectedDelivery,
                        plantOrigin = plantOrigin.ifBlank { "Local Plants" },
                        feathers = feathers,
                        rootstock = rootStock,
                        recordType = "gardenplanning",
                        totalKanalArea = area,
                        costPerPlant = cost
                    )

                    val bitmap = ReceiptGenerator.generateReceiptBitmap(receiptData, context)
                    val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bitmap, serialNumber)
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, "Dear ${farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning ($serialNumber).")
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Dear ${farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning ($serialNumber).")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(fallbackIntent, "Share Digital Receipt"))
                        }
                    } else {
                        Toast.makeText(context, "Failed to generate receipt image", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_digital_receipt_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Digital Receipt Image",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 5. Clear Form Button
            OutlinedButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Clear Form",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDatePickerDialog) {
        AppDatePickerDialog(
            initialDateStr = datePickerInitialStr,
            onDateSelected = { selected ->
                datePickerOnSelected(selected)
                showDatePickerDialog = false
            },
            onDismissRequest = { showDatePickerDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GardenPlanningRecordsTab(
    viewModel: GardenPlanningViewModel,
    entries: List<GardenPlanningEntry>,
    isDark: Boolean,
    onEdit: (GardenPlanningEntry) -> Unit,
    onAddNewEntry: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val lazyListState = rememberLazyListState()
    lazyListState.rememberScrollHapticFeedback()

    var selectedDetailEntry by remember { mutableStateOf<GardenPlanningEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<GardenPlanningEntry?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isInitialLoading by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(entries) {
        if (entries.isNotEmpty()) {
            isInitialLoading = false
        } else {
            kotlinx.coroutines.delay(300)
            isInitialLoading = false
        }
    }

    if (selectedDetailEntry != null) {
        GardenBookingRecordDetailDialog(
            entry = selectedDetailEntry!!,
            viewModel = viewModel,
            isDark = isDark,
            onDismiss = { selectedDetailEntry = null },
            onEdit = { entry ->
                selectedDetailEntry = null
                onEdit(entry)
            }
        )
    }

    entryToDelete?.let { entry ->
        DeleteBookingConfirmationDialog(
            title = "Delete this booking?",
            farmerName = entry.farmerName,
            identifier = entry.serialNumber,
            onConfirm = {
                viewModel.deleteEntry(entry)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }

    // Financial & Quantity Summary Metrics
    val totalPayment = entries.sumOf { it.totalCost }
    val animatedItemIds = remember(selectedPaymentFilter, searchQuery) { mutableSetOf<Any>() }
    val receivedPayment = entries.sumOf { entry ->
        if (entry.amountPaid > 0) entry.amountPaid
        else when (entry.paymentStatus) {
            "Fully Paid" -> entry.totalCost
            "Advance Paid" -> entry.totalCost * 0.5
            else -> 0.0
        }
    }
    val pendingPayment = (totalPayment - receivedPayment).coerceAtLeast(0.0)
    val totalQuantity = entries.sumOf { (it.totalKanalArea * it.plantsPerKanal).toInt() }

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
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        // 1. Active Recording Book Header Banner (positioned directly ABOVE search box)
        item {
            RecordingBookHeader(
                title = "Garden Planning Recording Book",
                count = entries.size
            )
        }

        // 2. Sticky Search Bar & Payment Status Filter Dropdown
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                SearchBarWithStatusFilter(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedFilter = selectedPaymentFilter,
                    onFilterSelected = { viewModel.setPaymentFilter(it) },
                    placeholderText = "Search by farmer name, phone, serial or address...",
                    isDark = isDark,
                    testTagPrefix = "garden_search",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // 3. Summary Statistics Cards Grid
        item {
            GardenRecordSummaryCards(
                totalPayment = totalPayment,
                receivedPayment = receivedPayment,
                pendingPayment = pendingPayment,
                totalQuantity = totalQuantity,
                isDark = isDark
            )
        }

        if (entries.isEmpty()) {
            if (isInitialLoading) {
                items(4) {
                    SkeletonCard(isDark = isDark, lineCount = 4, hasActionRow = true)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "No Records Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedPaymentFilter != "All Records")
                                "No garden planning records match your search or filter criteria."
                            else
                                "No entries saved in the Garden Planning Recording Book yet. Create a new entry under Garden Planning to add it here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { onAddNewEntry() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Add New Entry to Garden Planning")
                        }
                    }
                }
                }
            }
        } else {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                StaggeredEntranceWrapper(
                    itemId = entry.id,
                    index = index,
                    animatedItemIds = animatedItemIds
                ) {
                    SwipeableGardenPlanningItem(
                        entry = entry,
                        onDelete = { entryToDelete = entry },
                        context = context
                    ) {
                        GardenPlanningRecordCard(
                            entry = entry,
                            currencyFormat = currencyFormat,
                            onViewDetails = { selectedDetailEntry = entry },
                            onEdit = { onEdit(entry) },
                            onDelete = { entryToDelete = entry },
                            context = context,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGardenPlanningItem(
    entry: GardenPlanningEntry,
    onDelete: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showWhatsAppDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right -> Send via WhatsApp
                    if (entry.contactNumber.isNotBlank()) {
                        showWhatsAppDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "No contact number available for ${entry.farmerName.ifBlank { "Farmer" }}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left -> Delete
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor = if (isStartToEnd) Color(0xFF16A34A) else Color(0xFFDC2626)
            val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (isStartToEnd) Icons.Default.Chat else Icons.Default.DeleteOutline
            val text = if (isStartToEnd) "WhatsApp" else "Delete"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isStartToEnd) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Swipe to Send WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = "Swipe to Delete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        content = {
            content()
        }
    )

    if (showWhatsAppDialog) {
        val totalCost = entry.totalCost
        val amountPaid = entry.amountPaid
        val remBalance = if (entry.remainingBalance > 0) entry.remainingBalance else maxOf(0.0, totalCost - amountPaid)

        WhatsAppTemplateDialog(
            farmerName = entry.farmerName.ifBlank { "Farmer" },
            contactNumber = entry.contactNumber,
            serviceType = "Garden Planning",
            amountPaid = amountPaid,
            totalAmount = totalCost,
            remainingBalance = remBalance,
            paymentStatus = entry.paymentStatus,
            serialNumber = if (entry.serialNumber.isBlank()) "N/A" else entry.serialNumber,
            onDismiss = { showWhatsAppDialog = false }
        )
    }
}

@Composable
private fun GardenPlanningRecordCard(
    entry: GardenPlanningEntry,
    currencyFormat: NumberFormat,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    context: Context,
    isDark: Boolean = isAppInDarkMode()
) {
    val initialLetter = entry.farmerName.trim().take(1).uppercase().ifBlank { "F" }
    val avatarBgColor = MaterialTheme.colorScheme.primary

    val (statusBadgeBg, statusBadgeText) = when (entry.paymentStatus) {
        "Fully Paid" -> Pair(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7), if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D))
        "Advance Paid" -> Pair(if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5), if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C))
        else -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Profile Initial Avatar + Farmer Name + Serial Number & Payment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Avatar with Initial
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Farmer Name & Serial No
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.farmerName.ifBlank { "Farmer Name Not Specified" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "#${entry.serialNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (entry.bookingDate.isNotBlank()) {
                            Text(
                                text = "• ${entry.bookingDate}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Payment Status Badge
                Surface(
                    color = statusBadgeBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = entry.paymentStatus.ifBlank { "Pending" },
                        color = statusBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Address & Contact Info
            if (entry.farmerAddress.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = entry.farmerAddress,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (entry.contactNumber.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.contactNumber}"))
                        try { context.startActivity(dialIntent) } catch (e: Exception) {}
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = entry.contactNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            // Area & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.totalKanalArea} Kanals • ${entry.plantsPerKanal} Plants/Kanal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = currencyFormat.format(entry.totalCost),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Uniform Bottom Action Row: 1. WhatsApp, 2. "View Details", 3. Right Arrow, 4. Edit, 5. Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. WhatsApp Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7))
                        .clickable {
                            val cleanPhone = entry.contactNumber.replace("[^0-9]".toRegex(), "")
                            val msg = com.example.data.MessageTemplateRepository.renderTemplate(
                                templateId = "garden_booking_note",
                                data = mapOf(
                                    "farmerName" to entry.farmerName.ifBlank { "Farmer" },
                                    "serialNumber" to entry.serialNumber,
                                    "totalCost" to currencyFormat.format(entry.totalCost),
                                    "paymentStatus" to entry.paymentStatus
                                )
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 2. Text 'View Details' & 3. Right-pointing Arrow Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewDetails() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. Edit Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Record",
                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 5. Delete Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Record",
                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    isBold: Boolean = false,
    isDark: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        val isDateField = label.contains("Date", ignoreCase = true) || label.contains("Delivery", ignoreCase = true)
        Text(
            text = value.ifBlank { "N/A" },
            fontSize = if (isDateField) 12.5.sp else 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (valueColor != Color.Unspecified) valueColor else (if (isDark) Color.White else Color(0xFF0F172A)),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false,
    isDark: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun GardenBookingRecordDetailDialog(
    entry: GardenPlanningEntry,
    viewModel: GardenPlanningViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onEdit: (GardenPlanningEntry) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSavingInstallment by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var receiptPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showWhatsAppConfirm by remember { mutableStateOf(false) }
    var showSmsConfirm by remember { mutableStateOf(false) }
    var showTrackingWaConfirm by remember { mutableStateOf(false) }
    var installmentToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTemplate by remember { mutableStateOf("Booking Confirmation") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var currentEntry by remember { mutableStateOf(entry) }
    var installments by remember(currentEntry.installmentHistoryJson, currentEntry.amountPaid) {
        mutableStateOf(parseGardenInstallments(currentEntry.installmentHistoryJson, currentEntry.amountPaid, currentEntry.bookingDate))
    }

    LaunchedEffect(currentEntry.id, currentEntry.installmentHistoryJson, currentEntry.amountPaid) {
        installments = parseGardenInstallments(currentEntry.installmentHistoryJson, currentEntry.amountPaid, currentEntry.bookingDate)
    }

    var newAmountText by remember { mutableStateOf("") }
    var dateTFV by remember { mutableStateOf(TextFieldValue(text = todayStr, selection = TextRange(todayStr.length))) }
    var modeNoteText by remember { mutableStateOf("Cash") }

    val totalPlants = (currentEntry.totalKanalArea * currentEntry.plantsPerKanal).toInt()
    val totalRecordValue = currentEntry.totalCost
    val totalPaidSoFar = installments.sumOf { it.amount }
    val remainingBalance = maxOf(0.0, totalRecordValue - totalPaidSoFar)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDark) Color(0xFF121826) else Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Bar: Serial No. Pill on Left | Edit, Delete, Close Icons on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Serial No. ${currentEntry.serialNumber.ifBlank { "01" }}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEdit(currentEntry)
                            },
                            modifier = Modifier.testTag("edit_record_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Record",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_record_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Record",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_detail_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close View",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // 2. Farmer Header Card (Profile Avatar in Theme Color + Category + Name in bold + Phone + Address)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentEntry.farmerName.trim().take(1).uppercase().ifBlank { "F" },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Garden Planning",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentEntry.farmerName.ifBlank { "Farmer Name" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            if (currentEntry.contactNumber.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentEntry.contactNumber}"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Phone",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = currentEntry.contactNumber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                            Text(
                                text = "Address: ${currentEntry.farmerAddress.ifBlank { "Not specified" }}",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }

                // 3. Specifications List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRowItem(label = "Category", value = "Garden Planning", isDark = isDark)
                    DetailRowItem(label = "Total Area", value = "${currentEntry.totalKanalArea} Kanals", isDark = isDark)
                    DetailRowItem(label = "Plants per Kanal", value = "${currentEntry.plantsPerKanal} Plants/Kanal", isDark = isDark)
                    DetailRowItem(label = "Total Calculated Plants", value = "$totalPlants Plants", isDark = isDark)
                    DetailRowItem(label = "Rate / Unit Price", value = "₹${currentEntry.costPerPlant.toInt()}", isDark = isDark)
                    DetailRowItem(label = "Plant Origin", value = currentEntry.plantOrigin.ifBlank { "Local Plants" }, isDark = isDark)
                    if (currentEntry.plantVariety.isNotBlank()) {
                        DetailRowItem(label = "Plant Variety", value = currentEntry.plantVariety, isDark = isDark)
                    }
                    if (currentEntry.rootStock.isNotBlank()) {
                        DetailRowItem(label = "Rootstock Variety", value = currentEntry.rootStock, isDark = isDark)
                    }
                    if (currentEntry.feathers.isNotBlank()) {
                        DetailRowItem(label = "Feathers", value = if (currentEntry.feathers.all { it.isDigit() }) "${currentEntry.feathers} branches" else currentEntry.feathers, isDark = isDark)
                    }

                    DetailRowItem(
                        label = "Total Amount",
                        value = "₹${totalRecordValue.toInt()}",
                        valueColor = MaterialTheme.colorScheme.primary,
                        isBold = true,
                        isDark = isDark
                    )
                    DetailRowItem(
                        label = "Amount Paid",
                        value = "₹${totalPaidSoFar.toInt()}",
                        valueColor = if (remainingBalance <= 0) (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)) else MaterialTheme.colorScheme.primary,
                        isBold = true,
                        isDark = isDark
                    )
                    DetailRowItem(
                        label = "Remaining Balance",
                        value = "₹${remainingBalance.toInt()}",
                        valueColor = if (remainingBalance <= 0) (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)) else MaterialTheme.colorScheme.primary,
                        isBold = true,
                        isDark = isDark
                    )
                    DetailRowItem(label = "Booking Date", value = currentEntry.bookingDate.ifBlank { todayStr }, isDark = isDark)
                    DetailRowItem(label = "Expected Delivery", value = currentEntry.expectedDelivery.ifBlank { "Not set" }, isDark = isDark)
                    if (currentEntry.notes.isNotBlank()) {
                        DetailRowItem(label = "Orchard / Notes", value = currentEntry.notes, isDark = isDark)
                    }
                }

                // 4. Installment Payment Tracking Section
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Title + Fully Paid Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Installment Payment Tracking",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            val (statusText, statusBg) = when {
                                remainingBalance <= 0.01 -> "Fully Paid" to (if (isDark) Color(0xFF15803D) else Color(0xFF16A34A))
                                totalPaidSoFar > 0 -> "Advance Paid" to Color(0xFFE65100)
                                else -> "Pending" to MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(statusBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Summary Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryLine(
                                    label = "Total Record Value:",
                                    value = "₹${totalRecordValue.toInt()}",
                                    valueColor = if (isDark) Color.White else Color(0xFF0F172A),
                                    isDark = isDark
                                )
                                SummaryLine(
                                    label = "Total Paid So Far:",
                                    value = "₹${totalPaidSoFar.toInt()}",
                                    valueColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                    isDark = isDark
                                )
                                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), thickness = 0.5.dp)
                                SummaryLine(
                                    label = "Remaining Balance Due:",
                                    value = "₹${remainingBalance.toInt()}",
                                    valueColor = if (remainingBalance <= 0) (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)) else MaterialTheme.colorScheme.primary,
                                    isBold = true,
                                    isDark = isDark
                                )
                            }
                        }

                        // Sub-section: RECORD NEW INSTALLMENT PAYMENT
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "RECORD NEW INSTALLMENT PAYMENT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = newAmountText,
                                onValueChange = { newAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("New Payment Amount (₹)") },
                                placeholder = { Text("e.g. 1000") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                    unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                    focusedTextColor = if (isDark) Color.White else Color.Black,
                                    unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = dateTFV,
                                    onValueChange = { dateTFV = it },
                                    label = { Text("Payment Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                        focusedTextColor = if (isDark) Color.White else Color.Black,
                                        unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                OutlinedTextField(
                                    value = modeNoteText,
                                    onValueChange = { modeNoteText = capitalizeWordsNaturally(it) },
                                    label = { Text("Mode / Note") },
                                    singleLine = true,
                                    keyboardOptions = AppDefaultWordKeyboardOptions,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                        focusedTextColor = if (isDark) Color.White else Color.Black,
                                        unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            val amtVal = newAmountText.toDoubleOrNull() ?: 0.0
                            val canSave = amtVal > 0

                            Button(
                                onClick = {
                                    if (canSave && !isSavingInstallment) {
                                        isSavingInstallment = true
                                        val newInst = GardenInstallment(
                                            amount = amtVal,
                                            date = dateTFV.text.ifBlank { todayStr },
                                            paymentMode = modeNoteText.ifBlank { "Cash" },
                                            note = if (installments.isEmpty()) "Initial Advance Payment" else "Payment Addition"
                                        )
                                        val updatedList = installments + newInst
                                        val newPaidSum = updatedList.sumOf { it.amount }
                                        val newStatus = when {
                                            newPaidSum >= totalRecordValue - 0.01 -> "Fully Paid"
                                            newPaidSum > 0 -> "Advance Paid"
                                            else -> "Pending"
                                        }
                                        val jsonStr = serializeGardenInstallments(updatedList)

                                        val updatedEntry = currentEntry.copy(
                                            amountPaid = newPaidSum,
                                            remainingBalance = (totalRecordValue - newPaidSum).coerceAtLeast(0.0),
                                            paymentStatus = newStatus,
                                            installmentHistoryJson = jsonStr
                                        )

                                        currentEntry = updatedEntry
                                        viewModel.updateEntrySync(updatedEntry)
                                        installments = updatedList
                                        Toast.makeText(context, "Installment recorded successfully!", Toast.LENGTH_SHORT).show()
                                        newAmountText = ""
                                        isSavingInstallment = false
                                    }
                                },
                                enabled = canSave && !isSavingInstallment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canSave && !isSavingInstallment) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                    disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                )
                            ) {
                                if (isSavingInstallment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving Installment...", fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (canSave) Color.White else Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Record Installment Payment", fontWeight = FontWeight.Bold, color = if (canSave) Color.White else Color(0xFF94A3B8))
                                }
                            }
                        }

                        // Sub-section: PAYMENT HISTORY LOG
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PAYMENT HISTORY LOG (${installments.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                                Text(
                                    text = "Total: ₹${totalPaidSoFar.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (installments.isEmpty()) {
                                Text("No payment installments recorded yet.", fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color.Gray)
                            } else {
                                installments.forEachIndexed { index, inst ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Installment #${index + 1}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = if (inst.note.isNotBlank()) inst.note else inst.paymentMode,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Date: ${inst.date.ifBlank { todayStr }} • ${inst.paymentMode}",
                                                    fontSize = 12.sp,
                                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "+ ₹${inst.amount.toInt()}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        installmentToDeleteIndex = index
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Installment",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val generatedRecordMsg = generateGardenMessageForEntry(currentEntry, selectedTemplate)

                    // Message Preview Section with Select Template Dropdown
                    MessagePreviewComponent(
                        selectedTemplate = selectedTemplate,
                        onSelectTemplate = { selectedTemplate = it },
                        generatedMessage = generatedRecordMsg,
                        isDark = isDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Button 1: Preview & Send Digital Receipt Image
                    Button(
                        onClick = {
                            val rData = ReceiptData(
                                serialNumber = currentEntry.serialNumber,
                                bookingDate = currentEntry.bookingDate.ifBlank { todayStr },
                                farmerName = currentEntry.farmerName,
                                contactNumber = currentEntry.contactNumber,
                                address = currentEntry.farmerAddress,
                                orchardLocation = currentEntry.farmerAddress,
                                serviceCategory = "Garden Planning",
                                plantVariety = currentEntry.plantVariety.ifBlank { "Apple Plants" },
                                quantity = "$totalPlants",
                                totalAmount = totalRecordValue,
                                amountPaid = totalPaidSoFar,
                                remainingBalance = remainingBalance,
                                paymentStatus = currentEntry.paymentStatus,
                                expectedDelivery = currentEntry.expectedDelivery.ifBlank { "Not set" },
                                plantOrigin = currentEntry.plantOrigin.ifBlank { "Local Plants" },
                                feathers = currentEntry.feathers,
                                rootstock = currentEntry.rootStock,
                                recordType = "gardenplanning",
                                recordId = currentEntry.id,
                                totalKanalArea = currentEntry.totalKanalArea,
                                costPerPlant = currentEntry.costPerPlant
                            )
                            val bmp = ReceiptGenerator.generateReceiptBitmap(rData, context)
                            receiptPreviewBitmap = bmp
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF334155) else Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview & Send Digital Receipt Image", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    // Button 2: Send WhatsApp Confirmation
                    Button(
                        onClick = {
                            showWhatsAppConfirm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF16A34A) else Color(0xFF22C55E))
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send WhatsApp Confirmation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    // Button 3: Send SMS Confirmation
                    Button(
                        onClick = {
                            showSmsConfirm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF15803D) else Color(0xFF16A34A))
                    ) {
                        Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send SMS Confirmation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    // Button 4: Send Tracking Details on WhatsApp
                    OutlinedButton(
                        onClick = {
                            showTrackingWaConfirm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Tracking Details on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF25D366))
                    }
                }

                // Generous bottom spacer so the last button can be scrolled up clearly and comfortably
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delete Record Confirmation Dialog
    if (showDeleteConfirm) {
        DeleteBookingConfirmationDialog(
            title = "Delete this booking?",
            farmerName = currentEntry.farmerName,
            identifier = currentEntry.serialNumber,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteEntry(currentEntry)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Delete Installment Confirmation Dialog
    if (installmentToDeleteIndex != null) {
        val idx = installmentToDeleteIndex!!
        AlertDialog(
            onDismissRequest = { installmentToDeleteIndex = null },
            title = { Text("Delete Installment") },
            text = { Text("Are you sure you want to delete Installment #${idx + 1}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedList = installments.toMutableList().apply { removeAt(idx) }
                        val newPaidSum = updatedList.sumOf { it.amount }
                        val newStatus = when {
                            newPaidSum >= totalRecordValue - 0.01 -> "Fully Paid"
                            newPaidSum > 0 -> "Advance Paid"
                            else -> "Pending"
                        }
                        val jsonStr = serializeGardenInstallments(updatedList)
                        val updatedEntry = currentEntry.copy(
                            amountPaid = newPaidSum,
                            remainingBalance = (totalRecordValue - newPaidSum).coerceAtLeast(0.0),
                            paymentStatus = newStatus,
                            installmentHistoryJson = jsonStr
                        )
                        currentEntry = updatedEntry
                        viewModel.updateEntrySync(updatedEntry)
                        installments = updatedList
                        installmentToDeleteIndex = null
                        Toast.makeText(context, "Installment removed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { installmentToDeleteIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for WhatsApp message
    if (showWhatsAppConfirm) {
        val cleanPhone = currentEntry.contactNumber.replace("[^0-9]".toRegex(), "")
        val msg = generateGardenMessageForEntry(currentEntry, selectedTemplate)
        AlertDialog(
            onDismissRequest = { showWhatsAppConfirm = false },
            title = { Text("Confirm WhatsApp Delivery") },
            text = { Text("Send record details to ${currentEntry.farmerName} ($cleanPhone) via WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWhatsAppConfirm = false
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send WhatsApp", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for SMS message
    if (showSmsConfirm) {
        val msg = generateGardenMessageForEntry(currentEntry, selectedTemplate)
        AlertDialog(
            onDismissRequest = { showSmsConfirm = false },
            title = { Text("Confirm SMS Delivery") },
            text = { Text("Send record details to ${currentEntry.farmerName} (${currentEntry.contactNumber}) via SMS?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSmsConfirm = false
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("smsto:${currentEntry.contactNumber}")
                            putExtra("sms_body", msg)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Messaging app not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send SMS", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for Tracking Details WhatsApp message
    if (showTrackingWaConfirm) {
        val cleanPhone = currentEntry.contactNumber.replace("[^0-9]".toRegex(), "")
        val trackingMsg = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "garden_tracking_note",
            data = mapOf(
                "farmerName" to currentEntry.farmerName,
                "serialNumber" to currentEntry.serialNumber,
                "paymentStatus" to currentEntry.paymentStatus,
                "expectedDelivery" to currentEntry.expectedDelivery.ifBlank { "TBD" }
            )
        )
        AlertDialog(
            onDismissRequest = { showTrackingWaConfirm = false },
            title = { Text("Confirm Tracking Delivery") },
            text = { Text("Send tracking details to ${currentEntry.farmerName} ($cleanPhone) via WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTrackingWaConfirm = false
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(trackingMsg)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send Tracking", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackingWaConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Receipt Preview Dialog
    if (receiptPreviewBitmap != null) {
        Dialog(
            onDismissRequest = { receiptPreviewBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Digital Receipt Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { receiptPreviewBitmap = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Image(
                        bitmap = receiptPreviewBitmap!!.asImageBitmap(),
                        contentDescription = "Receipt Image",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { receiptPreviewBitmap = null },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = {
                                if (receiptPreviewBitmap != null) {
                                    ReceiptGenerator.printReceiptBitmap(context, receiptPreviewBitmap!!, currentEntry.serialNumber)
                                }
                            },
                            modifier = Modifier.weight(1.1f).testTag("print_receipt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print Receipt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val cleanPhone = currentEntry.contactNumber.replace("[^0-9]".toRegex(), "")
                                val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, receiptPreviewBitmap!!, currentEntry.serialNumber)
                                val msgText = "Dear ${currentEntry.farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning (#${currentEntry.serialNumber})."
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, msgText)
                                        if (cleanPhone.isNotBlank()) {
                                            setPackage("com.whatsapp")
                                            putExtra("jid", "$cleanPhone@s.whatsapp.net")
                                        }
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("share_whatsapp_receipt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    isDark: Boolean = isAppInDarkMode(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E2430) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333A48) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val isDateField = label.contains("Date", ignoreCase = true) || label.contains("Delivery", ignoreCase = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                fontSize = if (isDateField) 11.sp else (if (highlight) 14.sp else 12.sp),
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            action?.invoke()
        }
    }
}

private fun generateGardenMessageForEntry(entry: GardenPlanningEntry, template: String): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalPlants = (entry.totalKanalArea * entry.plantsPerKanal).toInt()
    val farmer = entry.farmerName.ifBlank { "Valued Farmer" }

    return when (template) {
        "Booking Confirmation" -> {
            "Dear $farmer,\nThank you for booking Garden Planning with The Streets of Kashmir.\n\n" +
                    "📋 Booking Serial: #${entry.serialNumber}\n" +
                    "📅 Date: ${entry.bookingDate}\n" +
                    "📐 Garden Area: ${entry.totalKanalArea} Kanals ($totalPlants Plants)\n" +
                    "💰 Total Cost: ${currencyFormat.format(entry.totalCost)}\n" +
                    "💳 Status: ${entry.paymentStatus}\n" +
                    "🚚 Expected Delivery: ${entry.expectedDelivery}\n\n" +
                    "For queries, contact us at +91 9876543210."
        }
        "Payment Received Receipt" -> {
            "OFFICIAL PAYMENT RECEIPT\n\n" +
                    "Received from: $farmer\n" +
                    "Serial No: #${entry.serialNumber}\n" +
                    "Service: Garden Planning (${entry.totalKanalArea} Kanals)\n" +
                    "Total Cost: ${currencyFormat.format(entry.totalCost)}\n" +
                    "Amount Paid: ${currencyFormat.format(entry.amountPaid)}\n" +
                    "Remaining Balance: ${currencyFormat.format(entry.remainingBalance)}\n" +
                    "Status: ${entry.paymentStatus}\n\n" +
                    "Bank Account: 0018010100007537 (IFSC: JAKA0SHOPAN)\n" +
                    "UPI ID: streetsofkashmir@upi\n" +
                    "The Streets of Kashmir"
        }
        "Delivery Schedule Reminder" -> {
            "Dear $farmer,\nYour Garden Planning delivery (#${entry.serialNumber}) is scheduled for ${entry.expectedDelivery}.\n" +
                    "Total Plants: $totalPlants (${entry.totalKanalArea} Kanals)\n" +
                    "Remaining Balance: ${currencyFormat.format(entry.remainingBalance)}\n\n" +
                    "Please ensure site readiness. - The Streets of Kashmir"
        }
        "Balance Due Notice" -> {
            "PAYMENT REMINDER\n\n" +
                    "Dear $farmer,\nRegarding your Garden Planning booking (#${entry.serialNumber}):\n" +
                    "Total Cost: ${currencyFormat.format(entry.totalCost)}\n" +
                    "Amount Paid: ${currencyFormat.format(entry.amountPaid)}\n" +
                    "Outstanding Balance: ${currencyFormat.format(entry.remainingBalance)}\n\n" +
                    "Pay via UPI: streetsofkashmir@upi or Bank Account: 0018010100007537 (IFSC: JAKA0SHOPAN).\n" +
                    "The Streets of Kashmir"
        }
        else -> "Dear $farmer, your Garden Planning booking details: Serial #${entry.serialNumber}, Total: ${currencyFormat.format(entry.totalCost)}, Paid: ${currencyFormat.format(entry.amountPaid)}."
    }
}

@Composable
private fun GardenRecordSummaryCards(
    totalPayment: Double,
    receivedPayment: Double,
    pendingPayment: Double,
    totalQuantity: Int,
    isDark: Boolean
) {
    val numberFmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Payment
            GardenSummaryCardItem(
                title = "Total Payment",
                value = "₹${numberFmt.format(totalPayment.toLong())}",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.primary,
                bgColor = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )

            // Card 2: Received Payment
            GardenSummaryCardItem(
                title = "Received Payment",
                value = "₹${numberFmt.format(receivedPayment.toLong())}",
                icon = Icons.Default.CheckCircle,
                accentColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                bgColor = if (isDark) Color(0xFF1B2E1B) else Color(0xFFE8F5E9),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Pending Payment
            GardenSummaryCardItem(
                title = "Pending Payment",
                value = "₹${numberFmt.format(pendingPayment.toLong())}",
                icon = Icons.Default.HourglassTop,
                accentColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828),
                bgColor = if (isDark) Color(0xFF331C1C) else Color(0xFFFFEBEE),
                modifier = Modifier.weight(1f)
            )

            // Card 4: Total Quantity
            GardenSummaryCardItem(
                title = "Total Quantity",
                value = "${numberFmt.format(totalQuantity)} Plants",
                icon = Icons.Default.Inventory2,
                accentColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF0288D1),
                bgColor = if (isDark) Color(0xFF1A2A38) else Color(0xFFE1F5FE),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GardenSummaryCardItem(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
