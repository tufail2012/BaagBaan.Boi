package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppNotification
import com.example.data.NotificationRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = repository.allNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = repository.unreadCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }

    fun sendBookingConfirmation(
        context: Context,
        farmerName: String,
        serviceType: String,
        serialNo: String
    ) {
        NotificationHelper.postBookingConfirmation(
            context = context.applicationContext,
            repository = repository,
            scope = viewModelScope,
            farmerName = farmerName,
            serviceType = serviceType,
            serialNo = serialNo
        )
    }

    fun sendReminder(
        context: Context,
        title: String,
        message: String,
        triggerAtMillis: Long = System.currentTimeMillis()
    ) {
        NotificationHelper.postReminderNotification(
            context = context.applicationContext,
            repository = repository,
            scope = viewModelScope,
            title = title,
            message = message,
            triggerAtMillis = triggerAtMillis
        )
    }
}

class NotificationViewModelFactory(
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
