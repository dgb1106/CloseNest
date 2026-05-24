package com.example.closenest.features.auth.repository

import com.example.closenest.features.auth.model.UserDocument
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TIMEZONE_UTC7 = "Asia/Ho_Chi_Minh"
    }

    override val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not found after login"))
            updateStreakOnLogin(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(user: UserDocument, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("User creation failed"))

            val now = Timestamp.now()
            val userDoc = user.copy(
                uid = uid,
                createdAt = now,
                lastCheckedIn = now,
                streakCount = 1
            )

            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userDoc.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserDocument(uid: String): Result<UserDocument> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            if (snapshot.exists()) {
                Result.success(snapshot.toUserDocument(uid))
            } else {
                Result.failure(Exception("User document not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateStreakOnLogin(uid: String) {
        try {
            val docResult = getUserDocument(uid)
            if (docResult.isFailure) return

            val userDoc = docResult.getOrNull() ?: return
            val lastCheckedIn = userDoc.lastCheckedIn
            val newStreakCount = computeStreak(lastCheckedIn, userDoc.streakCount)

            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(
                    mapOf(
                        "lastCheckedIn" to Timestamp.now(),
                        "streakCount" to newStreakCount
                    )
                )
                .await()
        } catch (_: Exception) {
            // Streak update is non-critical; silently ignore failures
        }
    }

    private fun computeStreak(lastCheckedIn: Timestamp?, currentStreak: Int): Int {
        if (lastCheckedIn == null) return 1

        val utc7 = TimeZone.getTimeZone(TIMEZONE_UTC7)
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

    override fun logout() {
        auth.signOut()
    }
}

object AuthRepositoryProvider {
    private var instance: AuthRepository? = null

    fun getInstance(): AuthRepository {
        return instance ?: FirebaseAuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        ).also { instance = it }
    }
}

private fun UserDocument.toMap(): Map<String, Any?> {
    return mapOf(
        "uid" to uid,
        "firstName" to firstName,
        "lastName" to lastName,
        "birthday" to birthday,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "gender" to gender,
        "createdAt" to createdAt,
        "lastCheckedIn" to lastCheckedIn,
        "streakCount" to streakCount
    )
}

private fun DocumentSnapshot.toUserDocument(uid: String): UserDocument {
    return UserDocument(
        uid = uid,
        firstName = getString("firstName") ?: "",
        lastName = getString("lastName") ?: "",
        birthday = getTimestamp("birthday"),
        email = getString("email") ?: "",
        phoneNumber = getString("phoneNumber"),
        gender = getString("gender"),
        createdAt = getTimestamp("createdAt"),
        lastCheckedIn = getTimestamp("lastCheckedIn"),
        streakCount = getLong("streakCount")?.toInt() ?: 0
    )
}
