@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.model.SelectableRelationshipTags
import com.example.closenest.features.relationships.viewmodel.AddRelationshipUiState
import com.example.closenest.features.relationships.viewmodel.AddRelationshipViewModel

@Composable
fun AddRelationshipRoute(
    onNavigateBack: () -> Unit,
    editRelationshipId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AddRelationshipViewModel = viewModel(
        factory = AddRelationshipViewModel.factory(editRelationshipId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    AddRelationshipScreen(
        uiState = uiState,
        isEditMode = viewModel.isEditMode,
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
        onErrorMessageShown = viewModel::clearErrorMessage,
        onSave = viewModel::saveRelationship,
        modifier = modifier
    )
}

@Composable
fun AddRelationshipScreen(
    uiState: AddRelationshipUiState,
    isEditMode: Boolean = false,
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
    onErrorMessageShown: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.errorMessageRes?.let { stringResource(it) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(errorMessage)
            onErrorMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(
                        if (isEditMode) {
                            R.string.relationship_edit_action
                        } else {
                            R.string.add_relationship_title
                        }
                    ))
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
                color = MaterialTheme.colorScheme.onPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FieldTitle(text = stringResource(R.string.add_relationship_name_label))
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = onNameChanged,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                isError = uiState.nameError,
                                placeholder = {
                                    Text(text = stringResource(R.string.add_relationship_name_label))
                                }
                            )
                        }

                        RelationshipGroupPicker(
                            selectedTag = uiState.selectedTag,
                            onTagChanged = onTagChanged
                        )

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
                            MoreDetailsForm(
                                uiState = uiState,
                                onPhoneNumberChanged = onPhoneNumberChanged,
                                onEmailChanged = onEmailChanged,
                                onBirthdayChanged = onBirthdayChanged,
                                onPriorityChanged = onPriorityChanged,
                                onInterestsChanged = onInterestsChanged,
                                onNotesChanged = onNotesChanged
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipGroupPicker(
    selectedTag: RelationshipTag,
    onTagChanged: (RelationshipTag) -> Unit
) {
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
            SelectableRelationshipTags.forEach { tag ->
                SelectChip(
                    label = stringResource(tag.labelRes),
                    selected = selectedTag == tag,
                    onClick = { onTagChanged(tag) }
                )
            }
        }
    }
}

@Composable
private fun MoreDetailsForm(
    uiState: AddRelationshipUiState,
    onPhoneNumberChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onBirthdayChanged: (String) -> Unit,
    onPriorityChanged: (RelationshipPriority) -> Unit,
    onInterestsChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldTitle(text = stringResource(R.string.add_relationship_phone_label))
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneNumberChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(text = stringResource(R.string.add_relationship_phone_label))
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldTitle(text = stringResource(R.string.add_relationship_email_label))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                isError = uiState.emailError,
                placeholder = {
                    Text(text = stringResource(R.string.add_relationship_email_label))
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldTitle(text = stringResource(R.string.add_relationship_birthday_label))
            OutlinedTextField(
                value = uiState.birthday,
                onValueChange = onBirthdayChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                isError = uiState.birthdayError,
                placeholder = {
                    Text(text = stringResource(R.string.add_relationship_birthday_label))
                }
            )
        }

        RelationshipPriorityPicker(
            selectedPriority = uiState.priority,
            onPriorityChanged = onPriorityChanged
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldTitle(text = stringResource(R.string.add_relationship_interests_label))
            OutlinedTextField(
                value = uiState.interests,
                onValueChange = onInterestsChanged,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(text = stringResource(R.string.add_relationship_interests_label))
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldTitle(text = stringResource(R.string.add_relationship_notes_label))
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(text = stringResource(R.string.add_relationship_notes_label))
                }
            )
        }
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun RelationshipPriorityPicker(
    selectedPriority: RelationshipPriority,
    onPriorityChanged: (RelationshipPriority) -> Unit
) {
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
                    colors = if (selectedPriority == priority) {
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
            isEditMode = false,
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
            onErrorMessageShown = {},
            onSave = {}
        )
    }
}
