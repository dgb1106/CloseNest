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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _showLogoutDialog = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = repository.observeProfileUiState()
        .stateIn(
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
                // TODO: Navigate to account settings
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
