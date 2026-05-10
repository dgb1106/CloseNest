package com.example.closenest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.model.AuthMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val authMode: AuthMode = AuthMode.Login,
    val message: String = "Nhập email và mật khẩu để đăng nhập.",
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(isLoggedIn = auth.currentUser != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onModeChange(mode: AuthMode) {
        _uiState.update {
            it.copy(
                authMode = mode,
                message = ""
            )
        }
    }

    fun onLogin(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isEmpty() || password.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập đầy đủ email và mật khẩu.") }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                message = "Đang đăng nhập..."
            )
        }

        auth.signInWithEmailAndPassword(normalizedEmail, password)
            .addOnCompleteListener { task ->
                _uiState.update {
                    if (task.isSuccessful) {
                        it.copy(
                            isLoggedIn = true,
                            isLoading = false,
                            message = ""
                        )
                    } else {
                        it.copy(
                            isLoading = false,
                            message = task.exception?.localizedMessage ?: "Đăng nhập thất bại."
                        )
                    }
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

        _uiState.update {
            it.copy(
                isLoading = true,
                message = "Đang tạo tài khoản..."
            )
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = task.exception?.localizedMessage ?: "Tạo tài khoản thất bại."
                        )
                    }
                    return@addOnCompleteListener
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("$normalizedFirstName $normalizedLastName".trim())
                    .build()

                auth.currentUser
                    ?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener {
                        _uiState.update { state ->
                            state.copy(
                                isLoggedIn = true,
                                isLoading = false,
                                message = ""
                            )
                        }
                    }
                    ?: _uiState.update { state ->
                        state.copy(
                            isLoggedIn = true,
                            isLoading = false,
                            message = ""
                        )
                    }
            }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(auth = FirebaseAuth.getInstance())
            }
        }
    }
}
