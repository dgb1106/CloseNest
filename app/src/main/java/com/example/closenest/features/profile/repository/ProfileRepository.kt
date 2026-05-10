package com.example.closenest.features.profile.repository

import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeCurrentUser(): Flow<UserProfile?>
    fun observeProfileUiState(): Flow<ProfileUiState>
    suspend fun logout(): Result<Unit>
    suspend fun updateProfile(user: UserProfile): Result<Unit>
}
