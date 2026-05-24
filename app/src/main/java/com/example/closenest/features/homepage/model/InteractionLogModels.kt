package com.example.closenest.features.homepage.model

data class NewInteractionLogRequest(
    val contactId: String,
    val contactName: String,
    val title: String?,
    val type: String,
    val note: String?,
    val photoUri: String?,
    val location: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val createdAtMillis: Long
)
