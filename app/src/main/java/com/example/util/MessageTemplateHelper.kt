package com.example.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

object MessageTemplateHelper {

    val TEMPLATE_OPTIONS = listOf(
        "Booking Confirmation",
        "Payment Reminder",
        "Thank You Note",
        "Delivery Tracking"
    )

    fun generateMessage(
        template: String,
        farmerName: String,
        contactNumber: String,
        address: String,
        location: String,
        serviceCategory: String,
        plantVariety: String,
        quantity: String,
        totalAmount: Double,
        amountPaid: Double,
        remainingBalance: Double,
        paymentStatus: String,
        bookingDate: String,
        expectedDelivery: String,
        serialNumber: String
    ): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        val totalAmtFormatted = "₹${numberFormat.format(totalAmount.toLong())}"
        val paidAmtFormatted = "₹${numberFormat.format(amountPaid.toLong())}"
        val remBalFormatted = "₹${numberFormat.format(remainingBalance.toLong())}"
        val farmerNameStr = farmerName.ifBlank { "Valued Customer" }
        val contactStr = contactNumber.ifBlank { "N/A" }
        val serialStr = serialNumber.ifBlank { "N/A" }
        val varietyStr = plantVariety.ifBlank { "Standard Variety" }
        val qtyStr = quantity.ifBlank { "1" }
        val addressStr = address.ifBlank { "N/A" }
        val locationStr = location.ifBlank { "N/A" }
        val bookingDateStr = bookingDate.ifBlank { "N/A" }
        val deliveryDateStr = expectedDelivery.ifBlank { "To be scheduled" }
        val serviceStr = serviceCategory.ifBlank { "Agriculture Booking" }

        return when (template) {
            "Payment Reminder" -> """
                Dear $farmerNameStr,

                This is a friendly payment reminder for your $serviceStr booking ($serialStr - $varietyStr).

                • Total Amount: $totalAmtFormatted
                • Amount Paid: $paidAmtFormatted
                • Remaining Balance: $remBalFormatted
                • Payment Status: $paymentStatus

                Please clear the balance of $remBalFormatted at your earliest convenience. Thank you!
            """.trimIndent()

            "Thank You Note" -> """
                Dear $farmerNameStr,

                Thank you for booking $serviceStr ($varietyStr) with us!

                Ref #: $serialStr | Quantity: $qtyStr plants
                Booking Date: $bookingDateStr

                We appreciate your trust in our nursery and wish you a fruitful harvest season!
            """.trimIndent()

            "Delivery Tracking" -> """
                Dear $farmerNameStr,

                Your $serviceStr order ($serialStr - $varietyStr, Qty: $qtyStr) is scheduled for delivery/fulfillment.

                • Expected Delivery: $deliveryDateStr
                • Remaining Balance: $remBalFormatted
                • Address: $addressStr
                • Orchard Location: $locationStr

                Thank you for choosing our agricultural service!
            """.trimIndent()

            else -> """
                🧾 BAAGBAAN BOI
                Ramnagri 192303
                Contacts: +916006143037, +917006996169, +917051826858, +916005096439

                OFFICIAL DIGITAL RECEIPT / BOOKING CONFIRMATION
                ----------------------------------
                FARMER / CUSTOMER DETAILS:
                • Receipt / Serial #: $serialStr
                • Booking Date: $bookingDateStr
                • Customer Name: $farmerNameStr
                • Contact Phone: $contactStr
                • Address: $addressStr
                • Orchard / Location: $locationStr

                ORDER & SERVICE DETAILS:
                • Category: $serviceStr
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
    }
}

@Composable
fun MessagePreviewComponent(
    selectedTemplate: String,
    onSelectTemplate: (String) -> Unit,
    generatedMessage: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var templateMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val pillShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Title: MESSAGE PREVIEW
        Text(
            text = "MESSAGE PREVIEW",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Select Template Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTemplate,
                onValueChange = {},
                readOnly = true,
                enabled = true,
                label = { Text("Select Template") },
                shape = pillShape,
                trailingIcon = {
                    IconButton(onClick = { templateMenuExpanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_template_dropdown")
            )

            // Overlay box to ensure click registers anywhere on the field
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { templateMenuExpanded = true }
            )

            DropdownMenu(
                expanded = templateMenuExpanded,
                onDismissRequest = { templateMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                MessageTemplateHelper.TEMPLATE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (option == selectedTemplate) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == selectedTemplate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelectTemplate(option)
                            templateMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Message Preview Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 4.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1C1D22) else Color(0xFFF8F9FA),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF333540) else Color(0xFFE2E8F0)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            android.widget.Toast.makeText(context, "Preview text copied!", android.widget.Toast.LENGTH_SHORT).show()
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

                Text(
                    text = generatedMessage,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
