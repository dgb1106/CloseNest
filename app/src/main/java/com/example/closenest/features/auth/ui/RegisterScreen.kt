package com.example.closenest.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.CloseNestPrimary
private val genderOptions = listOf(
    "Nam" to R.string.register_gender_male,
    "Nữ" to R.string.register_gender_female,
    "Khác" to R.string.register_gender_other,
    "Không muốn chia sẻ" to R.string.register_gender_unprovided
)

@Composable
fun RegisterScreen(
    lastName: String,
    firstName: String,
    birthdayDisplay: String,
    email: String,
    phoneNumber: String,
    gender: String,
    password: String,
    confirmPassword: String,
    isLoading: Boolean,
    errorMessage: String,
    onLastNameChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val loginAnnotatedString = buildLoginAnnotatedString()

    var genderDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var genderDropdownSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier

            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(id = R.drawable.closenest_logo),
            contentDescription = "CloseNest Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.register_last_name_label))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.hint_input)) },
                    singleLine = true,
                    enabled = !isLoading
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.register_first_name_label))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.hint_input)) },
                    singleLine = true,
                    enabled = !isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(0.35f)) {
                SectionLabel(text = stringResource(R.string.register_birthday_label))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = birthdayDisplay,
                    onValueChange = {
                        if (it.length <= 10) onBirthdayChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.hint_select_date)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Column(modifier = Modifier.weight(0.65f)) {
                SectionLabel(text = stringResource(R.string.auth_email_label))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.hint_input)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(0.55f)) {
                SectionLabel(text = stringResource(R.string.register_phone_label))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.hint_input)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Column(modifier = Modifier.weight(0.45f)) {
                SectionLabel(text = stringResource(R.string.register_gender_label))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = genderDisplayText(gender),
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { genderDropdownSize = it.size.toSize() },
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.hint_select_from_list)) },
                        readOnly = true,
                        singleLine = true,
                        enabled = !isLoading,
                        trailingIcon = {
                            IconButton(onClick = { genderDropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "Mở danh sách",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = genderDropdownExpanded,
                        onDismissRequest = { genderDropdownExpanded = false },
                        modifier = Modifier.width(with(density) { genderDropdownSize.width.toDp() })
                    ) {
                        genderOptions.forEach { (value, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    onGenderChange(value)
                                    genderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel(text = stringResource(R.string.auth_password_label), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(stringResource(R.string.hint_input)) },
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

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel(text = stringResource(R.string.register_confirm_password_label), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(stringResource(R.string.hint_input)) },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        )

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRegisterClick,
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
                    text = stringResource(R.string.register_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClickableText(
            text = loginAnnotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                loginAnnotatedString.getStringAnnotations("login", offset, offset)
                    .firstOrNull()
                    ?.let { onNavigateToLogin() }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
private fun buildLoginAnnotatedString() = buildAnnotatedString {
    val hasAccount = stringResource(R.string.auth_has_account)
    val loginLink = stringResource(R.string.auth_login_link)

    append(hasAccount)
    append(" ")

    withStyle(
        SpanStyle(
            color = CloseNestPrimary,
            fontWeight = FontWeight.Bold
        )
    ) {
        append(loginLink)
    }
    addStringAnnotation(
        tag = "login",
        annotation = "login",
        start = hasAccount.length + 1,
        end = hasAccount.length + 1 + loginLink.length
    )
}

@Composable
private fun genderDisplayText(gender: String): String {
    return when (gender) {
        "Nam" -> stringResource(R.string.register_gender_male)
        "Nữ" -> stringResource(R.string.register_gender_female)
        "Khác" -> stringResource(R.string.register_gender_other)
        "Không muốn chia sẻ" -> stringResource(R.string.register_gender_unprovided)
        else -> ""
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenEmptyPreview() {
    AppTheme {
        RegisterScreen(
            lastName = "",
            firstName = "",
            birthdayDisplay = "",
            email = "",
            phoneNumber = "",
            gender = "",
            password = "",
            confirmPassword = "",
            isLoading = false,
            errorMessage = "",
            onLastNameChange = {},
            onFirstNameChange = {},
            onBirthdayChange = {},
            onEmailChange = {},
            onPhoneNumberChange = {},
            onGenderChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenFilledPreview() {
    AppTheme {
        RegisterScreen(
            lastName = "Nguyễn",
            firstName = "Văn A",
            birthdayDisplay = "01/01/2000",
            email = "nguyenvana@example.com",
            phoneNumber = "0912345678",
            gender = "Nam",
            password = "123456",
            confirmPassword = "123456",
            isLoading = false,
            errorMessage = "",
            onLastNameChange = {},
            onFirstNameChange = {},
            onBirthdayChange = {},
            onEmailChange = {},
            onPhoneNumberChange = {},
            onGenderChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenLoadingPreview() {
    AppTheme {
        RegisterScreen(
            lastName = "Nguyễn",
            firstName = "Văn A",
            birthdayDisplay = "",
            email = "nguyenvana@example.com",
            phoneNumber = "",
            gender = "Nam",
            password = "123456",
            confirmPassword = "123456",
            isLoading = true,
            errorMessage = "",
            onLastNameChange = {},
            onFirstNameChange = {},
            onBirthdayChange = {},
            onEmailChange = {},
            onPhoneNumberChange = {},
            onGenderChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onNavigateToLogin = {}
        )
    }
}

