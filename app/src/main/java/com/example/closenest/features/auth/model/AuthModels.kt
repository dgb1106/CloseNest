package com.example.closenest.features.auth.model

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val authMode: AuthMode = AuthMode.Login,
    val message: String = "Nhập email và mật khẩu để đăng nhập.",
    val isLoading: Boolean = false
)

enum class AuthMode {
    Login,
    Register
}
