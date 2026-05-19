@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.closenest.features.homepage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.homepage.viewmodel.AddHubUiState
import com.example.closenest.features.homepage.viewmodel.AddHubViewModel
import com.example.closenest.features.homepage.viewmodel.ReflectionContactListItem
import com.example.closenest.features.homepage.viewmodel.ReflectionFeeling
import com.example.closenest.features.homepage.viewmodel.ReflectionFeelingOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionMood
import com.example.closenest.features.homepage.viewmodel.ReflectionMoodOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionSource
import com.example.closenest.features.homepage.viewmodel.ReflectionSourceOptions

@Composable
fun AddHubRoute(
    onAddRelationship: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddHubScreen(
        uiState = uiState,
        onAddRelationship = onAddRelationship,
        onContactToggled = viewModel::onContactToggled,
        onOpenReflection = viewModel::openReflectionSheet,
        onCloseReflection = viewModel::closeReflectionSheet,
        onMoodSelected = viewModel::onMoodSelected,
        onFeelingToggled = viewModel::onFeelingToggled,
        onSourceToggled = viewModel::onSourceToggled,
        onCompleteReflection = viewModel::completeReflection,
        modifier = modifier
    )
}

@Composable
fun AddHubScreen(
    uiState: AddHubUiState,
    onAddRelationship: () -> Unit,
    onContactToggled: (String) -> Unit,
    onOpenReflection: () -> Unit,
    onCloseReflection: () -> Unit,
    onMoodSelected: (ReflectionMood) -> Unit,
    onFeelingToggled: (ReflectionFeeling) -> Unit,
    onSourceToggled: (ReflectionSource) -> Unit,
    onCompleteReflection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReflectionCard(
                    uiState = uiState,
                    onContactToggled = onContactToggled,
                    onOpenReflection = onOpenReflection
                )
            }

            item {
                AddActionCard(
                    title = stringResource(R.string.add_hub_relationship_title),
                    description = stringResource(R.string.add_hub_relationship_body),
                    icon = Icons.Outlined.Add,
                    ctaLabel = stringResource(R.string.relationship_add_person),
                    onClick = onAddRelationship
                )
            }
        }

        if (uiState.showReflectionSheet) {
            ReflectionDialog(
                uiState = uiState,
                onClose = onCloseReflection,
                onMoodSelected = onMoodSelected,
                onFeelingToggled = onFeelingToggled,
                onSourceToggled = onSourceToggled,
                onCompleteReflection = onCompleteReflection
            )
        }
    }
}

@Composable
private fun AddActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ctaLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = ctaLabel)
            }
        }
    }
}

@Composable
private fun ReflectionCard(
    uiState: AddHubUiState,
    onContactToggled: (String) -> Unit,
    onOpenReflection: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.add_reflection_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.add_reflection_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            when {
                uiState.isLoadingContacts -> {
                    LoadingContactsRow()
                }

                uiState.contactsErrorMessageRes != null -> {
                    Text(
                        text = stringResource(uiState.contactsErrorMessageRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.contacts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.add_reflection_empty_contacts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.add_reflection_contact_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ContactChecklist(
                        contacts = uiState.contacts,
                        selectedContactIds = uiState.selectedContactIds,
                        onContactToggled = onContactToggled
                    )
                    Text(
                        text = stringResource(
                            R.string.add_reflection_selected_count,
                            uiState.selectedContactIds.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            FilledTonalButton(
                onClick = onOpenReflection,
                enabled = !uiState.isLoadingContacts &&
                    uiState.contactsErrorMessageRes == null &&
                    !uiState.isSavingReflection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.add_reflection_open))
            }

            uiState.reflectionSavedMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun LoadingContactsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = stringResource(R.string.add_reflection_loading_contacts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ContactChecklist(
    contacts: List<ReflectionContactListItem>,
    selectedContactIds: Set<String>,
    onContactToggled: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(
            items = contacts,
            key = { contact -> contact.id }
        ) { contact ->
            val checked = contact.id in selectedContactIds

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { onContactToggled(contact.id) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Checkbox(
                    checked = checked,
                    onCheckedChange = null
                )
            }
        }
    }
}

@Composable
private fun ReflectionDialog(
    uiState: AddHubUiState,
    onClose: () -> Unit,
    onMoodSelected: (ReflectionMood) -> Unit,
    onFeelingToggled: (ReflectionFeeling) -> Unit,
    onSourceToggled: (ReflectionSource) -> Unit,
    onCompleteReflection: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.add_reflection_sheet_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.add_reflection_sheet_subtitle,
                                uiState.selectedContactIds.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MoodSection(
                        selectedMood = uiState.selectedMood,
                        onMoodSelected = onMoodSelected
                    )

                    FeelingsSection(
                        selectedFeelings = uiState.selectedFeelings,
                        onFeelingToggled = onFeelingToggled
                    )

                    SourcesSection(
                        selectedSources = uiState.selectedSources,
                        onSourceToggled = onSourceToggled
                    )

                    uiState.reflectionErrorMessageRes?.let { messageRes ->
                        Text(
                            text = stringResource(messageRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Button(
                    onClick = onCompleteReflection,
                    enabled = !uiState.isSavingReflection,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isSavingReflection) {
                                R.string.add_reflection_saving
                            } else {
                                R.string.add_reflection_done
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodSection(
    selectedMood: ReflectionMood,
    onMoodSelected: (ReflectionMood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.add_reflection_mood_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReflectionMoodOptions.forEach { mood ->
                SelectChip(
                    label = stringResource(mood.labelRes),
                    selected = selectedMood == mood,
                    onClick = { onMoodSelected(mood) }
                )
            }
        }
    }
}

@Composable
private fun FeelingsSection(
    selectedFeelings: Set<ReflectionFeeling>,
    onFeelingToggled: (ReflectionFeeling) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.add_reflection_feeling_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReflectionFeelingOptions.forEach { feeling ->
                SelectChip(
                    label = stringResource(feeling.labelRes),
                    selected = feeling in selectedFeelings,
                    onClick = { onFeelingToggled(feeling) }
                )
            }
        }
    }
}

@Composable
private fun SourcesSection(
    selectedSources: Set<ReflectionSource>,
    onSourceToggled: (ReflectionSource) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.add_reflection_source_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReflectionSourceOptions.forEach { source ->
                SelectChip(
                    label = stringResource(source.labelRes),
                    selected = source in selectedSources,
                    onClick = { onSourceToggled(source) }
                )
            }
        }
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
        label = { Text(text = label) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary,
                leadingIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AddHubScreenPreview() {
    AppTheme {
        AddHubScreen(
            uiState = AddHubUiState(
                isLoadingContacts = false,
                contacts = listOf(
                    ReflectionContactListItem("1", "Minh Anh"),
                    ReflectionContactListItem("2", "Bảo Nam"),
                    ReflectionContactListItem("3", "Gia Hân")
                ),
                selectedContactIds = setOf("1")
            ),
            onAddRelationship = {},
            onContactToggled = {},
            onOpenReflection = {},
            onCloseReflection = {},
            onMoodSelected = {},
            onFeelingToggled = {},
            onSourceToggled = {},
            onCompleteReflection = {}
        )
    }
}
