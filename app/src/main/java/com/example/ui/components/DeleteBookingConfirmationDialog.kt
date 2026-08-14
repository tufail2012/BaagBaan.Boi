package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DeleteBookingConfirmationDialog(
    title: String = "Delete this booking?",
    farmerName: String,
    identifier: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val nameDisplay = farmerName.trim().ifBlank { "Farmer" }
    val idDisplay = if (identifier.isNotBlank()) " (#${identifier.removePrefix("#")})" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text("Are you sure you want to delete the booking for $nameDisplay$idDisplay? It will be moved to Recycle Bin.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
