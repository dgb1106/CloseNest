package com.example.closenest.features.homepage.model

data class NewAppointmentRequest(
    val name: String,
    val location: String,
    val locationLatitude: Double,
    val locationLongitude: Double,
    val appointmentDateMillis: Long,
    val createdAtMillis: Long
)
