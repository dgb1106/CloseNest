package com.example.closenest.features.profile.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.closenest.R
import com.example.closenest.features.homepage.model.MoodDayEntry
import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthday: Timestamp? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val gender: String? = null,
    val createdAt: Timestamp? = null,
    val lastCheckedIn: Timestamp? = null,
    val streakCount: Int = 0
) {
    val name: String
        get() = "$firstName $lastName".trim()
}

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
    val errorMessage: String? = null,
    val showLogoutDialog: Boolean = false,
    // Account detail screen state
    val showAccountDetail: Boolean = false,
    val isEditingAccount: Boolean = false,
    val accountEditFirstName: String = "",
    val accountEditLastName: String = "",
    val accountEditEmail: String = "",
    val accountEditPhone: String = "",
    val accountEditBirthdayIso: String = "",
    val accountEditGender: String = "",
    val moodMap: List<MoodDayEntry> = emptyList()
)
