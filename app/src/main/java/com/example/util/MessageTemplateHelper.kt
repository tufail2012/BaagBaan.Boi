package com.example.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.MessageTemplateRepository
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

        val templateId = when (template) {
            "Payment Reminder" -> "payment_reminder"
            "Thank You Note" -> "thank_you_note"
            "Delivery Tracking" -> "delivery_tracking"
            else -> "booking_confirmation_official"
        }

        val data = mapOf(
            "farmerName" to farmerNameStr,
            "contactNumber" to contactStr,
            "serialNumber" to serialStr,
            "plantVariety" to varietyStr,
            "quantity" to qtyStr,
            "address" to addressStr,
            "location" to locationStr,
            "bookingDate" to bookingDateStr,
            "expectedDelivery" to deliveryDateStr,
            "serviceCategory" to serviceStr,
            "serviceType" to serviceStr,
            "totalAmount" to totalAmtFormatted,
            "amountPaid" to paidAmtFormatted,
            "remainingBalance" to remBalFormatted,
            "paymentStatus" to paymentStatus
        )

        return MessageTemplateRepository.renderTemplate(templateId, data)
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
    var isExpanded by remember { mutableStateOf(false) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val pillShape = RoundedCornerShape(24.dp)
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = cardShape
            )
            .clip(cardShape)
            .testTag("message_preview_collapsible_card"),
        shape = cardShape,
        color = if (isDark) Color(0xFF1C1D22) else Color(0xFFF8F9FA),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF333540) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tappable Accordion Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("message_preview_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Message Preview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Message Preview" else "Expand Message Preview",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expandable Message Content (Collapsed by default)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF333540) else Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )

                    // Select Template Dropdown
                    Box(
                        modifier = Modifier.fillMaxWidth()
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
                                elevation = if (isDark) 2.dp else 1.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF141518) else Color(0xFFFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF2A2C36) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
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
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("copy_preview_button")
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
                                    .testTag("preview_message_text")
                            )
                        }
                    }
                }
            }
        }
    }
}
