package com.example.closenest.features.profile.repository

import com.example.closenest.features.auth.model.UserDocument
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProfileRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override fun observeCurrentUser(): Flow<UserProfile?> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(null)
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val userDoc = snapshot.toUserDocument(currentUid)
                    val userProfile = userDoc.toUserProfile()
                    trySend(userProfile)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeProfileUiState(): Flow<ProfileUiState> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(ProfileUiState(isLoading = false, errorMessage = "Chưa đăng nhập"))
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ProfileUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Lỗi tải dữ liệu"
                    ))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val userDoc = snapshot.toUserDocument(currentUid)
                    val userProfile = userDoc.toUserProfile()
                    trySend(ProfileUiState(
                        isLoading = false,
                        user = userProfile,
                        recentRelationships = emptyList(),  // TODO: Load from relationships collection
                        errorMessage = null
                    ))
                } else {
                    trySend(ProfileUiState(
                        isLoading = false,
                        errorMessage = "Không tìm thấy profile"
                    ))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(user: UserProfile): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(user.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension: Convert UserDocument → UserProfile
private fun UserDocument.toUserProfile(): UserProfile {
    return UserProfile(
        uid = uid,
        firstName = firstName,
        lastName = lastName,
        email = email,
        birthday = birthday,
        phoneNumber = phoneNumber,
        gender = gender,
        createdAt = createdAt,
        lastCheckedIn = lastCheckedIn,
        streakCount = streakCount,
        avatarUrl = null  // TODO: Support avatar in future
    )
}

// Extension: Convert UserProfile → Firestore Map
private fun UserProfile.toMap(): Map<String, Any?> {
    return mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "gender" to gender,
        "birthday" to birthday,
        "lastCheckedIn" to lastCheckedIn,
        "streakCount" to streakCount
        // uid, createdAt không update (read-only)
    )
}

// Extension: Convert Firestore DocumentSnapshot → UserDocument
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
