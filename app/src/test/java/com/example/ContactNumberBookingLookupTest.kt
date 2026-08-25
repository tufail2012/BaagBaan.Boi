package com.example

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.ui.components.BookingLookupHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactNumberBookingLookupTest {

    @Test
    fun testPhoneNormalizationFormats() {
        val expected = "9876543210"
        assertEquals(expected, BookingLookupHelper.normalizePhone("+91 9876543210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("9876543210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("+919876543210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("09876543210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("919876543210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("+91 98765 43210"))
        assertEquals(expected, BookingLookupHelper.normalizePhone("+91-9876543210"))
    }

    @Test
    fun testShortOrIncompletePhoneReturnsEmpty() {
        assertEquals("", BookingLookupHelper.normalizePhone("+91 "))
        assertEquals("", BookingLookupHelper.normalizePhone("+91 987"))
        assertEquals("", BookingLookupHelper.normalizePhone("98765"))
        assertEquals("", BookingLookupHelper.normalizePhone(""))
        assertEquals("", BookingLookupHelper.normalizePhone(null))
    }

    @Test
    fun testLookupMatchesAcrossAllTabsAndCategories() {
        val localPlant = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious",
            bookingDate = "25/08/2026",
            paymentStatus = "Pending",
            timestamp = 1000L
        )

        val gardenPlanning = GardenPlanningEntry(
            id = 2L,
            serialNumber = "GP-15",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "9876543210",
            bookingDate = "23/08/2026",
            paymentStatus = "Advance Paid",
            timestamp = 2000L
        )

        val importedPlant = CropRecord(
            id = 3L,
            serialNumber = "IP-22",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+919876543210",
            serviceType = "Imported",
            plantVariety = "Gala",
            bookingDate = "20/08/2026",
            paymentStatus = "Fully Paid",
            timestamp = 3000L
        )

        val cropRecords = listOf(localPlant, importedPlant)
        val gardenEntries = listOf(gardenPlanning)

        // Lookup with "+91 9876543210"
        val matches1 = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "+91 9876543210",
            cropRecords = cropRecords,
            gardenEntries = gardenEntries
        )

        assertEquals(3, matches1.size)
        // Sorted by newest timestamp descending: IP-22 (3000L), GP-15 (2000L), LP-69 (1000L)
        assertEquals("IP-22", matches1[0].serialNumber)
        assertEquals("Imported Plants", matches1[0].category)
        assertEquals("Fully Paid", matches1[0].paymentStatus)

        assertEquals("GP-15", matches1[1].serialNumber)
        assertEquals("Garden Planning", matches1[1].category)
        assertEquals("Advance Paid", matches1[1].paymentStatus)

        assertEquals("LP-69", matches1[2].serialNumber)
        assertEquals("Local Plants", matches1[2].category)
        assertEquals("Pending", matches1[2].paymentStatus)

        // Matching with standard 10 digits
        val matches2 = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "9876543210",
            cropRecords = cropRecords,
            gardenEntries = gardenEntries
        )
        assertEquals(3, matches2.size)

        // Matching with 0 prefix
        val matches3 = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "09876543210",
            cropRecords = cropRecords,
            gardenEntries = gardenEntries
        )
        assertEquals(3, matches3.size)
    }

    @Test
    fun testExcludeCurrentEditingRecord() {
        val localPlant = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val gardenPlanning = GardenPlanningEntry(
            id = 2L,
            serialNumber = "GP-15",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210"
        )

        // Editing LP-69 (id=1L) -> should only return GP-15
        val matches1 = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "9876543210",
            cropRecords = listOf(localPlant),
            gardenEntries = listOf(gardenPlanning),
            excludeCropId = 1L
        )
        assertEquals(1, matches1.size)
        assertEquals("GP-15", matches1[0].serialNumber)

        // Editing GP-15 (id=2L) -> should only return LP-69
        val matches2 = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "9876543210",
            cropRecords = listOf(localPlant),
            gardenEntries = listOf(gardenPlanning),
            excludeGardenId = 2L
        )
        assertEquals(1, matches2.size)
        assertEquals("LP-69", matches2[0].serialNumber)
    }

    @Test
    fun testNoMatchForUnusedNumber() {
        val localPlant = CropRecord(
            id = 1L,
            serialNumber = "LP-69",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Shopian",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val matches = BookingLookupHelper.findMatchingBookings(
            rawInputPhone = "+91 9123456789",
            cropRecords = listOf(localPlant),
            gardenEntries = emptyList()
        )
        assertTrue(matches.isEmpty())
    }
}
