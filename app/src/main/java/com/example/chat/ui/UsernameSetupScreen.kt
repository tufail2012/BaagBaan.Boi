package com.example.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.data.ChatRepository
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode

@Composable
fun UsernameSetupScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val isSubmitting by viewModel.isSubmittingUsername.collectAsState()
    val serverError by viewModel.usernameError.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    val cleanedInput = remember(usernameInput) { usernameInput.trim().lowercase() }
    val isValid = remember(cleanedInput) { ChatRepository.isValidUsername(cleanedInput) }

    val bgBrush = remember(isDark, isAmoled, accentColor) {
        val top = if (isAmoled) Color.Black else if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        val bottom = if (isAmoled) Color.Black else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        Brush.verticalGradient(listOf(top, bottom))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .imePadding(),
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top row with back button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("username_setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose Username",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar Icon badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Pick a unique handle",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will be your identity in Baagbaan Boi Chat. Other members can find and connect with you using this username.",
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Text field
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { input ->
                        // Automatically lowercase and filter out invalid characters
                        val filtered = input.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
                        if (filtered.length <= 20) {
                            usernameInput = filtered
                            viewModel.clearUsernameError()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input_field"),
                    singleLine = true,
                    placeholder = {
                        Text("your_username", color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                    },
                    leadingIcon = {
                        Text(
                            text = "@",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accentColor,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    trailingIcon = {
                        if (cleanedInput.isNotEmpty()) {
                            Icon(
                                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isValid) Color(0xFF22C55E) else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        focusedContainerColor = if (isDark) Color(0x331E293B) else Color(0x66FFFFFF),
                        unfocusedContainerColor = if (isDark) Color(0x221E293B) else Color(0x44FFFFFF),
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid && !isSubmitting) {
                                viewModel.registerUsername(cleanedInput)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Rules / Error explanation
                AnimatedVisibility(
                    visible = serverError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = serverError ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (serverError == null) {
                    Text(
                        text = "Rules: 3–20 characters, lowercase letters, numbers, and underscores only.",
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Bottom CTA
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        viewModel.registerUsername(cleanedInput)
                    }
                },
                enabled = isValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_username_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.35f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue to Chat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
