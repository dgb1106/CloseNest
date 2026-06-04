@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.closenest.features.relationships.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.model.SelectableRelationshipTags
import com.example.closenest.features.relationships.viewmodel.RelationshipListItem
import com.example.closenest.features.relationships.viewmodel.RelationshipsUiState
import com.example.closenest.features.relationships.viewmodel.RelationshipsViewModel

@Composable
fun RelationshipsRoute(
    onAddRelationship: () -> Unit,
    onRelationshipSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RelationshipsViewModel = viewModel(factory = RelationshipsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RelationshipsScreen(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onTagSelected = viewModel::onTagSelected,
        onResetFilters = viewModel::clearFilters,
        onAddRelationship = onAddRelationship,
        onRelationshipSelected = onRelationshipSelected,
        modifier = modifier
    )
}

@Composable
fun RelationshipDetailRoute(
    relationshipId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RelationshipsViewModel = viewModel(factory = RelationshipsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val relationship = uiState.relationships.firstOrNull { item -> item.id == relationshipId }

    LaunchedEffect(uiState.deletedRelationshipId) {
        if (uiState.deletedRelationshipId == relationshipId) {
            onNavigateBack()
        }
    }

    RelationshipDetailScreen(
        uiState = uiState,
        relationship = relationship,
        onNavigateBack = onNavigateBack,
        onDeleteRelationship = { viewModel.deleteRelationship(relationshipId) },
        modifier = modifier
    )
}

@Composable
fun RelationshipsScreen(
    uiState: RelationshipsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onTagSelected: (RelationshipTag?) -> Unit,
    onResetFilters: () -> Unit,
    onAddRelationship: () -> Unit,
    onRelationshipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(
                totalRelationships = uiState.totalRelationships,
                onAddRelationship = onAddRelationship
            )
        }

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                label = {
                    Text(stringResource(R.string.relationship_search_label))
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectableRelationshipTags.forEach { tag ->
                    FilterChip(
                        label = stringResource(tag.labelRes),
                        selected = uiState.selectedTag == tag,
                        onClick = { onTagSelected(tag) }
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                item {
                    LoadingState()
                }
            }

            uiState.errorMessageRes != null -> {
                item {
                    ErrorState(
                        message = stringResource(uiState.errorMessageRes),
                        onAddRelationship = onAddRelationship
                    )
                }
            }

            uiState.relationships.isEmpty() -> {
                item {
                    EmptyState(
                        hasFilters = uiState.searchQuery.isNotBlank() || uiState.selectedTag != null,
                        onPrimaryAction = if (uiState.totalRelationships == 0) {
                            onAddRelationship
                        } else {
                            onResetFilters
                        }
                    )
                }
            }

            else -> {
                items(
                    items = uiState.relationships,
                    key = { it.id }
                ) { relationship ->
                    RelationshipCard(
                        relationship = relationship,
                        onClick = { onRelationshipSelected(relationship.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationshipDetailScreen(
    uiState: RelationshipsUiState,
    relationship: RelationshipListItem?,
    onNavigateBack: () -> Unit,
    onDeleteRelationship: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.relationship_detail_title)) },
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
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        LoadingState()
                    }
                }

                uiState.errorMessageRes != null -> {
                    item {
                        MessageCard(message = stringResource(uiState.errorMessageRes))
                    }
                }

                relationship == null -> {
                    item {
                        MessageCard(
                            message = stringResource(R.string.relationship_detail_not_found_body),
                            title = stringResource(R.string.relationship_detail_not_found_title)
                        )
                    }
                }

                else -> {
                    item {
                        DetailHeader(relationship = relationship)
                    }

                    item {
                        DetailSection(title = stringResource(R.string.relationship_detail_contact_title)) {
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_phone_label),
                                value = relationship.phoneNumber,
                                icon = Icons.Outlined.Phone,
                                onAction = relationship.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                                    { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$it") }) }
                                }
                            )
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_email_label),
                                value = relationship.email,
                                icon = Icons.Outlined.Email,
                                onAction = relationship.email?.takeIf { it.isNotBlank() }?.let {
                                    { context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:"); putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }) }
                                }
                            )
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_birthday_label),
                                value = relationship.birthdayIso.toDisplayDate(),
                                icon = Icons.Outlined.Cake
                            )
                        }
                    }

                    item {
                        DetailSection(title = stringResource(R.string.relationship_detail_profile_title)) {
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_group_label),
                                value = stringResource(relationship.tag.labelRes)
                            )
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_priority_label),
                                value = stringResource(relationship.priority.labelRes)
                            )
                        }
                    }

                    item {
                        DetailSection(title = stringResource(R.string.relationship_detail_notes_title)) {
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_interests_label),
                                value = relationship.interests.joinToString(", ").ifBlank { null }
                            )
                            DetailRow(
                                label = stringResource(R.string.relationship_detail_notes_label),
                                value = relationship.notes,
                                icon = Icons.AutoMirrored.Outlined.Notes
                            )
                        }
                    }

                    item {
                        DeleteRelationshipSection(
                            isDeleting = uiState.isDeletingRelationship,
                            errorMessageRes = uiState.deleteErrorMessageRes,
                            onDeleteClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && relationship != null) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingRelationship) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text(text = stringResource(R.string.relationship_delete_confirm_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.relationship_delete_confirm_body,
                        relationship.name
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteRelationship()
                    },
                    enabled = !uiState.isDeletingRelationship,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.relationship_delete_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isDeletingRelationship
                ) {
                    Text(text = stringResource(R.string.relationship_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(
    totalRelationships: Int,
    onAddRelationship: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.relationships_title, totalRelationships),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.relationships_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(
            onClick = onAddRelationship,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(R.string.relationship_add_person))
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        colors = if (selected) {
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun RelationshipCard(
    relationship: RelationshipListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(initials = relationship.initials)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = relationship.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = stringResource(
                            R.string.relationship_card_meta,
                            stringResource(relationship.tag.labelRes),
                            stringResource(relationship.priority.labelRes)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ContactInfoRow(
                phoneNumber = relationship.phoneNumber,
                email = relationship.email
            )

            if (relationship.interests.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    relationship.interests.take(3).forEach { interest ->
                        InterestPill(label = interest)
                    }
                }
            }

            relationship.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(relationship: RelationshipListItem) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(initials = relationship.initials, size = 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = relationship.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(relationship.tag.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f)
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
private fun DeleteRelationshipSection(
    isDeleting: Boolean,
    errorMessageRes: Int?,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            errorMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onDeleteClick,
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        if (isDeleting) {
                            R.string.relationship_delete_saving
                        } else {
                            R.string.relationship_delete_action
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                text = value?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.relationship_detail_empty_value),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon ?: Icons.Outlined.Phone,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ContactInfoRow(
    phoneNumber: String?,
    email: String?
) {
    if (phoneNumber == null && email == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        phoneNumber?.let {
            InfoPill(
                icon = Icons.Outlined.Phone,
                text = it
            )
        }
        email?.let {
            InfoPill(
                icon = Icons.Outlined.Email,
                text = it
            )
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InterestPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onAddRelationship: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(onClick = onAddRelationship) {
                Text(text = stringResource(R.string.relationship_add_person))
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    title: String? = null
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(
    hasFilters: Boolean,
    onPrimaryAction: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonSearch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = stringResource(
                    if (hasFilters) {
                        R.string.relationships_empty_filtered_title
                    } else {
                        R.string.relationships_empty_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    if (hasFilters) {
                        R.string.relationships_empty_filtered_body
                    } else {
                        R.string.relationships_empty_body
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onPrimaryAction) {
                Text(
                    text = stringResource(
                        if (hasFilters) {
                            R.string.relationships_reset_filters
                        } else {
                            R.string.relationship_add_person
                        }
                    )
                )
            }
        }
    }
}

private fun String?.toDisplayDate(): String? {
    val parts = this?.split("-")
    if (parts == null || parts.size != 3) return this
    val year = parts[0]
    val month = parts[1]
    val day = parts[2]
    return "$day/$month/$year"
}

@Preview(showBackground = true)
@Composable
private fun RelationshipsScreenPreview() {
    AppTheme {
        RelationshipsScreen(
            uiState = RelationshipsUiState(
                isLoading = false,
                relationships = listOf(
                    RelationshipListItem(
                        id = "1",
                        name = "Minh Anh",
                        initials = "MA",
                        tag = RelationshipTag.CloseFriend,
                        birthdayIso = "2003-11-12",
                        phoneNumber = "0901234567",
                        email = "minhanh@example.com",
                        interests = listOf("Cà phê", "Ảnh film"),
                        notes = "Hay đi bộ buổi tối.",
                        priority = RelationshipPriority.High
                    )
                ),
                totalRelationships = 3
            ),
            onSearchQueryChanged = {},
            onTagSelected = {},
            onResetFilters = {},
            onAddRelationship = {},
            onRelationshipSelected = {}
        )
    }
}
