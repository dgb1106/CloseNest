package com.example.closenest.core.model

import androidx.annotation.StringRes
import com.example.closenest.R

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
