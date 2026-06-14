package com.example.closenest.core.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.closenest.core.worker.AppointmentReminderWorker
import com.example.closenest.features.homepage.model.AppointmentItem
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

object AppointmentReminderScheduler {
    private const val DayBeforeOffsetMillis = 24L * 60L * 60L * 1_000L
    private const val UpcomingOffsetMillis = 2L * 60L * 60L * 1_000L

    fun scheduleAppointmentReminders(
        context: Context,
        appointment: AppointmentItem,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        cancelAppointmentReminders(appContext, appointment.id)

        if (appointment.appointmentDateMillis <= nowMillis) return

        val dayBeforeAtMillis = appointment.appointmentDateMillis - DayBeforeOffsetMillis
        val upcomingAtMillis = appointment.appointmentDateMillis - UpcomingOffsetMillis
        val isAllDay = appointment.appointmentDateMillis.isStartOfDay()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        when {
            !isAllDay && upcomingAtMillis <= nowMillis -> {
                enqueueReminder(
                    context = appContext,
                    appointment = appointment,
                    kind = AppointmentReminderKind.UPCOMING,
                    triggerAtMillis = nowMillis,
                    nowMillis = nowMillis,
                    userId = userId
                )
            }
            dayBeforeAtMillis <= nowMillis -> {
                enqueueReminder(
                    context = appContext,
                    appointment = appointment,
                    kind = AppointmentReminderKind.DAY_BEFORE,
                    triggerAtMillis = nowMillis,
                    nowMillis = nowMillis,
                    userId = userId
                )
                if (!isAllDay) {
                    enqueueReminder(
                        context = appContext,
                        appointment = appointment,
                        kind = AppointmentReminderKind.UPCOMING,
                        triggerAtMillis = upcomingAtMillis,
                        nowMillis = nowMillis,
                        userId = userId
                    )
                }
            }
            else -> {
                enqueueReminder(
                    context = appContext,
                    appointment = appointment,
                    kind = AppointmentReminderKind.DAY_BEFORE,
                    triggerAtMillis = dayBeforeAtMillis,
                    nowMillis = nowMillis,
                    userId = userId
                )
                if (!isAllDay) {
                    enqueueReminder(
                        context = appContext,
                        appointment = appointment,
                        kind = AppointmentReminderKind.UPCOMING,
                        triggerAtMillis = upcomingAtMillis,
                        nowMillis = nowMillis,
                        userId = userId
                    )
                }
            }
        }
    }

    fun cancelAppointmentReminders(context: Context, appointmentId: String) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(uniqueWorkName(appointmentId, AppointmentReminderKind.DAY_BEFORE))
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(uniqueWorkName(appointmentId, AppointmentReminderKind.UPCOMING))
    }

    private fun enqueueReminder(
        context: Context,
        appointment: AppointmentItem,
        kind: AppointmentReminderKind,
        triggerAtMillis: Long,
        nowMillis: Long,
        userId: String?
    ) {
        val delayMillis = max(0L, triggerAtMillis - nowMillis)
        val workRequest = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInputData(
                workDataOf(
                    AppointmentReminderWorker.InputAppointmentId to appointment.id,
                    AppointmentReminderWorker.InputName to appointment.name,
                    AppointmentReminderWorker.InputLocation to appointment.location,
                    AppointmentReminderWorker.InputUserId to userId.orEmpty(),
                    AppointmentReminderWorker.InputAppointmentTimeMillis to appointment.appointmentDateMillis,
                    AppointmentReminderWorker.InputScheduledAtMillis to triggerAtMillis,
                    AppointmentReminderWorker.InputReminderKind to kind.name
                )
            )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueWorkName(appointment.id, kind),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun uniqueWorkName(appointmentId: String, kind: AppointmentReminderKind): String {
        return "appointment_reminder_${appointmentId}_${kind.name.lowercase()}"
    }

    private fun Long.isStartOfDay(): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = this@isStartOfDay }
        return calendar.get(Calendar.HOUR_OF_DAY) == 0 &&
            calendar.get(Calendar.MINUTE) == 0
    }
}
