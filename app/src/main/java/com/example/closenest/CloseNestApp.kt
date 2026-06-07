package com.example.closenest

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.auth.model.AuthRoute
import com.example.closenest.features.auth.ui.AuthScreen
import com.example.closenest.features.auth.ui.RegisterScreen
import com.example.closenest.features.auth.viewmodel.AuthViewModel
import com.example.closenest.features.auth.viewmodel.RegisterViewModel
import com.example.closenest.navigation.AppNavigation
import kotlinx.coroutines.delay

@Composable
fun CloseNestApp(
    openAppointmentId: String? = null,
    onAppointmentOpened: () -> Unit = {}
) {
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

        AnimatedContent(
            targetState = registerUiState.authRoute,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val isRegistering = targetState == AuthRoute.Register
                val enter = if (isRegistering) {
                    slideInHorizontally { it } + fadeIn()
                } else {
                    slideInHorizontally { -it } + fadeIn()
                }
                val exit = if (isRegistering) {
                    slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideOutHorizontally { it } + fadeOut()
                }
                (enter togetherWith exit).using(SizeTransform(clip = false))
            }
        ) { route ->
            when (route) {
                AuthRoute.Login -> {
                    AuthScreen(
                        message = authUiState.message,
                        isLoading = authUiState.isLoading,
                        onLogin = authViewModel::onLogin,
                        onNavigateToRegister = registerViewModel::navigateToRegister,
                        onGoogleLoginClick = authViewModel::loginWithGoogle
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
        }

        if (registerUiState.isSuccess) {
            LaunchedEffect(registerUiState.isSuccess) {
                delay(3_000)
                registerViewModel.onRegisterSuccessHandled()
            }
            RegisterSuccessDialog(onDismiss = registerViewModel::onRegisterSuccessHandled)
        }
        return
    }

    AppNavigation(
        onLogout = authViewModel::onLogout,
        openAppointmentId = openAppointmentId,
        onAppointmentOpened = onAppointmentOpened
    )
}

@Composable
private fun RegisterSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Đóng"
                        )
                    }
                }
                Text(
                    text = "Đăng ký thành công!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E34),
                    textAlign = TextAlign.Center,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "Chào mừng đến với CloseNest, hãy cùng nuôi dưỡng những mối quan hệ của bạn!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}
