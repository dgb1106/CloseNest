package com.example.closenest.features.homepage.repository

import com.example.closenest.features.homepage.model.AppointmentItem
import com.example.closenest.features.homepage.model.NewAppointmentRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseAppointmentRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AppointmentRepository {

    override suspend fun addAppointment(request: NewAppointmentRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val document = appointmentsCollection(userId).document()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldName to request.name,
                FieldParticipantContactIds to request.participantContactIds,
                FieldParticipantContactNames to request.participantContactNames,
                FieldLocation to request.location,
                FieldLocationLatitude to request.locationLatitude,
                FieldLocationLongitude to request.locationLongitude,
                FieldAppointmentDateMillis to request.appointmentDateMillis,
                FieldDateKey to dateFormatter.format(Date(request.appointmentDateMillis)),
                FieldCreatedAtMillis to request.createdAtMillis
            )
        ).awaitCompletion()
    }

    override suspend fun countUpcomingAppointments(todayMillis: Long): Int {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val startOfTodayMillis = startOfDayMillis(todayMillis)

        val snapshot = appointmentsCollection(userId)
            .whereGreaterThanOrEqualTo(FieldAppointmentDateMillis, startOfTodayMillis)
            .get()
            .awaitResult()

        return snapshot.size()
    }

    override suspend fun getUpcomingAppointments(todayMillis: Long): List<AppointmentItem> {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val startOfTodayMillis = startOfDayMillis(todayMillis)

        val snapshot = appointmentsCollection(userId)
            .whereGreaterThanOrEqualTo(FieldAppointmentDateMillis, startOfTodayMillis)
            .orderBy(FieldAppointmentDateMillis)
            .get()
            .awaitResult()

        return snapshot.documents.mapNotNull { doc ->
            val id = doc.getString(FieldId) ?: return@mapNotNull null
            val name = doc.getString(FieldName) ?: return@mapNotNull null
            val participantContactIds = doc.get(FieldParticipantContactIds) as? List<*>
            val participantContactNames = doc.get(FieldParticipantContactNames) as? List<*>
            val location = doc.getString(FieldLocation) ?: return@mapNotNull null
            val locationLatitude = doc.getDouble(FieldLocationLatitude) ?: return@mapNotNull null
            val locationLongitude = doc.getDouble(FieldLocationLongitude) ?: return@mapNotNull null
            val appointmentDateMillis = doc.getLong(FieldAppointmentDateMillis) ?: return@mapNotNull null
            val dateKey = doc.getString(FieldDateKey) ?: return@mapNotNull null
            val createdAtMillis = doc.getLong(FieldCreatedAtMillis) ?: return@mapNotNull null
            AppointmentItem(
                id = id,
                name = name,
                participantContactIds = participantContactIds?.filterIsInstance<String>().orEmpty(),
                participantContactNames = participantContactNames?.filterIsInstance<String>().orEmpty(),
                location = location,
                locationLatitude = locationLatitude,
                locationLongitude = locationLongitude,
                appointmentDateMillis = appointmentDateMillis,
                dateKey = dateKey,
                createdAtMillis = createdAtMillis
            )
        }
    }

    private fun startOfDayMillis(todayMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = todayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
}

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private const val UsersCollection = "users"
private const val AppointmentsCollection = "appointments"

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldName = "name"
private const val FieldParticipantContactIds = "participantContactIds"
private const val FieldParticipantContactNames = "participantContactNames"
private const val FieldLocation = "location"
private const val FieldLocationLatitude = "locationLatitude"
private const val FieldLocationLongitude = "locationLongitude"
private const val FieldAppointmentDateMillis = "appointmentDateMillis"
private const val FieldDateKey = "dateKey"
private const val FieldCreatedAtMillis = "createdAtMillis"
