package com.example

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.GlobalSearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchResultBadgeStatusTest {

    private fun resolveStatusBadge(item: GlobalSearchResult): String {
        return when {
            item.isCancelled -> "Cancelled"
            item.isReceived -> "Received"
            item.isPaymentCleared -> "Fully Paid"
            item.amountPaid > 0 -> "Advance Paid"
            else -> "Pending"
        }
    }

    @Test
    fun testCancelledBookingShowsCancelledBadgeRegardlessOfPaymentStatus() {
        val cancelledAdvancePaid = CropRecord(
            id = 1L,
            serialNumber = "M9-10",
            farmerName = "Mudassir Bashir",
            farmerAddress = "Kashmir",
            contactNumber = "+91 7006396102",
            serviceType = "Rootstocks",
            plantVariety = "M9 T337",
            amountPaid = 7000.0,
            paymentStatus = "Advance Paid",
            isCancelled = true,
            cancelledDate = "25-08-2026"
        )
        val searchResult1 = GlobalSearchResult.Crop(cancelledAdvancePaid)
        assertEquals("Cancelled", resolveStatusBadge(searchResult1))
        assertEquals(7000.0, searchResult1.amountPaid, 0.001)

        val cancelledFullyPaid = CropRecord(
            id = 2L,
            serialNumber = "M9-11",
            farmerName = "Tariq Ahmad",
            farmerAddress = "Kashmir",
            contactNumber = "+91 7006000000",
            serviceType = "Rootstocks",
            plantVariety = "M9 T337",
            amountPaid = 50000.0,
            paymentStatus = "Fully Paid",
            isCancelled = true,
            cancelledDate = "25-08-2026"
        )
        val searchResult2 = GlobalSearchResult.Crop(cancelledFullyPaid)
        assertEquals("Cancelled", resolveStatusBadge(searchResult2))

        val cancelledPending = CropRecord(
            id = 3L,
            serialNumber = "M9-12",
            farmerName = "Showkat Ali",
            farmerAddress = "Kashmir",
            contactNumber = "+91 7006000001",
            serviceType = "Rootstocks",
            plantVariety = "M9 T337",
            amountPaid = 0.0,
            paymentStatus = "Pending",
            isCancelled = true
        )
        val searchResult3 = GlobalSearchResult.Crop(cancelledPending)
        assertEquals("Cancelled", resolveStatusBadge(searchResult3))
    }

    @Test
    fun testReceivedBookingShowsReceivedBadge() {
        val receivedBooking = CropRecord(
            id = 4L,
            serialNumber = "LP-05",
            farmerName = "Bilal Dar",
            farmerAddress = "Kashmir",
            contactNumber = "+91 9906112233",
            serviceType = "Local Plants",
            plantVariety = "Gala",
            amountPaid = 20000.0,
            paymentStatus = "Advance Paid",
            isReceived = true,
            receivedDate = "25-08-2026"
        )
        val searchResult = GlobalSearchResult.Crop(receivedBooking)
        assertEquals("Received", resolveStatusBadge(searchResult))

        val receivedFullyPaid = CropRecord(
            id = 5L,
            serialNumber = "LP-06",
            farmerName = "Ghulam Hassan",
            farmerAddress = "Kashmir",
            contactNumber = "+91 9906112234",
            serviceType = "Local Plants",
            plantVariety = "Gala",
            amountPaid = 100000.0,
            paymentStatus = "Fully Paid",
            isReceived = true,
            receivedDate = "25-08-2026"
        )
        val searchResultFullyPaid = GlobalSearchResult.Crop(receivedFullyPaid)
        assertEquals("Received", resolveStatusBadge(searchResultFullyPaid))
    }

    @Test
    fun testActiveBookingsShowPaymentStatus() {
        val fullyPaid = CropRecord(
            id = 6L,
            serialNumber = "IMP-01",
            farmerName = "Zahoor Lone",
            farmerAddress = "Kashmir",
            contactNumber = "+91 9906999999",
            serviceType = "Imported Plants",
            plantVariety = "Gala",
            quantity = 10,
            amountPaid = 3500.0,
            paymentStatus = "Fully Paid",
            notes = "Rate: 350"
        )
        val searchResultFullyPaid = GlobalSearchResult.Crop(fullyPaid)
        assertEquals("Fully Paid", resolveStatusBadge(searchResultFullyPaid))

        val advancePaid = CropRecord(
            id = 7L,
            serialNumber = "IMP-02",
            farmerName = "Shabir Wani",
            farmerAddress = "Kashmir",
            contactNumber = "+91 9906888888",
            serviceType = "Imported Plants",
            plantVariety = "Gala",
            quantity = 10,
            landAreaAcres = 350.0,
            amountPaid = 1000.0,
            paymentStatus = "Advance Paid",
            notes = "Rate: 350"
        )
        val searchResultAdvance = GlobalSearchResult.Crop(advancePaid)
        assertEquals("Advance Paid", resolveStatusBadge(searchResultAdvance))

        val pending = CropRecord(
            id = 8L,
            serialNumber = "IMP-03",
            farmerName = "Farooq Mir",
            farmerAddress = "Kashmir",
            contactNumber = "+91 9906777777",
            serviceType = "Imported Plants",
            plantVariety = "Gala",
            quantity = 10,
            landAreaAcres = 350.0,
            amountPaid = 0.0,
            paymentStatus = "Pending",
            notes = "Rate: 350"
        )
        val searchResultPending = GlobalSearchResult.Crop(pending)
        assertEquals("Pending", resolveStatusBadge(searchResultPending))
    }

    @Test
    fun testGardenPlanningSearchResultBadge() {
        val gardenEntry = GardenPlanningEntry(
            id = 10L,
            serialNumber = "GP-01",
            farmerName = "Altaf Malik",
            contactNumber = "+91 9906333333",
            totalCost = 50000.0,
            amountPaid = 15000.0,
            remainingBalance = 35000.0,
            paymentStatus = "Advance Paid"
        )
        val searchResult = GlobalSearchResult.Garden(gardenEntry)
        assertEquals("Advance Paid", resolveStatusBadge(searchResult))

        val gardenEntryPaid = gardenEntry.copy(
            amountPaid = 50000.0,
            remainingBalance = 0.0,
            paymentStatus = "Fully Paid"
        )
        val searchResultPaid = GlobalSearchResult.Garden(gardenEntryPaid)
        assertEquals("Fully Paid", resolveStatusBadge(searchResultPaid))
    }
}
