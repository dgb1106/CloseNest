package com.example.closenest.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.repository.ProfileRepository
import com.example.closenest.features.profile.repository.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog

    // Account detail state management (editable fields)
    private data class AccountEditState(
        val showAccountDetail: Boolean = false,
        val isEditingAccount: Boolean = false,
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phone: String = "",
        val birthdayIso: String = "",
        val gender: String = ""
    )

    private val _accountEditState = MutableStateFlow(AccountEditState())

    // Combine repository profile state with account edit state
    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeProfileUiState(),
        _accountEditState
    ) { profileState, accountEditState ->
        profileState.copy(
            showAccountDetail = accountEditState.showAccountDetail,
            isEditingAccount = accountEditState.isEditingAccount,
            accountEditFirstName = accountEditState.firstName,
            accountEditLastName = accountEditState.lastName,
            accountEditEmail = accountEditState.email,
            accountEditPhone = accountEditState.phone,
            accountEditBirthdayIso = accountEditState.birthdayIso,
            accountEditGender = accountEditState.gender
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun onMenuItemClicked(itemId: String) {
        when (itemId) {
            "logout" -> {
                _showLogoutDialog.update { true }
            }
            "account" -> {
                onShowAccountDetail()
            }
            "settings" -> {
                // TODO: Navigate to settings
            }
            "language" -> {
                // TODO: Navigate to language selection
            }
            "ui_customization" -> {
                // TODO: Navigate to UI customization
            }
        }
    }

    // Account detail screen methods
    fun onShowAccountDetail() {
        val currentUser = uiState.value.user
        _accountEditState.update {
            AccountEditState(
                showAccountDetail = true,
                isEditingAccount = false,
                firstName = currentUser?.firstName ?: "",
                lastName = currentUser?.lastName ?: "",
                email = currentUser?.email ?: "",
                phone = currentUser?.phoneNumber ?: "",
                birthdayIso = "",
                gender = currentUser?.gender ?: ""
            )
        }
    }

    fun toggleAccountEditMode() {
        _accountEditState.update { it.copy(isEditingAccount = !it.isEditingAccount) }
    }

    fun updateAccountFirstName(value: String) {
        _accountEditState.update { it.copy(firstName = value) }
    }

    fun updateAccountLastName(value: String) {
        _accountEditState.update { it.copy(lastName = value) }
    }

    fun updateAccountEmail(value: String) {
        _accountEditState.update { it.copy(email = value) }
    }

    fun updateAccountPhone(value: String) {
        _accountEditState.update { it.copy(phone = value) }
    }

    fun updateAccountBirthday(value: String) {
        _accountEditState.update { it.copy(birthdayIso = value) }
    }

    fun updateAccountGender(value: String) {
        _accountEditState.update { it.copy(gender = value) }
    }

    fun saveAccountChanges() {
        viewModelScope.launch {
            val currentUser = uiState.value.user
            val editState = _accountEditState.value

            if (currentUser != null) {
                val updatedUser = currentUser.copy(
                    firstName = editState.firstName,
                    lastName = editState.lastName,
                    email = editState.email,
                    phoneNumber = editState.phone,
                    gender = editState.gender
                    // Note: birthday date picker to be implemented in phase 2
                )

                repository.updateProfile(updatedUser)
                _accountEditState.update { it.copy(isEditingAccount = false) }
            }
        }
    }

    fun cancelAccountEdit() {
        onShowAccountDetail()  // Reload from current user, discarding edits
    }

    fun onBackFromAccountDetail() {
        _accountEditState.update { AccountEditState() }
    }

    fun onConfirmLogout() {
        viewModelScope.launch {
            repository.logout()
            // TODO: Navigate to AuthScreen
        }
    }

    fun onCancelLogout() {
        _showLogoutDialog.update { false }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    repository = ProfileRepositoryProvider.getInstance()
                )
            }
        }
    }
}
