package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.ui.components.PendingPaymentItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptManager {

    /**
     * Map a [PendingPaymentItem] into comprehensive [ReceiptData] by looking up
     * details in the underlying crop records or garden planning entries.
     */
    fun mapToReceiptData(
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ): ReceiptData {
        val cropMatch = if (item.source == "CROP") {
            cropRecords.find { it.id == item.id || it.serialNumber == item.serialNumber }
        } else null

        val gardenMatch = if (item.source == "GARDEN") {
            gardenEntries.find { it.id == item.id || it.serialNumber == item.serialNumber }
        } else null

        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val address = cropMatch?.farmerAddress?.ifBlank { "" }
            ?: gardenMatch?.farmerAddress?.ifBlank { "" }
            ?: ""

        val orchardLocation = cropMatch?.location?.ifBlank { "" } ?: ""

        val plantVariety = cropMatch?.plantVariety?.ifBlank { "" }
            ?: gardenMatch?.plantVariety?.ifBlank { "" }
            ?: item.serviceType

        val quantityStr = if (cropMatch != null) {
            "${cropMatch.quantity} Plants"
        } else if (gardenMatch != null) {
            val totalPlants = (gardenMatch.totalKanalArea * gardenMatch.plantsPerKanal).toInt()
            if (totalPlants > 0) "$totalPlants Plants (${gardenMatch.totalKanalArea} Kanals)"
            else "${gardenMatch.totalKanalArea} Kanals"
        } else {
            "1 Order"
        }

        val bookingDate = cropMatch?.bookingDate?.ifBlank { currentDate }
            ?: gardenMatch?.bookingDate?.ifBlank { currentDate }
            ?: currentDate

        val expectedDelivery = cropMatch?.expectedDelivery?.ifBlank { "As Scheduled" }
            ?: gardenMatch?.expectedDelivery?.ifBlank { "As Scheduled" }
            ?: "As Scheduled"

        val rootstock = cropMatch?.rootstock?.ifBlank { "" }
            ?: gardenMatch?.rootStock?.ifBlank { "" }
            ?: ""

        val paymentStatus = when {
            item.paymentStatus.isNotBlank() -> item.paymentStatus
            item.amountPaid > 0 -> "Advance Paid"
            else -> "Pending"
        }

        return ReceiptData(
            serialNumber = item.serialNumber,
            bookingDate = bookingDate,
            farmerName = item.farmerName.ifBlank { "Valued Customer" },
            contactNumber = item.contactNumber,
            address = address,
            orchardLocation = orchardLocation,
            serviceCategory = item.serviceType,
            plantVariety = plantVariety,
            quantity = quantityStr,
            totalAmount = item.totalCost,
            amountPaid = item.amountPaid,
            remainingBalance = item.amountDue,
            paymentStatus = paymentStatus,
            expectedDelivery = expectedDelivery,
            rootstock = rootstock,
            plantOrigin = gardenMatch?.plantOrigin ?: "",
            recordType = item.source,
            recordId = item.id,
            totalKanalArea = gardenMatch?.totalKanalArea ?: 0.0,
            costPerPlant = gardenMatch?.costPerPlant ?: 0.0
        )
    }

    /**
     * Generates a high-quality Bitmap for previewing the payment receipt.
     */
    fun generatePreviewBitmap(
        context: Context,
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ): Bitmap {
        val data = mapToReceiptData(item, cropRecords, gardenEntries)
        return ReceiptGenerator.generateReceiptBitmap(data, context)
    }

    /**
     * Generates a high-resolution, vector-scaled PDF document for the payment receipt
     * conforming to ISO A4 dimensions.
     */
    fun generatePdfReceipt(
        context: Context,
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ): File {
        val data = mapToReceiptData(item, cropRecords, gardenEntries)
        val bitmap = ReceiptGenerator.generateReceiptBitmap(data, context)

        val pdfDocument = PdfDocument()

        // Standard ISO A4 dimensions in PostScript points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Outer margin for professional print borders
        val marginHorizontal = 16f
        val marginVertical = 16f
        val destRect = RectF(
            marginHorizontal,
            marginVertical,
            pageWidth - marginHorizontal,
            pageHeight - marginVertical
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "receipts")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val cleanSerial = item.serialNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(cacheDir, "Payment_Receipt_${cleanSerial.ifBlank { "Statement" }}.pdf")

        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
            fos.flush()
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Initiates the native Android Print Spooler with the generated PDF document,
     * allowing direct printing to WiFi/Bluetooth printers or "Save as PDF".
     */
    fun printReceipt(
        context: Context,
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ) {
        try {
            val pdfFile = generatePdfReceipt(context, item, cropRecords, gardenEntries)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager

            if (printManager == null) {
                Toast.makeText(context, "Print service is not available on this device", Toast.LENGTH_SHORT).show()
                return
            }

            val jobName = "Receipt_${item.serialNumber.ifBlank { "Farmer" }}"
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to initiate print: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the generated PDF receipt document via standard Android Share sheet.
     */
    fun sharePdfReceipt(
        context: Context,
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ) {
        try {
            val pdfFile = generatePdfReceipt(context, item, cropRecords, gardenEntries)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareText = "Dear ${item.farmerName.ifBlank { "Valued Customer" }}, here is your official digital payment receipt (Serial #${item.serialNumber}) from Baagbaan Boi."

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Payment Receipt - ${item.serialNumber}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share PDF Receipt"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends the generated PDF receipt directly to WhatsApp or WhatsApp Business.
     */
    fun sharePdfViaWhatsApp(
        context: Context,
        item: PendingPaymentItem,
        cropRecords: List<CropRecord> = emptyList(),
        gardenEntries: List<GardenPlanningEntry> = emptyList()
    ) {
        try {
            val pdfFile = generatePdfReceipt(context, item, cropRecords, gardenEntries)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val phone = item.contactNumber.trim()
            var cleanDigits = phone.replace(Regex("[^0-9]"), "")
            if (cleanDigits.startsWith("91") && cleanDigits.length > 10) {
                cleanDigits = cleanDigits.takeLast(10)
            } else if (cleanDigits.startsWith("0") && cleanDigits.length == 11) {
                cleanDigits = cleanDigits.substring(1)
            }
            if (cleanDigits.length > 10) {
                cleanDigits = cleanDigits.takeLast(10)
            }
            val formattedPhone = if (cleanDigits.isNotEmpty()) "91$cleanDigits" else ""

            val caption = "Dear ${item.farmerName.ifBlank { "Valued Customer" }}, here is your official digital payment receipt (Serial #${item.serialNumber}) from Baagbaan Boi."

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                if (formattedPhone.isNotEmpty()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, caption)
                        if (formattedPhone.isNotEmpty()) {
                            putExtra("jid", "$formattedPhone@s.whatsapp.net")
                        }
                        setPackage("com.whatsapp.w4b")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(waBusinessIntent)
                } catch (ex: Exception) {
                    // Fallback to general share
                    intent.setPackage(null)
                    context.startActivity(Intent.createChooser(intent, "Send Receipt PDF"))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send PDF receipt: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
