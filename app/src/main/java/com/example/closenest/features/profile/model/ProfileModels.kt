package com.example.closenest.features.profile.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.closenest.R
import java.time.Instant

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

enum class ProfileMenuItem(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val description: String
) {
    ACCOUNT(
        id = "account",
        labelRes = R.string.profile_menu_account,
        icon = Icons.Outlined.AccountCircle,
        description = "Account settings"
    ),
    SETTINGS(
        id = "settings",
        labelRes = R.string.profile_menu_settings,
        icon = Icons.Outlined.Settings,
        description = "Settings and privacy"
    ),
    LANGUAGE(
        id = "language",
        labelRes = R.string.profile_menu_language,
        icon = Icons.Outlined.Public,
        description = "Language preferences"
    ),
    UI_CUSTOMIZATION(
        id = "ui_customization",
        labelRes = R.string.profile_menu_ui_customization,
        icon = Icons.Outlined.Palette,
        description = "UI customization"
    ),
    LOGOUT(
        id = "logout",
        labelRes = R.string.profile_menu_logout,
        icon = Icons.AutoMirrored.Outlined.Logout,
        description = "Sign out"
    )
}

data class RelationshipQuickPreview(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val initials: String = ""
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: UserProfile? = null,
    val recentRelationships: List<RelationshipQuickPreview> = emptyList(),
    val streak: Int = 0,
    val errorMessage: String? = null,
    val showLogoutDialog: Boolean = false
)
