package com.example.closenest.features.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.AppointmentReminderNotificationFactory
import com.example.closenest.core.notification.AppointmentSourceEntityType
import com.example.closenest.core.notification.appointmentReminderDedupeKey
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppointmentReminderNotificationFactoryTest {

    @Test
    fun `dedupe key is stable per appointment and reminder kind`() {
        assertEquals(
            "appointment:appt-1:day_before",
            appointmentReminderDedupeKey("appt-1", AppointmentReminderKind.DAY_BEFORE)
        )
        assertEquals(
            "appointment:appt-1:upcoming",
            appointmentReminderDedupeKey("appt-1", AppointmentReminderKind.UPCOMING)
        )
    }

    @Test
    fun `factory creates appointment reminder notification payload`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val notification = AppointmentReminderNotificationFactory.create(
            context = context,
            appointmentId = "appt-1",
            appointmentName = "Minh Anh",
            title = "Reminder title",
            body = "Reminder body",
            appointmentTimeMillis = 1_800_000_000_000L,
            kind = AppointmentReminderKind.UPCOMING,
            scheduledAtMillis = 1_799_992_800_000L,
            createdAtMillis = 1_799_990_000_000L
        )

        assertEquals("appointment:appt-1:upcoming", notification.id)
        assertEquals("appointment:appt-1:upcoming", notification.dedupeKey)
        assertEquals(NotificationType.APPOINTMENT_REMINDER, notification.type)
        assertEquals(NotificationStatus.ACTIVE, notification.status)
        assertEquals(NotificationActionType.VIEW_APPOINTMENT, notification.actionType)
        assertEquals("appt-1", notification.sourceEntityId)
        assertEquals(AppointmentSourceEntityType, notification.sourceEntityType)
        assertEquals(1_800_000_000_000L, notification.expiresAtMillis)
        assertEquals(1_799_992_800_000L, notification.scheduledAtMillis)
    }
}
