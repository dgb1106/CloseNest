package com.example.closenest.features.auth.repository

import com.example.closenest.features.auth.model.UserDocument
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<Boolean>
    fun isLoggedIn(): Boolean
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(user: UserDocument, password: String): Result<Unit>
    suspend fun getUserDocument(uid: String): Result<UserDocument>
    suspend fun loginWithGoogle(idToken: String): Result<Unit>
    fun logout()
}
