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
                        it.copy(isLoggedIn = false, isLoading = false)
                    }
                }
            }
        }
    }

    fun onLogin(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isEmpty() || password.isBlank()) {
            _uiState.update {
                it.copy(message = "Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 email v\u00e0 m\u1eadt kh\u1ea9u.")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, message = "") }

        viewModelScope.launch {
            repository.login(normalizedEmail, password)
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoggedIn = true, isLoading = false, message = "") }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = "Sai th\u00f4ng tin \u0111\u0103ng nh\u1eadp. Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i email v\u00e0 m\u1eadt kh\u1ea9u."
                            )
                        }
                    }
                )
        }
    }

    fun onGoogleLoginClick() {
        viewModelScope.launch {
            _toastEvent.emit("T\u00ednh n\u0103ng s\u1eafp ra m\u1eaft")
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, message = "") }

        viewModelScope.launch {
            repository.loginWithGoogle(idToken)
                .fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(isLoggedIn = true, isLoading = false, message = "")
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = "\u0110\u0103ng nh\u1eadp v\u1edbi Google th\u1ea5t b\u1ea1i."
                            )
                        }
                    }
                )
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
