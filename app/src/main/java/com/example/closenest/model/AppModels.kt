package com.example.closenest.model

import androidx.annotation.StringRes
import com.example.closenest.R

const val DemoEmail = "demo@closenest.app"
const val DemoPassword = "123456"

enum class AuthMode {
    Login,
    Register
}

enum class MainTab(
    val route: String,
    @param:StringRes val labelRes: Int
) {
    Map("map", R.string.tab_map),
    Relationships("relationships", R.string.tab_relationships),
    Add("add", R.string.tab_add),
    Notifications("notifications", R.string.tab_notifications),
    Profile("profile", R.string.tab_profile);

    companion object {
        fun fromRoute(route: String?): MainTab? = entries.firstOrNull { tab ->
            route == tab.route
        }
    }
}

data class MapMarker(
    val label: String,
    val x: Float,
    val y: Float,
    val highlighted: Boolean
)

val DemoMarkers = listOf(
    MapMarker("A", 0.16f, 0.24f, false),
    MapMarker("B", 0.48f, 0.14f, false),
    MapMarker("C", 0.85f, 0.24f, false),
    MapMarker("D", 0.12f, 0.54f, false),
    MapMarker("E", 0.32f, 0.68f, false),
    MapMarker("F", 0.73f, 0.57f, false),
    MapMarker("JT", 0.51f, 0.43f, true)
)
