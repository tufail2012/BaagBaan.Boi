package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CropRecord
import com.example.data.CropRecordRepository
import com.example.data.ThemePreferencesManager
import com.example.data.isPaymentCleared
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CropViewModel(private val repository: CropRecordRepository) : ViewModel() {

    private var themePreferencesManager: ThemePreferencesManager? = null

    // Theme mode state: SYSTEM, LIGHT, DARK, AMOLED
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColorArgb = MutableStateFlow(ThemePreferencesManager.DEFAULT_ACCENT_COLOR_ARGB)
    val accentColorArgb: StateFlow<Long> = _accentColorArgb.asStateFlow()

    val accentColorHex: StateFlow<String> = _accentColorArgb.map { argb ->
        val hexInt = (argb.toInt() and 0xFFFFFF)
        String.format("#%06X", hexInt)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "#D32F2F")

    fun loadThemeSettings(context: android.content.Context) {
        val manager = themePreferencesManager ?: ThemePreferencesManager(context.applicationContext).also {
            themePreferencesManager = it
        }
        viewModelScope.launch {
            manager.accentColorArgbFlow.collect { argb ->
                _accentColorArgb.value = argb
            }
        }
        viewModelScope.launch {
            manager.themeModeFlow.collect { modeStr ->
                _themeMode.value = try {
                    AppThemeMode.valueOf(modeStr)
                } catch (_: Exception) {
                    AppThemeMode.SYSTEM
                }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            themePreferencesManager?.setThemeMode(mode.name)
        }
    }

    fun setThemeMode(context: android.content.Context, mode: AppThemeMode) {
        val manager = themePreferencesManager ?: ThemePreferencesManager(context.applicationContext).also {
            themePreferencesManager = it
        }
        _themeMode.value = mode
        viewModelScope.launch {
            manager.setThemeMode(mode.name)
        }
    }

    fun setAccentColorArgb(context: android.content.Context, argb: Long) {
        val manager = themePreferencesManager ?: ThemePreferencesManager(context.applicationContext).also {
            themePreferencesManager = it
        }
        _accentColorArgb.value = argb
        viewModelScope.launch {
            manager.setAccentColor(argb)
        }
    }

    fun setAccentColorHex(context: android.content.Context, hex: String) {
        val argb = try {
            android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL
        } catch (e: Exception) {
            ThemePreferencesManager.DEFAULT_ACCENT_COLOR_ARGB
        }
        setAccentColorArgb(context, argb)
    }

    fun cycleThemeMode() {
        val nextMode = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.AMOLED
            AppThemeMode.AMOLED -> AppThemeMode.SYSTEM
        }
        _themeMode.value = nextMode
        viewModelScope.launch {
            themePreferencesManager?.setThemeMode(nextMode.name)
        }
    }

    // Main selected category in bottom bar ("Local Plants", "Imported", "Rootstocks", "Site Visit", "Pruning")
    private val _selectedService = MutableStateFlow("Local Plants")
    val selectedService: StateFlow<String> = _selectedService.asStateFlow()

    // View mode: 0 = "New Entry", 1 = "Records", 2 = "Analytics / Overview"
    private val _viewMode = MutableStateFlow(0)
    val viewMode: StateFlow<Int> = _viewMode.asStateFlow()

    // Search query & Global Search Active state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGlobalSearchActive = MutableStateFlow(false)
    val isGlobalSearchActive: StateFlow<Boolean> = _isGlobalSearchActive.asStateFlow()

    fun openGlobalSearch() {
        _isGlobalSearchActive.value = true
    }

    fun closeGlobalSearch() {
        _isGlobalSearchActive.value = false
        _searchQuery.value = ""
    }

    // Payment Status filter ("All Records", "Payments Cleared", "Payments Pending")
    private val _selectedPaymentFilter = MutableStateFlow("All Records")
    val selectedPaymentFilter: StateFlow<String> = _selectedPaymentFilter.asStateFlow()

    private val _recordSearchFilter = MutableStateFlow("All")
    val recordSearchFilter: StateFlow<String> = _recordSearchFilter.asStateFlow()

    fun setRecordSearchFilter(filter: String) {
        _recordSearchFilter.value = filter
    }

    // Filter by category in records (null or specific service)
    private val _recordsFilterService = MutableStateFlow<String?>(null)
    val recordsFilterService: StateFlow<String?> = _recordsFilterService.asStateFlow()

    // Sub-tab selection states
    private val _selectedPruningSubTab = MutableStateFlow("Summer Pruning")
    val selectedPruningSubTab: StateFlow<String> = _selectedPruningSubTab.asStateFlow()

    private val _selectedRootstockSubTab = MutableStateFlow("M9-T337")
    val selectedRootstockSubTab: StateFlow<String> = _selectedRootstockSubTab.asStateFlow()

    private val _selectedGenevaSubOption = MutableStateFlow<String?>("G41")
    val selectedGenevaOption: StateFlow<String?> = _selectedGenevaSubOption.asStateFlow()

    // Form inputs
    val serialNumber = MutableStateFlow("")
    val isSerialLocked = MutableStateFlow(false)

    // Tab-independent serial number cache
    private val tabSerialNumbers = mutableMapOf<String, String>()
    private val tabSerialLocks = mutableMapOf<String, Boolean>()

    private fun getCurrentTabKey(): String {
        val service = _selectedService.value
        return when {
            service.equals("Pruning", ignoreCase = true) -> {
                "Pruning_${_selectedPruningSubTab.value}"
            }
            service.equals("Rootstocks", ignoreCase = true) -> {
                val rootTab = _selectedRootstockSubTab.value
                if (rootTab.startsWith("Geneva")) {
                    val genevaOpt = _selectedGenevaSubOption.value ?: "G41"
                    "${service}_Geneva_${genevaOpt}"
                } else {
                    "${service}_${rootTab}"
                }
            }
            else -> service
        }
    }

    private fun saveCurrentTabSerialState() {
        val key = getCurrentTabKey()
        tabSerialNumbers[key] = serialNumber.value
        tabSerialLocks[key] = isSerialLocked.value
    }

    private fun loadTabSerialState() {
        val key = getCurrentTabKey()
        serialNumber.value = tabSerialNumbers[key] ?: ""
        isSerialLocked.value = tabSerialLocks[key] ?: false
    }

    fun updateSerialNumber(newSerial: String) {
        if (!isSerialLocked.value) {
            serialNumber.value = newSerial
            tabSerialNumbers[getCurrentTabKey()] = newSerial
        }
    }
    val farmerName = MutableStateFlow("")
    val farmerAddress = MutableStateFlow("")
    val contactNumber = MutableStateFlow("+91 ")
    val serviceType = MutableStateFlow("Local Plants")
    val plantVariety = MutableStateFlow("")
    val rootstock = MutableStateFlow("")
    val importCountry = MutableStateFlow("Italy")
    val quantity = MutableStateFlow("100")
    val landAreaAcres = MutableStateFlow("250")
    val soilType = MutableStateFlow("Clay Loam")
    val healthStage = MutableStateFlow("1 Year")
    val location = MutableStateFlow("")
    val visitDate = MutableStateFlow(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    val soilHealthObservations = MutableStateFlow("")
    val plantHealthObservations = MutableStateFlow("")
    val graftType = MutableStateFlow("Bench Grafting")
    val scionVariety = MutableStateFlow("")
    val perUnitGraftingCharge = MutableStateFlow("")
    val graftingCharges = MutableStateFlow("")
    val notes = MutableStateFlow("")

    private fun getDefaultExpectedDeliveryDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 30)
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }

    // Payment & Schedule inputs
    val amountPaid = MutableStateFlow("0")
    val paymentStatus = MutableStateFlow("Pending") // "Pending", "Advance Paid", "Fully Paid"
    val bookingDate = MutableStateFlow(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    val expectedDelivery = MutableStateFlow(getDefaultExpectedDeliveryDate())
    val paymentProofUri = MutableStateFlow("")
    val paymentProofName = MutableStateFlow("")

    val editingRecordId = MutableStateFlow<Long?>(null)

    // User feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Combined list of records based on search and filters
    val allRecords: StateFlow<List<CropRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val automaticSerials = setOf("LP-1001", "IMP-2042", "RS-3088", "SV-4012", "PR-5005")

    private fun isSameCategory(svc1: String, svc2: String): Boolean {
        fun norm(s: String) = when (s.lowercase().trim()) {
            "local", "local plants", "local plant" -> "local plants"
            "imported", "imported plants" -> "imported"
            "rootstock", "rootstocks" -> "rootstocks"
            "site visit", "site visits", "sitevisit" -> "site visit"
            "pruning", "pruning service" -> "pruning"
            else -> s.lowercase().trim()
        }
        return norm(svc1) == norm(svc2)
    }

    val filteredRecords: StateFlow<List<CropRecord>> = combine(
        repository.allRecords,
        _searchQuery,
        _selectedService,
        combine(_selectedPaymentFilter, _recordSearchFilter) { pf, sf -> Pair(pf, sf) },
        combine(_selectedPruningSubTab, _selectedRootstockSubTab, _selectedGenevaSubOption) { p, r, g ->
            Triple(p, r, g)
        }
    ) { records, query, selectedSvc, (paymentFilter, searchFilterMode), (pruningTab, rootstockTab, genevaOpt) ->
        val trimmedQuery = query.trim()
        records.filter { rec ->
            val isManualBooking = rec.serialNumber !in automaticSerials
            val matchesCategory = if (trimmedQuery.isNotBlank()) true else isSameCategory(rec.serviceType, selectedSvc)

            val matchesSubTab = if (trimmedQuery.isNotBlank()) true else when {
                selectedSvc.equals("Pruning", ignoreCase = true) -> {
                    rec.rootstock.contains(pruningTab, ignoreCase = true) ||
                    rec.plantVariety.contains(pruningTab, ignoreCase = true) ||
                    rec.notes.contains(pruningTab, ignoreCase = true) ||
                    rec.serviceType.contains(pruningTab, ignoreCase = true) ||
                    (rec.rootstock.isBlank() && rec.plantVariety.isBlank())
                }
                selectedSvc.equals("Rootstocks", ignoreCase = true) -> {
                    val rootstockTarget = if (rootstockTab.startsWith("Geneva")) {
                        genevaOpt ?: "Geneva"
                    } else {
                        rootstockTab
                    }
                    rec.rootstock.contains(rootstockTarget, ignoreCase = true) ||
                    rec.plantVariety.contains(rootstockTarget, ignoreCase = true) ||
                    rec.notes.contains(rootstockTarget, ignoreCase = true) ||
                    rec.serviceType.contains(rootstockTarget, ignoreCase = true) ||
                    (rec.rootstock.isBlank() && rec.plantVariety.isBlank())
                }
                else -> true
            }

            val matchesQuery = if (trimmedQuery.isBlank()) {
                true
            } else {
                when (searchFilterMode) {
                    "Name" -> rec.farmerName.contains(trimmedQuery, ignoreCase = true)
                    "Serial No" -> rec.serialNumber.contains(trimmedQuery, ignoreCase = true)
                    "Phone" -> rec.contactNumber.contains(trimmedQuery, ignoreCase = true)
                    "Quantity" -> rec.quantity.toString() == trimmedQuery || (trimmedQuery.toIntOrNull() != null && rec.quantity == trimmedQuery.toInt())
                    else -> rec.farmerName.contains(trimmedQuery, ignoreCase = true) ||
                            rec.farmerAddress.contains(trimmedQuery, ignoreCase = true) ||
                            rec.contactNumber.contains(trimmedQuery, ignoreCase = true) ||
                            rec.plantVariety.contains(trimmedQuery, ignoreCase = true) ||
                            rec.serviceType.contains(trimmedQuery, ignoreCase = true) ||
                            rec.rootstock.contains(trimmedQuery, ignoreCase = true) ||
                            rec.serialNumber.contains(trimmedQuery, ignoreCase = true) ||
                            rec.quantity.toString() == trimmedQuery ||
                            (trimmedQuery.toIntOrNull() != null && rec.quantity == trimmedQuery.toInt())
                }
            }

            val matchesPayment = when (paymentFilter) {
                "Payments Cleared" -> rec.isPaymentCleared()
                "Payments Pending" -> !rec.isPaymentCleared()
                else -> true
            }

            isManualBooking && matchesCategory && matchesSubTab && matchesQuery && matchesPayment
        }.sortedWith(compareByDescending<CropRecord> { it.timestamp }.thenByDescending { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search Flow across all services & sub-categories (Farmer Name, Serial No, Contact No)
    val globalSearchResults: StateFlow<List<CropRecord>> = combine(
        repository.allRecords,
        _searchQuery
    ) { records, query ->
        val trimmed = query.trim()
        val manualRecords = records.filter { it.serialNumber !in automaticSerials }
        if (trimmed.isBlank()) {
            manualRecords.sortedWith(compareByDescending<CropRecord> { it.timestamp }.thenByDescending { it.id })
        } else {
            val cleanQuery = trimmed.replace(" ", "").lowercase()
            manualRecords.filter { rec ->
                val farmerNameMatch = rec.farmerName.contains(trimmed, ignoreCase = true)
                val serialMatch = rec.serialNumber.replace(" ", "").lowercase().contains(cleanQuery) || rec.serialNumber.contains(trimmed, ignoreCase = true)
                val phoneMatch = rec.contactNumber.replace(" ", "").replace("-", "").contains(cleanQuery) || rec.contactNumber.contains(trimmed, ignoreCase = true)
                val varietyMatch = rec.plantVariety.contains(trimmed, ignoreCase = true)
                val serviceMatch = rec.serviceType.contains(trimmed, ignoreCase = true)
                val rootstockMatch = rec.rootstock.contains(trimmed, ignoreCase = true)
                val addressMatch = rec.farmerAddress.contains(trimmed, ignoreCase = true)

                farmerNameMatch || serialMatch || phoneMatch || varietyMatch || serviceMatch || rootstockMatch || addressMatch
            }.sortedWith(compareByDescending<CropRecord> { it.timestamp }.thenByDescending { it.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun selectServiceCategory(service: String) {
        saveCurrentTabSerialState()
        _selectedService.value = service
        serviceType.value = service
        _recordsFilterService.value = service
        if (service.equals("Pruning", ignoreCase = true)) {
            rootstock.value = _selectedPruningSubTab.value
        } else if (service.equals("Rootstocks", ignoreCase = true)) {
            val rsTab = _selectedRootstockSubTab.value
            val opt = _selectedGenevaSubOption.value ?: "G41"
            rootstock.value = if (rsTab.startsWith("Geneva")) "Geneva ($opt)" else rsTab
        } else {
            rootstock.value = ""
        }
        loadTabSerialState()
        recalculatePaymentStatus()
    }

    fun selectPruningSubTab(subTab: String) {
        saveCurrentTabSerialState()
        _selectedPruningSubTab.value = subTab
        if (_selectedService.value.equals("Pruning", ignoreCase = true)) {
            rootstock.value = subTab
        }
        loadTabSerialState()
    }

    fun selectRootstockSubTab(subTab: String, genevaSubOption: String? = null) {
        saveCurrentTabSerialState()
        _selectedRootstockSubTab.value = subTab
        if (genevaSubOption != null) {
            _selectedGenevaSubOption.value = genevaSubOption
        }
        if (_selectedService.value.equals("Rootstocks", ignoreCase = true)) {
            val rootstockName = if (subTab.startsWith("Geneva")) {
                val opt = genevaSubOption ?: _selectedGenevaSubOption.value ?: "G41"
                "Geneva ($opt)"
            } else {
                subTab
            }
            rootstock.value = rootstockName
        }
        loadTabSerialState()
    }

    fun setViewMode(mode: Int) {
        _viewMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _isGlobalSearchActive.value = true
        }
    }

    fun setPaymentFilter(filter: String) {
        _selectedPaymentFilter.value = filter
    }


    fun setRecordsFilter(service: String?) {
        _recordsFilterService.value = service
    }

    fun lockSerialNumber() {
        if (serialNumber.value.isBlank()) {
            _userMessage.value = "Please type a serial number first"
        } else {
            isSerialLocked.value = true
            tabSerialLocks[getCurrentTabKey()] = true
            _userMessage.value = "Serial number saved and locked"
        }
    }

    fun unlockSerialNumber() {
        isSerialLocked.value = false
        tabSerialLocks[getCurrentTabKey()] = false
    }

    fun updateAmountPaid(amount: String) {
        amountPaid.value = amount
        recalculatePaymentStatus()
    }

    fun updateQuantityOrPrice() {
        val isImportedRootstocks = serviceType.value.equals("Rootstocks", ignoreCase = true)
        if (isImportedRootstocks) {
            val qtyNum = quantity.value.toDoubleOrNull() ?: quantity.value.toIntOrNull()?.toDouble() ?: 0.0
            val perUnitGraftNum = perUnitGraftingCharge.value.toDoubleOrNull() ?: 0.0
            if (perUnitGraftingCharge.value.isNotBlank() && qtyNum > 0) {
                val totalGraft = qtyNum * perUnitGraftNum
                graftingCharges.value = if (totalGraft % 1.0 == 0.0) totalGraft.toLong().toString() else String.format(Locale.US, "%.2f", totalGraft)
            } else if (perUnitGraftingCharge.value.isBlank()) {
                graftingCharges.value = ""
            }
        }
        recalculatePaymentStatus()
    }

    fun recalculatePaymentStatus() {
        val qtyNum = quantity.value.toDoubleOrNull() ?: quantity.value.toIntOrNull()?.toDouble() ?: 0.0
        val priceNum = landAreaAcres.value.toDoubleOrNull() ?: 0.0
        val isImportedRootstocks = serviceType.value.equals("Rootstocks", ignoreCase = true)
        val graftingChargesNum = if (isImportedRootstocks && graftingCharges.value.isNotBlank()) (graftingCharges.value.toDoubleOrNull() ?: 0.0) else 0.0
        val totalAmount = (qtyNum * priceNum) + graftingChargesNum
        val paidNum = amountPaid.value.toDoubleOrNull() ?: 0.0

        paymentStatus.value = when {
            paidNum <= 0.0 -> "Pending"
            totalAmount > 0.0 && paidNum >= totalAmount -> "Fully Paid"
            totalAmount <= 0.0 && paidNum > 0.0 -> "Fully Paid"
            else -> "Advance Paid"
        }
    }

    private val editingPaymentHistoryJson = MutableStateFlow("")
    private var editingOldRecord: CropRecord? = null

    fun resetForm() {
        editingRecordId.value = null
        editingPaymentHistoryJson.value = ""
        editingOldRecord = null
        val currentSvc = _selectedService.value
        serialNumber.value = ""
        isSerialLocked.value = false
        val currentKey = getCurrentTabKey()
        tabSerialNumbers[currentKey] = ""
        tabSerialLocks[currentKey] = false
        farmerName.value = ""
        farmerAddress.value = ""
        contactNumber.value = "+91 "
        serviceType.value = currentSvc
        plantVariety.value = ""
        rootstock.value = ""
        importCountry.value = "Italy"
        quantity.value = "100"
        landAreaAcres.value = "250"
        soilType.value = "Clay Loam"
        healthStage.value = "1 Year"
        location.value = ""
        visitDate.value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        soilHealthObservations.value = ""
        plantHealthObservations.value = ""
        graftType.value = "Bench Grafting"
        scionVariety.value = ""
        perUnitGraftingCharge.value = ""
        graftingCharges.value = ""
        notes.value = ""
        amountPaid.value = "0"
        bookingDate.value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        expectedDelivery.value = getDefaultExpectedDeliveryDate()
        paymentProofUri.value = ""
        paymentProofName.value = ""
        recalculatePaymentStatus()
    }

    fun loadRecordForEditing(record: CropRecord) {
        editingRecordId.value = record.id
        editingOldRecord = record
        editingPaymentHistoryJson.value = record.paymentHistoryJson
        serialNumber.value = record.serialNumber
        isSerialLocked.value = true
        val currentKey = getCurrentTabKey()
        tabSerialNumbers[currentKey] = record.serialNumber
        tabSerialLocks[currentKey] = true
        farmerName.value = record.farmerName
        farmerAddress.value = record.farmerAddress
        val rawPhone = record.contactNumber
        contactNumber.value = if (rawPhone.isBlank()) {
            "+91 "
        } else if (!rawPhone.startsWith("+91")) {
            var digits = rawPhone.replace("[^0-9]".toRegex(), "")
            if (digits.startsWith("91") && digits.length > 10) {
                digits = digits.substring(digits.length - 10)
            }
            if (digits.length > 10) {
                digits = digits.takeLast(10)
            }
            if (digits.isNotEmpty()) "+91 $digits" else "+91 "
        } else {
            rawPhone
        }
        serviceType.value = record.serviceType
        plantVariety.value = record.plantVariety
        rootstock.value = record.rootstock
        quantity.value = record.quantity.toString()
        landAreaAcres.value = record.landAreaAcres.toString()
        soilType.value = record.soilType
        healthStage.value = record.healthStage
        location.value = record.location
        notes.value = record.notes
        amountPaid.value = if (record.amountPaid % 1.0 == 0.0) record.amountPaid.toLong().toString() else record.amountPaid.toString()
        bookingDate.value = record.bookingDate.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
        expectedDelivery.value = record.expectedDelivery.ifBlank { getDefaultExpectedDeliveryDate() }
        paymentProofUri.value = record.paymentProofUri
        paymentProofName.value = if (record.paymentProofUri.isNotBlank()) "UPI_Proof_Attached" else ""
        if (record.serviceType.equals("Site Visit", ignoreCase = true)) {
            visitDate.value = record.bookingDate.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
            val notesText = record.notes
            val soilMatch = Regex("Soil Health:\\s*([^|\\]\n]+)").find(notesText)
            soilHealthObservations.value = soilMatch?.groupValues?.get(1)?.trim() ?: ""
            val plantMatch = Regex("Plant Health:\\s*([^|\\]\n]+)").find(notesText)
            plantHealthObservations.value = plantMatch?.groupValues?.get(1)?.trim() ?: ""
        } else {
            visitDate.value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            soilHealthObservations.value = ""
            plantHealthObservations.value = ""
        }
        if (record.serviceType.equals("Rootstocks", ignoreCase = true)) {
            val graftMatch = Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(record.notes)
            val totalGraft = graftMatch?.groupValues?.get(1) ?: ""
            graftingCharges.value = totalGraft
            val totalGraftNum = totalGraft.toDoubleOrNull() ?: 0.0
            val qtyNum = record.quantity.toDouble()
            if (qtyNum > 0 && totalGraftNum > 0) {
                val perUnit = totalGraftNum / qtyNum
                perUnitGraftingCharge.value = if (perUnit % 1.0 == 0.0) perUnit.toLong().toString() else String.format(Locale.US, "%.2f", perUnit)
            } else {
                perUnitGraftingCharge.value = ""
            }
        } else {
            perUnitGraftingCharge.value = ""
        }
        recalculatePaymentStatus()
        _viewMode.value = 0 // Switch to form
    }

    private var onBookingSavedListener: ((farmerName: String, serviceType: String, serialNo: String, expectedDelivery: String) -> Unit)? = null

    fun setOnBookingSavedListener(listener: (farmerName: String, serviceType: String, serialNo: String, expectedDelivery: String) -> Unit) {
        onBookingSavedListener = listener
    }

    fun saveRecord() {
        if (serialNumber.value.isBlank()) {
            _userMessage.value = "Please enter and save a Serial Number"
            return
        }
        if (farmerName.value.isBlank()) {
            _userMessage.value = "Please enter Farmer Name"
            return
        }
        if (farmerAddress.value.isBlank()) {
            _userMessage.value = "Please enter Farmer Address"
            return
        }
        if (contactNumber.value.isBlank() || contactNumber.value.length < 7) {
            _userMessage.value = "Please enter a valid Contact Number"
            return
        }

        viewModelScope.launch {
            val isSiteVisit = serviceType.value.equals("Site Visit", ignoreCase = true)
            val formattedNotes = if (isSiteVisit) {
                val siteVisitDetails = buildString {
                    if (visitDate.value.isNotBlank()) append("Visit Date: ${visitDate.value}")
                    if (soilHealthObservations.value.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Soil Health: ${soilHealthObservations.value}")
                    }
                    if (plantHealthObservations.value.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Plant Health: ${plantHealthObservations.value}")
                    }
                }
                if (siteVisitDetails.isNotBlank() && !notes.value.contains("Visit Date") && !notes.value.contains("Soil Health")) {
                    if (notes.value.isBlank()) siteVisitDetails else "${notes.value.trim()}\n[$siteVisitDetails]"
                } else {
                    notes.value.trim()
                }
            } else if (serviceType.value.equals("Rootstocks", ignoreCase = true)) {
                val graftDetails = buildString {
                    if (scionVariety.value.isNotBlank()) append("Scion: ${scionVariety.value}")
                    if (graftType.value.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Graft Type: ${graftType.value}")
                    }
                    if (graftingCharges.value.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Grafting Charges: ₹${graftingCharges.value}")
                    }
                }
                if (graftDetails.isNotBlank() && !notes.value.contains("Graft Type") && !notes.value.contains("Scion:")) {
                    if (notes.value.isBlank()) graftDetails else "${notes.value.trim()}\n[$graftDetails]"
                } else {
                    notes.value.trim()
                }
            } else {
                notes.value.trim()
            }

            val isPruning = serviceType.value.equals("Pruning", ignoreCase = true)
            val finalPlantVariety = when {
                serviceType.value.equals("Rootstocks", ignoreCase = true) && scionVariety.value.isNotBlank() -> scionVariety.value.trim()
                isSiteVisit -> "Site Visit"
                isPruning -> _selectedPruningSubTab.value
                else -> plantVariety.value.ifBlank { "Standard Variety" }.trim()
            }

            val finalBookingDate = if (isSiteVisit && visitDate.value.isNotBlank()) {
                visitDate.value
            } else {
                bookingDate.value
            }

            val record = CropRecord(
                id = editingRecordId.value ?: 0,
                serialNumber = serialNumber.value.ifBlank { generateDefaultSerial(serviceType.value) },
                farmerName = farmerName.value.trim(),
                farmerAddress = farmerAddress.value.trim(),
                contactNumber = contactNumber.value.trim(),
                serviceType = serviceType.value,
                plantVariety = finalPlantVariety,
                rootstock = if (isSiteVisit || isPruning) "" else rootstock.value,
                quantity = quantity.value.toIntOrNull() ?: 1,
                landAreaAcres = landAreaAcres.value.toDoubleOrNull() ?: 1.0,
                soilType = soilType.value,
                healthStage = healthStage.value,
                location = location.value.trim(),
                notes = formattedNotes,
                amountPaid = amountPaid.value.toDoubleOrNull() ?: 0.0,
                paymentStatus = paymentStatus.value,
                bookingDate = finalBookingDate,
                expectedDelivery = expectedDelivery.value,
                paymentProofUri = paymentProofUri.value,
                paymentHistoryJson = editingPaymentHistoryJson.value,
                timestamp = System.currentTimeMillis()
            )

            if (editingRecordId.value == null) {
                repository.insert(record)
                _userMessage.value = "New record saved successfully!"
                onBookingSavedListener?.invoke(record.farmerName, record.serviceType, record.serialNumber, record.expectedDelivery)
            } else {
                repository.update(record, oldRecord = editingOldRecord)
                _userMessage.value = "Record updated successfully!"
                onBookingSavedListener?.invoke(record.farmerName, record.serviceType, record.serialNumber, record.expectedDelivery)
            }

            resetForm()
            _viewMode.value = 1 // Switch to records view
        }
    }

    fun deleteRecord(record: CropRecord) {
        viewModelScope.launch {
            repository.delete(record)
            _userMessage.value = "Record deleted"
        }
    }

    fun updateRecord(record: CropRecord) {
        viewModelScope.launch {
            repository.update(record)
            _userMessage.value = "Record updated"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun generateNextSerialNumber(currentSerial: String, service: String, existingRecords: List<CropRecord>): String {
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
            prefix = when (service) {
                "Local Plants" -> "LP-"
                "Imported" -> "IMP-"
                "Rootstocks" -> "RS-"
                "Site Visit" -> "SV-"
                "Pruning" -> "PR-"
                else -> "AGRI-"
            }
        }

        val targetPrefix = prefix
        var maxVal = 0L
        var maxDigitLen = currentDigitsStr?.length ?: 2

        currentDigitsStr?.toLongOrNull()?.let { inputVal ->
            if (inputVal > maxVal) {
                maxVal = inputVal
            }
        }

        val cleanPrefix = targetPrefix.trim()

        existingRecords.forEach { record ->
            val recSerial = record.serialNumber.trim()
            if (recSerial.isBlank()) return@forEach

            val recRegex = Regex("""^(${Regex.escape(cleanPrefix)})(\d+)$""", RegexOption.IGNORE_CASE)
            val recMatch = recRegex.find(recSerial)
            if (recMatch != null) {
                val digitsPart = recMatch.groupValues[2]
                val numVal = digitsPart.toLongOrNull()
                if (numVal != null) {
                    if (numVal > maxVal) {
                        maxVal = numVal
                    }
                    if (digitsPart.length > maxDigitLen) {
                        maxDigitLen = digitsPart.length
                    }
                }
            } else if (recSerial.startsWith(cleanPrefix, ignoreCase = true)) {
                val remaining = recSerial.substring(cleanPrefix.length).trim()
                val digitsOnly = remaining.takeWhile { it.isDigit() }
                if (digitsOnly.isNotEmpty()) {
                    val numVal = digitsOnly.toLongOrNull()
                    if (numVal != null) {
                        if (numVal > maxVal) {
                            maxVal = numVal
                        }
                        if (digitsOnly.length > maxDigitLen) {
                            maxDigitLen = digitsOnly.length
                        }
                    }
                }
            }
        }

        val nextVal = maxVal + 1
        val formattedDigits = "%0${maxDigitLen}d".format(nextVal)
        return "$targetPrefix$formattedDigits"
    }

    fun generateNewSerialNumber() {
        val newSerial = generateNextSerialNumber(serialNumber.value, serviceType.value, allRecords.value)
        serialNumber.value = newSerial
        tabSerialNumbers[getCurrentTabKey()] = newSerial
        isSerialLocked.value = false
        tabSerialLocks[getCurrentTabKey()] = false
        _userMessage.value = "New serial number generated: $newSerial"
    }

    private fun generateDefaultSerial(service: String): String {
        return generateNextSerialNumber(serialNumber.value, service, allRecords.value)
    }

    fun exportRecordsAsText(): String {
        val list = filteredRecords.value
        if (list.isEmpty()) return "No crop records found."

        val sb = StringBuilder()
        sb.append("AGRI CROP MANAGEMENT REPORT\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sb.append("Generated on: ").append(dateFormat.format(Date())).append("\n")
        sb.append("Total Records: ").append(list.size).append("\n\n")

        list.forEachIndexed { idx, item ->
            sb.append("${idx + 1}. [${item.serialNumber}] ${item.farmerName}\n")
            sb.append("   Service: ${item.serviceType}\n")
            sb.append("   Contact: ${item.contactNumber}\n")
            sb.append("   Address: ${item.farmerAddress}\n")
            sb.append("   Variety: ${item.plantVariety} | Rootstock: ${item.rootstock}\n")
            sb.append("   Quantity: ${item.quantity} plants (${item.landAreaAcres} Acres)\n")
            sb.append("   Stage: ${item.healthStage} | Soil: ${item.soilType}\n")
            if (item.notes.isNotBlank()) sb.append("   Notes: ${item.notes}\n")
            sb.append("--------------------------------------------------\n")
        }
        return sb.toString()
    }
}

class CropViewModelFactory(private val repository: CropRecordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CropViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CropViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
