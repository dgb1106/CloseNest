@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.closenest.features.relationships.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.viewmodel.AddRelationshipUiState
import com.example.closenest.features.relationships.viewmodel.AddRelationshipViewModel
import com.example.closenest.ui.theme.AppTheme

@Composable
fun AddRelationshipRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddRelationshipViewModel = viewModel(factory = AddRelationshipViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    AddRelationshipScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::onNameChanged,
        onTagChanged = viewModel::onTagChanged,
        onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onBirthdayChanged = viewModel::onBirthdayChanged,
        onInterestsChanged = viewModel::onInterestsChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onPriorityChanged = viewModel::onPriorityChanged,
        onToggleMoreDetails = viewModel::onToggleMoreDetails,
        onSave = viewModel::saveRelationship,
        modifier = modifier
    )
}

@Composable
fun AddRelationshipScreen(
    uiState: AddRelationshipUiState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTagChanged: (RelationshipTag) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onBirthdayChanged: (String) -> Unit,
    onInterestsChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPriorityChanged: (RelationshipPriority) -> Unit,
    onToggleMoreDetails: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.add_relationship_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enabled = !uiState.isSubmitting,
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isSubmitting) {
                                R.string.add_relationship_saving
                            } else {
                                R.string.add_relationship_save
                            }
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IntroCard()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        isError = uiState.nameError,
                        label = {
                            Text(text = stringResource(R.string.add_relationship_name_label))
                        },
                        supportingText = {
                            if (uiState.nameError) {
                                Text(text = stringResource(R.string.add_relationship_name_error))
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.add_relationship_tag_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RelationshipTag.entries.forEach { tag ->
                                SelectChip(
                                    label = stringResource(tag.labelRes),
                                    selected = uiState.selectedTag == tag,
                                    onClick = { onTagChanged(tag) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerDefaults.color)

                    TextButton(onClick = onToggleMoreDetails) {
                        Text(
                            text = stringResource(
                                if (uiState.showMoreDetails) {
                                    R.string.add_relationship_hide_more
                                } else {
                                    R.string.add_relationship_show_more
                                }
                            )
                        )
                        Icon(
                            imageVector = if (uiState.showMoreDetails) {
                                Icons.Outlined.ExpandLess
                            } else {
                                Icons.Outlined.ExpandMore
                            },
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    AnimatedVisibility(visible = uiState.showMoreDetails) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.phoneNumber,
                                onValueChange = onPhoneNumberChanged,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                label = {
                                    Text(text = stringResource(R.string.add_relationship_phone_label))
                                }
                            )

                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = onEmailChanged,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                isError = uiState.emailError,
                                label = {
                                    Text(text = stringResource(R.string.add_relationship_email_label))
                                },
                                supportingText = {
                                    if (uiState.emailError) {
                                        Text(text = stringResource(R.string.add_relationship_email_error))
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = uiState.birthday,
                                onValueChange = onBirthdayChanged,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                isError = uiState.birthdayError,
                                label = {
                                    Text(text = stringResource(R.string.add_relationship_birthday_label))
                                },
                                supportingText = {
                                    Text(
                                        text = stringResource(
                                            if (uiState.birthdayError) {
                                                R.string.add_relationship_birthday_error
                                            } else {
                                                R.string.add_relationship_birthday_hint
                                            }
                                        )
                                    )
                                }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = stringResource(R.string.add_relationship_priority_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    RelationshipPriority.entries.forEach { priority ->
                                        FilledTonalButton(
                                            onClick = { onPriorityChanged(priority) },
                                            colors = if (uiState.priority == priority) {
                                                ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                ButtonDefaults.filledTonalButtonColors()
                                            }
                                        ) {
                                            Text(text = stringResource(priority.labelRes))
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = uiState.interests,
                                onValueChange = onInterestsChanged,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                label = {
                                    Text(text = stringResource(R.string.add_relationship_interests_label))
                                },
                                supportingText = {
                                    Text(text = stringResource(R.string.add_relationship_interests_hint))
                                }
                            )

                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = onNotesChanged,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(18.dp),
                                label = {
                                    Text(text = stringResource(R.string.add_relationship_notes_label))
                                }
                            )
                        }
                    }

                    uiState.errorMessageRes?.let { messageRes ->
                        Text(
                            text = stringResource(messageRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.add_relationship_intro_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.add_relationship_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
        label = { Text(text = label) },
        colors = if (selected) {
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AddRelationshipScreenPreview() {
    AppTheme {
        AddRelationshipScreen(
            uiState = AddRelationshipUiState(showMoreDetails = true),
            onNavigateBack = {},
            onNameChanged = {},
            onTagChanged = {},
            onPhoneNumberChanged = {},
            onEmailChanged = {},
            onBirthdayChanged = {},
            onInterestsChanged = {},
            onNotesChanged = {},
            onPriorityChanged = {},
            onToggleMoreDetails = {},
            onSave = {}
        )
    }
}
