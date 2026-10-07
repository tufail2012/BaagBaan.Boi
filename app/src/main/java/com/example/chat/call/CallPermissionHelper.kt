package com.example.chat.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

object CallPermissionHelper {

    fun getRequiredCallPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions
    }

    fun hasCallPermissions(context: Context): Boolean {
        return getRequiredCallPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun CallPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = accentColor
            )
        },
        title = {
            Text(
                text = "Permissions Required for Calls",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Baagbaan Boi requires Camera and Microphone access to make high-definition voice and video calls with fellow members.",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Microphone: Enables two-way voice streaming during audio and video calls.\n• Camera: Enables live video streaming during video calls.\n• Notifications: Informs you of incoming calls even when the app is running in the background.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow Permissions", color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Composable
fun ZegoConfigRequiredDialog(
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    reason: String? = null
) {
    val liveReason = reason ?: ZegoCallManager.getConfigurationError() ?: ZegoCallManager.getCredentialsError()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accentColor
            )
        },
        title = {
            Text(
                text = "ZegoCloud Setup Required",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Calling infrastructure (ZegoCloud Call Kit with FCM Offline Push) is ready in code. To connect live calls:",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                val reasonSuffix = if (!liveReason.isNullOrBlank()) "\n\nReason: $liveReason" else ""
                Text(
                    text = "1. Open gradle.properties (or local.properties)\n2. Add your real Zego credentials:\n   ZEGO_APP_ID=YOUR_APP_ID\n   ZEGO_APP_SIGN=YOUR_APP_SIGN\n3. Rebuild the app to initiate live calls.$reasonSuffix",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
