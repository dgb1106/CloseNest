package com.example.closenest.integration

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.ThemeMode
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import com.example.closenest.features.chatbot.repository.ChatbotAiReply
import com.example.closenest.features.chatbot.repository.ChatbotAiService
import com.example.closenest.features.chatbot.repository.ChatbotRepository
import com.example.closenest.features.chatbot.repository.GiftCatalogService
import com.example.closenest.features.chatbot.ui.FloatingChatbotOverlay
import com.example.closenest.features.chatbot.viewmodel.ChatbotViewModel
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test

class ChatbotOverlayIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chatbotOverlay_selectsModeSendsMessageAndShowsAssistantReply() {
        val viewModel = ChatbotViewModel(
            repository = FakeChatbotRepository(),
            aiService = FakeChatbotAiService(),
            relationshipRepository = FakeRelationshipRepository(),
            giftCatalogService = GiftCatalogService(ApplicationProvider.getApplicationContext())
        )

        composeRule.setCloseNestContent {
            FloatingChatbotOverlay(viewModel = viewModel)
        }

        composeRule.onNodeWithContentDescription("Mở chatbot").performClick()
        composeRule.onNodeWithText("Hôm nay bạn muốn nói chuyện gì?").assertIsDisplayed()
        composeRule.onNodeWithText("Mình muốn xả chút").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Nhắn với CloseNest").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Nhắn với CloseNest").performTextInput("Hôm nay mình hơi căng thẳng")
        composeRule.onNodeWithContentDescription("Gửi").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mình đã nghe bạn chia sẻ.", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Hôm nay mình hơi căng thẳng").assertIsDisplayed()
        composeRule.onNodeWithText("Mình đã nghe bạn chia sẻ.", substring = true).assertIsDisplayed()
    }
}

private class FakeChatbotRepository : ChatbotRepository {
    private val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messagesBySession = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private var sessionCounter = 0
    private var messageCounter = 0

    override fun observeSessions(): Flow<List<ChatSession>> = sessions

    override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        messagesBySession.map { messages -> messages[sessionId].orEmpty() }

    override suspend fun createSession(mode: ChatMode): ChatSession {
        val nowMillis = System.currentTimeMillis()
        val session = ChatSession(
            id = "session-${++sessionCounter}",
            userId = "user-1",
            mode = mode,
            title = mode.name,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
        sessions.value = listOf(session) + sessions.value
        return session
    }

    override suspend fun addMessage(
        sessionId: String,
        role: ChatRole,
        text: String,
        model: String?
    ): ChatMessage {
        val message = ChatMessage(
            id = "message-${++messageCounter}",
            sessionId = sessionId,
            role = role,
            text = text,
            createdAtMillis = System.currentTimeMillis(),
            model = model
        )
        messagesBySession.value = messagesBySession.value + (
            sessionId to messagesBySession.value[sessionId].orEmpty() + message
            )
        return message
    }

    override suspend fun deleteSessions(sessionIds: List<String>) {
        sessions.value = sessions.value.filterNot { it.id in sessionIds }
        messagesBySession.value = messagesBySession.value - sessionIds.toSet()
    }
}

private class FakeChatbotAiService : ChatbotAiService {
    override val modelName: String = "fake-gemini"

    override suspend fun generateReply(
        mode: ChatMode,
        messages: List<ChatMessage>
    ): ChatbotAiReply = ChatbotAiReply(
        text = "Mình đã nghe bạn chia sẻ. Mình sẽ phản hồi nhẹ nhàng và không lộ dữ liệu kỹ thuật.",
        modelName = modelName
    )

    override suspend fun generateReplyFromPrompt(prompt: String): ChatbotAiReply =
        ChatbotAiReply(
            text = """[{"id":"gift-1","reason":"Phù hợp sở thích."}]""",
            modelName = modelName
        )
}

private class FakeRelationshipRepository : RelationshipRepository {
    private val relationships = MutableStateFlow(
        listOf(
            RelationshipProfile(
                id = "rel-1",
                userId = "user-1",
                name = "An Nguyen",
                tag = RelationshipTag.Friend,
                birthdayIso = "2000-01-02",
                phoneNumber = "0900000000",
                email = "an@test.local",
                interests = listOf("sach", "cafe"),
                notes = null,
                avatarUrl = null,
                priority = RelationshipPriority.High,
                createdAtMillis = 1_700_000_000_000,
                updatedAtMillis = 1_700_000_000_000
            )
        )
    )

    override fun observeRelationships(): Flow<List<RelationshipProfile>> = relationships

    override suspend fun addRelationship(request: NewRelationshipRequest) = Unit

    override suspend fun deleteRelationship(relationshipId: String) = Unit

    override suspend fun updateRelationship(
        relationshipId: String,
        request: NewRelationshipRequest
    ) = Unit
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setCloseNestContent(
    content: @Composable () -> Unit
) {
    setContent {
        AppTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
            content()
        }
    }
}
