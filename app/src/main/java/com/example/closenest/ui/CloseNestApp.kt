package com.example.closenest.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.closenest.R
import com.example.closenest.features.relationships.ui.AddRelationshipRoute
import com.example.closenest.features.relationships.ui.RelationshipsRoute
import com.example.closenest.model.AuthMode
import com.example.closenest.model.DemoEmail
import com.example.closenest.model.DemoPassword
import com.example.closenest.model.MainTab
import com.example.closenest.ui.components.HomeBottomBar
import com.example.closenest.ui.screens.AddHubScreen
import com.example.closenest.ui.screens.AuthScreen
import com.example.closenest.ui.screens.HomeMapScreen
import com.example.closenest.ui.screens.SectionPlaceholderScreen

private const val AddRelationshipRouteName = "add_relationship"

@Composable
fun CloseNestApp() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var authMode by rememberSaveable { mutableStateOf(AuthMode.Login) }
    var message by rememberSaveable {
        mutableStateOf("Dùng demo@closenest.app / 123456")
    }

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

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute) ?: MainTab.Map
    val showBottomBar = currentRoute != AddRelationshipRouteName
    val navigateToAddRelationship = {
        navController.navigate(AddRelationshipRouteName) {
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                HomeBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.Map.route) {
                HomeMapScreen(modifier = Modifier)
            }
            composable(MainTab.Relationships.route) {
                RelationshipsRoute(
                    onAddRelationship = navigateToAddRelationship,
                    modifier = Modifier
                )
            }
            composable(MainTab.Add.route) {
                AddHubScreen(
                    onAddRelationship = navigateToAddRelationship,
                    modifier = Modifier
                )
            }
            composable(MainTab.Notifications.route) {
                SectionPlaceholderScreen(
                    titleRes = R.string.notifications_placeholder_title,
                    descriptionRes = R.string.notifications_placeholder_body,
                    modifier = Modifier
                )
            }
            composable(MainTab.Profile.route) {
                SectionPlaceholderScreen(
                    titleRes = R.string.profile_placeholder_title,
                    descriptionRes = R.string.profile_placeholder_body,
                    modifier = Modifier
                )
            }
            composable(AddRelationshipRouteName) {
                AddRelationshipRoute(
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier
                )
            }
        }
    }
}
