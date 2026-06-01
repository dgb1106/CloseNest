package com.example.closenest.features.homepage.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewInteractionLogRequest
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

class FirebaseInteractionLogRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : InteractionLogRepository {

    override suspend fun addInteractionLog(request: NewInteractionLogRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val document = interactionsCollection(userId).document()

        ensureFirestoreReachable()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldContactId to request.contactId,
                FieldContactName to request.contactName,
                FieldTitle to request.title,
                FieldType to request.type,
                FieldNote to request.note,
                FieldPhotoUri to request.photoUri,
                FieldLocation to request.location,
                FieldLocationLatitude to request.locationLatitude,
                FieldLocationLongitude to request.locationLongitude,
                FieldDateKey to dateFormatter.format(Date(request.createdAtMillis)),
                FieldCreatedAtMillis to request.createdAtMillis
            )
        ).awaitCompletion()
    }

    private fun interactionsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(InteractionsCollection)
}

object InteractionLogRepositoryProvider {
    val repository: InteractionLogRepository by lazy {
        FirebaseInteractionLogRepository(
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
private const val InteractionsCollection = "interactions"

private const val FirestoreHost = "firestore.googleapis.com"
private const val HttpsPort = 443
private const val ConnectionCheckTimeoutMillis = 5_000

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldContactId = "contactId"
private const val FieldContactName = "contactName"
private const val FieldTitle = "title"
private const val FieldType = "type"
private const val FieldNote = "note"
private const val FieldPhotoUri = "photoUri"
private const val FieldLocation = "location"
private const val FieldLocationLatitude = "locationLatitude"
private const val FieldLocationLongitude = "locationLongitude"
private const val FieldDateKey = "dateKey"
private const val FieldCreatedAtMillis = "createdAtMillis"
