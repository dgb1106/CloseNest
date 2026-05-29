package com.example.closenest.features.chatbot.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class FirebaseChatbotRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ChatbotRepository {

    override fun observeSessions(): Flow<List<ChatSession>> = callbackFlow {
        var sessionRegistration: ListenerRegistration? = null

        fun stopSessionListener() {
            sessionRegistration?.remove()
            sessionRegistration = null
        }

        fun listenForSessions(userId: String) {
            stopSessionListener()
            sessionRegistration = sessionsCollection(userId)
                .orderBy(FieldUpdatedAtMillis, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val sessions = snapshot
                        ?.documents
                        .orEmpty()
                        .mapNotNull { document ->
                            document.toChatSession(defaultUserId = userId)
                        }
                    trySend(sessions)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId == null) {
                stopSessionListener()
                trySend(emptyList())
            } else {
                listenForSessions(currentUserId)
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            stopSessionListener()
            auth.removeAuthStateListener(authListener)
        }
    }

    override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = messagesCollection(userId, sessionId)
            .orderBy(FieldCreatedAtMillis, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        document.toChatMessage(defaultSessionId = sessionId)
                    }
                trySend(messages)
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun createSession(mode: ChatMode): ChatSession {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val now = System.currentTimeMillis()
        val document = sessionsCollection(userId).document()
        val title = mode.defaultTitle()

        ensureFirestoreReachable()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldMode to mode.storageValue,
                FieldTitle to title,
                FieldCreatedAtMillis to now,
                FieldUpdatedAtMillis to now
            )
        ).awaitCompletion()

        return ChatSession(
            id = document.id,
            userId = userId,
            mode = mode,
            title = title,
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    override suspend fun addMessage(
        sessionId: String,
        role: ChatRole,
        text: String,
        model: String?
    ): ChatMessage {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val now = System.currentTimeMillis()
        val messageDocument = messagesCollection(userId, sessionId).document()
        val sessionDocument = sessionsCollection(userId).document(sessionId)

        ensureFirestoreReachable()

        firestore.batch()
            .set(
                messageDocument,
                mapOf(
                    FieldId to messageDocument.id,
                    FieldSessionId to sessionId,
                    FieldRole to role.storageValue,
                    FieldText to text,
                    FieldModel to model,
                    FieldCreatedAtMillis to now
                )
            )
            .update(sessionDocument, FieldUpdatedAtMillis, now)
            .commit()
            .awaitCompletion()

        return ChatMessage(
            id = messageDocument.id,
            sessionId = sessionId,
            role = role,
            text = text,
            createdAtMillis = now,
            model = model
        )
    }

    private fun sessionsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(ChatSessionsCollection)

    private fun messagesCollection(userId: String, sessionId: String) =
        sessionsCollection(userId)
            .document(sessionId)
            .collection(MessagesCollection)
}

object ChatbotRepositoryProvider {
    val repository: ChatbotRepository by lazy {
        FirebaseChatbotRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
}

private fun DocumentSnapshot.toChatSession(defaultUserId: String): ChatSession? {
    val mode = ChatMode.fromStorageValue(getNullableString(FieldMode)) ?: return null
    return ChatSession(
        id = getNullableString(FieldId) ?: id,
        userId = getNullableString(FieldUserId) ?: defaultUserId,
        mode = mode,
        title = getNullableString(FieldTitle) ?: mode.defaultTitle(),
        createdAtMillis = getMillis(FieldCreatedAtMillis) ?: 0L,
        updatedAtMillis = getMillis(FieldUpdatedAtMillis) ?: 0L
    )
}

private fun DocumentSnapshot.toChatMessage(defaultSessionId: String): ChatMessage? {
    val role = ChatRole.fromStorageValue(getNullableString(FieldRole)) ?: return null
    val text = getNullableString(FieldText)?.takeIf { it.isNotBlank() } ?: return null
    return ChatMessage(
        id = getNullableString(FieldId) ?: id,
        sessionId = getNullableString(FieldSessionId) ?: defaultSessionId,
        role = role,
        text = text,
        createdAtMillis = getMillis(FieldCreatedAtMillis) ?: 0L,
        model = getNullableString(FieldModel)
    )
}

private fun ChatMode.defaultTitle(): String {
    return when (this) {
        ChatMode.General -> "Trò chuyện"
        ChatMode.Vent -> "Xả cảm xúc"
        ChatMode.GiftAdvice -> "Tư vấn quà tặng"
    }
}

private fun DocumentSnapshot.getNullableString(field: String): String? =
    get(field) as? String

private fun DocumentSnapshot.getMillis(field: String): Long? {
    return when (val value = get(field)) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        else -> null
    }
}

private suspend fun Task<*>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}

private suspend fun ensureFirestoreReachable() {
    withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(FirestoreHost, HttpsPort),
                    ConnectionCheckTimeoutMillis
                )
            }
        }.onFailure { throwable ->
            throw FirebaseConnectionException(throwable)
        }
    }
}

private const val UsersCollection = "users"
private const val ChatSessionsCollection = "chatSessions"
private const val MessagesCollection = "messages"

private const val FirestoreHost = "firestore.googleapis.com"
private const val HttpsPort = 443
private const val ConnectionCheckTimeoutMillis = 5_000

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldSessionId = "sessionId"
private const val FieldMode = "mode"
private const val FieldTitle = "title"
private const val FieldRole = "role"
private const val FieldText = "text"
private const val FieldModel = "model"
private const val FieldCreatedAtMillis = "createdAtMillis"
private const val FieldUpdatedAtMillis = "updatedAtMillis"
