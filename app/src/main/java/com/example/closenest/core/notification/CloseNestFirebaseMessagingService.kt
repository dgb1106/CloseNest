package com.example.closenest.core.notification

import com.example.closenest.features.notifications.repository.NotificationRepositoryProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CloseNestFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[DataType] != AppointmentReminderType) return

        val appointmentId = data[DataAppointmentId]?.takeIf { it.isNotBlank() } ?: return
        val title = message.notification?.title
            ?: data[DataTitle]?.takeIf { it.isNotBlank() }
            ?: return
        val body = message.notification?.body
            ?: data[DataBody]?.takeIf { it.isNotBlank() }
            ?: return
        val appointmentName = data[DataName]?.takeIf { it.isNotBlank() }
            ?: data[DataAppointmentName]?.takeIf { it.isNotBlank() }
            ?: title
        val appointmentTimeMillis = data[DataAppointmentTimeMillis]?.toLongOrNull()
            ?: System.currentTimeMillis()
        val scheduledAtMillis = data[DataScheduledAtMillis]?.toLongOrNull()
            ?: System.currentTimeMillis()
        val kind = data[DataReminderKind]
            ?.let { runCatching { AppointmentReminderKind.valueOf(it.trim().uppercase()) }.getOrNull() }
            ?: AppointmentReminderKind.UPCOMING

        CloseNestNotificationHelper.showAppointmentReminder(
            context = applicationContext,
            appointmentId = appointmentId,
            title = title,
            body = body
        )
        val notification = AppointmentReminderNotificationFactory.create(
            context = applicationContext,
            appointmentId = appointmentId,
            appointmentName = appointmentName,
            title = title,
            body = body,
            appointmentTimeMillis = appointmentTimeMillis,
            kind = kind,
            scheduledAtMillis = scheduledAtMillis
        )
        serviceScope.launch {
            runCatching {
                NotificationRepositoryProvider.repository.upsertNotification(notification)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val AppointmentReminderType = "APPOINTMENT_REMINDER"
        const val DataType = "type"
        const val DataAppointmentId = "appointmentId"
        const val DataAppointmentName = "appointmentName"
        const val DataAppointmentTimeMillis = "appointmentTimeMillis"
        const val DataTitle = "title"
        const val DataBody = "body"
        const val DataName = "name"
        const val DataReminderKind = "reminderKind"
        const val DataScheduledAtMillis = "scheduledAtMillis"
    }
}
