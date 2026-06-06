package com.example.closenest.features.notifications.repository

import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationSummary
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun observeNotifications(): Flow<List<NotificationItem>>

    fun observeNotificationSummary(): Flow<NotificationSummary>

    suspend fun upsertNotification(notification: NotificationItem, userId: String? = null)

    suspend fun markAsRead(notificationId: String)

    suspend fun dismissNotification(notificationId: String)

    suspend fun getNotification(notificationId: String): NotificationItem?

    suspend fun deleteNotification(notificationId: String)

    suspend fun deleteAppointmentReminderNotifications(appointmentId: String)
}
