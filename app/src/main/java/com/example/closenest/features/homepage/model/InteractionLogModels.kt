package com.example.closenest.features.homepage.model

data class NewInteractionLogRequest(
    val contactId: String,
    val contactName: String,
    val type: String,
    val note: String?,
    val createdAtMillis: Long
)
