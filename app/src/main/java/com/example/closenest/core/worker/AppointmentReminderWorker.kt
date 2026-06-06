package com.example.closenest.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.closenest.R
import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.CloseNestNotificationHelper
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
        val appointmentTimeMillis = inputData.getLong(InputAppointmentTimeMillis, 0L)
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
        return Result.success()
    }

    companion object {
        const val InputAppointmentId = "appointment_id"
        const val InputName = "name"
        const val InputLocation = "location"
        const val InputAppointmentTimeMillis = "appointment_time_millis"
        const val InputReminderKind = "reminder_kind"
    }
}

private val appointmentTimeFormatter = SimpleDateFormat("HH:mm dd/MM", Locale("vi"))

