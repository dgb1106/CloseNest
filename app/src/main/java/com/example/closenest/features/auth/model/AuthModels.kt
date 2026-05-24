package com.example.closenest.features.auth.model

import com.google.firebase.Timestamp

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val message: String = "",
    val isLoading: Boolean = false
)

enum class AuthRoute {
    Login, Register
}

data class RegisterUiState(
    val authRoute: AuthRoute = AuthRoute.Login,
    val lastName: String = "",
    val firstName: String = "",
    val birthday: Timestamp? = null,
    val birthdayDisplay: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isSuccess: Boolean = false
)

data class UserDocument(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthday: Timestamp? = null,
    val email: String = "",
    val phoneNumber: String? = null,
    val gender: String? = null,
    val createdAt: Timestamp? = null,
    val lastCheckedIn: Timestamp? = null,
    val streakCount: Int = 0
)
