package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Reusable Voice Search Icon Button that integrates Android's native Speech Recognition.
 *
 * Features:
 * - Detects microphone permissions dynamically and requests if required.
 * - Launches native Speech Recognizer with localized prompts.
 * - Extracts spoken text and passes it directly to [onQueryChange].
 * - Displays active listening pulse / glowing indicator when active.
 * - Preserves existing typed text if cancelled or unsupported.
 * - Form-fitted for trailing icons inside search fields.
 */
@Composable
fun VoiceSearchIconButton(
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isAppInDarkMode(),
    buttonSize: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    testTag: String = "voice_search_button"
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    // Result launcher for Speech Recognition
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onQueryChange(spokenText.trim())
            }
        }
    }

    // Function to launch the native speech recognizer
    val launchSpeechRecognition = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search records...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            isListening = true
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            isListening = false
            Toast.makeText(context, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isListening = false
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechRecognition()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice search", Toast.LENGTH_SHORT).show()
        }
    }

    // Listening pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceSearchPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(buttonSize),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Visual listening halo
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha))
            )
        }

        IconButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    launchSpeechRecognition()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier
                .size(buttonSize)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening for voice search..." else "Voice Search",
                tint = if (isListening) accentColor else accentColor.copy(alpha = if (isDark) 0.92f else 0.85f),
                modifier = Modifier
                    .size(iconSize)
                    .scale(if (isListening) 1.15f else 1.0f)
            )
        }
    }
}
