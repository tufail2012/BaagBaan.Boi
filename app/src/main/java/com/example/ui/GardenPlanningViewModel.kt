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

    val filteredEntries: StateFlow<List<GardenPlanningEntry>> = combine(allEntries, _searchQuery) { entries, query ->
        if (query.isBlank()) {
            entries
        } else {
            val q = query.lowercase(Locale.getDefault())
            entries.filter {
                it.farmerName.lowercase(Locale.getDefault()).contains(q) ||
                        it.serialNumber.lowercase(Locale.getDefault()).contains(q) ||
                        it.contactNumber.lowercase(Locale.getDefault()).contains(q) ||
                        it.farmerAddress.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val editingEntryId = MutableStateFlow<Long?>(null)

    val serialNumber = MutableStateFlow("")
    val farmerName = MutableStateFlow("")
    val farmerAddress = MutableStateFlow("")
    val contactNumber = MutableStateFlow("")
    val totalKanalArea = MutableStateFlow("")
    val plantsPerKanal = MutableStateFlow("")
    val costPerPlant = MutableStateFlow("")
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
        val plants = plantsPerKanal.value.toIntOrNull() ?: 0
        val cost = costPerPlant.value.toDoubleOrNull() ?: 0.0
        return area * plants * cost
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
        val plants = plantsPerKanal.value.toIntOrNull() ?: 0
        val cost = costPerPlant.value.toDoubleOrNull() ?: 0.0
        val calcTotalCost = area * plants * cost

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
            totalCost = calcTotalCost,
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

        return MessageTemplateHelper.generateMessage(
            template = selectedTemplate.value,
            farmerName = farmerName.value,
            contactNumber = contactNumber.value,
            address = farmerAddress.value,
            location = farmerAddress.value,
            serviceCategory = "Garden Planning",
            plantVariety = "$area Kanals ($plants Plants/Kanal)",
            quantity = totalPlants.toString(),
            totalAmount = totalAmount,
            amountPaid = if (paymentStatus.value == "Fully Paid") totalAmount else 0.0,
            remainingBalance = if (paymentStatus.value == "Fully Paid") 0.0 else totalAmount,
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
