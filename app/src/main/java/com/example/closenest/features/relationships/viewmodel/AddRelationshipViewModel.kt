package com.example.closenest.features.relationships.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRelationshipUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val selectedTag: RelationshipTag = RelationshipTag.Friend,
    val phoneNumber: String = "",
    val email: String = "",
    val birthday: String = "",
    val interests: String = "",
    val notes: String = "",
    val priority: RelationshipPriority = RelationshipPriority.Medium,
    val showMoreDetails: Boolean = false,
    val isSubmitting: Boolean = false,
    val nameError: Boolean = false,
    val emailError: Boolean = false,
    val birthdayError: Boolean = false,
    val errorMessageRes: Int? = null,
    val isSaved: Boolean = false
)

class AddRelationshipViewModel(
    private val repository: RelationshipRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddRelationshipUiState())
    val uiState: StateFlow<AddRelationshipUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                name = value,
                nameError = false,
                errorMessageRes = null
            )
        }
    }

    fun onTagChanged(value: RelationshipTag) {
        _uiState.update { it.copy(selectedTag = value) }
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = false,
                errorMessageRes = null
            )
        }
    }

    fun onBirthdayChanged(value: String) {
        _uiState.update {
            it.copy(
                birthday = value,
                birthdayError = false,
                errorMessageRes = null
            )
        }
    }

    fun onInterestsChanged(value: String) {
        _uiState.update { it.copy(interests = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onPriorityChanged(value: RelationshipPriority) {
        _uiState.update { it.copy(priority = value) }
    }

    fun onToggleMoreDetails() {
        _uiState.update { it.copy(showMoreDetails = !it.showMoreDetails) }
    }

    fun saveRelationship() {
        val currentState = _uiState.value
        val trimmedName = currentState.name.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedBirthday = currentState.birthday.trim()
        val isNameInvalid = trimmedName.isBlank()
        val isEmailInvalid = trimmedEmail.isNotEmpty() && !emailRegex.matches(trimmedEmail)
        val birthdayIso = parseBirthdayToIso(trimmedBirthday)
        val isBirthdayInvalid = trimmedBirthday.isNotEmpty() && birthdayIso == null

        if (isNameInvalid || isEmailInvalid || isBirthdayInvalid) {
            _uiState.update {
                it.copy(
                    nameError = isNameInvalid,
                    emailError = isEmailInvalid,
                    birthdayError = isBirthdayInvalid
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null) }
            runCatching {
                repository.addRelationship(
                    NewRelationshipRequest(
                        name = trimmedName,
                        tag = currentState.selectedTag,
                        birthdayIso = birthdayIso,
                        phoneNumber = currentState.phoneNumber.trim().ifBlank { null },
                        email = trimmedEmail.ifBlank { null },
                        interests = currentState.interests
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                        notes = currentState.notes.trim().ifBlank { null },
                        priority = currentState.priority
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSubmitting = false, isSaved = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessageRes = R.string.add_relationship_unknown_error
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddRelationshipViewModel(
                    repository = RelationshipRepositoryProvider.repository
                )
            }
        }

        private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

        private fun parseBirthdayToIso(value: String): String? {
            if (value.isBlank()) return null
            if (!birthdayInputRegex.matches(value)) return null

            val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                isLenient = false
            }
            val parsePosition = ParsePosition(0)
            val parsedDate = inputFormatter.parse(value, parsePosition) ?: return null
            if (parsePosition.index != value.length) return null

            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsedDate)
        }

        private val birthdayInputRegex = Regex("^\\d{2}/\\d{2}/\\d{4}$")
    }
}
