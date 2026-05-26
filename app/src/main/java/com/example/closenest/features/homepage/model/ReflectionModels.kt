package com.example.closenest.features.homepage.model

data class NewReflectionRequest(
    val interactedContacts: List<ReflectionContactSnapshot>,
    val mood: String,
    val feelings: List<String>,
    val sources: List<String>,
    val createdAtMillis: Long
)

data class ReflectionContactSnapshot(
    val id: String,
    val name: String
)
