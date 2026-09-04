package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.UserBooking
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.BrandedPullToRefreshBox
import com.example.ui.AppThemeMode
import com.example.ui.CropViewModel
import com.example.ui.GardenPlanningViewModel
import com.example.ui.UserDashboardViewModel
import com.example.ui.theme.getAppDimBackgroundBrush
import com.example.ui.theme.getSectionAccentColor
import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * CompositionLocal providing HazeState across all etched glass surfaces on the dashboard.
 */
val LocalDashboardHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Inscribed / Etched Glass Typography Style.
 * Renders text with sharp contrast, refined tracking, and physical specular drop highlights
 * or luminous edge glows, creating the authentic optical illusion that text is physically
 * written, laser-etched, or printed onto the glass surface.
 */
@Composable
fun glassEtchedTextStyle(
    isDark: Boolean,
    color: Color? = null,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    isAccent: Boolean = false,
    accentColor: Color = Color.Unspecified,
    letterSpacing: TextUnit = 0.25.sp,
    isProminent: Boolean = false,
    isSecondary: Boolean = false
): TextStyle {
    val resolvedColor = color ?: if (isDark) {
        when {
            isAccent && accentColor != Color.Unspecified -> accentColor
            isSecondary -> Color(0xFFCBD5E1)
            else -> Color(0xFFFFFFFF)
        }
    } else {
        when {
            isAccent && accentColor != Color.Unspecified -> accentColor
            isSecondary -> Color(0xFF334155)
            else -> Color(0xFF0F172A)
        }
    }

    val shadow = if (isDark) {
        if (isAccent && accentColor != Color.Unspecified) {
            Shadow(
                color = accentColor.copy(alpha = if (isProminent) 0.70f else 0.45f),
                offset = Offset(0f, 0f),
                blurRadius = if (isProminent) 8f else 4f
            )
        } else if (isSecondary) {
            Shadow(
                color = Color.White.copy(alpha = 0.20f),
                offset = Offset(0f, 0.5f),
                blurRadius = 2f
            )
        } else {
            Shadow(
                color = Color.White.copy(alpha = if (isProminent) 0.55f else 0.35f),
                offset = Offset(0f, 0.5f),
                blurRadius = if (isProminent) 6f else 3f
            )
        }
    } else {
        if (isAccent && accentColor != Color.Unspecified) {
            Shadow(
                color = Color.White.copy(alpha = 0.95f),
                offset = Offset(0f, 1.2f),
                blurRadius = 1.5f
            )
        } else if (isSecondary) {
            Shadow(
                color = Color.White.copy(alpha = 0.90f),
                offset = Offset(0f, 1f),
                blurRadius = 1.2f
            )
        } else {
            // Bright white specular relief under dark text: physically mimics light catching
            // the engraved bevel or raised enamel stroke on glass
            Shadow(
                color = Color.White.copy(alpha = 0.98f),
                offset = Offset(0f, 1.2f),
                blurRadius = 1.5f
            )
        }
    }

    return TextStyle(
        color = resolvedColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        shadow = shadow
    )
}

/**
 * Realistic Multi-Layer Frosted & Etched Glass Modifier.
 * Transforms components into physical glass slabs featuring:
 * - Floating depth shadows with subtle ambient reflection
 * - Frosted, semi-translucent crystalline substrate gradient
 * - Dual-layer beveled edge rim reflecting ambient light
 * - Top-edge specular glint beam and 35-degree diagonal refraction sheen
 */
@Composable
fun Modifier.etchedGlassSurface(
    isDark: Boolean,
    isAmoled: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 3.dp,
    borderWidth: Dp = 1.dp,
    showTopGlint: Boolean = true,
    hazeState: HazeState? = LocalDashboardHazeState.current
): Modifier {
    val spotShadowColor = if (isAmoled) {
        Color.Black.copy(alpha = 0.40f)
    } else if (isDark) {
        Color.Black.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }
    val ambientShadowColor = if (isDark || isAmoled) {
        accentColor.copy(alpha = 0.08f)
    } else {
        accentColor.copy(alpha = 0.04f)
    }

    // Refined frosted-glass HazeStyle: 24.dp blur for true translucent Gaussian dispersion
    val hazeStyle = HazeStyle(
        backgroundColor = if (isAmoled) {
            Color.Black.copy(alpha = 0.22f)
        } else if (isDark) {
            Color(0xFF14121B).copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.15f)
        },
        blurRadius = 24.dp,
        tints = listOf(
            HazeTint(
                color = if (isDark || isAmoled) {
                    accentColor.copy(alpha = 0.04f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                }
            )
        ),
        noiseFactor = 0.02f
    )

    // Translucent glass body wash: highly transparent, crisp specular upper edge, letting background diffuse through
    val glassBaseBrush = when {
        isAmoled -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color(0xFF100E14).copy(alpha = 0.16f),
                    accentColor.copy(alpha = 0.03f),
                    Color(0xFF000000).copy(alpha = 0.30f)
                )
            )
        }
        isDark -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f),
                    Color(0xFF221F2B).copy(alpha = 0.20f),
                    accentColor.copy(alpha = 0.04f),
                    Color(0xFF14121A).copy(alpha = 0.26f)
                )
            )
        }
        else -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f), // Subtle specular rim along top edge
                    Color.White.copy(alpha = 0.12f), // Crystal-clear translucent glass body
                    accentColor.copy(alpha = 0.03f), // Very subtle luminous accent sheen
                    Color.White.copy(alpha = 0.18f)  // Soft ambient bottom reflection
                )
            )
        }
    }

    // Specular reflective glass rim border: subtle and refined
    val borderBrush = Brush.linearGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.38f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.22f), // Reflective edge sheen
                Color.White.copy(alpha = 0.10f), // Clear lateral glass sides
                Color.White.copy(alpha = 0.05f)  // Soft specular bottom return
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.75f), // Crisp specular reflection along top edge
                Color.White.copy(alpha = 0.35f), // Clear lateral glass sides
                accentColor.copy(alpha = 0.12f), // Ambient edge reflection
                Color.White.copy(alpha = 0.25f)  // Soft specular bottom return
            )
        },
        start = Offset.Zero,
        end = Offset.Infinite
    )

    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    spotColor = spotShadowColor,
                    ambientColor = ambientShadowColor
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .background(brush = glassBaseBrush, shape = shape)
        .drawWithContent {
            drawContent()
            val w = size.width
            val margin = 12.dp.toPx()
            val highlightH = 1.dp.toPx()

            // Ultra-subtle analog micro-grain texture for tactile frosted glass feel
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.02f else 0.025f
            )

            if (showTopGlint) {
                // Crisp top specular glint beam along upper rim
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.35f else 0.50f),
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.65f else 0.85f),
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.35f else 0.50f),
                            Color.Transparent
                        ),
                        startX = margin,
                        endX = w - margin
                    ),
                    topLeft = Offset(margin, 1.dp.toPx()),
                    size = Size(w - (margin * 2), highlightH)
                )
                // Subtle top-left specular corner sheen
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.08f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.35f
                    )
                )
            }
        }
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Colorful ambient backdrop canvas for the Dashboard screen.
 * Renders luminous, harmonious color orbs and radiant gradient blooms behind the glass panels.
 * When captured by HazeState via hazeSource, these shapes diffuse naturally through the frosted
 * glass cards with optical light bleeding and depth, while maintaining high contrast in Light,
 * Dark, and AMOLED modes.
 */
@Composable
private fun DashboardAmbientBackdrop(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = com.example.ui.theme.LocalAppPalette.current
    val (effectivePrimary, secondaryColor, tertiaryColor) = remember(palette, accentColor, isDark, isAmoled) {
        if (!palette.isTwoColor && palette.id != "solid_active") {
            Triple(
                palette.getPrimary(isDark, isAmoled),
                palette.getSecondary(isDark, isAmoled),
                palette.getTertiary(isDark, isAmoled)
            )
        } else {
            val r = (accentColor.red * 255f).toInt().coerceIn(0, 255)
            val g = (accentColor.green * 255f).toInt().coerceIn(0, 255)
            val b = (accentColor.blue * 255f).toInt().coerceIn(0, 255)
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]
            val sec = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 42f) % 360f, if (isDark) 0.65f else 0.55f, 0.92f)))
            val tert = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue - 38f + 360f) % 360f, if (isDark) 0.60f else 0.50f, 0.88f)))
            Triple(accentColor, sec, tert)
        }
    }

    // Alpha scales tuned for natural diffusion through 24.dp blur without blowing out contrast
    val primaryAlpha = if (isAmoled) 0.26f else if (isDark) 0.34f else 0.42f
    val secondaryAlpha = if (isAmoled) 0.22f else if (isDark) 0.28f else 0.36f
    val tertiaryAlpha = if (isAmoled) 0.18f else if (isDark) 0.24f else 0.30f

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Top-right luminous accent orb (illuminating Header & Account banner)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(effectivePrimary.copy(alpha = primaryAlpha), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.80f
            ),
            center = Offset(w * 0.85f, h * 0.12f),
            radius = w * 0.80f
        )

        // 2. Upper-left harmonic bloom (illuminating Financial Breakdown card)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = secondaryAlpha), Color.Transparent),
                center = Offset(w * 0.10f, h * 0.28f),
                radius = w * 0.70f
            ),
            center = Offset(w * 0.10f, h * 0.28f),
            radius = w * 0.70f
        )

        // 2b. Organic botanical petal bloom on left edge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(effectivePrimary.copy(alpha = primaryAlpha * 0.65f), Color.Transparent),
                center = Offset(w * 0.05f, h * 0.42f),
                radius = w * 0.55f
            ),
            center = Offset(w * 0.05f, h * 0.42f),
            radius = w * 0.55f
        )

        // 3. Mid-right organic diffusion bloom (behind Operational Summaries)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryColor.copy(alpha = tertiaryAlpha), Color.Transparent),
                center = Offset(w * 0.92f, h * 0.54f),
                radius = w * 0.72f
            ),
            center = Offset(w * 0.92f, h * 0.54f),
            radius = w * 0.72f
        )

        // 4. Lower-left radiant bloom (behind lower module cards & recent logs)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(effectivePrimary.copy(alpha = primaryAlpha * 0.85f), Color.Transparent),
                center = Offset(w * 0.12f, h * 0.76f),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.12f, h * 0.76f),
            radius = w * 0.75f
        )

        // 5. Bottom-right subtle return pool
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = secondaryAlpha * 0.80f), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.95f),
                radius = w * 0.65f
            ),
            center = Offset(w * 0.82f, h * 0.95f),
            radius = w * 0.65f
        )
    }
}

/**
 * Main AgriCrop Executive Operations Dashboard.
 * Designed with a realistic frosted and etched glass visual identity, featuring
 * bright, sharp, luminous lettering that appears physically inscribed into the glass.
 */
@Composable
fun AgriDashboardScreen(
    viewModel: CropViewModel,
    userDashboardViewModel: UserDashboardViewModel,
    gardenPlanningViewModel: GardenPlanningViewModel,
    currentUserEmail: String? = null,
    onBack: () -> Unit,
    onNavigateToCategory: ((String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val fallbackHazeState = remember { HazeState() }
    val effectiveHazeState = hazeState ?: fallbackHazeState
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val themeMode by viewModel.themeMode.collectAsState()
    val isAmoled = themeMode == AppThemeMode.AMOLED || isAppInAmoledMode()

    val dashboardAccent = MaterialTheme.colorScheme.primary

    BackHandler {
        onBack()
    }

    // Data streams
    val allRecords by viewModel.allRecords.collectAsState()
    val gardenEntries by gardenPlanningViewModel.allEntries.collectAsState()
    val rawBookings by userDashboardViewModel.rawBookings.collectAsState()

    var currentUser by remember {
        mutableStateOf(
            try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Throwable) {
                null
            }
        )
    }

    DisposableEffect(Unit) {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
        val listener = FirebaseAuth.AuthStateListener { a ->
            currentUser = a.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    var selectedFilterTab by remember { mutableStateOf("All") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(allRecords, gardenEntries) {
        if (allRecords.isNotEmpty() || gardenEntries.isNotEmpty()) {
            isInitialLoading = false
        } else {
            delay(300)
            isInitialLoading = false
        }
    }

    val filteredLogRecords = remember(allRecords, rawBookings, selectedFilterTab) {
        when (selectedFilterTab) {
            "Pending" -> allRecords.filter { !it.isPaymentCleared() }
            "Paid" -> allRecords.filter { it.isPaymentCleared() }
            "Bookings" -> emptyList()
            else -> allRecords
        }
    }

    // Computations for CropRecords
    val importedRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Imported", ignoreCase = true) }
    }
    val localRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Local Plants", ignoreCase = true) }
    }
    val rootstockRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Rootstocks", ignoreCase = true) || it.serviceType.contains("Rootstock", ignoreCase = true) }
    }
    val siteVisitRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Site Visit", ignoreCase = true) }
    }
    val pruningRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Pruning", ignoreCase = true) }
    }

    // Garden Planning Financial Metrics
    val gardenRevenue = remember(gardenEntries) { gardenEntries.sumOf { it.totalCost } }
    val gardenPaid = remember(gardenEntries) { gardenEntries.sumOf { it.amountPaid } }
    val gardenRemaining = remember(gardenEntries) { gardenEntries.sumOf { it.remainingBalance } }

    val gardenFullyPaidCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Fully Paid" } }
    val gardenAdvancePaidCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Advance Paid" } }
    val gardenPendingCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Pending" || it.paymentStatus == "Unpaid" } }

    // Aggregate Financial Metrics
    val totalRevenue = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.calculateTotalAmount() } + gardenRevenue
    }
    val totalPaid = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.amountPaid } + gardenPaid
    }
    val totalRemaining = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.calculateRemainingBalance() } + gardenRemaining
    }

    val fullyPaidCount = remember(allRecords, gardenEntries) {
        allRecords.count { it.isPaymentCleared() } + gardenFullyPaidCount
    }
    val advancePaidCount = remember(allRecords, gardenEntries) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid > 0 } + gardenAdvancePaidCount
    }
    val pendingCount = remember(allRecords, gardenEntries) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid <= 0 } + gardenPendingCount
    }
    val totalRecordsCount = remember(allRecords, gardenEntries) {
        allRecords.size + gardenEntries.size
    }

    val paidRatio = if (totalRevenue > 0) (totalPaid / totalRevenue).toFloat().coerceIn(0f, 1f) else 0f

    val dashboardBgBrush = remember(isDark, isAmoled, dashboardAccent) {
        getAppDimBackgroundBrush(dashboardAccent, isDark = isDark, isAmoled = isAmoled)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Colorful background canvas with shapes & diffusion blooms captured by hazeSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = effectiveHazeState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dashboardBgBrush)
            )

            DashboardAmbientBackdrop(
                accentColor = dashboardAccent,
                isDark = isDark,
                isAmoled = isAmoled
            )
        }

        // 2. Foreground Glass Panels & Inscribed Content
        CompositionLocalProvider(LocalDashboardHazeState provides effectiveHazeState) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Floating Real Glass Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .etchedGlassSurface(
                        isDark = isDark,
                        isAmoled = isAmoled,
                        accentColor = dashboardAccent,
                        shape = RoundedCornerShape(percent = 50),
                        elevation = 3.dp
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Glass button well for Back button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.45f))
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.70f), CircleShape)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = dashboardAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Glass Emblem Pod
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(dashboardAccent.copy(alpha = 0.16f))
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.70f), dashboardAccent.copy(alpha = 0.40f))), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard Icon",
                                tint = dashboardAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AgriCrop Operations Dashboard",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    isProminent = true
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Comprehensive Operations & Financial Overview",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    isSecondary = true
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onNavigateToSettings != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.45f))
                                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.70f), CircleShape)
                                    .clickable { onNavigateToSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings & Security",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.45f))
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.70f), CircleShape)
                            .clickable { userDashboardViewModel.refreshUser() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data",
                                tint = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            BrandedPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (isRefreshing) return@BrandedPullToRefreshBox
                    isRefreshing = true
                    coroutineScope.launch {
                        try {
                            userDashboardViewModel.refreshUser()
                            delay(600)
                        } catch (_: Exception) {
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    // 1. Account & System Banner Card
                    item {
                        AccountBannerCard(
                            currentUser = currentUser,
                            totalEntriesCount = totalRecordsCount,
                            totalVolume = totalRevenue,
                            accentColor = dashboardAccent,
                            isDark = isDark,
                            isAmoled = isAmoled
                        )
                    }

                    // 2. Financial Overview & Payment Breakdown Card
                    item {
                        FinancialBreakdownCard(
                            totalRevenue = totalRevenue,
                            totalPaid = totalPaid,
                            totalRemaining = totalRemaining,
                            paidRatio = paidRatio,
                            fullyPaidCount = fullyPaidCount,
                            advancePaidCount = advancePaidCount,
                            pendingCount = pendingCount,
                            totalRecordsCount = totalRecordsCount,
                            accentColor = dashboardAccent,
                            isDark = isDark,
                            isAmoled = isAmoled
                        )
                    }

                    // 3. Operational Category Summaries Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(dashboardAccent.copy(alpha = 0.16f))
                                            .border(1.dp, dashboardAccent.copy(alpha = 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assessment,
                                            contentDescription = null,
                                            tint = dashboardAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Operational Category Summaries",
                                        style = glassEtchedTextStyle(
                                            isDark = isDark,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            isProminent = true
                                        )
                                    )
                                }

                                // Frosted glass count pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.40f))
                                        .border(
                                            1.dp,
                                            if (isDark) SolidColor(dashboardAccent.copy(alpha = 0.40f))
                                            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.80f), dashboardAccent.copy(alpha = 0.40f))),
                                            RoundedCornerShape(percent = 50)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "6 Modules",
                                        style = glassEtchedTextStyle(
                                            isDark = isDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            isAccent = true,
                                            accentColor = dashboardAccent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Module Category Cards
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Imported Plants
                            CategorySummaryCard(
                                title = "Imported Plants",
                                icon = Icons.Default.Spa,
                                badgeColor = Color(0xFF1E88E5),
                                recordsCount = importedRecords.size,
                                totalQuantity = importedRecords.sumOf { it.quantity },
                                unitLabel = "Plants",
                                totalValue = importedRecords.sumOf { it.calculateTotalAmount() },
                                remainingBalance = importedRecords.sumOf { it.calculateRemainingBalance() },
                                paidCount = importedRecords.count { it.isPaymentCleared() },
                                onClick = { onNavigateToCategory?.invoke("Imported") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )

                            // Local Plants
                            CategorySummaryCard(
                                title = "Local Plants",
                                icon = Icons.Outlined.LocalFlorist,
                                badgeColor = Color(0xFF43A047),
                                recordsCount = localRecords.size,
                                totalQuantity = localRecords.sumOf { it.quantity },
                                unitLabel = "Plants",
                                totalValue = localRecords.sumOf { it.calculateTotalAmount() },
                                remainingBalance = localRecords.sumOf { it.calculateRemainingBalance() },
                                paidCount = localRecords.count { it.isPaymentCleared() },
                                onClick = { onNavigateToCategory?.invoke("Local Plants") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )

                            // Rootstock
                            CategorySummaryCard(
                                title = "Rootstock",
                                icon = Icons.Default.TrendingUp,
                                badgeColor = Color(0xFF8E24AA),
                                recordsCount = rootstockRecords.size,
                                totalQuantity = rootstockRecords.sumOf { it.quantity },
                                unitLabel = "Rootstocks",
                                totalValue = rootstockRecords.sumOf { it.calculateTotalAmount() },
                                remainingBalance = rootstockRecords.sumOf { it.calculateRemainingBalance() },
                                paidCount = rootstockRecords.count { it.isPaymentCleared() },
                                onClick = { onNavigateToCategory?.invoke("Rootstock") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )

                            // Site Visits
                            CategorySummaryCard(
                                title = "Site Visits",
                                icon = Icons.Default.EventAvailable,
                                badgeColor = Color(0xFFFB8C00),
                                recordsCount = siteVisitRecords.size,
                                totalQuantity = siteVisitRecords.sumOf { it.landAreaAcres.toInt() },
                                unitLabel = "Acres Visited",
                                totalValue = siteVisitRecords.sumOf { it.calculateTotalAmount() },
                                remainingBalance = siteVisitRecords.sumOf { it.calculateRemainingBalance() },
                                paidCount = siteVisitRecords.count { it.isPaymentCleared() },
                                onClick = { onNavigateToCategory?.invoke("Site Visit") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )

                            // Pruning Records
                            CategorySummaryCard(
                                title = "Pruning Records",
                                icon = Icons.Default.ContentCut,
                                badgeColor = Color(0xFFD32F2F),
                                recordsCount = pruningRecords.size,
                                totalQuantity = pruningRecords.sumOf { it.quantity },
                                unitLabel = "Trees / Acres",
                                totalValue = pruningRecords.sumOf { it.calculateTotalAmount() },
                                remainingBalance = pruningRecords.sumOf { it.calculateRemainingBalance() },
                                paidCount = pruningRecords.count { it.isPaymentCleared() },
                                onClick = { onNavigateToCategory?.invoke("Pruning") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )

                            // Garden Planning
                            CategorySummaryCard(
                                title = "Garden Planning",
                                icon = Icons.Default.Park,
                                badgeColor = Color(0xFF00897B),
                                recordsCount = gardenEntries.size,
                                totalQuantity = gardenEntries.sumOf { (it.totalKanalArea * it.plantsPerKanal).toInt() },
                                unitLabel = "Plants (Calculated)",
                                totalValue = gardenRevenue,
                                remainingBalance = gardenRemaining,
                                paidCount = gardenFullyPaidCount,
                                onClick = { onNavigateToCategory?.invoke("Garden Planning") },
                                isDark = isDark,
                                isAmoled = isAmoled
                            )
                        }
                    }

                    // 5. Varieties & Inventory Snapshot
                    item {
                        VarietyDistributionCard(
                            allRecords = allRecords,
                            accentColor = dashboardAccent,
                            isDark = isDark,
                            isAmoled = isAmoled
                        )
                    }

                    // 6. Filterable Activity Log / Recent Records
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent Operations Log",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    isProminent = true
                                )
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val tabs = listOf("All", "Pending", "Paid", "Garden Planning")
                                items(tabs) { tab ->
                                    val isSelected = selectedFilterTab == tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(percent = 50))
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    dashboardAccent.copy(alpha = if (isDark) 0.45f else 0.90f),
                                                                    dashboardAccent.copy(alpha = if (isDark) 0.30f else 0.80f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            1.2.dp,
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    Color.White.copy(alpha = 0.90f),
                                                                    dashboardAccent,
                                                                    Color.White.copy(alpha = 0.40f)
                                                                )
                                                            ),
                                                            RoundedCornerShape(percent = 50)
                                                        )
                                                } else {
                                                    Modifier
                                                        .background(
                                                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.38f)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.65f),
                                                            RoundedCornerShape(percent = 50)
                                                        )
                                                }
                                            )
                                            .clickable { selectedFilterTab = tab }
                                            .padding(horizontal = 13.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab,
                                            style = glassEtchedTextStyle(
                                                isDark = isDark,
                                                color = if (isSelected) {
                                                    if (isDark || isAmoled) Color.White else Color.Black
                                                } else null,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                isSecondary = !isSelected,
                                                isProminent = isSelected
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedFilterTab == "Garden Planning") {
                        if (gardenEntries.isEmpty()) {
                            if (isInitialLoading) {
                                items(3) {
                                    SkeletonCard(isDark = isDark, lineCount = 3, hasActionRow = false)
                                }
                            } else {
                                item {
                                    EmptyStateCard(message = "No Garden Planning entries registered yet.", isDark = isDark, isAmoled = isAmoled)
                                }
                            }
                        } else {
                            val displayGardenEntries = gardenEntries.take(8)
                            items(displayGardenEntries) { entry ->
                                GardenLogItemCard(entry = entry, isDark = isDark, isAmoled = isAmoled)
                            }
                        }
                    } else {
                        if (filteredLogRecords.isEmpty()) {
                            if (isInitialLoading) {
                                items(3) {
                                    SkeletonCard(isDark = isDark, lineCount = 3, hasActionRow = false)
                                }
                            } else {
                                item {
                                    EmptyStateCard(message = "No records found matching current filter.", isDark = isDark, isAmoled = isAmoled)
                                }
                            }
                        } else {
                            val displayRecords = filteredLogRecords.take(10)
                            items(displayRecords) { record ->
                                RecordLogItemCard(record = record, isDark = isDark, isAmoled = isAmoled)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
}

@Composable
private fun AccountBannerCard(
    currentUser: FirebaseUser?,
    totalEntriesCount: Int,
    totalVolume: Double,
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val primaryText = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.takeIf { it.isNotBlank() }
        ?: "Guest Operator"

    val secondaryText = if (!currentUser?.displayName.isNullOrBlank() && !currentUser?.email.isNullOrBlank()) {
        currentUser!!.email!!
    } else if (currentUser != null) {
        "AgriCrop Cloud Sync Active"
    } else {
        "Local Operator Session"
    }

    val photoUrl = currentUser?.photoUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = accentColor,
                shape = RoundedCornerShape(20.dp),
                elevation = 3.dp
            )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Frosted Glass Avatar Ring
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    accentColor.copy(alpha = 0.60f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Account",
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = primaryText,
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                isProminent = true
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Inscribed glass badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (currentUser != null) Color(0xFF2E7D32).copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                                )
                                .border(
                                    1.dp,
                                    if (currentUser != null) Color(0xFF2E7D32).copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentUser != null) "ACTIVE" else "GUEST",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    color = if (currentUser != null) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = secondaryText,
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            isSecondary = true
                        )
                    )
                }
            }

            // Total Entries Glass Display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalEntriesCount",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isAccent = true,
                        accentColor = accentColor,
                        isProminent = true
                    )
                )
                Text(
                    text = "Total Entries",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        isSecondary = true
                    )
                )
            }
        }
    }
}

@Composable
private fun FinancialBreakdownCard(
    totalRevenue: Double,
    totalPaid: Double,
    totalRemaining: Double,
    paidRatio: Float,
    fullyPaidCount: Int,
    advancePaidCount: Int,
    pendingCount: Int,
    totalRecordsCount: Int,
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = accentColor,
                shape = RoundedCornerShape(20.dp),
                elevation = 3.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Financial & Payment Status",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isProminent = true
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Illuminated Collection Ratio Glass Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(paidRatio * 100).toInt()}% Collected",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isAccent = true,
                            accentColor = accentColor,
                            isProminent = true
                        )
                    )
                }
            }

            // Summary 3-Boxes in Sculpted Glass Pods
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Volume
                FinancialMetricBox(
                    label = "Total Volume",
                    targetValue = totalRevenue,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.TrendingUp,
                    accentColor = accentColor,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                // Paid / Received
                FinancialMetricBox(
                    label = "Amount Paid",
                    targetValue = totalPaid,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF10B981),
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                // Remaining Balance
                FinancialMetricBox(
                    label = "Remaining",
                    targetValue = totalRemaining,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.ReceiptLong,
                    accentColor = if (totalRemaining > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recessed Glass Groove Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Payment Collection Progress",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            isSecondary = true
                        )
                    )
                    Text(
                        text = "${(paidRatio * 100).toInt()}% Paid",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isAccent = true,
                            accentColor = accentColor
                        )
                    )
                }

                // Sculpted Glass Groove Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFFCBD5E1).copy(alpha = 0.40f))
                        .border(
                            0.8.dp,
                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f),
                            RoundedCornerShape(5.dp)
                        )
                ) {
                    if (paidRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(paidRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF059669),
                                            Color(0xFF10B981),
                                            Color(0xFF34D399)
                                        )
                                    )
                                )
                                .drawWithContent {
                                    drawContent()
                                    // Top specular glint on liquid bar
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        ),
                                        size = Size(size.width, size.height * 0.4f)
                                    )
                                }
                        )
                    }
                }
            }

            // Glass Seam Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Payment Status Counts Row in Frosted Glass Pods
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusBadgeCount(
                    label = "Fully Paid",
                    count = fullyPaidCount,
                    total = totalRecordsCount,
                    color = Color(0xFF10B981),
                    icon = Icons.Default.CheckCircle,
                    isDark = isDark
                )

                StatusBadgeCount(
                    label = "Advance Paid",
                    count = advancePaidCount,
                    total = totalRecordsCount,
                    color = Color(0xFFF59E0B),
                    icon = Icons.Default.HourglassEmpty,
                    isDark = isDark
                )

                StatusBadgeCount(
                    label = "Pending",
                    count = pendingCount,
                    total = totalRecordsCount,
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.ReceiptLong,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun FinancialMetricBox(
    label: String,
    targetValue: Double? = null,
    formatter: ((Double) -> String)? = null,
    value: String = "",
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val podShape = RoundedCornerShape(14.dp)
    val podBgBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.22f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.16f)
            )
        }
    )
    val podBorderBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.80f),
                Color.White.copy(alpha = 0.35f)
            )
        }
    )

    Box(
        modifier = modifier
            .clip(podShape)
            .background(podBgBrush)
            .border(1.dp, podBorderBrush, podShape)
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = label,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        isSecondary = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (targetValue != null && formatter != null) {
                CountUpText(
                    targetValue = targetValue,
                    formatter = formatter,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isAccent = true,
                        accentColor = accentColor,
                        isProminent = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = value,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isAccent = true,
                        accentColor = accentColor,
                        isProminent = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadgeCount(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    icon: ImageVector,
    isDark: Boolean
) {
    val chipShape = RoundedCornerShape(10.dp)
    val chipBgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                color.copy(alpha = 0.18f),
                color.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                color.copy(alpha = 0.10f)
            )
        )
    }
    val chipBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                color.copy(alpha = 0.45f),
                color.copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.75f),
                color.copy(alpha = 0.30f)
            )
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(chipShape)
            .background(chipBgBrush)
            .border(1.dp, chipBorderBrush, chipShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(0.8.dp, color.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Column {
            Text(
                text = "$count entries",
                style = glassEtchedTextStyle(
                    isDark = isDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    isAccent = true,
                    accentColor = color,
                    isProminent = true
                )
            )
            Text(
                text = label,
                style = glassEtchedTextStyle(
                    isDark = isDark,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    isSecondary = true
                )
            )
        }
    }
}

@Composable
private fun CategorySummaryCard(
    title: String,
    icon: ImageVector,
    badgeColor: Color,
    recordsCount: Int,
    totalQuantity: Int,
    unitLabel: String,
    totalValue: Double,
    remainingBalance: Double,
    paidCount: Int,
    onClick: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = badgeColor,
                shape = RoundedCornerShape(16.dp),
                elevation = 2.5.dp,
                borderWidth = 1.dp
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Frosted Glass Bubble for Category Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.70f),
                                    badgeColor.copy(alpha = 0.50f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isProminent = true
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$recordsCount entries",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                isAccent = true,
                                accentColor = badgeColor
                            )
                        )
                        Text(
                            text = "•",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                isSecondary = true
                            )
                        )
                        Text(
                            text = "$totalQuantity $unitLabel",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                isSecondary = true
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (totalValue > 0) {
                    Text(
                        text = "₹${currencyFormat.format(totalValue.toLong())}",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isProminent = true
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (remainingBalance > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                                .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.40f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Due: ₹${currencyFormat.format(remainingBalance.toLong())}",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.40f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cleared",
                                style = glassEtchedTextStyle(
                                    isDark = isDark,
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "$recordsCount Total",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = badgeColor
                        )
                    )
                    Text(
                        text = "Active Module",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            isSecondary = true
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun VarietyDistributionCard(
    allRecords: List<CropRecord>,
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val topVarieties = remember(allRecords) {
        allRecords
            .filter { it.plantVariety.isNotBlank() }
            .groupBy { it.plantVariety.trim() }
            .mapValues { it.value.sumOf { record -> record.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    val maxVarietyCount = remember(topVarieties) {
        topVarieties.maxOfOrNull { it.second } ?: 1
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = accentColor,
                shape = RoundedCornerShape(18.dp),
                elevation = 3.dp
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Top Operational Varieties",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isProminent = true
                        )
                    )
                }
                Text(
                    text = "Inventory Ratio",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        isSecondary = true
                    )
                )
            }

            if (topVarieties.isEmpty()) {
                Text(
                    text = "No plant variety data recorded yet.",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 12.sp,
                        isSecondary = true
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topVarieties.forEach { (variety, count) ->
                        val ratio = if (maxVarietyCount > 0) count.toFloat() / maxVarietyCount else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = variety,
                                    style = glassEtchedTextStyle(
                                        isDark = isDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "$count units",
                                    style = glassEtchedTextStyle(
                                        isDark = isDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        isAccent = true,
                                        accentColor = accentColor
                                    )
                                )
                            }
                            // Recessed glass progress track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color.Black.copy(alpha = 0.40f) else Color(0xFFCBD5E1).copy(alpha = 0.40f))
                                    .border(
                                        0.6.dp,
                                        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        RoundedCornerShape(4.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    accentColor.copy(alpha = 0.70f),
                                                    accentColor
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordLogItemCard(
    record: CropRecord,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val currencyFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
    val serviceAccent = getSectionAccentColor(record.serviceType, defaultColor = MaterialTheme.colorScheme.primary)
    val isCleared = record.isPaymentCleared()
    val remaining = record.calculateRemainingBalance()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = serviceAccent,
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp,
                borderWidth = 1.dp
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = record.serialNumber.ifBlank { "SN-${record.id}" },
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isAccent = true,
                            accentColor = serviceAccent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(serviceAccent.copy(alpha = 0.16f))
                            .border(0.8.dp, serviceAccent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = record.serviceType,
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                color = serviceAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = record.farmerName,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        isProminent = true
                    )
                )
                Text(
                    text = "${record.plantVariety} • Qty: ${record.quantity}",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        isSecondary = true
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${currencyFormat.format(record.calculateTotalAmount().toLong())}",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isProminent = true
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (isCleared) Color(0xFF10B981).copy(alpha = 0.16f)
                            else Color(0xFFEF4444).copy(alpha = 0.16f)
                        )
                        .border(
                            0.8.dp,
                            if (isCleared) Color(0xFF10B981).copy(alpha = 0.40f)
                            else Color(0xFFEF4444).copy(alpha = 0.40f),
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCleared) "PAID" else "DUE ₹${currencyFormat.format(remaining.toLong())}",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            color = if (isCleared) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GardenLogItemCard(
    entry: GardenPlanningEntry,
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val gardenAccent = Color(0xFF00897B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = gardenAccent,
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp,
                borderWidth = 1.dp
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Garden Planning",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            isAccent = true,
                            accentColor = gardenAccent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gardenAccent.copy(alpha = 0.16f))
                            .border(0.8.dp, gardenAccent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${entry.serialNumber}",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                color = gardenAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.farmerName.ifBlank { "Garden Entry" },
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        isProminent = true
                    )
                )
                val infoText = buildString {
                    if (entry.plantVariety.isNotBlank()) append(entry.plantVariety).append(" • ")
                    append("${entry.totalKanalArea} Kanals (${entry.plantsPerKanal}/Kanal)")
                }
                Text(
                    text = infoText,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        isSecondary = true
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(entry.totalCost),
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isProminent = true
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                val isCleared = entry.paymentStatus == "Fully Paid"
                val isAdvance = entry.paymentStatus == "Advance Paid"
                val badgeColor = if (isCleared) Color(0xFF10B981) else if (isAdvance) Color(0xFFF59E0B) else Color(0xFFEF4444)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.40f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCleared) "PAID" else if (isAdvance) "ADVANCE" else "DUE ${currencyFormat.format(entry.remainingBalance.toLong())}",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            color = badgeColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    isDark: Boolean = false,
    isAmoled: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .etchedGlassSurface(
                isDark = isDark,
                isAmoled = isAmoled,
                accentColor = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp,
                borderWidth = 1.dp
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = glassEtchedTextStyle(
                isDark = isDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                isSecondary = true
            )
        )
    }
}
