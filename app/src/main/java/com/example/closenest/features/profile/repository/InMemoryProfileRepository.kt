package com.example.closenest.features.profile.repository

import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryProfileRepository : ProfileRepository {
    
    private val mockRecentRelationships = listOf(
        RelationshipQuickPreview(
            id = "rel_1",
            name = "Bảo Nam",
            avatarUrl = null,
            initials = "BN"
        ),
        RelationshipQuickPreview(
            id = "rel_2",
            name = "Gia Hân",
            avatarUrl = null,
            initials = "GH"
        ),
        RelationshipQuickPreview(
            id = "rel_3",
            name = "Minh Anh",
            avatarUrl = null,
            initials = "MA"
        )
    )

    // Use MutableStateFlow for reactive updates
    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = "user_1",
            firstName = "Starry",
            lastName = "Skies",
            email = "starry@example.com",
            phoneNumber = "+84 9 1234 5678",
            birthday = Timestamp.now(),
            gender = "Female",
            createdAt = Timestamp.now(),
            lastCheckedIn = Timestamp.now(),
            streakCount = 1
        )
    )

    override fun observeCurrentUser(): Flow<UserProfile?> {
        return _currentUser
    }

    override fun observeProfileUiState(): Flow<ProfileUiState> {
        return _currentUser.map { user ->
            ProfileUiState(
                isLoading = false,
                user = user,
                recentRelationships = mockRecentRelationships,
                errorMessage = null,
                showLogoutDialog = false
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateProfile(user: UserProfile): Result<Unit> {
        // Update the mutable state flow
        _currentUser.value = user
        return Result.success(Unit)
    }
}

object ProfileRepositoryProvider {
    private var instance: ProfileRepository? = null

    fun getInstance(): ProfileRepository {
        return instance ?: InMemoryProfileRepository().also {
            instance = it
        }
    }
}
