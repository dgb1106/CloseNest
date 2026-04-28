package com.example.closenest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.model.AuthMode
import com.example.closenest.model.DemoEmail
import com.example.closenest.model.DemoPassword

@Composable
fun AuthScreen(
    mode: AuthMode,
    message: String,
    onModeChange: (AuthMode) -> Unit,
    onLogin: (String, String) -> Unit,
    onUseDemoAccount: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(DemoEmail) }
    var password by rememberSaveable { mutableStateOf(DemoPassword) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CloseNest",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onBackground
        )

        Text(
            text = "Bản thử giao diện di động với đăng nhập, đăng ký mẫu và trang chủ bản đồ.",
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthModeCard(
                title = "Đăng nhập",
                selected = mode == AuthMode.Login,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(AuthMode.Login) }
            )
            AuthModeCard(
                title = "Đăng ký",
                selected = mode == AuthMode.Register,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(AuthMode.Register) }
            )
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        label = { Text("Họ và tên") },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Email") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Mật khẩu") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onLogin(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (mode == AuthMode.Login) "Vào ứng dụng" else "Tạo tài khoản mẫu")
                }

                OutlinedButton(
                    onClick = {
                        email = DemoEmail
                        password = DemoPassword
                        onUseDemoAccount()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Dùng tài khoản mẫu")
                }

                Text(
                    text = "Tài khoản mẫu: $DemoEmail / $DemoPassword",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun AuthModeCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = if (selected) colorScheme.primary else colorScheme.surfaceContainerLow
    val content = if (selected) colorScheme.onPrimary else colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}
