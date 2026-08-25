package com.example

import com.example.data.CropRecord
import com.example.data.calculateTotalAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingReceivedStatusTest {

    @Test
    fun testMarkBookingAsReceivedPreservesPaymentAndCalculations() {
        val originalRecord = CropRecord(
            id = 101L,
            serialNumber = "LP-88",
            farmerName = "Bilal Ahmad",
            farmerAddress = "Shopian, Kashmir",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Gala Must",
            quantity = 500,
            amountPaid = 15000.0,
            paymentStatus = "Advance Paid",
            bookingDate = "2026-08-20",
            notes = "Rate: 120 per plant"
        )

        // Initial state
        assertFalse(originalRecord.isReceived)
        assertFalse(originalRecord.isCancelled)
        assertEquals(15000.0, originalRecord.amountPaid, 0.001)
        assertEquals("Advance Paid", originalRecord.paymentStatus)

        // Mark as received
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val receivedRecord = originalRecord.copy(
            isReceived = true,
            receivedDate = today,
            timestamp = System.currentTimeMillis()
        )

        // Verify Received status is updated
        assertTrue(receivedRecord.isReceived)
        assertEquals(today, receivedRecord.receivedDate)
        assertFalse(receivedRecord.isCancelled)

        // Verify Payment info is strictly preserved and not altered
        assertEquals(originalRecord.amountPaid, receivedRecord.amountPaid, 0.001)
        assertEquals(originalRecord.paymentStatus, receivedRecord.paymentStatus)
        assertEquals(originalRecord.calculateTotalAmount(), receivedRecord.calculateTotalAmount(), 0.001)
        assertEquals(originalRecord.quantity, receivedRecord.quantity)
        assertEquals(originalRecord.serialNumber, receivedRecord.serialNumber)
        assertEquals(originalRecord.farmerName, receivedRecord.farmerName)
    }

    @Test
    fun testCancellationAndReceivedAreIndependent() {
        val originalRecord = CropRecord(
            id = 102L,
            serialNumber = "IMP-12",
            farmerName = "Mohammad Yousuf",
            farmerAddress = "Baramulla",
            contactNumber = "9906000000",
            serviceType = "Imported",
            plantVariety = "Red Velox",
            quantity = 200,
            amountPaid = 20000.0
        )

        val cancelledRecord = originalRecord.copy(
            isCancelled = true,
            cancelledDate = "25-08-2026"
        )
        assertTrue(cancelledRecord.isCancelled)
        assertFalse(cancelledRecord.isReceived)

        val receivedRecord = originalRecord.copy(
            isReceived = true,
            receivedDate = "25-08-2026"
        )
        assertTrue(receivedRecord.isReceived)
        assertFalse(receivedRecord.isCancelled)
    }
}
