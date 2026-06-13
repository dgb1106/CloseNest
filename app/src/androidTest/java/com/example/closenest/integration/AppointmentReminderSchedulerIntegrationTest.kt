package com.example.closenest.integration

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.AppointmentReminderScheduler
import com.example.closenest.features.homepage.model.AppointmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AppointmentReminderSchedulerIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun scheduleAppointmentReminders_enqueuesDayBeforeAndUpcomingReminderWork() {
        val nowMillis = System.currentTimeMillis()
        val appointment = sampleAppointment(
            id = "appointment-schedule",
            appointmentDateMillis = nowMillis + TimeUnit.DAYS.toMillis(3)
        )

        AppointmentReminderScheduler.scheduleAppointmentReminders(
            context = context,
            appointment = appointment,
            nowMillis = nowMillis
        )

        val workManager = WorkManager.getInstance(context)
        val dayBeforeWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.DAY_BEFORE))
            .get()
        val upcomingWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.UPCOMING))
            .get()

        assertEquals(1, dayBeforeWork.size)
        assertEquals(1, upcomingWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, dayBeforeWork.single().state)
        assertEquals(WorkInfo.State.ENQUEUED, upcomingWork.single().state)
    }

    @Test
    fun cancelAppointmentReminders_cancelsBothUniqueReminderWorks() {
        val nowMillis = System.currentTimeMillis()
        val appointment = sampleAppointment(
            id = "appointment-cancel",
            appointmentDateMillis = nowMillis + TimeUnit.DAYS.toMillis(3)
        )

        AppointmentReminderScheduler.scheduleAppointmentReminders(
            context = context,
            appointment = appointment,
            nowMillis = nowMillis
        )
        AppointmentReminderScheduler.cancelAppointmentReminders(context, appointment.id)

        val workManager = WorkManager.getInstance(context)
        val dayBeforeWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.DAY_BEFORE))
            .get()
        val upcomingWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.UPCOMING))
            .get()

        assertTrue(dayBeforeWork.all { it.state == WorkInfo.State.CANCELLED })
        assertTrue(upcomingWork.all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun scheduleAppointmentReminders_doesNotSchedulePastAppointment() {
        val nowMillis = System.currentTimeMillis()
        val appointment = sampleAppointment(
            id = "appointment-past",
            appointmentDateMillis = nowMillis - TimeUnit.HOURS.toMillis(1)
        )

        AppointmentReminderScheduler.scheduleAppointmentReminders(
            context = context,
            appointment = appointment,
            nowMillis = nowMillis
        )

        val workManager = WorkManager.getInstance(context)
        val dayBeforeWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.DAY_BEFORE))
            .get()
        val upcomingWork = workManager
            .getWorkInfosForUniqueWork(uniqueWorkName(appointment.id, AppointmentReminderKind.UPCOMING))
            .get()

        assertTrue(dayBeforeWork.isEmpty())
        assertTrue(upcomingWork.isEmpty())
    }

    private fun sampleAppointment(
        id: String,
        appointmentDateMillis: Long
    ) = AppointmentItem(
        id = id,
        name = "Cafe voi An",
        participantContactIds = listOf("rel-1"),
        participantContactNames = listOf("An Nguyen"),
        location = "Thu vien",
        locationLatitude = 10.762622,
        locationLongitude = 106.660172,
        appointmentDateMillis = appointmentDateMillis,
        dateKey = "2026-06-13",
        note = "Trao doi bai tap",
        createdAtMillis = appointmentDateMillis - TimeUnit.DAYS.toMillis(1)
    )

    private fun uniqueWorkName(
        appointmentId: String,
        kind: AppointmentReminderKind
    ): String = "appointment_reminder_${appointmentId}_${kind.name.lowercase()}"
}
