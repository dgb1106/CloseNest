package com.example.closenest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.features.auth.ui.AuthScreen
import com.example.closenest.features.auth.viewmodel.AuthViewModel
import com.example.closenest.navigation.AppNavigation

@Composable
fun CloseNestApp() {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    if (!authUiState.isLoggedIn) {
        AuthScreen(
            mode = authUiState.authMode,
            message = authUiState.message,
            isLoading = authUiState.isLoading,
            onModeChange = authViewModel::onModeChange,
            onLogin = authViewModel::onLogin,
            onRegister = authViewModel::onRegister
        )
        return
    }

    AppNavigation(onLogout = authViewModel::onLogout)
}
