package com.example.closenest.features.homepage.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewAppointmentRequest
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

class FirebaseAppointmentRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AppointmentRepository {

    override suspend fun addAppointment(request: NewAppointmentRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val document = appointmentsCollection(userId).document()

        ensureFirestoreReachable()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldName to request.name,
                FieldLocation to request.location,
                FieldAppointmentDateMillis to request.appointmentDateMillis,
                FieldDateKey to dateFormatter.format(Date(request.appointmentDateMillis)),
                FieldCreatedAtMillis to request.createdAtMillis
            )
        ).awaitCompletion()
    }

    private fun appointmentsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(AppointmentsCollection)
}

object AppointmentRepositoryProvider {
    val repository: AppointmentRepository by lazy {
        FirebaseAppointmentRepository(
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
private const val AppointmentsCollection = "appointments"

private const val FirestoreHost = "firestore.googleapis.com"
private const val HttpsPort = 443
private const val ConnectionCheckTimeoutMillis = 5_000

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldName = "name"
private const val FieldLocation = "location"
private const val FieldAppointmentDateMillis = "appointmentDateMillis"
private const val FieldDateKey = "dateKey"
private const val FieldCreatedAtMillis = "createdAtMillis"
