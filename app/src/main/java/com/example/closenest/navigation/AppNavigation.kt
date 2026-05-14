package com.example.closenest.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.closenest.R
import com.example.closenest.core.model.MainTab
import com.example.closenest.core.ui.components.HomeBottomBar
import com.example.closenest.features.homepage.ui.AddHubScreen
import com.example.closenest.features.homepage.ui.HomeMapScreen
import com.example.closenest.features.homepage.ui.SectionPlaceholderScreen
import com.example.closenest.features.profile.ui.ProfileRoute
import com.example.closenest.features.relationships.ui.AddRelationshipRoute
import com.example.closenest.features.relationships.ui.RelationshipsRoute

private const val AddRelationshipRouteName = "add_relationship"

@Composable
fun AppNavigation(onLogout: () -> Unit) {
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = MainTab.Map.route,
                modifier = Modifier.fillMaxSize()
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
            }

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
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
