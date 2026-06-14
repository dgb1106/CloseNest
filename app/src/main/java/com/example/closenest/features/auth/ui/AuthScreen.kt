package com.example.closenest.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.CloseNestPrimary
import com.example.closenest.core.ui.theme.CloseNestSurface
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    message: String,
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onDismissMessage: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleLoginClick: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var googleLoginLoading by rememberSaveable { mutableStateOf(false) }

    val registerAnnotatedString = buildRegisterAnnotatedString()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = onDismissMessage,
            confirmButton = {
                TextButton(onClick = onDismissMessage) {
                    Text(text = "OK")
                }
            },
            title = {
                Text(text = "Th\u00f4ng b\u00e1o")
            },
            text = {
                Text(text = message)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(id = R.drawable.closenest_logo),
            contentDescription = "CloseNest Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.auth_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = CloseNestPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.auth_login_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ClickableText(
            text = registerAnnotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                registerAnnotatedString.getStringAnnotations("register", offset, offset)
                    .firstOrNull()
                    ?.let { onNavigateToRegister() }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = stringResource(R.string.auth_divider_text),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    googleLoginLoading = true
                    val result = getGoogleIdToken(context)
                    googleLoginLoading = false
                    result.fold(
                        onSuccess = { idToken ->
                            onGoogleLoginClick(idToken)
                        },
                        onFailure = { e ->
                            // Error is shown via message in UI from ViewModel
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && !googleLoginLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CloseNestSurface
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (googleLoginLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.google_icon),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auth_google_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun buildRegisterAnnotatedString() = buildAnnotatedString {
    val noAccount = stringResource(R.string.auth_no_account)
    val registerLink = stringResource(R.string.auth_register_link)

    append(noAccount)
    append(" ")

    withStyle(
        SpanStyle(
            color = CloseNestPrimary,
            fontWeight = FontWeight.Bold
        )
    ) {
        append(registerLink)
    }
    addStringAnnotation(
        tag = "register",
        annotation = "register",
        start = noAccount.length + 1,
        end = noAccount.length + 1 + registerLink.length
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenPreview() {
    AppTheme {
        AuthScreen(
            message = "",
            isLoading = false,
            onLogin = { _, _ -> },
            onDismissMessage = {},
            onNavigateToRegister = {},
            onGoogleLoginClick = { _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenLoadingPreview() {
    AppTheme {
        AuthScreen(
            message = "",
            isLoading = true,
            onLogin = { _, _ -> },
            onDismissMessage = {},
            onNavigateToRegister = {},
            onGoogleLoginClick = { _ -> }
        )
    }
}

