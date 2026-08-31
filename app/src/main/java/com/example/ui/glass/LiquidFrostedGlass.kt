package com.example.ui.glass

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AppThemeMode
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
 * AGSL Shader source for true Optical Liquid Lens Refraction and Chromatic Dispersion.
 * Active on Android 13+ (API 33 / Tiramisu), gracefully skipped on older Android APIs.
 */
private const val LIQUID_GLASS_AGSL = """
    uniform shader image;
    uniform float2 resolution;
    uniform float refraction;
    uniform float chromaticAberration;
    uniform float innerDepth;

    half4 main(float2 coord) {
        if (resolution.x <= 0.0 || resolution.y <= 0.0) {
            return image.eval(coord);
        }
        
        float2 uv = coord / resolution;
        float2 center = float2(0.5, 0.5);
        float2 delta = uv - center;
        float dist = length(delta);
        
        // Physical lens curvature displacement near borders
        float edgeFactor = smoothstep(0.20, 0.50, dist);
        float2 displacement = delta * edgeFactor * refraction * 4.0;
        
        if (chromaticAberration > 0.005) {
            float2 rOffset = displacement * (1.0 + chromaticAberration * 2.0);
            float2 gOffset = displacement;
            float2 bOffset = displacement * (1.0 - chromaticAberration * 2.0);
            
            float r = image.eval(coord + rOffset).r;
            float g = image.eval(coord + gOffset).g;
            float b = image.eval(coord + bOffset).b;
            float a = image.eval(coord + gOffset).a;
            
            half4 color = half4(r, g, b, a);
            float depthOcclusion = 1.0 - (edgeFactor * innerDepth * 0.10);
            return color * depthOcclusion;
        } else {
            half4 color = image.eval(coord + displacement);
            float depthOcclusion = 1.0 - (edgeFactor * innerDepth * 0.10);
            return color * depthOcclusion;
        }
    }
"""

/**
 * Unified Central Frosted Liquid Glass Engine.
 *
 * RENDERING PIPELINE:
 * 1. ACTUAL BACKDROP: Real background canvas captured via root `hazeSource`.
 * 2. BACKDROP FROST & BLUR: Hardware-accelerated Haze backdrop blur (24-28dp) with calibrated tinting.
 * 3. OPTICAL PROCESSING: AGSL RuntimeShader lens curvature and chromatic edge dispersion on API 33+.
 * 4. TRANSLUCENT GLASS BASE: Subtle, non-milky surface gradient letting 100% of backdrop colors shine through.
 * 5. SPECULAR LIGHTING & DEPTH: Top-edge crest reflection, soft inner lens occlusion, and ambient elevation shadow.
 * 6. CRISP CONTENT: 100% sharp text, icons, and UI controls.
 */
@Composable
fun Modifier.liquidFrostedGlass(
    hazeState: HazeState? = LocalLiquidHazeState.current,
    isDark: Boolean = isSystemInDarkTheme(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 24.dp,
    frostTintAlpha: Float = 0.08f,
    surfaceOpacity: Float = 0.10f,
    refractionStrength: Float = 0.30f,
    chromaticAberration: Float = 0.06f,
    innerDepthStrength: Float = 0.25f,
    highlightStrength: Float = 0.70f,
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

    // 1. BACKDROP BLUR & TINT
    val hazeStyle = remember(
        effectiveIsDark,
        isAmoled,
        accentColor,
        screenBgColor,
        blurRadius,
        frostTintAlpha
    ) {
        when {
            isAmoled -> HazeStyle(
                backgroundColor = Color.Black,
                tint = HazeTint(accentColor.copy(alpha = (frostTintAlpha * 0.85f).coerceIn(0.02f, 0.10f))),
                blurRadius = blurRadius
            )
            effectiveIsDark -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = frostTintAlpha.coerceIn(0.04f, 0.12f))),
                blurRadius = blurRadius
            )
            else -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color.White.copy(alpha = frostTintAlpha.coerceIn(0.04f, 0.14f))),
                blurRadius = blurRadius
            )
        }
    }

    // 2. TRANSLUCENT GLASS BASE (True translucent permeability)
    val baseGlassSurface = remember(effectiveIsDark, isAmoled, surfaceOpacity, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                listOf(
                    Color(0xFF141414).copy(alpha = (surfaceOpacity * 0.80f).coerceIn(0.04f, 0.10f)),
                    Color(0xFF070707).copy(alpha = (surfaceOpacity * 0.50f).coerceIn(0.02f, 0.06f))
                )
            )
            effectiveIsDark -> Brush.verticalGradient(
                listOf(
                    Color(0xFF1E293B).copy(alpha = (surfaceOpacity * 1.10f).coerceIn(0.05f, 0.14f)),
                    Color(0xFF0F172A).copy(alpha = (surfaceOpacity * 0.70f).coerceIn(0.03f, 0.09f))
                )
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (surfaceOpacity * 1.20f).coerceIn(0.06f, 0.15f)),
                    Color.White.copy(alpha = (surfaceOpacity * 0.65f).coerceIn(0.03f, 0.08f))
                )
            )
        }
    }

    // 3. INNER LENS CURVATURE OCCLUSION
    val innerLensOcclusion = remember(effectiveIsDark, innerDepthStrength) {
        Brush.radialGradient(
            colors = if (effectiveIsDark) {
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (innerDepthStrength * 0.04f).coerceIn(0.005f, 0.020f)),
                    Color.Black.copy(alpha = (innerDepthStrength * 0.12f).coerceIn(0.010f, 0.045f))
                )
            } else {
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (innerDepthStrength * 0.08f).coerceIn(0.010f, 0.035f)),
                    Color.Black.copy(alpha = (innerDepthStrength * 0.08f).coerceIn(0.008f, 0.030f))
                )
            }
        )
    }

    // 4. SPECULAR TOP-EDGE CREST REFLECTION
    val topCrestReflection = remember(effectiveIsDark, isFocused, accentColor, highlightStrength) {
        val crestAlpha = highlightStrength.coerceIn(0.3f, 1.0f)
        Brush.verticalGradient(
            colors = if (isFocused) {
                listOf(
                    accentColor.copy(alpha = (0.60f * crestAlpha).coerceAtMost(0.95f)),
                    accentColor.copy(alpha = (0.20f * crestAlpha).coerceAtMost(0.40f)),
                    Color.Transparent
                )
            } else if (effectiveIsDark) {
                listOf(
                    Color.White.copy(alpha = (0.45f * crestAlpha).coerceAtMost(0.80f)),
                    Color.White.copy(alpha = (0.12f * crestAlpha).coerceAtMost(0.25f)),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.White.copy(alpha = (0.85f * crestAlpha).coerceAtMost(0.95f)),
                    Color.White.copy(alpha = (0.22f * crestAlpha).coerceAtMost(0.35f)),
                    Color.Transparent
                )
            }
        )
    }

    // 5. SPECULAR LIQUID RIM / BORDER
    val specularRimBrush = remember(effectiveIsDark, isFocused, accentColor, highlightStrength) {
        val rimAlpha = highlightStrength.coerceIn(0.3f, 1.0f)
        if (isFocused) {
            Brush.verticalGradient(
                listOf(
                    accentColor.copy(alpha = (0.90f * rimAlpha).coerceAtMost(1.0f)),
                    accentColor.copy(alpha = (0.45f * rimAlpha).coerceAtMost(0.60f)),
                    accentColor.copy(alpha = (0.20f * rimAlpha).coerceAtMost(0.30f))
                )
            )
        } else if (effectiveIsDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.42f * rimAlpha).coerceAtMost(0.70f)),
                    Color.White.copy(alpha = (0.14f * rimAlpha).coerceAtMost(0.25f)),
                    Color.White.copy(alpha = (0.04f * rimAlpha).coerceAtMost(0.10f))
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.85f * rimAlpha).coerceAtMost(0.95f)),
                    Color.White.copy(alpha = (0.30f * rimAlpha).coerceAtMost(0.45f)),
                    Color(0xFFCBD5E1).copy(alpha = (0.16f * rimAlpha).coerceAtMost(0.25f))
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

    // 7. RUNTIME AGSL SHADER (API 33+)
    val runtimeShader = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RuntimeShader(LIQUID_GLASS_AGSL)
            } else null
        } catch (e: Throwable) {
            null
        }
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
        // AGSL Optical Lens Refraction (API 33+)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null && refractionStrength > 0.01f) {
                Modifier.graphicsLayer {
                    try {
                        runtimeShader.setFloatUniform("resolution", size.width, size.height)
                        runtimeShader.setFloatUniform("refraction", refractionStrength)
                        runtimeShader.setFloatUniform("chromaticAberration", chromaticAberration)
                        runtimeShader.setFloatUniform("innerDepth", innerDepthStrength)
                        val effect = android.graphics.RenderEffect.createRuntimeShaderEffect(runtimeShader, "image")
                        this.renderEffect = effect.asComposeRenderEffect()
                    } catch (e: Throwable) {
                        // Safe fallback on hardware without AGSL support
                    }
                }
            } else {
                Modifier
            }
        )
        // LAYER 1: Hardware Backdrop Blur via Haze
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
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
