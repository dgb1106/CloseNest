package com.example.closenest.features.homepage.model

data class NewAppointmentRequest(
    val name: String,
    val location: String,
    val appointmentDateMillis: Long,
    val createdAtMillis: Long
)
