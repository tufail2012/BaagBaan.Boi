package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentUris
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.GardenPlanningEntry
import com.example.ui.GardenPlanningViewModel
import com.example.util.MessagePreviewComponent
import com.example.util.ReceiptData
import com.example.util.ReceiptGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenPlanningScreen(
    viewModel: GardenPlanningViewModel,
    onBack: () -> Unit,
    isDark: Boolean = isAppInDarkMode(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val userMessage by viewModel.userMessage.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val filteredEntries by viewModel.filteredEntries.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.userMessage.value = null
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with status bar padding
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("garden_planning_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Garden Planning",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Area & Cost Calculation • ${allEntries.size} Records",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            if (viewModel.editingEntryId.collectAsState().value != null) "EDIT ENTRY" else "NEW ENTRY",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("RECORDS (${allEntries.size})", fontWeight = FontWeight.Bold) }
                )
            }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header: SERIAL & FARMER DETAILS
        Text(
            text = "SERIAL & FARMER DETAILS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Serial Number Field Pattern
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { viewModel.serialNumber.value = it },
            label = { Text("Serial No. (Garden Planning) *") },
            placeholder = { Text("e.g. GP-01") },
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

        // Section Header: GARDEN SPECIFICATION
        Text(
            text = "GARDEN SPECIFICATION",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Total Kanal Area
        OutlinedTextField(
            value = totalKanalArea,
            onValueChange = { viewModel.totalKanalArea.value = it },
            label = { Text("Total Kanal Area (Kanals) *") },
            placeholder = { Text("e.g. 5.5") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_kanal_area_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Plants per Kanal
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

        // Cost per Plant
        OutlinedTextField(
            value = costPerPlant,
            onValueChange = { viewModel.costPerPlant.value = it },
            label = { Text("Cost per Plant (₹) *") },
            placeholder = { Text("e.g. 150") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("garden_cost_per_plant_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Total Cost Summary Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
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
                Column {
                    Text(
                        text = "TOTAL COST (AUTO-CALCULATED)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${totalKanalArea.ifBlank { "0" }} Kanals × ${plantsPerKanal.ifBlank { "0" }}/Kanal = ${((totalKanalArea.toDoubleOrNull() ?: 0.0) * (plantsPerKanal.toIntOrNull() ?: 0)).toInt()} Total Plants",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = totalCostFormatted,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section Header: PAYMENT & DATES
        Text(
            text = "PAYMENT & DATES",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Payment Status
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
                        onClick = { viewModel.paymentStatus.value = status },
                        label = { Text(status, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Booking Date & Expected Delivery
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
                label = { Text("Expected Delivery *") },
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

        // Action Buttons Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("garden_save_button")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingEntryId != null) "UPDATE GARDEN PLAN" else "SAVE BOOKING ENTRY",
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share WhatsApp
                OutlinedButton(
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 12.sp)
                }

                // Share SMS
                OutlinedButton(
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Receipt Image Button
                OutlinedButton(
                    onClick = {
                        val area = totalKanalArea.toDoubleOrNull() ?: 0.0
                        val plants = plantsPerKanal.toIntOrNull() ?: 0
                        val totalPlants = (area * plants).toInt()
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
                            amountPaid = if (paymentStatus == "Fully Paid") totalAmount else 0.0,
                            remainingBalance = if (paymentStatus == "Fully Paid") 0.0 else totalAmount,
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receipt Image", fontSize = 12.sp)
                }

                // Clear Form
                OutlinedButton(
                    onClick = { viewModel.clearForm() },
                    shape = pillShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Form", fontSize = 12.sp)
                }
            }
        }
    }
}

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

    // Financial & Quantity Summary Metrics
    val totalPayment = entries.sumOf { it.totalCost }
    val receivedPayment = entries.sumOf { entry ->
        when (entry.paymentStatus) {
            "Fully Paid" -> entry.totalCost
            "Advance Paid" -> entry.totalCost * 0.5
            else -> 0.0
        }
    }
    val pendingPayment = totalPayment - receivedPayment
    val totalQuantity = entries.sumOf { (it.totalKanalArea * it.plantsPerKanal).toInt() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Garden Planning Recording Book",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${entries.size} ${if (entries.size == 1) "entry" else "entries"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 1. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by farmer name, phone, serial no...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = searchShape,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .boundedFormFieldRipple(shape = searchShape)
                    .elevated3dShadow(shape = searchShape, isDark = isDark)
                    .testTag("garden_records_search_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // 2. Filter by Payment Status
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterOptions = listOf("All Records", "Payments Cleared", "Payments Pending")
                filterOptions.forEach { option ->
                    val isSelected = selectedPaymentFilter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPaymentFilter(option) },
                        label = {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF1F5F9),
                            labelColor = if (isDark) Color.White else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .boundedFormFieldRipple(shape = RoundedCornerShape(16.dp))
                            .testTag("garden_filter_chip_${option.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        // 3. Summary Statistics Cards
        item {
            GardenRecordSummaryCards(
                totalPayment = totalPayment,
                receivedPayment = receivedPayment,
                pendingPayment = pendingPayment,
                totalQuantity = totalQuantity,
                isDark = isDark
            )
        }

        // 4. Records List or Empty State
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
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
                                "No entries saved in the Garden Planning Recording Book yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "#${entry.serialNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = when (entry.paymentStatus) {
                                    "Fully Paid" -> MaterialTheme.colorScheme.primary
                                    "Advance Paid" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = entry.paymentStatus,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = entry.farmerName.ifBlank { "Farmer Name Not Specified" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (entry.farmerAddress.isNotBlank()) {
                            Text(
                                text = "📍 ${entry.farmerAddress}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (entry.contactNumber.isNotBlank()) {
                            Text(
                                text = "📞 ${entry.contactNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.totalKanalArea} Kanals • ${entry.plantsPerKanal} Plants/Kanal",
                                fontSize = 13.sp,
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

                        if (entry.notes.isNotBlank()) {
                            Text(
                                text = "Notes: ${entry.notes}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onEdit(entry) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = {
                                val totalPlants = (entry.totalKanalArea * entry.plantsPerKanal).toInt()
                                val receiptData = ReceiptData(
                                    serialNumber = entry.serialNumber,
                                    bookingDate = entry.bookingDate,
                                    farmerName = entry.farmerName,
                                    contactNumber = entry.contactNumber,
                                    address = entry.farmerAddress,
                                    orchardLocation = entry.farmerAddress,
                                    serviceCategory = "Garden Planning",
                                    plantVariety = "${entry.totalKanalArea} Kanals (${entry.plantsPerKanal} Plants/Kanal)",
                                    quantity = totalPlants.toString(),
                                    totalAmount = entry.totalCost,
                                    amountPaid = if (entry.paymentStatus == "Fully Paid") entry.totalCost else 0.0,
                                    remainingBalance = if (entry.paymentStatus == "Fully Paid") 0.0 else entry.totalCost,
                                    paymentStatus = entry.paymentStatus,
                                    expectedDelivery = entry.expectedDelivery
                                )

                                val bitmap = ReceiptGenerator.generateReceiptBitmap(receiptData, context)
                                val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bitmap, entry.serialNumber)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "Dear ${entry.farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning (${entry.serialNumber}).")
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
                                            putExtra(Intent.EXTRA_TEXT, "Dear ${entry.farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning (${entry.serialNumber}).")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(fallbackIntent, "Share Digital Receipt"))
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Receipt",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = { viewModel.deleteEntry(entry) }) {
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
