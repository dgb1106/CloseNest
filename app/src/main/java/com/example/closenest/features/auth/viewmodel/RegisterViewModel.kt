package com.example.closenest.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.features.auth.model.AuthRoute
import com.example.closenest.features.auth.model.RegisterUiState
import com.example.closenest.features.auth.model.UserDocument
import com.example.closenest.features.auth.repository.AuthRepository
import com.example.closenest.features.auth.repository.AuthRepositoryProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RegisterViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun updateLastName(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun updateFirstName(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun updateBirthdayText(text: String) {
        val parsed = parseBirthdayText(text)
        val display = if (parsed != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(parsed.toDate())
        } else text
        _uiState.update { it.copy(birthday = parsed, birthdayDisplay = display) }
    }

    private fun parseBirthdayText(text: String): Timestamp? {
        if (text.length != 10) return null
        val parts = text.split("/")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (day !in 1..31 || month !in 1..12 || year < 1900) return null
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return Timestamp(Date(cal.timeInMillis))
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updatePhoneNumber(value: String) {
        _uiState.update { it.copy(phoneNumber = value) }
    }

    fun updateGender(value: String) {
        _uiState.update { it.copy(gender = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun navigateToRegister() {
        _uiState.update { it.copy(authRoute = AuthRoute.Register, errorMessage = "") }
    }

    fun navigateToLogin() {
        _uiState.update { it.copy(authRoute = AuthRoute.Login, errorMessage = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    fun register() {
        val state = _uiState.value

        if (state.lastName.isBlank() || state.firstName.isBlank() ||
            state.email.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Vui lòng điền đầy đủ thông tin") }
            return
        }

        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailPattern.matches(state.email.trim())) {
            _uiState.update { it.copy(errorMessage = "Email không đúng định dạng") }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự") }
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu không khớp") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = "") }

        val userDoc = UserDocument(
            firstName = state.firstName.trim(),
            lastName = state.lastName.trim(),
            birthday = state.birthday,
            email = state.email.trim(),
            phoneNumber = state.phoneNumber.ifBlank { null },
            gender = state.gender.ifBlank { null }
        )

        viewModelScope.launch {
            repository.register(userDoc, state.password)
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.localizedMessage ?: "Tạo tài khoản thất bại."
                            )
                        }
                    }
                )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RegisterViewModel(repository = AuthRepositoryProvider.getInstance())
            }
        }
    }
}
