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
        json.put("feathers", record.feathers)
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
        json.put("varietyLinesJson", record.varietyLinesJson)
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
            feathers = json.optString("feathers", ""),
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
            varietyLinesJson = json.optString("varietyLinesJson", ""),
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

    fun gardenPlanningToJson(entry: GardenPlanningEntry): String {
        val json = JSONObject()
        json.put("id", entry.id)
        json.put("serialNumber", entry.serialNumber)
        json.put("farmerName", entry.farmerName)
        json.put("farmerAddress", entry.farmerAddress)
        json.put("contactNumber", entry.contactNumber)
        json.put("totalKanalArea", entry.totalKanalArea)
        json.put("plantsPerKanal", entry.plantsPerKanal)
        json.put("costPerPlant", entry.costPerPlant)
        json.put("plantVariety", entry.plantVariety)
        json.put("rootStock", entry.rootStock)
        json.put("feathers", entry.feathers)
        json.put("saplingAge", entry.saplingAge)
        json.put("plantOrigin", entry.plantOrigin)
        json.put("totalCost", entry.totalCost)
        json.put("amountPaid", entry.amountPaid)
        json.put("remainingBalance", entry.remainingBalance)
        json.put("paymentStatus", entry.paymentStatus)
        json.put("bookingDate", entry.bookingDate)
        json.put("expectedDelivery", entry.expectedDelivery)
        json.put("notes", entry.notes)
        json.put("installmentHistoryJson", entry.installmentHistoryJson)
        json.put("timestamp", entry.timestamp)
        return json.toString()
    }

    fun jsonToGardenPlanning(jsonStr: String): GardenPlanningEntry {
        val json = JSONObject(jsonStr)
        return GardenPlanningEntry(
            id = json.optLong("id", 0L),
            serialNumber = json.optString("serialNumber", ""),
            farmerName = json.optString("farmerName", ""),
            farmerAddress = json.optString("farmerAddress", ""),
            contactNumber = json.optString("contactNumber", ""),
            totalKanalArea = json.optDouble("totalKanalArea", 0.0),
            plantsPerKanal = json.optInt("plantsPerKanal", 0),
            costPerPlant = json.optDouble("costPerPlant", 0.0),
            plantVariety = json.optString("plantVariety", ""),
            rootStock = json.optString("rootStock", ""),
            feathers = json.optString("feathers", ""),
            saplingAge = json.optString("saplingAge", "1 Year"),
            plantOrigin = json.optString("plantOrigin", "Local Plants"),
            totalCost = json.optDouble("totalCost", 0.0),
            amountPaid = json.optDouble("amountPaid", 0.0),
            remainingBalance = json.optDouble("remainingBalance", 0.0),
            paymentStatus = json.optString("paymentStatus", "Pending"),
            bookingDate = json.optString("bookingDate", ""),
            expectedDelivery = json.optString("expectedDelivery", ""),
            notes = json.optString("notes", ""),
            installmentHistoryJson = json.optString("installmentHistoryJson", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}
