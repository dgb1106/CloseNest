package com.example.closenest.features.auth.repository

import com.example.closenest.features.auth.model.UserDocument
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val USERS_COLLECTION = "users"

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

            // Keep post-register flow on the auth screens so user can log in explicitly.
            auth.signOut()

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

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()

            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not found after Google login"))

            createOrUpdateGoogleUser(uid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createOrUpdateGoogleUser(uid: String) {
        try {
            val userDoc = getUserDocument(uid).getOrNull()
            if (userDoc == null) {
                // Create new user from Google
                val googleUser = auth.currentUser
                val now = Timestamp.now()
                val newUser = UserDocument(
                    uid = uid,
                    email = googleUser?.email ?: "",
                    firstName = googleUser?.displayName?.split(" ")?.firstOrNull() ?: "User",
                    lastName = googleUser?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                    createdAt = now,
                    lastCheckedIn = now,
                    streakCount = 1
                )
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .set(newUser.toMap())
                    .await()
            }
        } catch (_: Exception) {
            // User doc creation is non-critical for Google login; silently ignore failures
        }
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
