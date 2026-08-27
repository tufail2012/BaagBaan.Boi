package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val AgriRedPrimary = Color(0xFFD32F2F)
val AgriRedLight = Color(0xFFFFEBEE)
val AgriRedDark = Color(0xFF9A0007)

val AgriGreenPrimary = Color(0xFF2E7D32)
val AgriGreenLight = Color(0xFFE8F5E9)

// Section-specific brand accent colors
val SectionLocalPlantsAccent = Color(0xFF2E7D32)     // Forest Green
val SectionImportedPlantsAccent = Color(0xFF7C3AED)  // Vibrant Purple
val SectionRootstocksAccent = Color(0xFF059669)      // Emerald Green
val SectionSiteVisitAccent = Color(0xFF2563EB)       // Royal Blue
val SectionPruningAccent = Color(0xFFD32F2F)         // Crimson Red
val SectionGardenPlanningAccent = Color(0xFF047857)  // Teal / Deep Forest
val SectionBookingsAccent = Color(0xFF6366F1)        // Indigo
val SectionAttendanceAccent = Color(0xFFEA580C)      // Deep Orange
val SectionInventoryAccent = Color(0xFF0891B2)       // Cyan

/**
 * Returns the definitive section accent color for a given service category or app section.
 * This is the SINGLE SOURCE OF TRUTH used across Segmented Controls, Sub-Tabs, Headers,
 * Bottom Navigation, Badges, Glows, and Card Highlights.
 */
fun getSectionAccentColor(
    serviceCategory: String,
    defaultColor: Color = SectionLocalPlantsAccent,
    customPaletteColor: Color? = null
): Color {
    return customPaletteColor ?: defaultColor
}

val AgriBackground = Color(0xFFF8F9FA)
val AgriSurface = Color(0xFFFFFFFF)
val AgriOutline = Color(0xFFE0E0E0)

val AgriTextPrimary = Color(0xFF212121)
val AgriTextSecondary = Color(0xFF666666)
val AgriAccentPill = Color(0xFFFFF0F0)

