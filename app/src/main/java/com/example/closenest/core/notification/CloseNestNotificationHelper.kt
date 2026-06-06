package com.example.closenest.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.closenest.MainActivity
import com.example.closenest.R
import kotlin.math.absoluteValue

object CloseNestNotificationHelper {
    private const val AppointmentReminderChannelId = "appointment_reminders"
    private const val AppointmentReminderRequestCodeOffset = 20_000

    fun showAppointmentReminder(
        context: Context,
        appointmentId: String,
        title: String,
        body: String
    ) {
        if (!canPostNotifications(context)) return

        ensureAppointmentReminderChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppointmentReminderAppointmentIdExtra, appointmentId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            AppointmentReminderRequestCodeOffset + appointmentId.hashCode().absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AppointmentReminderChannelId)
            .setSmallIcon(R.drawable.closenest_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(
            appointmentId.hashCode().absoluteValue,
            notification
        )
    }

    private fun ensureAppointmentReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            AppointmentReminderChannelId,
            context.getString(R.string.appointment_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.appointment_reminder_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}

