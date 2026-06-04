package com.example.closenest.features.homepage.repository

import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.MoodDayEntry
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
        val todayKey = dateFormatter.format(Date(request.createdAtMillis))

        ensureFirestoreReachable()

        val existingSnapshot = reflectionsCollection(userId)
            .whereEqualTo(FieldDateKey, todayKey)
            .limit(1)
            .get()
            .awaitResult()

        val reflectionData = mapOf(
            FieldUserId to userId,
            FieldInteractedContactIds to request.interactedContacts.map { contact -> contact.id },
            FieldInteractedContactNames to request.interactedContacts.map { contact -> contact.name },
            FieldMood to request.mood,
            FieldFeelings to request.feelings,
            FieldSources to request.sources,
            FieldDateKey to todayKey,
            FieldCreatedAtMillis to request.createdAtMillis
        )

        if (existingSnapshot != null && !existingSnapshot.isEmpty) {
            val existingDoc = existingSnapshot.documents[0]
            existingDoc.reference.update(reflectionData).awaitCompletion()
        } else {
            val document = reflectionsCollection(userId).document()
            val dataWithId = reflectionData + mapOf(FieldId to document.id)
            document.set(dataWithId).awaitCompletion()
        }

        updateStreakOnReflection(userId)
    }

    override suspend fun getRecentMoods(days: Int): Result<List<MoodDayEntry>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(
                IllegalStateException("No signed-in Firebase user.")
            )

            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            }
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            cal.add(Calendar.DAY_OF_YEAR, -days + 1)
            val startDate = formatter.format(cal.time)
            val endDate = formatter.format(Date())

            val snapshot = reflectionsCollection(userId)
                .whereGreaterThanOrEqualTo(FieldDateKey, startDate)
                .whereLessThanOrEqualTo(FieldDateKey, endDate)
                .get()
                .awaitResult()

            val entries = snapshot.documents.mapNotNull { doc ->
                val dateKey = doc.getString(FieldDateKey) ?: return@mapNotNull null
                val mood = doc.getString(FieldMood) ?: return@mapNotNull null
                MoodDayEntry(dateKey = dateKey, mood = mood)
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateStreakOnReflection(userId: String) {
        try {
            val userDoc = firestore.collection(UsersCollection)
                .document(userId)
                .get()
                .awaitResult()

            if (userDoc == null || !userDoc.exists()) return

            val lastCheckedIn = userDoc.getTimestamp("lastCheckedIn")
            val currentStreak = userDoc.getLong("streakCount")?.toInt() ?: 0
            val newStreak = computeStreak(lastCheckedIn, currentStreak)

            firestore.collection(UsersCollection)
                .document(userId)
                .update(
                    mapOf(
                        "lastCheckedIn" to Timestamp.now(),
                        "streakCount" to newStreak
                    )
                )
                .awaitCompletion()
        } catch (_: Exception) {
            // non-critical
        }
    }

    private fun computeStreak(lastCheckedIn: Timestamp?, currentStreak: Int): Int {
        if (lastCheckedIn == null) return 1

        val utc7 = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val nowCal = Calendar.getInstance(utc7)
        val lastCal = Calendar.getInstance(utc7).apply {
            timeInMillis = lastCheckedIn.seconds * 1000
        }

        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val lastDay = lastCal.get(Calendar.DAY_OF_YEAR)
        val lastYear = lastCal.get(Calendar.YEAR)

        return when {
            nowYear == lastYear && nowDay == lastDay -> currentStreak
            nowYear == lastYear && nowDay - lastDay == 1 -> currentStreak + 1
            nowYear != lastYear && nowDay == 1 && lastDay == daysInYear(lastYear) -> currentStreak + 1
            else -> 1
        }
    }

    private fun daysInYear(year: Int): Int {
        val cal = Calendar.getInstance().apply { set(Calendar.YEAR, year) }
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR)
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

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
}

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
