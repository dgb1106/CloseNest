package com.example.closenest.features.chatbot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.closenest.R
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import com.example.closenest.features.chatbot.repository.ChatbotAiReply
import com.example.closenest.features.chatbot.repository.ChatbotAiService
import com.example.closenest.features.chatbot.repository.ChatbotRepository
import com.example.closenest.features.chatbot.repository.GeminiApiException
import com.example.closenest.features.chatbot.repository.GiftCatalogService
import com.example.closenest.features.chatbot.viewmodel.ChatbotViewModel
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `select mode creates session and greeting message`() = runTest {
        val repository = FakeChatbotRepository()
        val viewModel = viewModel(repository = repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectMode(ChatMode.Vent)
        advanceUntilIdle()

        assertEquals(ChatMode.Vent, viewModel.uiState.value.mode)
        assertEquals("session-1", viewModel.uiState.value.sessionId)
        assertEquals(ChatRole.Assistant, repository.messages("session-1").first().role)
        assertFalse(viewModel.uiState.value.isCreatingSession)
    }

    @Test
    fun `send message stores user message then assistant reply with model`() = runTest {
        val repository = FakeChatbotRepository()
        val aiService = FakeAiService(reply = ChatbotAiReply("Mình nghe bạn.", "test-model"))
        val viewModel = viewModel(repository = repository, aiService = aiService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.selectMode(ChatMode.General)
        advanceUntilIdle()

        viewModel.onInputChanged("Hom nay minh hoi met")
        viewModel.sendMessage()
        advanceUntilIdle()

        val messages = repository.messages("session-1")
        assertEquals(listOf(ChatRole.Assistant, ChatRole.User, ChatRole.Assistant), messages.map { it.role })
        assertEquals("Hom nay minh hoi met", messages[1].text)
        assertEquals("Mình nghe bạn.", messages[2].text)
        assertEquals("test-model", messages[2].model)
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `ai quota error maps to friendly resource and hides raw payload`() = runTest {
        val repository = FakeChatbotRepository()
        val aiService = FakeAiService(failure = GeminiApiException(429, "quota exhausted API_KEY=secret"))
        val viewModel = viewModel(repository = repository, aiService = aiService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.selectMode(ChatMode.General)
        advanceUntilIdle()

        viewModel.onInputChanged("Can giup minh")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(R.string.chatbot_ai_quota_error, viewModel.uiState.value.errorMessageRes)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `delete selected sessions delegates repository and clears selection`() = runTest {
        val repository = FakeChatbotRepository(
            initialSessions = listOf(session(id = "s1"), session(id = "s2"))
        )
        val viewModel = viewModel(repository = repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.startSessionSelection()
        viewModel.toggleSessionSelection("s1")
        viewModel.deleteSelectedSessions()
        advanceUntilIdle()

        assertEquals(listOf("s1"), repository.lastDeletedIds)
        assertFalse(viewModel.uiState.value.isSelectingSessions)
        assertTrue(viewModel.uiState.value.selectedSessionIds.isEmpty())
    }

    private fun viewModel(
        repository: FakeChatbotRepository = FakeChatbotRepository(),
        aiService: FakeAiService = FakeAiService(),
        relationshipRepository: RelationshipRepository = FakeRelationshipRepository()
    ) = ChatbotViewModel(
        repository = repository,
        aiService = aiService,
        relationshipRepository = relationshipRepository,
        giftCatalogService = GiftCatalogService(ApplicationProvider.getApplicationContext<Context>())
    )

    private class FakeChatbotRepository(
        initialSessions: List<ChatSession> = emptyList()
    ) : ChatbotRepository {
        private val sessions = MutableStateFlow(initialSessions)
        private val messageFlows = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
        private var sessionCounter = initialSessions.size
        private var messageCounter = 0
        var lastDeletedIds: List<String> = emptyList()

        override fun observeSessions(): Flow<List<ChatSession>> = sessions

        override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
            messageFlows.getOrPut(sessionId) { MutableStateFlow(emptyList()) }

        override suspend fun createSession(mode: ChatMode): ChatSession {
            sessionCounter++
            val newSession = session(id = "session-$sessionCounter", mode = mode)
            sessions.value = sessions.value + newSession
            return newSession
        }

        override suspend fun addMessage(
            sessionId: String,
            role: ChatRole,
            text: String,
            model: String?
        ): ChatMessage {
            messageCounter++
            val message = ChatMessage(
                id = "message-$messageCounter",
                sessionId = sessionId,
                role = role,
                text = text,
                createdAtMillis = messageCounter.toLong(),
                model = model
            )
            val flow = messageFlows.getOrPut(sessionId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + message
            return message
        }

        override suspend fun deleteSessions(sessionIds: List<String>) {
            lastDeletedIds = sessionIds
            sessions.value = sessions.value.filterNot { it.id in sessionIds }
        }

        fun messages(sessionId: String): List<ChatMessage> =
            messageFlows[sessionId]?.value.orEmpty()
    }

    private class FakeAiService(
        private val reply: ChatbotAiReply = ChatbotAiReply("Tra loi tu AI", "fake-model"),
        private val failure: Throwable? = null
    ) : ChatbotAiService {
        override val modelName: String = reply.modelName

        override suspend fun generateReply(
            mode: ChatMode,
            messages: List<ChatMessage>
        ): ChatbotAiReply {
            failure?.let { throw it }
            return reply
        }

        override suspend fun generateReplyFromPrompt(prompt: String): ChatbotAiReply {
            failure?.let { throw it }
            return reply
        }
    }

    private class FakeRelationshipRepository : RelationshipRepository {
        private val relationships = MutableStateFlow(emptyList<RelationshipProfile>())

        override fun observeRelationships(): Flow<List<RelationshipProfile>> = relationships

        override suspend fun addRelationship(request: NewRelationshipRequest) = Unit

        override suspend fun deleteRelationship(relationshipId: String) = Unit

        override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) = Unit
    }
}

private fun session(
    id: String,
    mode: ChatMode = ChatMode.General
) = ChatSession(
    id = id,
    userId = "user-1",
    mode = mode,
    title = "Chat",
    createdAtMillis = 1_000L,
    updatedAtMillis = 1_000L
)
