package com.example.closenest.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.closenest.R
import com.example.closenest.core.model.MainTab
import com.example.closenest.core.ui.components.HomeBottomBar
import com.example.closenest.features.homepage.ui.AddHubRoute
import com.example.closenest.features.homepage.ui.HomeMapScreen
import com.example.closenest.features.homepage.ui.SectionPlaceholderScreen
import com.example.closenest.features.notifications.ui.NotificationsRoute
import com.example.closenest.features.profile.ui.ProfileRoute
import com.example.closenest.features.relationships.ui.AddRelationshipRoute
import com.example.closenest.features.relationships.ui.RelationshipDetailRoute
import com.example.closenest.features.relationships.ui.RelationshipsRoute

private const val AddRelationshipRouteName = "add_relationship"
private const val RelationshipDetailRouteName = "relationship_detail"
private const val RelationshipIdArgument = "relationshipId"
private const val RelationshipDetailRoutePattern = "$RelationshipDetailRouteName/{$RelationshipIdArgument}"

@Composable
fun AppNavigation(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute) ?: MainTab.Map
    val showBottomBar = currentRoute != AddRelationshipRouteName &&
        currentRoute != RelationshipDetailRoutePattern
    val navigateToAddRelationship = {
        navController.navigate(AddRelationshipRouteName) {
            launchSingleTop = true
        }
    }
    val navigateToRelationshipDetail: (String) -> Unit = { relationshipId ->
        navController.navigate("$RelationshipDetailRouteName/${Uri.encode(relationshipId)}") {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(MainTab.Map.route) {
                HomeMapScreen(modifier = Modifier)
            }
            composable(MainTab.Relationships.route) {
                RelationshipsRoute(
                    onAddRelationship = navigateToAddRelationship,
                    onRelationshipSelected = navigateToRelationshipDetail,
                    modifier = Modifier
                )
            }
            composable(MainTab.Add.route) {
                AddHubRoute(
                    onAddRelationship = navigateToAddRelationship,
                    modifier = Modifier
                )
            }
            composable(MainTab.Notifications.route) {
                NotificationsRoute(
                    onNotificationAction = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                )
            }
            composable(MainTab.Profile.route) {
                ProfileRoute(
                    onLogout = {
                        onLogout()
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
            composable(
                route = RelationshipDetailRoutePattern,
                arguments = listOf(
                    navArgument(RelationshipIdArgument) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                RelationshipDetailRoute(
                    relationshipId = backStackEntry.arguments
                        ?.getString(RelationshipIdArgument)
                        .orEmpty(),
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier
                )
            }
        }
    }
}
