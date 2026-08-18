package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserAttendance
import com.example.data.UserBooking
import com.example.data.UserFirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserDashboardViewModel(
    private val repository: UserFirestoreRepository = UserFirestoreRepository()
) : ViewModel() {

    private val auth get() = com.example.util.SafeFirebase.auth
    private val currentUidState = MutableStateFlow(auth?.currentUser?.uid)

    private val _rawBookings = MutableStateFlow<List<UserBooking>>(emptyList())
    val rawBookings: StateFlow<List<UserBooking>> = _rawBookings.asStateFlow()

    private val _rawAttendance = MutableStateFlow<List<UserAttendance>>(emptyList())
    val rawAttendance: StateFlow<List<UserAttendance>> = _rawAttendance.asStateFlow()

    private val _selectedBookingFilter = MutableStateFlow("All")
    val selectedBookingFilter: StateFlow<String> = _selectedBookingFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _isLoadingBookings = MutableStateFlow(false)
    val isLoadingBookings: StateFlow<Boolean> = _isLoadingBookings.asStateFlow()

    private val _isLoadingAttendance = MutableStateFlow(false)
    val isLoadingAttendance: StateFlow<Boolean> = _isLoadingAttendance.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val filteredBookings: StateFlow<List<UserBooking>> = combine(_rawBookings, _selectedBookingFilter, _searchQuery) { bookings, filter, query ->
        val typeFiltered = if (filter == "All") {
            bookings
        } else {
            bookings.filter { it.type.equals(filter, ignoreCase = true) }
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            typeFiltered
        } else {
            typeFiltered.filter {
                it.farmerName.contains(trimmed, ignoreCase = true) ||
                it.itemName.contains(trimmed, ignoreCase = true) ||
                it.variety.contains(trimmed, ignoreCase = true) ||
                it.type.contains(trimmed, ignoreCase = true) ||
                it.season.contains(trimmed, ignoreCase = true) ||
                it.notes.contains(trimmed, ignoreCase = true) ||
                it.bookingDate.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        listenToAuthAndLoadData()
    }

    fun refreshUser() {
        val uid = auth?.currentUser?.uid
        currentUidState.value = uid
        listenToAuthAndLoadData()
    }

    private fun listenToAuthAndLoadData() {
        val uid = auth?.currentUser?.uid ?: return
        
        _isLoadingBookings.value = true
        _isLoadingAttendance.value = true

        viewModelScope.launch {
            repository.getBookingsFlow(uid).collect { list ->
                _rawBookings.value = list
                _isLoadingBookings.value = false
            }
        }

        viewModelScope.launch {
            repository.getAttendanceFlow(uid).collect { list ->
                _rawAttendance.value = list
                _isLoadingAttendance.value = false
            }
        }
    }

    fun setBookingFilter(filter: String) {
        _selectedBookingFilter.value = filter
    }

    fun saveBooking(booking: UserBooking, onComplete: (Boolean) -> Unit = {}) {
        val uid = auth?.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            _userMessage.value = "Please sign in to save bookings"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoadingBookings.value = true
            val result = repository.saveBooking(booking, uid)
            _isLoadingBookings.value = false
            if (result.isSuccess) {
                _userMessage.value = "Booking saved successfully to Firestore!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save booking"
                onComplete(false)
            }
        }
    }

    fun deleteBooking(bookingId: String) {
        val uid = auth?.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.deleteBooking(bookingId, uid)
            if (result.isSuccess) {
                _userMessage.value = "Booking deleted"
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete booking"
            }
        }
    }

    fun saveAttendance(attendance: UserAttendance, onComplete: (Boolean) -> Unit = {}) {
        val uid = auth?.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            _userMessage.value = "Please sign in to mark attendance"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoadingAttendance.value = true
            val result = repository.saveAttendance(attendance, uid)
            _isLoadingAttendance.value = false
            if (result.isSuccess) {
                _userMessage.value = "Attendance marked successfully in Firestore!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save attendance"
                onComplete(false)
            }
        }
    }

    fun deleteAttendance(attendanceId: String) {
        val uid = auth?.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.deleteAttendance(attendanceId, uid)
            if (result.isSuccess) {
                _userMessage.value = "Attendance record deleted"
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete record"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
