package com.example.closenest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.closenest.model.AuthMode
import com.example.closenest.model.DemoEmail
import com.example.closenest.model.DemoPassword
import com.example.closenest.model.MainTab
import com.example.closenest.ui.components.HomeBottomBar
import com.example.closenest.ui.screens.AuthScreen
import com.example.closenest.ui.screens.HomeMapScreen

@Composable
fun CloseNestApp() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var authMode by rememberSaveable { mutableStateOf(AuthMode.Login) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Map) }
    var message by rememberSaveable { mutableStateOf("Dùng demo@closenest.app / 123456") }

    if (!isLoggedIn) {
        AuthScreen(
            mode = authMode,
            message = message,
            onModeChange = { authMode = it },
            onLogin = { email, password ->
                val isValid = email.trim().equals(DemoEmail, ignoreCase = true) &&
                    password == DemoPassword

                if (isValid) {
                    isLoggedIn = true
                    message = ""
                } else {
                    message = "Sai thông tin. Tài khoản mẫu: $DemoEmail / $DemoPassword"
                }
            },
            onUseDemoAccount = {
                authMode = AuthMode.Login
                message = "Tài khoản mẫu đã sẵn sàng để đăng nhập"
            }
        )
        return
    }

    Scaffold(
        containerColor = Color(0xFFF6F1EA),
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Map -> HomeMapScreen(
                modifier = Modifier.padding(innerPadding)
            )

            MainTab.Relationships -> BlankSection(
                title = "Kết nối",
                modifier = Modifier.padding(innerPadding)
            )

            MainTab.Notifications -> BlankSection(
                title = "Thông báo",
                modifier = Modifier.padding(innerPadding)
            )

            MainTab.Profile -> BlankSection(
                title = "Hồ sơ",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun BlankSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F1EA))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFAAA096),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
