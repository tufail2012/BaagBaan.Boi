package com.example.data

import org.json.JSONObject

object RecycleBinConverter {

    fun cropRecordToJson(record: CropRecord): String {
        val json = JSONObject()
        json.put("id", record.id)
        json.put("serialNumber", record.serialNumber)
        json.put("farmerName", record.farmerName)
        json.put("farmerAddress", record.farmerAddress)
        json.put("contactNumber", record.contactNumber)
        json.put("serviceType", record.serviceType)
        json.put("plantVariety", record.plantVariety)
        json.put("rootstock", record.rootstock)
        json.put("quantity", record.quantity)
        json.put("landAreaAcres", record.landAreaAcres)
        json.put("soilType", record.soilType)
        json.put("healthStage", record.healthStage)
        json.put("location", record.location)
        json.put("notes", record.notes)
        json.put("amountPaid", record.amountPaid)
        json.put("paymentStatus", record.paymentStatus)
        json.put("bookingDate", record.bookingDate)
        json.put("expectedDelivery", record.expectedDelivery)
        json.put("paymentProofUri", record.paymentProofUri)
        json.put("paymentHistoryJson", record.paymentHistoryJson)
        json.put("timestamp", record.timestamp)
        return json.toString()
    }

    fun jsonToCropRecord(jsonStr: String): CropRecord {
        val json = JSONObject(jsonStr)
        return CropRecord(
            id = json.optLong("id", 0L),
            serialNumber = json.optString("serialNumber", ""),
            farmerName = json.optString("farmerName", ""),
            farmerAddress = json.optString("farmerAddress", ""),
            contactNumber = json.optString("contactNumber", ""),
            serviceType = json.optString("serviceType", "Local Plants"),
            plantVariety = json.optString("plantVariety", ""),
            rootstock = json.optString("rootstock", ""),
            quantity = json.optInt("quantity", 1),
            landAreaAcres = json.optDouble("landAreaAcres", 1.0),
            soilType = json.optString("soilType", "Loamy"),
            healthStage = json.optString("healthStage", "Active Sapling"),
            location = json.optString("location", ""),
            notes = json.optString("notes", ""),
            amountPaid = json.optDouble("amountPaid", 0.0),
            paymentStatus = json.optString("paymentStatus", "Pending"),
            bookingDate = json.optString("bookingDate", ""),
            expectedDelivery = json.optString("expectedDelivery", ""),
            paymentProofUri = json.optString("paymentProofUri", ""),
            paymentHistoryJson = json.optString("paymentHistoryJson", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    fun contactToJson(contact: FarmerContact): String {
        val json = JSONObject()
        json.put("id", contact.id)
        json.put("name", contact.name)
        json.put("phone", contact.phone)
        json.put("address", contact.address)
        json.put("category", contact.category)
        json.put("notes", contact.notes)
        json.put("timestamp", contact.timestamp)
        return json.toString()
    }

    fun jsonToContact(jsonStr: String): FarmerContact {
        val json = JSONObject(jsonStr)
        return FarmerContact(
            id = json.optLong("id", 0L),
            name = json.optString("name", ""),
            phone = json.optString("phone", ""),
            address = json.optString("address", ""),
            category = json.optString("category", "Farmer"),
            notes = json.optString("notes", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}
