package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector

val PRESET_ICON_COLORS: List<Color> = listOf(
    Color(0xFF2563EB), // 0: blue
    Color(0xFF16A34A), // 1: green
    Color(0xFFEA580C), // 2: orange
    Color(0xFFDC2626), // 3: red
    Color(0xFF9333EA), // 4: purple
    Color(0xFF0D9488), // 5: teal
    Color(0xFFD97706), // 6: amber
    Color(0xFFDB2777), // 7: pink
    Color(0xFF4F46E5), // 8: indigo
    Color(0xFF0891B2), // 9: cyan
    Color(0xFF65A30D), // 10: lime
    Color(0xFF92400E), // 11: brown
    Color(0xFF475569), // 12: slate
    Color(0xFFBE123B), // 13: rose
    Color(0xFF0E7490), // 14: dark cyan
    Color(0xFF7C3AED)  // 15: violet
)

private val COLOR_BLUE = Color(0xFF2563EB)
private val COLOR_GREEN = Color(0xFF16A34A)
private val COLOR_RED = Color(0xFFDC2626)
private val COLOR_TEAL = Color(0xFF0D9488)
private val COLOR_AMBER = Color(0xFFD97706)
private val COLOR_PINK = Color(0xFFDB2777)
private val COLOR_INDIGO = Color(0xFF4F46E5)
private val COLOR_CYAN = Color(0xFF0891B2)
private val COLOR_LIME = Color(0xFF65A30D)
private val COLOR_SLATE = Color(0xFF475569)
private val COLOR_ROSE = Color(0xFFBE123B)
private val COLOR_VIOLET = Color(0xFF7C3AED)

val EXPLICIT_ICON_COLORS: Map<String, Color> = mapOf(
    // Dashboard: blue
    "Filled.Dashboard" to COLOR_BLUE,
    "Outlined.Dashboard" to COLOR_BLUE,
    "Dashboard" to COLOR_BLUE,
    // Inventory2 or Inventory: amber
    "Filled.Inventory2" to COLOR_AMBER,
    "Outlined.Inventory2" to COLOR_AMBER,
    "Inventory2" to COLOR_AMBER,
    "Filled.Inventory" to COLOR_AMBER,
    "Outlined.Inventory" to COLOR_AMBER,
    "Inventory" to COLOR_AMBER,
    // EventAvailable or the glyph used for Attendance: green
    "Filled.EventAvailable" to COLOR_GREEN,
    "Outlined.EventAvailable" to COLOR_GREEN,
    "EventAvailable" to COLOR_GREEN,
    // ContactPage or the glyph used for Contact Directory (Contacts): teal
    "Filled.ContactPage" to COLOR_TEAL,
    "Outlined.ContactPage" to COLOR_TEAL,
    "ContactPage" to COLOR_TEAL,
    "Filled.Contacts" to COLOR_TEAL,
    "Outlined.Contacts" to COLOR_TEAL,
    "Contacts" to COLOR_TEAL,
    // ReceiptLong or the glyph used for Payment Reminder: red
    "Filled.ReceiptLong" to COLOR_RED,
    "Outlined.ReceiptLong" to COLOR_RED,
    "ReceiptLong" to COLOR_RED,
    // Park, Forest or the tree glyph used for Seasonal Reminders: lime
    "Filled.Park" to COLOR_LIME,
    "Outlined.Park" to COLOR_LIME,
    "Park" to COLOR_LIME,
    "Filled.Forest" to COLOR_LIME,
    "Outlined.Forest" to COLOR_LIME,
    "Forest" to COLOR_LIME,
    // QrCodeScanner or the glyph used for Scan QR: indigo
    "Filled.QrCodeScanner" to COLOR_INDIGO,
    "Outlined.QrCodeScanner" to COLOR_INDIGO,
    "QrCodeScanner" to COLOR_INDIGO,
    // Chat: violet
    "AutoMirrored.Filled.Chat" to COLOR_VIOLET,
    "AutoMirrored.Outlined.Chat" to COLOR_VIOLET,
    "Filled.Chat" to COLOR_VIOLET,
    "Outlined.Chat" to COLOR_VIOLET,
    "Chat" to COLOR_VIOLET,
    // Settings: slate
    "Filled.Settings" to COLOR_SLATE,
    "Outlined.Settings" to COLOR_SLATE,
    "Settings" to COLOR_SLATE,
    // Notifications: pink
    "Filled.Notifications" to COLOR_PINK,
    "Outlined.Notifications" to COLOR_PINK,
    "Notifications" to COLOR_PINK,
    // Search: cyan
    "Filled.Search" to COLOR_CYAN,
    "Outlined.Search" to COLOR_CYAN,
    "Search" to COLOR_CYAN,
    // Call or Phone: rose
    "Filled.Call" to COLOR_ROSE,
    "Outlined.Call" to COLOR_ROSE,
    "Call" to COLOR_ROSE,
    "Filled.Phone" to COLOR_ROSE,
    "Outlined.Phone" to COLOR_ROSE,
    "Phone" to COLOR_ROSE
)

fun presetIconColor(icon: ImageVector, isDark: Boolean): Color {
    val name = icon.name
    val shortName = name.substringAfterLast('.')
    val baseColor = EXPLICIT_ICON_COLORS[name]
        ?: EXPLICIT_ICON_COLORS[shortName]
        ?: PRESET_ICON_COLORS[(name.hashCode() and 0x7fffffff) % PRESET_ICON_COLORS.size]
    return if (isDark) {
        lerp(baseColor, Color.White, 0.25f)
    } else {
        baseColor
    }
}
