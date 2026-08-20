package com.example

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.util.SerialNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SerialNumberSortingTest {

    @Test
    fun testDescendingNumericalOrderSingleDigits() {
        val records = listOf(
            createCropRecord(5, "LP-05", 500L),
            createCropRecord(2, "LP-02", 200L),
            createCropRecord(4, "LP-04", 400L),
            createCropRecord(1, "LP-01", 100L),
            createCropRecord(3, "LP-03", 300L)
        )

        val sorted = records.sortedWith(SerialNumberUtils.cropRecordComparator)
        val sortedSerials = sorted.map { it.serialNumber }

        assertEquals(listOf("LP-05", "LP-04", "LP-03", "LP-02", "LP-01"), sortedSerials)
        assertEquals("LP-05", sorted.first().serialNumber)
        assertEquals("LP-01", sorted.last().serialNumber)
    }

    @Test
    fun testMultiDigitNumericalOrderDescendingDoesNotSortAlphabetically() {
        // String alphabetical descending would incorrectly place LP-09 before LP-10
        // Numerical descending sort must yield: LP-100, LP-12, LP-11, LP-10, LP-09, LP-02, LP-01
        val records = listOf(
            createCropRecord(12, "LP-12", 1200L),
            createCropRecord(10, "LP-10", 1000L),
            createCropRecord(1, "LP-01", 100L),
            createCropRecord(100, "LP-100", 10000L),
            createCropRecord(11, "LP-11", 1100L),
            createCropRecord(9, "LP-09", 900L),
            createCropRecord(2, "LP-02", 200L)
        )

        val sorted = records.sortedWith(SerialNumberUtils.cropRecordComparator)
        val sortedSerials = sorted.map { it.serialNumber }

        assertEquals(
            listOf("LP-100", "LP-12", "LP-11", "LP-10", "LP-09", "LP-02", "LP-01"),
            sortedSerials
        )
    }

    @Test
    fun testEditedBookingPreservesPositionBasedOnExistingSerialNumber() {
        // Current order: LP-05, LP-04, LP-03, LP-02, LP-01
        // If LP-02 is edited with newer timestamp (999999L), order must remain: LP-05, LP-04, LP-03, LP-02, LP-01
        val r5 = createCropRecord(5, "LP-05", 5000L)
        val r4 = createCropRecord(4, "LP-04", 4000L)
        val r3 = createCropRecord(3, "LP-03", 3000L)
        val r2Edited = createCropRecord(2, "LP-02", 999999L, farmerName = "Farmer 2 Edited")
        val r1 = createCropRecord(1, "LP-01", 1000L)

        val records = listOf(r1, r4, r2Edited, r5, r3)
        val sorted = records.sortedWith(SerialNumberUtils.cropRecordComparator)
        val sortedSerials = sorted.map { it.serialNumber }

        assertEquals(listOf("LP-05", "LP-04", "LP-03", "LP-02", "LP-01"), sortedSerials)
        assertEquals("Farmer 2 Edited", sorted[3].farmerName)
    }

    @Test
    fun testGardenPlanningEntrySortingDescending() {
        val entries = listOf(
            createGardenEntry(12, "GP-12"),
            createGardenEntry(2, "GP-02"),
            createGardenEntry(1, "GP-01"),
            createGardenEntry(10, "GP-10")
        )

        val sorted = entries.sortedWith(SerialNumberUtils.gardenEntryComparator)
        val sortedSerials = sorted.map { it.serialNumber }

        assertEquals(listOf("GP-12", "GP-10", "GP-02", "GP-01"), sortedSerials)
    }

    @Test
    fun testExtractNumericValueHelper() {
        assertEquals(1L, SerialNumberUtils.extractNumericValue("LP-01"))
        assertEquals(2L, SerialNumberUtils.extractNumericValue("LP-02"))
        assertEquals(9L, SerialNumberUtils.extractNumericValue("LP-09"))
        assertEquals(10L, SerialNumberUtils.extractNumericValue("LP-10"))
        assertEquals(105L, SerialNumberUtils.extractNumericValue("IMP-105"))
        assertEquals(3088L, SerialNumberUtils.extractNumericValue("RS-3088"))
        assertEquals(7L, SerialNumberUtils.extractNumericValue("7"))
        assertEquals(-1L, SerialNumberUtils.extractNumericValue(""))
        assertEquals(-1L, SerialNumberUtils.extractNumericValue("NoDigits"))
    }

    private fun createCropRecord(
        id: Long,
        serialNumber: String,
        timestamp: Long,
        farmerName: String = "Test Farmer"
    ): CropRecord {
        return CropRecord(
            id = id,
            serialNumber = serialNumber,
            farmerName = farmerName,
            farmerAddress = "Shop 12",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Gala",
            rootstock = "M9",
            quantity = 10,
            landAreaAcres = 2.0,
            soilType = "Loam",
            healthStage = "Healthy",
            location = "Block A",
            notes = "",
            amountPaid = 1000.0,
            paymentStatus = "Cleared",
            bookingDate = "01/01/2026",
            expectedDelivery = "01/02/2026",
            timestamp = timestamp
        )
    }

    private fun createGardenEntry(id: Long, serialNumber: String): GardenPlanningEntry {
        return GardenPlanningEntry(
            id = id,
            serialNumber = serialNumber,
            farmerName = "Test Farmer",
            farmerAddress = "Field 1",
            contactNumber = "+91 9876543210",
            totalKanalArea = 5.0,
            plantsPerKanal = 100,
            costPerPlant = 250.0,
            plantVariety = "Gala",
            rootStock = "M9",
            saplingAge = "1 Year",
            plantOrigin = "Local Plants",
            totalCost = 125000.0,
            amountPaid = 25000.0,
            remainingBalance = 100000.0,
            paymentStatus = "Advance Paid",
            bookingDate = "01/01/2026",
            expectedDelivery = "01/02/2026",
            notes = "",
            timestamp = System.currentTimeMillis()
        )
    }
}
