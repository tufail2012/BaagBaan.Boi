package com.example.ui.components

import kotlin.math.roundToInt
import android.Manifest
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material.icons.filled.Message
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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

fun parseGardenInstallments(jsonStr: String): List<GardenInstallment> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<GardenInstallment>()
    try {
        val array = JSONArray(jsonStr)
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
    } catch (e: Exception) {
        Log.e("GardenPlanning", "Error parsing installment json", e)
    }
    return list
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
    onBack: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
                onBack = onBack
            )
            }

            // Unified Segmented Control matching the app design system
            val isEditing = viewModel.editingEntryId.collectAsState().value != null
            AgriSegmentedControl(
                selectedMode = selectedTabIndex,
                onModeSelected = { selectedTabIndex = it },
                newEntryLabel = if (isEditing) "Edit Entry" else "New Entry",
                recordsLabel = "Records (${allEntries.size})"
            )

            when (selectedTabIndex) {
                0 -> {
                    GardenPlanningFormTab(
                        viewModel = viewModel,
                        isDark = isDark,
                        onSaved = { selectedTabIndex = 1 }
                    )
                }
                1 -> {
                    GardenPlanningRecordsTab(
                        viewModel = viewModel,
                        entries = filteredEntries,
                        isDark = isDark,
                        onEdit = { entry ->
                            viewModel.loadEntryForEdit(entry)
                            selectedTabIndex = 0
                        }
                    )
                }
            }
        }
    }
}

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

    val serialNumber by viewModel.serialNumber.collectAsState()
    val farmerName by viewModel.farmerName.collectAsState()
    val farmerAddress by viewModel.farmerAddress.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()
    val totalKanalArea by viewModel.totalKanalArea.collectAsState()
    val plantsPerKanal by viewModel.plantsPerKanal.collectAsState()
    val costPerPlant by viewModel.costPerPlant.collectAsState()
    val plantVariety by viewModel.plantVariety.collectAsState()
    val rootStock by viewModel.rootStock.collectAsState()
    val saplingAge by viewModel.saplingAge.collectAsState()
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

    // Date Pickers
    val openDatePicker = { isBookingDate: Boolean ->
        val calendar = Calendar.getInstance()
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                if (isBookingDate) {
                    viewModel.bookingDate.value = formatted
                } else {
                    viewModel.expectedDelivery.value = formatted
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
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
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Serial Number field with NO label on top
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { viewModel.serialNumber.value = it },
            label = null,
            placeholder = { Text("Serial Number (e.g. GP-01)") },
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_serial_number_input"),
            colors = elevatedInputFieldColors(isDark = isDark),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.resetSerialNumber() },
                        modifier = Modifier.testTag("new_serial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Serial",
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
            onValueChange = { viewModel.farmerName.value = it },
            label = { Text("Farmer Name *") },
            placeholder = { Text("e.g. Mohammad Abdullah") },
            shape = textFieldShape,
            singleLine = true,
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
            onValueChange = { viewModel.farmerAddress.value = it },
            label = { Text("Farmer Address *") },
            placeholder = { Text("e.g. Village Green Valley, Sector 4") },
            shape = textFieldShape,
            singleLine = false,
            maxLines = 2,
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

        // Section Header: PLANTS SPECIFICATION
        Text(
            text = "PLANTS SPECIFICATION",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Plant Variety
        OutlinedTextField(
            value = plantVariety,
            onValueChange = { viewModel.plantVariety.value = it },
            label = { Text("Plant Variety") },
            placeholder = { Text("e.g. M9 / Gala / Red Delicious") },
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_plant_variety_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Root Stock
        OutlinedTextField(
            value = rootStock,
            onValueChange = { viewModel.rootStock.value = it },
            label = { Text("Rootstock") },
            placeholder = { Text("e.g. M9 / MM106 / Seedling") },
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_root_stock_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Sapling Age Dropdown
        var ageDropdownExpanded by remember { mutableStateOf(false) }
        val saplingAgeOptions = listOf("1 Year", "2 Years", "3 Years", "4 Years", "Grafted / Budded")

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = saplingAge,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sapling Age") },
                placeholder = { Text("Select Sapling Age") },
                trailingIcon = {
                    IconButton(onClick = { ageDropdownExpanded = !ageDropdownExpanded }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Sapling Age")
                    }
                },
                shape = textFieldShape,
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

        // Section Header: COST & QUANTITY DETAILS
        Text(
            text = "COST & QUANTITY DETAILS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Field 1: Total Kanals Area (accepts decimals e.g. 1.2)
        OutlinedTextField(
            value = totalKanalArea,
            onValueChange = { viewModel.totalKanalArea.value = it },
            label = { Text("Total Kanals Area *") },
            placeholder = { Text("e.g. 1.2") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_kanal_area_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Field 2: Plants per Kanal
        OutlinedTextField(
            value = plantsPerKanal,
            onValueChange = { viewModel.plantsPerKanal.value = it },
            label = { Text("Plants per Kanal *") },
            placeholder = { Text("e.g. 100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_plants_per_kanal_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Field 3: Unit Price per Plant
        OutlinedTextField(
            value = costPerPlant,
            onValueChange = { viewModel.costPerPlant.value = it },
            label = { Text("Unit Price per Plant *") },
            placeholder = { Text("e.g. 150") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_cost_per_plant_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Computed Total Cost & Summary Box (displayed when all 3 fields are filled)
        val areaVal = totalKanalArea.toDoubleOrNull()
        val plantsVal = plantsPerKanal.toDoubleOrNull()
        val costVal = costPerPlant.toDoubleOrNull()
        val isAllCostFieldsFilled = totalKanalArea.isNotBlank() && plantsPerKanal.isNotBlank() && costPerPlant.isNotBlank() && areaVal != null && plantsVal != null && costVal != null

        if (isAllCostFieldsFilled) {
            val calcTotalCost = areaVal!! * plantsVal!! * costVal!!
            val calculatedTotalPlants = Math.round(areaVal * plantsVal).toInt()
            val formattedTotalPlants = NumberFormat.getIntegerInstance(Locale("en", "IN")).format(calculatedTotalPlants)
            val formattedTotalCost = currencyFormat.format(calcTotalCost)

            val areaDisplay = if (areaVal % 1.0 == 0.0) areaVal.toLong().toString() else areaVal.toString()
            val plantsDisplay = if (plantsVal % 1.0 == 0.0) plantsVal.toLong().toString() else plantsVal.toString()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("garden_total_cost_card"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$areaDisplay Kanals × $plantsDisplay/Kanal = $formattedTotalPlants Total Plants",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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

        // Amount Paid Input Field
        OutlinedTextField(
            value = amountPaid,
            onValueChange = { viewModel.onAmountPaidChanged(it) },
            label = { Text("Amount Paid (₹) *") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_amount_paid_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Dynamic Payment Status Buttons styled as pills
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Payment Status",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Pending", "Advance Paid", "Fully Paid").forEach { status ->
                    val isSelected = paymentStatus == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onPaymentStatusSelected(status) },
                        label = { Text(status, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = pillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Payment Summary Box
        val amountPaidDouble = viewModel.calculateAmountPaid()
        val remainingBalance = viewModel.calculateRemainingBalance()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PAYMENT SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Total Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currencyFormat.format(calculatedCost),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Amount Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currencyFormat.format(amountPaidDouble),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currencyFormat.format(remainingBalance),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Booking Date & Expected Delivery Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = bookingDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Booking Date *") },
                shape = textFieldShape,
                trailingIcon = {
                    IconButton(onClick = { openDatePicker(true) }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape, onClick = { openDatePicker(true) })
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            OutlinedTextField(
                value = expectedDelivery,
                onValueChange = {},
                readOnly = true,
                label = { Text("Expected Delivery Date *") },
                shape = textFieldShape,
                trailingIcon = {
                    IconButton(onClick = { openDatePicker(false) }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .boundedFormFieldRipple(shape = textFieldShape, onClick = { openDatePicker(false) })
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // Section Header: NOTES & COMMUNICATION
        Text(
            text = "NOTES & COMMUNICATION",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = { Text("Notes / Inspection Remarks") },
            placeholder = { Text("Enter any special instructions or land conditions...") },
            minLines = 2,
            maxLines = 4,
            shape = textFieldShape,
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
            // 1. SAVE BOOKING ENTRY (Prominent RED button)
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
                shape = pillShape,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("garden_save_button")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingEntryId != null) "UPDATE GARDEN PLAN" else "SAVE BOOKING ENTRY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            // 2. Share via WhatsApp (Green) & Share via SMS (Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cleanPhone = contactNumber.replace("[^0-9]".toRegex(), "")
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(previewMsg)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("smsto:${contactNumber}")
                            putExtra("sms_body", previewMsg)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Messaging app not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via SMS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // 3. Send Digital Receipt Image (White button with RED border)
            OutlinedButton(
                onClick = {
                    val area = totalKanalArea.toDoubleOrNull() ?: 0.0
                    val plants = plantsPerKanal.toDoubleOrNull()?.toInt() ?: plantsPerKanal.toIntOrNull() ?: 0
                    val totalPlants = Math.round(area * plants).toInt()
                    val totalAmount = calculatedCost

                    val receiptData = ReceiptData(
                        serialNumber = serialNumber,
                        bookingDate = bookingDate,
                        farmerName = farmerName,
                        contactNumber = contactNumber,
                        address = farmerAddress,
                        orchardLocation = farmerAddress,
                        serviceCategory = "Garden Planning",
                        plantVariety = "$area Kanals ($plants Plants/Kanal)",
                        quantity = totalPlants.toString(),
                        totalAmount = totalAmount,
                        amountPaid = amountPaidDouble,
                        remainingBalance = remainingBalance,
                        paymentStatus = paymentStatus,
                        expectedDelivery = expectedDelivery
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
                shape = pillShape,
                border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Digital Receipt Image", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }

            // 4. Clear Form (Text link)
            TextButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Clear Form",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GardenPlanningRecordsTab(
    viewModel: GardenPlanningViewModel,
    entries: List<GardenPlanningEntry>,
    isDark: Boolean,
    onEdit: (GardenPlanningEntry) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val searchShape = RoundedCornerShape(24.dp)

    val lazyListState = rememberLazyListState()
    lazyListState.rememberScrollHapticFeedback()

    var selectedDetailEntry by remember { mutableStateOf<GardenPlanningEntry?>(null) }

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

    // Financial & Quantity Summary Metrics
    val totalPayment = entries.sumOf { it.totalCost }
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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sticky Search Bar & Filter Chips (positioned ABOVE summary cards & sticky on scroll)
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by farmer name, phone, serial or address...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = searchShape,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = searchShape)
                            .testTag("garden_search_input"),
                        colors = elevatedInputFieldColors(isDark = isDark)
                    )

                    // Payment Status Filter Chips Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All Records", "Pending", "Advance Paid", "Fully Paid").forEach { filter ->
                            val isSelected = selectedPaymentFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPaymentFilter(filter) },
                                label = { Text(filter, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary Statistics Cards Grid (Total Payment, Received Payment, Pending Payment, Total Quantity)
        item {
            GardenRecordSummaryCards(
                totalPayment = totalPayment,
                receivedPayment = receivedPayment,
                pendingPayment = pendingPayment,
                totalQuantity = totalQuantity,
                isDark = isDark
            )
        }

        // Active Recording Book Header Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Garden Planning Recording Book",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${entries.size} Records",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
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
                                "No entries saved in the Garden Planning Recording Book yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                GardenPlanningRecordCard(
                    entry = entry,
                    currencyFormat = currencyFormat,
                    onViewDetails = { selectedDetailEntry = entry },
                    onEdit = { onEdit(entry) },
                    onDelete = { viewModel.deleteEntry(entry) },
                    context = context
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun GardenPlanningRecordCard(
    entry: GardenPlanningEntry,
    currencyFormat: NumberFormat,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    context: Context
) {
    val initialLetter = entry.farmerName.trim().take(1).uppercase().ifBlank { "F" }
    val avatarBgColor = when (entry.paymentStatus) {
        "Fully Paid" -> Color(0xFF2E7D32)
        "Advance Paid" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
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
                        color = Color.White,
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
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Payment Status Badge
                Surface(
                    color = when (entry.paymentStatus) {
                        "Fully Paid" -> Color(0xFF2E7D32)
                        "Advance Paid" -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = entry.paymentStatus,
                        color = Color.White,
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

            // Action Buttons Row: WhatsApp, Full Details, Edit, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Action
                IconButton(
                    onClick = {
                        val cleanPhone = entry.contactNumber.replace("[^0-9]".toRegex(), "")
                        val msg = "Dear ${entry.farmerName.ifBlank { "Farmer" }}, regarding your Garden Planning booking (#${entry.serialNumber}): Total Cost ${currencyFormat.format(entry.totalCost)}, Payment Status: ${entry.paymentStatus}."
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
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366)
                    )
                }

                // Full Details Button
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("View Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
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
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    var currentEntry by remember { mutableStateOf(entry) }
    var selectedTemplate by remember { mutableStateOf("Payment Received Receipt") }

    // New Installment Form state
    var newPaymentAmount by remember { mutableStateOf("") }
    var newPaymentMode by remember { mutableStateOf("Cash") }
    var newPaymentDate by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var newPaymentNote by remember { mutableStateOf("") }

    val initialLetter = currentEntry.farmerName.trim().take(1).uppercase().ifBlank { "F" }
    val totalPlants = (currentEntry.totalKanalArea * currentEntry.plantsPerKanal).toInt()

    val currentInstallments = remember(currentEntry.installmentHistoryJson) {
        parseGardenInstallments(currentEntry.installmentHistoryJson)
    }

    val copyToClipboard: (String, String) -> Unit = { label, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initialLetter,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = currentEntry.farmerName.ifBlank { "Booking Details" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Serial No: #${currentEntry.serialNumber} • ${currentEntry.bookingDate}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: FARMER DETAILS
                    DetailSectionCard(title = "FARMER DETAILS", icon = Icons.Default.Person) {
                        DetailInfoRow("Farmer Name", currentEntry.farmerName.ifBlank { "N/A" })
                        DetailInfoRow("Contact Number", currentEntry.contactNumber.ifBlank { "N/A" }) {
                            if (currentEntry.contactNumber.isNotBlank()) {
                                IconButton(onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentEntry.contactNumber}"))
                                    try { context.startActivity(dialIntent) } catch (e: Exception) {}
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        DetailInfoRow("Farmer Address", currentEntry.farmerAddress.ifBlank { "N/A" })
                        DetailInfoRow("Serial Number", "#${currentEntry.serialNumber}")
                        DetailInfoRow("Booking Date", currentEntry.bookingDate.ifBlank { "N/A" })
                    }

                    // SECTION 2: ORDER & SERVICE DETAILS
                    DetailSectionCard(title = "ORDER & SERVICE DETAILS", icon = Icons.Default.Park) {
                        DetailInfoRow("Service Category", "Garden Planning")
                        DetailInfoRow("Total Kanal Area", "${currentEntry.totalKanalArea} Kanals")
                        DetailInfoRow("Plants per Kanal", "${currentEntry.plantsPerKanal} Plants/Kanal")
                        DetailInfoRow("Total Calculated Plants", "${currencyFormat.format(totalPlants).replace("₹", "")} Plants")
                        DetailInfoRow("Cost per Plant", currencyFormat.format(currentEntry.costPerPlant))
                        if (currentEntry.plantVariety.isNotBlank()) {
                            DetailInfoRow("Plant Variety", currentEntry.plantVariety)
                        }
                        if (currentEntry.rootStock.isNotBlank()) {
                            DetailInfoRow("Root Stock", currentEntry.rootStock)
                        }
                        DetailInfoRow("Total Estimated Cost", currencyFormat.format(currentEntry.totalCost), highlight = true)
                        DetailInfoRow("Expected Delivery", currentEntry.expectedDelivery.ifBlank { "N/A" })
                        if (currentEntry.notes.isNotBlank()) {
                            DetailInfoRow("Orchard / Notes", currentEntry.notes)
                        }
                    }

                    // SECTION 3: PAYMENT BREAKDOWN & BANK ACCOUNT INFO
                    DetailSectionCard(title = "PAYMENT BREAKDOWN & ACCOUNT INFO", icon = Icons.Default.AccountBalance) {
                        // 2x2 Grid for Payment Totals to prevent text overlaps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(currencyFormat.format(currentEntry.totalCost), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Amount Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(currencyFormat.format(currentEntry.amountPaid), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Remaining Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        currencyFormat.format(currentEntry.remainingBalance),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentEntry.remainingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Payment Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(currentEntry.paymentStatus, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Bank Account Info Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL BANK & UPI DETAILS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Account Name", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Aamir Manzoor Ganaie", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Business Name", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("The Streets of Kashmir", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Account Number", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("0018010100007537", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard("Account Number", "0018010100007537") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("IFSC Code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("JAKA0MAINSR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard("IFSC Code", "JAKA0MAINSR") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("UPI ID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("streetsofkashmir@upi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard("UPI ID", "streetsofkashmir@upi") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: INSTALLMENT PAYMENT TRACKING AREA
                    DetailSectionCard(title = "INSTALLMENT PAYMENT TRACKING", icon = Icons.Default.History) {
                        // History Log
                        if (currentInstallments.isEmpty()) {
                            Text(
                                text = "No installment payments recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Payment History Log:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                currentInstallments.forEachIndexed { idx, inst ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Installment #${idx + 1} • ${inst.date}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("Mode: ${inst.paymentMode}${if (inst.note.isNotBlank()) " (${inst.note})" else ""}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                currencyFormat.format(inst.amount),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Form to Record New Installment Payment
                        Text("Record New Installment Payment:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = newPaymentAmount,
                            onValueChange = { newPaymentAmount = it },
                            label = { Text("Payment Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Payment Mode FilterChips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Cash", "UPI", "Bank", "Cheque").forEach { mode ->
                                FilterChip(
                                    selected = newPaymentMode == mode,
                                    onClick = { newPaymentMode = mode },
                                    label = { Text(mode, fontSize = 10.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newPaymentNote,
                            onValueChange = { newPaymentNote = it },
                            label = { Text("Note / Ref ID (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val amt = newPaymentAmount.toDoubleOrNull() ?: 0.0
                                if (amt <= 0) {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val newInst = GardenInstallment(
                                    amount = amt,
                                    paymentMode = newPaymentMode,
                                    date = newPaymentDate,
                                    note = newPaymentNote
                                )

                                val updatedInstallments = currentInstallments + newInst
                                val newInstallmentJson = serializeGardenInstallments(updatedInstallments)

                                val newAmountPaid = currentEntry.amountPaid + amt
                                val newRemainingBalance = (currentEntry.totalCost - newAmountPaid).coerceAtLeast(0.0)
                                val newPaymentStatus = when {
                                    newAmountPaid >= currentEntry.totalCost -> "Fully Paid"
                                    newAmountPaid > 0 -> "Advance Paid"
                                    else -> "Pending"
                                }

                                val updatedEntry = currentEntry.copy(
                                    amountPaid = newAmountPaid,
                                    remainingBalance = newRemainingBalance,
                                    paymentStatus = newPaymentStatus,
                                    installmentHistoryJson = newInstallmentJson
                                )

                                currentEntry = updatedEntry
                                viewModel.updateEntrySync(updatedEntry)

                                newPaymentAmount = ""
                                newPaymentNote = ""
                                Toast.makeText(context, "Installment of ${currencyFormat.format(amt)} recorded!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Installment Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // SECTION 5: MESSAGE PREVIEW SECTION & TEMPLATE SELECTOR
                    DetailSectionCard(title = "COMMUNICATION & MESSAGE PREVIEW", icon = Icons.Default.Message) {
                        val previewMsg = generateGardenMessageForEntry(currentEntry, selectedTemplate)

                        MessagePreviewComponent(
                            selectedTemplate = selectedTemplate,
                            onSelectTemplate = { selectedTemplate = it },
                            generatedMessage = previewMsg,
                            isDark = isDark
                        )
                    }
                }

                // Modal Bottom Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // WhatsApp Share button
                            Button(
                                onClick = {
                                    val cleanPhone = currentEntry.contactNumber.replace("[^0-9]".toRegex(), "")
                                    val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                    val msg = generateGardenMessageForEntry(currentEntry, selectedTemplate)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(msg)}")
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Message, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Receipt button next to WhatsApp button
                            OutlinedButton(
                                onClick = {
                                    val cleanPhone = currentEntry.contactNumber.replace("[^0-9]".toRegex(), "")
                                    val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

                                    val receiptData = ReceiptData(
                                        serialNumber = currentEntry.serialNumber,
                                        bookingDate = currentEntry.bookingDate,
                                        farmerName = currentEntry.farmerName,
                                        contactNumber = currentEntry.contactNumber,
                                        address = currentEntry.farmerAddress,
                                        orchardLocation = currentEntry.farmerAddress,
                                        serviceCategory = "Garden Planning",
                                        plantVariety = if (currentEntry.plantVariety.isNotBlank()) "${currentEntry.plantVariety} (${if (currentEntry.rootStock.isNotBlank()) currentEntry.rootStock else "N/A"})" else "${currentEntry.totalKanalArea} Kanals (${currentEntry.plantsPerKanal} Plants/Kanal)",
                                        quantity = totalPlants.toString(),
                                        totalAmount = currentEntry.totalCost,
                                        amountPaid = currentEntry.amountPaid,
                                        remainingBalance = currentEntry.remainingBalance,
                                        paymentStatus = currentEntry.paymentStatus,
                                        expectedDelivery = currentEntry.expectedDelivery
                                    )

                                    val bitmap = ReceiptGenerator.generateReceiptBitmap(receiptData, context)
                                    val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bitmap, currentEntry.serialNumber)
                                    val msgText = "Dear ${currentEntry.farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning (${currentEntry.serialNumber})."

                                    if (formattedPhone.isNotBlank()) {
                                        if (uri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/*"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_TEXT, msgText)
                                                setPackage("com.whatsapp")
                                                putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                val directChatIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(msgText)}")
                                                }
                                                try {
                                                    context.startActivity(directChatIntent)
                                                } catch (ex: Exception) {
                                                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            val directChatIntent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(msgText)}")
                                            }
                                            try {
                                                context.startActivity(directChatIntent)
                                            } catch (ex: Exception) {
                                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "No contact number linked to this farmer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEdit(currentEntry) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Booking", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
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
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                fontSize = if (highlight) 14.sp else 12.sp,
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
                    "Bank Account: 0018010100007537 (IFSC: JAKA0MAINSR)\n" +
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
                    "Pay via UPI: streetsofkashmir@upi or Bank Account: 0018010100007537 (JAKA0MAINSR).\n" +
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
                accentColor = Color(0xFF2E7D32),
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
                accentColor = Color(0xFFC62828),
                bgColor = if (isDark) Color(0xFF331C1C) else Color(0xFFFFEBEE),
                modifier = Modifier.weight(1f)
            )

            // Card 4: Total Quantity
            GardenSummaryCardItem(
                title = "Total Quantity",
                value = "${numberFmt.format(totalQuantity)} Plants",
                icon = Icons.Default.Inventory2,
                accentColor = Color(0xFF0288D1),
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
