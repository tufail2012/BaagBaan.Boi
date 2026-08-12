package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import com.example.util.rememberScrollHapticFeedback
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AgriGreenPrimary
import com.example.util.ReceiptData
import com.example.util.ReceiptGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// WhatsApp Brand Green Color
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)
val WhatsAppLightBg = Color(0xFFE7F8EE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppTemplateDialog(
    farmerName: String,
    contactNumber: String,
    serviceType: String = "Agri Service",
    amountPaid: Double = 0.0,
    totalAmount: Double = 0.0,
    remainingBalance: Double = 0.0,
    paymentStatus: String = "Pending",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTemplateIndex by remember { mutableIntStateOf(0) }

    // Digital Receipt Generation State
    var receiptBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var showFullScreenReceipt by remember { mutableStateOf(false) }

    val currentDateStr = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    val serialNumber = remember(farmerName, serviceType) {
        val hash = kotlin.math.abs("$farmerName$serviceType".hashCode()) % 100000
        "REC-${String.format("%05d", hash)}"
    }

    val calculatedBalance = remember(remainingBalance, totalAmount, amountPaid) {
        if (remainingBalance > 0) remainingBalance else maxOf(0.0, totalAmount - amountPaid)
    }

    // Generate Receipt bitmap & URI when Digital Receipt (index 2) is selected
    LaunchedEffect(selectedTemplateIndex, farmerName, totalAmount, amountPaid) {
        if (selectedTemplateIndex == 2 && receiptBitmap == null) {
            val rData = ReceiptData(
                serialNumber = serialNumber,
                bookingDate = currentDateStr,
                farmerName = farmerName.ifBlank { "Farmer" },
                contactNumber = contactNumber,
                address = "Jammu & Kashmir",
                orchardLocation = "Apple Orchard",
                serviceCategory = serviceType,
                plantVariety = serviceType,
                quantity = "1",
                totalAmount = totalAmount,
                amountPaid = amountPaid,
                remainingBalance = calculatedBalance,
                paymentStatus = paymentStatus,
                expectedDelivery = "Scheduled As Agreed"
            )
            val bmp = ReceiptGenerator.generateReceiptBitmap(rData, context)
            val uri = ReceiptGenerator.saveReceiptImageAndGetUri(context, bmp, serialNumber)
            receiptBitmap = bmp
            receiptUri = uri
        }
    }

    fun generateMessage(index: Int): String {
        return when (index) {
            0 -> {
                // Booking Confirmation
                "Hello $farmerName,\n\nYour booking for *$serviceType* with *AgriCrop* is confirmed!\n\n" +
                        "• Farmer Name: $farmerName\n" +
                        "• Service: $serviceType\n" +
                        "• Total Amount: ₹${String.format("%.2f", totalAmount)}\n" +
                        "• Status: $paymentStatus\n\n" +
                        "Thank you for choosing AgriCrop!"
            }
            1 -> {
                // Payment Reminder
                "Dear $farmerName,\n\nThis is a friendly payment reminder regarding your *$serviceType* service with *AgriCrop*.\n\n" +
                        "• Total Amount: ₹${String.format("%.2f", totalAmount)}\n" +
                        "• Amount Paid: ₹${String.format("%.2f", amountPaid)}\n" +
                        "• Pending Balance: ₹${String.format("%.2f", calculatedBalance)}\n\n" +
                        "Kindly complete the remaining payment at your earliest convenience. Thank you!"
            }
            2 -> {
                // Digital Receipt Text Summary
                "🌾 *BAAGBAAN BOI - OFFICIAL DIGITAL RECEIPT* 🌾\n" +
                        "Registration Number: 01EBWPG3946L1Z7\n\n" +
                        "• Receipt #: $serialNumber\n" +
                        "• Date: $currentDateStr\n" +
                        "• Customer Name: $farmerName\n" +
                        "• Service Category: $serviceType\n" +
                        "• Total Amount: ₹${String.format("%.2f", totalAmount)}\n" +
                        "• Amount Paid: ₹${String.format("%.2f", amountPaid)}\n" +
                        "• Balance Due: ₹${String.format("%.2f", calculatedBalance)}\n" +
                        "• Payment Status: $paymentStatus\n" +
                        "• Account No: 0018010100007537\n" +
                        "• IFSC Code: JAKA0SHOPAN\n\n" +
                        "Thank you for doing business with Baagbaan Boi!"
            }
            else -> ""
        }
    }

    var editedMessage by remember(selectedTemplateIndex, farmerName, totalAmount, amountPaid) {
        mutableStateOf(generateMessage(selectedTemplateIndex))
    }

    fun launchWhatsAppText(phone: String, text: String) {
        var cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.length == 10) {
            cleanPhone = "91$cleanPhone"
        }

        try {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchWhatsAppImage(phone: String, uri: Uri, caption: String) {
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

        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (formattedPhone.isNotEmpty()) {
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
            }
            putExtra(Intent.EXTRA_TEXT, caption)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(waIntent)
        } catch (_: Exception) {
            try {
                val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    if (formattedPhone.isNotEmpty()) {
                        putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    }
                    putExtra(Intent.EXTRA_TEXT, caption)
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(waBusinessIntent)
            } catch (_: Exception) {
                // Fallback to chooser
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Digital Receipt"))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        val dialogScrollState = rememberScrollState()
        dialogScrollState.rememberScrollHapticFeedback()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(dialogScrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Send WhatsApp Message",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "$farmerName (${contactNumber.ifEmpty { "No phone" }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Template Option",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Template Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Template 0: Booking Confirmation
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTemplateIndex = 0
                            editedMessage = generateMessage(0)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplateIndex == 0) WhatsAppLightBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (selectedTemplateIndex == 0) androidx.compose.foundation.BorderStroke(2.dp, WhatsAppGreen) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedTemplateIndex == 0) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Booking\nConfirm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTemplateIndex == 0) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Template 1: Payment Reminder
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTemplateIndex = 1
                            editedMessage = generateMessage(1)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplateIndex == 1) WhatsAppLightBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (selectedTemplateIndex == 1) androidx.compose.foundation.BorderStroke(2.dp, WhatsAppGreen) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = if (selectedTemplateIndex == 1) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Payment\nReminder",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTemplateIndex == 1) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Template 2: Digital Receipt
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTemplateIndex = 2
                            editedMessage = generateMessage(2)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplateIndex == 2) WhatsAppLightBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (selectedTemplateIndex == 2) androidx.compose.foundation.BorderStroke(2.dp, WhatsAppGreen) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (selectedTemplateIndex == 2) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Digital\nReceipt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTemplateIndex == 2) WhatsAppDarkGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTemplateIndex == 2) {
                // DIGITAL RECEIPT DOCUMENT VIEW & PREVIEW
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = WhatsAppDarkGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Formatted Digital Receipt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = WhatsAppDarkGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WhatsAppGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = serialNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = WhatsAppDarkGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rendered Document Image Preview Card
                        if (receiptBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D0D0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFullScreenReceipt = true }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = receiptBitmap!!.asImageBitmap(),
                                        contentDescription = "Digital Receipt Document Image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Zoom hint badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tap to expand",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Generating official receipt document...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Receipt Breakdown Card Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Customer Name:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(farmerName.ifBlank { "Farmer" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Service / Variety:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(serviceType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Amount:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format("%.2f", totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount Paid:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format("%.2f", amountPaid)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining Balance:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "₹${String.format("%.2f", calculatedBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (calculatedBalance > 0) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                                    )
                                }

                                 Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Account No:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("0018010100007537", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("IFSC Code:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("JAKA0SHOPAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (paymentStatus.equals("Cleared", ignoreCase = true) || paymentStatus.equals("Fully Paid", ignoreCase = true)) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                    ) {
                                        Text(
                                            text = paymentStatus,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (paymentStatus.equals("Cleared", ignoreCase = true) || paymentStatus.equals("Fully Paid", ignoreCase = true)) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main Action Buttons for Digital Receipt Document
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Send Receipt Image via WhatsApp Button
                            Button(
                                onClick = {
                                    if (receiptUri != null) {
                                        launchWhatsAppImage(
                                            phone = contactNumber,
                                            uri = receiptUri!!,
                                            caption = "Dear $farmerName, here is your official digital receipt from Baagbaan Boi ($serialNumber)."
                                        )
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Receipt image still generating...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("send_whatsapp_receipt_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Receipt Document via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Share Image File
                                OutlinedButton(
                                    onClick = {
                                        if (receiptUri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, receiptUri)
                                                putExtra(Intent.EXTRA_TEXT, "Official Digital Receipt - $farmerName ($serialNumber)")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt Document"))
                                        } else {
                                            Toast.makeText(context, "Receipt image file not ready", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share File", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Copy Text Summary
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Receipt Summary", editedMessage)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Text receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Editable Text Area for Template 0 & 1
                OutlinedTextField(
                    value = editedMessage,
                    onValueChange = { editedMessage = it },
                    label = { Text("Message Text (Editable)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppDarkGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("WhatsApp Template", editedMessage)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontWeight = FontWeight.Bold)
                    }

                    // Send via WhatsApp Button
                    Button(
                        onClick = {
                            if (contactNumber.trim().isEmpty()) {
                                Toast.makeText(context, "No contact number provided for this farmer", Toast.LENGTH_SHORT).show()
                            } else {
                                launchWhatsAppText(contactNumber, editedMessage)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("send_whatsapp_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    // Full Screen Receipt Preview Modal
    if (showFullScreenReceipt && receiptBitmap != null) {
        Dialog(onDismissRequest = { showFullScreenReceipt = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Digital Receipt Document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WhatsAppDarkGreen
                        )
                        IconButton(onClick = { showFullScreenReceipt = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Image(
                        bitmap = receiptBitmap!!.asImageBitmap(),
                        contentDescription = "Full Digital Receipt Document",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showFullScreenReceipt = false
                            if (receiptUri != null) {
                                launchWhatsAppImage(
                                    phone = contactNumber,
                                    uri = receiptUri!!,
                                    caption = "Dear $farmerName, here is your official digital receipt from Baagbaan Boi ($serialNumber)."
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
