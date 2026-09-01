package com.example.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AppThemeMode
import com.example.ui.components.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Ambient CompositionLocal providing the root-level HazeState for seamless Liquid Frosted Glass
 * backdrop sampling throughout the component hierarchy.
 */
val LocalLiquidHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Unified Central Frosted Liquid Glass Engine.
 *
 * RENDERING PIPELINE:
 * 1. REAL BACKDROP SAMPLING: Background canvas captured via root `hazeSource`.
 * 2. BACKDROP FROST & BLUR: Hardware-accelerated Haze backdrop blur (24-28dp) with calibrated translucent tinting.
 * 3. TRANSLUCENT GLASS BODY: Permeable, non-opaque surface gradient letting backdrop colors shine through.
 * 4. OPTICAL DEPTH & OCCLUSION: Radial lens occlusion giving physical thickness to the glass container.
 * 5. SPECULAR TOP LIGHTING: Luminous top-crest reflection and gradient illumination.
 * 6. CRISP CONTENT: 100% sharp text, icons, and interactive controls rendered without distortion.
 * 7. PERIMETER SPECULAR RIM: Multi-stop gradient border creating glass refraction along the edge.
 */
@Composable
fun Modifier.liquidFrostedGlass(
    hazeState: HazeState? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 24.dp,
    frostTintAlpha: Float = if (isDark) 0.55f else 0.50f,
    surfaceOpacity: Float = if (isDark) 0.28f else 0.22f,
    refractionStrength: Float = 0.25f,
    chromaticAberration: Float = 0.05f,
    innerDepthStrength: Float = 0.40f,
    highlightStrength: Float = 0.85f,
    themeMode: AppThemeMode? = null,
    isFocused: Boolean = false,
    flatStyle: Boolean = false
): Modifier {
    val effectiveIsDark = when (themeMode) {
        AppThemeMode.DARK,
        AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM,
        null -> isDark
    }

    val screenBgColor = MaterialTheme.colorScheme.background
    val isAmoled = themeMode == AppThemeMode.AMOLED ||
            (effectiveIsDark && (screenBgColor.luminance() < 0.01f || screenBgColor == Color.Black))

    val effectiveElevation = if (flatStyle) 0.dp else elevation
    val effectiveBorderWidth = if (isFocused) {
        (borderWidth * 1.35f).coerceAtLeast(1.25.dp)
    } else {
        (borderWidth * 0.90f).coerceAtLeast(0.8.dp)
    }

    val effectiveHazeState = hazeState ?: LocalLiquidHazeState.current ?: LocalHazeState.current

    val safeBgColor = remember(effectiveIsDark, isAmoled, screenBgColor) {
        when {
            isAmoled -> Color(0xFF050505)
            screenBgColor != Color.Unspecified && screenBgColor.alpha > 0.05f -> screenBgColor
            effectiveIsDark -> Color(0xFF0F172A)
            else -> Color(0xFFF8FAFC)
        }
    }

    // 1. BACKDROP BLUR & TINT (True translucent frost with guaranteed non-null backgroundColor)
    val hazeStyle = remember(
        effectiveIsDark,
        isAmoled,
        safeBgColor,
        accentColor,
        blurRadius,
        frostTintAlpha
    ) {
        val tintAlpha = frostTintAlpha.coerceIn(0.02f, 0.90f)
        when {
            isAmoled -> HazeStyle(
                backgroundColor = Color.Black,
                tint = HazeTint(accentColor.copy(alpha = (tintAlpha * 0.35f).coerceIn(0.04f, 0.35f))),
                blurRadius = blurRadius
            )
            effectiveIsDark -> HazeStyle(
                backgroundColor = safeBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = tintAlpha)),
                blurRadius = blurRadius
            )
            else -> HazeStyle(
                backgroundColor = safeBgColor,
                tint = HazeTint(Color.White.copy(alpha = tintAlpha)),
                blurRadius = blurRadius
            )
        }
    }

    // 2. TRANSLUCENT GLASS BASE (True translucent permeability)
    val baseGlassSurface = remember(effectiveIsDark, isAmoled, surfaceOpacity, accentColor) {
        val baseAlpha = surfaceOpacity.coerceIn(0.04f, 0.85f)
        when {
            isAmoled -> Brush.verticalGradient(
                listOf(
                    Color(0xFF141414).copy(alpha = (baseAlpha * 0.85f).coerceIn(0.04f, 0.80f)),
                    Color(0xFF070707).copy(alpha = (baseAlpha * 0.55f).coerceIn(0.02f, 0.60f))
                )
            )
            effectiveIsDark -> Brush.verticalGradient(
                listOf(
                    Color(0xFF1E293B).copy(alpha = (baseAlpha * 1.10f).coerceIn(0.05f, 0.85f)),
                    Color(0xFF0F172A).copy(alpha = (baseAlpha * 0.75f).coerceIn(0.03f, 0.70f))
                )
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (baseAlpha * 1.15f).coerceIn(0.06f, 0.85f)),
                    Color.White.copy(alpha = (baseAlpha * 0.70f).coerceIn(0.03f, 0.65f))
                )
            )
        }
    }

    // 3. INNER LENS CURVATURE OCCLUSION
    val innerLensOcclusion = remember(effectiveIsDark, innerDepthStrength) {
        val depthAlpha = innerDepthStrength.coerceIn(0.0f, 1.0f)
        Brush.radialGradient(
            colors = if (effectiveIsDark) {
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (depthAlpha * 0.04f).coerceIn(0.005f, 0.040f)),
                    Color.Black.copy(alpha = (depthAlpha * 0.16f).coerceIn(0.010f, 0.160f))
                )
            } else {
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (depthAlpha * 0.08f).coerceIn(0.010f, 0.080f)),
                    Color.Black.copy(alpha = (depthAlpha * 0.08f).coerceIn(0.008f, 0.080f))
                )
            }
        )
    }

    // 4. SPECULAR TOP-EDGE CREST REFLECTION
    val topCrestReflection = remember(effectiveIsDark, isFocused, accentColor, highlightStrength) {
        val crestAlpha = highlightStrength.coerceIn(0.2f, 1.0f)
        Brush.verticalGradient(
            colors = if (isFocused) {
                listOf(
                    accentColor.copy(alpha = (0.70f * crestAlpha).coerceAtMost(0.95f)),
                    accentColor.copy(alpha = (0.25f * crestAlpha).coerceAtMost(0.40f)),
                    Color.Transparent
                )
            } else if (effectiveIsDark) {
                listOf(
                    Color.White.copy(alpha = (0.50f * crestAlpha).coerceAtMost(0.85f)),
                    Color.White.copy(alpha = (0.15f * crestAlpha).coerceAtMost(0.30f)),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.White.copy(alpha = (0.90f * crestAlpha).coerceAtMost(0.98f)),
                    Color.White.copy(alpha = (0.35f * crestAlpha).coerceAtMost(0.50f)),
                    Color.Transparent
                )
            }
        )
    }

    // 5. SPECULAR LIQUID RIM / BORDER
    val specularRimBrush = remember(effectiveIsDark, isFocused, accentColor, highlightStrength) {
        val rimAlpha = highlightStrength.coerceIn(0.2f, 1.0f)
        if (isFocused) {
            Brush.verticalGradient(
                listOf(
                    accentColor.copy(alpha = (0.95f * rimAlpha).coerceAtMost(1.0f)),
                    accentColor.copy(alpha = (0.45f * rimAlpha).coerceAtMost(0.60f)),
                    accentColor.copy(alpha = (0.20f * rimAlpha).coerceAtMost(0.30f))
                )
            )
        } else if (effectiveIsDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.48f * rimAlpha).coerceAtMost(0.75f)),
                    Color.White.copy(alpha = (0.18f * rimAlpha).coerceAtMost(0.32f)),
                    Color.White.copy(alpha = (0.06f * rimAlpha).coerceAtMost(0.14f))
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.92f * rimAlpha).coerceAtMost(0.98f)),
                    Color.White.copy(alpha = (0.40f * rimAlpha).coerceAtMost(0.55f)),
                    Color(0xFFCBD5E1).copy(alpha = (0.22f * rimAlpha).coerceAtMost(0.35f))
                )
            )
        }
    }

    // 6. AMBIENT & SPOT ELEVATION SHADOWS
    val shadowAmbient = if (effectiveIsDark) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.05f)
    }
    val shadowSpot = if (effectiveIsDark) {
        Color.Black.copy(alpha = 0.40f)
    } else {
        accentColor.copy(alpha = 0.08f)
    }

    return this
        // Soft elevation shadow
        .then(
            if (effectiveElevation > 0.dp) {
                Modifier.shadow(
                    elevation = effectiveElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowAmbient,
                    spotColor = shadowSpot
                )
            } else {
                Modifier
            }
        )
        // Clip to exact shape boundary
        .clip(shape)
        // LAYER 1: Hardware Backdrop Blur via Haze
        .then(
            if (effectiveHazeState != null) {
                Modifier.hazeEffect(state = effectiveHazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        // LAYER 2: Translucent Base Surface & Inner Lens Occlusion
        .background(brush = baseGlassSurface, shape = shape)
        .background(brush = innerLensOcclusion, shape = shape)
        // LAYER 3: Render Crisp Content, then overlay Specular Top-Edge Reflection
        .drawWithContent {
            // Draw crisp unblurred child content
            drawContent()

            // Draw upper optical specular light crest (top 4.5dp)
            val outline = shape.createOutline(size, layoutDirection, this)
            clipPath(
                path = Path().apply {
                    addOutline(outline)
                }
            ) {
                val crestHeight = 4.5.dp.toPx()
                drawRect(
                    brush = topCrestReflection,
                    topLeft = Offset.Zero,
                    size = Size(size.width, crestHeight)
                )

                if (isFocused) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.04f), Color.Transparent)
                        )
                    )
                }
            }
        }
        // Specular Liquid Rim Border
        .border(
            width = effectiveBorderWidth,
            brush = specularRimBrush,
            shape = shape
        )
}
