package com.example.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val allNotifications: Flow<List<AppNotification>> = dao.getAllNotifications()
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    suspend fun insertNotification(notification: AppNotification): Long {
        return dao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        dao.deleteNotification(id)
    }

    suspend fun deleteAllNotifications() {
        dao.deleteAllNotifications()
    }
}
