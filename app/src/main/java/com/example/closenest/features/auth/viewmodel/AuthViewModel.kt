package com.example.closenest.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.features.auth.model.AuthMode
import com.example.closenest.features.auth.model.AuthUiState
import com.example.closenest.features.auth.repository.AuthRepository
import com.example.closenest.features.auth.repository.AuthRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(isLoggedIn = repository.isLoggedIn())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authState.collect { isLoggedIn ->
                if (!isLoggedIn) {
                    _uiState.update { it.copy(isLoggedIn = false, message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.") }
                }
            }
        }
    }

    fun onModeChange(mode: AuthMode) {
        _uiState.update {
            it.copy(authMode = mode, message = "")
        }
    }

    fun onLogin(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isEmpty() || password.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập đầy đủ email và mật khẩu.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, message = "Đang đăng nhập...") }

        repository.login(normalizedEmail, password) { result ->
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isLoggedIn = true, isLoading = false, message = "") },
                    onFailure = { e ->
                        state.copy(
                            isLoading = false,
                            message = e.localizedMessage ?: "Đăng nhập thất bại."
                        )
                    }
                )
            }
        }
    }

    fun onRegister(firstName: String, lastName: String, email: String, password: String) {
        val normalizedFirstName = firstName.trim()
        val normalizedLastName = lastName.trim()
        val normalizedEmail = email.trim()

        when {
            normalizedFirstName.isEmpty() -> {
                _uiState.update { it.copy(message = "Vui lòng nhập tên.") }
                return
            }
            normalizedLastName.isEmpty() -> {
                _uiState.update { it.copy(message = "Vui lòng nhập họ.") }
                return
            }
            normalizedEmail.isEmpty() || password.isBlank() -> {
                _uiState.update { it.copy(message = "Vui lòng nhập đầy đủ email và mật khẩu.") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(message = "Mật khẩu phải có ít nhất 6 ký tự.") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, message = "Đang tạo tài khoản...") }

        repository.register(
            normalizedFirstName,
            normalizedLastName,
            normalizedEmail,
            password
        ) { result ->
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isLoggedIn = true, isLoading = false, message = "") },
                    onFailure = { e ->
                        state.copy(
                            isLoading = false,
                            message = e.localizedMessage ?: "Tạo tài khoản thất bại."
                        )
                    }
                )
            }
        }
    }

    fun onLogout() {
        repository.logout()
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(repository = AuthRepositoryProvider.getInstance())
            }
        }
    }
}
