@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.closenest.features.homepage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.homepage.viewmodel.AddHubUiState
import com.example.closenest.features.homepage.viewmodel.AddHubViewModel
import com.example.closenest.features.homepage.viewmodel.InteractionLogType
import com.example.closenest.features.homepage.viewmodel.InteractionLogTypeOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionContactListItem
import com.example.closenest.features.homepage.viewmodel.ReflectionFeeling
import com.example.closenest.features.homepage.viewmodel.ReflectionFeelingOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionMood
import com.example.closenest.features.homepage.viewmodel.ReflectionMoodOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionSource
import com.example.closenest.features.homepage.viewmodel.ReflectionSourceOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddHubRoute(
    onAddRelationship: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenInteractionLog: () -> Unit,
    onOpenAppointment: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddHubScreen(
        uiState = uiState,
        onAddRelationship = onAddRelationship,
        onOpenReflection = {
            viewModel.startReflection()
            onOpenReflection()
        },
        onOpenInteractionLog = {
            viewModel.startInteractionLog()
            onOpenInteractionLog()
        },
        onOpenAppointment = {
            viewModel.startAppointment()
            onOpenAppointment()
        },
        modifier = modifier
    )
}

@Composable
fun ReflectionRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.reflectionSavedMessageRes) {
        if (uiState.reflectionSavedMessageRes != null) {
            onNavigateBack()
        }
    }

    ReflectionScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onContactToggled = viewModel::onContactToggled,
        onMoodSelected = viewModel::onMoodSelected,
        onFeelingToggled = viewModel::onFeelingToggled,
        onSourceToggled = viewModel::onSourceToggled,
        onCompleteReflection = viewModel::completeReflection,
        modifier = modifier
    )
}

@Composable
fun InteractionLogRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.interactionSavedMessageRes) {
        if (uiState.interactionSavedMessageRes != null) {
            onNavigateBack()
        }
    }

    InteractionLogScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onContactSelected = viewModel::onInteractionContactSelected,
        onTypeSelected = viewModel::onInteractionTypeSelected,
        onNoteChanged = viewModel::onInteractionNoteChanged,
        onSave = viewModel::saveInteractionLog,
        modifier = modifier
    )
}

@Composable
fun AppointmentRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.appointmentSavedMessageRes) {
        if (uiState.appointmentSavedMessageRes != null) {
            onNavigateBack()
        }
    }

    AppointmentScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::onAppointmentNameChanged,
        onLocationChanged = viewModel::onAppointmentLocationChanged,
        onDateSelected = viewModel::onAppointmentDateSelected,
        onSave = viewModel::saveAppointment,
        modifier = modifier
    )
}

@Composable
fun AddHubScreen(
    uiState: AddHubUiState,
    onAddRelationship: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenInteractionLog: () -> Unit,
    onOpenAppointment: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ReflectionEntryCard(
                uiState = uiState,
                onOpenReflection = onOpenReflection
            )
        }

        item {
            AddActionCard(
                title = stringResource(R.string.add_interaction_title),
                description = stringResource(R.string.add_interaction_body),
                icon = Icons.AutoMirrored.Outlined.Notes,
                ctaLabel = stringResource(R.string.add_interaction_open),
                messageRes = uiState.interactionSavedMessageRes,
                onClick = onOpenInteractionLog
            )
        }

        item {
            AddActionCard(
                title = stringResource(R.string.add_appointment_title),
                description = stringResource(R.string.add_appointment_body),
                icon = Icons.Outlined.Event,
                ctaLabel = stringResource(R.string.add_appointment_open),
                messageRes = uiState.appointmentSavedMessageRes,
                onClick = onOpenAppointment
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
}

@Composable
fun AppointmentScreen(
    uiState: AddHubUiState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val appointmentNameEntered = uiState.appointmentName.isNotBlank()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.add_appointment_screen_title)) },
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
            BottomSaveBar(
                errorMessageRes = uiState.appointmentErrorMessageRes,
                isSaving = uiState.isSavingAppointment,
                savingLabelRes = R.string.add_appointment_saving,
                saveLabelRes = R.string.add_appointment_done,
                enabled = !uiState.isSavingAppointment &&
                    uiState.appointmentName.isNotBlank() &&
                    uiState.appointmentLocation.isNotBlank() &&
                    uiState.appointmentDateMillis != null,
                onSave = onSave
            )
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
                ScreenIntroCard(
                    title = stringResource(R.string.add_appointment_title),
                    body = stringResource(R.string.add_appointment_body)
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.appointmentName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    label = {
                        Text(text = stringResource(R.string.add_appointment_name_label))
                    }
                )
            }

            if (appointmentNameEntered) {
                item {
                    OutlinedTextField(
                        value = uiState.appointmentLocation,
                        onValueChange = onLocationChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        label = {
                            Text(text = stringResource(R.string.add_appointment_location_label))
                        }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(text = stringResource(R.string.add_appointment_date_label))
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(
                                text = uiState.appointmentDateMillis?.let { dateMillis ->
                                    formatAppointmentDate(dateMillis)
                                } ?: stringResource(R.string.add_appointment_pick_date)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppointmentDatePickerDialog(
            selectedDateMillis = uiState.appointmentDateMillis,
            onDateSelected = { selectedDateMillis ->
                onDateSelected(selectedDateMillis)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun ReflectionScreen(
    uiState: AddHubUiState,
    onNavigateBack: () -> Unit,
    onContactToggled: (String) -> Unit,
    onMoodSelected: (ReflectionMood) -> Unit,
    onFeelingToggled: (ReflectionFeeling) -> Unit,
    onSourceToggled: (ReflectionSource) -> Unit,
    onCompleteReflection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReflectionOptions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedContactIds.isEmpty()) {
        if (uiState.selectedContactIds.isEmpty()) {
            showReflectionOptions = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.add_reflection_title)) },
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
            if (showReflectionOptions) {
                BottomSaveBar(
                    errorMessageRes = uiState.reflectionErrorMessageRes,
                    isSaving = uiState.isSavingReflection,
                    savingLabelRes = R.string.add_reflection_saving,
                    saveLabelRes = R.string.add_reflection_done,
                    enabled = !uiState.isSavingReflection &&
                        !uiState.isLoadingContacts &&
                        uiState.contactsErrorMessageRes == null,
                    onSave = onCompleteReflection
                )
            } else if (uiState.selectedContactIds.isNotEmpty()) {
                BottomSaveBar(
                    errorMessageRes = null,
                    isSaving = false,
                    savingLabelRes = R.string.add_reflection_next,
                    saveLabelRes = R.string.add_reflection_next,
                    enabled = !uiState.isLoadingContacts &&
                        uiState.contactsErrorMessageRes == null,
                    onSave = { showReflectionOptions = true }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showReflectionOptions) {
                item {
                    ScreenIntroCard(
                        title = stringResource(R.string.add_reflection_sheet_title),
                        body = stringResource(
                            R.string.add_reflection_sheet_subtitle,
                            uiState.selectedContactIds.size
                        )
                    )
                }

                item {
                    MoodSection(
                        selectedMood = uiState.selectedMood,
                        onMoodSelected = onMoodSelected
                    )
                }

                item {
                    FeelingsSection(
                        selectedFeelings = uiState.selectedFeelings,
                        onFeelingToggled = onFeelingToggled
                    )
                }

                item {
                    SourcesSection(
                        selectedSources = uiState.selectedSources,
                        onSourceToggled = onSourceToggled
                    )
                }
            } else {
                when {
                    uiState.isLoadingContacts -> {
                        item { LoadingContactsRow() }
                    }

                    uiState.contactsErrorMessageRes != null -> {
                        item {
                            Text(
                                text = stringResource(uiState.contactsErrorMessageRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    uiState.contacts.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.add_reflection_empty_contacts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    else -> {
                        item {
                            SectionTitle(
                                text = stringResource(R.string.add_reflection_contact_title)
                            )
                        }
                        items(
                            items = uiState.contacts,
                            key = { contact -> contact.id }
                        ) { contact ->
                            ContactCheckboxRow(
                                contact = contact,
                                checked = contact.id in uiState.selectedContactIds,
                                onContactToggled = onContactToggled
                            )
                        }
                        item {
                            Text(
                                text = stringResource(
                                    R.string.add_reflection_selected_count,
                                    uiState.selectedContactIds.size
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentDatePickerDialog(
    selectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text(text = stringResource(R.string.add_appointment_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.profile_logout_cancel_button))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun InteractionLogScreen(
    uiState: AddHubUiState,
    onNavigateBack: () -> Unit,
    onContactSelected: (String) -> Unit,
    onTypeSelected: (InteractionLogType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.add_interaction_screen_title)) },
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
            BottomSaveBar(
                errorMessageRes = uiState.interactionErrorMessageRes,
                isSaving = uiState.isSavingInteractionLog,
                savingLabelRes = R.string.add_interaction_saving,
                saveLabelRes = R.string.add_interaction_done,
                enabled = !uiState.isSavingInteractionLog &&
                    !uiState.isLoadingContacts &&
                    uiState.contactsErrorMessageRes == null &&
                    uiState.contacts.isNotEmpty(),
                onSave = onSave
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenIntroCard(
                    title = stringResource(R.string.add_interaction_screen_title),
                    body = stringResource(R.string.add_interaction_body)
                )
            }

            when {
                uiState.isLoadingContacts -> {
                    item { LoadingContactsRow() }
                }

                uiState.contactsErrorMessageRes != null -> {
                    item {
                        Text(
                            text = stringResource(uiState.contactsErrorMessageRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.contacts.isEmpty() -> {
                    item {
                        Text(
                            text = stringResource(R.string.add_interaction_empty_contacts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                else -> {
                    item {
                        SectionTitle(text = stringResource(R.string.add_interaction_contact_title))
                    }
                    items(
                        items = uiState.contacts,
                        key = { contact -> contact.id }
                    ) { contact ->
                        ContactRadioRow(
                            contact = contact,
                            selected = contact.id == uiState.selectedInteractionContactId,
                            onContactSelected = onContactSelected
                        )
                    }
                }
            }

            item {
                InteractionTypeSection(
                    selectedType = uiState.selectedInteractionType,
                    onTypeSelected = onTypeSelected
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.interactionNote,
                    onValueChange = onNoteChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    label = {
                        Text(text = stringResource(R.string.add_interaction_note_label))
                    },
                    supportingText = {
                        Text(text = stringResource(R.string.add_interaction_note_hint))
                    }
                )
            }
        }
    }
}

@Composable
private fun AddActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ctaLabel: String,
    onClick: () -> Unit,
    messageRes: Int? = null
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
            messageRes?.let { savedMessageRes ->
                Text(
                    text = stringResource(savedMessageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReflectionEntryCard(
    uiState: AddHubUiState,
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
            FilledTonalButton(
                onClick = onOpenReflection,
                enabled = !uiState.isSavingReflection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.add_reflection_start))
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
private fun BottomSaveBar(
    errorMessageRes: Int?,
    isSaving: Boolean,
    savingLabelRes: Int,
    saveLabelRes: Int,
    enabled: Boolean,
    onSave: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            errorMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isSaving) {
                            savingLabelRes
                        } else {
                            saveLabelRes
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun ScreenIntroCard(
    title: String,
    body: String
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold
    )
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
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.add_reflection_loading_contacts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ContactCheckboxRow(
    contact: ReflectionContactListItem,
    checked: Boolean,
    onContactToggled: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = { onContactToggled(contact.id) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
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

@Composable
private fun ContactRadioRow(
    contact: ReflectionContactListItem,
    selected: Boolean,
    onContactSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onContactSelected(contact.id) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@Composable
private fun MoodSection(
    selectedMood: ReflectionMood,
    onMoodSelected: (ReflectionMood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.add_reflection_mood_question))
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
        SectionTitle(text = stringResource(R.string.add_reflection_feeling_question))
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
        SectionTitle(text = stringResource(R.string.add_reflection_source_question))
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
private fun InteractionTypeSection(
    selectedType: InteractionLogType,
    onTypeSelected: (InteractionLogType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.add_interaction_type_question))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InteractionLogTypeOptions.forEach { type ->
                SelectChip(
                    label = stringResource(type.labelRes),
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) }
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
            onOpenReflection = {},
            onOpenInteractionLog = {},
            onOpenAppointment = {}
        )
    }
}

private fun formatAppointmentDate(dateMillis: Long): String {
    return appointmentDateFormatter.format(Date(dateMillis))
}

private val appointmentDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
