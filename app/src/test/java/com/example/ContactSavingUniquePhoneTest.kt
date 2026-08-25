package com.example

import com.example.data.CropRecord
import com.example.data.FarmerContact
import com.example.data.GardenPlanningEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContactSavingUniquePhoneTest {

    @Test
    fun testTwoBookingsWithSameNameDifferentPhonesCreateSeparateKeys() {
        val booking1 = CropRecord(
            id = 1L,
            serialNumber = "LP-01",
            farmerName = "Tariq Ahmad",
            contactNumber = "9876543210",
            farmerAddress = "Shopian",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val booking2 = CropRecord(
            id = 2L,
            serialNumber = "LP-02",
            farmerName = "Tariq Ahmad",
            contactNumber = "9123456789",
            farmerAddress = "Kulgam",
            serviceType = "Local Plants",
            plantVariety = "Gala"
        )

        val phone1 = booking1.contactNumber.filter { it.isDigit() }.takeLast(10)
        val phone2 = booking2.contactNumber.filter { it.isDigit() }.takeLast(10)

        // Contact identity must be distinct based on phone
        assertNotEquals(phone1, phone2)
        assertEquals("9876543210", phone1)
        assertEquals("9123456789", phone2)

        val contacts = mutableListOf<FarmerContact>()

        fun syncContact(record: CropRecord) {
            val cleanPhone = record.contactNumber.filter { it.isDigit() }.takeLast(10)
            val existing = if (cleanPhone.isNotEmpty()) {
                contacts.firstOrNull { it.phone.filter { c -> c.isDigit() }.takeLast(10) == cleanPhone }
            } else {
                contacts.firstOrNull { it.phone.isBlank() && it.name.trim().equals(record.farmerName.trim(), ignoreCase = true) }
            }

            if (existing == null) {
                contacts.add(
                    FarmerContact(
                        id = contacts.size.toLong() + 1,
                        name = record.farmerName,
                        phone = record.contactNumber,
                        address = record.farmerAddress,
                        category = "Farmer"
                    )
                )
            } else {
                val idx = contacts.indexOf(existing)
                contacts[idx] = existing.copy(
                    name = record.farmerName.ifBlank { existing.name },
                    address = record.farmerAddress.ifBlank { existing.address }
                )
            }
        }

        syncContact(booking1)
        syncContact(booking2)

        // Must have two separate contacts in directory despite having the exact same name
        assertEquals(2, contacts.size)
        assertEquals("9876543210", contacts[0].phone)
        assertEquals("9123456789", contacts[1].phone)
        assertEquals("Tariq Ahmad", contacts[0].name)
        assertEquals("Tariq Ahmad", contacts[1].name)
    }

    @Test
    fun testBookingsWithSamePhoneUpdatesExistingContact() {
        val booking1 = CropRecord(
            id = 1L,
            serialNumber = "LP-01",
            farmerName = "Tariq Ahmad",
            contactNumber = "+91 9876543210",
            farmerAddress = "Shopian",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )
        val booking2 = CropRecord(
            id = 2L,
            serialNumber = "LP-02",
            farmerName = "Tariq Ahmad",
            contactNumber = "9876543210",
            farmerAddress = "Shopian, Kashmir",
            serviceType = "Local Plants",
            plantVariety = "Red Delicious"
        )

        val contacts = mutableListOf<FarmerContact>()

        fun syncContact(record: CropRecord) {
            val cleanPhone = record.contactNumber.filter { it.isDigit() }.takeLast(10)
            val existing = if (cleanPhone.isNotEmpty()) {
                contacts.firstOrNull { it.phone.filter { c -> c.isDigit() }.takeLast(10) == cleanPhone }
            } else {
                contacts.firstOrNull { it.phone.isBlank() && it.name.trim().equals(record.farmerName.trim(), ignoreCase = true) }
            }

            if (existing == null) {
                contacts.add(
                    FarmerContact(
                        id = contacts.size.toLong() + 1,
                        name = record.farmerName,
                        phone = record.contactNumber,
                        address = record.farmerAddress,
                        category = "Farmer"
                    )
                )
            } else {
                val idx = contacts.indexOf(existing)
                contacts[idx] = existing.copy(
                    address = record.farmerAddress.ifBlank { existing.address }
                )
            }
        }

        syncContact(booking1)
        syncContact(booking2)

        // Same phone must update the single contact rather than creating a duplicate
        assertEquals(1, contacts.size)
        assertEquals("Shopian, Kashmir", contacts[0].address)
    }
}
