package com.example.closenest.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.closenest.R
import com.example.closenest.features.profile.ui.ProfileRoute
import com.example.closenest.features.relationships.ui.AddRelationshipRoute
import com.example.closenest.features.relationships.ui.RelationshipsRoute
import com.example.closenest.model.MainTab
import com.example.closenest.ui.auth.AuthViewModel
import com.example.closenest.ui.components.HomeBottomBar
import com.example.closenest.ui.screens.AddHubScreen
import com.example.closenest.ui.screens.AuthScreen
import com.example.closenest.ui.screens.HomeMapScreen
import com.example.closenest.ui.screens.SectionPlaceholderScreen

private const val AddRelationshipRouteName = "add_relationship"

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
        containerColor = MaterialTheme.colorScheme.background,
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
                ProfileRoute(
                    onLogout = {
                        authViewModel.onLogout()
                        navController.navigate(MainTab.Map.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
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
