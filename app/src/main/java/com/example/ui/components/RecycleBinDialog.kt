package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinDialog(
    onDismissRequest: () -> Unit,
    db: AppDatabase,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreSyncManager = remember { FirestoreSyncManager() }

    val deletedItems by db.recycleBinDao().getAllDeletedItems().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RestoreFromTrash,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Recycle Bin",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "${deletedItems.size} deleted item(s)",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    TextButton(onClick = onDismissRequest) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                if (deletedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Recycle Bin is empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = "Deleted bookings and contacts will appear here",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
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
                        items(deletedItems, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
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
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.itemType == "BOOKING") Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (item.itemType == "BOOKING") "BOOKING RECORD" else "CONTACT",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.itemType == "BOOKING") Color(0xFF2563EB) else Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = dateFormat.format(Date(item.deletedAt)),
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    Text(
                                        text = item.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )

                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Restore Button
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        if (item.itemType == "BOOKING") {
                                                            val restoredRecord = RecycleBinConverter.jsonToCropRecord(item.jsonPayload)
                                                            db.cropRecordDao().insertRecord(restoredRecord)
                                                            firestoreSyncManager.saveCropRecord(restoredRecord)
                                                            val validCategories = listOf("Local Plants", "Imported Plants", "Imported Rootstock")
                                                            val categoryMatch = validCategories.firstOrNull { restoredRecord.serviceType.contains(it, ignoreCase = true) }
                                                            if (categoryMatch != null) {
                                                                val varietyToMatch = if (restoredRecord.rootstock.isNotBlank()) restoredRecord.rootstock else restoredRecord.plantVariety
                                                                val invItem = db.inventoryDao().findMatchingItem(categoryMatch, varietyToMatch)
                                                                    ?: db.inventoryDao().findMatchingItemByCategory(categoryMatch)
                                                                if (invItem != null) {
                                                                    db.inventoryDao().decrementQuantity(invItem.id, restoredRecord.quantity)
                                                                    val updated = db.inventoryDao().getItemById(invItem.id)
                                                                    if (updated != null) {
                                                                        firestoreSyncManager.saveInventoryItem(updated)
                                                                    }
                                                                }
                                                            }
                                                        } else if (item.itemType == "CONTACT") {
                                                            val restoredContact = RecycleBinConverter.jsonToContact(item.jsonPayload)
                                                            db.farmerContactDao().insertContact(restoredContact)
                                                        } else if (item.itemType == "GARDEN_PLANNING") {
                                                            val restoredEntry = RecycleBinConverter.jsonToGardenPlanning(item.jsonPayload)
                                                            db.gardenPlanningDao().insertEntry(restoredEntry)
                                                            firestoreSyncManager.saveGardenPlanningEntry(restoredEntry)
                                                        }
                                                        db.recycleBinDao().delete(item)
                                                        firestoreSyncManager.deleteRecycleBinItem(item.id)

                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Item restored successfully", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Error restoring item: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("restore_item_button_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Permanently Delete Button
                                        Button(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    db.recycleBinDao().delete(item)
                                                    firestoreSyncManager.deleteRecycleBinItem(item.id)
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("delete_permanently_button_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete Permanently", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
