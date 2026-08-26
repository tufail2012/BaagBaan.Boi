package com.example.data

sealed class GlobalSearchResult {
    abstract val id: Long
    abstract val farmerName: String
    abstract val contactNumber: String
    abstract val serialNumber: String
    abstract val serviceType: String
    abstract val farmerAddress: String
    abstract val timestamp: Long
    abstract val totalAmount: Double
    abstract val amountPaid: Double
    abstract val isPaymentCleared: Boolean
    abstract val isCancelled: Boolean
    abstract val isReceived: Boolean

    data class Crop(val record: CropRecord) : GlobalSearchResult() {
        override val id: Long get() = record.id
        override val farmerName: String get() = record.farmerName
        override val contactNumber: String get() = record.contactNumber
        override val serialNumber: String get() = record.serialNumber
        override val serviceType: String get() = record.serviceType
        override val farmerAddress: String get() = record.farmerAddress
        override val timestamp: Long get() = record.timestamp
        override val totalAmount: Double get() = record.calculateTotalAmount()
        override val amountPaid: Double get() = record.amountPaid
        override val isPaymentCleared: Boolean get() = record.isPaymentCleared()
        override val isCancelled: Boolean get() = record.isCancelled
        override val isReceived: Boolean get() = record.isReceived
    }

    data class Garden(val entry: GardenPlanningEntry) : GlobalSearchResult() {
        override val id: Long get() = entry.id
        override val farmerName: String get() = entry.farmerName
        override val contactNumber: String get() = entry.contactNumber
        override val serialNumber: String get() = entry.serialNumber
        override val serviceType: String get() = "Garden Planning"
        override val farmerAddress: String get() = entry.farmerAddress
        override val timestamp: Long get() = entry.timestamp
        override val totalAmount: Double get() = entry.totalCost
        override val amountPaid: Double get() = entry.amountPaid
        override val isPaymentCleared: Boolean get() = entry.remainingBalance <= 0 || entry.paymentStatus.equals("Fully Paid", ignoreCase = true)
        override val isCancelled: Boolean get() = false
        override val isReceived: Boolean get() = false
    }
}
