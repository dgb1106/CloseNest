package com.example.closenest.features.chatbot.model

enum class ChatMode(val storageValue: String) {
    General("general"),
    Vent("vent"),
    GiftAdvice("gift_advice");

    companion object {
        fun fromStorageValue(value: String?): ChatMode? =
            entries.firstOrNull { mode -> mode.storageValue == value }
    }
}

enum class ChatRole(val storageValue: String) {
    User("user"),
    Assistant("assistant");

    companion object {
        fun fromStorageValue(value: String?): ChatRole? =
            entries.firstOrNull { role -> role.storageValue == value }
    }
}

data class ChatSession(
    val id: String,
    val userId: String,
    val mode: ChatMode,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val text: String,
    val createdAtMillis: Long,
    val model: String? = null
)
