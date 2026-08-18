package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.UserBooking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewBookingModal(
    onDismiss: () -> Unit,
    onSave: (UserBooking) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bookingTypes = listOf("Local Plants", "Imported Plants", "Imported Rootstock", "Pruning", "Site Visit", "Garden Planning")
    val rootstockVarieties = listOf("M9T337", "MM111", "Geneva G-41", "Geneva G-11", "Geneva G-214", "Geneva G-969", "Geneva G-35", "Geneva G-979", "Geneva G-890")
    val pruningSeasons = listOf("Summer", "Winter")

    val currentDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedType by remember { mutableStateOf(bookingTypes[0]) }
    var selectedVariety by remember { mutableStateOf(rootstockVarieties[0]) }
    var selectedSeason by remember { mutableStateOf(pruningSeasons[0]) }
    var itemName by remember { mutableStateOf("") }
    var farmerName by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var bookingDate by remember { mutableStateOf(currentDateStr) }
    var notes by remember { mutableStateOf("") }

    val speechToTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                notes = if (notes.isBlank()) spokenText else "$notes $spokenText"
            }
        }
    }

    val launchSpeechToText = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak booking notes...")
            }
            speechToTextLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechToText()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var varietyMenuExpanded by remember { mutableStateOf(false) }
    var seasonMenuExpanded by remember { mutableStateOf(false) }

    var farmerNameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAddCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Booking",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_new_booking_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Booking Type Dropdown
                Text(
                    text = "Booking Type *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("booking_type_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedType,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        bookingTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontWeight = if (type == selectedType) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Conditional: Variety Dropdown (when type = Imported Rootstock)
                if (selectedType == "Imported Rootstock") {
                    Text(
                        text = "Rootstock Variety *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { varietyMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("rootstock_variety_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedVariety,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = varietyMenuExpanded,
                            onDismissRequest = { varietyMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            rootstockVarieties.forEach { varName ->
                                DropdownMenuItem(
                                    text = { Text(varName, fontWeight = if (varName == selectedVariety) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedVariety = varName
                                        varietyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. Conditional: Season Dropdown (when type = Pruning)
                if (selectedType == "Pruning") {
                    Text(
                        text = "Pruning Season *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { seasonMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("pruning_season_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSeason,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = seasonMenuExpanded,
                            onDismissRequest = { seasonMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            pruningSeasons.forEach { season ->
                                DropdownMenuItem(
                                    text = { Text(season, fontWeight = if (season == selectedSeason) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedSeason = season
                                        seasonMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 4. Item / Plant Name (Optional)
                if (selectedType != "Pruning" && selectedType != "Site Visit") {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = capitalizeWordsNaturally(it) },
                        label = { Text("Item / Plant Name (Optional)") },
                        placeholder = { Text("e.g. Gala Apple, Crimson Crisp") },
                        singleLine = true,
                        keyboardOptions = AppDefaultWordKeyboardOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_item_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 5. Farmer Name (Required)
                OutlinedTextField(
                    value = farmerName,
                    onValueChange = {
                        farmerName = capitalizeWordsNaturally(it)
                        if (it.isNotBlank()) farmerNameError = null
                    },
                    label = { Text("Farmer Name *") },
                    placeholder = { Text("e.g. Ghulam Hassan") },
                    keyboardOptions = AppDefaultWordKeyboardOptions,
                    isError = farmerNameError != null,
                    supportingText = {
                        farmerNameError?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_farmer_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 6. Quantity (Optional)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantity (Optional)") },
                    placeholder = { Text("e.g. 150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_quantity_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Booking Date
                OutlinedTextField(
                    value = bookingDate,
                    onValueChange = { bookingDate = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Booking Date (YYYY-MM-DD) *") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = capitalizeWordsNaturally(it) },
                    label = { Text("Notes / Particulars (Optional)") },
                    placeholder = { Text("Add any special instructions or requirements...") },
                    maxLines = 3,
                    keyboardOptions = AppDefaultWordKeyboardOptions,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    launchSpeechToText()
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("booking_notes_voice_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons (Cancel / Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("cancel_booking_button")
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (farmerName.trim().isEmpty()) {
                                farmerNameError = "Farmer Name is required"
                                return@Button
                            }

                            val qty = quantityText.toIntOrNull()
                            val booking = UserBooking(
                                type = selectedType,
                                variety = if (selectedType == "Imported Rootstock") selectedVariety else "",
                                season = if (selectedType == "Pruning") selectedSeason else "",
                                itemName = itemName.trim(),
                                farmerName = farmerName.trim(),
                                quantity = qty,
                                bookingDate = bookingDate.trim().ifEmpty { currentDateStr },
                                notes = notes.trim(),
                                createdAt = System.currentTimeMillis()
                            )
                            onSave(booking)
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_booking_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save Booking", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
