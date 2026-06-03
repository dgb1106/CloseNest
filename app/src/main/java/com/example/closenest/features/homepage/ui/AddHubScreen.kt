@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.closenest.features.homepage.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.homepage.viewmodel.AddHubUiState
import com.example.closenest.features.homepage.viewmodel.AddHubViewModel
import com.example.closenest.features.homepage.viewmodel.MemoryType
import com.example.closenest.features.homepage.viewmodel.MemoryTypeOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionContactListItem
import com.example.closenest.features.homepage.viewmodel.ReflectionFeeling
import com.example.closenest.features.homepage.viewmodel.ReflectionFeelingOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionMood
import com.example.closenest.features.homepage.viewmodel.ReflectionMoodOptions
import com.example.closenest.features.homepage.viewmodel.ReflectionSource
import com.example.closenest.features.homepage.viewmodel.ReflectionSourceOptions
import com.example.closenest.core.network.PlacesApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun AddHubRoute(
    onAddRelationship: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenMemory: () -> Unit,
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
        onOpenMemory = {
            viewModel.startMemory()
            onOpenMemory()
        },
        onOpenAppointment = {
            viewModel.startAppointment()
            onOpenAppointment()
        },
        onSavedFeedbackDismissed = viewModel::clearSavedFeedback,
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
fun MemoryRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHubViewModel = viewModel(factory = AddHubViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPhotoOptions by rememberSaveable { mutableStateOf(false) }
    var pendingCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onMemoryPhotoChanged(uri.toString())
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onMemoryPhotoChanged(
            if (success) pendingCameraPhotoUri?.toString() else null
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = context.createMemoryPhotoUri()
            pendingCameraPhotoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    LaunchedEffect(uiState.memorySavedMessageRes) {
        if (uiState.memorySavedMessageRes != null) {
            onNavigateBack()
        }
    }

    MemoryScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onContactToggled = viewModel::onMemoryContactToggled,
        onContactQueryChanged = viewModel::onMemoryContactQueryChanged,
        onTitleChanged = viewModel::onMemoryTitleChanged,
        onTypeSelected = viewModel::onMemoryTypeSelected,
        onNoteChanged = viewModel::onMemoryNoteChanged,
        onAddPhotoClick = { showPhotoOptions = true },
        onLocationChanged = viewModel::onMemoryLocationChanged,
        onLocationSelected = viewModel::onMemoryLocationSelected,
        onSave = viewModel::saveMemory,
        modifier = modifier,
        context = context
    )

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text(text = stringResource(R.string.add_interaction_photo_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            showPhotoOptions = false
                            openDocumentLauncher.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.add_interaction_photo_album))
                    }
                    FilledTonalButton(
                        onClick = {
                            showPhotoOptions = false
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                val uri = context.createMemoryPhotoUri()
                                pendingCameraPhotoUri = uri
                                takePictureLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.add_interaction_photo_camera))
                    }
                }
            },
            confirmButton = {}
        )
    }
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
        onContactToggled = viewModel::onAppointmentContactToggled,
        onContactQueryChanged = viewModel::onAppointmentContactQueryChanged,
        onLocationChanged = viewModel::onAppointmentLocationChanged,
        onLocationSelected = viewModel::onAppointmentLocationSelected,
        onDateSelected = viewModel::onAppointmentDateSelected,
        onAllDayToggled = viewModel::onAppointmentAllDayToggled,
        onTimeSelected = viewModel::onAppointmentTimeSelected,
        onSave = viewModel::saveAppointment,
        modifier = modifier
    )
}

@Composable
fun AddHubScreen(
    uiState: AddHubUiState,
    onAddRelationship: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenAppointment: () -> Unit,
    onSavedFeedbackDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var savedFeedbackMessageRes by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.reflectionSavedMessageRes) {
        uiState.reflectionSavedMessageRes?.let { messageRes ->
            savedFeedbackMessageRes = messageRes
        }
    }

    LaunchedEffect(uiState.memorySavedMessageRes) {
        uiState.memorySavedMessageRes?.let { messageRes ->
            savedFeedbackMessageRes = messageRes
        }
    }

    LaunchedEffect(uiState.appointmentSavedMessageRes) {
        uiState.appointmentSavedMessageRes?.let { messageRes ->
            savedFeedbackMessageRes = messageRes
        }
    }

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
                onClick = onOpenMemory
            )
        }

        item {
            AddActionCard(
                title = stringResource(R.string.add_appointment_title),
                description = stringResource(R.string.add_appointment_body),
                icon = Icons.Outlined.Event,
                ctaLabel = stringResource(R.string.add_appointment_open),
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

    savedFeedbackMessageRes?.let { messageRes ->
        SavedFeedbackDialog(
            messageRes = messageRes,
            onDismiss = {
                savedFeedbackMessageRes = null
                onSavedFeedbackDismissed()
            }
        )
    }
}

@Composable
fun AppointmentScreen(
    uiState: AddHubUiState,
    onNavigateBack: () -> Unit,
    onContactToggled: (String) -> Unit,
    onContactQueryChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    onDateSelected: (Long) -> Unit,
    onAllDayToggled: (Boolean) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val appointmentContactsSelected = uiState.selectedAppointmentContactIds.isNotEmpty()
    val appointmentLocationSelected = uiState.appointmentLocationLatitude != null &&
        uiState.appointmentLocationLongitude != null

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
                    appointmentContactsSelected &&
                    uiState.appointmentLocation.isNotBlank() &&
                    appointmentLocationSelected &&
                    uiState.appointmentDateMillis != null &&
                    (uiState.isAppointmentAllDay || (
                        uiState.appointmentTimeHour != null &&
                            uiState.appointmentTimeMinute != null
                        )),
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(text = stringResource(R.string.add_appointment_name_label))
                    AppointmentSelectedContactsSummary(
                        contacts = uiState.contacts.filter { it.id in uiState.selectedAppointmentContactIds },
                        onContactToggled = onContactToggled
                    )
                    AppointmentContactPicker(
                        query = uiState.appointmentContactQuery,
                        contacts = uiState.contacts,
                        selectedContactIds = uiState.selectedAppointmentContactIds,
                        onQueryChanged = onContactQueryChanged,
                        onContactToggled = onContactToggled
                    )
                }
            }

            item {
                LocationSection(
                    location = uiState.appointmentLocation,
                    onLocationChanged = onLocationChanged,
                    onLocationSelected = onLocationSelected,
                    context = LocalContext.current
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(text = stringResource(R.string.add_appointment_all_day))
                        Switch(
                            checked = uiState.isAppointmentAllDay,
                            onCheckedChange = onAllDayToggled
                        )
                    }

                    if (!uiState.isAppointmentAllDay) {
                        SectionTitle(text = stringResource(R.string.add_appointment_time_label))
                        FilledTonalButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(
                                text = if (uiState.appointmentTimeHour != null &&
                                    uiState.appointmentTimeMinute != null
                                ) {
                                    formatAppointmentTime(
                                        hour = uiState.appointmentTimeHour,
                                        minute = uiState.appointmentTimeMinute
                                    )
                                } else {
                                    stringResource(R.string.add_appointment_pick_time)
                                }
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

    if (showTimePicker) {
        AppointmentTimePickerDialog(
            selectedHour = uiState.appointmentTimeHour,
            selectedMinute = uiState.appointmentTimeMinute,
            onTimeSelected = { hour, minute ->
                onTimeSelected(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentSelectedContactsSummary(
    contacts: List<ReflectionContactListItem>,
    onContactToggled: (String) -> Unit
) {
    if (contacts.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.add_appointment_selected_count, contacts.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contacts.forEach { contact ->
                MultiSelectChip(
                    label = contact.name,
                    selected = true,
                    onClick = { onContactToggled(contact.id) }
                )
            }
        }
    }
}

@Composable
private fun AppointmentContactPicker(
    query: String,
    contacts: List<ReflectionContactListItem>,
    selectedContactIds: Set<String>,
    onQueryChanged: (String) -> Unit,
    onContactToggled: (String) -> Unit
) {
    val normalizedQuery = query.trim()
    val visibleContacts = remember(normalizedQuery, contacts) {
        if (normalizedQuery.isBlank()) {
            contacts
        } else {
            val matchingContacts = contacts.filter {
                it.name.contains(normalizedQuery, ignoreCase = true)
            }
            val remainingContacts = contacts.filterNot { contact ->
                contact.name.contains(normalizedQuery, ignoreCase = true)
            }
            matchingContacts + remainingContacts
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        placeholder = {
            Text(text = stringResource(R.string.add_appointment_search_hint))
        }
    )

    when {
        contacts.isEmpty() -> {
            Text(
                text = stringResource(R.string.add_interaction_empty_contacts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (normalizedQuery.isNotBlank() && visibleContacts.none {
                        it.name.contains(normalizedQuery, ignoreCase = true)
                    }) {
                    Text(
                        text = stringResource(R.string.relationships_empty_filtered_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SectionTitle(text = stringResource(R.string.add_appointment_all_contacts))
                visibleContacts.forEach { contact ->
                    ContactCheckboxRow(
                        contact = contact,
                        checked = contact.id in selectedContactIds,
                        onContactToggled = onContactToggled
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemorySelectedContactsSummary(
    contacts: List<ReflectionContactListItem>,
    onContactToggled: (String) -> Unit
) {
    if (contacts.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.add_interaction_selected_count, contacts.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contacts.forEach { contact ->
                MultiSelectChip(
                    label = contact.name,
                    selected = true,
                    onClick = { onContactToggled(contact.id) }
                )
            }
        }
    }
}

@Composable
private fun MemoryContactPicker(
    query: String,
    contacts: List<ReflectionContactListItem>,
    selectedContactIds: Set<String>,
    onQueryChanged: (String) -> Unit,
    onContactToggled: (String) -> Unit
) {
    val normalizedQuery = query.trim()
    val visibleContacts = remember(normalizedQuery, contacts) {
        if (normalizedQuery.isBlank()) {
            contacts
        } else {
            val matchingContacts = contacts.filter {
                it.name.contains(normalizedQuery, ignoreCase = true)
            }
            val remainingContacts = contacts.filterNot { contact ->
                contact.name.contains(normalizedQuery, ignoreCase = true)
            }
            matchingContacts + remainingContacts
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        placeholder = {
            Text(text = stringResource(R.string.add_interaction_search_hint))
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (normalizedQuery.isNotBlank() && visibleContacts.none {
                it.name.contains(normalizedQuery, ignoreCase = true)
            }) {
            Text(
                text = stringResource(R.string.relationships_empty_filtered_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionTitle(text = stringResource(R.string.add_interaction_all_contacts))
        visibleContacts.forEach { contact ->
            ContactCheckboxRow(
                contact = contact,
                checked = contact.id in selectedContactIds,
                onContactToggled = onContactToggled
            )
        }
    }
}

@Composable
fun MemoryScreen(
    uiState: AddHubUiState,
    onNavigateBack: () -> Unit,
    onContactToggled: (String) -> Unit,
    onContactQueryChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onTypeSelected: (MemoryType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onLocationChanged: (String) -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
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
                errorMessageRes = uiState.memoryErrorMessageRes,
                isSaving = uiState.isSavingMemory,
                savingLabelRes = R.string.add_interaction_saving,
                saveLabelRes = R.string.add_interaction_done,
                enabled = !uiState.isSavingMemory &&
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
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle(text = stringResource(R.string.add_interaction_contact_title))
                            MemorySelectedContactsSummary(
                                contacts = uiState.contacts.filter { it.id in uiState.selectedMemoryContactIds },
                                onContactToggled = onContactToggled
                            )
                            MemoryContactPicker(
                                query = uiState.memoryContactQuery,
                                contacts = uiState.contacts,
                                selectedContactIds = uiState.selectedMemoryContactIds,
                                onQueryChanged = onContactQueryChanged,
                                onContactToggled = onContactToggled
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(text = "Bạn đã có kỷ niệm gì?")
                    OutlinedTextField(
                        value = uiState.memoryTitle,
                        onValueChange = onTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        label = {
                            Text(text = "Tên kỷ niệm")
                        }
                    )
                }
            }

            item {
                MemoryTypeSection(
                    selectedType = uiState.selectedMemoryType,
                    onTypeSelected = onTypeSelected
                )
            }

            item {
                PhotoSection(
                    photoUri = uiState.memoryPhotoUri,
                    onAddPhotoClick = onAddPhotoClick
                )
            }

            item {
                LocationSection(
                    location = uiState.memoryLocation,
                    onLocationChanged = onLocationChanged,
                    onLocationSelected = onLocationSelected,
                    context = context
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(text = "Ghi chú")
                    OutlinedTextField(
                        value = uiState.memoryNote,
                        onValueChange = onNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        label = {
                            Text(text = stringResource(R.string.add_interaction_note_label))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentTimePickerDialog(
    selectedHour: Int?,
    selectedMinute: Int?,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember {
        Calendar.getInstance()
    }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour ?: calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = selectedMinute ?: calendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }
            ) {
                Text(text = stringResource(R.string.add_appointment_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.profile_logout_cancel_button))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onAddPhotoClick: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.add_interaction_photo_title))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onAddPhotoClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (photoUri.isNullOrBlank()) {
                            Icons.Outlined.Image
                        } else {
                            Icons.Outlined.Check
                        },
                        contentDescription = null
                    )
                    Text(
                        text = if (photoUri.isNullOrBlank()) {
                            stringResource(R.string.add_interaction_photo_add)
                        } else {
                            stringResource(R.string.add_interaction_photo_added)
                        }
                    )
                }
            }

            if (!photoUri.isNullOrBlank()) {
                val bitmap = remember(photoUri) {
                    val uri = Uri.parse(photoUri)
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0EDE8))
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Ảnh kỷ niệm",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFFB0A89E),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    location: String,
    onLocationChanged: (String) -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    context: Context
) {
    val tag = "LocationSection"
    val accessToken = context.getString(R.string.mapbox_access_token)
    val sessionToken = remember { java.util.UUID.randomUUID().toString() }
    var query by rememberSaveable { mutableStateOf(location) }
    var suggestions by remember { mutableStateOf<List<PlacesApiClient.PlaceSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var stopAutoSearchUntilUserTypes by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(query) {
        Log.d(tag, "query='$query' focused=$isFocused")
        if (query.isBlank() || !isFocused || stopAutoSearchUntilUserTypes) {
            suggestions = emptyList()
            showDropdown = false
            Log.d(tag, "skip suggest blankOrNoFocus showDropdown=false")
            return@LaunchedEffect
        }
        delay(350)
        isSearching = true
        suggestions = PlacesApiClient.suggest(
            query = query,
            accessToken = accessToken,
            sessionToken = sessionToken
        )
        showDropdown = suggestions.isNotEmpty() && isFocused
        Log.d(tag, "suggestions=${suggestions.size} showDropdown=$showDropdown")
        isSearching = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.add_interaction_location_title))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(18.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(3f)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { newValue ->
                            stopAutoSearchUntilUserTypes = false
                            query = newValue
                            onLocationChanged(newValue)
                            showDropdown = newValue.isNotBlank() && isFocused
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                showDropdown = focusState.isFocused && suggestions.isNotEmpty()
                                Log.d(tag, "focus=$isFocused showDropdown=$showDropdown suggestions=${suggestions.size}")
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = {
                            Text(text = stringResource(R.string.add_interaction_location_label))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            coroutineScope.launch {
                                context.findCurrentReadableLocation()?.let { (label, latitude, longitude) ->
                                    stopAutoSearchUntilUserTypes = true
                                    suggestions = emptyList()
                                    showDropdown = false
                                    query = label
                                    onLocationSelected(label, latitude, longitude)
                                }
                            }
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = stringResource(R.string.add_interaction_location_add)
                    )
                }
            }

            DropdownMenu(
                expanded = showDropdown && isFocused && suggestions.isNotEmpty(),
                onDismissRequest = { showDropdown = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                suggestions.take(5).forEach { suggestion ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = suggestion.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = suggestion.placeFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        onClick = {
                            val locationLabel = suggestion.name.ifBlank {
                                suggestion.placeFormatted.ifBlank { suggestion.fullAddress }
                            }
                            stopAutoSearchUntilUserTypes = true
                            suggestions = emptyList()
                            query = locationLabel
                            showDropdown = false
                            Log.d(tag, "click suggestion name='${suggestion.name}' mapboxId='${suggestion.mapboxId}'")
                            coroutineScope.launch {
                                val result = PlacesApiClient.retrieve(
                                    mapboxId = suggestion.mapboxId,
                                    accessToken = accessToken,
                                    sessionToken = sessionToken
                                )
                                if (result != null) {
                                    Log.d(tag, "retrieve lat=${result.latitude} lon=${result.longitude}")
                                    onLocationSelected(locationLabel, result.latitude, result.longitude)
                                } else {
                                    Log.e(tag, "retrieve null for mapboxId='${suggestion.mapboxId}'")
                                    onLocationChanged(locationLabel)
                                }
                            }
                        }
                    )
                }
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun ReflectionEntryCard(
    uiState: AddHubUiState,
    onOpenReflection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

@Composable
private fun SavedFeedbackDialog(
    messageRes: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.common_saved_title))
        },
        text = {
            Text(text = stringResource(messageRes))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        }
    )
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
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
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
        modifier = Modifier.fillMaxWidth(),
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
private fun MoodSection(
    selectedMood: ReflectionMood,
    onMoodSelected: (ReflectionMood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.add_reflection_mood_question))
        ChipRows(options = ReflectionMoodOptions) { mood, chipModifier ->
            SelectChip(
                label = stringResource(mood.labelRes),
                selected = selectedMood == mood,
                onClick = { onMoodSelected(mood) },
                modifier = chipModifier
            )
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
        ChipRows(options = ReflectionFeelingOptions) { feeling, chipModifier ->
            MultiSelectChip(
                label = stringResource(feeling.labelRes),
                selected = feeling in selectedFeelings,
                onClick = { onFeelingToggled(feeling) },
                modifier = chipModifier
            )
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
        ChipRows(options = ReflectionSourceOptions) { source, chipModifier ->
            MultiSelectChip(
                label = stringResource(source.labelRes),
                selected = source in selectedSources,
                onClick = { onSourceToggled(source) },
                modifier = chipModifier
            )
        }
    }
}

@Composable
private fun MemoryTypeSection(
    selectedType: MemoryType,
    onTypeSelected: (MemoryType) -> Unit
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
            MemoryTypeOptions.forEach { type ->
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
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
        label = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
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

@Composable
private fun MultiSelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
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
        label = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun <T> ChipRows(
    options: List<T>,
    columns: Int = 2,
    chip: @Composable (T, Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    chip(option, Modifier.weight(1f))
                }
                repeat(columns - rowOptions.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
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
            onOpenMemory = {},
            onOpenAppointment = {},
            onSavedFeedbackDismissed = {}
        )
    }
}

private fun formatAppointmentDate(dateMillis: Long): String {
    return appointmentDateFormatter.format(Date(dateMillis))
}

private fun formatAppointmentTime(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

private fun Context.createMemoryPhotoUri(): Uri {
    val imagesDirectory = File(cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("memory_", ".jpg", imagesDirectory)
    return FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        imageFile
    )
}

private suspend fun Context.findCurrentReadableLocation(): Triple<String, Double, Double>? {
    val location = findBestUserLocation() ?: return null
    val label = reverseGeocode(location) ?: "${location.latitude}, ${location.longitude}"
    return Triple(label, location.latitude, location.longitude)
}

private suspend fun Context.findBestUserLocation(): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    val lastLocation = runCatching { fusedLocationClient.lastLocation.awaitResult() }.getOrNull()
    if (lastLocation != null) {
        return lastLocation
    }

    val cancellationTokenSource = CancellationTokenSource()
    return try {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .awaitResult()
    } finally {
        cancellationTokenSource.cancel()
    }
}

private suspend fun Context.reverseGeocode(location: Location): String? =
    withContext(kotlinx.coroutines.Dispatchers.IO) {
        val geocoder = Geocoder(this@reverseGeocode, Locale.getDefault())
        val addresses = try {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        } catch (_: IOException) {
            null
        }.orEmpty()

        addresses.firstOrNull()?.toPreferredLocationLabel()
    }

private fun Address.toPreferredLocationLabel(): String? {
    val preferredName = listOf(
        featureName,
        premises,
        thoroughfare,
        subLocality,
        locality
    ).firstOrNull { !it.isNullOrBlank() }?.trim()

    if (!preferredName.isNullOrBlank()) {
        return preferredName
    }

    return buildList {
        for (index in 0..maxAddressLineIndex) {
            getAddressLine(index)?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }.joinToString(", ").takeIf { it.isNotBlank() }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.cancel()
            }
        }
    }

private val appointmentDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
