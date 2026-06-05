package com.example.closenest.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.profile.model.ProfileMenuItem
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import com.example.closenest.features.profile.viewmodel.ProfileViewModel
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.homepage.model.MoodDayEntry
import com.example.closenest.features.homepage.viewmodel.ReflectionMood
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    onMenuItemClicked: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsStateWithLifecycle()

    if (uiState.showAccountDetail) {
        AccountDetailRoute(
            onNavigateBack = viewModel::onBackFromAccountDetail,
            modifier = modifier,
            viewModel = viewModel
        )
    } else {
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
            MoodHeatmap(
                moodEntries = uiState.moodMap,
                createdAt = uiState.user?.createdAt
            )
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
                color = colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!user?.phoneNumber.isNullOrEmpty()) {
                Text(
                    text = user?.phoneNumber ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        StreakBadgeSection(
            streakCount = user?.streakCount ?: 0
        )
    }
}

@Composable
fun StreakBadgeSection(
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .background(
                color = Color(0xFFFFF8F6),
                shape = RoundedCornerShape(28.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.streak_flame),
                contentDescription = stringResource(R.string.profile_streak_label),
                modifier = Modifier.size(60.dp),
                tint = Color.Unspecified
            )

            Text(
                text = streakCount.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    drawStyle = Stroke(
                        width = 8f,
                        join = StrokeJoin.Round
                    )
                ),
                color = colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            Text(
                text = streakCount.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Text(
            text = stringResource(R.string.profile_streak_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colorScheme.primary
        )

    }
}

@Composable
fun MoodHeatmap(
    moodEntries: List<MoodDayEntry>,
    createdAt: Timestamp?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cols = 15
    val rows = 4
    val totalCells = cols * rows

    val tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = tz
    }

    fun Calendar.startOfDay(): Calendar {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        return this
    }

    val today = Calendar.getInstance(tz).startOfDay()
    val windowStart = (today.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -totalCells + 1)
    }
    val startCal = createdAt?.toDate()?.let { date ->
        Calendar.getInstance(tz).apply {
            time = date
            startOfDay()
        }
    } ?: today
    if (startCal.after(windowStart)) {
        windowStart.timeInMillis = startCal.timeInMillis
    }

    val dayKeys = List(totalCells) { i ->
        val cloned = windowStart.clone() as Calendar
        cloned.add(Calendar.DAY_OF_YEAR, i)
        if (!cloned.after(today)) dateFormatter.format(cloned.time) else null
    }

    val moodByDay = moodEntries.associateBy { it.dateKey }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_mood_heatmap_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            val dateKey = dayKeys[idx]
                            val entry = if (dateKey != null) moodByDay[dateKey] else null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(moodColor(entry?.mood, colorScheme))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun moodColor(mood: String?, colorScheme: androidx.compose.material3.ColorScheme): Color {
    if (mood == null) return colorScheme.secondary.copy(alpha = 0.3f)

    val moodEnum = ReflectionMood.fromStorageValue(mood)
    return when (moodEnum) {
        ReflectionMood.VeryUnpleasant -> colorScheme.onPrimaryContainer
        ReflectionMood.Unpleasant -> colorScheme.onSurfaceVariant
        ReflectionMood.Neutral -> colorScheme.primaryContainer
        ReflectionMood.Pleasant -> colorScheme.primary.copy(alpha = 0.45f)
        ReflectionMood.VeryPleasant -> colorScheme.primary
        null -> colorScheme.secondary.copy(alpha = 0.12f)
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

// ========================== ACCOUNT DETAIL SCREEN ==========================

@Composable
fun AccountDetailRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleEditMode = viewModel::toggleAccountEditMode,
        onFirstNameChanged = viewModel::updateAccountFirstName,
        onLastNameChanged = viewModel::updateAccountLastName,
        onEmailChanged = viewModel::updateAccountEmail,
        onPhoneChanged = viewModel::updateAccountPhone,
        onBirthdayChanged = viewModel::updateAccountBirthday,
        onGenderChanged = viewModel::updateAccountGender,
        onSaveChanges = viewModel::saveAccountChanges,
        onCancelEdit = viewModel::cancelAccountEdit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailScreen(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit,
    onToggleEditMode: () -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onBirthdayChanged: (String) -> Unit,
    onGenderChanged: (String) -> Unit,
    onSaveChanges: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_account_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AccountDetailHeader(user = uiState.user)
            }

            item {
                AccountPersonalInfoSection(
                    isEditing = uiState.isEditingAccount,
                    firstName = uiState.accountEditFirstName,
                    lastName = uiState.accountEditLastName,
                    gender = uiState.accountEditGender,
                    onFirstNameChanged = onFirstNameChanged,
                    onLastNameChanged = onLastNameChanged,
                    onGenderChanged = onGenderChanged
                )
            }

            item {
                AccountContactInfoSection(
                    isEditing = uiState.isEditingAccount,
                    email = uiState.accountEditEmail,
                    phone = uiState.accountEditPhone,
                    birthday = uiState.accountEditBirthdayIso,
                    onEmailChanged = onEmailChanged,
                    onPhoneChanged = onPhoneChanged,
                    onBirthdayChanged = onBirthdayChanged
                )
            }

            item {
                AccountProfileInfoSection(user = uiState.user)
            }

            item {
                AccountActionSection(
                    isEditing = uiState.isEditingAccount,
                    onToggleEditMode = onToggleEditMode,
                    onSaveChanges = onSaveChanges,
                    onCancelEdit = onCancelEdit
                )
            }
        }
    }
}

@Composable
private fun AccountDetailHeader(
    user: UserProfile?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.primaryContainer
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user?.firstName?.take(1) ?: "?") + (user?.lastName?.take(1) ?: "?"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = user?.name ?: stringResource(R.string.profile_unknown_user),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onPrimaryContainer
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.84f)
                )
            }
        }
    }
}

@Composable
private fun AccountPersonalInfoSection(
    isEditing: Boolean,
    firstName: String,
    lastName: String,
    gender: String,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onGenderChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AccountDetailSection(
        title = stringResource(R.string.profile_account_personal_info_title),
        modifier = modifier
    ) {
        AccountDetailField(
            label = stringResource(R.string.profile_account_first_name_label),
            value = firstName,
            onValueChanged = onFirstNameChanged,
            isEditing = isEditing
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccountDetailField(
            label = stringResource(R.string.profile_account_last_name_label),
            value = lastName,
            onValueChanged = onLastNameChanged,
            isEditing = isEditing
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccountDetailField(
            label = stringResource(R.string.profile_account_gender_label),
            value = gender,
            onValueChanged = onGenderChanged,
            isEditing = isEditing
        )
    }
}

@Composable
private fun AccountContactInfoSection(
    isEditing: Boolean,
    email: String,
    phone: String,
    birthday: String,
    onEmailChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onBirthdayChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AccountDetailSection(
        title = stringResource(R.string.profile_account_contact_info_title),
        modifier = modifier
    ) {
        AccountDetailField(
            label = stringResource(R.string.profile_account_email_label),
            value = email,
            onValueChanged = onEmailChanged,
            isEditing = isEditing,
            icon = Icons.Outlined.Email
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccountDetailField(
            label = stringResource(R.string.profile_account_phone_label),
            value = phone,
            onValueChanged = onPhoneChanged,
            isEditing = isEditing,
            icon = Icons.Outlined.Phone
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccountDetailField(
            label = stringResource(R.string.profile_account_birthday_label),
            value = birthday,
            onValueChanged = onBirthdayChanged,
            isEditing = isEditing,
            icon = Icons.Outlined.Cake
        )
    }
}

@Composable
private fun AccountProfileInfoSection(
    user: UserProfile?,
    modifier: Modifier = Modifier
) {
    AccountDetailSection(
        title = stringResource(R.string.profile_account_profile_info_title),
        modifier = modifier
    ) {
        AccountDetailReadOnlyRow(
            label = stringResource(R.string.profile_account_created_date_label),
            value = user?.createdAt?.toDate()?.toString()?.take(10) ?: "-"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccountDetailReadOnlyRow(
            label = stringResource(R.string.profile_account_streak_label),
            value = (user?.streakCount ?: 0).toString()
        )
    }
}

@Composable
private fun AccountActionSection(
    isEditing: Boolean,
    onToggleEditMode: () -> Unit,
    onSaveChanges: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isEditing) {
            Button(
                onClick = onToggleEditMode,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.profile_account_edit_button))
            }
        } else {
            TextButton(
                onClick = onCancelEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.profile_account_cancel_button))
            }

            Button(
                onClick = onSaveChanges,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.profile_account_save_button))
            }
        }
    }
}

@Composable
private fun AccountDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun AccountDetailField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    isEditing: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    if (isEditing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            label = { Text(label) },
            modifier = modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value.ifBlank { stringResource(R.string.profile_account_empty_value) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AccountDetailReadOnlyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailScreenPreview() {
    AppTheme {
        AccountDetailScreen(
            uiState = ProfileUiState(
                isLoading = false,
                user = UserProfile(
                    uid = "user_1",
                    firstName = "Starry",
                    lastName = "Skies",
                    email = "starry@example.com",
                    phoneNumber = "+84 9 1234 5678",
                    gender = "Female",
                    createdAt = Timestamp.now(),
                    lastCheckedIn = Timestamp.now(),
                    streakCount = 1
                ),
                showAccountDetail = true,
                isEditingAccount = false,
                accountEditFirstName = "Starry",
                accountEditLastName = "Skies",
                accountEditEmail = "starry@example.com",
                accountEditPhone = "+84 9 1234 5678",
                accountEditGender = "Female"
            ),
            onNavigateBack = {},
            onToggleEditMode = {},
            onFirstNameChanged = {},
            onLastNameChanged = {},
            onEmailChanged = {},
            onPhoneChanged = {},
            onBirthdayChanged = {},
            onGenderChanged = {},
            onSaveChanges = {},
            onCancelEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                user = UserProfile(
                    uid = "user_1",
                    firstName = "Starry",
                    lastName = "Skies",
                    email = "starry@example.com",
                    phoneNumber = "+84 9 1234 5678",
                    createdAt = Timestamp.now(),
                    lastCheckedIn = Timestamp.now(),
                    streakCount = 1
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
