@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.components

import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.CropRecord
import com.example.data.FarmerContact
import com.example.ui.theme.AgriGreenPrimary
import com.example.ui.theme.AgriGreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactDisplayItem(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String,
    val category: String,
    val isFromCropRecords: Boolean = false,
    val associatedService: String = "",
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentStatus: String = "Pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDirectoryDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // WhatsApp Dialog State
    var selectedWhatsAppContact by remember { mutableStateOf<ContactDisplayItem?>(null) }
    // Expanded Contact Detail State
    var selectedContactForDetails by remember { mutableStateOf<ContactDisplayItem?>(null) }
    // Delete Contact State
    var contactToDelete by remember { mutableStateOf<ContactDisplayItem?>(null) }

    // Load custom contacts from DB
    val dbContacts by db.farmerContactDao().getAllContacts().collectAsState(initial = emptyList())

    // Load Crop Records to aggregate farmer contacts automatically
    val cropRecords by db.cropRecordDao().getAllRecords().collectAsState(initial = emptyList())

    var deletedContactKeys by remember { mutableStateOf(setOf<String>()) }

    // Auto-sync crop booking farmer contacts to DB contacts table
    LaunchedEffect(cropRecords, deletedContactKeys) {
        if (cropRecords.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                cropRecords.forEach { record ->
                    val cleanPhone = record.contactNumber.replace(Regex("[^0-9]"), "")
                    val key = if (cleanPhone.isNotEmpty()) cleanPhone else record.farmerName.trim().lowercase()
                    if ((record.farmerName.isNotBlank() || record.contactNumber.isNotBlank()) && !deletedContactKeys.contains(key)) {
                        val existing = db.farmerContactDao().getContactByPhoneOrName(record.contactNumber, record.farmerName)
                        if (existing == null) {
                            db.farmerContactDao().insertContact(
                                FarmerContact(
                                    name = record.farmerName.ifBlank { "Farmer" },
                                    phone = record.contactNumber,
                                    address = record.farmerAddress,
                                    category = "Farmer"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Aggregated list
    val allContactsList = remember(dbContacts, cropRecords, searchQuery, deletedContactKeys) {
        val list = mutableListOf<ContactDisplayItem>()
        val phoneSet = mutableSetOf<String>()

        // 1. First add saved contacts from DB
        dbContacts.forEach { fc ->
            val cleanPhone = fc.phone.replace(Regex("[^0-9]"), "")
            val key = if (cleanPhone.isNotEmpty()) cleanPhone else fc.name.trim().lowercase()
            if (!deletedContactKeys.contains(key)) {
                list.add(
                    ContactDisplayItem(
                        id = fc.id,
                        name = fc.name,
                        phone = fc.phone,
                        address = fc.address,
                        category = fc.category
                    )
                )
                if (cleanPhone.isNotEmpty()) phoneSet.add(cleanPhone)
            }
        }

        // 2. Add contacts from Crop / Booking Records if not already present
        cropRecords.forEach { record ->
            val cleanPhone = record.contactNumber.replace(Regex("[^0-9]"), "")
            val key = if (cleanPhone.isNotEmpty()) cleanPhone else record.farmerName.trim().lowercase()
            if (!deletedContactKeys.contains(key) && cleanPhone.isNotEmpty() && !phoneSet.contains(cleanPhone)) {
                phoneSet.add(cleanPhone)
                list.add(
                    ContactDisplayItem(
                        id = record.id,
                        name = record.farmerName,
                        phone = record.contactNumber,
                        address = record.farmerAddress,
                        category = "Farmer (${record.serviceType})",
                        isFromCropRecords = true,
                        associatedService = record.serviceType,
                        totalAmount = record.quantity * record.landAreaAcres,
                        amountPaid = record.amountPaid,
                        paymentStatus = record.paymentStatus
                    )
                )
            }
        }

        // Filter search
        if (searchQuery.trim().isEmpty()) {
            list
        } else {
            val q = searchQuery.trim().lowercase()
            list.filter {
                it.name.lowercase().contains(q) ||
                        it.phone.contains(q) ||
                        it.address.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }
    }

    fun saveToSystemPhoneContacts(name: String, phone: String, address: String) {
        try {
            val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                putExtra(ContactsContract.Intents.Insert.NAME, name)
                putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                putExtra(ContactsContract.Intents.Insert.POSTAL, address)
                putExtra(ContactsContract.Intents.Insert.NOTES, "AgriCrop Farmer Contact")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open contacts app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot make phone call", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Contact Directory",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "${allContactsList.size} Saved Contacts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 36.dp, end = 16.dp)
                            .size(56.dp)
                            .testTag("add_contact_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        placeholder = { Text("Search farmer name, phone, or address...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (allContactsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching contacts found" else "No contacts added yet",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap '+' to create a new entry manually.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        val contactListState = rememberLazyListState()
                        contactListState.rememberScrollHapticFeedback()

                        LazyColumn(
                            state = contactListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(allContactsList, key = { "${it.id}_${it.phone}_${it.name}" }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedContactForDetails = item },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar badge
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                                    HighlightedText(
                                                        text = item.name,
                                                        query = searchQuery,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    // WhatsApp Icon directly next to Farmer's Name!
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(WhatsAppGreen)
                                                            .clickable {
                                                                selectedWhatsAppContact = item
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Chat,
                                                            contentDescription = "WhatsApp Templates",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    HighlightedText(
                                                        text = item.phone.ifEmpty { "No phone number" },
                                                        query = searchQuery,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (item.address.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        HighlightedText(
                                                            text = item.address,
                                                            query = searchQuery,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Action buttons: Call, Save, Delete
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { makePhoneCall(item.phone) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Call",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    saveToSystemPhoneContacts(item.name, item.phone, item.address)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = "Save to device contacts",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    contactToDelete = item
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Contact",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modal to Enter Contact Details Manually
    if (showAddDialog) {
        ManualAddContactModal(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, address, category, autoSavePhone ->
                scope.launch(Dispatchers.IO) {
                    val newContact = FarmerContact(
                        name = name,
                        phone = phone,
                        address = address,
                        category = category
                    )
                    db.farmerContactDao().insertContact(newContact)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Contact '$name' saved to directory!", Toast.LENGTH_SHORT).show()
                        if (autoSavePhone) {
                            saveToSystemPhoneContacts(name, phone, address)
                        }
                        showAddDialog = false
                    }
                }
            }
        )
    }

    // Modal for WhatsApp Templates
    selectedWhatsAppContact?.let { contact ->
        WhatsAppTemplateDialog(
            farmerName = contact.name,
            contactNumber = contact.phone,
            serviceType = contact.associatedService.ifEmpty { "Agri Booking" },
            amountPaid = contact.amountPaid,
            totalAmount = contact.totalAmount,
            remainingBalance = maxOf(0.0, contact.totalAmount - contact.amountPaid),
            paymentStatus = contact.paymentStatus,
            onDismiss = { selectedWhatsAppContact = null }
        )
    }

    // Expanded Contact Details Dialog on Tap
    selectedContactForDetails?.let { contactItem ->
        ContactDetailsDialog(
            contact = contactItem,
            cropRecords = cropRecords,
            onDismiss = { selectedContactForDetails = null },
            onMakeCall = { makePhoneCall(contactItem.phone) },
            onOpenWhatsApp = {
                selectedWhatsAppContact = contactItem
                selectedContactForDetails = null
            },
            onSaveToPhone = { saveToSystemPhoneContacts(contactItem.name, contactItem.phone, contactItem.address) },
            onDeleteContact = {
                contactToDelete = contactItem
                selectedContactForDetails = null
            }
        )
    }

    // Delete Confirmation Dialog
    contactToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete Contact", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(text = "Are you sure you want to remove '${target.name}' from your contacts directory?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPhone = target.phone.replace(Regex("[^0-9]"), "")
                        val key = if (cleanPhone.isNotEmpty()) cleanPhone else target.name.trim().lowercase()
                        deletedContactKeys = deletedContactKeys + key

                        scope.launch(Dispatchers.IO) {
                            if (target.id != 0L && !target.isFromCropRecords) {
                                db.farmerContactDao().deleteContactById(target.id)
                                com.example.data.FirestoreSyncManager().deleteFarmerContact(target.id)
                            }
                            db.farmerContactDao().deleteContactByPhoneOrName(target.phone, target.name)

                            val binItem = com.example.data.RecycleBinEntity(
                                itemType = "CONTACT",
                                title = "Contact: ${target.name}",
                                subtitle = "Phone: ${target.phone} • ${target.category}",
                                jsonPayload = com.example.data.RecycleBinConverter.contactToJson(
                                    com.example.data.FarmerContact(
                                        id = target.id,
                                        name = target.name,
                                        phone = target.phone,
                                        address = target.address,
                                        category = target.category
                                    )
                                ),
                                deletedAt = System.currentTimeMillis()
                            )
                            val insertedId = db.recycleBinDao().insert(binItem)
                            com.example.data.FirestoreSyncManager().saveRecycleBinItem(binItem.copy(id = insertedId))

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Contact '${target.name}' deleted", Toast.LENGTH_SHORT).show()
                                contactToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text(text = "Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsDialog(
    contact: ContactDisplayItem,
    cropRecords: List<CropRecord>,
    onDismiss: () -> Unit,
    onMakeCall: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onSaveToPhone: () -> Unit,
    onDeleteContact: () -> Unit
) {
    // Filter related crop records for this farmer
    val relatedRecords = remember(contact, cropRecords) {
        val cleanContactPhone = contact.phone.replace(Regex("[^0-9]"), "")
        val contactNameLower = contact.name.trim().lowercase()
        cropRecords.filter { record ->
            val cleanRecordPhone = record.contactNumber.replace(Regex("[^0-9]"), "")
            (cleanContactPhone.isNotEmpty() && cleanRecordPhone == cleanContactPhone) ||
                    (contactNameLower.isNotEmpty() && record.farmerName.trim().lowercase() == contactNameLower)
        }
    }

    val totalBookings = relatedRecords.size
    val totalAmount = relatedRecords.sumOf { record ->
        val amt = record.quantity * record.landAreaAcres
        if (amt > 0) amt else record.amountPaid
    }
    val totalPaid = relatedRecords.sumOf { it.amountPaid }
    val balanceDue = maxOf(0.0, totalAmount - totalPaid)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = "Contact Profile & Bookings", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(onClick = onDeleteContact) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Contact",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Header Profile Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AgriGreenPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = contact.category.ifEmpty { "Farmer" },
                                        color = AgriGreenPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Contact Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = contact.phone.ifEmpty { "No phone number" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (contact.address.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = contact.address,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Call
                                    IconButton(
                                        onClick = onMakeCall,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    // WhatsApp
                                    IconButton(
                                        onClick = onOpenWhatsApp,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(WhatsAppGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "WhatsApp",
                                            tint = Color.White
                                        )
                                    }

                                    // Save Phone
                                    IconButton(
                                        onClick = onSaveToPhone,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Save to Phone",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Booking Summary Stats
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Booking Overview",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Bookings", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("$totalBookings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }

                                    Column {
                                        Text("Amount Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("₹${String.format("%.0f", totalPaid)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                                    }

                                    Column {
                                        Text("Balance Due", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = if (balanceDue > 0) "₹${String.format("%.0f", balanceDue)}" else "Cleared",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (balanceDue > 0) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Related Bookings Title
                    item {
                        Text(
                            text = "Related Bookings & History (${relatedRecords.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (relatedRecords.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No booking records found",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Bookings saved for ${contact.name} will automatically appear here.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(relatedRecords, key = { "detail_booking_${it.id}" }) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = record.serialNumber.ifEmpty { "SL-#${record.id}" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (record.paymentStatus == "Cleared") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ) {
                                            Text(
                                                text = record.paymentStatus,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (record.paymentStatus == "Cleared") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${record.serviceType} • ${record.plantVariety}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Qty: ${record.quantity.toInt()} | Area: ${record.landAreaAcres} Acres",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "Paid: ₹${String.format("%.0f", record.amountPaid)}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (record.bookingDate.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Booking Date: ${record.bookingDate}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAddContactModal(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, category: String, autoSavePhone: Boolean) -> Unit
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
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Farmer") }
    var autoSavePhoneContacts by remember { mutableStateOf(true) }
    var isSavingContact by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New Contact Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Farmer / Contact Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
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
                    label = { Text("Phone Number *") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Village / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Farmer, Supplier, Buyer)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoSavePhoneContacts = !autoSavePhoneContacts }
                        .padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = autoSavePhoneContacts,
                        onCheckedChange = { autoSavePhoneContacts = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Automatically add to device Phone Contacts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            val phoneDigits = phone.removePrefix("+91").filter { it.isDigit() }
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && phoneDigits.isNotEmpty() && !isSavingContact) {
                        isSavingContact = true
                        coroutineScope.launch {
                            try {
                                onSave(name.trim(), phone.trim(), address.trim(), category.trim(), autoSavePhoneContacts)
                            } catch (t: Throwable) {
                                android.util.Log.e("ManualAddContact", "Error saving contact", t)
                            } finally {
                                isSavingContact = false
                            }
                        }
                    }
                },
                enabled = !isSavingContact && name.trim().isNotEmpty() && phoneDigits.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSavingContact) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Save Contact", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
