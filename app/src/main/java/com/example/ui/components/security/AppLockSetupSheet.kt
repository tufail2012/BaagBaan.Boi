package com.example.ui.components.security

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.security.UnlockMethod

private enum class SetupStep {
    SELECT_METHOD,
    SETUP_PIN_CREATE,
    SETUP_PIN_CONFIRM,
    SETUP_PATTERN_CREATE,
    SETUP_PATTERN_CONFIRM,
    SETUP_PASSWORD_CREATE,
    SETUP_PASSWORD_CONFIRM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupSheet(
    isChangeMethodFlow: Boolean = false,
    onCompleteSetup: (method: UnlockMethod, credential: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity

    var currentStep by remember { mutableStateOf(SetupStep.SELECT_METHOD) }

    // PIN State
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Pattern State
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var patternError by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }

    // Password State
    var firstPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passVisible1 by remember { mutableStateOf(false) }
    var passVisible2 by remember { mutableStateOf(false) }

    val biometricStatus = remember { BiometricHelper.getBiometricStatus(context) }
    val isBiometricAvailable = biometricStatus == BiometricHelper.BiometricStatus.AVAILABLE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep != SetupStep.SELECT_METHOD) {
                    IconButton(
                        onClick = {
                            currentStep = SetupStep.SELECT_METHOD
                            pinError = null
                            patternError = null
                            passwordError = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(36.dp))
                }

                Text(
                    text = if (isChangeMethodFlow) "Choose New Unlock Method" else "Protect your app",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SetupStepTransition"
            ) { step ->
                when (step) {
                    SetupStep.SELECT_METHOD -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Choose how you want to unlock the app.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // 1. Fingerprint / Biometric
                            val bioSubtitle = when (biometricStatus) {
                                BiometricHelper.BiometricStatus.AVAILABLE -> "Use your device biometric"
                                BiometricHelper.BiometricStatus.NONE_ENROLLED -> "No fingerprint enrolled in device settings"
                                BiometricHelper.BiometricStatus.NOT_SUPPORTED -> "Biometric hardware not supported"
                                BiometricHelper.BiometricStatus.UNAVAILABLE -> "Biometric currently unavailable"
                            }

                            MethodOptionCard(
                                icon = Icons.Default.Fingerprint,
                                title = "1. Fingerprint / Biometric",
                                subtitle = bioSubtitle,
                                enabled = isBiometricAvailable,
                                onClick = {
                                    if (isBiometricAvailable && fragmentActivity != null) {
                                        BiometricHelper.authenticate(
                                            activity = fragmentActivity,
                                            title = "Set up Biometric Lock",
                                            subtitle = "Verify your fingerprint to enable",
                                            negativeButtonText = "Cancel",
                                            onSuccess = {
                                                onCompleteSetup(UnlockMethod.BIOMETRIC, null)
                                            },
                                            onError = { _, _ -> },
                                            onFailed = { }
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. PIN
                            MethodOptionCard(
                                icon = Icons.Default.Pin,
                                title = "2. PIN",
                                subtitle = "Unlock with a secure PIN",
                                enabled = true,
                                onClick = {
                                    firstPin = ""
                                    confirmPin = ""
                                    pinError = null
                                    currentStep = SetupStep.SETUP_PIN_CREATE
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Pattern
                            MethodOptionCard(
                                icon = Icons.Default.Pattern,
                                title = "3. Pattern",
                                subtitle = "Unlock using a pattern",
                                enabled = true,
                                onClick = {
                                    firstPattern = emptyList()
                                    patternError = null
                                    isPatternError = false
                                    currentStep = SetupStep.SETUP_PATTERN_CREATE
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Password
                            MethodOptionCard(
                                icon = Icons.Default.Password,
                                title = "4. Password",
                                subtitle = "Unlock with a password",
                                enabled = true,
                                onClick = {
                                    firstPassword = ""
                                    confirmPassword = ""
                                    passwordError = null
                                    currentStep = SetupStep.SETUP_PASSWORD_CREATE
                                }
                            )
                        }
                    }

                    SetupStep.SETUP_PIN_CREATE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Create PIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter a 4-digit PIN for your app",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            PinKeypadView(
                                pinLength = 4,
                                currentPin = firstPin,
                                onPinChange = { newPin ->
                                    pinError = null
                                    firstPin = newPin
                                    if (newPin.length == 4) {
                                        currentStep = SetupStep.SETUP_PIN_CONFIRM
                                    }
                                },
                                errorMessage = pinError
                            )
                        }
                    }

                    SetupStep.SETUP_PIN_CONFIRM -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Confirm PIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Re-enter your 4-digit PIN to confirm",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            PinKeypadView(
                                pinLength = 4,
                                currentPin = confirmPin,
                                onPinChange = { newPin ->
                                    pinError = null
                                    confirmPin = newPin
                                    if (newPin.length == 4) {
                                        if (newPin == firstPin) {
                                            onCompleteSetup(UnlockMethod.PIN, newPin)
                                        } else {
                                            pinError = "PIN confirmation mismatch"
                                            confirmPin = ""
                                        }
                                    }
                                },
                                errorMessage = pinError
                            )
                        }
                    }

                    SetupStep.SETUP_PATTERN_CREATE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Create Pattern",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Connect at least 4 dots",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            PatternLockView(
                                onPatternCompleted = { patternNodes ->
                                    if (patternNodes.size < 4) {
                                        isPatternError = true
                                        patternError = "Connect at least 4 dots"
                                    } else {
                                        isPatternError = false
                                        patternError = null
                                        firstPattern = patternNodes
                                        currentStep = SetupStep.SETUP_PATTERN_CONFIRM
                                    }
                                },
                                isError = isPatternError,
                                errorMessage = patternError
                            )
                        }
                    }

                    SetupStep.SETUP_PATTERN_CONFIRM -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Confirm Pattern",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Draw your pattern again to confirm",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            PatternLockView(
                                onPatternCompleted = { patternNodes ->
                                    if (patternNodes == firstPattern) {
                                        isPatternError = false
                                        patternError = null
                                        val patternString = patternNodes.joinToString("-")
                                        onCompleteSetup(UnlockMethod.PATTERN, patternString)
                                    } else {
                                        isPatternError = true
                                        patternError = "Pattern confirmation mismatch"
                                    }
                                },
                                isError = isPatternError,
                                errorMessage = patternError
                            )
                        }
                    }

                    SetupStep.SETUP_PASSWORD_CREATE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Create Password",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter a strong password (at least 4 characters)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = firstPassword,
                                onValueChange = {
                                    firstPassword = it
                                    passwordError = null
                                },
                                placeholder = { Text("Enter password") },
                                trailingIcon = {
                                    val image = if (passVisible1) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passVisible1 = !passVisible1 }) {
                                        Icon(image, contentDescription = null)
                                    }
                                },
                                visualTransformation = if (passVisible1) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("create_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            AnimatedVisibility(visible = !passwordError.isNullOrBlank()) {
                                Text(
                                    text = passwordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (firstPassword.length < 4) {
                                        passwordError = "Password must be at least 4 characters"
                                    } else {
                                        passwordError = null
                                        currentStep = SetupStep.SETUP_PASSWORD_CONFIRM
                                    }
                                },
                                enabled = firstPassword.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Continue")
                            }
                        }
                    }

                    SetupStep.SETUP_PASSWORD_CONFIRM -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Confirm Password",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Re-enter your password to confirm",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    passwordError = null
                                },
                                placeholder = { Text("Re-enter password") },
                                trailingIcon = {
                                    val image = if (passVisible2) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passVisible2 = !passVisible2 }) {
                                        Icon(image, contentDescription = null)
                                    }
                                },
                                visualTransformation = if (passVisible2) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("confirm_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            AnimatedVisibility(visible = !passwordError.isNullOrBlank()) {
                                Text(
                                    text = passwordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (confirmPassword == firstPassword) {
                                        passwordError = null
                                        onCompleteSetup(UnlockMethod.PASSWORD, confirmPassword)
                                    } else {
                                        passwordError = "Password confirmation mismatch"
                                    }
                                },
                                enabled = confirmPassword.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Confirm & Save")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MethodOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
