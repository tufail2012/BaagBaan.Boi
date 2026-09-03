package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Predefined 4-color and 2-color App Palettes
 *
 * COLOR ROLES:
 * - PRIMARY: Main/primary buttons, selected tab text/icons, active navigation icon,
 *            selected navigation pill, important action icons (Add, Save, Edit, Refresh),
 *            focused input-field border/glow, active indicators, important highlights.
 * - SECONDARY: Secondary button backgrounds, supporting cards and containers, soft
 *              input-field surface tints, chips, supporting UI surfaces, subtle background accents.
 * - TERTIARY: Status badges, notification accents, small highlights, supporting indicators,
 *             decorative accents, subtle gradient/reflection details.
 * - NEUTRAL: General text, form values, inactive text, inactive icons, borders, dividers,
 *            supporting structural elements. (Neutral is a ROLE, not a requirement to use black or gray).
 */
data class AppPalette(
    val id: String,
    val name: String,
    val isTwoColor: Boolean = false,
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String = secondaryHex,
    val neutralHex: String = secondaryHex,
    val primaryName: String,
    val secondaryName: String,
    val tertiaryName: String = "",
    val neutralName: String = ""
) {
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
     * Resolves an adaptive contrast text color for the Neutral role in dark or light mode.
     */
    fun getNeutralTextColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color.White else neutral
        } else {
            if (isTwoColor) Color(0xFF18181B) else Color(0xFF1E293B)
        }
    }

    /**
     * Resolves an adaptive border/divider color for the Neutral role in dark or light mode.
     */
    fun getNeutralBorderColor(isDark: Boolean): Color {
        return if (isDark) {
            if (isTwoColor) Color(0xFF3F3F46) else neutral.copy(alpha = 0.35f)
        } else {
            if (isTwoColor) Color(0xFFE4E4E7) else neutral.copy(alpha = 0.45f)
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
        tertiaryName = "Fresh Leaf Green",
        neutralName = "Warm Cream"
    ),
    // 2. Royal Garden
    AppPalette(
        id = "royal_garden",
        name = "2. Royal Garden",
        isTwoColor = false,
        primaryHex = "#7C3AED",
        secondaryHex = "#C084FC",
        tertiaryHex = "#15803D",
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
        secondaryHex = "#14B8A6",
        tertiaryHex = "#5EEAD4",
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
        primaryHex = "#059669",
        secondaryHex = "#0D9488",
        tertiaryHex = "#FACC15",
        neutralHex = "#BEF264",
        primaryName = "Emerald Green",
        secondaryName = "Tropical Teal",
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
        secondaryName = "Violet",
        tertiaryName = "Soft Rose",
        neutralName = "Pale Pink"
    ),
    // 7. Monochrome
    AppPalette(
        id = "monochrome",
        name = "7. Monochrome",
        isTwoColor = true,
        primaryHex = "#212121",
        secondaryHex = "#F4F4F5",
        primaryName = "Black",
        secondaryName = "White"
    ),
    // 8. Black & White
    AppPalette(
        id = "black_and_white",
        name = "8. Black & White",
        isTwoColor = true,
        primaryHex = "#000000",
        secondaryHex = "#FFFFFF",
        primaryName = "Pure Black",
        secondaryName = "Pure White"
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
