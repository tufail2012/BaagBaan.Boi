package com.example.ui.components.security

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.AppLockManager
import com.example.security.BiometricHelper
import com.example.security.UnlockMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier
) {
    // Intercept back press when locked so user cannot bypass
    BackHandler(enabled = true) {
        // Keeps the app locked and moves task to back if needed
    }

    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val configuredMethod = appLockManager.preferences.unlockMethod
    var activeMethod by remember { mutableStateOf(configuredMethod) }

    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }

    fun triggerBiometricPrompt() {
        if (fragmentActivity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.authenticate(
                activity = fragmentActivity,
                title = "App Locked",
                subtitle = "Unlock to continue",
                negativeButtonText = "Use alternative method",
                onSuccess = {
                    errorMessage = null
                    appLockManager.unlockApp()
                },
                onError = { code, err ->
                    errorMessage = "Authentication failed: $err"
                },
                onFailed = {
                    errorMessage = "Authentication failed"
                }
            )
        } else {
            errorMessage = "Biometric authentication not available"
        }
    }

    // Auto-trigger biometric on first launch if configured
    LaunchedEffect(Unit) {
        if (configuredMethod == UnlockMethod.BIOMETRIC) {
            triggerBiometricPrompt()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Logo / Shield Lock Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "App Lock Icon",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "App Locked",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Unlock to continue",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Render Active Unlock Method
                AnimatedContent(
                    targetState = activeMethod,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "UnlockMethodSwitch"
                ) { method ->
                    when (method) {
                        UnlockMethod.BIOMETRIC -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    onClick = {
                                        errorMessage = null
                                        triggerBiometricPrompt()
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .testTag("biometric_unlock_trigger")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint sensor",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(52.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Touch the fingerprint sensor",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                OutlinedButton(
                                    onClick = { triggerBiometricPrompt() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Use Fingerprint")
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
                                            errorMessage = null
                                            appLockManager.unlockApp()
                                        } else {
                                            errorMessage = "Incorrect PIN"
                                            enteredPin = ""
                                        }
                                    }
                                },
                                errorMessage = errorMessage,
                                onBiometricClick = if (BiometricHelper.isBiometricAvailable(context)) {
                                    { triggerBiometricPrompt() }
                                } else null
                            )
                        }

                        UnlockMethod.PATTERN -> {
                            PatternLockView(
                                onPatternCompleted = { patternNodes ->
                                    val patternString = patternNodes.joinToString("-")
                                    if (appLockManager.verifyCredential(patternString)) {
                                        isPatternError = false
                                        errorMessage = null
                                        appLockManager.unlockApp()
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
                                        errorMessage = null
                                        appLockManager.unlockApp()
                                    } else {
                                        errorMessage = "Incorrect password"
                                    }
                                },
                                errorMessage = errorMessage
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Secondary "Use another method" Option
                Box {
                    TextButton(
                        onClick = { showMethodPicker = true },
                        modifier = Modifier.testTag("use_another_method_button")
                    ) {
                        Text(
                            text = "Use another method",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showMethodPicker,
                        onDismissRequest = { showMethodPicker = false }
                    ) {
                        if (BiometricHelper.isBiometricAvailable(context)) {
                            DropdownMenuItem(
                                text = { Text("Fingerprint / Biometric") },
                                leadingIcon = {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showMethodPicker = false
                                    activeMethod = UnlockMethod.BIOMETRIC
                                    errorMessage = null
                                    triggerBiometricPrompt()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("PIN") },
                            leadingIcon = {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMethodPicker = false
                                activeMethod = UnlockMethod.PIN
                                enteredPin = ""
                                errorMessage = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pattern") },
                            leadingIcon = {
                                Icon(Icons.Default.Pattern, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMethodPicker = false
                                activeMethod = UnlockMethod.PATTERN
                                isPatternError = false
                                errorMessage = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMethodPicker = false
                                activeMethod = UnlockMethod.PASSWORD
                                enteredPassword = ""
                                errorMessage = null
                            }
                        )
                    }
                }
            }
        }
    }
}
