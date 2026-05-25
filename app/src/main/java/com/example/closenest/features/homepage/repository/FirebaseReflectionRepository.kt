package com.example.closenest.features.homepage.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class FirebaseReflectionRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReflectionRepository {

    override suspend fun addReflection(request: NewReflectionRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val document = reflectionsCollection(userId).document()

        ensureFirestoreReachable()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldInteractedContactIds to request.interactedContacts.map { contact -> contact.id },
                FieldInteractedContactNames to request.interactedContacts.map { contact -> contact.name },
                FieldMood to request.mood,
                FieldFeelings to request.feelings,
                FieldSources to request.sources,
                FieldDateKey to dateFormatter.format(Date(request.createdAtMillis)),
                FieldCreatedAtMillis to request.createdAtMillis
            )
        ).awaitCompletion()
    }

    private fun reflectionsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(ReflectionsCollection)
}

object ReflectionRepositoryProvider {
    val repository: ReflectionRepository by lazy {
        FirebaseReflectionRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
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

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private const val UsersCollection = "users"
private const val ReflectionsCollection = "reflections"

private const val FirestoreHost = "firestore.googleapis.com"
private const val HttpsPort = 443
private const val ConnectionCheckTimeoutMillis = 5_000

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldInteractedContactIds = "interactedContactIds"
private const val FieldInteractedContactNames = "interactedContactNames"
private const val FieldMood = "mood"
private const val FieldFeelings = "feelings"
private const val FieldSources = "sources"
private const val FieldDateKey = "dateKey"
private const val FieldCreatedAtMillis = "createdAtMillis"
