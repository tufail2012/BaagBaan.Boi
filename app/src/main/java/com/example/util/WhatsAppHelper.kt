package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    /**
     * Safely normalizes a phone number into international format without '+' or spaces.
     * Examples:
     * - "+91 9876543210" -> "919876543210"
     * - "9876543210" -> "919876543210"
     * - "0919876543210" -> "919876543210"
     * - "09876543210" -> "919876543210"
     * - "+919876543210" -> "919876543210"
     * - "919876543210" -> "919876543210"
     * - "+1 415 555 2671" -> "14155552671"
     */
    fun normalizePhoneNumber(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        val trimmed = rawPhone.trim()
        val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
        if (digitsOnly.isEmpty()) return null

        // 1. If starts with 091 followed by 10 digits (13 digits total): e.g. 0919876543210 -> 919876543210
        if (digitsOnly.startsWith("091") && digitsOnly.length == 13) {
            val nationalPart = digitsOnly.substring(3)
            return "91$nationalPart"
        }

        // 2. If starts with leading 0 followed by 10 digits (11 digits total): e.g. 09876543210 -> 919876543210
        if (digitsOnly.startsWith("0") && digitsOnly.length == 11) {
            val nationalPart = digitsOnly.substring(1)
            return "91$nationalPart"
        }

        // 3. Exactly 10 digits (Standard Indian national number): e.g. 9876543210 -> 919876543210
        if (digitsOnly.length == 10) {
            return "91$digitsOnly"
        }

        // 4. Exactly 12 digits starting with 91: e.g. 919876543210 -> 919876543210
        if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
            return digitsOnly
        }

        // 5. If original input had '+' and valid length (International format)
        if (trimmed.startsWith("+") && digitsOnly.length in 7..15) {
            return digitsOnly
        }

        // 6. If digits length > 10 and starts with 91, extract the 10-digit national number
        if (digitsOnly.length > 10 && digitsOnly.startsWith("91")) {
            val last10 = digitsOnly.takeLast(10)
            return "91$last10"
        }

        // 7. General valid numeric phone length
        if (digitsOnly.length in 7..15) {
            return digitsOnly
        }

        return null
    }

    /**
     * Directly opens the WhatsApp 1-on-1 chat window for the specified phone number,
     * without requiring the number to be saved in the device's Contacts app and without
     * requiring manual contact selection or searching.
     *
     * Pre-populates the message text inside the chat.
     */
    fun openWhatsAppChat(
        context: Context,
        rawPhone: String?,
        messageText: String = "",
        onInvalidNumber: (() -> Unit)? = null
    ): Boolean {
        val normalized = normalizePhoneNumber(rawPhone)
        if (normalized == null || normalized.length < 7) {
            if (onInvalidNumber != null) {
                onInvalidNumber()
            } else {
                Toast.makeText(
                    context,
                    "Please enter a valid contact phone number for WhatsApp",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }

        val encodedMsg = if (messageText.isNotEmpty()) Uri.encode(messageText) else ""

        val waSchemeUrl = if (encodedMsg.isNotEmpty()) {
            "whatsapp://send?phone=$normalized&text=$encodedMsg"
        } else {
            "whatsapp://send?phone=$normalized"
        }

        val waApiUrl = if (encodedMsg.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$normalized&text=$encodedMsg"
        } else {
            "https://api.whatsapp.com/send?phone=$normalized"
        }

        val waShortUrl = if (encodedMsg.isNotEmpty()) {
            "https://wa.me/$normalized?text=$encodedMsg"
        } else {
            "https://wa.me/$normalized"
        }

        // 1. Try direct whatsapp:// scheme with standard WhatsApp package
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waSchemeUrl)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            // 2. Try direct whatsapp:// scheme with WhatsApp Business package
            try {
                val waBusinessIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waSchemeUrl)).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(waBusinessIntent)
                return true
            } catch (_: Exception) {
                // 3. Try direct whatsapp:// URI scheme without explicit package lock
                try {
                    val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waSchemeUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(schemeIntent)
                    return true
                } catch (_: Exception) {
                    // 4. Try api.whatsapp.com universal link
                    try {
                        val apiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waApiUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(apiIntent)
                        return true
                    } catch (_: Exception) {
                        // 5. Try wa.me short link
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waShortUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                            return true
                        } catch (finalEx: Exception) {
                            Toast.makeText(
                                context,
                                "WhatsApp is not installed on this device",
                                Toast.LENGTH_SHORT
                            ).show()
                            return false
                        }
                    }
                }
            }
        }
    }

    /**
     * Sends Digital Receipt image file to the WhatsApp chat using the exact same destination
     * and number normalization mechanism as openWhatsAppChat / Send WhatsApp Confirmation.
     *
     * Directly attaches the generated image file to the WhatsApp compose window with the
     * phone number targeted via the WhatsApp JID (`[phone]@s.whatsapp.net`) so the receipt
     * image is physically attached and ready to send.
     */
    fun sendWhatsAppMedia(
        context: Context,
        rawPhone: String?,
        mediaUri: Uri,
        mimeType: String = "image/png",
        messageText: String = "",
        onInvalidNumber: (() -> Unit)? = null
    ): Boolean {
        val normalized = normalizePhoneNumber(rawPhone)
        if (normalized == null || normalized.length < 7) {
            if (onInvalidNumber != null) {
                onInvalidNumber()
            } else {
                Toast.makeText(
                    context,
                    "Please enter a valid contact phone number for WhatsApp",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }

        val jid = "$normalized@s.whatsapp.net"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            if (messageText.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            putExtra("jid", jid)
            clipData = android.content.ClipData.newUri(context.contentResolver, "Digital Receipt Image", mediaUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 1. Try direct ACTION_SEND targeting official WhatsApp
        try {
            val waIntent = Intent(sendIntent).apply {
                setPackage("com.whatsapp")
            }
            context.startActivity(waIntent)
            return true
        } catch (_: Exception) {
            // 2. Try direct ACTION_SEND targeting WhatsApp Business
            try {
                val w4bIntent = Intent(sendIntent).apply {
                    setPackage("com.whatsapp.w4b")
                }
                context.startActivity(w4bIntent)
                return true
            } catch (_: Exception) {
                // 3. Try ACTION_SEND without package restriction
                try {
                    val genericIntent = Intent(sendIntent)
                    context.startActivity(genericIntent)
                    return true
                } catch (_: Exception) {
                    // 4. Try standard chooser with image attachment
                    try {
                        val chooser = Intent.createChooser(sendIntent, "Send Receipt via WhatsApp").apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                        return true
                    } catch (finalEx: Exception) {
                        Toast.makeText(
                            context,
                            "WhatsApp is not installed on this device",
                            Toast.LENGTH_SHORT
                        ).show()
                        return false
                    }
                }
            }
        }
    }
}
