package com.example.closenest.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.closenest.features.chatbot.ui.FloatingChatbotOverlay
import com.example.closenest.features.homepage.ui.AddHubRoute
import com.example.closenest.features.homepage.ui.AppointmentRoute
import com.example.closenest.features.homepage.ui.HomeMapScreen
import com.example.closenest.features.homepage.ui.MemoryRoute
import com.example.closenest.features.homepage.ui.ReflectionRoute
import com.example.closenest.features.homepage.ui.SectionPlaceholderScreen
import com.example.closenest.features.homepage.viewmodel.AddHubViewModel
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.ui.NotificationsRoute
import com.example.closenest.features.profile.ui.ProfileRoute
import com.example.closenest.features.relationships.ui.AddRelationshipRoute
import com.example.closenest.features.relationships.ui.RelationshipDetailRoute
import com.example.closenest.features.relationships.ui.RelationshipsRoute

private const val AddRelationshipRouteName = "add_relationship"
private const val EditRelationshipRouteName = "edit_relationship"
private const val EditRelationshipIdArg = "editRelationshipId"
private const val EditRelationshipRoutePattern = "$EditRelationshipRouteName/{$EditRelationshipIdArg}"
private const val AddReflectionRouteName = "add_reflection"
private const val AddMemoryRouteName = "add_memory"
private const val AddAppointmentRouteName = "add_appointment"
private const val RelationshipDetailRouteName = "relationship_detail"
private const val RelationshipIdArgument = "relationshipId"
private const val RelationshipDetailRoutePattern = "$RelationshipDetailRouteName/{$RelationshipIdArgument}"

@Composable
fun AppNavigation(
    onLogout: () -> Unit,
    openAppointmentId: String? = null,
    onAppointmentOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    var pendingOpenAppointmentId by remember { mutableStateOf(openAppointmentId) }

    LaunchedEffect(openAppointmentId) {
        pendingOpenAppointmentId = openAppointmentId
        if (openAppointmentId != null) {
            navController.navigate(MainTab.Map.route) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute) ?: MainTab.Map
    val showBottomBar = currentRoute != AddRelationshipRouteName &&
        currentRoute != AddReflectionRouteName &&
        currentRoute != AddMemoryRouteName &&
        currentRoute != AddAppointmentRouteName &&
        currentRoute != RelationshipDetailRoutePattern &&
        currentRoute != EditRelationshipRoutePattern
    val navigateToAddRelationship = {
        navController.navigate(AddRelationshipRouteName) {
            launchSingleTop = true
        }
    }
    val navigateToReflection = {
        navController.navigate(AddReflectionRouteName) {
            launchSingleTop = true
        }
    }
    val navigateToMemory = {
        navController.navigate(AddMemoryRouteName) {
            launchSingleTop = true
        }
    }
    val navigateToAppointment = {
        navController.navigate(AddAppointmentRouteName) {
            launchSingleTop = true
        }
    }
    val navigateToRelationshipDetail: (String) -> Unit = { relationshipId ->
        navController.navigate("$RelationshipDetailRouteName/${Uri.encode(relationshipId)}") {
            launchSingleTop = true
        }
    }
    val navigateToEditRelationship: (String) -> Unit = { relationshipId ->
        navController.navigate("$EditRelationshipRouteName/${Uri.encode(relationshipId)}") {
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    HomeMapScreen(
                        openAppointmentId = pendingOpenAppointmentId,
                        onAppointmentOpened = {
                            pendingOpenAppointmentId = null
                            onAppointmentOpened()
                        },
                        modifier = Modifier
                    )
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
                        onOpenReflection = navigateToReflection,
                        onOpenMemory = navigateToMemory,
                        onOpenAppointment = navigateToAppointment,
                        modifier = Modifier
                    )
                }
                composable(MainTab.Notifications.route) {
                    NotificationsRoute(
                        onNotificationAction = { notification ->
                            when (notification.actionType) {
                                NotificationActionType.VIEW_APPOINTMENT -> {
                                    pendingOpenAppointmentId = notification.sourceEntityId
                                    navController.navigate(MainTab.Map.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                else -> {
                                    notification.relationshipId?.let { relationshipId ->
                                        navigateToRelationshipDetail(relationshipId)
                                    }
                                }
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
                    route = EditRelationshipRoutePattern,
                    arguments = listOf(
                        navArgument(EditRelationshipIdArg) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    AddRelationshipRoute(
                        editRelationshipId = backStackEntry.arguments
                            ?.getString(EditRelationshipIdArg)
                            .orEmpty(),
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier
                    )
                }
                composable(AddReflectionRouteName) { backStackEntry ->
                    val addBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(MainTab.Add.route)
                    }

                    ReflectionRoute(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier,
                        viewModel = viewModel(
                            viewModelStoreOwner = addBackStackEntry,
                            factory = AddHubViewModel.Factory
                        )
                    )
                }
                composable(AddMemoryRouteName) { backStackEntry ->
                    val addBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(MainTab.Add.route)
                    }

                    MemoryRoute(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier,
                        viewModel = viewModel(
                            viewModelStoreOwner = addBackStackEntry,
                            factory = AddHubViewModel.Factory
                        )
                    )
                }
                composable(AddAppointmentRouteName) { backStackEntry ->
                    val addBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(MainTab.Add.route)
                    }

                    AppointmentRoute(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier,
                        viewModel = viewModel(
                            viewModelStoreOwner = addBackStackEntry,
                            factory = AddHubViewModel.Factory
                        )
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
                        onEditRelationship = navigateToEditRelationship,
                        modifier = Modifier
                    )
                }
            }
        }

        FloatingChatbotOverlay(modifier = Modifier.fillMaxSize())
    }
}
