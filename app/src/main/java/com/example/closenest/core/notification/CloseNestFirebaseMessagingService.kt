package com.example.closenest.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CloseNestFirebaseMessagingService : FirebaseMessagingService() {

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

        CloseNestNotificationHelper.showAppointmentReminder(
            context = applicationContext,
            appointmentId = appointmentId,
            title = title,
            body = body
        )
    }

    private companion object {
        const val AppointmentReminderType = "APPOINTMENT_REMINDER"
        const val DataType = "type"
        const val DataAppointmentId = "appointmentId"
        const val DataTitle = "title"
        const val DataBody = "body"
    }
}

