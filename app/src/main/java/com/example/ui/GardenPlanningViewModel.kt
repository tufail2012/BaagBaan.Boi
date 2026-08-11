package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GardenPlanningEntry
import com.example.data.GardenPlanningRepository
import com.example.util.MessageTemplateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GardenPlanningViewModel(
    private val repository: GardenPlanningRepository
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val allEntries: StateFlow<List<GardenPlanningEntry>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow("All Records")
    val selectedPaymentFilter: StateFlow<String> = _selectedPaymentFilter.asStateFlow()

    val filteredEntries: StateFlow<List<GardenPlanningEntry>> = combine(allEntries, _searchQuery, _selectedPaymentFilter) { entries, query, paymentFilter ->
        var result = entries
        if (query.isNotBlank()) {
            val q = query.lowercase(Locale.getDefault())
            result = result.filter {
                it.farmerName.lowercase(Locale.getDefault()).contains(q) ||
                        it.serialNumber.lowercase(Locale.getDefault()).contains(q) ||
                        it.contactNumber.lowercase(Locale.getDefault()).contains(q) ||
                        it.farmerAddress.lowercase(Locale.getDefault()).contains(q)
            }
        }
        when (paymentFilter) {
            "Pending" -> result.filter { it.paymentStatus == "Pending" || it.paymentStatus == "Unpaid" }
            "Advance Paid" -> result.filter { it.paymentStatus == "Advance Paid" }
            "Fully Paid" -> result.filter { it.paymentStatus == "Fully Paid" }
            "Payments Cleared" -> result.filter { it.paymentStatus == "Fully Paid" }
            "Payments Pending" -> result.filter { it.paymentStatus != "Fully Paid" }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPaymentFilter(filter: String) {
        _selectedPaymentFilter.value = filter
    }

    val editingEntryId = MutableStateFlow<Long?>(null)

    val serialNumber = MutableStateFlow("")
    val farmerName = MutableStateFlow("")
    val farmerAddress = MutableStateFlow("")
    val contactNumber = MutableStateFlow("")
    val totalKanalArea = MutableStateFlow("")
    val plantsPerKanal = MutableStateFlow("")
    val costPerPlant = MutableStateFlow("")
    val plantVariety = MutableStateFlow("")
    val rootStock = MutableStateFlow("")
    val saplingAge = MutableStateFlow("1 Year")
    val amountPaid = MutableStateFlow("0")
    val paymentStatus = MutableStateFlow("Pending")
    val bookingDate = MutableStateFlow(dateFormatter.format(Date()))
    val expectedDelivery = MutableStateFlow(dateFormatter.format(Date()))
    val notes = MutableStateFlow("")
    val selectedTemplate = MutableStateFlow("Booking Confirmation")
    val userMessage = MutableStateFlow<String?>(null)

    init {
        // Auto-generate initial serial number
        viewModelScope.launch {
            allEntries.collect { entries ->
                if (editingEntryId.value == null && serialNumber.value.isBlank()) {
                    serialNumber.value = generateNextSerialNumber("GP-01", entries)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun calculateTotalCost(): Double {
        val area = totalKanalArea.value.toDoubleOrNull() ?: 0.0
        val plants = plantsPerKanal.value.toDoubleOrNull() ?: 0.0
        val cost = costPerPlant.value.toDoubleOrNull() ?: 0.0
        return area * plants * cost
    }

    fun calculateAmountPaid(): Double {
        return amountPaid.value.toDoubleOrNull() ?: 0.0
    }

    fun calculateRemainingBalance(): Double {
        return (calculateTotalCost() - calculateAmountPaid()).coerceAtLeast(0.0)
    }

    fun onAmountPaidChanged(newAmount: String) {
        amountPaid.value = newAmount
        val paid = newAmount.toDoubleOrNull() ?: 0.0
        val total = calculateTotalCost()
        if (paid <= 0.0) {
            paymentStatus.value = "Pending"
        } else if (total > 0.0 && paid >= total) {
            paymentStatus.value = "Fully Paid"
        } else {
            paymentStatus.value = "Advance Paid"
        }
    }

    fun onPaymentStatusSelected(status: String) {
        paymentStatus.value = status
        val total = calculateTotalCost()
        when (status) {
            "Pending" -> amountPaid.value = "0"
            "Fully Paid" -> amountPaid.value = if (total > 0 && total % 1.0 == 0.0) total.toLong().toString() else if (total > 0) total.toString() else "0"
            "Advance Paid" -> {
                val currentPaid = amountPaid.value.toDoubleOrNull() ?: 0.0
                if (currentPaid <= 0.0 || (total > 0 && currentPaid >= total)) {
                    val half = total / 2.0
                    amountPaid.value = if (half > 0 && half % 1.0 == 0.0) half.toLong().toString() else if (half > 0) half.toString() else "0"
                }
            }
        }
    }

    fun generateNextSerialNumber(currentSerial: String, existingEntries: List<GardenPlanningEntry>): String {
        val rawSerial = currentSerial.trim()
        var prefix = ""
        var currentDigitsStr: String? = null

        if (rawSerial.isNotBlank()) {
            val regex = Regex("""^(.*?)(\d+)$""")
            val match = regex.find(rawSerial)
            if (match != null) {
                prefix = match.groupValues[1]
                currentDigitsStr = match.groupValues[2]
                if (!prefix.endsWith("-") && prefix.isNotBlank()) {
                    prefix = "$prefix-"
                }
            } else {
                prefix = if (rawSerial.endsWith("-")) rawSerial else "$rawSerial-"
            }
        }

        if (prefix.isBlank() || prefix == "-") {
            prefix = "GP-"
        }

        val cleanPrefix = prefix.trim()
        var maxVal = 0L
        var maxDigitLen = currentDigitsStr?.length ?: 2

        currentDigitsStr?.toLongOrNull()?.let { inputVal ->
            if (inputVal > maxVal) {
                maxVal = inputVal
            }
        }

        existingEntries.forEach { entry ->
            val recSerial = entry.serialNumber.trim()
            if (recSerial.isBlank()) return@forEach

            val recRegex = Regex("""^(${Regex.escape(cleanPrefix)})(\d+)$""", RegexOption.IGNORE_CASE)
            val recMatch = recRegex.find(recSerial)
            if (recMatch != null) {
                val digitsPart = recMatch.groupValues[2]
                val numVal = digitsPart.toLongOrNull()
                if (numVal != null) {
                    if (numVal > maxVal) maxVal = numVal
                    if (digitsPart.length > maxDigitLen) maxDigitLen = digitsPart.length
                }
            } else if (recSerial.startsWith(cleanPrefix, ignoreCase = true)) {
                val remaining = recSerial.substring(cleanPrefix.length).trim()
                val digitsOnly = remaining.takeWhile { it.isDigit() }
                if (digitsOnly.isNotEmpty()) {
                    val numVal = digitsOnly.toLongOrNull()
                    if (numVal != null) {
                        if (numVal > maxVal) maxVal = numVal
                        if (digitsOnly.length > maxDigitLen) maxDigitLen = digitsOnly.length
                    }
                }
            }
        }

        val nextVal = maxVal + 1
        val formattedDigits = "%0${maxDigitLen}d".format(nextVal)
        return "$prefix$formattedDigits"
    }

    fun resetSerialNumber() {
        serialNumber.value = generateNextSerialNumber("GP-01", allEntries.value)
    }

    fun loadEntryForEdit(entry: GardenPlanningEntry) {
        editingEntryId.value = entry.id
        serialNumber.value = entry.serialNumber
        farmerName.value = entry.farmerName
        farmerAddress.value = entry.farmerAddress
        contactNumber.value = entry.contactNumber
        totalKanalArea.value = if (entry.totalKanalArea > 0) entry.totalKanalArea.toString() else ""
        plantsPerKanal.value = if (entry.plantsPerKanal > 0) entry.plantsPerKanal.toString() else ""
        costPerPlant.value = if (entry.costPerPlant > 0) entry.costPerPlant.toString() else ""
        plantVariety.value = entry.plantVariety
        rootStock.value = entry.rootStock
        saplingAge.value = entry.saplingAge.ifBlank { "1 Year" }
        val paidVal = if (entry.amountPaid > 0) entry.amountPaid else if (entry.paymentStatus == "Fully Paid") entry.totalCost else 0.0
        amountPaid.value = if (paidVal > 0) {
            if (paidVal % 1.0 == 0.0) paidVal.toLong().toString() else paidVal.toString()
        } else "0"
        paymentStatus.value = entry.paymentStatus
        bookingDate.value = entry.bookingDate.ifBlank { dateFormatter.format(Date()) }
        expectedDelivery.value = entry.expectedDelivery.ifBlank { dateFormatter.format(Date()) }
        notes.value = entry.notes
    }

    fun clearForm() {
        editingEntryId.value = null
        farmerName.value = ""
        farmerAddress.value = ""
        contactNumber.value = ""
        totalKanalArea.value = ""
        plantsPerKanal.value = ""
        costPerPlant.value = ""
        plantVariety.value = ""
        rootStock.value = ""
        saplingAge.value = "1 Year"
        amountPaid.value = "0"
        paymentStatus.value = "Pending"
        bookingDate.value = dateFormatter.format(Date())
        expectedDelivery.value = dateFormatter.format(Date())
        notes.value = ""
        resetSerialNumber()
    }

    suspend fun saveEntrySync(): Boolean {
        if (farmerName.value.isBlank()) {
            userMessage.value = "Farmer Name is required"
            return false
        }

        val area = totalKanalArea.value.toDoubleOrNull() ?: 0.0
        val plants = plantsPerKanal.value.toDoubleOrNull()?.toInt() ?: plantsPerKanal.value.toIntOrNull() ?: 0
        val cost = costPerPlant.value.toDoubleOrNull() ?: 0.0
        val calcTotalCost = area * plants * cost
        val paid = amountPaid.value.toDoubleOrNull() ?: 0.0
        val rem = (calcTotalCost - paid).coerceAtLeast(0.0)

        val sn = serialNumber.value.ifBlank { generateNextSerialNumber("GP-01", allEntries.value) }

        val entry = GardenPlanningEntry(
            id = editingEntryId.value ?: 0L,
            serialNumber = sn,
            farmerName = farmerName.value.trim(),
            farmerAddress = farmerAddress.value.trim(),
            contactNumber = contactNumber.value.trim(),
            totalKanalArea = area,
            plantsPerKanal = plants,
            costPerPlant = cost,
            plantVariety = plantVariety.value.trim(),
            rootStock = rootStock.value.trim(),
            saplingAge = saplingAge.value.trim(),
            totalCost = calcTotalCost,
            amountPaid = paid,
            remainingBalance = rem,
            paymentStatus = paymentStatus.value,
            bookingDate = bookingDate.value,
            expectedDelivery = expectedDelivery.value,
            notes = notes.value.trim(),
            timestamp = System.currentTimeMillis()
        )

        try {
            if (entry.id == 0L) {
                repository.insert(entry)
                userMessage.value = "Garden Planning Entry Saved Successfully!"
            } else {
                repository.update(entry)
                userMessage.value = "Garden Planning Entry Updated Successfully!"
            }
            clearForm()
            return true
        } catch (e: Exception) {
            userMessage.value = "Error saving entry: ${e.message}"
            return false
        }
    }

    fun updateEntrySync(entry: GardenPlanningEntry) {
        viewModelScope.launch {
            try {
                repository.update(entry)
                userMessage.value = "Entry updated successfully"
            } catch (e: Exception) {
                userMessage.value = "Error updating entry: ${e.message}"
            }
        }
    }

    fun deleteEntry(entry: GardenPlanningEntry) {
        viewModelScope.launch {
            try {
                repository.delete(entry)
                userMessage.value = "Entry deleted"
            } catch (e: Exception) {
                userMessage.value = "Error deleting entry: ${e.message}"
            }
        }
    }

    fun getGeneratedPreviewMessage(): String {
        val area = totalKanalArea.value.toDoubleOrNull() ?: 0.0
        val plants = plantsPerKanal.value.toIntOrNull() ?: 0
        val totalPlants = (area * plants).toInt()
        val totalAmount = calculateTotalCost()
        val paid = calculateAmountPaid()
        val rem = calculateRemainingBalance()

        val varietyInfo = buildString {
            if (plantVariety.value.isNotBlank()) {
                append(plantVariety.value.trim())
                if (rootStock.value.isNotBlank()) {
                    append(" (${rootStock.value.trim()})")
                }
                append(" • ")
            }
            append("$area Kanals ($plants/Kanal)")
        }

        return MessageTemplateHelper.generateMessage(
            template = selectedTemplate.value,
            farmerName = farmerName.value,
            contactNumber = contactNumber.value,
            address = farmerAddress.value,
            location = farmerAddress.value,
            serviceCategory = "Garden Planning",
            plantVariety = varietyInfo,
            quantity = totalPlants.toString(),
            totalAmount = totalAmount,
            amountPaid = paid,
            remainingBalance = rem,
            paymentStatus = paymentStatus.value,
            bookingDate = bookingDate.value,
            expectedDelivery = expectedDelivery.value,
            serialNumber = serialNumber.value
        )
    }

    class Factory(private val repository: GardenPlanningRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GardenPlanningViewModel::class.java)) {
                return GardenPlanningViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
