package com.example.ui.components

import androidx.compose.material3.LocalTextStyle

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import com.example.util.ReceiptData
import com.example.util.ReceiptGenerator
import com.example.ui.CropViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FarmerFormScreen(
    viewModel: CropViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var multiplePhoneNumbers by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var selectedContactNameForDialog by remember { mutableStateOf<String?>(null) }

    val formatPhoneNumber = { rawInput: String ->
        val pfx = "+91 "
        var digits = rawInput.replace("[^0-9]".toRegex(), "")
        if (digits.startsWith("91") && digits.length > 10) {
            digits = digits.substring(digits.length - 10)
        }
        if (digits.length > 10) {
            digits = digits.takeLast(10)
        }
        if (digits.isNotEmpty()) {
            pfx + digits
        } else {
            pfx
        }
    }

    val processSelectedContact: (Uri) -> Unit = { contactUri ->
        Log.d("FarmerFormScreen", "Processing contact URI: $contactUri")
        var contactId: String? = null
        var displayName: String? = null
        val phoneNumbers = mutableListOf<Pair<String, String>>()

        try {
            // Query 1: Query Contact details (ID and Display Name)
            context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                ),
                null,
                null,
                null
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
                contactId = try {
                    ContentUris.parseId(contactUri).toString()
                } catch (e: Exception) {
                    contactUri.lastPathSegment
                }
            }

            Log.d("FarmerFormScreen", "Resolved Contact ID: $contactId, Display Name: $displayName")

            // Query 2: Fetch phone numbers associated with this Contact ID
            if (!contactId.isNullOrEmpty()) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val nameIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    while (phoneCursor.moveToNext()) {
                        if (numIdx >= 0) {
                            val num = phoneCursor.getString(numIdx)
                            if (!num.isNullOrBlank()) {
                                val type = if (typeIdx >= 0) phoneCursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                                val customLabel = if (labelIdx >= 0) phoneCursor.getString(labelIdx) else null
                                val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                    context.resources,
                                    type,
                                    customLabel ?: ""
                                ).toString()

                                if (displayName.isNullOrBlank() && nameIdx >= 0) {
                                    val fetchedName = phoneCursor.getString(nameIdx)
                                    if (!fetchedName.isNullOrBlank()) displayName = fetchedName
                                }

                                val cleanNum = num.replace("[^0-9+]".toRegex(), "")
                                if (phoneNumbers.none { it.first.replace("[^0-9+]".toRegex(), "") == cleanNum }) {
                                    phoneNumbers.add(Pair(num, typeLabel))
                                }
                            }
                        }
                    }
                }
            }

            // Query 3: Fallback query directly on contactUri if no phone numbers retrieved yet
            if (phoneNumbers.isEmpty()) {
                context.contentResolver.query(
                    contactUri,
                    null,
                    null,
                    null,
                    null
                )?.use { fallbackCursor ->
                    if (fallbackCursor.moveToFirst()) {
                        val numIdx = fallbackCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIdx = fallbackCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (numIdx >= 0) {
                            val num = fallbackCursor.getString(numIdx)
                            if (!num.isNullOrBlank()) {
                                phoneNumbers.add(Pair(num, "Phone"))
                            }
                        }
                        if (displayName.isNullOrBlank() && nameIdx >= 0) {
                            val fetchedName = fallbackCursor.getString(nameIdx)
                            if (!fetchedName.isNullOrBlank()) displayName = fetchedName
                        }
                    }
                }
            }

            // Populate Farmer Name into ViewModel
            if (!displayName.isNullOrBlank()) {
                viewModel.farmerName.value = displayName!!
            }

            // Handle Phone Numbers
            if (phoneNumbers.isEmpty()) {
                Toast.makeText(context, "Selected contact has no phone number", Toast.LENGTH_SHORT).show()
            } else if (phoneNumbers.size == 1) {
                val formatted = formatPhoneNumber(phoneNumbers.first().first)
                viewModel.contactNumber.value = formatted
                Toast.makeText(context, "Contact number added", Toast.LENGTH_SHORT).show()
            } else {
                selectedContactNameForDialog = displayName
                multiplePhoneNumbers = phoneNumbers
            }

        } catch (e: Exception) {
            Log.e("FarmerFormScreen", "Error resolving contact details", e)
            Toast.makeText(context, "Failed to read contact details", Toast.LENGTH_SHORT).show()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            Log.d("FarmerFormScreen", "Selected contact URI: $contactUri")
            processSelectedContact(contactUri)
        } else {
            Log.d("FarmerFormScreen", "Contact picker cancelled or no contact selected")
        }
    }

    val launchContactPicker = {
        try {
            contactPickerLauncher.launch(null)
        } catch (e: ActivityNotFoundException) {
            Log.e("FarmerFormScreen", "No contact picker activity found", e)
            Toast.makeText(context, "No contact picker app found on device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("FarmerFormScreen", "Failed to launch contact picker", e)
            Toast.makeText(context, "Unable to open contacts picker", Toast.LENGTH_SHORT).show()
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchContactPicker()
        } else {
            Toast.makeText(context, "Contacts permission is required to select a contact", Toast.LENGTH_SHORT).show()
        }
    }
    val serialNumber by viewModel.serialNumber.collectAsState()
    val farmerName by viewModel.farmerName.collectAsState()
    val farmerAddress by viewModel.farmerAddress.collectAsState()
    val contactNumber by viewModel.contactNumber.collectAsState()
    val serviceType by viewModel.serviceType.collectAsState()
    val plantVariety by viewModel.plantVariety.collectAsState()
    val rootstock by viewModel.rootstock.collectAsState()
    val importCountry by viewModel.importCountry.collectAsState()
    val rootDiameter by viewModel.rootDiameter.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val landAreaAcres by viewModel.landAreaAcres.collectAsState()
    val soilType by viewModel.soilType.collectAsState()
    val healthStage by viewModel.healthStage.collectAsState()
    val location by viewModel.location.collectAsState()
    val visitDate by viewModel.visitDate.collectAsState()
    val soilHealthObservations by viewModel.soilHealthObservations.collectAsState()
    val plantHealthObservations by viewModel.plantHealthObservations.collectAsState()
    val graftType by viewModel.graftType.collectAsState()
    val scionVariety by viewModel.scionVariety.collectAsState()
    val perUnitGraftingCharge by viewModel.perUnitGraftingCharge.collectAsState()
    val graftingCharges by viewModel.graftingCharges.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val amountPaid by viewModel.amountPaid.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val bookingDate by viewModel.bookingDate.collectAsState()
    val expectedDelivery by viewModel.expectedDelivery.collectAsState()
    val paymentProofUri by viewModel.paymentProofUri.collectAsState()
    val paymentProofName by viewModel.paymentProofName.collectAsState()
    val editingId by viewModel.editingRecordId.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedPruningSubTab by viewModel.selectedPruningSubTab.collectAsState()
    val selectedRootstockSubTab by viewModel.selectedRootstockSubTab.collectAsState()
    val selectedGenevaOption by viewModel.selectedGenevaOption.collectAsState()

    var selectedTemplate by remember { mutableStateOf("Booking Confirmation") }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    val templateOptions = com.example.util.MessageTemplateHelper.TEMPLATE_OPTIONS
    val clipboardManager = LocalClipboardManager.current

    var generatedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var generatedReceiptBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    var showWaFormConfirmDialog by remember { mutableStateOf(false) }
    var showSmsFormConfirmDialog by remember { mutableStateOf(false) }
    var showShareFormReceiptConfirmDialog by remember { mutableStateOf(false) }
    var showWaFormReceiptConfirmDialog by remember { mutableStateOf(false) }

    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.paymentProofUri.value = uri.toString()
            viewModel.paymentProofName.value = uri.lastPathSegment ?: "UPI_Payment_Proof.jpg"
            Toast.makeText(context, "Payment proof attached successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val storagePermissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proofPickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Storage permission denied. Upload operation cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    val requestStoragePermissionAndUpload = {
        if (ContextCompat.checkSelfPermission(context, storagePermissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            proofPickerLauncher.launch("image/*")
        } else {
            storagePermissionLauncher.launch(storagePermissionToRequest)
        }
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var datePickerInitialStr by remember { mutableStateOf("") }
    var datePickerOnSelected by remember { mutableStateOf<(String) -> Unit>({}) }

    val showDatePicker: (String, (String) -> Unit) -> Unit = { initialDateStr, onDateSelected ->
        datePickerInitialStr = initialDateStr
        datePickerOnSelected = onDateSelected
        showDatePickerDialog = true
    }

    var showManualDateDialog by remember { mutableStateOf(false) }
    var manualDateLabel by remember { mutableStateOf("") }
    var manualDateText by remember { mutableStateOf("") }
    var onManualDateSaved by remember { mutableStateOf<(String) -> Unit>({}) }

    val openManualDateDialog: (String, String, (String) -> Unit) -> Unit = { label, currentValue, onSave ->
        manualDateLabel = label
        manualDateText = currentValue
        onManualDateSaved = onSave
        showManualDateDialog = true
    }

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

    var visitDateTFV by remember {
        mutableStateOf(TextFieldValue(text = visitDate, selection = TextRange(visitDate.length)))
    }
    LaunchedEffect(visitDate) {
        if (visitDate != visitDateTFV.text) {
            visitDateTFV = TextFieldValue(text = visitDate, selection = TextRange(visitDate.length))
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

    val scrollState = rememberScrollState()
    scrollState.rememberScrollHapticFeedback()

    LaunchedEffect(selectedService, selectedPruningSubTab, selectedRootstockSubTab, selectedGenevaOption) {
        scrollState.scrollTo(0)
    }

    var soilMenuExpanded by remember { mutableStateOf(false) }
    var saplingAgeMenuExpanded by remember { mutableStateOf(false) }

    val soilOptions = listOf("Clay Loam", "Sandy Loam", "Rich Alluvial", "Peaty", "Chalky / Rocky")
    val saplingAgeOptions = listOf("1 Year", "2 Years", "3 Years", "4 Years", "5 Years")

    val textFieldShape = RoundedCornerShape(16.dp)
    val isDark = isAppInDarkMode()

    val totalPayment = (quantity.toDoubleOrNull() ?: 0.0) * (landAreaAcres.toDoubleOrNull() ?: 0.0)
    val paidAmountNum = amountPaid.toDoubleOrNull() ?: 0.0
    val remainingBalance = maxOf(0.0, totalPayment - paidAmountNum)

    val farmerNameStr = if (farmerName.isBlank()) "Valued Farmer" else farmerName
    val contactNumberStr = if (contactNumber.isBlank()) "N/A" else contactNumber
    val serialStr = if (serialNumber.isBlank()) "N/A" else serialNumber
    val varietyStr = if (plantVariety.isNotBlank()) plantVariety else if (rootstock.isNotBlank()) rootstock else selectedService
    val qtyStr = quantity
    val totalAmtFormatted = "₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalPayment.toLong())}"
    val paidAmtFormatted = "₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(paidAmountNum.toLong())}"
    val remBalFormatted = "₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(remainingBalance.toLong())}"
    val bookingDateStr = if (bookingDate.isBlank()) "N/A" else bookingDate
    val deliveryDateStr = if (expectedDelivery.isBlank()) "To be scheduled" else expectedDelivery

    val generatedMessage = when (selectedTemplate) {
        "Payment Reminder" -> """
            Dear $farmerNameStr,

            This is a friendly payment reminder for your $selectedService booking ($serialStr - $varietyStr).

            • Total Amount: $totalAmtFormatted
            • Amount Paid: $paidAmtFormatted
            • Remaining Balance: $remBalFormatted
            • Payment Status: $paymentStatus

            Please clear the balance of $remBalFormatted at your earliest convenience. Thank you!
        """.trimIndent()

        "Delivery Scheduled" -> """
            Dear $farmerNameStr,

            Your $selectedService order ($serialStr - $varietyStr, Qty: $qtyStr) is scheduled for delivery/fulfillment.

            • Expected Delivery: $deliveryDateStr
            • Remaining Balance: $remBalFormatted
            • Address: ${if (farmerAddress.isNotBlank()) farmerAddress else "Registered Address"}

            Thank you for choosing our agricultural service!
        """.trimIndent()

        "Thank You Note" -> """
            Dear $farmerNameStr,

            Thank you for booking $selectedService ($varietyStr) with us!

            Ref #: $serialStr | Quantity: $qtyStr plants
            Booking Date: $bookingDateStr

            We appreciate your trust in our nursery and wish you a fruitful harvest season!
        """.trimIndent()

        "Live" -> """
            📱 LIVE BOOKING PREVIEW ($selectedService)
            ----------------------------------
            Serial #: $serialStr
            Farmer: $farmerNameStr ($contactNumberStr)
            Address: ${if (farmerAddress.isNotBlank()) farmerAddress else "N/A"}
            Category/Variety: $varietyStr (Qty: $qtyStr)
            Total: $totalAmtFormatted | Paid: $paidAmtFormatted
            Balance: $remBalFormatted | Status: $paymentStatus
            Booking Date: $bookingDateStr | Delivery: $deliveryDateStr
        """.trimIndent()

        else -> """
            🧾 BAAGBAAN BOI
            Ramnagri 192303
            Contacts: +916006143037, +917006996169, +917051826858, +916005096439

            OFFICIAL DIGITAL RECEIPT
            ----------------------------------
            FARMER / CUSTOMER DETAILS:
            • Receipt / Serial #: $serialStr
            • Booking Date: $bookingDateStr
            • Customer Name: $farmerNameStr
            • Contact Phone: $contactNumberStr
            • Address: ${if (farmerAddress.isNotBlank()) farmerAddress else "N/A"}
            • Orchard / Location: ${if (location.isNotBlank()) location else "N/A"}

            ORDER & SERVICE DETAILS:
            • Category: $selectedService
            • Variety / Item: $varietyStr
            • Quantity: $qtyStr plants
            • Expected Delivery: $deliveryDateStr

            PAYMENT BREAKDOWN:
            • Total Amount: $totalAmtFormatted
            • Advance Paid: $paidAmtFormatted
            • Balance Due: $remBalFormatted
            • Payment Status: $paymentStatus
            ----------------------------------
            Thank you for choosing Baagbaan Boi!
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Serial Number Manual Field with inner Save Icon & Full Width
        val isSerialLocked by viewModel.isSerialLocked.collectAsState()

        OutlinedTextField(
            value = serialNumber,
            onValueChange = { 
                if (!isSerialLocked) {
                    viewModel.updateSerialNumber(it)
                }
            },
            readOnly = isSerialLocked,
            label = { Text("Serial No. ($serviceType) *") },
            placeholder = { Text("Type serial number (e.g. LP-1001)") },
            shape = textFieldShape,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                .testTag("serial_number_input"),
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
                        onClick = { viewModel.generateNewSerialNumber() },
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

        // Section Title: FARMER DETAILS
        Text(
            text = "FARMER DETAILS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Farmer Name
        OutlinedTextField(
            value = farmerName,
            onValueChange = { viewModel.farmerName.value = it },
            label = { Text("Farmer Name *") },
            placeholder = { Text("e.g. Mohammad Abdullah") },
            shape = textFieldShape,
            singleLine = true,
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
                .testTag("farmer_name_input"),
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
                .testTag("farmer_address_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Contact Number
        OutlinedTextField(
            value = contactTextFieldValue,
            onValueChange = { newValue ->
                val rawText = newValue.text

                // Extract digits typed after "+91 " prefix or from user input
                var cleanDigits = if (rawText.startsWith(prefix)) {
                    rawText.substring(prefix.length).filter { it.isDigit() }
                } else {
                    // If user tried to delete or backspace into "+91 "
                    rawText.removePrefix("+91").removePrefix("+").filter { it.isDigit() }
                }

                if (cleanDigits.length > 10) {
                    cleanDigits = cleanDigits.take(10)
                }

                val formattedText = prefix + cleanDigits

                // Prevent cursor from going before "+91 "
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
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            launchContactPicker()
                        } else {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
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
                                selection = TextRange(initialText.length)
                            )
                        } else {
                            val selStart = maxOf(prefix.length, contactTextFieldValue.selection.start)
                            val selEnd = maxOf(prefix.length, contactTextFieldValue.selection.end)
                            contactTextFieldValue = contactTextFieldValue.copy(
                                selection = TextRange(selStart, selEnd)
                            )
                        }
                    }
                }
                .testTag("contact_number_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )


        val isImportedPlants = serviceType.equals("Imported", ignoreCase = true)
        val isImportedRootstocks = serviceType.equals("Rootstocks", ignoreCase = true)
        val isSiteVisit = serviceType.equals("Site Visit", ignoreCase = true)
        val isPruning = serviceType.equals("Pruning", ignoreCase = true)

        val specTitle = when {
            isImportedPlants -> "IMPORTED PLANTS SPECIFICATION"
            isImportedRootstocks -> "IMPORTED ROOTSTOCKS SPECIFICATION"
            isSiteVisit -> "SITE VISIT SPECIFICATION"
            isPruning -> "PRUNING SPECIFICATION"
            else -> "$serviceType SPECIFICATION".uppercase()
        }

        // Section Title: CROP / PLANT / SITE VISIT SPECIFICATION
        Text(
            text = specTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (isSiteVisit) {
            // 1. Visit Date
            OutlinedTextField(
                value = visitDateTFV,
                onValueChange = { newVal ->
                    val formatted = formatAutoSlashDate(visitDateTFV.text, newVal.text)
                    val newPos = if (formatted.length > visitDateTFV.text.length && formatted.endsWith("/")) {
                        formatted.length
                    } else if (newVal.selection.end <= formatted.length) {
                        newVal.selection.end
                    } else {
                        formatted.length
                    }
                    visitDateTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                    viewModel.visitDate.value = formatted
                },
                label = { Text("Visit Date *") },
                placeholder = { Text("DD/MM/YYYY") },
                shape = textFieldShape,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            showDatePicker(visitDate.ifBlank { bookingDate }) { selected ->
                                viewModel.visitDate.value = selected
                                if (viewModel.bookingDate.value.isBlank()) {
                                    viewModel.bookingDate.value = selected
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("visit_date_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Visit Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("visit_date_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 2. Soil Health Observations
            OutlinedTextField(
                value = soilHealthObservations,
                onValueChange = { viewModel.soilHealthObservations.value = it },
                label = { Text("Soil Health Observations") },
                placeholder = { Text("e.g. Good organic content, pH 6.5, well drained") },
                shape = textFieldShape,
                singleLine = false,
                maxLines = 3,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("soil_health_observations_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 3. Plant Health Observations
            OutlinedTextField(
                value = plantHealthObservations,
                onValueChange = { viewModel.plantHealthObservations.value = it },
                label = { Text("Plant Health Observations") },
                placeholder = { Text("e.g. Healthy foliage, minor pest damage on lower leaves") },
                shape = textFieldShape,
                singleLine = false,
                maxLines = 3,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("plant_health_observations_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 4. Orchard/Site Location
            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.location.value = it },
                label = { Text("Orchard/Site Location *") },
                placeholder = { Text("e.g. Block A, North Field, Village Green Valley") },
                shape = textFieldShape,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("orchard_site_location_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        } else if (isPruning) {
            // Orchard Location (Single field for Pruning Specification)
            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.location.value = it },
                label = { Text("Orchard Location *") },
                placeholder = { Text("e.g. Block A, North Field, Village Green Valley") },
                shape = textFieldShape,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("orchard_location_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        } else {
            // 1. Plant Variety (Manual Input Text Field) - Removed for Imported Rootstocks
            if (!isImportedRootstocks) {
                OutlinedTextField(
                    value = plantVariety,
                    onValueChange = { viewModel.plantVariety.value = it },
                    label = { Text("Plant Variety *") },
                    placeholder = { Text("Type plant variety (e.g. Gala Apple, Cherry, Wheat)") },
                    shape = textFieldShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocalFlorist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("plant_variety_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
            }

            // 2. Rootstock (Manual Input Text Field)
            OutlinedTextField(
                value = rootstock,
                onValueChange = { viewModel.rootstock.value = it },
                label = { Text("Rootstock *") },
                placeholder = { Text("Type rootstock (e.g. M9, MM106, Seedling)") },
                shape = textFieldShape,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = textFieldShape)
                    .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                    .testTag("rootstock_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 3. Country / Source of Import (Manual Text Input for Imported Plants and Imported Rootstocks)
            if (isImportedPlants || isImportedRootstocks) {
                OutlinedTextField(
                    value = importCountry,
                    onValueChange = { viewModel.importCountry.value = it },
                    label = { Text("Country / Source of Import *") },
                    placeholder = { Text("Enter country or source of import (e.g. Italy, Netherlands)") },
                    shape = textFieldShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("import_country_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
            }

            // 4. Root Diameter ('mm') (Manual Text Input for Imported Rootstocks)
            if (isImportedRootstocks) {
                OutlinedTextField(
                    value = rootDiameter,
                    onValueChange = { viewModel.rootDiameter.value = it },
                    label = { Text("Root Diameter ('mm') *") },
                    placeholder = { Text("e.g. 9 to 12 mm") },
                    shape = textFieldShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("root_diameter_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
            }

            // 3. Sapling Age (Dropdown Field with Defined Age Options) - Removed for Imported Rootstocks
            if (!isImportedRootstocks) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saplingAgeMenuExpanded = true }
                        .boundedFormFieldRipple(shape = textFieldShape) { saplingAgeMenuExpanded = true }
                ) {
                    OutlinedTextField(
                        value = healthStage.ifBlank { "1 Year" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sapling Age *") },
                        shape = textFieldShape,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { saplingAgeMenuExpanded = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saplingAgeMenuExpanded = true }
                            .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                            .testTag("sapling_age_dropdown"),
                        colors = elevatedInputFieldColors(isDark = isDark)
                    )

                    DropdownMenu(
                        expanded = saplingAgeMenuExpanded,
                        onDismissRequest = { saplingAgeMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        saplingAgeOptions.forEach { age ->
                            DropdownMenuItem(
                                text = { Text(age) },
                                onClick = {
                                    viewModel.healthStage.value = age
                                    saplingAgeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section Title: GRAFTING DETAILS (Only for Imported Rootstocks)
        if (isImportedRootstocks) {
            Text(
                text = "GRAFTING DETAILS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Scion Variety (Manual Text Field)
            OutlinedTextField(
                value = scionVariety,
                onValueChange = { viewModel.scionVariety.value = it },
                label = { Text("Scion Variety") },
                placeholder = { Text("Enter scion variety (e.g. Honeycrisp, Gala)") },
                shape = textFieldShape,
                singleLine = true,
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
                    .testTag("scion_variety_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            var graftTypeMenuExpanded by remember { mutableStateOf(false) }
            val graftTypeOptions = listOf("Bench Grafting", "Tongue Grafting", "T-Budding", "Chip Budding", "Cleft Grafting", "Whip & Tongue", "Bark Grafting")

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = graftType,
                    onValueChange = { viewModel.graftType.value = it },
                    label = { Text("Graft Type") },
                    placeholder = { Text("Type or select graft type (e.g. Bench Grafting)") },
                    shape = textFieldShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { graftTypeMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Graft Type")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .boundedFormFieldRipple(shape = textFieldShape)
                        .elevated3dShadow(shape = textFieldShape, isDark = isDark)
                        .testTag("graft_type_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )

                DropdownMenu(
                    expanded = graftTypeMenuExpanded,
                    onDismissRequest = { graftTypeMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    graftTypeOptions.forEach { typeOption ->
                        DropdownMenuItem(
                            text = { Text(typeOption) },
                            onClick = {
                                viewModel.graftType.value = typeOption
                                graftTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Section Title: PRICING & QUANTITY / PRICING & VISIT DETAILS
        Text(
            text = if (isSiteVisit) "PRICING & VISIT DETAILS" else "PRICING & QUANTITY",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        val pillShape = RoundedCornerShape(28.dp)

        val sanitizeCurrencyInput: (String) -> String = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            val clean = if (filtered.count { it == '.' } <= 1) {
                filtered
            } else {
                val firstDotIndex = filtered.indexOf('.')
                val sb = StringBuilder()
                filtered.forEachIndexed { index, c ->
                    if (c.isDigit() || index == firstDotIndex) sb.append(c)
                }
                sb.toString()
            }
            val parts = clean.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                parts[0] + "." + parts[1].take(2)
            } else {
                clean
            }
        }

        if (isImportedRootstocks) {
            val isUnitPriceError = landAreaAcres.isNotBlank() && (landAreaAcres.toDoubleOrNull() ?: 0.0) <= 0.0
            val isGraftChargeError = perUnitGraftingCharge.isNotBlank() && (perUnitGraftingCharge.toDoubleOrNull() ?: -1.0) < 0.0

            // 1. Quantity (Roots) on its own line at the top
            OutlinedTextField(
                value = quantity,
                onValueChange = { 
                    viewModel.quantity.value = it 
                    viewModel.updateQuantityOrPrice()
                },
                label = { Text("Quantity (Roots) *") },
                placeholder = { Text("Enter quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = pillShape,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .boundedFormFieldRipple(shape = pillShape)
                    .elevated3dShadow(shape = pillShape, isDark = isDark)
                    .testTag("quantity_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 2. Unit Price & Graft Charge / Unit Side-by-Side below Quantity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit Price
                OutlinedTextField(
                    value = landAreaAcres,
                    onValueChange = { 
                        viewModel.landAreaAcres.value = sanitizeCurrencyInput(it)
                        viewModel.updateQuantityOrPrice()
                    },
                    label = { 
                        Text(
                            text = "Unit Price (₹) *", 
                            fontSize = 12.sp,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    placeholder = { 
                        Text(
                            text = "Enter price", 
                            fontSize = 12.sp,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    isError = isUnitPriceError,
                    supportingText = if (isUnitPriceError) {
                        { Text("Must be > 0", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = pillShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .boundedFormFieldRipple(shape = pillShape)
                        .elevated3dShadow(shape = pillShape, isDark = isDark)
                        .testTag("unit_price_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )

                // Graft Charge / Unit
                OutlinedTextField(
                    value = perUnitGraftingCharge,
                    onValueChange = { 
                        viewModel.perUnitGraftingCharge.value = sanitizeCurrencyInput(it)
                        viewModel.updateQuantityOrPrice()
                    },
                    label = { 
                        Text(
                            text = "Graft Charge / Unit (₹)", 
                            fontSize = 12.sp,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    placeholder = { 
                        Text(
                            text = "Per unit charge", 
                            fontSize = 12.sp,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    isError = isGraftChargeError,
                    supportingText = if (isGraftChargeError) {
                        { Text("Must be ≥ 0", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = pillShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .boundedFormFieldRipple(shape = pillShape)
                        .elevated3dShadow(shape = pillShape, isDark = isDark)
                        .testTag("grafting_charges_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
            }

            if (graftingCharges.isNotBlank() && (graftingCharges.toDoubleOrNull() ?: 0.0) > 0.0) {
                Text(
                    text = "Total Grafting Charges: ₹$graftingCharges",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
        } else {
            // Side-by-Side: Quantity & Unit Price for other categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Quantity (Plants / Visits)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        viewModel.quantity.value = it 
                        viewModel.updateQuantityOrPrice()
                    },
                    label = { Text(if (isSiteVisit) "No. of Visits *" else "Quantity *") },
                    placeholder = { Text(if (isSiteVisit) "e.g. 1" else "Enter quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = pillShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .boundedFormFieldRipple(shape = pillShape)
                        .elevated3dShadow(shape = pillShape, isDark = isDark)
                        .testTag("quantity_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )

                // 2. Unit Price / Visit Charge
                OutlinedTextField(
                    value = landAreaAcres,
                    onValueChange = { 
                        viewModel.landAreaAcres.value = it 
                        viewModel.updateQuantityOrPrice()
                    },
                    label = { Text(if (isSiteVisit) "Visit Fee (₹) *" else "Unit Price (₹) *") },
                    placeholder = { Text("Enter price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = pillShape,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .boundedFormFieldRipple(shape = pillShape)
                        .elevated3dShadow(shape = pillShape, isDark = isDark)
                        .testTag("unit_price_input"),
                    colors = elevatedInputFieldColors(isDark = isDark)
                )
            }
        }

        // Automatic Calculation: Total Payment = (Quantity * Unit Price) + Grafting Charges (if entered)
        val qtyNum = quantity.toIntOrNull() ?: quantity.toDoubleOrNull()?.toInt() ?: 0
        val priceNum = landAreaAcres.toDoubleOrNull() ?: 0.0
        val graftingChargesNum = if (isImportedRootstocks && graftingCharges.isNotBlank()) (graftingCharges.toDoubleOrNull() ?: 0.0) else 0.0
        val totalPayment = (qtyNum * priceNum) + graftingChargesNum
        val paidAmountNum = amountPaid.toDoubleOrNull() ?: 0.0
        val remainingBalance = maxOf(0.0, totalPayment - paidAmountNum)

        // Section Title: PAYMENT STATUS
        Text(
            text = "PAYMENT STATUS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

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
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF4A4D58) else Color(0xFFD0D0D0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .boundedFormFieldRipple(shape = RoundedCornerShape(24.dp))
                        .testTag("payment_status_$statusOption")
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

        // Input Field: Amount Paid (₹)
        OutlinedTextField(
            value = amountPaid,
            onValueChange = { viewModel.updateAmountPaid(it) },
            label = { Text("Amount Paid (₹)") },
            placeholder = { Text("Enter amount paid") },
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
                .testTag("amount_paid_input"),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        // Calculated Payment Summary Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF22242B) else Color(0xFFF8F9FA),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF373A45) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalPayment.toLong())}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount Paid:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(paidAmountNum.toLong())}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32))
                }

                androidx.compose.material3.Divider(color = if (isDark) Color(0xFF373A45) else Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Balance:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("₹${java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).format(remainingBalance.toLong())}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Section Title: SCHEDULE & DATES
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
            // 1. Booking Date
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
                        onClick = { showDatePicker(bookingDate) { viewModel.bookingDate.value = it } },
                        modifier = Modifier.size(36.dp).testTag("booking_date_picker_button")
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
                    .testTag("booking_date_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            // 2. Expected Delivery
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
                        onClick = { showDatePicker(if (expectedDelivery.isBlank()) bookingDate else expectedDelivery) { viewModel.expectedDelivery.value = it } },
                        modifier = Modifier.size(36.dp).testTag("expected_delivery_picker_button")
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
                    .testTag("expected_delivery_input"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )
        }

        // Section Title: ATTACH UPI PAYMENT PROOF
        Text(
            text = "ATTACH UPI PAYMENT PROOF",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPI Payment Proof / Screenshot",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (paymentProofUri.isNotBlank()) "Attached: ${paymentProofName.ifBlank { "UPI_Proof.jpg" }}" else "No file attached (Images / PDF)",
                        fontSize = 11.sp,
                        color = if (paymentProofUri.isNotBlank()) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (paymentProofUri.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { requestStoragePermissionAndUpload() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("upload_proof_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (paymentProofUri.isNotBlank()) "Change" else "Upload Proof",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Notes & Special Observations
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = { Text("Notes / Inspection Remarks") },
            placeholder = { Text("Enter pruning history, soil treatment, disease status, or special requests...") },
            shape = textFieldShape,
            minLines = 2,
            maxLines = 4,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = textFieldShape)
                .elevated3dShadow(shape = textFieldShape, isDark = isDark),
            colors = elevatedInputFieldColors(isDark = isDark)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Section Title: MESSAGE PREVIEW
        Text(
            text = "MESSAGE PREVIEW",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Select Template Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .boundedFormFieldRipple(shape = pillShape) { templateMenuExpanded = true }
        ) {
            OutlinedTextField(
                value = selectedTemplate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Template") },
                shape = pillShape,
                trailingIcon = {
                    IconButton(onClick = { templateMenuExpanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .elevated3dShadow(shape = pillShape, isDark = isDark)
                    .testTag("select_template_dropdown"),
                colors = elevatedInputFieldColors(isDark = isDark)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { templateMenuExpanded = true }
            )

            DropdownMenu(
                expanded = templateMenuExpanded,
                onDismissRequest = { templateMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                templateOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (option == selectedTemplate) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == selectedTemplate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            selectedTemplate = option
                            templateMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Preview Box Display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 6.dp else 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = if (isDark) Color.Black else Color(0x20000000),
                    spotColor = if (isDark) Color.Black else Color(0x30000000)
                ),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF1C1D22) else Color(0xFFF8F9FA),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF333540) else Color(0xFFE2E8F0)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Template Preview ($selectedTemplate)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(generatedMessage))
                            Toast.makeText(context, "Preview text copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF121316) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2E313A) else Color(0xFFE0E0E0)
                    ),
                    shadowElevation = if (isDark) 4.dp else 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = if (isDark) Color.Black else Color(0x18000000),
                            spotColor = if (isDark) Color.Black else Color(0x25000000)
                        )
                ) {
                    Text(
                        text = generatedMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF111111),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        val generateReceipt = {
            val isRootstockForm = selectedService.equals("Rootstocks", ignoreCase = true) || selectedService.contains("Rootstock", ignoreCase = true)
            val actualRootstockVal = if (rootstock.isNotBlank()) rootstock else selectedRootstockSubTab
            val actualScionVal = if (scionVariety.isNotBlank()) scionVariety else plantVariety
            val actualDiamVal = if (rootDiameter.isNotBlank()) rootDiameter else "9 to 12 mm"

            val rData = ReceiptData(
                serialNumber = if (serialNumber.isBlank()) "N/A" else serialNumber,
                bookingDate = if (bookingDate.isBlank()) "N/A" else bookingDate,
                farmerName = if (farmerName.isBlank()) "N/A" else farmerName,
                contactNumber = if (contactNumber.isBlank()) "N/A" else contactNumber,
                address = if (farmerAddress.isBlank()) "N/A" else farmerAddress,
                orchardLocation = if (location.isBlank()) "N/A" else location,
                serviceCategory = if (isRootstockForm) "Imported Rootstocks" else selectedService,
                plantVariety = if (isRootstockForm) actualScionVal.ifBlank { "N/A" } else if (plantVariety.isBlank()) selectedService else plantVariety,
                quantity = if (quantity.isBlank()) "0" else quantity,
                totalAmount = totalPayment,
                amountPaid = paidAmountNum,
                remainingBalance = remainingBalance,
                paymentStatus = paymentStatus,
                expectedDelivery = if (expectedDelivery.isBlank()) "TBD" else expectedDelivery,
                rootstock = actualRootstockVal,
                rootDiameter = actualDiamVal,
                scionVariety = actualScionVal
            )
            val bmp = ReceiptGenerator.generateReceiptBitmap(rData, context)
            val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bmp, serialNumber)
            generatedReceiptBitmap = bmp
            generatedReceiptUri = uri
            showReceiptDialog = true
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Save Booking Entry
            Button(
                onClick = {
                    viewModel.saveRecord()
                    generateReceipt()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_booking_entry_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingId == null) "Save Booking Entry" else "Update Booking Entry",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2 & 3. Send Text via WhatsApp & Send Text via SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Send Text via WhatsApp
                Button(
                    onClick = {
                        if (contactNumber.isBlank()) {
                            Toast.makeText(context, "Please enter farmer's contact phone number first", Toast.LENGTH_SHORT).show()
                        } else {
                            showWaFormConfirmDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("send_text_via_whatsapp_button"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share via WhatsApp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Send Text via SMS
                Button(
                    onClick = {
                        if (contactNumber.isBlank()) {
                            Toast.makeText(context, "Please enter farmer's contact phone number first", Toast.LENGTH_SHORT).show()
                        } else {
                            showSmsFormConfirmDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("send_text_via_sms_button"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share via SMS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Send Digital Receipt
            OutlinedButton(
                onClick = {
                    generateReceipt()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_digital_receipt_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
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

            // Clear Form Button
            OutlinedButton(
                onClick = { viewModel.resetForm() },
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
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Multiple Phone Numbers Selection Dialog
    if (multiplePhoneNumbers != null) {
        AlertDialog(
            onDismissRequest = { multiplePhoneNumbers = null },
            title = {
                Column {
                    Text(
                        text = "Select Phone Number",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!selectedContactNameForDialog.isNullOrBlank()) {
                        Text(
                            text = selectedContactNameForDialog!!,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    multiplePhoneNumbers!!.forEach { (number, typeLabel) ->
                        Surface(
                            onClick = {
                                val formatted = formatPhoneNumber(number)
                                viewModel.contactNumber.value = formatted
                                multiplePhoneNumbers = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF22242B) else Color(0xFFF5F5F5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF373A45) else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = number,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF222222)
                                    )
                                    Text(
                                        text = typeLabel,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { multiplePhoneNumbers = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Receipt Image Dialog
    if (showReceiptDialog && generatedReceiptBitmap != null) {
        Dialog(onDismissRequest = { showReceiptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generated Digital Receipt",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { showReceiptDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {
                        Image(
                            bitmap = generatedReceiptBitmap!!.asImageBitmap(),
                            contentDescription = "Official Receipt Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Share buttons
                    Button(
                        onClick = {
                            if (generatedReceiptUri != null) {
                                showShareFormReceiptConfirmDialog = true
                            } else {
                                Toast.makeText(context, "Failed to load receipt file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_receipt_image_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Receipt Image", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (generatedReceiptUri != null) {
                                if (contactNumber.isBlank()) {
                                    Toast.makeText(context, "Please enter farmer's contact phone number first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showWaFormReceiptConfirmDialog = true
                                }
                            } else {
                                Toast.makeText(context, "Failed to load receipt image file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("share_receipt_whatsapp_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    TextButton(onClick = { showReceiptDialog = false }) {
                        Text("Done / Close", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Confirmation Modal 1: WhatsApp Notification Dialog
    if (showWaFormConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWaFormConfirmDialog = false },
            title = { Text("Send WhatsApp Notification", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to send this booking notification message to ${farmerName.ifBlank { "the farmer" }} (${contactNumber}) on WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWaFormConfirmDialog = false
                        var cleanDigits = contactNumber.replace("[^0-9]".toRegex(), "")
                        if (cleanDigits.startsWith("91") && cleanDigits.length > 10) {
                            cleanDigits = cleanDigits.takeLast(10)
                        } else if (cleanDigits.startsWith("0") && cleanDigits.length == 11) {
                            cleanDigits = cleanDigits.substring(1)
                        }
                        if (cleanDigits.length > 10) {
                            cleanDigits = cleanDigits.takeLast(10)
                        }
                        val formattedPhone = if (cleanDigits.isNotEmpty()) "91$cleanDigits" else ""
                        val encodedMsg = Uri.encode(generatedMessage)

                        if (formattedPhone.isNotEmpty()) {
                            val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg")
                            val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(waIntent)
                            } catch (e: Exception) {
                                try {
                                    val waBusinessIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                        setPackage("com.whatsapp.w4b")
                                    }
                                    context.startActivity(waBusinessIntent)
                                } catch (ex: Exception) {
                                    try {
                                        val directWaUri = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMsg")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, directWaUri))
                                    } catch (exc: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Invalid phone number format", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaFormConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Modal 2: SMS Notification Dialog
    if (showSmsFormConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSmsFormConfirmDialog = false },
            title = { Text("Send SMS Notification", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to send this booking notification message to ${farmerName.ifBlank { "the farmer" }} (${contactNumber}) via SMS?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSmsFormConfirmDialog = false
                        var smsPhone = contactNumber.replace("[^0-9+]".toRegex(), "")
                        if (!smsPhone.startsWith("+") && !smsPhone.startsWith("91")) {
                            val digitsOnly = smsPhone.replace("[^0-9]".toRegex(), "")
                            if (digitsOnly.length == 10) {
                                smsPhone = "+91$digitsOnly"
                            }
                        } else if (!smsPhone.startsWith("+")) {
                            smsPhone = "+$smsPhone"
                        }

                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$smsPhone")
                            putExtra("sms_body", generatedMessage)
                            putExtra("address", smsPhone)
                        }
                        try {
                            context.startActivity(smsIntent)
                        } catch (e: Exception) {
                            try {
                                val fallbackSms = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("sms:$smsPhone")
                                    putExtra("sms_body", generatedMessage)
                                }
                                context.startActivity(fallbackSms)
                            } catch (ex: Exception) {
                                Toast.makeText(context, "SMS app not available on device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Send", color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsFormConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Modal 3: Share Receipt Image Dialog
    if (showShareFormReceiptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showShareFormReceiptConfirmDialog = false },
            title = { Text("Share Digital Receipt", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to share the digital receipt image to ${farmerName.ifBlank { "the farmer" }} (${contactNumber.ifBlank { "N/A" }}) on WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareFormReceiptConfirmDialog = false
                        if (generatedReceiptUri != null) {
                            var cleanDigits = contactNumber.replace("[^0-9]".toRegex(), "")
                            if (cleanDigits.startsWith("91") && cleanDigits.length > 10) {
                                cleanDigits = cleanDigits.takeLast(10)
                            } else if (cleanDigits.startsWith("0") && cleanDigits.length == 11) {
                                cleanDigits = cleanDigits.substring(1)
                            }
                            if (cleanDigits.length > 10) {
                                cleanDigits = cleanDigits.takeLast(10)
                            }
                            val formattedPhone = if (cleanDigits.isNotEmpty()) "91$cleanDigits" else ""

                            if (formattedPhone.isNotEmpty()) {
                                val waIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, generatedReceiptUri)
                                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                    putExtra(Intent.EXTRA_TEXT, "Dear ${if (farmerName.isBlank()) "Farmer" else farmerName}, here is your official digital receipt from Baagbaan Boi${if (serialNumber.isNotBlank()) " (Serial #$serialNumber)" else ""}.")
                                    setPackage("com.whatsapp")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(waIntent)
                                } catch (_: Exception) {
                                    try {
                                        val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, generatedReceiptUri)
                                            putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                            putExtra(Intent.EXTRA_TEXT, "Dear ${if (farmerName.isBlank()) "Farmer" else farmerName}, here is your official digital receipt from Baagbaan Boi${if (serialNumber.isNotBlank()) " (Serial #$serialNumber)" else ""}.")
                                            setPackage("com.whatsapp.w4b")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(waBusinessIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter a valid contact phone number for the farmer", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to load receipt file", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Share", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareFormReceiptConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Modal 4: Send Receipt via WhatsApp Dialog
    if (showWaFormReceiptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWaFormReceiptConfirmDialog = false },
            title = { Text("Send Receipt via WhatsApp", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to send the digital receipt image to ${farmerName.ifBlank { "the farmer" }} (${contactNumber}) on WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWaFormReceiptConfirmDialog = false
                        if (generatedReceiptUri != null) {
                            var cleanDigits = contactNumber.replace("[^0-9]".toRegex(), "")
                            if (cleanDigits.startsWith("91") && cleanDigits.length > 10) {
                                cleanDigits = cleanDigits.takeLast(10)
                            } else if (cleanDigits.startsWith("0") && cleanDigits.length == 11) {
                                cleanDigits = cleanDigits.substring(1)
                            }
                            if (cleanDigits.length > 10) {
                                cleanDigits = cleanDigits.takeLast(10)
                            }
                            val formattedPhone = if (cleanDigits.isNotEmpty()) "91$cleanDigits" else ""

                            if (formattedPhone.isNotEmpty()) {
                                val waIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, generatedReceiptUri)
                                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                    putExtra(Intent.EXTRA_TEXT, "Dear ${if (farmerName.isBlank()) "Farmer" else farmerName}, here is your official digital receipt from Baagbaan Boi${if (serialNumber.isNotBlank()) " (Serial #$serialNumber)" else ""}.")
                                    setPackage("com.whatsapp")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(waIntent)
                                } catch (_: Exception) {
                                    try {
                                        val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, generatedReceiptUri)
                                            putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                            putExtra(Intent.EXTRA_TEXT, "Dear ${if (farmerName.isBlank()) "Farmer" else farmerName}, here is your official digital receipt from Baagbaan Boi${if (serialNumber.isNotBlank()) " (Serial #$serialNumber)" else ""}.")
                                            setPackage("com.whatsapp.w4b")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(waBusinessIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter a valid contact phone number for the farmer", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to load receipt image file", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaFormReceiptConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManualDateDialog) {
        var manualDateTFV by remember(manualDateText) {
            mutableStateOf(TextFieldValue(text = manualDateText, selection = TextRange(manualDateText.length)))
        }
        AlertDialog(
            onDismissRequest = { showManualDateDialog = false },
            title = { Text(text = "Enter $manualDateLabel", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type the date directly (e.g. DD/MM/YYYY):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = manualDateTFV,
                        onValueChange = { newVal ->
                            val formatted = formatAutoSlashDate(manualDateTFV.text, newVal.text)
                            val newPos = if (formatted.length > manualDateTFV.text.length && formatted.endsWith("/")) {
                                formatted.length
                            } else if (newVal.selection.end <= formatted.length) {
                                newVal.selection.end
                            } else {
                                formatted.length
                            }
                            manualDateTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                            manualDateText = formatted
                        },
                        label = { Text(manualDateLabel) },
                        placeholder = { Text("DD/MM/YYYY") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("manual_date_entry_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onManualDateSaved(manualDateText.trim())
                        showManualDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("manual_date_save_button")
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualDateDialog = false },
                    modifier = Modifier.testTag("manual_date_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePickerDialog) {
        AppDatePickerDialog(
            initialDateStr = datePickerInitialStr,
            onDateSelected = { selectedDate ->
                datePickerOnSelected(selectedDate)
                showDatePickerDialog = false
            },
            onDismissRequest = { showDatePickerDialog = false }
        )
    }
}

@Composable
fun AppDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }

    val (initYear, initMonth, initDay) = remember(initialDateStr) {
        if (initialDateStr.contains("-")) {
            val parts = initialDateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                val m = (parts[1].toIntOrNull()?.minus(1)) ?: calendar.get(Calendar.MONTH)
                val d = parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                Triple(y, m, d)
            } else Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        } else if (initialDateStr.contains("/")) {
            val parts = initialDateStr.split("/")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                val m = (parts[1].toIntOrNull()?.minus(1)) ?: calendar.get(Calendar.MONTH)
                val y = parts[2].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                Triple(y, m, d)
            } else Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        } else {
            Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
    }

    var selectedYear by remember { mutableStateOf(initYear) }
    var selectedMonth by remember { mutableStateOf(initMonth) } // 0-indexed
    var selectedDay by remember { mutableStateOf(initDay) }
    var viewMonthCalendar by remember { mutableStateOf(Calendar.getInstance().apply { set(initYear, initMonth, 1) }) }

    var isManualMode by remember { mutableStateOf(false) }
    var manualInputTFV by remember {
        val initialStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", initDay, initMonth + 1, initYear)
        mutableStateOf(TextFieldValue(text = initialStr, selection = TextRange(initialStr.length)))
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Banner with Selected Date & Pencil Icon
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "SELECT DATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayHeader = if (isManualMode) {
                                if (manualInputTFV.text.isNotBlank()) manualInputTFV.text else "DD/MM/YYYY"
                            } else {
                                val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
                                SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
                            }

                            Text(
                                text = displayHeader,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            // Pencil Icon right next to the selected date header
                            IconButton(
                                onClick = {
                                    if (!isManualMode) {
                                        val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                                        manualInputTFV = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                    } else {
                                        val parts = manualInputTFV.text.split("/")
                                        if (parts.size == 3) {
                                            val d = parts[0].toIntOrNull()
                                            val m = parts[1].toIntOrNull()
                                            val y = parts[2].toIntOrNull()
                                            if (d != null && m != null && m in 1..12 && y != null && y > 1900) {
                                                selectedDay = d
                                                selectedMonth = m - 1
                                                selectedYear = y
                                                viewMonthCalendar = Calendar.getInstance().apply { set(y, m - 1, 1) }
                                            }
                                        }
                                    }
                                    isManualMode = !isManualMode
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dialog_date_header_pencil")
                            ) {
                                Icon(
                                    imageVector = if (isManualMode) Icons.Default.DateRange else Icons.Default.Edit,
                                    contentDescription = if (isManualMode) "Switch to Calendar View" else "Edit Date Manually",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Body Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isManualMode) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Manual Date Entry (DD/MM/YYYY)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Slashes (/) are automatically added as you type.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = manualInputTFV,
                                onValueChange = { newVal ->
                                    val formatted = formatAutoSlashDate(manualInputTFV.text, newVal.text)
                                    val newPos = if (formatted.length > manualInputTFV.text.length && formatted.endsWith("/")) {
                                        formatted.length
                                    } else if (newVal.selection.end <= formatted.length) {
                                        newVal.selection.end
                                    } else {
                                        formatted.length
                                    }
                                    manualInputTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                                },
                                label = { Text("Date (DD/MM/YYYY)") },
                                placeholder = { Text("e.g. 03/08/2026") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_manual_date_input")
                            )
                        }
                    } else {
                        // Month Navigation
                        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newCal = viewMonthCalendar.clone() as Calendar
                                    newCal.add(Calendar.MONTH, -1)
                                    viewMonthCalendar = newCal
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = monthYearFormat.format(viewMonthCalendar.time),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    val newCal = viewMonthCalendar.clone() as Calendar
                                    newCal.add(Calendar.MONTH, 1)
                                    viewMonthCalendar = newCal
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        // Days of Week Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Grid of days
                        val currentCal = viewMonthCalendar.clone() as Calendar
                        currentCal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
                        val daysInMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val viewYear = currentCal.get(Calendar.YEAR)
                        val viewMonth = currentCal.get(Calendar.MONTH)

                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            var dayCounter = 1
                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (c in 0 until 7) {
                                        val cellIndex = r * 7 + c
                                        if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                                            Spacer(modifier = Modifier.size(36.dp))
                                        } else {
                                            val thisDay = dayCounter
                                            val isSelected = (selectedYear == viewYear && selectedMonth == viewMonth && selectedDay == thisDay)

                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .boundedFormFieldRipple(
                                                        shape = CircleShape,
                                                        rippleColor = if (isSelected) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                    ) {
                                                        selectedYear = viewYear
                                                        selectedMonth = viewMonth
                                                        selectedDay = thisDay
                                                    }
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$thisDay",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            dayCounter++
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag("date_dialog_cancel_button")
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val result = if (isManualMode) {
                                    val parts = manualInputTFV.text.split("/")
                                    if (parts.size == 3) {
                                        val d = parts[0].toIntOrNull()
                                        val m = parts[1].toIntOrNull()
                                        val y = parts[2].toIntOrNull()
                                        if (d != null && m != null && y != null) {
                                            String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                                        } else manualInputTFV.text
                                    } else manualInputTFV.text
                                } else {
                                    String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                                }
                                onDateSelected(result)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("date_dialog_confirm_button")
                        ) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatAutoSlashDate(oldVal: String, newVal: String): String {
    val isDeleting = newVal.length < oldVal.length
    val oldDigits = oldVal.filter { it.isDigit() }
    val newDigitsRaw = newVal.filter { it.isDigit() }

    if (isDeleting) {
        var digits = newDigitsRaw
        if (oldVal.endsWith("/") && !newVal.endsWith("/") && oldDigits == newDigitsRaw && oldDigits.isNotEmpty()) {
            digits = oldDigits.dropLast(1)
        }
        return buildDDMMYYYYFormattedFromDigits(digits)
    }

    var userTypedSlash = newVal.endsWith("/") && !oldVal.endsWith("/")
    val digits = newDigitsRaw.take(8)

    // Day Part
    var dayStr = ""
    var restDigits = ""

    if (digits.isNotEmpty()) {
        if (digits.length == 1) {
            val firstChar = digits[0]
            if (userTypedSlash) {
                dayStr = "0$firstChar"
                userTypedSlash = false
            } else if (firstChar in '4'..'9') {
                dayStr = "0$firstChar"
            } else {
                dayStr = digits
            }
        } else {
            val d = digits.substring(0, 2).toIntOrNull() ?: 0
            val validDay = when {
                d == 0 -> "01"
                d > 31 -> "31"
                else -> String.format(Locale.getDefault(), "%02d", d)
            }
            dayStr = validDay
            restDigits = digits.substring(2)
        }
    }

    // Month Part
    var monthStr = ""
    var yearStr = ""

    if (restDigits.isNotEmpty()) {
        if (restDigits.length == 1) {
            val firstMonthChar = restDigits[0]
            if (userTypedSlash) {
                monthStr = "0$firstMonthChar"
            } else if (firstMonthChar in '2'..'9') {
                monthStr = "0$firstMonthChar"
            } else {
                monthStr = restDigits
            }
        } else {
            val m = restDigits.substring(0, 2).toIntOrNull() ?: 0
            val validMonth = when {
                m == 0 -> "01"
                m > 12 -> "12"
                else -> String.format(Locale.getDefault(), "%02d", m)
            }
            monthStr = validMonth
            yearStr = restDigits.substring(2)
        }
    }

    return buildDDMMYYYYFormatted(dayStr, monthStr, yearStr)
}

fun buildDDMMYYYYFormattedFromDigits(digits: String): String {
    var day = ""
    var month = ""
    var year = ""
    if (digits.isNotEmpty()) {
        if (digits.length == 1) {
            day = digits
        } else {
            day = digits.substring(0, 2)
            if (digits.length > 2) {
                val rest = digits.substring(2)
                if (rest.length == 1) {
                    month = rest
                } else {
                    month = rest.substring(0, 2)
                    if (rest.length > 2) {
                        year = rest.substring(2)
                    }
                }
            }
        }
    }
    return buildDDMMYYYYFormatted(day, month, year)
}

fun buildDDMMYYYYFormatted(day: String, month: String, year: String): String {
    val sb = StringBuilder()
    sb.append(day)
    if (day.length == 2) {
        sb.append('/')
        if (month.isNotEmpty()) {
            sb.append(month)
            if (month.length == 2) {
                sb.append('/')
                if (year.isNotEmpty()) {
                    sb.append(year)
                }
            }
        }
    }
    return sb.toString()
}
