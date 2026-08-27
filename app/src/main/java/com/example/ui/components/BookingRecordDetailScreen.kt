package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import com.example.util.MapHelper
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CropRecord
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import com.example.util.ReceiptData
import com.example.util.ReceiptGenerator
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PaymentInstallment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val date: String,
    val modeNote: String = "Cash",
    val typeLabel: String = "Payment Addition"
)

fun parsePaymentHistory(json: String, currentPaid: Double, bookingDate: String): List<PaymentInstallment> {
    if (json.isBlank()) {
        if (currentPaid > 0) {
            return listOf(
                PaymentInstallment(
                    id = "init_1",
                    amount = currentPaid,
                    date = bookingDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                    modeNote = "Cash",
                    typeLabel = "Initial Advance Payment"
                )
            )
        }
        return emptyList()
    }
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<PaymentInstallment>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                PaymentInstallment(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    amount = obj.optDouble("amount", 0.0),
                    date = obj.optString("date", ""),
                    modeNote = obj.optString("modeNote", "Cash"),
                    typeLabel = obj.optString("typeLabel", "Payment Addition")
                )
            )
        }
        list
    } catch (e: Exception) {
        if (currentPaid > 0) {
            listOf(
                PaymentInstallment(
                    id = "init_1",
                    amount = currentPaid,
                    date = bookingDate.ifBlank { "2026-08-03" },
                    modeNote = "Cash",
                    typeLabel = "Initial Advance Payment"
                )
            )
        } else emptyList()
    }
}

fun serializePaymentHistory(list: List<PaymentInstallment>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("amount", item.amount)
        obj.put("date", item.date)
        obj.put("modeNote", item.modeNote)
        obj.put("typeLabel", item.typeLabel)
        array.put(obj)
    }
    return array.toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingRecordDetailDialog(
    record: CropRecord,
    onDismiss: () -> Unit,
    onEdit: (CropRecord) -> Unit,
    onDelete: (CropRecord) -> Unit,
    onUpdateRecord: suspend (CropRecord) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSavingInstallment by remember { mutableStateOf(false) }
    val isDark = isAppInDarkMode()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var showReceivedConfirm by remember { mutableStateOf(false) }
    var isMarkingReceived by remember { mutableStateOf(false) }
    var receiptPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showWhatsAppConfirm by remember { mutableStateOf(false) }
    var showSmsConfirm by remember { mutableStateOf(false) }
    var showTrackingWaConfirm by remember { mutableStateOf(false) }
    var showShareReceiptConfirm by remember { mutableStateOf(false) }
    var installmentToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTemplate by remember { mutableStateOf("Booking Confirmation") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var installments by remember(record.paymentHistoryJson, record.amountPaid) {
        mutableStateOf(parsePaymentHistory(record.paymentHistoryJson, record.amountPaid, record.bookingDate))
    }

    LaunchedEffect(record.id, record.paymentHistoryJson, record.amountPaid) {
        installments = parsePaymentHistory(record.paymentHistoryJson, record.amountPaid, record.bookingDate)
    }

    var newAmountText by remember { mutableStateOf("") }
    var dateTFV by remember { mutableStateOf(TextFieldValue(text = todayStr, selection = TextRange(todayStr.length))) }
    var modeNoteText by remember { mutableStateOf("Cash") }

    val totalRecordValue = record.calculateTotalAmount()
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (record.isCancelled) {
                    CancelledWatermark(isDark = isDark)
                } else if (record.isReceived) {
                    ReceivedWatermark(isDark = isDark)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Serial No. ${record.serialNumber.ifBlank { "01" }}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (record.isCancelled) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = "CANCELLED",
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        } else if (record.isReceived) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF6EE7B7))
                            ) {
                                Text(
                                    text = "RECEIVED",
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEdit(record)
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

                // 2. Farmer Header Card (Profile Avatar in Theme Color + Category + Name + Phone + Address)
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
                        // Profile Avatar in Theme Palette
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = record.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F",
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
                                text = record.serviceType.ifBlank { "Local Plants" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = record.farmerName.ifBlank { "Farmer Name" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            if (record.contactNumber.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${record.contactNumber}"))
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
                                        text = record.contactNumber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                            Text(
                                text = "Address: ${record.farmerAddress.ifBlank { "Not specified" }}",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            if (MapHelper.isGoogleMapsUrl(record.location)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            MapHelper.openGoogleMaps(context, record.location)
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Open in Google Maps",
                                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "📍 Open Location in Google Maps",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Specifications List
                val isImportedRootstocks = record.serviceType.equals("Rootstocks", ignoreCase = true) ||
                        record.serviceType.contains("Rootstock", ignoreCase = true) ||
                        record.serviceType.equals("Imported Rootstocks", ignoreCase = true) ||
                        record.serviceType.equals("Imported Rootstock", ignoreCase = true)
                val isSiteVisit = record.serviceType.equals("Site Visit", ignoreCase = true)

                val parsedVarietyLines = remember(record.varietyLinesJson) { com.example.data.parseVarietyLines(record.varietyLinesJson) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRowItem(label = "Category", value = record.serviceType, isDark = isDark)

                    if (record.location.isNotBlank()) {
                        if (MapHelper.isGoogleMapsUrl(record.location)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        MapHelper.openGoogleMaps(context, record.location)
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSiteVisit) "Orchard/Site Location" else "Orchard Location",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Open in Google Maps",
                                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "📍 Open Location in Google Maps",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        } else {
                            DetailRowItem(
                                label = if (isSiteVisit) "Orchard/Site Location" else "Orchard Location",
                                value = record.location,
                                isDark = isDark
                            )
                        }
                    }
                    
                    if (parsedVarietyLines.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Itemized Varieties (${parsedVarietyLines.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                parsedVarietyLines.forEachIndexed { idx, line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${line.variety}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                            val subInfo = buildString {
                                                if (line.rootstock.isNotBlank()) append("RS: ${line.rootstock}")
                                                if (line.feathers.isNotBlank()) {
                                                    if (isNotEmpty()) append(" • ")
                                                    append("Feathers: ${line.feathers}")
                                                }
                                            }
                                            if (subInfo.isNotBlank()) {
                                                Text(
                                                    text = subInfo,
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${line.quantity} × ₹${line.unitPrice.toInt()} = ₹${(line.quantity * line.unitPrice).toInt()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (idx < parsedVarietyLines.size - 1) {
                                        Divider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    } else {
                        DetailRowItem(
                            label = if (isImportedRootstocks) "Scion Variety" else "Variety / Type",
                            value = record.plantVariety,
                            isDark = isDark
                        )
                        if (!isImportedRootstocks && record.healthStage.isNotBlank()) {
                            DetailRowItem(label = "Sapling Age", value = record.healthStage, isDark = isDark)
                        }
                        if (record.rootstock.isNotBlank()) {
                            DetailRowItem(label = "Rootstock Variety", value = record.rootstock, isDark = isDark)
                        }
                        if (record.feathers.isNotBlank()) {
                            DetailRowItem(label = "Feathers", value = if (record.feathers.all { it.isDigit() }) "${record.feathers} branches" else record.feathers, isDark = isDark)
                        }
                    }
                    DetailRowItem(
                        label = if (isImportedRootstocks) "Quantity / Roots" else "Quantity / Trees",
                        value = "${record.quantity}",
                        isDark = isDark
                    )
                    if (parsedVarietyLines.isEmpty()) {
                        DetailRowItem(label = "Rate (₹)", value = "₹${record.landAreaAcres.toInt()}", isDark = isDark)
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
                    DetailRowItem(label = "Booking Date", value = record.bookingDate.ifBlank { todayStr }, isDark = isDark)
                    DetailRowItem(label = "Expected Delivery", value = record.expectedDelivery.ifBlank { "Not set" }, isDark = isDark)
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
                                record.isCancelled -> "Cancelled" to Color(0xFFDC2626)
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

                        // Summary Box (Total Value, Total Paid, Remaining Balance Due)
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
                                Divider(color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), thickness = 0.5.dp)
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
                                    onValueChange = { newVal ->
                                        val formatted = formatAutoSlashDate(dateTFV.text, newVal.text)
                                        val newPos = if (formatted.length > dateTFV.text.length && formatted.endsWith("/")) {
                                            formatted.length
                                        } else if (newVal.selection.end <= formatted.length) {
                                            newVal.selection.end
                                        } else {
                                            formatted.length
                                        }
                                        dateTFV = TextFieldValue(text = formatted, selection = TextRange(newPos))
                                    },
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
                                        coroutineScope.launch {
                                            isSavingInstallment = true
                                            try {
                                                val newInst = PaymentInstallment(
                                                    amount = amtVal,
                                                    date = dateTFV.text.ifBlank { todayStr },
                                                    modeNote = modeNoteText.ifBlank { "Cash" },
                                                    typeLabel = if (installments.isEmpty()) "Initial Advance Payment" else "Payment Addition"
                                                )
                                                val updatedList = installments + newInst

                                                val newPaidSum = updatedList.sumOf { it.amount }
                                                val newStatus = when {
                                                    newPaidSum >= totalRecordValue - 0.01 -> "Fully Paid"
                                                    newPaidSum > 0 -> "Advance Paid"
                                                    else -> "Pending"
                                                }
                                                val jsonStr = serializePaymentHistory(updatedList)

                                                val updatedRecord = record.copy(
                                                    amountPaid = newPaidSum,
                                                    paymentStatus = newStatus,
                                                    paymentHistoryJson = jsonStr,
                                                    timestamp = System.currentTimeMillis()
                                                )

                                                try {
                                                    withTimeout(15000) {
                                                        onUpdateRecord(updatedRecord)
                                                    }
                                                    installments = updatedList
                                                    Toast.makeText(context, "Installment recorded successfully!", Toast.LENGTH_SHORT).show()
                                                    newAmountText = ""
                                                } catch (e: TimeoutCancellationException) {
                                                    Toast.makeText(context, "Save timed out — check connection", Toast.LENGTH_LONG).show()
                                                } catch (e: Throwable) {
                                                    Toast.makeText(context, "Failed to save installment: ${e.javaClass.simpleName}: ${e.message ?: e.toString()}", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Throwable) {
                                                Toast.makeText(context, "Error: ${e.javaClass.simpleName}: ${e.message ?: e.toString()}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSavingInstallment = false
                                            }
                                        }
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
                                                            text = inst.typeLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Date: ${inst.date.ifBlank { todayStr }} • ${inst.modeNote}",
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
                    val generatedRecordMsg = com.example.util.MessageTemplateHelper.generateMessage(
                        template = selectedTemplate,
                        farmerName = record.farmerName,
                        contactNumber = record.contactNumber,
                        address = record.farmerAddress,
                        location = record.location,
                        serviceCategory = record.serviceType,
                        plantVariety = record.plantVariety,
                        quantity = "${record.quantity}",
                        totalAmount = totalRecordValue,
                        amountPaid = totalPaidSoFar,
                        remainingBalance = remainingBalance,
                        paymentStatus = record.paymentStatus,
                        bookingDate = record.bookingDate,
                        expectedDelivery = record.expectedDelivery,
                        serialNumber = record.serialNumber
                    )

                    // Message Preview Section with Select Template Dropdown
                    com.example.util.MessagePreviewComponent(
                        selectedTemplate = selectedTemplate,
                        onSelectTemplate = { selectedTemplate = it },
                        generatedMessage = generatedRecordMsg,
                        isDark = isDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Button 1: Preview & Send Digital Receipt Image
                    Button(
                        onClick = {
                            val isRootstockRec = record.serviceType.equals("Rootstocks", ignoreCase = true) ||
                                    record.serviceType.contains("Rootstock", ignoreCase = true) ||
                                    record.serviceType.equals("Imported Rootstocks", ignoreCase = true) ||
                                    record.serviceType.equals("Imported Rootstock", ignoreCase = true)

                            val extDiameter = Regex("Root Diameter:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
                            val extScion = Regex("Scion:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
                            val extRootstock = if (record.rootstock.isNotBlank()) record.rootstock else (Regex("Rootstock:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: "")

                            val actualRs = if (isRootstockRec) extRootstock.ifBlank { "M9-T337" } else extRootstock
                            val actualDiam = extDiameter.ifBlank { "9 to 12 mm" }
                            val actualScion = extScion.ifBlank { record.plantVariety.ifBlank { "" } }

                            val rData = ReceiptData(
                                serialNumber = record.serialNumber,
                                bookingDate = record.bookingDate.ifBlank { todayStr },
                                farmerName = record.farmerName,
                                contactNumber = record.contactNumber,
                                address = record.farmerAddress,
                                orchardLocation = record.location,
                                serviceCategory = if (isRootstockRec) "Imported Rootstocks" else record.serviceType,
                                plantVariety = if (isRootstockRec) actualScion else record.plantVariety,
                                quantity = "${record.quantity}",
                                totalAmount = totalRecordValue,
                                amountPaid = totalPaidSoFar,
                                remainingBalance = remainingBalance,
                                paymentStatus = record.paymentStatus,
                                expectedDelivery = record.expectedDelivery.ifBlank { "Not set" },
                                rootstock = actualRs,
                                feathers = record.feathers,
                                rootDiameter = actualDiam,
                                scionVariety = actualScion,
                                recordType = "croprecord",
                                recordId = record.id,
                                varietyLinesJson = record.varietyLinesJson
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
                        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF4ADE80) else Color(0xFF22C55E)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF22C55E))
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Tracking Details on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Actions: Received & Cancel Booking (Visible only when booking is not cancelled and not received)
                    if (!record.isCancelled && !record.isReceived) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Button: Received
                            OutlinedButton(
                                onClick = {
                                    showReceivedConfirm = true
                                },
                                enabled = !isCancelling && !isMarkingReceived,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("received_booking_button"),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.5.dp, if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                )
                            ) {
                                if (isMarkingReceived) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Received", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Button: Cancel Booking
                            OutlinedButton(
                                onClick = {
                                    showCancelConfirm = true
                                },
                                enabled = !isCancelling && !isMarkingReceived,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("cancel_booking_button"),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                if (isCancelling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancelling...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel Booking", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Generous bottom spacer so the last button can be scrolled up clearly and comfortably
                Spacer(modifier = Modifier.height(32.dp))
            }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        DeleteBookingConfirmationDialog(
            title = "Delete this booking?",
            farmerName = record.farmerName,
            identifier = if (record.serialNumber.isNotBlank()) record.serialNumber else record.serviceType,
            onConfirm = {
                showDeleteConfirm = false
                onDismiss()
                onDelete(record)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Cancel Booking Confirmation Dialog
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { 
                if (!isCancelling) showCancelConfirm = false 
            },
            title = { 
                Text(
                    text = "Confirm Cancel?", 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                ) 
            },
            text = { 
                Text("Are you sure you want to cancel this booking? This will restore allocated stock back to inventory and mark the booking as cancelled.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        coroutineScope.launch {
                            isCancelling = true
                            try {
                                val db = com.example.data.AppDatabase.getDatabase(context)
                                val inventoryDao = db.inventoryDao()
                                val firestoreSyncManager = com.example.data.FirestoreSyncManager()

                                // 1. Restore stock to inventory directly using applyBookingDelete without deleting Room row
                                com.example.data.InventoryStockManager.applyBookingDelete(
                                    inventoryDao = inventoryDao,
                                    firestoreSyncManager = firestoreSyncManager,
                                    record = record,
                                    context = context
                                )

                                // 2. Update booking record status to isCancelled = true
                                val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                                val updatedRecord = record.copy(
                                    isCancelled = true,
                                    cancelledDate = today,
                                    timestamp = System.currentTimeMillis()
                                )

                                onUpdateRecord(updatedRecord)
                                Toast.makeText(context, "Booking cancelled and stock restored to inventory", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error cancelling booking: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isCancelling = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelConfirm = false },
                    enabled = !isCancelling
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Mark Booking as Received Confirmation Dialog
    if (showReceivedConfirm) {
        AlertDialog(
            onDismissRequest = { 
                if (!isMarkingReceived) showReceivedConfirm = false 
            },
            title = { 
                Text(
                    text = "Mark Booking as Received?", 
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                ) 
            },
            text = { 
                Text("Are you sure you want to mark this booking as Received?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReceivedConfirm = false
                        coroutineScope.launch {
                            isMarkingReceived = true
                            try {
                                val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                                val updatedRecord = record.copy(
                                    isReceived = true,
                                    receivedDate = today,
                                    timestamp = System.currentTimeMillis()
                                )

                                onUpdateRecord(updatedRecord)
                                Toast.makeText(context, "Booking marked as Received", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error updating booking: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isMarkingReceived = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF16A34A) else Color(0xFF22C55E)
                    ),
                    modifier = Modifier.testTag("confirm_received_button")
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReceivedConfirm = false },
                    enabled = !isMarkingReceived,
                    modifier = Modifier.testTag("cancel_received_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // WhatsApp Confirmation Dialog with Templates
    if (showWhatsAppConfirm) {
        val extScion = Regex("Scion:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extDiameter = Regex("Root Diameter:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extRootstock = if (record.rootstock.isNotBlank()) record.rootstock else (Regex("Rootstock:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: "")

        WhatsAppTemplateDialog(
            farmerName = record.farmerName.ifBlank { "Farmer" },
            contactNumber = record.contactNumber,
            serviceType = record.serviceType,
            amountPaid = totalPaidSoFar,
            totalAmount = totalRecordValue,
            remainingBalance = remainingBalance,
            paymentStatus = record.paymentStatus,
            serialNumber = if (record.serialNumber.isBlank()) "N/A" else record.serialNumber,
            plantVariety = record.plantVariety,
            scionVariety = extScion.ifBlank { record.plantVariety.ifBlank { "" } },
            rootstock = extRootstock,
            rootDiameter = extDiameter,
            quantity = "${record.quantity}",
            notes = record.notes,
            varietyLinesJson = record.varietyLinesJson,
            expectedDelivery = record.expectedDelivery,
            onDismiss = { showWhatsAppConfirm = false }
        )
    }

    // SMS Confirmation Dialog
    if (showSmsConfirm) {
        AlertDialog(
            onDismissRequest = { showSmsConfirm = false },
            title = { Text("Send SMS Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to send the booking confirmation SMS to ${record.farmerName} (${record.contactNumber})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSmsConfirm = false
                        sendSmsBookingConfirmation(context, record, totalRecordValue, totalPaidSoFar, remainingBalance)
                    }
                ) {
                    Text("Send", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Tracking Details WhatsApp Confirmation Dialog
    if (showTrackingWaConfirm) {
        AlertDialog(
            onDismissRequest = { showTrackingWaConfirm = false },
            title = { Text("Send Tracking Details", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to send the payment installment history and tracking details to ${record.farmerName} (${record.contactNumber}) on WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTrackingWaConfirm = false
                        sendWhatsAppTrackingDetails(context, record, installments, totalRecordValue, totalPaidSoFar, remainingBalance)
                    }
                ) {
                    Text("Send", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackingWaConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Installment Delete Confirmation Dialog
    installmentToDeleteIndex?.let { idx ->
        val inst = installments.getOrNull(idx)
        if (inst != null) {
            AlertDialog(
                onDismissRequest = { installmentToDeleteIndex = null },
                title = { Text("Delete Payment Installment", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this payment installment of ₹${inst.amount.toInt()} paid on ${inst.date.ifBlank { todayStr }}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val idxToDelete = idx
                            installmentToDeleteIndex = null
                            coroutineScope.launch {
                                val updatedList = installments.filterIndexed { i, _ -> i != idxToDelete }

                                val newPaidSum = updatedList.sumOf { it.amount }
                                val newStatus = when {
                                    newPaidSum >= totalRecordValue - 0.01 -> "Fully Paid"
                                    newPaidSum > 0 -> "Advance Paid"
                                    else -> "Pending"
                                }
                                val jsonStr = serializePaymentHistory(updatedList)

                                val updatedRecord = record.copy(
                                    amountPaid = newPaidSum,
                                    paymentStatus = newStatus,
                                    paymentHistoryJson = jsonStr,
                                    timestamp = System.currentTimeMillis()
                                )
                                onUpdateRecord(updatedRecord)
                                installments = updatedList
                            }
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
    }

    // Receipt Image Preview Dialog
    receiptPreviewBitmap?.let { bmp ->
        Dialog(onDismissRequest = { receiptPreviewBitmap = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Digital Receipt Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Receipt Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    ReceiptGenerator.printReceiptBitmap(context, bmp, record.serialNumber)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("print_receipt_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print Receipt", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }

                            Button(
                                onClick = {
                                    showShareReceiptConfirm = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("share_whatsapp_receipt_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { receiptPreviewBitmap = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            ) {
                                Text("Close", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        PrintDiagnosticTraceCard(
                            isDark = isDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Share Receipt Confirmation Dialog
    if (showShareReceiptConfirm) {
        AlertDialog(
            onDismissRequest = { showShareReceiptConfirm = false },
            title = { Text("Share Digital Receipt", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to share the digital receipt to ${record.farmerName} (${record.contactNumber.ifBlank { "N/A" }}) on WhatsApp?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareReceiptConfirm = false
                        val caption = "Dear ${if (record.farmerName.isBlank()) "Farmer" else record.farmerName}, here is your official digital receipt from Baagbaan Boi (Serial #${record.serialNumber})."
                        if (receiptPreviewBitmap != null) {
                            val uri = com.example.util.ReceiptGenerator.saveReceiptImageAndGetUri(context, receiptPreviewBitmap!!, record.serialNumber)
                            if (uri != null) {
                                com.example.util.WhatsAppHelper.sendWhatsAppMedia(
                                    context = context,
                                    rawPhone = record.contactNumber,
                                    mediaUri = uri,
                                    messageText = caption
                                )
                            } else {
                                com.example.util.WhatsAppHelper.openWhatsAppChat(
                                    context = context,
                                    rawPhone = record.contactNumber,
                                    messageText = caption
                                )
                            }
                        } else {
                            com.example.util.WhatsAppHelper.openWhatsAppChat(
                                context = context,
                                rawPhone = record.contactNumber,
                                messageText = caption
                            )
                        }
                    }
                ) {
                    Text("Send", color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareReceiptConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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
        Text(
            text = value.ifBlank { "N/A" },
            fontSize = 14.sp,
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
        val isDateField = label.contains("Date", ignoreCase = true) || label.contains("Delivery", ignoreCase = true)
        Text(
            text = value,
            fontSize = if (isDateField) 12.5.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

private fun sendWhatsAppBookingConfirmation(
    context: Context,
    record: CropRecord,
    totalVal: Double,
    paidVal: Double,
    remVal: Double
) {
    val phone = record.contactNumber
    if (phone.isNotBlank()) {
        val cleanNumber = phone.replace("[^0-9]".toRegex(), "")
        val formattedNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
        val msg = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "whatsapp_booking_short",
            data = mapOf(
                "serialNumber" to record.serialNumber,
                "farmerName" to record.farmerName,
                "serviceType" to record.serviceType,
                "plantVariety" to record.plantVariety,
                "quantity" to record.quantity.toString(),
                "totalAmount" to "₹${totalVal.toInt()}",
                "amountPaid" to "₹${paidVal.toInt()}",
                "remainingBalance" to "₹${remVal.toInt()}",
                "paymentStatus" to record.paymentStatus,
                "bookingDate" to record.bookingDate
            )
        )

        val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(msg)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Contact number is not available", Toast.LENGTH_SHORT).show()
    }
}

private fun sendSmsBookingConfirmation(
    context: Context,
    record: CropRecord,
    totalVal: Double,
    paidVal: Double,
    remVal: Double
) {
    val phone = record.contactNumber
    if (phone.isNotBlank()) {
        val msg = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "sms_booking_confirmation",
            data = mapOf(
                "serialNumber" to record.serialNumber,
                "farmerName" to record.farmerName,
                "serviceType" to record.serviceType,
                "plantVariety" to record.plantVariety,
                "totalAmount" to "₹${totalVal.toInt()}",
                "amountPaid" to "₹${paidVal.toInt()}",
                "remainingBalance" to "₹${remVal.toInt()}",
                "paymentStatus" to record.paymentStatus
            )
        )

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", msg)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "SMS app not available", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Contact number is not available", Toast.LENGTH_SHORT).show()
    }
}

private fun sendWhatsAppTrackingDetails(
    context: Context,
    record: CropRecord,
    installments: List<PaymentInstallment>,
    totalVal: Double,
    paidVal: Double,
    remVal: Double
) {
    val phone = record.contactNumber
    if (phone.isNotBlank()) {
        val instText = if (installments.isEmpty()) "None" else installments.mapIndexed { idx, item ->
            "${idx + 1}. ${item.date}: ₹${item.amount.toInt()} (${item.modeNote})"
        }.joinToString("\n")

        val msg = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "whatsapp_tracking_details",
            data = mapOf(
                "serialNumber" to record.serialNumber,
                "farmerName" to record.farmerName,
                "serviceType" to record.serviceType,
                "plantVariety" to record.plantVariety,
                "quantity" to record.quantity.toString(),
                "location" to record.location.ifBlank { record.farmerAddress },
                "expectedDelivery" to record.expectedDelivery.ifBlank { "To be scheduled" },
                "totalAmount" to "₹${totalVal.toInt()}",
                "amountPaid" to "₹${paidVal.toInt()}",
                "remainingBalance" to "₹${remVal.toInt()}",
                "paymentStatus" to record.paymentStatus,
                "paymentHistory" to instText
            )
        )

        com.example.util.WhatsAppHelper.openWhatsAppChat(
            context = context,
            rawPhone = phone,
            messageText = msg
        )
    } else {
        Toast.makeText(context, "Contact number is not available", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CancelledWatermark(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val watermarkColor = if (isDark) {
        Color(0xFFEF4444).copy(alpha = 0.16f)
    } else {
        Color(0xFFDC2626).copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .rotate(-30f)
                .border(
                    width = 4.dp,
                    color = watermarkColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text(
                text = "CANCELLED",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = watermarkColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ReceivedWatermark(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val watermarkColor = if (isDark) {
        Color(0xFF22C55E).copy(alpha = 0.16f)
    } else {
        Color(0xFF16A34A).copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .rotate(-30f)
                .border(
                    width = 4.dp,
                    color = watermarkColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text(
                text = "RECEIVED",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = watermarkColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

