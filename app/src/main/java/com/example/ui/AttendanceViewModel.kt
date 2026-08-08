package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdvancePayment
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.Worker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    // Active workers list
    val activeWorkers: StateFlow<List<Worker>> = repository.activeWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's date string
    fun getTodayString(): String = dateFormat.format(Date())

    // Selected Date for Daily Marking (Default: Today)
    private val _selectedDailyDate = MutableStateFlow(getTodayString())
    val selectedDailyDate: StateFlow<String> = _selectedDailyDate.asStateFlow()

    // Selected Month-Year for Home/Calendar View (Default: Current YYYY-MM)
    private val _selectedMonthYear = MutableStateFlow(monthFormat.format(Date()))
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    // Selected Worker for Detail/Calendar View
    private val _selectedWorker = MutableStateFlow<Worker?>(null)
    val selectedWorker: StateFlow<Worker?> = _selectedWorker.asStateFlow()

    // User feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Attendance records for the selected daily date
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForSelectedDate: StateFlow<List<AttendanceRecord>> = _selectedDailyDate
        .flatMapLatest { date -> repository.getAttendanceForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance records for the selected month (start date: YYYY-MM-01, end date: YYYY-MM-31)
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForSelectedMonth: StateFlow<List<AttendanceRecord>> = _selectedMonthYear
        .flatMapLatest { yearMonth ->
            val startDate = "$yearMonth-01"
            val endDate = "$yearMonth-31"
            repository.getAttendanceInRange(startDate, endDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Individual advance payments for the selected worker
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedWorkerAdvancePayments: StateFlow<List<AdvancePayment>> = _selectedWorker
        .flatMapLatest { worker ->
            if (worker != null) {
                repository.getAdvancePaymentsForWorker(worker.workerId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setSelectedDailyDate(dateStr: String) {
        val today = getTodayString()
        if (dateStr > today) {
            _userMessage.value = "Cannot mark attendance for future dates"
            return
        }
        _selectedDailyDate.value = dateStr
    }

    fun setSelectedMonthYear(monthYearStr: String) {
        _selectedMonthYear.value = monthYearStr
    }

    fun setSelectedWorker(worker: Worker?) {
        _selectedWorker.value = worker
    }

    fun addWorker(name: String, phoneNumber: String, dailyRate: Double = 0.0, advancePaid: Double = 0.0) {
        if (name.isBlank()) {
            _userMessage.value = "Worker name cannot be empty"
            return
        }
        viewModelScope.launch {
            repository.insertWorker(
                Worker(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    dailyRate = dailyRate,
                    advancePaid = advancePaid,
                    isActive = true
                )
            )
            _userMessage.value = "Worker '${name.trim()}' added successfully"
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch {
            repository.updateWorker(worker)
            if (_selectedWorker.value?.workerId == worker.workerId) {
                _selectedWorker.value = worker
            }
            _userMessage.value = "Worker updated"
        }
    }

    fun updateWorkerAdvance(worker: Worker, totalAdvance: Double) {
        viewModelScope.launch {
            val updated = worker.copy(advancePaid = totalAdvance)
            repository.updateWorker(updated)
            if (_selectedWorker.value?.workerId == worker.workerId) {
                _selectedWorker.value = updated
            }
            _userMessage.value = "Advance payment updated for ${worker.name}"
        }
    }

    fun recordAdvancePayment(worker: Worker, amount: Double, date: String, note: String = "") {
        if (amount <= 0) {
            _userMessage.value = "Please enter a valid advance amount"
            return
        }
        viewModelScope.launch {
            repository.insertAdvancePayment(
                AdvancePayment(
                    workerId = worker.workerId,
                    amount = amount,
                    date = date.ifBlank { getTodayString() },
                    note = note
                )
            )
            val updatedTotal = worker.advancePaid + amount
            val updatedWorker = worker.copy(advancePaid = updatedTotal)
            repository.updateWorker(updatedWorker)
            if (_selectedWorker.value?.workerId == worker.workerId) {
                _selectedWorker.value = updatedWorker
            }
            _userMessage.value = "Advance of ₹${amount.toInt()} recorded for ${worker.name} ($date)"
        }
    }

    fun deleteAdvancePayment(payment: AdvancePayment) {
        val worker = _selectedWorker.value
        if (worker != null) {
            deleteAdvancePayment(worker, payment)
        } else {
            viewModelScope.launch {
                repository.deleteAdvancePayment(payment.paymentId)
            }
        }
    }

    fun deleteAdvancePayment(worker: Worker, payment: AdvancePayment) {
        viewModelScope.launch {
            repository.deleteAdvancePayment(payment.paymentId)
            val newTotal = (worker.advancePaid - payment.amount).coerceAtLeast(0.0)
            val updatedWorker = worker.copy(advancePaid = newTotal)
            repository.updateWorker(updatedWorker)
            if (_selectedWorker.value?.workerId == worker.workerId) {
                _selectedWorker.value = updatedWorker
            }
            _userMessage.value = "Advance record removed"
        }
    }

    fun updateWorkerRate(worker: Worker, newRate: Double) {
        viewModelScope.launch {
            val updated = worker.copy(dailyRate = newRate)
            repository.updateWorker(updated)
            if (_selectedWorker.value?.workerId == worker.workerId) {
                _selectedWorker.value = updated
            }
            _userMessage.value = "Daily rate updated for ${worker.name}"
        }
    }

    fun deactivateWorker(workerId: Long, workerName: String) {
        viewModelScope.launch {
            repository.deactivateWorker(workerId)
            if (_selectedWorker.value?.workerId == workerId) {
                _selectedWorker.value = null
            }
            _userMessage.value = "Worker '$workerName' deactivated"
        }
    }

    fun setAttendanceStatus(workerId: Long, date: String, status: AttendanceStatus) {
        if (date > getTodayString()) {
            _userMessage.value = "Cannot mark attendance for future dates"
            return
        }
        viewModelScope.launch {
            repository.insertOrUpdateAttendance(
                AttendanceRecord(
                    workerId = workerId,
                    date = date,
                    status = status,
                    markedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleAttendance(workerId: Long, date: String, currentStatus: AttendanceStatus?) {
        val nextStatus = when (currentStatus) {
            AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
            AttendanceStatus.ABSENT -> AttendanceStatus.PRESENT
            null -> AttendanceStatus.PRESENT
        }
        setAttendanceStatus(workerId, date, nextStatus)
    }

    fun markAllPresent(date: String, workers: List<Worker>) {
        if (date > getTodayString()) {
            _userMessage.value = "Cannot mark attendance for future dates"
            return
        }
        if (workers.isEmpty()) return
        viewModelScope.launch {
            val records = workers.map { worker ->
                AttendanceRecord(
                    workerId = worker.workerId,
                    date = date,
                    status = AttendanceStatus.PRESENT,
                    markedAt = System.currentTimeMillis()
                )
            }
            repository.insertOrUpdateAttendanceList(records)
            _userMessage.value = "All active workers marked Present for $date"
        }
    }

    fun deleteAttendanceRecord(workerId: Long, date: String) {
        viewModelScope.launch {
            repository.deleteAttendanceForWorkerAndDate(workerId, date)
        }
    }
}

class AttendanceViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
