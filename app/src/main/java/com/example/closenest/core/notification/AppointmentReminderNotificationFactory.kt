package com.example.closenest.core.notification

import android.content.Context
import com.example.closenest.R
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationType

object AppointmentReminderNotificationFactory {

    fun create(
        context: Context,
        appointmentId: String,
        appointmentName: String,
        title: String,
        body: String,
        appointmentTimeMillis: Long,
        kind: AppointmentReminderKind,
        scheduledAtMillis: Long,
        createdAtMillis: Long = System.currentTimeMillis()
    ): NotificationItem {
        val dedupeKey = appointmentReminderDedupeKey(appointmentId, kind)
        return NotificationItem(
            id = dedupeKey,
            type = NotificationType.APPOINTMENT_REMINDER,
            status = NotificationStatus.ACTIVE,
            relationshipId = null,
            relationshipName = appointmentName,
            relationshipAvatarUrl = null,
            title = title,
            description = body,
            createdAtMillis = createdAtMillis,
            expiresAtMillis = appointmentTimeMillis,
            actionLabel = context.getString(R.string.notification_action_view_appointment),
            actionType = NotificationActionType.VIEW_APPOINTMENT,
            dedupeKey = dedupeKey,
            scheduledAtMillis = scheduledAtMillis,
            completedAtMillis = null,
            sourceEntityId = appointmentId,
            sourceEntityType = AppointmentSourceEntityType
        )
    }
}

