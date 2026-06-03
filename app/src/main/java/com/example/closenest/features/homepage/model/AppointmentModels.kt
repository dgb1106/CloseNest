package com.example.closenest.features.homepage.model

data class NewAppointmentRequest(
    val name: String,
    val participantContactIds: List<String>,
    val participantContactNames: List<String>,
    val location: String,
    val locationLatitude: Double,
    val locationLongitude: Double,
    val appointmentDateMillis: Long,
    val createdAtMillis: Long
)

data class AppointmentItem(
    val id: String,
    val name: String,
    val participantContactIds: List<String>,
    val participantContactNames: List<String>,
    val location: String,
    val locationLatitude: Double,
    val locationLongitude: Double,
    val appointmentDateMillis: Long,
    val dateKey: String,
    val createdAtMillis: Long
)
