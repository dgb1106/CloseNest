package com.example.closenest.features.profile.repository

import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

class InMemoryProfileRepository : ProfileRepository {
    
    private val mockUser = UserProfile(
        id = "user_1",
        name = "starryskies23",
        email = "starry@example.com",
        phoneNumber = "+84 9 1234 5678",
        avatarUrl = null, // Will show initials
        dateOfBirth = "1995-03-15",
        gender = "Female",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private val mockRecentRelationships = listOf(
        RelationshipQuickPreview(
            id = "rel_1",
            name = "Bảo Nam",
            avatarUrl = null,
            initials = "AJ"
        ),
        RelationshipQuickPreview(
            id = "rel_2",
            name = "Gia Hân",
            avatarUrl = null,
            initials = "BS"
        ),
        RelationshipQuickPreview(
            id = "rel_3",
            name = "Minh Anh",
            avatarUrl = null,
            initials = "CB"
        )
    )

    override fun observeCurrentUser(): Flow<UserProfile?> {
        return flowOf(mockUser)
    }

    override fun observeProfileUiState(): Flow<ProfileUiState> {
        return flowOf(
            ProfileUiState(
                isLoading = false,
                user = mockUser,
                recentRelationships = mockRecentRelationships,
                errorMessage = null,
                showLogoutDialog = false
            )
        )
    }

    override suspend fun logout(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateProfile(user: UserProfile): Result<Unit> {
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
