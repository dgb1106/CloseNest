package com.example.closenest.features.relationships.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.relationships.model.AttentionStatus
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.viewmodel.RelationshipListItem
import com.example.closenest.features.relationships.viewmodel.RelationshipsUiState
import com.example.closenest.features.relationships.viewmodel.RelationshipsViewModel
import com.example.closenest.features.relationships.viewmodel.SuggestedAction
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.CloseNestAttention
import com.example.closenest.core.ui.theme.CloseNestConnected
import com.example.closenest.core.ui.theme.CloseNestWarm

@Composable
fun RelationshipsRoute(
    onAddRelationship: () -> Unit,
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
            HeaderSection(onAddRelationship = onAddRelationship)
        }

        item {
            SummaryCard(
                totalRelationships = uiState.totalRelationships,
                relationshipsNeedingAttention = uiState.relationshipsNeedingAttention
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
                RelationshipTag.entries.forEach { tag ->
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

            uiState.errorMessage != null -> {
                item {
                    ErrorState(
                        message = uiState.errorMessage,
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
                    RelationshipCard(relationship = relationship)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    onAddRelationship: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.relationships_title),
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
private fun SummaryCard(
    totalRelationships: Int,
    relationshipsNeedingAttention: Int
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.relationship_summary_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.relationship_summary_people,
                    count = totalRelationships,
                    totalRelationships
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.relationship_summary_attention,
                    count = relationshipsNeedingAttention,
                    relationshipsNeedingAttention
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f)
            )
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
    relationship: RelationshipListItem
) {
    Card(
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
                verticalAlignment = Alignment.Top
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
                            relationship.lastInteractionText()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AttentionBadge(status = relationship.attentionStatus)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.relationship_suggestion_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = relationship.suggestionText(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(relationship.suggestedAction.labelRes))
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(initials: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
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
private fun AttentionBadge(status: AttentionStatus) {
    val background = when (status) {
        AttentionStatus.NeedsAttention -> CloseNestAttention.copy(alpha = 0.16f)
        AttentionStatus.Warm -> CloseNestWarm.copy(alpha = 0.18f)
        AttentionStatus.RecentlyConnected -> CloseNestConnected.copy(alpha = 0.18f)
    }
    val content = when (status) {
        AttentionStatus.NeedsAttention -> CloseNestAttention
        AttentionStatus.Warm -> CloseNestWarm
        AttentionStatus.RecentlyConnected -> CloseNestConnected
    }

    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = stringResource(status.labelRes),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content
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

@Composable
private fun RelationshipListItem.lastInteractionText(): String {
    if (daysSinceLastInteraction == null || lastInteractionLabelRes == null) {
        return stringResource(R.string.relationship_no_interaction_yet)
    }

    val timeText = if (daysSinceLastInteraction == 0L) {
        stringResource(R.string.relationship_last_interaction_today)
    } else {
        pluralStringResource(
            id = R.plurals.relationship_last_interaction_days_ago,
            count = daysSinceLastInteraction.toInt(),
            daysSinceLastInteraction
        )
    }

    return stringResource(
        R.string.relationship_last_interaction_template,
        stringResource(lastInteractionLabelRes),
        timeText
    )
}

@Composable
private fun RelationshipListItem.suggestionText(): String {
    val firstName = name.substringBefore(" ")
    return when (suggestedAction) {
        SuggestedAction.SendCheckIn -> stringResource(
            R.string.relationship_suggestion_check_in,
            firstName
        )
        SuggestedAction.MakeCall -> stringResource(
            R.string.relationship_suggestion_call,
            firstName
        )
        SuggestedAction.PlanMeet -> stringResource(
            R.string.relationship_suggestion_meet,
            firstName
        )
    }
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
                        phoneNumber = "0901234567",
                        email = "minhanh@example.com",
                        interests = listOf("Cà phê", "Ảnh film"),
                        notes = "Hay đi bộ buổi tối.",
                        priority = RelationshipPriority.High,
                        daysSinceLastInteraction = 5,
                        lastInteractionLabelRes = R.string.interaction_type_chat,
                        attentionStatus = AttentionStatus.Warm,
                        suggestedAction = SuggestedAction.PlanMeet
                    )
                ),
                totalRelationships = 3,
                relationshipsNeedingAttention = 1
            ),
            onSearchQueryChanged = {},
            onTagSelected = {},
            onResetFilters = {},
            onAddRelationship = {}
        )
    }
}
