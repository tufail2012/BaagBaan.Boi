package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
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
    customPaletteColor: Color? = null,
    defaultColor: Color = SectionLocalPlantsAccent
): Color {
    if (customPaletteColor != null) return customPaletteColor
    return when (serviceCategory.trim()) {
        "Local Plants", "Local" -> SectionLocalPlantsAccent
        "Imported Plants", "Imported" -> SectionImportedPlantsAccent
        "Rootstocks" -> SectionRootstocksAccent
        "Site Visit", "Visit" -> SectionSiteVisitAccent
        "Pruning" -> SectionPruningAccent
        "Garden Planning", "Planning" -> SectionGardenPlanningAccent
        "Bookings" -> SectionBookingsAccent
        "Attendance" -> SectionAttendanceAccent
        "Inventory" -> SectionInventoryAccent
        "Dashboard" -> Color(0xFF10B981)
        "Contact Directory", "Contacts" -> Color(0xFF0EA5E9)
        "Payment Reminder", "Payment Reminders" -> Color(0xFFF59E0B)
        "Seasonal Reminders", "Seasonal" -> Color(0xFF10B981)
        "Scan QR", "QR" -> Color(0xFF8B5CF6)
        "Settings" -> Color(0xFF64748B)
        "Profile" -> Color(0xFF10B981)
        else -> defaultColor
    }
}

/**
 * Transforms an accent/palette color into a very dim, subdued, low-saturation,
 * and low-luminance background tone while preserving its recognizable hue.
 */
fun getAppDimBackgroundColor(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
): Color {
    val r = (accentColor.red * 255f).toInt().coerceIn(0, 255)
    val g = (accentColor.green * 255f).toInt().coerceIn(0, 255)
    val b = (accentColor.blue * 255f).toInt().coerceIn(0, 255)
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    val hue = hsv[0]

    return when {
        isAmoled -> {
            // Pure pitch black background for AMOLED Mode
            Color(0xFF000000)
        }
        isDark -> {
            // Refined dark charcoal gray background for standard Dark Mode with a subtle tint
            val darkTone = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.08f, 0.09f))
            Color(darkTone)
        }
        else -> {
            val toneRgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.055f, 0.950f))
            Color(toneRgb)
        }
    }
}

/**
 * Computes a soft vertical gradient brush using very dim, subdued, low-saturation,
 * and low-luminance tones derived strictly from the selected accent / palette color.
 */
fun getAppDimBackgroundBrush(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
): Brush {
    val r = (accentColor.red * 255f).toInt().coerceIn(0, 255)
    val g = (accentColor.green * 255f).toInt().coerceIn(0, 255)
    val b = (accentColor.blue * 255f).toInt().coerceIn(0, 255)
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    val hue = hsv[0]

    return when {
        isAmoled -> {
            // Pure pitch black background for AMOLED Mode
            Brush.verticalGradient(
                colors = listOf(Color(0xFF000000), Color(0xFF000000))
            )
        }
        isDark -> {
            // Elegant dark slate/charcoal gray gradient for standard Dark Mode (non-pure-black)
            val topColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.09f, 0.12f)))
            val midColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.07f, 0.095f)))
            val bottomColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.05f, 0.08f)))
            Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor)
            )
        }
        else -> {
            val topColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.065f, 0.945f)))
            val midColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.045f, 0.960f)))
            val bottomColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.025f, 0.975f)))
            Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor)
            )
        }
    }
}

val AgriBackground = Color(0xFFF8F9FA)
val AgriSurface = Color(0xFFFFFFFF)
val AgriOutline = Color(0xFFE0E0E0)

val AgriTextPrimary = Color(0xFF212121)
val AgriTextSecondary = Color(0xFF666666)
val AgriAccentPill = Color(0xFFFFF0F0)

