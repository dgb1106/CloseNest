package com.example.closenest.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.features.auth.model.AuthUiState
import com.example.closenest.features.auth.repository.AuthRepository
import com.example.closenest.features.auth.repository.AuthRepositoryProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = repository.isLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.authState.collect { isLoggedIn ->
                if (!isLoggedIn) {
                    _uiState.update {
                        it.copy(isLoggedIn = false, message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                    }
                }
            }
        }
    }

    fun onLogin(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isEmpty() || password.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập đầy đủ email và mật khẩu.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, message = "") }

        viewModelScope.launch {
            repository.login(normalizedEmail, password)
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoggedIn = true, isLoading = false, message = "") }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = e.localizedMessage ?: "Đăng nhập thất bại."
                            )
                        }
                    }
                )
        }
    }

    fun onGoogleLoginClick() {
        viewModelScope.launch {
            _toastEvent.emit("Tính năng sắp ra mắt")
        }
    }

    fun onLogout() {
        repository.logout()
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = "") }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(repository = AuthRepositoryProvider.getInstance())
            }
        }
    }
}
