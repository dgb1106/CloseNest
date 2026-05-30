package com.example.closenest.features.chatbot.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import com.example.closenest.features.chatbot.repository.ChatbotAiService
import com.example.closenest.features.chatbot.repository.ChatbotAiServiceProvider
import com.example.closenest.features.chatbot.repository.ChatbotRepository
import com.example.closenest.features.chatbot.repository.ChatbotRepositoryProvider
import com.example.closenest.features.chatbot.repository.GeminiApiException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ChatbotUiState(
    val isLoading: Boolean = true,
    val sessionId: String? = null,
    val mode: ChatMode? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val isSelectingSessions: Boolean = false,
    val selectedSessionIds: Set<String> = emptySet(),
    val isDeletingSessions: Boolean = false,
    val inputText: String = "",
    val isCreatingSession: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    @param:StringRes val errorMessageRes: Int? = null
) {
    val showModeOptions: Boolean = !isLoading && mode == null
    val canSend: Boolean = inputText.isNotBlank() && !isSending && !isCreatingSession &&
        !isLoading && !isDeletingSessions
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModel(
    private val repository: ChatbotRepository,
    private val aiService: ChatbotAiService
) : ViewModel() {
    private val draft = MutableStateFlow(ChatbotDraft())
    private val selectedSession = MutableStateFlow<ChatSession?>(null)

    private val sessionHistoryResult = repository.observeSessions()
        .map { sessions ->
            ChatSessionsResult(sessions = sessions)
        }
        .onStart {
            emit(ChatSessionsResult(isLoading = true))
        }
        .catch {
            emit(ChatSessionsResult(errorMessageRes = R.string.chatbot_load_error))
        }

    private val messageResult = selectedSession.flatMapLatest { session ->
        when (session) {
            null -> flowOf(ChatMessagesResult())
            else -> repository.observeMessages(session.id)
                .map { messages -> ChatMessagesResult(messages = messages) }
                .onStart { emit(ChatMessagesResult(isLoading = true)) }
                .catch {
                    emit(ChatMessagesResult(errorMessageRes = R.string.chatbot_load_error))
                }
        }
    }

    val uiState = combine(
        selectedSession,
        sessionHistoryResult,
        messageResult,
        draft
    ) { session, sessionsResult, messagesResult, currentDraft ->
        ChatbotUiState(
            isLoading = messagesResult.isLoading || (session == null && sessionsResult.isLoading),
            sessionId = session?.id,
            mode = session?.mode,
            messages = messagesResult.messages,
            sessions = sessionsResult.sessions,
            isSelectingSessions = currentDraft.isSelectingSessions,
            selectedSessionIds = currentDraft.selectedSessionIds,
            isDeletingSessions = currentDraft.isDeletingSessions,
            inputText = currentDraft.inputText,
            isCreatingSession = currentDraft.isCreatingSession,
            isSending = currentDraft.isSending,
            errorMessage = currentDraft.errorMessage,
            errorMessageRes = currentDraft.errorMessageRes
                ?: messagesResult.errorMessageRes
                ?: sessionsResult.errorMessageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatbotUiState()
    )

    fun onInputChanged(value: String) {
        draft.update { current ->
            current.copy(
                inputText = value,
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    fun requestNewConversation() {
        selectedSession.value = null
        draft.update { current ->
            current.copy(
                inputText = "",
                isSelectingSessions = false,
                selectedSessionIds = emptySet(),
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    fun selectSession(session: ChatSession) {
        selectedSession.value = session
        draft.update { current ->
            current.copy(
                inputText = "",
                isSelectingSessions = false,
                selectedSessionIds = emptySet(),
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    fun startSessionSelection() {
        draft.update { current ->
            if (current.isDeletingSessions) {
                current
            } else {
                current.copy(
                    isSelectingSessions = true,
                    selectedSessionIds = emptySet()
                )
            }
        }
    }

    fun cancelSessionSelection() {
        draft.update { current ->
            current.copy(
                isSelectingSessions = false,
                selectedSessionIds = emptySet()
            )
        }
    }

    fun toggleSessionSelection(sessionId: String) {
        draft.update { current ->
            if (!current.isSelectingSessions || current.isDeletingSessions) {
                current
            } else {
                val updated = current.selectedSessionIds.toMutableSet()
                if (!updated.add(sessionId)) {
                    updated.remove(sessionId)
                }
                current.copy(selectedSessionIds = updated)
            }
        }
    }

    fun deleteSelectedSessions() {
        val selectedIds = uiState.value.selectedSessionIds
        if (selectedIds.isEmpty() || uiState.value.isDeletingSessions) return

        viewModelScope.launch {
            draft.update { current ->
                current.copy(
                    isDeletingSessions = true,
                    errorMessage = null,
                    errorMessageRes = null
                )
            }

            runCatching {
                withTimeout(FirestoreTimeoutMillis) {
                    repository.deleteSessions(selectedIds.toList())
                }
            }.onSuccess {
                if (selectedSession.value?.id in selectedIds) {
                    selectedSession.value = null
                }
                draft.update { current ->
                    current.copy(
                        isDeletingSessions = false,
                        isSelectingSessions = false,
                        selectedSessionIds = emptySet()
                    )
                }
            }.onFailure { throwable ->
                Log.e(ChatbotLogTag, "Failed to delete chatbot sessions.", throwable)
                draft.update { current ->
                    current.copy(
                        isDeletingSessions = false,
                        errorMessageRes = R.string.chatbot_delete_error
                    )
                }
            }
        }
    }

    fun selectMode(mode: ChatMode) {
        val currentState = uiState.value
        if (currentState.isCreatingSession || currentState.isSending || currentState.isDeletingSessions) return

        viewModelScope.launch {
            draft.update { current ->
                current.copy(
                    isCreatingSession = true,
                    isSelectingSessions = false,
                    selectedSessionIds = emptySet(),
                    errorMessage = null,
                    errorMessageRes = null
                )
            }

            runCatching {
                withTimeout(FirestoreTimeoutMillis) {
                    repository.createSession(mode)
                }.also { session ->
                    withTimeout(FirestoreTimeoutMillis) {
                        repository.addMessage(
                            sessionId = session.id,
                            role = ChatRole.Assistant,
                            text = mode.greeting()
                        )
                    }
                }
            }.onSuccess { session ->
                selectedSession.value = session
                draft.update { current ->
                    current.copy(isCreatingSession = false)
                }
            }.onFailure { throwable ->
                Log.e(ChatbotLogTag, "Failed to create chatbot session.", throwable)
                draft.update { current ->
                    current.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.toChatbotErrorMessage(),
                        errorMessageRes = throwable.toChatbotErrorMessageRes()
                    )
                }
            }
        }
    }

    fun sendMessage() {
        val currentState = uiState.value
        val text = currentState.inputText.trim()
        if (text.isBlank() || !currentState.canSend || currentState.isDeletingSessions) return

        viewModelScope.launch {
            draft.update { current ->
                current.copy(
                    inputText = "",
                    isSending = true,
                    errorMessage = null,
                    errorMessageRes = null
                )
            }

            runCatching {
                val sessionId = currentState.sessionId
                val mode = currentState.mode
                if (sessionId == null || mode == null) {
                    val session = withTimeout(FirestoreTimeoutMillis) {
                        repository.createSession(ChatMode.General)
                    }
                    selectedSession.value = session
                    sendMessageInSession(
                        sessionId = session.id,
                        mode = session.mode,
                        existingMessages = emptyList(),
                        text = text
                    )
                } else {
                    sendMessageInSession(
                        sessionId = sessionId,
                        mode = mode,
                        existingMessages = currentState.messages,
                        text = text
                    )
                }
            }.onSuccess {
                draft.update { current ->
                    current.copy(isSending = false)
                }
            }.onFailure { throwable ->
                Log.e(ChatbotLogTag, "Failed to send chatbot message.", throwable)
                draft.update { current ->
                    current.copy(
                        isSending = false,
                        errorMessage = throwable.toChatbotErrorMessage(),
                        errorMessageRes = throwable.toChatbotErrorMessageRes()
                    )
                }
            }
        }
    }

    private suspend fun sendMessageInSession(
        sessionId: String,
        mode: ChatMode,
        existingMessages: List<ChatMessage>,
        text: String
    ) {
        val userMessage = withTimeout(FirestoreTimeoutMillis) {
            repository.addMessage(
                sessionId = sessionId,
                role = ChatRole.User,
                text = text
            )
        }
        val reply = withTimeout(AiTimeoutMillis) {
            aiService.generateReply(
                mode = mode,
                messages = existingMessages + userMessage
            )
        }
        withTimeout(FirestoreTimeoutMillis) {
            repository.addMessage(
                sessionId = sessionId,
                role = ChatRole.Assistant,
                text = reply.text,
                model = reply.modelName
            )
        }
    }

    fun clearError() {
        draft.update { current ->
            current.copy(
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatbotViewModel(
                    repository = ChatbotRepositoryProvider.repository,
                    aiService = ChatbotAiServiceProvider.service
                )
            }
        }

        private const val FirestoreTimeoutMillis = 15_000L
        private const val AiTimeoutMillis = 45_000L
    }
}

private data class ChatbotDraft(
    val inputText: String = "",
    val isSelectingSessions: Boolean = false,
    val selectedSessionIds: Set<String> = emptySet(),
    val isDeletingSessions: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    @param:StringRes val errorMessageRes: Int? = null
)

private data class ChatSessionsResult(
    val sessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null
)

private data class ChatMessagesResult(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null
)

private fun ChatMode.greeting(): String {
    return when (this) {
        ChatMode.General -> "Mình đây. Bạn cứ nhắn điều bạn đang muốn nói, mình sẽ theo mạch của bạn."
        ChatMode.Vent -> "Mình ở đây rồi. Bạn cứ nói hết những gì đang làm bạn bực hoặc mệt, mình sẽ nghe trước đã."
        ChatMode.GiftAdvice -> "Mình giúp bạn chọn quà nhé. Bạn muốn tặng ai, vào dịp gì và khoảng ngân sách bao nhiêu?"
    }
}

private fun Throwable.toChatbotErrorMessageRes(): Int {
    return when (this) {
        is FirebaseConnectionException -> R.string.chatbot_connection_error
        is FirebaseFirestoreException -> {
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                R.string.chatbot_permission_error
            } else {
                R.string.chatbot_unknown_error
            }
        }
        is TimeoutCancellationException -> R.string.chatbot_timeout_error
        is GeminiApiException -> toGeminiErrorMessageRes()
        else -> R.string.chatbot_unknown_error
    }
}

private fun Throwable.toChatbotErrorMessage(): String? {
    return when (this) {
        is GeminiApiException -> {
            if (toGeminiErrorMessageRes() == R.string.chatbot_unknown_error) {
                "Gemini lỗi HTTP $statusCode: $apiMessage"
            } else {
                null
            }
        }
        else -> null
    }
}

private fun GeminiApiException.toGeminiErrorMessageRes(): Int {
    return when {
        statusCode == 0 -> R.string.chatbot_ai_key_error
        statusCode == 400 && apiMessage.contains("API key", ignoreCase = true) ->
            R.string.chatbot_ai_key_error
        statusCode == 401 || statusCode == 403 -> R.string.chatbot_ai_permission_error
        statusCode == 429 || isQuotaLikeError() -> R.string.chatbot_ai_quota_error
        apiMessage.contains("not supported", ignoreCase = true) -> R.string.chatbot_ai_location_error
        apiMessage.contains("blocked", ignoreCase = true) ||
            apiMessage.contains("SAFETY", ignoreCase = true) ->
            R.string.chatbot_ai_blocked_error
        else -> R.string.chatbot_unknown_error
    }
}

private fun GeminiApiException.isQuotaLikeError(): Boolean {
    return apiMessage.contains("quota", ignoreCase = true) ||
        apiMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
        apiMessage.contains("prepayment", ignoreCase = true) ||
        apiMessage.contains("depleted", ignoreCase = true) ||
        apiMessage.contains("billing", ignoreCase = true)
}

private const val ChatbotLogTag = "CloseNestChatbot"
