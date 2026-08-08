package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

data class ReceiptData(
    val serialNumber: String,
    val bookingDate: String,
    val farmerName: String,
    val contactNumber: String,
    val address: String,
    val orchardLocation: String,
    val serviceCategory: String,
    val plantVariety: String,
    val quantity: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val remainingBalance: Double,
    val paymentStatus: String,
    val expectedDelivery: String
)

object ReceiptGenerator {

    fun generateReceiptBitmap(data: ReceiptData): Bitmap {
        val width = 1080
        val height = 1680
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Color palette matching Baagbaan Boi branding
        val darkGreen = Color.parseColor("#122E1F")
        val headerBg = Color.parseColor("#1B3B2B")
        val maroonBg = Color.parseColor("#801B1B")
        val creamCard = Color.parseColor("#FAF8F2")
        val creamBg = Color.parseColor("#F5F2E9")
        val textDark = Color.parseColor("#1C2520")
        val textGray = Color.parseColor("#5A6B62")
        val cardBorder = Color.parseColor("#E0D8C8")
        val goldAccent = Color.parseColor("#D4AF37")

        // Status Colors
        val (statusBg, statusText) = when (data.paymentStatus.lowercase()) {
            "fully paid" -> Color.parseColor("#E8F5E9") to Color.parseColor("#2E7D32")
            "advance paid" -> Color.parseColor("#FFF3E0") to Color.parseColor("#E65100")
            else -> Color.parseColor("#FFEBEE") to Color.parseColor("#C62828")
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Main Background
        canvas.drawColor(creamBg)

        // Outer Decorative Border
        paint.color = darkGreen
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)

        paint.strokeWidth = 2f
        canvas.drawRect(34f, 34f, width - 34f, height - 34f, paint)

        // 2. Header Box (Dark Green)
        paint.style = Paint.Style.FILL
        paint.color = headerBg
        val headerRect = RectF(48f, 48f, width - 48f, 290f)
        canvas.drawRoundRect(headerRect, 20f, 20f, paint)

        // Header Title: BAAGBAAN BOI
        paint.color = Color.WHITE
        paint.textSize = 54f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("BAAGBAAN BOI", width / 2f, 125f, paint)

        // Subtitle
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.color = goldAccent
        canvas.drawText("The Streets of Kashmir • Ramnagri 192303", width / 2f, 165f, paint)

        // Contacts Row
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.WHITE
        canvas.drawText("Contacts: +916006143037  |  +917006996169", width / 2f, 215f, paint)
        canvas.drawText("+91 7051826858  |  +91 6005096439", width / 2f, 250f, paint)

        // 3. Official Digital Receipt Banner
        paint.color = maroonBg
        paint.style = Paint.Style.FILL
        val bannerRect = RectF(180f, 315f, width - 180f, 385f)
        canvas.drawRoundRect(bannerRect, 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OFFICIAL DIGITAL RECEIPT", width / 2f, 360f, paint)

        var currentY = 415f

        // Helper function for drawing a section card
        fun drawSectionCard(
            title: String,
            items: List<Pair<String, String>>,
            extraBadge: Pair<String, Pair<Int, Int>>? = null
        ) {
            val startY = currentY
            val padding = 26f
            val lineHeight = 44f
            val cardHeight = padding * 2 + 38f + items.size * lineHeight

            val cardRect = RectF(60f, startY, width - 60f, startY + cardHeight)

            // Card Background & Border
            paint.style = Paint.Style.FILL
            paint.color = creamCard
            canvas.drawRoundRect(cardRect, 22f, 22f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = cardBorder
            paint.strokeWidth = 3f
            canvas.drawRoundRect(cardRect, 22f, 22f, paint)

            // Section Title
            paint.style = Paint.Style.FILL
            paint.color = darkGreen
            paint.textSize = 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(title, 90f, startY + 48f, paint)

            // Draw Title underline
            paint.color = cardBorder
            paint.strokeWidth = 2f
            canvas.drawLine(90f, startY + 62f, width - 90f, startY + 62f, paint)

            var rowY = startY + 105f

            items.forEach { (label, value) ->
                paint.color = textGray
                paint.textSize = 21f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(label, 90f, rowY, paint)

                paint.color = textDark
                paint.textSize = 21f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(": $value", 380f, rowY, paint)

                rowY += lineHeight
            }

            if (extraBadge != null) {
                val (bText, bColors) = extraBadge
                val (bBg, bFg) = bColors

                paint.style = Paint.Style.FILL
                paint.color = bBg
                val badgeRect = RectF(width - 280f, startY + 25f, width - 90f, startY + 68f)
                canvas.drawRoundRect(badgeRect, 12f, 12f, paint)

                paint.color = bFg
                paint.textSize = 19f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(bText, badgeRect.centerX(), badgeRect.centerY() + 6f, paint)
            }

            currentY = startY + cardHeight + 20f
        }

        val currencyFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        // Section 1: Farmer / Customer Details
        drawSectionCard(
            title = "👤 FARMER / CUSTOMER DETAILS",
            items = listOf(
                "Receipt / Serial #" to data.serialNumber.ifBlank { "N/A" },
                "Booking Date" to data.bookingDate.ifBlank { "N/A" },
                "Customer Name" to data.farmerName.ifBlank { "N/A" },
                "Contact Phone" to data.contactNumber.ifBlank { "N/A" },
                "Address" to data.address.ifBlank { "N/A" },
                "Orchard / Location" to data.orchardLocation.ifBlank { "N/A" }
            )
        )

        // Section 2: Order & Service Details
        drawSectionCard(
            title = "🌱 ORDER & SERVICE DETAILS",
            items = listOf(
                "Service Category" to data.serviceCategory.ifBlank { "N/A" },
                "Item / Variety" to data.plantVariety.ifBlank { data.serviceCategory },
                "Quantity / Units" to "${data.quantity} Plants",
                "Expected Delivery" to data.expectedDelivery.ifBlank { "To be scheduled" }
            )
        )

        // Section 3: Payment Breakdown
        drawSectionCard(
            title = "💳 PAYMENT BREAKDOWN",
            items = listOf(
                "Total Amount" to currencyFmt.format(data.totalAmount),
                "Advance Paid" to currencyFmt.format(data.amountPaid),
                "Balance Due" to currencyFmt.format(data.remainingBalance),
                "Payment Status" to data.paymentStatus
            ),
            extraBadge = data.paymentStatus to (statusBg to statusText)
        )

        // 4. Footer Section
        paint.style = Paint.Style.FILL
        paint.color = maroonBg
        val footerRect = RectF(60f, height - 150f, width - 60f, height - 55f)
        canvas.drawRoundRect(footerRect, 20f, 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 25f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("THANK YOU FOR CHOOSING BAAGBAAN BOI!", width / 2f, height - 102f, paint)

        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = goldAccent
        canvas.drawText("Ramnagri 192303, Shopian, Jammu & Kashmir", width / 2f, height - 70f, paint)

        return bitmap
    }

    fun saveReceiptImageAndGetUri(context: Context, bitmap: Bitmap, serialNumber: String): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "receipts")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val fileName = "Receipt_${serialNumber.ifBlank { "Entry" }}.png"
            val file = File(cachePath, fileName)
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
