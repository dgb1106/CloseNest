package com.example.closenest.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.profile.model.ProfileMenuItem
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import com.example.closenest.features.profile.viewmodel.ProfileViewModel
import com.example.closenest.core.ui.theme.AppTheme
import java.time.Instant

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    onMenuItemClicked: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        showLogoutDialog = showLogoutDialog,
        onMenuItemClicked = { itemId ->
            when (itemId) {
                "logout" -> viewModel.onMenuItemClicked(itemId)
                else -> {
                    viewModel.onMenuItemClicked(itemId)
                    onMenuItemClicked(itemId)
                }
            }
        },
        onConfirmLogout = {
            viewModel.onConfirmLogout()
            onLogout()
        },
        onCancelLogout = viewModel::onCancelLogout,
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    showLogoutDialog: Boolean,
    onMenuItemClicked: (String) -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 108.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ProfileHeaderSection(user = uiState.user)
        }

        item {
            ProfileMenuSection(
                onMenuItemClicked = onMenuItemClicked
            )
        }

        if (uiState.recentRelationships.isNotEmpty()) {
            item {
                RelationshipsPreviewSection(
                    relationships = uiState.recentRelationships
                )
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = onConfirmLogout,
            onCancel = onCancelLogout
        )
    }
}

@Composable
fun ProfileHeaderSection(
    user: UserProfile?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (user?.avatarUrl != null) {
                // TODO: Load image with coil
                Surface(color = colorScheme.primary) {
                    Text("")
                }
            } else {
                Text(
                    text = user?.name?.take(2)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // User info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = user?.name ?: stringResource(R.string.profile_unknown_user),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.outlineVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!user?.phoneNumber.isNullOrEmpty()) {
                Text(
                    text = user?.phoneNumber ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.outlineVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProfileMenuSection(
    onMenuItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileMenuItem.entries.forEach { menuItem ->
            ProfileMenuItemCard(
                menuItem = menuItem,
                onClick = { onMenuItemClicked(menuItem.id) }
            )
        }
    }
}

@Composable
fun ProfileMenuItemCard(
    menuItem: ProfileMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = menuItem.icon,
                contentDescription = menuItem.description,
                modifier = Modifier.size(24.dp),
                tint = colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(menuItem.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RelationshipsPreviewSection(
    relationships: List<RelationshipQuickPreview>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_relationships_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            relationships.forEach { relationship ->
                RelationshipAvatarBubble(relationship)
            }
        }
    }
}

@Composable
fun RelationshipAvatarBubble(
    relationship: RelationshipQuickPreview,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = relationship.initials,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onTertiary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = relationship.name.take(10),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.profile_logout_confirm_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(R.string.profile_logout_confirm_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.profile_logout_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.profile_logout_cancel_button))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                user = UserProfile(
                    id = "user_1",
                    name = "starryskies23",
                    email = "starry@example.com",
                    phoneNumber = "+84 9 1234 5678",
                    createdAt = Instant.now()
                ),
                recentRelationships = listOf(
                    RelationshipQuickPreview("1", "Bảo Nam", initials = "BN"),
                    RelationshipQuickPreview("2", "Gia Hân", initials = "GH"),
                    RelationshipQuickPreview("3", "Minh Anh", initials = "MA"),
                )
            ),
            showLogoutDialog = false,
            onMenuItemClicked = {},
            onConfirmLogout = {},
            onCancelLogout = {}
        )
    }
}
