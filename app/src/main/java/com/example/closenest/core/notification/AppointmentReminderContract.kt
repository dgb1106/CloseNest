package com.example.closenest.core.notification

const val AppointmentReminderAppointmentIdExtra = "appointment_id"
const val AppointmentSourceEntityType = "appointment"

enum class AppointmentReminderKind {
    DAY_BEFORE,
    UPCOMING
}

fun appointmentReminderDedupeKey(
    appointmentId: String,
    kind: AppointmentReminderKind
): String {
    return "appointment:$appointmentId:${kind.dedupeSuffix}"
}

val AppointmentReminderKind.dedupeSuffix: String
    get() = when (this) {
        AppointmentReminderKind.DAY_BEFORE -> "day_before"
        AppointmentReminderKind.UPCOMING -> "upcoming"
    }
