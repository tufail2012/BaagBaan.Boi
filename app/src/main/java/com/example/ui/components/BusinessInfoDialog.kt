package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BusinessInfo
import com.example.data.BusinessInfoRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessInfoDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Observe live business info from repository
    val cloudBusinessInfo by BusinessInfoRepository.businessInfo.collectAsState()

    // Local form editing states
    var businessName by remember { mutableStateOf(cloudBusinessInfo.businessName) }
    var tagline by remember { mutableStateOf(cloudBusinessInfo.tagline) }
    var address by remember { mutableStateOf(cloudBusinessInfo.address) }
    var contactNumbers by remember {
        mutableStateOf(
            if (cloudBusinessInfo.contactNumbers.isNotEmpty()) cloudBusinessInfo.contactNumbers else listOf("+91 ")
        )
    }
    var accountNumber by remember { mutableStateOf(cloudBusinessInfo.accountNumber) }
    var accountHolderName by remember { mutableStateOf(cloudBusinessInfo.accountHolderName) }
    var ifscCode by remember { mutableStateOf(cloudBusinessInfo.ifscCode) }
    var registrationNumber by remember { mutableStateOf(cloudBusinessInfo.registrationNumber) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Synchronize local form whenever remote business info changes (unless user is currently editing and saving)
    LaunchedEffect(cloudBusinessInfo) {
        if (!isSaving) {
            businessName = cloudBusinessInfo.businessName
            tagline = cloudBusinessInfo.tagline
            address = cloudBusinessInfo.address
            contactNumbers = if (cloudBusinessInfo.contactNumbers.isNotEmpty()) cloudBusinessInfo.contactNumbers else listOf("+91 ")
            accountNumber = cloudBusinessInfo.accountNumber
            accountHolderName = cloudBusinessInfo.accountHolderName
            ifscCode = cloudBusinessInfo.ifscCode
            registrationNumber = cloudBusinessInfo.registrationNumber
        }
    }

    BackHandler(enabled = !isSaving) {
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // Wide Pill-Shaped Glass Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .glassCardBackground(
                            isDark = isDark,
                            accentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(percent = 50)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isSaving,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("business_info_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text(
                                    text = "Business Info",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Shared identity for receipts & bookings",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Live Sync Indicator
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32))
                                )
                                Text(
                                    text = "Cloud Sync",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error / Success Feedback Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = successMessage != null) {
                    successMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                    // Business Branding Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Business Identity",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Business Name
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = capitalizeWordsNaturally(it) },
                                label = { Text("Business Name *") },
                                placeholder = { Text("e.g. BAAGBAAN BOI") },
                                singleLine = true,
                                keyboardOptions = AppDefaultWordKeyboardOptions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_name_input")
                            )

                            // Tagline
                            OutlinedTextField(
                                value = tagline,
                                onValueChange = { tagline = capitalizeWordsNaturally(it) },
                                label = { Text("Tagline / Slogan") },
                                placeholder = { Text("e.g. The Streets of Kashmir") },
                                singleLine = true,
                                keyboardOptions = AppDefaultWordKeyboardOptions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_tagline_input")
                            )

                            // Address
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = capitalizeWordsNaturally(it) },
                                label = { Text("Business Address *") },
                                placeholder = { Text("e.g. Ramnagri 192303, Shopian, Jammu & Kashmir") },
                                minLines = 2,
                                maxLines = 3,
                                keyboardOptions = AppDefaultWordKeyboardOptions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_address_input")
                            )

                            // Registration Number (GSTIN)
                            OutlinedTextField(
                                value = registrationNumber,
                                onValueChange = { registrationNumber = it },
                                label = { Text("Registration Number / GSTIN") },
                                placeholder = { Text("e.g. 01EBWPG3946L1Z7") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_registration_input")
                            )
                        }
                    }

                    // Contact Numbers Card (Dynamic List)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Contact Numbers",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        contactNumbers = contactNumbers + "+91 "
                                    },
                                    modifier = Modifier.testTag("business_info_add_contact_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Row", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            contactNumbers.forEachIndexed { index, phoneVal ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = phoneVal,
                                        onValueChange = { raw ->
                                            val prefix = "+91 "
                                            val text = raw
                                            val cleanDigits = if (text.startsWith(prefix)) {
                                                text.substring(prefix.length).filter { it.isDigit() }
                                            } else {
                                                text.removePrefix("+91").removePrefix("+").filter { it.isDigit() }
                                            }.take(10)

                                            val formatted = prefix + cleanDigits
                                            val updated = contactNumbers.toMutableList()
                                            updated[index] = formatted
                                            contactNumbers = updated
                                        },
                                        label = { Text("Phone #${index + 1}") },
                                        placeholder = { Text("e.g. 6006143037") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("business_info_phone_input_$index")
                                    )

                                    if (contactNumbers.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = contactNumbers.toMutableList()
                                                updated.removeAt(index)
                                                contactNumbers = updated
                                            },
                                            modifier = Modifier.testTag("business_info_remove_phone_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove Phone",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Banking & Payment Details Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Bank Account Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Account Number
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("Bank Account Number") },
                                placeholder = { Text("e.g. 0018010100007537") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_account_number_input")
                            )

                            // Account Holder Name
                            OutlinedTextField(
                                value = accountHolderName,
                                onValueChange = { accountHolderName = capitalizeWordsNaturally(it) },
                                label = { Text("Account Holder Name") },
                                placeholder = { Text("e.g. Aamir Manzoor Ganaie") },
                                singleLine = true,
                                keyboardOptions = AppDefaultWordKeyboardOptions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_holder_name_input")
                            )

                            // IFSC Code
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase() },
                                label = { Text("Bank IFSC Code") },
                                placeholder = { Text("e.g. JAKA0SHOPAN") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("business_info_ifsc_input")
                            )
                        }
                    }

                    // Action Buttons (Reset Default & Save Business Info)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                businessName = BusinessInfo.DEFAULT.businessName
                                tagline = BusinessInfo.DEFAULT.tagline
                                address = BusinessInfo.DEFAULT.address
                                contactNumbers = BusinessInfo.DEFAULT.contactNumbers
                                accountNumber = BusinessInfo.DEFAULT.accountNumber
                                accountHolderName = BusinessInfo.DEFAULT.accountHolderName
                                ifscCode = BusinessInfo.DEFAULT.ifscCode
                                registrationNumber = BusinessInfo.DEFAULT.registrationNumber
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("business_info_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Default", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (businessName.isBlank()) {
                                    errorMessage = "Business Name cannot be empty"
                                    return@Button
                                }
                                if (address.isBlank()) {
                                    errorMessage = "Address cannot be empty"
                                    return@Button
                                }

                                isSaving = true
                                errorMessage = null
                                successMessage = null

                                val sanitizedContacts = contactNumbers
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() && it != "+91" && it != "+91 " }

                                val updatedInfo = BusinessInfo(
                                    businessName = businessName.trim(),
                                    tagline = tagline.trim(),
                                    address = address.trim(),
                                    contactNumbers = if (sanitizedContacts.isNotEmpty()) sanitizedContacts else listOf("+916006143037"),
                                    accountNumber = accountNumber.trim(),
                                    accountHolderName = accountHolderName.trim(),
                                    ifscCode = ifscCode.trim(),
                                    registrationNumber = registrationNumber.trim()
                                )

                                coroutineScope.launch {
                                    try {
                                        val res = BusinessInfoRepository.saveBusinessInfo(updatedInfo, context)
                                        if (res.isSuccess) {
                                            successMessage = "Business Info saved & synced across devices!"
                                            Toast.makeText(context, "Business Info saved successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val err = res.exceptionOrNull()?.message ?: "Unknown error"
                                            errorMessage = "Failed to save: $err"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("business_info_save_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Business Info", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    // Safe bottom spacing & navigation bar inset clearance
                    Spacer(modifier = Modifier.navigationBarsPadding())
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
    }
}
