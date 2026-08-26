package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppReceiptDestinationTest {

    @Test
    fun testPhoneNumberNormalizationForUnsavedNumber() {
        val testNumber = "+91 9876543210"
        val normalized = WhatsAppHelper.normalizePhoneNumber(testNumber)
        assertEquals("919876543210", normalized)

        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("9876543210"))
        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("09876543210"))
        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("+919876543210"))
        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("919876543210"))
        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("+91 98765 43210"))
        assertEquals("919876543210", WhatsAppHelper.normalizePhoneNumber("+91-9876543210"))
    }

    @Test
    fun testSendWhatsAppConfirmationAndDigitalReceiptUseSameDestinationIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val booking = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious",
            quantity = 500,
            amountPaid = 10000.0
        )

        // 1. Test Send WhatsApp Confirmation Flow with unsaved number
        val confirmationOpened = WhatsAppHelper.openWhatsAppChat(
            context = context,
            rawPhone = booking.contactNumber,
            messageText = "Dear Tariq Ahmad, your booking LP-69 is confirmed."
        )
        assertTrue(confirmationOpened)

        val shadowApp = shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
        val confirmationIntent = shadowApp.nextStartedActivity
        assertNotNull(confirmationIntent)
        assertEquals(Intent.ACTION_VIEW, confirmationIntent.action)
        val confirmUri = confirmationIntent.data?.toString() ?: ""
        assertTrue(
            "Intent URI must target 919876543210 directly: $confirmUri",
            confirmUri.contains("919876543210")
        )

        // 2. Test Send Digital Receipt Flow with the exact same unsaved number and attached image
        val dummyMediaUri = Uri.parse("content://com.example.provider/receipts/LP-69.png")
        val receiptOpened = WhatsAppHelper.sendWhatsAppMedia(
            context = context,
            rawPhone = booking.contactNumber,
            mediaUri = dummyMediaUri,
            messageText = "Dear Tariq Ahmad, here is your official digital receipt (Serial #LP-69)."
        )
        assertTrue(receiptOpened)

        val receiptIntent = shadowApp.nextStartedActivity
        assertNotNull(receiptIntent)
        assertEquals(Intent.ACTION_SEND, receiptIntent.action)
        assertEquals("image/png", receiptIntent.type)
        assertEquals(dummyMediaUri, receiptIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("919876543210@s.whatsapp.net", receiptIntent.getStringExtra("jid"))
        assertEquals("com.whatsapp", receiptIntent.`package`)
        assertTrue(
            "Caption should be set in EXTRA_TEXT",
            receiptIntent.getStringExtra(Intent.EXTRA_TEXT)?.contains("LP-69") == true
        )
    }

    @Test
    fun testGardenPlanningBookingReceiptDestination() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gardenEntry = GardenPlanningEntry(
            id = 2L,
            serialNumber = "GP-15",
            farmerName = "Ghulam Hassan",
            farmerAddress = "Pulwama",
            contactNumber = "+91 9876543210",
            totalCost = 30000.0,
            amountPaid = 15000.0
        )

        val dummyMediaUri = Uri.parse("content://com.example.provider/receipts/GP-15.png")
        val receiptOpened = WhatsAppHelper.sendWhatsAppMedia(
            context = context,
            rawPhone = gardenEntry.contactNumber,
            mediaUri = dummyMediaUri,
            messageText = "Dear Ghulam Hassan, here is your official digital receipt (Serial #GP-15)."
        )
        assertTrue(receiptOpened)

        val shadowApp = shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
        val receiptIntent = shadowApp.nextStartedActivity
        assertNotNull(receiptIntent)
        assertEquals(Intent.ACTION_SEND, receiptIntent.action)
        assertEquals("image/png", receiptIntent.type)
        assertEquals(dummyMediaUri, receiptIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("919876543210@s.whatsapp.net", receiptIntent.getStringExtra("jid"))
    }
}
