package com.example.closenest.features.homepage.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewMemoryRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class FirebaseMemoryRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : MemoryRepository {

    override suspend fun addMemory(request: NewMemoryRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val document = memoriesCollection(userId).document()

        ensureFirestoreReachable()

        var photoDownloadUrl: String? = request.photoUri
        if (!request.photoUri.isNullOrBlank() && request.photoUri.startsWith("content://")) {
            photoDownloadUrl = uploadPhotoToStorage(userId, request.photoUri)
        }

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldContactId to request.contactId,
                FieldContactName to request.contactName,
                FieldTitle to request.title,
                FieldType to request.type,
                FieldNote to request.note,
                FieldPhotoUri to photoDownloadUrl,
                FieldLocation to request.location,
                FieldLocationLatitude to request.locationLatitude,
                FieldLocationLongitude to request.locationLongitude,
                FieldDateKey to dateFormatter.format(Date(request.createdAtMillis)),
                FieldCreatedAtMillis to request.createdAtMillis
            )
        ).awaitCompletion()
    }

    private suspend fun uploadPhotoToStorage(userId: String, localUri: String): String? {
        val extension = localUri.substringAfterLast(".").ifBlank { "jpg" }
            .substringBefore("?").takeIf { it.length <= 4 } ?: "jpg"
        val path = "memories/$userId/${UUID.randomUUID()}.$extension"
        val ref = storage.reference.child(path)
        return suspendCancellableCoroutine { continuation ->
            ref.putFile(android.net.Uri.parse(localUri))
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            if (continuation.isActive) {
                                continuation.resume(uri.toString())
                            }
                        }
                        .addOnFailureListener { exception ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    private fun memoriesCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(MemoriesCollection)
}

object MemoryRepositoryProvider {
    val repository: MemoryRepository by lazy {
        FirebaseMemoryRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
            storage = FirebaseStorage.getInstance()
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
private const val MemoriesCollection = "memories"

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
