package com.example.closenest

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.features.auth.model.AuthRoute
import com.example.closenest.features.auth.ui.AuthScreen
import com.example.closenest.features.auth.ui.RegisterScreen
import com.example.closenest.features.auth.viewmodel.AuthViewModel
import com.example.closenest.features.auth.viewmodel.RegisterViewModel
import com.example.closenest.navigation.AppNavigation

@Composable
fun CloseNestApp() {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (!authUiState.isLoggedIn) {
        val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
        val registerUiState by registerViewModel.uiState.collectAsStateWithLifecycle()

        if (registerUiState.isSuccess) {
            return
        }

        when (registerUiState.authRoute) {
            AuthRoute.Login -> {
                AuthScreen(
                    message = authUiState.message,
                    isLoading = authUiState.isLoading,
                    onLogin = authViewModel::onLogin,
                    onNavigateToRegister = registerViewModel::navigateToRegister,
                    onGoogleLoginClick = authViewModel::onGoogleLoginClick
                )
            }
            AuthRoute.Register -> {
                RegisterScreen(
                    lastName = registerUiState.lastName,
                    firstName = registerUiState.firstName,
                    birthdayDisplay = registerUiState.birthdayDisplay,
                    email = registerUiState.email,
                    phoneNumber = registerUiState.phoneNumber,
                    gender = registerUiState.gender,
                    password = registerUiState.password,
                    confirmPassword = registerUiState.confirmPassword,
                    isLoading = registerUiState.isLoading,
                    errorMessage = registerUiState.errorMessage,
                    onLastNameChange = registerViewModel::updateLastName,
                    onFirstNameChange = registerViewModel::updateFirstName,
                    onBirthdayChange = registerViewModel::updateBirthdayText,
                    onEmailChange = registerViewModel::updateEmail,
                    onPhoneNumberChange = registerViewModel::updatePhoneNumber,
                    onGenderChange = registerViewModel::updateGender,
                    onPasswordChange = registerViewModel::updatePassword,
                    onConfirmPasswordChange = registerViewModel::updateConfirmPassword,
                    onRegisterClick = registerViewModel::register,
                    onNavigateToLogin = registerViewModel::navigateToLogin
                )
            }
        }
        return
    }

    AppNavigation(onLogout = authViewModel::onLogout)
}
