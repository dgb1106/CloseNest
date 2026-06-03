package com.example.closenest.features.chatbot.repository

import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatbotRepository {
    fun observeSessions(): Flow<List<ChatSession>>

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>>

    suspend fun createSession(mode: ChatMode): ChatSession

    suspend fun addMessage(
        sessionId: String,
        role: ChatRole,
        text: String,
        model: String? = null
    ): ChatMessage

    suspend fun deleteSessions(sessionIds: List<String>)
}
