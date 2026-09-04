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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import dev.chrisbanes.haze.materials.HazeMaterials
import com.example.data.InventoryItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import com.example.ui.theme.getDynamicPaletteBackgroundBrush
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

    // Refined frosted-glass HazeStyle: 20.dp blur for true translucent Gaussian dispersion
    // Transparent background tint allows background shapes & colors to diffuse through vividly
    val hazeStyle = HazeStyle(
        backgroundColor = if (isAmoled) {
            Color.Black.copy(alpha = 0.12f)
        } else if (isDark) {
            Color(0xFF14121B).copy(alpha = 0.10f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        blurRadius = 20.dp,
        tints = listOf(
            HazeTint(
                color = if (isDark || isAmoled) {
                    accentColor.copy(alpha = 0.02f)
                } else {
                    Color.White.copy(alpha = 0.02f)
                }
            )
        ),
        noiseFactor = 0f
    )

    // Translucent glass body wash: thin crystal-clear glass allowing backdrop colors & shapes to softly show through
    val glassBaseBrush = when {
        isAmoled -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color(0xFF100E14).copy(alpha = 0.08f),
                    accentColor.copy(alpha = 0.02f),
                    Color(0xFF000000).copy(alpha = 0.18f)
                )
            )
        }
        isDark -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color(0xFF221F2B).copy(alpha = 0.10f),
                    accentColor.copy(alpha = 0.02f),
                    Color(0xFF14121A).copy(alpha = 0.14f)
                )
            )
        }
        else -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f), // Crisp specular reflection along top edge
                    Color.White.copy(alpha = 0.04f), // Crystal-clear translucent glass body
                    Color.White.copy(alpha = 0.02f), // Transparent center letting background shine through
                    Color.White.copy(alpha = 0.08f)  // Soft ambient bottom return
                )
            )
        }
    }

    // Specular reflective glass rim border: subtle, refined 1dp etched glass rim
    val borderBrush = Brush.linearGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.32f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.18f), // Reflective edge sheen
                Color.White.copy(alpha = 0.08f), // Clear lateral glass sides
                Color.White.copy(alpha = 0.04f)  // Soft specular bottom return
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.65f), // Crisp specular reflection along top edge
                Color.White.copy(alpha = 0.25f), // Clear lateral glass sides
                Color.White.copy(alpha = 0.10f), // Bottom subtle edge
                Color.White.copy(alpha = 0.20f)  // Soft specular bottom return
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
            val w = size.width
            val margin = 10.dp.toPx()
            val highlightH = 1.dp.toPx()

            if (showTopGlint) {
                // Crisp top specular glint beam along upper rim (drawn on glass surface beneath content)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.25f else 0.40f),
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.50f else 0.75f),
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.25f else 0.40f),
                            Color.Transparent
                        ),
                        startX = margin,
                        endX = w - margin
                    ),
                    topLeft = Offset(margin, 0.5.dp.toPx()),
                    size = Size(w - (margin * 2), highlightH)
                )
                // Subtle top-left specular corner reflection
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.05f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.30f
                    )
                )
            }

            // Draw content ON TOP of the glass reflections so all text, icons & numbers remain 100% sharp
            drawContent()
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
    scrollOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    val palette = com.example.ui.theme.LocalAppPalette.current

    // Dynamically derive soft, dim ambient diffusion bloom colors from the active palette
    val bloomColors = remember(palette, accentColor, isDark, isAmoled) {
        if (palette.isTwoColor) {
            // Refined neutral monochromatic/slate blooms for two-color & monochrome palettes
            val p = if (isDark || isAmoled) Color(0xFF3F3F46) else Color(0xFFCBD5E1)
            val s = if (isDark || isAmoled) Color(0xFF27272A) else Color(0xFFE2E8F0)
            val t = if (isDark || isAmoled) Color(0xFF52525B) else Color(0xFF94A3B8)
            val b1 = if (isDark || isAmoled) Color(0xFF333338) else Color(0xFFD8E0EA)
            val b2 = if (isDark || isAmoled) Color(0xFF202024) else Color(0xFFCBD5E1)
            listOf(p, s, t, b1, b2)
        } else {
            val (baseP, baseS, baseT) = if (palette.id != "solid_active") {
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
                val sec = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 42f) % 360f, 0.55f, 0.90f)))
                val tert = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue - 38f + 360f) % 360f, 0.50f, 0.85f)))
                Triple(accentColor, sec, tert)
            }

            fun toSoftBloomColor(c: Color, satScale: Float = 1f): Color {
                val r = (c.red * 255f).toInt().coerceIn(0, 255)
                val g = (c.green * 255f).toInt().coerceIn(0, 255)
                val b = (c.blue * 255f).toInt().coerceIn(0, 255)
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = if (isAmoled) 0.55f * satScale else if (isDark) 0.48f * satScale else 0.32f * satScale
                val value = if (isAmoled) 0.85f else if (isDark) 0.80f else 0.95f
                return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0.08f, 0.60f), value)))
            }

            val pBloom = toSoftBloomColor(baseP)
            val sBloom = toSoftBloomColor(baseS)
            val tBloom = toSoftBloomColor(baseT)
            val b1 = toSoftBloomColor(baseP, satScale = 0.85f)
            val b2 = toSoftBloomColor(baseS, satScale = 0.85f)
            listOf(pBloom, sBloom, tBloom, b1, b2)
        }
    }

    val primaryBloom = bloomColors[0]
    val secondaryBloom = bloomColors[1]
    val tertiaryBloom = bloomColors[2]
    val blendBloom1 = bloomColors[3]
    val blendBloom2 = bloomColors[4]

    // Soft, low-saturation, dim alpha scales so blooms remain calm, subtle, and readable
    val primaryAlpha = if (isAmoled) 0.12f else if (isDark) 0.16f else 0.20f
    val bloomAlpha = if (isAmoled) 0.10f else if (isDark) 0.14f else 0.17f

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val yShift = -scrollOffset * 0.30f

        // 1. Top-right pool (illuminating Header & Account banner) - Derived from Palette Primary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryBloom.copy(alpha = primaryAlpha), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.12f + yShift),
                radius = w * 0.80f
            ),
            center = Offset(w * 0.85f, h * 0.12f + yShift),
            radius = w * 0.80f
        )

        // 2. Upper-left pool (illuminating Financial Breakdown card) - Derived from Palette Secondary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryBloom.copy(alpha = bloomAlpha), Color.Transparent),
                center = Offset(w * 0.10f, h * 0.24f + yShift),
                radius = w * 0.70f
            ),
            center = Offset(w * 0.10f, h * 0.24f + yShift),
            radius = w * 0.70f
        )

        // 3. Mid-right pool (diffusing behind Inventory Management module) - Derived from Palette Tertiary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryBloom.copy(alpha = bloomAlpha * 0.95f), Color.Transparent),
                center = Offset(w * 0.90f, h * 0.38f + yShift),
                radius = w * 0.72f
            ),
            center = Offset(w * 0.90f, h * 0.38f + yShift),
            radius = w * 0.72f
        )

        // 4. Mid-left pool (diffusing behind Contract Director module) - Derived from Palette Primary Harmonic
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blendBloom1.copy(alpha = bloomAlpha * 0.95f), Color.Transparent),
                center = Offset(w * 0.08f, h * 0.50f + yShift),
                radius = w * 0.72f
            ),
            center = Offset(w * 0.08f, h * 0.50f + yShift),
            radius = w * 0.72f
        )

        // 5. Lower-right pool (diffusing behind Attendance module) - Derived from Palette Secondary Harmonic
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blendBloom2.copy(alpha = bloomAlpha * 0.90f), Color.Transparent),
                center = Offset(w * 0.92f, h * 0.64f + yShift),
                radius = w * 0.70f
            ),
            center = Offset(w * 0.92f, h * 0.64f + yShift),
            radius = w * 0.70f
        )

        // 6. Lower-left pool (diffusing behind Payment Reminder module) - Derived from Palette Primary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryBloom.copy(alpha = bloomAlpha * 0.90f), Color.Transparent),
                center = Offset(w * 0.10f, h * 0.76f + yShift),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.10f, h * 0.76f + yShift),
            radius = w * 0.75f
        )

        // 7. Bottom pool (diffusing behind lower summaries) - Derived from Palette Tertiary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryBloom.copy(alpha = bloomAlpha * 0.75f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.88f + yShift),
                radius = w * 0.68f
            ),
            center = Offset(w * 0.85f, h * 0.88f + yShift),
            radius = w * 0.68f
        )

        // 8. Flowing subtle organic wave ribbons (derived from Palette colors)
        val waveAlpha = if (isAmoled) 0.04f else if (isDark) 0.06f else 0.07f
        val waveStroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.5.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(-w * 0.2f, h * 0.22f + yShift)
            cubicTo(
                w * 0.3f, h * 0.18f + yShift,
                w * 0.6f, h * 0.30f + yShift,
                w * 1.2f, h * 0.24f + yShift
            )
        }
        drawPath(path1, color = primaryBloom.copy(alpha = waveAlpha), style = waveStroke)

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(-w * 0.1f, h * 0.48f + yShift)
            cubicTo(
                w * 0.35f, h * 0.56f + yShift,
                w * 0.70f, h * 0.42f + yShift,
                w * 1.2f, h * 0.52f + yShift
            )
        }
        drawPath(path2, color = secondaryBloom.copy(alpha = waveAlpha * 0.9f), style = waveStroke)

        val path3 = androidx.compose.ui.graphics.Path().apply {
            moveTo(-w * 0.2f, h * 0.78f + yShift)
            cubicTo(
                w * 0.4f, h * 0.72f + yShift,
                w * 0.65f, h * 0.84f + yShift,
                w * 1.15f, h * 0.76f + yShift
            )
        }
        drawPath(path3, color = tertiaryBloom.copy(alpha = waveAlpha * 0.85f), style = waveStroke)

        // 9. Stippled delicate micro-dot matrix pattern in lower section (matching reference footer)
        val dotSpacing = 16.dp.toPx()
        val dotRadius = 1.2.dp.toPx()
        val dotColor = primaryBloom.copy(alpha = if (isAmoled) 0.05f else if (isDark) 0.07f else 0.08f)
        val startX = w * 0.55f
        val startY = h * 0.82f + yShift
        var curX = startX
        while (curX < w) {
            var curY = startY
            while (curY < h) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(curX, curY))
                curY += dotSpacing
            }
            curX += dotSpacing
        }
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
    onNavigateToInventory: (() -> Unit)? = null,
    onNavigateToContactDirectory: (() -> Unit)? = null,
    onNavigateToAttendance: (() -> Unit)? = null,
    onNavigateToPaymentReminders: (() -> Unit)? = null,
    onNavigateToCalendar: (() -> Unit)? = null,
    onNavigateToSeasonalReminders: (() -> Unit)? = null,
    onNavigateToQrCode: (() -> Unit)? = null,
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
    val inventoryItems by viewModel.inventoryItems.collectAsState()

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

    val appPalette = com.example.ui.theme.LocalAppPalette.current
    val dashboardBgBrush = remember(appPalette, isDark, isAmoled) {
        getDynamicPaletteBackgroundBrush(appPalette, isDark = isDark, isAmoled = isAmoled)
    }

    val dashboardListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollOffset by remember {
        androidx.compose.runtime.derivedStateOf {
            dashboardListState.firstVisibleItemIndex * 260f + dashboardListState.firstVisibleItemScrollOffset
        }
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
                isAmoled = isAmoled,
                scrollOffset = scrollOffset
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
                    state = dashboardListState,
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

                    // 3. Frosted Liquid Glass Core Modules Hub Header
                    item {
                        FrostedLiquidModulesSectionHeader(
                            accentColor = dashboardAccent,
                            isDark = isDark
                        )
                    }

                    // 2-Column Grid of 8 Core Executive Modules (Matching reference screenshot)
                    item {
                        FrostedLiquidModulesGrid(
                            hazeState = effectiveHazeState,
                            onOpenInventory = { onNavigateToInventory?.invoke() },
                            onOpenAttendance = { onNavigateToAttendance?.invoke() },
                            onOpenCalendar = { onNavigateToCalendar?.invoke() ?: onNavigateToCategory?.invoke("Garden Planning") },
                            onOpenContactDirectory = { onNavigateToContactDirectory?.invoke() },
                            onOpenPaymentReminder = { onNavigateToPaymentReminders?.invoke() },
                            onOpenSeasonalReminders = { onNavigateToSeasonalReminders?.invoke() },
                            onOpenQrCode = { onNavigateToQrCode?.invoke() },
                            onOpenSettings = { onNavigateToSettings?.invoke() },
                            isDark = isDark,
                            isAmoled = isAmoled
                        )
                    }

                    // Modern Agriculture Smarter Operations Footer (Matching reference screenshot)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modern Agriculture Smarter Operations",
                                    style = glassEtchedTextStyle(
                                        isDark = isDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        isSecondary = true
                                    ).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                                Text(
                                    text = "Glass UI. Real Impact.",
                                    style = glassEtchedTextStyle(
                                        isDark = isDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dashboardAccent.copy(alpha = 0.80f)
                                    )
                                )
                            }
                        }
                    }

                    // 4. Operational Category Summaries Header
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
                                                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.18f)
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
                    isDark = isDark,
                    isAmoled = isAmoled
                )

                StatusBadgeCount(
                    label = "Advance Paid",
                    count = advancePaidCount,
                    total = totalRecordsCount,
                    color = Color(0xFFF59E0B),
                    icon = Icons.Default.HourglassEmpty,
                    isDark = isDark,
                    isAmoled = isAmoled
                )

                StatusBadgeCount(
                    label = "Pending",
                    count = pendingCount,
                    total = totalRecordsCount,
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.ReceiptLong,
                    isDark = isDark,
                    isAmoled = isAmoled
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
                accentColor.copy(alpha = 0.03f),
                Color(0xFF14121A).copy(alpha = 0.14f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.20f), // Crisp top specular reflection
                Color.White.copy(alpha = 0.05f), // Highly translucent glass body
                accentColor.copy(alpha = 0.03f), // Very gentle color breath
                Color.White.copy(alpha = 0.10f)  // Soft bottom return
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
                Color.White.copy(alpha = 0.70f), // Crisp specular upper rim
                Color.White.copy(alpha = 0.22f)  // Subtle lower rim
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
                        .border(
                            0.8.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.70f), accentColor.copy(alpha = 0.35f))
                            ),
                            CircleShape
                        ),
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
    isDark: Boolean,
    isAmoled: Boolean = false
) {
    val chipShape = RoundedCornerShape(10.dp)
    val chipBgBrush = if (isDark || isAmoled) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f),
                color.copy(alpha = 0.08f),
                Color(0xFF14121A).copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f), // Specular rim
                Color.White.copy(alpha = 0.05f), // Translucent glass body
                color.copy(alpha = 0.04f),       // Status color reflection
                Color.White.copy(alpha = 0.10f)
            )
        )
    }
    val chipBorderBrush = if (isDark || isAmoled) {
        Brush.verticalGradient(
            listOf(
                color.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                color.copy(alpha = 0.25f)
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
                .background(color.copy(alpha = 0.16f))
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.70f), color.copy(alpha = 0.35f))
                    ),
                    CircleShape
                ),
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

/**
 * Reusable Frosted Liquid Glass surface modifier.
 * Implements real-time background blur via HazeMaterials.thin(), fluid gradient background
 * for liquid refraction, and two-tone specular rim border highlight.
 */
@Composable
fun Modifier.frostedLiquidGlassSurface(
    hazeState: HazeState?,
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = false,
    isAmoled: Boolean = false
): Modifier {
    val hazeStyle = remember(isDark, isAmoled) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 24.dp,
            tints = listOf(
                HazeTint(
                    color = if (isAmoled) Color.Black.copy(alpha = 0.10f)
                    else if (isDark) Color(0xFF14121B).copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.06f)
                )
            ),
            noiseFactor = 0f
        )
    }
    val bgBrush = remember(isDark, isAmoled) {
        val topAlpha = if (isAmoled) 0.08f else if (isDark) 0.12f else 0.14f
        val bottomAlpha = if (isAmoled) 0.02f else if (isDark) 0.04f else 0.05f
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = bottomAlpha)
            )
        )
    }
    val rimBrush = remember(isDark, isAmoled) {
        val rimTop = if (isAmoled) 0.35f else if (isDark) 0.45f else 0.55f
        val rimBottom = if (isAmoled) 0.08f else if (isDark) 0.10f else 0.14f
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = rimTop),
                Color.White.copy(alpha = rimBottom)
            )
        )
    }

    return this
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(bgBrush, shape = shape)
        .border(BorderStroke(1.dp, rimBrush), shape = shape)
}

/**
 * Section header introducing the Core Executive Modules with an indicator pill.
 */
@Composable
fun FrostedLiquidModulesSectionHeader(
    accentColor: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
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
                        .background(accentColor.copy(alpha = if (isDark) 0.24f else 0.16f))
                        .border(1.dp, accentColor.copy(alpha = if (isDark) 0.50f else 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Core Executive Modules",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isProminent = true
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "8 Modules",
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

data class ExecutiveModuleItemData(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val tag: String,
    val onClick: () -> Unit
)

/**
 * 2-Column Frosted Liquid Glass Executive Modules Grid (Exactly as shown in design mockup)
 */
@Composable
fun FrostedLiquidModulesGrid(
    hazeState: HazeState?,
    onOpenInventory: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenContactDirectory: () -> Unit,
    onOpenPaymentReminder: () -> Unit,
    onOpenSeasonalReminders: () -> Unit,
    onOpenQrCode: () -> Unit,
    onOpenSettings: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val modules = listOf(
        ExecutiveModuleItemData(
            title = "Inventory Management",
            subtitle = "Live Stock, Nurseries & Supplies",
            icon = Icons.Default.Inventory2,
            iconTint = Color(0xFF10B981),
            iconBg = Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_inventory",
            onClick = onOpenInventory
        ) to ExecutiveModuleItemData(
            title = "Attendance",
            subtitle = "Daily Workforce, Shift Logs & Roster",
            icon = Icons.Default.Groups,
            iconTint = Color(0xFF3B82F6),
            iconBg = Color(0xFF3B82F6).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_attendance",
            onClick = onOpenAttendance
        ),
        ExecutiveModuleItemData(
            title = "Calendar",
            subtitle = "Events, Schedule & Field Activities",
            icon = Icons.Default.CalendarMonth,
            iconTint = Color(0xFFF97316),
            iconBg = Color(0xFFF97316).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_calendar",
            onClick = onOpenCalendar
        ) to ExecutiveModuleItemData(
            title = "Contact Directory",
            subtitle = "Farmer Contracts, Profiles & Directory",
            icon = Icons.Default.ContactPhone,
            iconTint = Color(0xFF8B5CF6),
            iconBg = Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_contacts",
            onClick = onOpenContactDirectory
        ),
        ExecutiveModuleItemData(
            title = "Payment Reminder",
            subtitle = "Dues, Follow-ups & Notifications",
            icon = Icons.Default.CreditCard,
            iconTint = Color(0xFFEF4444),
            iconBg = Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_payment_reminder",
            onClick = onOpenPaymentReminder
        ) to ExecutiveModuleItemData(
            title = "Seasonal Reminders",
            subtitle = "Crop Cycles, Tasks & Alerts",
            icon = Icons.Default.Eco,
            iconTint = Color(0xFF14B8A6),
            iconBg = Color(0xFF14B8A6).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_seasonal_reminders",
            onClick = onOpenSeasonalReminders
        ),
        ExecutiveModuleItemData(
            title = "QR Code",
            subtitle = "Scan, Generate & Manage",
            icon = Icons.Default.QrCodeScanner,
            iconTint = Color(0xFF06B6D4),
            iconBg = Color(0xFF06B6D4).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_qr_code",
            onClick = onOpenQrCode
        ) to ExecutiveModuleItemData(
            title = "Settings",
            subtitle = "Preferences, Theme & App Config",
            icon = Icons.Default.Settings,
            iconTint = Color(0xFF64748B),
            iconBg = Color(0xFF64748B).copy(alpha = if (isDark) 0.22f else 0.14f),
            tag = "dashboard_module_settings",
            onClick = onOpenSettings
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modules.forEach { (itemLeft, itemRight) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FrostedLiquidModuleTile(
                        data = itemLeft,
                        hazeState = hazeState,
                        isDark = isDark,
                        isAmoled = isAmoled
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FrostedLiquidModuleTile(
                        data = itemRight,
                        hazeState = hazeState,
                        isDark = isDark,
                        isAmoled = isAmoled
                    )
                }
            }
        }
    }
}

/**
 * Individual Frosted Liquid Glass Module Tile with squircle badge, chevron, and etched typography.
 */
@Composable
fun FrostedLiquidModuleTile(
    data: ExecutiveModuleItemData,
    hazeState: HazeState?,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .frostedLiquidGlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(18.dp),
                isDark = isDark,
                isAmoled = isAmoled,
                elevation = 1.dp
            )
            .clickable(onClick = data.onClick)
            .padding(12.dp)
            .testTag(data.tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(data.iconBg)
                        .border(1.dp, data.iconTint.copy(alpha = if (isDark) 0.45f else 0.30f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = data.title,
                        tint = data.iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = data.title,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        isProminent = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.subtitle,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        isSecondary = true
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

/**
 * 1. Inventory Management Frosted Liquid Glass Card
 */
@Composable
fun FrostedLiquidInventoryCard(
    inventoryItems: List<InventoryItem>,
    hazeState: HazeState?,
    onOpenInventory: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val leafGreen = Color(0xFF10B981)
    val totalStockUnits = remember(inventoryItems) { inventoryItems.sumOf { it.currentQuantity } }
    val lowStockItems = remember(inventoryItems) {
        inventoryItems.filter { it.isLowStock() }
    }
    val lowStockCount = lowStockItems.size
    val totalSkus = inventoryItems.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .frostedLiquidGlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark,
                isAmoled = isAmoled
            )
            .clickable(onClick = onOpenInventory)
            .padding(18.dp)
            .testTag("dashboard_module_inventory")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Glowing soft vibrant leaf green pill
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(leafGreen.copy(alpha = if (isDark) 0.24f else 0.16f))
                            .border(1.dp, leafGreen.copy(alpha = if (isDark) 0.60f else 0.40f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Inventory Management",
                            tint = leafGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Inventory Management",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                isProminent = true
                            )
                        )
                        Text(
                            text = "Live Stock, Nurseries & Supplies",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                isSecondary = true
                            )
                        )
                    }
                }

                // Alert / Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (lowStockCount > 0) Color(0xFFEF4444).copy(alpha = if (isDark) 0.25f else 0.14f)
                            else leafGreen.copy(alpha = if (isDark) 0.25f else 0.14f)
                        )
                        .border(
                            1.dp,
                            if (lowStockCount > 0) Color(0xFFEF4444).copy(alpha = 0.50f)
                            else leafGreen.copy(alpha = 0.50f),
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (lowStockCount > 0) Color(0xFFEF4444) else leafGreen)
                        )
                        Text(
                            text = if (lowStockCount > 0) "$lowStockCount Low Stock" else "Optimal Stock",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                isAccent = true,
                                accentColor = if (lowStockCount > 0) Color(0xFFEF4444) else leafGreen
                            )
                        )
                    }
                }
            }

            // Key Metrics Glass Pods Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrostedLiquidMetricPod(
                    value = "$totalSkus",
                    unit = "SKUs",
                    label = "Catalog Items",
                    accentColor = leafGreen,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "$totalStockUnits",
                    unit = "Units",
                    label = "In Stock",
                    accentColor = leafGreen,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "$lowStockCount",
                    unit = "Alerts",
                    label = "Reorder Needed",
                    accentColor = if (lowStockCount > 0) Color(0xFFEF4444) else leafGreen,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Footer Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(leafGreen.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                leafGreen.copy(alpha = if (isDark) 0.65f else 0.45f),
                                leafGreen.copy(alpha = if (isDark) 0.25f else 0.15f)
                            )
                        ),
                        RoundedCornerShape(percent = 50)
                    )
                    .clickable(onClick = onOpenInventory)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Manage Inventory",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = leafGreen
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = leafGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. Contract Director Frosted Liquid Glass Card
 */
@Composable
fun FrostedLiquidContractDirectorCard(
    cropRecords: List<CropRecord>,
    gardenEntries: List<GardenPlanningEntry>,
    hazeState: HazeState?,
    onOpenContractDirector: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val skyBlue = Color(0xFF0EA5E9)
    val uniqueFarmers = remember(cropRecords, gardenEntries) {
        val names = mutableSetOf<String>()
        cropRecords.forEach { if (it.farmerName.isNotBlank()) names.add(it.farmerName.trim().lowercase()) }
        gardenEntries.forEach { if (it.farmerName.isNotBlank()) names.add(it.farmerName.trim().lowercase()) }
        names.size
    }
    val contactsWithPhone = remember(cropRecords, gardenEntries) {
        val phones = mutableSetOf<String>()
        cropRecords.forEach { if (it.contactNumber.isNotBlank()) phones.add(it.contactNumber.trim()) }
        gardenEntries.forEach { if (it.contactNumber.isNotBlank()) phones.add(it.contactNumber.trim()) }
        phones.size
    }
    val totalContracts = cropRecords.size + gardenEntries.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .frostedLiquidGlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark,
                isAmoled = isAmoled
            )
            .clickable(onClick = onOpenContractDirector)
            .padding(18.dp)
            .testTag("dashboard_module_contract_director")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Glowing soft vibrant sky blue pill
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(skyBlue.copy(alpha = if (isDark) 0.24f else 0.16f))
                            .border(1.dp, skyBlue.copy(alpha = if (isDark) 0.60f else 0.40f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Contract Director",
                            tint = skyBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Contract Director",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                isProminent = true
                            )
                        )
                        Text(
                            text = "Farmer Contracts, Profiles & Directory",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                isSecondary = true
                            )
                        )
                    }
                }

                // Verified Directory status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(skyBlue.copy(alpha = if (isDark) 0.25f else 0.14f))
                        .border(1.dp, skyBlue.copy(alpha = 0.50f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(skyBlue)
                        )
                        Text(
                            text = "$uniqueFarmers Contacts",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                isAccent = true,
                                accentColor = skyBlue
                            )
                        )
                    }
                }
            }

            // Key Metrics Glass Pods Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrostedLiquidMetricPod(
                    value = "$uniqueFarmers",
                    unit = "Farmers",
                    label = "Active Contracts",
                    accentColor = skyBlue,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "$contactsWithPhone",
                    unit = "Verified",
                    label = "Direct Dial",
                    accentColor = skyBlue,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "$totalContracts",
                    unit = "Orders",
                    label = "Total Records",
                    accentColor = skyBlue,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Footer Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(skyBlue.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                skyBlue.copy(alpha = if (isDark) 0.65f else 0.45f),
                                skyBlue.copy(alpha = if (isDark) 0.25f else 0.15f)
                            )
                        ),
                        RoundedCornerShape(percent = 50)
                    )
                    .clickable(onClick = onOpenContractDirector)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Open Contract Directory",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = skyBlue
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = skyBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. Attendance Frosted Liquid Glass Card
 */
@Composable
fun FrostedLiquidAttendanceCard(
    userDashboardViewModel: UserDashboardViewModel,
    hazeState: HazeState?,
    onOpenAttendance: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val emeraldLeaf = Color(0xFF10B981)
    val rawAttendance by userDashboardViewModel.rawAttendance.collectAsState()
    val todayFormatted = remember {
        try {
            SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
        } catch (e: Throwable) {
            "Today"
        }
    }
    val todayIso = remember {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        } catch (e: Throwable) {
            ""
        }
    }
    val todayAttendance = remember(rawAttendance, todayIso) {
        rawAttendance.filter { it.date == todayIso || it.date.contains(todayIso) }
    }
    val presentCount = remember(todayAttendance) {
        todayAttendance.count { it.status.equals("Present", ignoreCase = true) || it.status.equals("P", ignoreCase = true) }
    }
    val totalWorkers = remember(rawAttendance) {
        val uniqueWorkers = rawAttendance.map { it.workerName }.filter { it.isNotBlank() }.distinct()
        if (uniqueWorkers.isNotEmpty()) uniqueWorkers.size else if (todayAttendance.isNotEmpty()) todayAttendance.size else 6
    }
    val coveragePct = if (totalWorkers > 0 && todayAttendance.isNotEmpty()) (presentCount * 100 / totalWorkers).coerceIn(0, 100) else if (rawAttendance.isNotEmpty()) 100 else 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .frostedLiquidGlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark,
                isAmoled = isAmoled
            )
            .clickable(onClick = onOpenAttendance)
            .padding(18.dp)
            .testTag("dashboard_module_attendance")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Glowing soft vibrant emerald leaf pill
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(emeraldLeaf.copy(alpha = if (isDark) 0.24f else 0.16f))
                            .border(1.dp, emeraldLeaf.copy(alpha = if (isDark) 0.60f else 0.40f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToReg,
                            contentDescription = "Attendance",
                            tint = emeraldLeaf,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Attendance",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                isProminent = true
                            )
                        )
                        Text(
                            text = "Daily Workforce, Shift Logs & Roster",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                isSecondary = true
                            )
                        )
                    }
                }

                // Date badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(emeraldLeaf.copy(alpha = if (isDark) 0.25f else 0.14f))
                        .border(1.dp, emeraldLeaf.copy(alpha = 0.50f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(emeraldLeaf)
                        )
                        Text(
                            text = todayFormatted,
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                isAccent = true,
                                accentColor = emeraldLeaf
                            )
                        )
                    }
                }
            }

            // Key Metrics Glass Pods Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrostedLiquidMetricPod(
                    value = if (todayAttendance.isNotEmpty()) "$presentCount" else "${rawAttendance.size}",
                    unit = if (todayAttendance.isNotEmpty()) "On Duty" else "Logs",
                    label = if (todayAttendance.isNotEmpty()) "Present Today" else "Total Shifts",
                    accentColor = emeraldLeaf,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "$totalWorkers",
                    unit = "Workers",
                    label = "Active Team",
                    accentColor = emeraldLeaf,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = if (todayAttendance.isNotEmpty()) "$coveragePct%" else "Active",
                    unit = if (todayAttendance.isNotEmpty()) "Coverage" else "Roster",
                    label = "Shift Health",
                    accentColor = emeraldLeaf,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Footer Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(emeraldLeaf.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                emeraldLeaf.copy(alpha = if (isDark) 0.65f else 0.45f),
                                emeraldLeaf.copy(alpha = if (isDark) 0.25f else 0.15f)
                            )
                        ),
                        RoundedCornerShape(percent = 50)
                    )
                    .clickable(onClick = onOpenAttendance)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Mark & View Attendance",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = emeraldLeaf
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = emeraldLeaf,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4. Payment Reminder Frosted Liquid Glass Card
 */
@Composable
fun FrostedLiquidPaymentReminderCard(
    cropRecords: List<CropRecord>,
    gardenEntries: List<GardenPlanningEntry>,
    totalRemaining: Double,
    pendingCount: Int,
    paidRatio: Float,
    hazeState: HazeState?,
    onOpenPaymentReminders: () -> Unit,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val warningAmber = Color(0xFFF59E0B)
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val formattedDue = remember(totalRemaining) {
        try {
            numberFormat.format(totalRemaining)
        } catch (e: Throwable) {
            "₹${totalRemaining.toLong()}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .frostedLiquidGlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark,
                isAmoled = isAmoled
            )
            .clickable(onClick = onOpenPaymentReminders)
            .padding(18.dp)
            .testTag("dashboard_module_payment_reminder")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Glowing soft vibrant warning amber pill
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(warningAmber.copy(alpha = if (isDark) 0.24f else 0.16f))
                            .border(1.dp, warningAmber.copy(alpha = if (isDark) 0.60f else 0.40f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Payment Reminder",
                            tint = warningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Payment Reminder",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                isProminent = true
                            )
                        )
                        Text(
                            text = "Receivables, Overdue & Notices",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                isSecondary = true
                            )
                        )
                    }
                }

                // Alert Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (pendingCount > 0) warningAmber.copy(alpha = if (isDark) 0.25f else 0.14f)
                            else Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.14f)
                        )
                        .border(
                            1.dp,
                            if (pendingCount > 0) warningAmber.copy(alpha = 0.50f)
                            else Color(0xFF10B981).copy(alpha = 0.50f),
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (pendingCount > 0) warningAmber else Color(0xFF10B981))
                        )
                        Text(
                            text = if (pendingCount > 0) "$pendingCount Due" else "All Cleared",
                            style = glassEtchedTextStyle(
                                isDark = isDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                isAccent = true,
                                accentColor = if (pendingCount > 0) warningAmber else Color(0xFF10B981)
                            )
                        )
                    }
                }
            }

            // Key Metrics Glass Pods Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrostedLiquidMetricPod(
                    value = formattedDue,
                    unit = "",
                    label = "Outstanding",
                    accentColor = if (totalRemaining > 0) warningAmber else Color(0xFF10B981),
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1.3f)
                )

                FrostedLiquidMetricPod(
                    value = "$pendingCount",
                    unit = "Accounts",
                    label = "Pending Dues",
                    accentColor = warningAmber,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )

                FrostedLiquidMetricPod(
                    value = "${(paidRatio * 100).toInt()}%",
                    unit = "Cleared",
                    label = "Cleared Ratio",
                    accentColor = Color(0xFF10B981),
                    isDark = isDark,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Footer Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(warningAmber.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                warningAmber.copy(alpha = if (isDark) 0.65f else 0.45f),
                                warningAmber.copy(alpha = if (isDark) 0.25f else 0.15f)
                            )
                        ),
                        RoundedCornerShape(percent = 50)
                    )
                    .clickable(onClick = onOpenPaymentReminders)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Send Payment Reminders",
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = warningAmber
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = warningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Frosted mini capsule pod displaying key numeric indicators inside each module.
 */
@Composable
private fun FrostedLiquidMetricPod(
    value: String,
    unit: String,
    label: String,
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.45f),
                        if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.12f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    style = glassEtchedTextStyle(
                        isDark = isDark,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        isProminent = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        style = glassEtchedTextStyle(
                            isDark = isDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            isAccent = true,
                            accentColor = accentColor
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
            Text(
                text = label,
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
}
