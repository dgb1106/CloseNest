package com.example.closenest.features.auth.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<Boolean>
    fun isLoggedIn(): Boolean
    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun register(firstName: String, lastName: String, email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun logout()
}
