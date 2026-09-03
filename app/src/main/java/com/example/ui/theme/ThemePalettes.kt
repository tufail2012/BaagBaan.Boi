package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Calculates high-contrast accessible text/icon color for any surface.
 */
fun getAccessibleTextColor(
    surfaceColor: Color,
    fallbackLight: Color = Color(0xFFFAFAFA),
    fallbackDark: Color = Color(0xFF0F172A)
): Color {
    val lum = surfaceColor.luminance()
    return if (lum > 0.48f) fallbackDark else fallbackLight
}

/**
 * Predefined 4-color and 2-color App Palettes
 *
 * COLOR ROLES:
 * - PRIMARY: Main/primary action buttons, selected tab text/icons, active navigation icon,
 *            selected navigation pill, important action icons (Add, Save, Edit, Refresh),
 *            focused input-field border/glow, active indicators, important interactive highlights.
 * - SECONDARY: Supporting card surfaces, input-field surface tint, secondary button backgrounds,
 *              chips, supporting containers, subtle UI surface accents.
 * - TERTIARY: Status badges, notification accents, small visual highlights, supporting indicators,
 *             secondary decorative accents, subtle gradient/reflection details.
 * - NEUTRAL: General body text, form values, inactive tab text, inactive icons, borders, dividers,
 *            structural UI elements, contrast areas.
 */
data class AppPalette(
    val id: String,
    val name: String,
    val isTwoColor: Boolean = false,
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String = secondaryHex,
    val neutralHex: String = secondaryHex,
    val primaryName: String = "",
    val secondaryName: String = "",
    val tertiaryName: String = "",
    val neutralName: String = ""
) {
    val isPredefinedPalette: Boolean
        get() = PredefinedThemePalettes.any { it.id.equals(id, ignoreCase = true) }

    val primary: Color get() = try {
        Color(android.graphics.Color.parseColor(primaryHex))
    } catch (e: Exception) {
        Color(0xFFD32F2F)
    }

    val secondary: Color get() = try {
        Color(android.graphics.Color.parseColor(secondaryHex))
    } catch (e: Exception) {
        Color(0xFFFB923C)
    }

    val tertiary: Color get() = try {
        Color(android.graphics.Color.parseColor(tertiaryHex))
    } catch (e: Exception) {
        Color(0xFF10B981)
    }

    val neutral: Color get() = try {
        Color(android.graphics.Color.parseColor(neutralHex))
    } catch (e: Exception) {
        Color(0xFFE2E8F0)
    }

    /**
     * Resolves the Primary color for interactive components across light, dark, and AMOLED modes.
     * In 2-color palettes (Monochrome, Black & White), switches between White (in Dark/AMOLED)
     * and Black (in Light) to guarantee perfect contrast and readability.
     */
    fun getPrimary(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isDark || isAmoled) Color.White else Color(0xFF09090B)
        } else {
            primary
        }
    }

    /**
     * Resolves the Secondary color for supporting surfaces, chips, and containers.
     */
    fun getSecondary(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isDark || isAmoled) Color(0xFF18181B) else Color(0xFFF4F4F5)
        } else {
            secondary
        }
    }

    /**
     * Resolves the Tertiary color for status badges, notification accents, and highlights.
     */
    fun getTertiary(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isDark || isAmoled) Color(0xFFA1A1AA) else Color(0xFF52525B)
        } else {
            tertiary
        }
    }

    /**
     * Resolves the Neutral color.
     */
    fun getNeutral(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isDark || isAmoled) Color.White else Color(0xFF0F172A)
        } else {
            neutral
        }
    }

    /**
     * Resolves high-contrast readable text for body, headings, and form values.
     * Guaranteed never to produce dark text on dark surfaces or light text on light surfaces.
     */
    fun getNeutralTextColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color(0xFFFAFAFA) else {
                if (neutral.luminance() > 0.45f) neutral else Color(0xFFF8FAFC)
            }
        } else {
            if (isTwoColor) Color(0xFF09090B) else Color(0xFF0F172A)
        }
    }

    /**
     * Resolves secondary/muted text color for inactive tabs, subtitles, and labels.
     */
    fun getNeutralMutedTextColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color(0xFFD4D4D8) else Color(0xFFCBD5E1)
        } else {
            if (isTwoColor) Color(0xFF52525B) else Color(0xFF475569)
        }
    }

    /**
     * Resolves border and divider colors with high structural clarity.
     */
    fun getNeutralBorderColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color(0xFF3F3F46) else neutral.copy(alpha = 0.35f)
        } else {
            if (isTwoColor) Color(0xFFE4E4E7) else neutral.copy(alpha = 0.40f)
        }
    }

    /**
     * Resolves inactive icon color.
     */
    fun getNeutralIconColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color(0xFFD4D4D8) else Color(0xFF94A3B8)
        } else {
            if (isTwoColor) Color(0xFF52525B) else Color(0xFF64748B)
        }
    }

    /**
     * Resolves supporting card background tint.
     */
    fun getSupportingCardTint(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isAmoled) Color(0xFF0A0A0A) else if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
        } else {
            if (isDark) secondary.copy(alpha = 0.12f) else secondary.copy(alpha = 0.08f)
        }
    }

    /**
     * Resolves input field inner surface tint.
     */
    fun getInputSurfaceTint(isDark: Boolean, isAmoled: Boolean = false): Color {
        return if (isTwoColor) {
            if (isDark || isAmoled) Color(0xFF141416) else Color(0xFFF9FAFB)
        } else {
            if (isDark) secondary.copy(alpha = 0.08f) else secondary.copy(alpha = 0.06f)
        }
    }
}

/**
 * 8 Predefined Theme Palettes
 */
val PredefinedThemePalettes = listOf(
    // 1. Apple Bloom
    AppPalette(
        id = "apple_bloom",
        name = "1. Apple Bloom",
        isTwoColor = false,
        primaryHex = "#E53935",
        secondaryHex = "#FF8A65",
        tertiaryHex = "#4CAF50",
        neutralHex = "#FFF8E7",
        primaryName = "Apple Red",
        secondaryName = "Coral Peach",
        tertiaryName = "Leaf Green",
        neutralName = "Warm Cream"
    ),
    // 2. Royal Garden
    AppPalette(
        id = "royal_garden",
        name = "2. Royal Garden",
        isTwoColor = false,
        primaryHex = "#7C3AED",
        secondaryHex = "#C084FC",
        tertiaryHex = "#10B981",
        neutralHex = "#EDE9FE",
        primaryName = "Royal Purple",
        secondaryName = "Soft Orchid",
        tertiaryName = "Garden Green",
        neutralName = "Pale Lavender"
    ),
    // 3. Ocean Breeze
    AppPalette(
        id = "ocean_breeze",
        name = "3. Ocean Breeze",
        isTwoColor = false,
        primaryHex = "#0284C7",
        secondaryHex = "#06B6D4",
        tertiaryHex = "#2DD4BF",
        neutralHex = "#E0F2FE",
        primaryName = "Ocean Blue",
        secondaryName = "Aqua Teal",
        tertiaryName = "Seafoam Green",
        neutralName = "Soft Sky"
    ),
    // 4. Tropical Garden
    AppPalette(
        id = "tropical_garden",
        name = "4. Tropical Garden",
        isTwoColor = false,
        primaryHex = "#0F766E",
        secondaryHex = "#06B6D4",
        tertiaryHex = "#EAB308",
        neutralHex = "#BEF264",
        primaryName = "Emerald Teal",
        secondaryName = "Tropical Cyan",
        tertiaryName = "Golden Yellow",
        neutralName = "Fresh Lime"
    ),
    // 5. Sunset Orchard
    AppPalette(
        id = "sunset_orchard",
        name = "5. Sunset Orchard",
        isTwoColor = false,
        primaryHex = "#EA580C",
        secondaryHex = "#FB7185",
        tertiaryHex = "#F59E0B",
        neutralHex = "#FFEDD5",
        primaryName = "Sunset Orange",
        secondaryName = "Coral Pink",
        tertiaryName = "Golden Amber",
        neutralName = "Peach Cream"
    ),
    // 6. Berry Blossom
    AppPalette(
        id = "berry_blossom",
        name = "6. Berry Blossom",
        isTwoColor = false,
        primaryHex = "#DB2777",
        secondaryHex = "#8B5CF6",
        tertiaryHex = "#F472B6",
        neutralHex = "#FCE7F3",
        primaryName = "Berry Magenta",
        secondaryName = "Soft Violet",
        tertiaryName = "Rose Pink",
        neutralName = "Pale Blush"
    ),
    // 7. Monochrome
    AppPalette(
        id = "monochrome",
        name = "7. Monochrome",
        isTwoColor = true,
        primaryHex = "#18181B",
        secondaryHex = "#F4F4F5",
        tertiaryHex = "#71717A",
        neutralHex = "#D4D4D8",
        primaryName = "Charcoal Black",
        secondaryName = "Clean Off-White",
        tertiaryName = "Mid Slate",
        neutralName = "Zinc"
    ),
    // 8. Black & White
    AppPalette(
        id = "black_and_white",
        name = "8. Black & White",
        isTwoColor = true,
        primaryHex = "#000000",
        secondaryHex = "#FFFFFF",
        tertiaryHex = "#FFFFFF",
        neutralHex = "#FFFFFF",
        primaryName = "Pure Black",
        secondaryName = "Pure White",
        tertiaryName = "Pure White",
        neutralName = "Pure Contrast"
    )
)

/**
 * Finds predefined palette by its ID.
 */
fun getPredefinedPaletteById(id: String?): AppPalette? {
    if (id == null) return null
    return PredefinedThemePalettes.firstOrNull { it.id.equals(id, ignoreCase = true) }
}

/**
 * Builds an AppPalette from either a selected palette ID or a fallback solid color hex.
 */
fun resolveAppPalette(paletteId: String?, solidAccentHex: String): AppPalette {
    val predefined = getPredefinedPaletteById(paletteId)
    if (predefined != null) return predefined

    // Construct a harmonious fallback palette from the solid color
    return AppPalette(
        id = "solid_${solidAccentHex.lowercase()}",
        name = "Solid Color",
        isTwoColor = false,
        primaryHex = solidAccentHex,
        secondaryHex = solidAccentHex,
        tertiaryHex = solidAccentHex,
        neutralHex = "#E2E8F0",
        primaryName = "Accent",
        secondaryName = "Secondary",
        tertiaryName = "Tertiary",
        neutralName = "Neutral"
    )
}

val LocalAppPalette = compositionLocalOf {
    PredefinedThemePalettes[0]
}

