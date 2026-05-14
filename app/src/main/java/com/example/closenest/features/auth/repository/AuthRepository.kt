package com.example.closenest.features.auth.repository

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun register(firstName: String, lastName: String, email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun logout()
}
