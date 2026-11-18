package com.example

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.util.RootstockFilter
import com.example.util.buildRootstockFilterOptionsForCropRecords
import com.example.util.buildRootstockFilterOptionsForGardenEntries
import com.example.util.classifyCropRecord
import com.example.util.classifyGardenEntry
import com.example.util.classifyRootstockValue
import com.example.util.matchesRootstockFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootstockFilterUtilsTest {

    @Test
    fun testM9FamilyVariantsClassifyAsM9() {
        val m9Variants = listOf(
            "M9-T337",
            "M9T337",
            "m9 t337",
            "M9 - T337",
            "T337",
            "M9",
            "m09-t337"
        )
        for (variant in m9Variants) {
            assertEquals(
                "Expected '$variant' to classify as Family(M9)",
                RootstockFilter.Family("M9"),
                classifyRootstockValue(variant)
            )
        }
    }

    @Test
    fun testMmFamilyVariantsClassifyAsMM() {
        val mmVariants = listOf(
            "MM111",
            "mm 111",
            "MM-111",
            "MM 111",
            "mm111",
            "MM106"
        )
        for (variant in mmVariants) {
            assertEquals(
                "Expected '$variant' to classify as Family(MM)",
                RootstockFilter.Family("MM"),
                classifyRootstockValue(variant)
            )
        }
    }

    @Test
    fun testOtherClassificationAndGroupingKeepsFirstSpelling() {
        val first = classifyRootstockValue("Geneva 41")
        val second = classifyRootstockValue("geneva41")
        val third = classifyRootstockValue("GENEVA-41")

        assertTrue(first is RootstockFilter.Other)
        assertEquals(first, second)
        assertEquals(first, third)

        val records = listOf(
            createCropRecord(id = 1L, rootstock = "Geneva 41"),
            createCropRecord(id = 2L, rootstock = "geneva41")
        )
        val options = buildRootstockFilterOptionsForCropRecords(records)
        val otherOptions = options.filter { it.filter is RootstockFilter.Other }

        assertEquals(1, otherOptions.size)
        assertEquals("Geneva 41 (2)", otherOptions.first().dropdownLabel)
        assertEquals("Geneva 41", otherOptions.first().shortLabel)
        assertEquals(2, otherOptions.first().count)
    }

    @Test
    fun testMultiVarietyCropRecordWithM9AndMmLinesCountsUnderBothFamilies() {
        val varietyLinesJson = """
            [
                {"variety":"Gala","rootstock":"M9-T337","quantity":50,"totalPlants":50,"unitPrice":250.0},
                {"variety":"Red Delicious","rootstock":"mm 111","quantity":40,"totalPlants":40,"unitPrice":220.0}
            ]
        """.trimIndent()

        // Top-level rootstock only stores the first line's value
        val record = createCropRecord(
            id = 10L,
            rootstock = "M9-T337",
            varietyLinesJson = varietyLinesJson
        )

        val classifications = classifyCropRecord(record)
        assertTrue(RootstockFilter.Family("M9") in classifications)
        assertTrue(RootstockFilter.Family("MM") in classifications)
        assertTrue(matchesRootstockFilter(record, RootstockFilter.Family("M9")))
        assertTrue(matchesRootstockFilter(record, RootstockFilter.Family("MM")))

        val options = buildRootstockFilterOptionsForCropRecords(listOf(record))
        val m9Option = options.first { it.filter == RootstockFilter.Family("M9") }
        val mmOption = options.first { it.filter == RootstockFilter.Family("MM") }
        assertEquals(1, m9Option.count)
        assertEquals(1, mmOption.count)
    }

    @Test
    fun testGardenEntryWithCommaSeparatedM9AndMmCountsUnderBothFamilies() {
        val entry = createGardenEntry(
            id = 20L,
            rootStock = "M9-T337, MM111"
        )

        val classifications = classifyGardenEntry(entry)
        assertEquals(
            setOf(RootstockFilter.Family("M9"), RootstockFilter.Family("MM")),
            classifications
        )
        assertTrue(matchesRootstockFilter(entry, RootstockFilter.Family("M9")))
        assertTrue(matchesRootstockFilter(entry, RootstockFilter.Family("MM")))
        assertFalse(matchesRootstockFilter(entry, RootstockFilter.NotSpecified))

        val options = buildRootstockFilterOptionsForGardenEntries(listOf(entry))
        val m9Option = options.first { it.filter == RootstockFilter.Family("M9") }
        val mmOption = options.first { it.filter == RootstockFilter.Family("MM") }
        assertEquals("M9 (1)", m9Option.dropdownLabel)
        assertEquals("MM (1)", mmOption.dropdownLabel)
    }

    @Test
    fun testBlankRootstockIsClassifiedAsNotSpecified() {
        val cropRecord = createCropRecord(id = 30L, rootstock = "   ", notes = "")
        val gardenEntry = createGardenEntry(id = 31L, rootStock = "")

        assertEquals(setOf(RootstockFilter.NotSpecified), classifyCropRecord(cropRecord))
        assertEquals(setOf(RootstockFilter.NotSpecified), classifyGardenEntry(gardenEntry))
        assertTrue(matchesRootstockFilter(cropRecord, RootstockFilter.NotSpecified))
        assertTrue(matchesRootstockFilter(gardenEntry, RootstockFilter.NotSpecified))

        val optionsWithBlank = buildRootstockFilterOptionsForCropRecords(listOf(cropRecord))
        assertTrue(optionsWithBlank.any { it.filter == RootstockFilter.NotSpecified && it.dropdownLabel == "Not Specified (1)" })

        val nonBlankRecord = createCropRecord(id = 32L, rootstock = "M9T337")
        val optionsWithoutBlank = buildRootstockFilterOptionsForCropRecords(listOf(nonBlankRecord))
        assertFalse(optionsWithoutBlank.any { it.filter == RootstockFilter.NotSpecified })
    }

    @Test
    fun testLegacyNotesRootstockFallbackPickedUpForEmptyCropRootstockField() {
        val legacyRecord = createCropRecord(
            id = 40L,
            rootstock = "",
            varietyLinesJson = "",
            notes = "Delivered early [Rootstock: M9-T337 | Feathers: 3+]"
        )

        val classifications = classifyCropRecord(legacyRecord)
        assertEquals(setOf(RootstockFilter.Family("M9")), classifications)
        assertTrue(matchesRootstockFilter(legacyRecord, RootstockFilter.Family("M9")))
        assertFalse(matchesRootstockFilter(legacyRecord, RootstockFilter.NotSpecified))
    }

    private fun createCropRecord(
        id: Long,
        rootstock: String = "",
        varietyLinesJson: String = "",
        notes: String = ""
    ): CropRecord {
        return CropRecord(
            id = id,
            serialNumber = "LP-0$id",
            farmerName = "Test Farmer",
            farmerAddress = "Shop 12",
            contactNumber = "+91 9876543210",
            serviceType = "Local Plants",
            plantVariety = "Red Gala",
            rootstock = rootstock,
            quantity = 10,
            landAreaAcres = 250.0,
            soilType = "Loam",
            healthStage = "1 Year",
            location = "Block A",
            notes = notes,
            amountPaid = 1000.0,
            paymentStatus = "Pending",
            bookingDate = "01/01/2026",
            expectedDelivery = "01/02/2026",
            varietyLinesJson = varietyLinesJson,
            timestamp = 1000L
        )
    }

    private fun createGardenEntry(
        id: Long,
        rootStock: String = "",
        varietyLinesJson: String = ""
    ): GardenPlanningEntry {
        return GardenPlanningEntry(
            id = id,
            serialNumber = "GP-0$id",
            farmerName = "Test Farmer",
            farmerAddress = "Field 1",
            contactNumber = "+91 9876543210",
            totalKanalArea = 5.0,
            plantsPerKanal = 100,
            costPerPlant = 250.0,
            plantVariety = "Gala",
            rootStock = rootStock,
            saplingAge = "1 Year",
            plantOrigin = "Local Plants",
            totalCost = 125000.0,
            amountPaid = 25000.0,
            remainingBalance = 100000.0,
            paymentStatus = "Advance Paid",
            bookingDate = "01/01/2026",
            expectedDelivery = "01/02/2026",
            notes = "",
            varietyLinesJson = varietyLinesJson,
            timestamp = 1000L
        )
    }
}
