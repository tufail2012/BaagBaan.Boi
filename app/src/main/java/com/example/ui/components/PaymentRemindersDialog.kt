@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import java.text.NumberFormat
import java.util.Locale

data class PendingPaymentItem(
    val id: Long,
    val serialNumber: String,
    val farmerName: String,
    val contactNumber: String,
    val amountDue: Double,
    val totalCost: Double,
    val amountPaid: Double,
    val paymentStatus: String,
    val serviceType: String,
    val source: String // "CROP" or "GARDEN"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRemindersDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val isDark = isSystemInDarkTheme()

    var searchQuery by remember { mutableStateOf("") }

    var loadErrorTrace by remember { mutableStateOf<String?>(null) }

    // Load Crop Records and Garden Planning Entries
    val cropRecords by db.cropRecordDao().getAllRecords().collectAsState(initial = emptyList())
    val gardenEntries by db.gardenPlanningDao().getAllEntries().collectAsState(initial = emptyList())

    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Filter all pending payment items
    val pendingItems = remember(cropRecords, gardenEntries, searchQuery) {
        try {
            val list = mutableListOf<PendingPaymentItem>()

            // 1. Process Crop Records
            cropRecords.forEach { crop ->
                val remDue = crop.calculateRemainingBalance()
                val isPending = !crop.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
                if (isPending) {
                    list.add(
                        PendingPaymentItem(
                            id = crop.id,
                            serialNumber = crop.serialNumber,
                            farmerName = crop.farmerName,
                            contactNumber = crop.contactNumber,
                            amountDue = remDue,
                            totalCost = crop.calculateTotalAmount(),
                            amountPaid = crop.amountPaid,
                            paymentStatus = crop.paymentStatus,
                            serviceType = if (crop.serviceType.isNotBlank()) crop.serviceType else "Crop Order",
                            source = "CROP"
                        )
                    )
                }
            }

            // 2. Process Garden Planning Entries
            gardenEntries.forEach { garden ->
                val remDue = (garden.totalCost - garden.amountPaid).coerceAtLeast(0.0)
                val isPending = !garden.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
                if (isPending) {
                    list.add(
                        PendingPaymentItem(
                            id = garden.id,
                            serialNumber = garden.serialNumber,
                            farmerName = garden.farmerName,
                            contactNumber = garden.contactNumber,
                            amountDue = remDue,
                            totalCost = garden.totalCost,
                            amountPaid = garden.amountPaid,
                            paymentStatus = garden.paymentStatus,
                            serviceType = "Garden Planning",
                            source = "GARDEN"
                        )
                    )
                }
            }

            // Filter by search query if non-blank
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list.filter { item ->
                    item.farmerName.lowercase().contains(q) ||
                            item.serialNumber.lowercase().contains(q) ||
                            item.contactNumber.contains(q) ||
                            item.serviceType.lowercase().contains(q)
                }
            } else {
                list
            }
        } catch (e: Exception) {
            loadErrorTrace = "Error loading payment reminders: ${e.message}\n${e.stackTraceToString()}"
            emptyList()
        }
    }

    val totalOutstanding = remember(pendingItems) {
        pendingItems.sumOf { it.amountDue }
    }

    fun openWhatsAppReminder(item: PendingPaymentItem) {
        val phone = item.contactNumber.trim()
        var cleanDigits = phone.replace(Regex("[^0-9]"), "")
        if (cleanDigits.startsWith("91") && cleanDigits.length > 10) {
            cleanDigits = cleanDigits.takeLast(10)
        } else if (cleanDigits.startsWith("0") && cleanDigits.length == 11) {
            cleanDigits = cleanDigits.substring(1)
        }
        if (cleanDigits.length > 10) {
            cleanDigits = cleanDigits.takeLast(10)
        }
        val formattedPhone = if (cleanDigits.isNotEmpty()) "91$cleanDigits" else ""

        if (formattedPhone.isEmpty()) {
            Toast.makeText(context, "No valid phone number for ${item.farmerName}", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedDue = numberFormat.format(item.amountDue)
        val farmerDisplayName = if (item.farmerName.isBlank()) "Valued Customer" else item.farmerName
        val message = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "quick_payment_reminder",
            data = mapOf(
                "farmerName" to farmerDisplayName,
                "serialNumber" to item.serialNumber,
                "serviceType" to item.serviceType,
                "amountDue" to formattedDue
            )
        )

        val encodedMsg = Uri.encode(message)
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
                                    text = "Payment Reminders",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "${pendingItems.size} Pending • ${numberFormat.format(totalOutstanding)} Dues",
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
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Outstanding Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF331E1E) else Color(0xFFFFF0F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Outstanding Dues",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = numberFormat.format(totalOutstanding),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("payment_reminders_search_input"),
                        placeholder = { Text("Search by farmer name, serial or phone...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pendingItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No pending payments found" else "No matching pending records",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingItems, key = { "${it.source}_${it.id}" }) { item ->
                                PendingPaymentRow(
                                    item = item,
                                    numberFormat = numberFormat,
                                    isDark = isDark,
                                    onSendWhatsApp = { openWhatsAppReminder(item) },
                                    onCall = { makePhoneCall(item.contactNumber) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingPaymentRow(
    item: PendingPaymentItem,
    numberFormat: NumberFormat,
    isDark: Boolean,
    onSendWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_payment_row_${item.serialNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Header (Farmer Name + Service Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.farmerName.ifBlank { "Unknown Farmer" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Serial #${item.serialNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Service tag chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (item.source == "GARDEN") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.serviceType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.source == "GARDEN") MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Outstanding Amount & Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount Due",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = numberFormat.format(item.amountDue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }

                if (item.contactNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCall() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.contactNumber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.contactNumber.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "No number",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("send_whatsapp_reminder_${item.serialNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send via WhatsApp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
