package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private data class RationalePoint(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private enum class PermissionStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    NOTIFICATION(
        stepIndex = 0,
        title = "Real-Time Alerts & Reminders",
        subtitle = "Never miss important harvest schedules, farmer dues, or weather advisories.",
        icon = Icons.Default.Notifications,
        accentColor = Color(0xFFF59E0B)
    ),
    CAMERA(
        stepIndex = 1,
        title = "Instant Camera & QR Scanner",
        subtitle = "Scan booking receipts, log tree grafting photos, and document crop health.",
        icon = Icons.Default.CameraAlt,
        accentColor = Color(0xFF06B6D4)
    ),
    LOCATION(
        stepIndex = 2,
        title = "Orchard & Weather Geotagging",
        subtitle = "Pin accurate plot coordinates, microclimate conditions, and route directions.",
        icon = Icons.Default.LocationOn,
        accentColor = Color(0xFF10B981)
    ),
    SUMMARY(
        stepIndex = 3,
        title = "Setup Complete!",
        subtitle = "Your orchard workspace is configured and ready to use.",
        icon = Icons.Default.CheckCircle,
        accentColor = Color(0xFF10B981)
    )
}

/**
 * Dedicated post-login runtime permission onboarding screen.
 * Guides users sequentially through Notification, Camera, and Location permissions
 * with clear visual rationale, graceful handling of grants and denials, and
 * persistence of completion status.
 */
@Composable
fun PermissionOnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkipAll: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Track granted state for each permission
    var isNotificationGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var isCameraGranted by remember {
        mutableStateOf(checkCameraPermission(context))
    }
    var isLocationGranted by remember {
        mutableStateOf(checkLocationPermission(context))
    }

    // Track whether a permission was explicitly denied or skipped in this session
    var isNotificationDenied by remember { mutableStateOf(false) }
    var isCameraDenied by remember { mutableStateOf(false) }
    var isLocationDenied by remember { mutableStateOf(false) }

    // Notification Permission Launcher
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
        isNotificationDenied = !granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Camera Permission Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isCameraGranted = granted
        isCameraDenied = !granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Location Permission Launcher (Multiple: FINE & COARSE)
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        isLocationGranted = granted
        isLocationDenied = !granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val currentStep = when (currentStepIndex) {
        0 -> PermissionStep.NOTIFICATION
        1 -> PermissionStep.CAMERA
        2 -> PermissionStep.LOCATION
        else -> PermissionStep.SUMMARY
    }

    val progressFraction by animateFloatAsState(
        targetValue = when (currentStepIndex) {
            0 -> 0.25f
            1 -> 0.55f
            2 -> 0.85f
            else -> 1f
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "onboardingProgress"
    )

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF090D16),
                Color(0xFF020617)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            )
        )
    }

    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Baagbaan Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "BAAGBAAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (currentStepIndex < 3) "Step ${currentStepIndex + 1} of 3" else "All Done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }
                }

                if (currentStepIndex < 3) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipAll()
                        },
                        modifier = Modifier.testTag("permission_onboarding_skip_all")
                    ) {
                        Text(
                            text = "Skip All",
                            color = textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Animated Progress Indicator
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = currentStep.accentColor,
                trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            )

            // Step Indicator Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepStatusIndicator(
                    label = "Alerts",
                    step = 0,
                    currentStep = currentStepIndex,
                    isGranted = isNotificationGranted,
                    accentColor = PermissionStep.NOTIFICATION.accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StepStatusIndicator(
                    label = "Camera",
                    step = 1,
                    currentStep = currentStepIndex,
                    isGranted = isCameraGranted,
                    accentColor = PermissionStep.CAMERA.accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StepStatusIndicator(
                    label = "Location",
                    step = 2,
                    currentStep = currentStepIndex,
                    isGranted = isLocationGranted,
                    accentColor = PermissionStep.LOCATION.accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Scrollable Content Area with Animated Transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                animationSpec = tween(350),
                                initialOffsetX = { it }
                            ) + fadeIn(animationSpec = tween(350)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300),
                                        targetOffsetX = { -it / 3 }
                                    ) + fadeOut(animationSpec = tween(200))
                                )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(350),
                                initialOffsetX = { -it }
                            ) + fadeIn(animationSpec = tween(350)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300),
                                        targetOffsetX = { it / 3 }
                                    ) + fadeOut(animationSpec = tween(200))
                                )
                        }
                    },
                    label = "OnboardingStepContent"
                ) { stepIndex ->
                    when (stepIndex) {
                        0 -> NotificationStepContent(
                            isGranted = isNotificationGranted,
                            isDenied = isNotificationDenied,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark
                        )
                        1 -> CameraStepContent(
                            isGranted = isCameraGranted,
                            isDenied = isCameraDenied,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark
                        )
                        2 -> LocationStepContent(
                            isGranted = isLocationGranted,
                            isDenied = isLocationDenied,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark
                        )
                        else -> SummaryStepContent(
                            isNotificationGranted = isNotificationGranted,
                            isCameraGranted = isCameraGranted,
                            isLocationGranted = isLocationGranted,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Sticky Navigation Buttons
            BottomActionControls(
                stepIndex = currentStepIndex,
                isNotificationGranted = isNotificationGranted,
                isCameraGranted = isCameraGranted,
                isLocationGranted = isLocationGranted,
                isNotificationDenied = isNotificationDenied,
                isCameraDenied = isCameraDenied,
                isLocationDenied = isLocationDenied,
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        isNotificationGranted = true
                        currentStepIndex = 1
                    }
                },
                onRequestCamera = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                },
                onRequestLocation = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onNextStep = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (currentStepIndex < 3) {
                        currentStepIndex++
                    } else {
                        onOnboardingComplete()
                    }
                },
                onFinishOnboarding = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOnboardingComplete()
                },
                accentColor = currentStep.accentColor,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun StepStatusIndicator(
    label: String,
    step: Int,
    currentStep: Int,
    isGranted: Boolean,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isActive = currentStep == step
    val isDone = isGranted || currentStep > step

    val indicatorBg by animateColorAsState(
        targetValue = when {
            isGranted -> Color(0xFF10B981).copy(alpha = 0.2f)
            isActive -> accentColor.copy(alpha = 0.18f)
            else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        },
        label = "indicatorBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isGranted -> Color(0xFF10B981)
            isActive -> accentColor
            else -> Color.Transparent
        },
        label = "indicatorBorder"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(indicatorBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isGranted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) accentColor else if (isGranted) Color(0xFF10B981) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}

@Composable
private fun NotificationStepContent(
    isGranted: Boolean,
    isDenied: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Visual
        HeroPermissionIcon(
            icon = Icons.Default.Notifications,
            accentColor = Color(0xFFF59E0B),
            isGranted = isGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Stay Ahead with Real-Time Alerts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Baagbaan uses notifications to send high-priority reminders for your orchard operations.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Badge
        StatusPillBadge(
            isGranted = isGranted,
            isDenied = isDenied,
            permissionName = "Notification Alerting"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Rationale List Card
        RationaleCard(
            title = "What You'll Receive",
            points = listOf(
                RationalePoint(
                    icon = Icons.Default.Timer,
                    title = "Booking & Harvest Reminders",
                    description = "Get alerted when saplings are ready for pickup or when harvest delivery milestones approach."
                ),
                RationalePoint(
                    icon = Icons.Default.Spa,
                    title = "Payment & Due Notices",
                    description = "Receive timely reminders before farmer payment due dates to streamline collections."
                ),
                RationalePoint(
                    icon = Icons.Default.WbSunny,
                    title = "Seasonal Orchard Advisories",
                    description = "Timely prompts for winter pruning, bloom spray cycles, and pest protection."
                )
            ),
            cardBg = cardBg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = Color(0xFFF59E0B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyAssuranceCard(
            textSecondary = textSecondary,
            isDark = isDark
        )
    }
}

@Composable
private fun CameraStepContent(
    isGranted: Boolean,
    isDenied: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroPermissionIcon(
            icon = Icons.Default.CameraAlt,
            accentColor = Color(0xFF06B6D4),
            isGranted = isGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan Receipts & Document Crops",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enable camera access to scan QR voucher codes and capture orchard field documentation.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusPillBadge(
            isGranted = isGranted,
            isDenied = isDenied,
            permissionName = "Camera & Scanner"
        )

        Spacer(modifier = Modifier.height(20.dp))

        RationaleCard(
            title = "What Camera Enables",
            points = listOf(
                RationalePoint(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Instant Receipt QR Scanning",
                    description = "Scan printed or digital payment vouchers to load bookings without typing."
                ),
                RationalePoint(
                    icon = Icons.Default.Spa,
                    title = "Visual Tree & Graft Logs",
                    description = "Photograph apple graft unions, nursery rootstocks, and field samples directly."
                ),
                RationalePoint(
                    icon = Icons.Default.CheckCircle,
                    title = "Attendance Verification",
                    description = "Document worker presence and field check-ins with quick photo records."
                )
            ),
            cardBg = cardBg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = Color(0xFF06B6D4)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyAssuranceCard(
            textSecondary = textSecondary,
            isDark = isDark,
            customNote = "The camera is only activated when you tap a scan or capture button. No photos are taken in the background."
        )
    }
}

@Composable
private fun LocationStepContent(
    isGranted: Boolean,
    isDenied: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroPermissionIcon(
            icon = Icons.Default.LocationOn,
            accentColor = Color(0xFF10B981),
            isGranted = isGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Accurate Orchard Mapping",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Geotag your orchard parcels and receive localized weather forecasts for better farm management.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusPillBadge(
            isGranted = isGranted,
            isDenied = isDenied,
            permissionName = "Plot Location"
        )

        Spacer(modifier = Modifier.height(20.dp))

        RationaleCard(
            title = "What Location Enables",
            points = listOf(
                RationalePoint(
                    icon = Icons.Default.LocationOn,
                    title = "Geotag Farm Plots & Nurseries",
                    description = "Save precise GPS coordinates for your apple blocks, spray tanks, and storage."
                ),
                RationalePoint(
                    icon = Icons.Default.WbSunny,
                    title = "Localized Weather & Forecasts",
                    description = "Get temperature, rainfall probabilities, and frost warnings specific to your plot elevation."
                ),
                RationalePoint(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    title = "Field Visit Navigation",
                    description = "One-tap navigation to farmer orchards using Google Maps directions."
                )
            ),
            cardBg = cardBg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = Color(0xFF10B981)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyAssuranceCard(
            textSecondary = textSecondary,
            isDark = isDark,
            customNote = "Location coordinates are stored locally on your device and are never shared or sold."
        )
    }
}

@Composable
private fun SummaryStepContent(
    isNotificationGranted: Boolean,
    isCameraGranted: Boolean,
    isLocationGranted: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.25f),
                            Color(0xFF10B981).copy(alpha = 0.05f)
                        )
                    )
                )
                .border(2.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're All Set!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your orchard permissions have been configured. You can modify these anytime in device settings.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Permission Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                SummaryRowItem(
                    icon = Icons.Default.Notifications,
                    name = "Alerts & Notifications",
                    isGranted = isNotificationGranted,
                    accentColor = Color(0xFFF59E0B),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                SummaryRowItem(
                    icon = Icons.Default.CameraAlt,
                    name = "Camera & QR Scanner",
                    isGranted = isCameraGranted,
                    accentColor = Color(0xFF06B6D4),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                SummaryRowItem(
                    icon = Icons.Default.LocationOn,
                    name = "Location & Microclimate",
                    isGranted = isLocationGranted,
                    accentColor = Color(0xFF10B981),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyAssuranceCard(
            textSecondary = textSecondary,
            isDark = isDark,
            customNote = "Non-critical permissions that were declined can be enabled at any time from your device's Application Settings."
        )
    }
}

@Composable
private fun SummaryRowItem(
    icon: ImageVector,
    name: String,
    isGranted: Boolean,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF64748B).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Optional",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun HeroPermissionIcon(
    icon: ImageVector,
    accentColor: Color,
    isGranted: Boolean
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f),
                        accentColor.copy(alpha = 0.05f)
                    )
                )
            )
            .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun StatusPillBadge(
    isGranted: Boolean,
    isDenied: Boolean,
    permissionName: String
) {
    val bg = when {
        isGranted -> Color(0xFF10B981).copy(alpha = 0.15f)
        isDenied -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        else -> Color(0xFF3B82F6).copy(alpha = 0.12f)
    }
    val textCol = when {
        isGranted -> Color(0xFF10B981)
        isDenied -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }
    val icon = when {
        isGranted -> Icons.Default.Check
        isDenied -> Icons.Default.Close
        else -> Icons.Default.Security
    }
    val text = when {
        isGranted -> "✓ $permissionName Granted"
        isDenied -> "Declined (App functions adapt gracefully)"
        else -> "Recommended for full functionality"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textCol,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textCol
        )
    }
}

@Composable
private fun RationaleCard(
    title: String,
    points: List<RationalePoint>,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            points.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = point.icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = point.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = point.description,
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAssuranceCard(
    textSecondary: Color,
    isDark: Boolean,
    customNote: String = "Your privacy is protected. Baagbaan stores records locally on device and only connects to sync services you configure."
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = customNote,
            fontSize = 11.sp,
            color = textSecondary,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun BottomActionControls(
    stepIndex: Int,
    isNotificationGranted: Boolean,
    isCameraGranted: Boolean,
    isLocationGranted: Boolean,
    isNotificationDenied: Boolean,
    isCameraDenied: Boolean,
    isLocationDenied: Boolean,
    onRequestNotification: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onNextStep: () -> Unit,
    onFinishOnboarding: () -> Unit,
    accentColor: Color,
    isDark: Boolean
) {
    val isCurrentStepGranted = when (stepIndex) {
        0 -> isNotificationGranted
        1 -> isCameraGranted
        2 -> isLocationGranted
        else -> true
    }

    val isCurrentStepDenied = when (stepIndex) {
        0 -> isNotificationDenied
        1 -> isCameraDenied
        2 -> isLocationDenied
        else -> false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stepIndex < 3) {
            if (!isCurrentStepGranted && !isCurrentStepDenied) {
                // Primary button to request permission
                Button(
                    onClick = {
                        when (stepIndex) {
                            0 -> onRequestNotification()
                            1 -> onRequestCamera()
                            2 -> onRequestLocation()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("permission_grant_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = when (stepIndex) {
                            0 -> "Allow Notifications"
                            1 -> "Allow Camera Access"
                            2 -> "Allow Location Access"
                            else -> "Allow"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary button to skip current permission
                OutlinedButton(
                    onClick = onNextStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("permission_skip_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                ) {
                    Text(
                        text = "Not Now (Skip)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // If granted or denied, show Continue button
                Button(
                    onClick = onNextStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("permission_continue_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentStepGranted) Color(0xFF10B981) else accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (stepIndex == 2) "View Summary" else "Continue to Next Step",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isCurrentStepDenied) {
                    Text(
                        text = "You can always grant this permission later from Settings.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        } else {
            // Final step: Finish button
            Button(
                onClick = onFinishOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("permission_onboarding_finish_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter Dashboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
