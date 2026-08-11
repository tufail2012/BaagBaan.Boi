package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
            // Header
            Surface(
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
                val cleanPhone = phoneNum!!.replace("[^0-9+]".toRegex(), "")
                viewModel.contactNumber.value = cleanPhone
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Serial No.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { viewModel.serialNumber.value = it },
                label = { Text("1. Serial No. (Auto-generated)") },
                shape = pillShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("garden_serial_number_input")
            )

            IconButton(
                onClick = { viewModel.resetSerialNumber() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Serial",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 2. Farmer Name
        OutlinedTextField(
            value = farmerName,
            onValueChange = { viewModel.farmerName.value = it },
            label = { Text("2. Farmer Name *") },
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_farmer_name_input")
        )

        // 3. Farmer Address
        OutlinedTextField(
            value = farmerAddress,
            onValueChange = { viewModel.farmerAddress.value = it },
            label = { Text("3. Farmer Address") },
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_farmer_address_input")
        )

        // 4. Contact Number with contact-picker icon
        OutlinedTextField(
            value = contactNumber,
            onValueChange = { viewModel.contactNumber.value = it },
            label = { Text("4. Contact Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = pillShape,
            trailingIcon = {
                IconButton(onClick = {
                    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        try { contactPickerLauncher.launch(null) } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open contacts", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "Select Contact",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_contact_number_input")
        )

        // 5. Total Kanal Area
        OutlinedTextField(
            value = totalKanalArea,
            onValueChange = { viewModel.totalKanalArea.value = it },
            label = { Text("5. Total Kanal Area (e.g. 5.5)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_kanal_area_input")
        )

        // 6. Number of Plants per Kanal
        OutlinedTextField(
            value = plantsPerKanal,
            onValueChange = { viewModel.plantsPerKanal.value = it },
            label = { Text("6. Plants per Kanal (e.g. 100)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_plants_per_kanal_input")
        )

        // 7. Cost per Plant
        OutlinedTextField(
            value = costPerPlant,
            onValueChange = { viewModel.costPerPlant.value = it },
            label = { Text("7. Cost per Plant (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_cost_per_plant_input")
        )

        // 8. Total Cost (Read-Only Summary)
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
                        text = "8. TOTAL COST (AUTO-CALCULATED)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$totalKanalArea Kanals × $plantsPerKanal/Kanal = ${(totalKanalArea.toDoubleOrNull() ?: 0.0) * (plantsPerKanal.toIntOrNull() ?: 0)} Total Plants",
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

        // 9. Payment Status
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "9. Payment Status",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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

        // 10. Booking Date & Expected Delivery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = bookingDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("10A. Booking Date") },
                shape = pillShape,
                trailingIcon = {
                    IconButton(onClick = { openDatePicker(true) }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clickable { openDatePicker(true) }
            )

            OutlinedTextField(
                value = expectedDelivery,
                onValueChange = {},
                readOnly = true,
                label = { Text("10B. Expected Delivery") },
                shape = pillShape,
                trailingIcon = {
                    IconButton(onClick = { openDatePicker(false) }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clickable { openDatePicker(false) }
            )
        }

        // 11. Notes / Inspection Remarks
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = { Text("11. Notes / Inspection Remarks") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("garden_notes_input")
        )

        // 12. Message Preview
        val previewMsg = viewModel.getGeneratedPreviewMessage()
        MessagePreviewComponent(
            selectedTemplate = selectedTemplate,
            onSelectTemplate = { viewModel.selectedTemplate.value = it },
            generatedMessage = previewMsg,
            isDark = isDark
        )

        // 13. Action Buttons Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

                        val bitmap = ReceiptGenerator.generateReceiptBitmap(receiptData)
                        val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bitmap, serialNumber)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Dear ${farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning ($serialNumber).")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Digital Receipt"))
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
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search by Farmer, Serial, or Contact") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("garden_records_search_input")
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No garden planning entries found." else "No matching records found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                Text(
                                    text = "#${entry.serialNumber}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    color = when (entry.paymentStatus) {
                                        "Fully Paid" -> Color(0xFF2E7D32)
                                        "Advance Paid" -> Color(0xFFE65100)
                                        else -> Color(0xFFC62828)
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

                                    val bitmap = ReceiptGenerator.generateReceiptBitmap(receiptData)
                                    val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bitmap, entry.serialNumber)
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_TEXT, "Dear ${entry.farmerName.ifBlank { "Farmer" }}, here is your official digital receipt for Garden Planning (${entry.serialNumber}).")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Digital Receipt"))
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
}
