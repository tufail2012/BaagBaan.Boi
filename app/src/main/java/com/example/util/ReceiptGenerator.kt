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
import com.example.data.BusinessInfo
import com.example.data.BusinessInfoRepository
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
    val expectedDelivery: String,
    val rootstock: String = "",
    val feathers: String = "",
    val rootDiameter: String = "",
    val scionVariety: String = "",
    val plantOrigin: String = "",
    val recordType: String = "",
    val recordId: Long = 0L,
    val varietyLinesJson: String = "",
    val totalKanalArea: Double = 0.0,
    val costPerPlant: Double = 0.0
)

object ReceiptGenerator {

    fun generateReceiptBitmap(
        data: ReceiptData,
        context: Context? = null,
        businessInfo: BusinessInfo? = null
    ): Bitmap {
        val info = businessInfo ?: BusinessInfoRepository.currentBusinessInfo

        val parsedLines = com.example.data.parseVarietyLines(data.varietyLinesJson)
        val extraHeight = if (parsedLines.size > 1) ((parsedLines.size - 1) * 55) else 0
        val width = 1080
        val height = 1720 + extraHeight
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
        val headerRect = RectF(48f, 48f, width - 48f, 320f)
        canvas.drawRoundRect(headerRect, 20f, 20f, paint)

        // Header Title: Business Name
        val displayName = info.businessName.ifBlank { "BAAGBAAN BOI" }.uppercase()
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(displayName, width / 2f, 115f, paint)

        // Company Registration Number (GSTIN) directly below company header
        if (info.registrationNumber.isNotBlank()) {
            paint.textSize = 21f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = goldAccent
            canvas.drawText("Registration Number: ${info.registrationNumber}", width / 2f, 155f, paint)
        }

        // Subtitle (Tagline & Location/Address)
        val subtitle = buildString {
            if (info.tagline.isNotBlank()) {
                append(info.tagline)
            }
            val shortAddress = info.address.split(",").firstOrNull()?.trim() ?: info.address
            if (shortAddress.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(shortAddress)
            }
        }.ifBlank { "The Streets of Kashmir • Ramnagri 192303" }

        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.color = Color.WHITE
        canvas.drawText(subtitle, width / 2f, 195f, paint)

        // Contacts Row(s)
        val contacts = info.contactNumbers.filter { it.isNotBlank() }
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.WHITE
        if (contacts.isNotEmpty()) {
            val firstRow = contacts.take(2).joinToString("  |  ")
            canvas.drawText("Contacts: $firstRow", width / 2f, 235f, paint)
            if (contacts.size > 2) {
                val secondRow = contacts.drop(2).take(2).joinToString("  |  ")
                canvas.drawText(secondRow, width / 2f, 275f, paint)
            }
        } else {
            canvas.drawText("Contacts: +916006143037  |  +917006996169", width / 2f, 235f, paint)
            canvas.drawText("+91 7051826858  |  +91 6005096439", width / 2f, 275f, paint)
        }

        // 3. Official Digital Receipt Banner
        paint.color = maroonBg
        paint.style = Paint.Style.FILL
        val bannerRect = RectF(180f, 340f, width - 180f, 410f)
        canvas.drawRoundRect(bannerRect, 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OFFICIAL DIGITAL RECEIPT", width / 2f, 385f, paint)

        var currentY = 435f

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

        // Section 2: Order & Service Details (Itemized Table if multi-variety, else standard card)
        if (parsedLines.isNotEmpty()) {
            val startY = currentY
            val tableRowHeight = 36f
            val headerHeight = 70f
            val tableHeaderHeight = 32f
            val footerHeight = 45f
            val cardHeight = headerHeight + tableHeaderHeight + (parsedLines.size * tableRowHeight) + footerHeight + 20f

            val cardRect = RectF(60f, startY, width - 60f, startY + cardHeight)

            paint.style = Paint.Style.FILL
            paint.color = creamCard
            canvas.drawRoundRect(cardRect, 16f, 16f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = cardBorder
            paint.strokeWidth = 2.5f
            canvas.drawRoundRect(cardRect, 16f, 16f, paint)

            // Section Title
            paint.style = Paint.Style.FILL
            paint.color = darkGreen
            paint.textSize = 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("🌱 ORDER & VARIETIES BREAKDOWN", 90f, startY + 44f, paint)

            // Category tag on right
            paint.color = textGray
            paint.textSize = 19f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(data.serviceCategory.ifBlank { "Plant Booking" }, width - 90f, startY + 44f, paint)

            // Title Underline
            paint.color = cardBorder
            paint.strokeWidth = 2f
            canvas.drawLine(90f, startY + 58f, width - 90f, startY + 58f, paint)

            // Table Column Headers: Variety | Qty | Unit Price | Subtotal
            val tableHeadY = startY + 86f
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = textGray

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Variety / Item", 90f, tableHeadY, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Qty", 520f, tableHeadY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Unit Price", 750f, tableHeadY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Subtotal", width - 90f, tableHeadY, paint)

            // Header line
            paint.color = cardBorder
            paint.strokeWidth = 1.5f
            canvas.drawLine(90f, tableHeadY + 8f, width - 90f, tableHeadY + 8f, paint)

            var rowY = tableHeadY + 34f
            var totalQtySum = 0
            var subtotalSum = 0.0

            parsedLines.forEach { line ->
                totalQtySum += line.quantity
                val lineTotal = line.quantity * line.unitPrice
                subtotalSum += lineTotal

                val varietyLabel = buildString {
                    append(line.variety.ifBlank { "Plant Sapling" })
                    if (line.rootstock.isNotBlank()) append(" (${line.rootstock})")
                    if (line.feathers.isNotBlank()) append(" [${line.feathers}]")
                }

                paint.color = textDark
                paint.textSize = 20f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(varietyLabel, 90f, rowY, paint)

                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${line.quantity}", 520f, rowY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("₹${line.unitPrice.toInt()}", 750f, rowY, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(currencyFmt.format(lineTotal), width - 90f, rowY, paint)

                rowY += tableRowHeight
            }

            // Summary Divider
            paint.color = cardBorder
            paint.strokeWidth = 1.5f
            canvas.drawLine(90f, rowY - 6f, width - 90f, rowY - 6f, paint)

            // Total Row
            paint.color = darkGreen
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total (${totalQtySum} Plants)", 90f, rowY + 22f, paint)

            if (data.expectedDelivery.isNotBlank()) {
                paint.color = textGray
                paint.textSize = 17f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Exp. Delivery: ${data.expectedDelivery}", 520f, rowY + 22f, paint)
            }

            paint.color = darkGreen
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(currencyFmt.format(subtotalSum), width - 90f, rowY + 22f, paint)

            currentY = startY + cardHeight + 20f
        } else {
            val isGardenPlanning = data.serviceCategory.equals("Garden Planning", ignoreCase = true) ||
                    data.recordType.equals("gardenplanning", ignoreCase = true) ||
                    data.serviceCategory.contains("Garden", ignoreCase = true)

            val isRootstock = data.serviceCategory.equals("Rootstocks", ignoreCase = true) ||
                    data.serviceCategory.contains("Rootstock", ignoreCase = true) ||
                    data.serviceCategory.equals("Imported Rootstocks", ignoreCase = true) ||
                    data.serviceCategory.equals("Imported Rootstock", ignoreCase = true)

            val orderDetailsItems = if (isGardenPlanning) {
                val list = mutableListOf<Pair<String, String>>()
                list.add("Service Category" to "Garden Planning")
                if (data.plantOrigin.isNotBlank()) {
                    list.add("Plant Origin" to data.plantOrigin)
                }
                val itemVariety = if (data.rootstock.isNotBlank()) {
                    val baseVariety = data.plantVariety.ifBlank { "Apple Plants" }
                    if (baseVariety.contains(data.rootstock) || baseVariety.contains("/")) {
                        baseVariety
                    } else {
                        "$baseVariety / ${data.rootstock}"
                    }
                } else {
                    data.plantVariety.ifBlank { "Apple Plants" }
                }
                list.add("Item / Variety" to itemVariety)
                if (data.feathers.isNotBlank()) {
                    list.add("Feathers" to if (data.feathers.all { it.isDigit() }) "${data.feathers} branches" else data.feathers)
                }
                val kanalAreaStr = if (data.totalKanalArea > 0.0) {
                    val areaFormatted = if (data.totalKanalArea % 1.0 == 0.0) "${data.totalKanalArea.toInt()}" else "${data.totalKanalArea}"
                    "$areaFormatted Kanals"
                } else {
                    "N/A"
                }
                list.add("Kanal Area" to kanalAreaStr)

                val qtyRateStr = if (data.costPerPlant > 0.0) {
                    val rateFormatted = if (data.costPerPlant % 1.0 == 0.0) "${data.costPerPlant}" else "${data.costPerPlant}"
                    "${data.quantity} Plants @ ₹$rateFormatted"
                } else {
                    "${data.quantity} Plants"
                }
                list.add("Quantity / Units" to qtyRateStr)
                list.add("Expected Delivery" to data.expectedDelivery.ifBlank { "To be scheduled" })
                list
            } else if (isRootstock) {
                val actualRootstock = data.rootstock.ifBlank { "M9-T337" }
                val rawDiam = data.rootDiameter.ifBlank { "9 to 12 mm" }
                val formattedDiam = if (rawDiam.lowercase().contains("mm")) rawDiam else "$rawDiam mm"
                val actualScion = data.scionVariety.ifBlank { data.plantVariety.ifBlank { "N/A" } }
                val qtyStr = if (data.quantity.lowercase().endsWith("plants") || data.quantity.lowercase().endsWith("rootstocks")) {
                    data.quantity
                } else {
                    "${data.quantity} Rootstocks"
                }

                listOf(
                    "Service Category" to data.serviceCategory.ifBlank { "Imported Rootstocks" },
                    "Rootstock" to actualRootstock,
                    "Root Diameter (mm)" to formattedDiam,
                    "Scion Variety" to actualScion,
                    "Quantity / Units" to qtyStr,
                    "Expected Delivery" to data.expectedDelivery.ifBlank { "To be scheduled" }
                )
            } else {
                val list = mutableListOf<Pair<String, String>>()
                list.add("Service Category" to data.serviceCategory.ifBlank { "N/A" })
                if (data.plantOrigin.isNotBlank()) {
                    list.add("Plant Origin" to data.plantOrigin)
                }
                val itemVariety = if (data.rootstock.isNotBlank()) {
                    val baseVariety = data.plantVariety.ifBlank { data.serviceCategory }
                    if (baseVariety.contains(data.rootstock) || baseVariety.contains("/")) {
                        baseVariety
                    } else {
                        "$baseVariety / ${data.rootstock}"
                    }
                } else {
                    data.plantVariety.ifBlank { data.serviceCategory }
                }
                list.add("Item / Variety" to itemVariety)
                if (data.feathers.isNotBlank()) {
                    list.add("Feathers" to if (data.feathers.all { it.isDigit() }) "${data.feathers} branches" else data.feathers)
                }
                list.add("Quantity / Units" to "${data.quantity} Plants")
                list.add("Expected Delivery" to data.expectedDelivery.ifBlank { "To be scheduled" })
                list
            }

            drawSectionCard(
                title = "🌱 ORDER & SERVICE DETAILS",
                items = orderDetailsItems
            )
        }

        // Section 3: Payment Breakdown
        val paymentBreakdownItems = mutableListOf(
            "Total Amount" to currencyFmt.format(data.totalAmount),
            "Advance Paid" to currencyFmt.format(data.amountPaid),
            "Balance Due" to currencyFmt.format(data.remainingBalance),
            "Payment Status" to data.paymentStatus
        )
        if (info.accountNumber.isNotBlank()) {
            paymentBreakdownItems.add("Account No" to info.accountNumber)
        }
        if (info.ifscCode.isNotBlank()) {
            paymentBreakdownItems.add("IFSC Code" to info.ifscCode)
        }
        if (info.accountHolderName.isNotBlank()) {
            paymentBreakdownItems.add("Account Holder" to info.accountHolderName)
        }

        drawSectionCard(
            title = "💳 PAYMENT BREAKDOWN",
            items = paymentBreakdownItems,
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
        canvas.drawText("THANK YOU FOR CHOOSING ${displayName}!", width / 2f, height - 102f, paint)

        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = goldAccent
        val footerAddress = info.address.ifBlank { "Ramnagri 192303, Shopian, Jammu & Kashmir" }
        canvas.drawText(footerAddress, width / 2f, height - 70f, paint)

        // 5. QR Code Section
        drawQrCodeSection(canvas, data, width.toFloat(), height.toFloat())

        // 6. Stamp Overlay
        drawStampOverlay(canvas, width.toFloat(), height.toFloat(), context)

        return bitmap
    }

    private fun generateQrBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java).apply {
                put(com.google.zxing.EncodeHintType.MARGIN, 1)
                put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
            }
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val darkGreen = Color.parseColor("#122E1F")
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) darkGreen else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun drawQrCodeSection(
        canvas: Canvas,
        data: ReceiptData,
        width: Float,
        height: Float
    ) {
        val isGarden = data.recordType.equals("gardenplanning", ignoreCase = true) ||
                (data.recordType.isBlank() && data.serviceCategory.contains("Garden", ignoreCase = true))
        val typeParam = if (isGarden) "gardenplanning" else "croprecord"
        val deepLinkUrl = if (data.recordId > 0L) {
            "baagbaanboi://record?type=$typeParam&id=${data.recordId}"
        } else if (data.serialNumber.isNotBlank() && data.serialNumber != "N/A") {
            "baagbaanboi://record?type=$typeParam&id=0&serial=${data.serialNumber}"
        } else {
            "baagbaanboi://record?type=$typeParam&id=0"
        }

        val qrSize = 140
        val qrBitmap = generateQrBitmap(deepLinkUrl, qrSize) ?: return

        val qrCardLeft = 60f
        val qrCardBottom = height - 165f
        val qrCardTop = qrCardBottom - 210f
        val qrCardRight = qrCardLeft + 250f
        val qrCardRect = RectF(qrCardLeft, qrCardTop, qrCardRight, qrCardBottom)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background card
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FAF8F2")
        canvas.drawRoundRect(qrCardRect, 16f, 16f, paint)

        // Card border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E0D8C8")
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(qrCardRect, 16f, 16f, paint)

        // Draw QR code image
        val qrX = qrCardLeft + (qrCardRight - qrCardLeft - qrSize) / 2f
        val qrY = qrCardTop + 14f
        canvas.drawBitmap(qrBitmap, qrX, qrY, null)

        // Caption beneath QR
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#122E1F")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Scan to view booking", qrCardRect.centerX(), qrCardBottom - 18f, paint)
    }

    private fun drawStampOverlay(canvas: Canvas, width: Float, height: Float, context: Context?) {
        val stampCx = width - 230f
        val stampCy = height - 175f
        val stampSize = 290f
        val stampRadius = stampSize / 2f

        var stampBitmap: Bitmap? = null
        if (context != null) {
            try {
                val resId = context.resources.getIdentifier("stamp_streets_of_kashmir", "drawable", context.packageName)
                if (resId != 0) {
                    val original = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                    if (original != null) {
                        stampBitmap = Bitmap.createScaledBitmap(original, stampSize.toInt(), stampSize.toInt(), true)
                    }
                }
            } catch (e: Exception) {
                stampBitmap = null
            }
        }

        canvas.save()
        canvas.rotate(-15f, stampCx, stampCy)

        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stampPaint.alpha = 160

        if (stampBitmap != null) {
            canvas.drawBitmap(stampBitmap, stampCx - stampRadius, stampCy - stampRadius, stampPaint)
        } else {
            val stampColor = Color.parseColor("#381E72")

            stampPaint.color = stampColor
            stampPaint.style = Paint.Style.STROKE
            stampPaint.strokeWidth = 6f
            canvas.drawCircle(stampCx, stampCy, stampRadius, stampPaint)

            stampPaint.strokeWidth = 2.5f
            canvas.drawCircle(stampCx, stampCy, stampRadius - 8f, stampPaint)
            canvas.drawCircle(stampCx, stampCy, stampRadius - 16f, stampPaint)

            stampPaint.style = Paint.Style.FILL
            stampPaint.textAlign = Paint.Align.CENTER

            stampPaint.textSize = 18f
            stampPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("THE STREETS OF", stampCx, stampCy - 50f, stampPaint)

            stampPaint.textSize = 34f
            stampPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("KASHMIR", stampCx, stampCy - 5f, stampPaint)

            stampPaint.textSize = 12f
            stampPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Pioneering Kashmir's Orchard Future", stampCx, stampCy + 32f, stampPaint)

            stampPaint.strokeWidth = 2f
            stampPaint.style = Paint.Style.STROKE
            canvas.drawLine(stampCx - 75f, stampCy + 55f, stampCx - 14f, stampCy + 55f, stampPaint)
            canvas.drawLine(stampCx + 14f, stampCy + 55f, stampCx + 75f, stampCy + 55f, stampPaint)
            stampPaint.style = Paint.Style.FILL
            canvas.drawCircle(stampCx, stampCy + 55f, 4f, stampPaint)
        }

        canvas.restore()
    }

    fun saveReceiptImageAndGetUri(context: Context, bitmap: Bitmap, serialNumber: String): Uri? {
        return com.example.util.saveReceiptImageAndGetUri(context, bitmap, serialNumber)
    }

    fun printReceiptBitmap(context: Context, bitmap: Bitmap, serialNumber: String = "Entry") {
        try {
            val printHelper = androidx.print.PrintHelper(context).apply {
                scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
            }
            printHelper.printBitmap("Receipt_${serialNumber.ifBlank { "Entry" }}", bitmap)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No printer found. Please connect a printer and try again.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
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
