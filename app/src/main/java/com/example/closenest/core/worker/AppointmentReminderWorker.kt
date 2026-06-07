package com.example.closenest.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.closenest.R
import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.AppointmentReminderNotificationFactory
import com.example.closenest.core.notification.CloseNestNotificationHelper
import com.example.closenest.features.notifications.repository.NotificationRepositoryProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appointmentId = inputData.getString(InputAppointmentId)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        val name = inputData.getString(InputName)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        val location = inputData.getString(InputLocation)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        val userId = inputData.getString(InputUserId)?.takeIf { it.isNotBlank() }
        val appointmentTimeMillis = inputData.getLong(InputAppointmentTimeMillis, 0L)
        val scheduledAtMillis = inputData.getLong(InputScheduledAtMillis, System.currentTimeMillis())
        if (appointmentTimeMillis <= System.currentTimeMillis()) {
            return Result.success()
        }

        val kind = inputData.getString(InputReminderKind)
            ?.let { runCatching { AppointmentReminderKind.valueOf(it) }.getOrNull() }
            ?: return Result.failure()
        val appointmentTime = appointmentTimeFormatter.format(Date(appointmentTimeMillis))

        val title = when (kind) {
            AppointmentReminderKind.DAY_BEFORE ->
                applicationContext.getString(R.string.appointment_reminder_day_before_title, name)
            AppointmentReminderKind.UPCOMING ->
                applicationContext.getString(R.string.appointment_reminder_upcoming_title, name)
        }
        val body = when (kind) {
            AppointmentReminderKind.DAY_BEFORE ->
                applicationContext.getString(
                    R.string.appointment_reminder_day_before_body,
                    location,
                    appointmentTime
                )
            AppointmentReminderKind.UPCOMING ->
                applicationContext.getString(
                    R.string.appointment_reminder_upcoming_body,
                    appointmentTime,
                    location
                )
        }

        CloseNestNotificationHelper.showAppointmentReminder(
            context = applicationContext,
            appointmentId = appointmentId,
            title = title,
            body = body
        )
        val notification = AppointmentReminderNotificationFactory.create(
            context = applicationContext,
            appointmentId = appointmentId,
            appointmentName = name,
            title = title,
            body = body,
            appointmentTimeMillis = appointmentTimeMillis,
            kind = kind,
            scheduledAtMillis = scheduledAtMillis
        )
        runCatching {
            NotificationRepositoryProvider.repository.upsertNotification(
                notification = notification,
                userId = userId
            )
        }.onFailure { throwable ->
            Log.w(
                WorkerLogTag,
                "Could not write appointment reminder notification to Firestore.",
                throwable
            )
        }
        return Result.success()
    }

    companion object {
        const val InputAppointmentId = "appointment_id"
        const val InputName = "name"
        const val InputLocation = "location"
        const val InputUserId = "user_id"
        const val InputAppointmentTimeMillis = "appointment_time_millis"
        const val InputScheduledAtMillis = "scheduled_at_millis"
        const val InputReminderKind = "reminder_kind"
    }
}

private const val WorkerLogTag = "AppointmentReminder"

private val appointmentTimeFormatter = SimpleDateFormat("HH:mm dd/MM", Locale("vi"))
