package com.example.ui.components.security

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.AppLockManager
import com.example.security.BiometricHelper
import com.example.security.UnlockMethod

@Composable
fun AuthenticateVerificationDialog(
    title: String,
    subtitle: String,
    appLockManager: AppLockManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val currentMethod = appLockManager.preferences.unlockMethod

    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        if (fragmentActivity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.authenticate(
                activity = fragmentActivity,
                title = title,
                subtitle = subtitle,
                negativeButtonText = "Cancel",
                onSuccess = {
                    onSuccess()
                },
                onError = { _, err ->
                    errorMessage = err.toString()
                },
                onFailed = {
                    errorMessage = "Authentication failed"
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (currentMethod == UnlockMethod.BIOMETRIC) {
            triggerBiometric()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentMethod) {
                    UnlockMethod.BIOMETRIC -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Please verify your fingerprint",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!errorMessage.isNullOrBlank()) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { triggerBiometric() }) {
                                Text("Retry Biometric")
                            }
                        }
                    }

                    UnlockMethod.PIN -> {
                        PinKeypadView(
                            pinLength = 4,
                            currentPin = enteredPin,
                            onPinChange = { newPin ->
                                errorMessage = null
                                enteredPin = newPin
                                if (newPin.length == 4) {
                                    if (appLockManager.verifyCredential(newPin)) {
                                        onSuccess()
                                    } else {
                                        errorMessage = "Incorrect PIN"
                                        enteredPin = ""
                                    }
                                }
                            },
                            errorMessage = errorMessage
                        )
                    }

                    UnlockMethod.PATTERN -> {
                        PatternLockView(
                            onPatternCompleted = { patternNodes ->
                                val patternString = patternNodes.joinToString("-")
                                if (appLockManager.verifyCredential(patternString)) {
                                    isPatternError = false
                                    errorMessage = null
                                    onSuccess()
                                } else {
                                    isPatternError = true
                                    errorMessage = "Incorrect pattern"
                                }
                            },
                            isError = isPatternError,
                            errorMessage = errorMessage
                        )
                    }

                    UnlockMethod.PASSWORD -> {
                        PasswordLockView(
                            password = enteredPassword,
                            onPasswordChange = {
                                enteredPassword = it
                                errorMessage = null
                            },
                            onUnlock = {
                                if (appLockManager.verifyCredential(enteredPassword)) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Incorrect password"
                                }
                            },
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
