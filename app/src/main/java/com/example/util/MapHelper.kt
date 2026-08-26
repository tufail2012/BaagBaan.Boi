package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapHelper {

    private val GOOGLE_MAPS_REGEX = Regex(
        """^(https?://)?((www\.)?google\.[a-z.]+/maps|maps\.google\.[a-z.]+|maps\.app\.goo\.gl|goo\.gl/maps)(/.*|\?.*)?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Determines whether the given string represents a valid Google Maps link.
     */
    fun isGoogleMapsUrl(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val trimmed = raw.trim()
        return GOOGLE_MAPS_REGEX.matches(trimmed) ||
                trimmed.startsWith("https://maps.google.", ignoreCase = true) ||
                trimmed.startsWith("http://maps.google.", ignoreCase = true) ||
                trimmed.startsWith("maps.google.", ignoreCase = true) ||
                trimmed.startsWith("https://www.google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("http://www.google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("www.google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("https://google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("http://google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("google.com/maps", ignoreCase = true) ||
                trimmed.startsWith("https://maps.app.goo.gl", ignoreCase = true) ||
                trimmed.startsWith("http://maps.app.goo.gl", ignoreCase = true) ||
                trimmed.startsWith("maps.app.goo.gl", ignoreCase = true) ||
                trimmed.startsWith("https://goo.gl/maps", ignoreCase = true) ||
                trimmed.startsWith("http://goo.gl/maps", ignoreCase = true) ||
                trimmed.startsWith("goo.gl/maps", ignoreCase = true)
    }

    /**
     * Ensures the link has a valid http/https scheme so it can be resolved by Android Intents.
     */
    fun sanitizeMapsUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    /**
     * Opens the Google Maps link using the Google Maps app if installed, or falls back to the browser.
     */
    fun openGoogleMaps(context: Context, rawUrl: String) {
        if (rawUrl.isBlank()) return
        val fullUrl = sanitizeMapsUrl(rawUrl)
        val uri = Uri.parse(fullUrl)

        // 1. Try launching specifically with Google Maps app package first
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(mapsIntent)
        } catch (_: Exception) {
            // 2. Fallback: Open with default browser or any handler
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open map location: ${e.localizedMessage ?: "App not found"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
