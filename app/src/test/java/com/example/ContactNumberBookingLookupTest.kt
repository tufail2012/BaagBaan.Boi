package com.example

import com.example.data.CropRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactNumberBookingLookupTest {

    private fun normalizePhoneForBookingLookup(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var clean = raw.trim()
        if (clean.startsWith("+91")) clean = clean.removePrefix("+91").trim()
        else if (clean.startsWith("91") && clean.length > 10) clean = clean.removePrefix("91").trim()
        else if (clean.startsWith("+")) clean = clean.removePrefix("+").trim()
        else if (clean.startsWith("0") && clean.length == 11) clean = clean.substring(1).trim()
        val digits = clean.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else ""
    }

    private fun lookupBookingsByPhone(
        inputPhone: String,
        records: List<CropRecord>,
        currentEditingId: Long? = null
    ): List<CropRecord> {
        val normInput = normalizePhoneForBookingLookup(inputPhone)
        if (normInput.length < 10) return emptyList()

        return records.filter { rec ->
            (currentEditingId == null || rec.id != currentEditingId) &&
            normalizePhoneForBookingLookup(rec.contactNumber) == normInput
        }.sortedByDescending { it.id }
    }

    @Test
    fun testPhoneNormalizationFormats() {
        val expected = "9876543210"
        assertEquals(expected, normalizePhoneForBookingLookup("+91 9876543210"))
        assertEquals(expected, normalizePhoneForBookingLookup("9876543210"))
        assertEquals(expected, normalizePhoneForBookingLookup("+919876543210"))
        assertEquals(expected, normalizePhoneForBookingLookup("09876543210"))
        assertEquals(expected, normalizePhoneForBookingLookup("919876543210"))
        assertEquals(expected, normalizePhoneForBookingLookup("+91 98765 43210"))
        assertEquals(expected, normalizePhoneForBookingLookup("+91-9876543210"))
    }

    @Test
    fun testShortOrIncompletePhoneReturnsEmpty() {
        assertEquals("", normalizePhoneForBookingLookup("+91 "))
        assertEquals("", normalizePhoneForBookingLookup("+91 987"))
        assertEquals("", normalizePhoneForBookingLookup("98765"))
        assertEquals("", normalizePhoneForBookingLookup(""))
        assertEquals("", normalizePhoneForBookingLookup(null))
    }

    @Test
    fun testLookupMatchesExistingBooking() {
        val record1 = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious",
            bookingDate = "25/08/2026",
            paymentStatus = "Pending"
        )
        val records = listOf(record1)

        // Matching with standard 10 digits
        val matches1 = lookupBookingsByPhone("9876543210", records)
        assertEquals(1, matches1.size)
        assertEquals("LP-69", matches1[0].serialNumber)
        assertEquals("Local Plants", matches1[0].serviceType)

        // Matching with +91 prefix
        val matches2 = lookupBookingsByPhone("+91 9876543210", records)
        assertEquals(1, matches2.size)
        assertEquals("LP-69", matches2[0].serialNumber)

        // Matching with no space +91
        val matches3 = lookupBookingsByPhone("+919876543210", records)
        assertEquals(1, matches3.size)
        assertEquals("LP-69", matches3[0].serialNumber)
    }

    @Test
    fun testMultipleBookingsForSameNumber() {
        val record1 = CropRecord(
            id = 1L,
            serialNumber = "LP-10",
            farmerName = "Ghulam Hassan",
            farmerAddress = "Pulwama",
            contactNumber = "9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious",
            bookingDate = "10/05/2026",
            paymentStatus = "Fully Paid"
        )
        val record2 = CropRecord(
            id = 2L,
            serialNumber = "IMP-25",
            farmerName = "Ghulam Hassan",
            farmerAddress = "Pulwama",
            contactNumber = "+91 9876543210",
            serviceType = "Imported Plants",
            plantVariety = "Gala",
            bookingDate = "20/07/2026",
            paymentStatus = "Advance Paid"
        )
        val records = listOf(record1, record2)

        val matches = lookupBookingsByPhone("+91 9876543210", records)
        assertEquals(2, matches.size)
        // Check order (sorted by descending id)
        assertEquals("IMP-25", matches[0].serialNumber)
        assertEquals("LP-10", matches[1].serialNumber)
    }

    @Test
    fun testNoMatchForUnusedNumber() {
        val record1 = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val records = listOf(record1)

        val matches = lookupBookingsByPhone("+91 9123456789", records)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testDoesNotMatchByOtherFields() {
        val record1 = CropRecord(
            id = 1L,
            serialNumber = "9876543210", // Serial happens to be digits
            farmerName = "9876543210", // Name happens to be digits
            farmerAddress = "Shopian 9876543210",
            contactNumber = "+91 9111111111", // Different contact number
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val records = listOf(record1)

        // Searching by 9876543210 should NOT match because contactNumber is 9111111111
        val matches = lookupBookingsByPhone("+91 9876543210", records)
        assertTrue(matches.isEmpty())
    }
}
