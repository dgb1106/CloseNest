package com.example.closenest.features.homepage.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewAppointmentRequest
import com.example.closenest.features.homepage.model.NewMemoryRequest
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.example.closenest.features.homepage.model.ReflectionContactSnapshot
import com.example.closenest.features.homepage.repository.AppointmentRepository
import com.example.closenest.features.homepage.repository.AppointmentRepositoryProvider
import com.example.closenest.features.homepage.repository.MemoryRepository
import com.example.closenest.features.homepage.repository.MemoryRepositoryProvider
import com.example.closenest.features.homepage.repository.ReflectionRepository
import com.example.closenest.features.homepage.repository.ReflectionRepositoryProvider
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class AddHubUiState(
    val isLoadingContacts: Boolean = true,
    val contacts: List<ReflectionContactListItem> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val selectedMood: ReflectionMood = ReflectionMood.Neutral,
    val selectedFeelings: Set<ReflectionFeeling> = emptySet(),
    val selectedSources: Set<ReflectionSource> = emptySet(),
    val isSavingReflection: Boolean = false,
    val selectedMemoryContactIds: Set<String> = emptySet(),
    val memoryContactQuery: String = "",
    val memoryTitle: String = "",
    val selectedMemoryType: MemoryType = MemoryType.Meetup,
    val memoryNote: String = "",
    val memoryPhotoUri: String? = null,
    val memoryLocation: String = "",
    val memoryLocationLatitude: Double? = null,
    val memoryLocationLongitude: Double? = null,
    val isSavingMemory: Boolean = false,
    val selectedAppointmentContactIds: Set<String> = emptySet(),
    val appointmentContactQuery: String = "",
    val appointmentLocation: String = "",
    val appointmentLocationLatitude: Double? = null,
    val appointmentLocationLongitude: Double? = null,
    val appointmentDateMillis: Long? = null,
    val isAppointmentAllDay: Boolean = false,
    val appointmentTimeHour: Int? = null,
    val appointmentTimeMinute: Int? = null,
    val appointmentNote: String = "",
    val isSavingAppointment: Boolean = false,
    @param:StringRes val contactsErrorMessageRes: Int? = null,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null,
    @param:StringRes val memoryErrorMessageRes: Int? = null,
    @param:StringRes val memorySavedMessageRes: Int? = null,
    @param:StringRes val appointmentErrorMessageRes: Int? = null,
    @param:StringRes val appointmentSavedMessageRes: Int? = null
)

data class ReflectionContactListItem(
    val id: String,
    val name: String
)

enum class ReflectionMood(
    val storageValue: String,
    @param:StringRes val labelRes: Int
) {
    VeryUnpleasant("Rất khó chịu", R.string.add_reflection_mood_very_unpleasant),
    Unpleasant("Khó chịu", R.string.add_reflection_mood_unpleasant),
    Neutral("Bình thường", R.string.add_reflection_mood_neutral),
    Pleasant("Dễ chịu", R.string.add_reflection_mood_pleasant),
    VeryPleasant("Rất dễ chịu", R.string.add_reflection_mood_very_pleasant)
}

val ReflectionMoodOptions = listOf(
    ReflectionMood.VeryUnpleasant,
    ReflectionMood.Unpleasant,
    ReflectionMood.Neutral,
    ReflectionMood.Pleasant,
    ReflectionMood.VeryPleasant
)

enum class ReflectionFeeling(
    val storageValue: String,
    @param:StringRes val labelRes: Int
) {
    Happy("Vui", R.string.add_reflection_feeling_happy),
    Sad("Buồn", R.string.add_reflection_feeling_sad),
    Guilty("Mặc cảm", R.string.add_reflection_feeling_guilty),
    Anxious("Lo lắng", R.string.add_reflection_feeling_anxious),
    Grateful("Biết ơn", R.string.add_reflection_feeling_grateful),
    Relieved("Nhẹ nhõm", R.string.add_reflection_feeling_relieved),
    Lonely("Cô đơn", R.string.add_reflection_feeling_lonely),
    Peaceful("Bình yên", R.string.add_reflection_feeling_peaceful)
}

val ReflectionFeelingOptions = listOf(
    ReflectionFeeling.Happy,
    ReflectionFeeling.Sad,
    ReflectionFeeling.Guilty,
    ReflectionFeeling.Anxious,
    ReflectionFeeling.Grateful,
    ReflectionFeeling.Relieved,
    ReflectionFeeling.Lonely,
    ReflectionFeeling.Peaceful
)

enum class ReflectionSource(
    val storageValue: String,
    @param:StringRes val labelRes: Int
) {
    Family("Gia đình", R.string.add_reflection_source_family),
    Friends("Bạn bè", R.string.add_reflection_source_friends),
    CloseFriend("Bạn thân", R.string.add_reflection_source_close_friend),
    Classmate("Bạn học", R.string.add_reflection_source_classmate)
}

val ReflectionSourceOptions = listOf(
    ReflectionSource.Family,
    ReflectionSource.Friends,
    ReflectionSource.CloseFriend,
    ReflectionSource.Classmate
)

enum class MemoryType(
    val storageValue: String,
    @param:StringRes val labelRes: Int
) {
    Message("message", R.string.add_interaction_type_message),
    Call("call", R.string.add_interaction_type_call),
    Meetup("meetup", R.string.add_interaction_type_meetup),
    Support("support", R.string.add_interaction_type_support),
    Other("other", R.string.add_interaction_type_other)
}

val MemoryTypeOptions = listOf(
    MemoryType.Meetup,
    MemoryType.Support,
    MemoryType.Other
)

class AddHubViewModel(
    private val relationshipRepository: RelationshipRepository,
    private val reflectionRepository: ReflectionRepository,
    private val memoryRepository: MemoryRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {
    private val addHubDraft = MutableStateFlow(AddHubDraft())
    private val contactResults = relationshipRepository.observeRelationships()
        .map { relationships ->
            ReflectionContactsResult(
                contacts = relationships
                    .sortedBy { relationship -> relationship.name.lowercase() }
                    .map { relationship -> relationship.toReflectionContactListItem() }
            )
        }
        .catch {
            emit(ReflectionContactsResult(errorMessageRes = R.string.add_reflection_contacts_error))
        }

    val uiState: StateFlow<AddHubUiState> = combine(
        contactResults,
        addHubDraft
    ) { result, draft ->
        val visibleContactIds = result.contacts.map { contact -> contact.id }.toSet()
        val selectedContactIds = draft.selectedContactIds.intersect(visibleContactIds)
        val selectedMemoryContactIds = draft.selectedMemoryContactIds.intersect(visibleContactIds)

        AddHubUiState(
            isLoadingContacts = false,
            contacts = result.contacts,
            selectedContactIds = selectedContactIds,
            selectedMood = draft.selectedMood,
            selectedFeelings = draft.selectedFeelings,
            selectedSources = draft.selectedSources,
            isSavingReflection = draft.isSavingReflection,
            selectedMemoryContactIds = selectedMemoryContactIds,
            memoryContactQuery = draft.memoryContactQuery,
            memoryTitle = draft.memoryTitle,
            selectedMemoryType = draft.selectedMemoryType,
            memoryNote = draft.memoryNote,
            memoryPhotoUri = draft.memoryPhotoUri,
            memoryLocation = draft.memoryLocation,
            memoryLocationLatitude = draft.memoryLocationLatitude,
            memoryLocationLongitude = draft.memoryLocationLongitude,
            isSavingMemory = draft.isSavingMemory,
            selectedAppointmentContactIds = draft.selectedAppointmentContactIds.intersect(visibleContactIds),
            appointmentContactQuery = draft.appointmentContactQuery,
            appointmentLocation = draft.appointmentLocation,
            appointmentLocationLatitude = draft.appointmentLocationLatitude,
            appointmentLocationLongitude = draft.appointmentLocationLongitude,
            appointmentDateMillis = draft.appointmentDateMillis,
            isAppointmentAllDay = draft.isAppointmentAllDay,
            appointmentTimeHour = draft.appointmentTimeHour,
            appointmentTimeMinute = draft.appointmentTimeMinute,
            appointmentNote = draft.appointmentNote,
            isSavingAppointment = draft.isSavingAppointment,
            contactsErrorMessageRes = result.errorMessageRes,
            reflectionErrorMessageRes = draft.reflectionErrorMessageRes,
            reflectionSavedMessageRes = draft.reflectionSavedMessageRes,
            memoryErrorMessageRes = draft.memoryErrorMessageRes,
            memorySavedMessageRes = draft.memorySavedMessageRes,
            appointmentErrorMessageRes = draft.appointmentErrorMessageRes,
            appointmentSavedMessageRes = draft.appointmentSavedMessageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddHubUiState()
    )

    fun startReflection() {
        addHubDraft.update { current ->
            current.copy(
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onContactToggled(contactId: String) {
        addHubDraft.update { current ->
            current.copy(
                selectedContactIds = current.selectedContactIds.toggle(contactId),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onMoodSelected(mood: ReflectionMood) {
        addHubDraft.update { current ->
            current.copy(
                selectedMood = mood,
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onFeelingToggled(feeling: ReflectionFeeling) {
        addHubDraft.update { current ->
            current.copy(
                selectedFeelings = current.selectedFeelings.toggle(feeling),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onSourceToggled(source: ReflectionSource) {
        addHubDraft.update { current ->
            current.copy(
                selectedSources = current.selectedSources.toggle(source),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun completeReflection() {
        val currentState = uiState.value
        if (currentState.isSavingReflection) return

        val selectedContacts = currentState.contacts
            .filter { contact -> contact.id in currentState.selectedContactIds }
            .map { contact ->
                ReflectionContactSnapshot(
                    id = contact.id,
                    name = contact.name
                )
            }
        val selectedFeelings = ReflectionFeelingOptions
            .filter { feeling -> feeling in currentState.selectedFeelings }
            .map { feeling -> feeling.storageValue }
        val selectedSources = ReflectionSourceOptions
            .filter { source -> source in currentState.selectedSources }
            .map { source -> source.storageValue }

        viewModelScope.launch {
            addHubDraft.update { current ->
                current.copy(
                    isSavingReflection = true,
                    reflectionErrorMessageRes = null,
                    reflectionSavedMessageRes = null
                )
            }

            runCatching {
                withTimeout(SaveTimeoutMillis) {
                    reflectionRepository.addReflection(
                        NewReflectionRequest(
                            interactedContacts = selectedContacts,
                            mood = currentState.selectedMood.storageValue,
                            feelings = selectedFeelings,
                            sources = selectedSources,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }.onSuccess {
                addHubDraft.update { current ->
                    current.copy(
                        selectedContactIds = emptySet(),
                        selectedMood = ReflectionMood.Neutral,
                        selectedFeelings = emptySet(),
                        selectedSources = emptySet(),
                        isSavingReflection = false,
                        reflectionSavedMessageRes = R.string.add_reflection_saved,
                        reflectionErrorMessageRes = null
                    )
                }
            }.onFailure { throwable ->
                addHubDraft.update { current ->
                    current.copy(
                        isSavingReflection = false,
                        reflectionErrorMessageRes = throwable.toReflectionErrorMessageRes(),
                        reflectionSavedMessageRes = null
                    )
                }
            }
        }
    }

    fun startMemory() {
        addHubDraft.update { current ->
            current.copy(
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryContactToggled(contactId: String) {
        addHubDraft.update { current ->
            current.copy(
                selectedMemoryContactIds = current.selectedMemoryContactIds.toggle(contactId),
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryContactQueryChanged(query: String) {
        addHubDraft.update { current ->
            current.copy(
                memoryContactQuery = query,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryTitleChanged(title: String) {
        addHubDraft.update { current ->
            current.copy(
                memoryTitle = title,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryTypeSelected(type: MemoryType) {
        addHubDraft.update { current ->
            current.copy(
                selectedMemoryType = type,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryNoteChanged(note: String) {
        addHubDraft.update { current ->
            current.copy(
                memoryNote = note,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryPhotoChanged(photoUri: String?) {
        addHubDraft.update { current ->
            current.copy(
                memoryPhotoUri = photoUri,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryLocationChanged(location: String) {
        addHubDraft.update { current ->
            current.copy(
                memoryLocation = location,
                memoryLocationLatitude = null,
                memoryLocationLongitude = null,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun onMemoryLocationSelected(
        location: String,
        latitude: Double,
        longitude: Double
    ) {
        addHubDraft.update { current ->
            current.copy(
                memoryLocation = location,
                memoryLocationLatitude = latitude,
                memoryLocationLongitude = longitude,
                memoryErrorMessageRes = null,
                memorySavedMessageRes = null
            )
        }
    }

    fun saveMemory() {
        val currentState = uiState.value
        if (currentState.isSavingMemory) return

        val selectedContacts = currentState.contacts
            .filter { contact -> contact.id in currentState.selectedMemoryContactIds }
        val title = currentState.memoryTitle.trim()

        val validationErrorRes = when {
            selectedContacts.isEmpty() -> R.string.add_interaction_contact_required
            title.isBlank() -> R.string.add_memory_title_required
            else -> null
        }

        if (validationErrorRes != null) {
            addHubDraft.update { current ->
                current.copy(
                    memoryErrorMessageRes = validationErrorRes,
                    memorySavedMessageRes = null
                )
            }
            return
        }

        viewModelScope.launch {
            addHubDraft.update { current ->
                current.copy(
                    isSavingMemory = true,
                    memoryErrorMessageRes = null,
                    memorySavedMessageRes = null
                )
            }

            runCatching {
                withTimeout(SaveTimeoutMillis) {
                    val createdAtMillis = System.currentTimeMillis()
                    selectedContacts.forEach { selectedContact ->
                        memoryRepository.addMemory(
                            NewMemoryRequest(
                                contactId = selectedContact.id,
                                contactName = selectedContact.name,
                                title = title,
                                type = currentState.selectedMemoryType.storageValue,
                                note = currentState.memoryNote.trim().takeIf { it.isNotEmpty() },
                                photoUri = currentState.memoryPhotoUri,
                                location = currentState.memoryLocation.trim().takeIf { it.isNotEmpty() },
                                locationLatitude = currentState.memoryLocationLatitude,
                                locationLongitude = currentState.memoryLocationLongitude,
                                createdAtMillis = createdAtMillis
                            )
                        )
                    }
                }
            }.onSuccess {
                addHubDraft.update { current ->
                        current.copy(
                            selectedMemoryContactIds = emptySet(),
                            memoryContactQuery = "",
                            memoryTitle = "",
                            selectedMemoryType = MemoryType.Meetup,
                            memoryNote = "",
                            memoryPhotoUri = null,
                            memoryLocation = "",
                            memoryLocationLatitude = null,
                            memoryLocationLongitude = null,
                            isSavingMemory = false,
                            memoryErrorMessageRes = null,
                            memorySavedMessageRes = R.string.add_interaction_saved
                    )
                }
            }.onFailure { throwable ->
                addHubDraft.update { current ->
                    current.copy(
                        isSavingMemory = false,
                        memoryErrorMessageRes = throwable.toMemoryErrorMessageRes(),
                        memorySavedMessageRes = null
                    )
                }
            }
        }
    }

    fun startAppointment() {
        addHubDraft.update { current ->
            current.copy(
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentContactToggled(contactId: String) {
        addHubDraft.update { current ->
            current.copy(
                selectedAppointmentContactIds = current.selectedAppointmentContactIds.toggle(contactId),
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentContactQueryChanged(query: String) {
        addHubDraft.update { current ->
            current.copy(
                appointmentContactQuery = query,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentLocationChanged(location: String) {
        addHubDraft.update { current ->
            current.copy(
                appointmentLocation = location,
                appointmentLocationLatitude = null,
                appointmentLocationLongitude = null,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentLocationSelected(
        location: String,
        latitude: Double,
        longitude: Double
    ) {
        addHubDraft.update { current ->
            current.copy(
                appointmentLocation = location,
                appointmentLocationLatitude = latitude,
                appointmentLocationLongitude = longitude,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentDateSelected(dateMillis: Long) {
        addHubDraft.update { current ->
            current.copy(
                appointmentDateMillis = dateMillis,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentAllDayToggled(isAllDay: Boolean) {
        addHubDraft.update { current ->
            current.copy(
                isAppointmentAllDay = isAllDay,
                appointmentTimeHour = if (isAllDay) null else current.appointmentTimeHour,
                appointmentTimeMinute = if (isAllDay) null else current.appointmentTimeMinute,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentNoteChanged(note: String) {
        addHubDraft.update { current ->
            current.copy(
                appointmentNote = note,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentTimeSelected(hour: Int, minute: Int) {
        addHubDraft.update { current ->
            current.copy(
                appointmentTimeHour = hour,
                appointmentTimeMinute = minute,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun saveAppointment() {
        val currentState = uiState.value
        if (currentState.isSavingAppointment) return

        val selectedContacts = currentState.contacts
            .filter { contact -> contact.id in currentState.selectedAppointmentContactIds }
        val name = selectedContacts.joinToString(separator = ", ") { it.name }
        val location = currentState.appointmentLocation.trim()
        val locationLatitude = currentState.appointmentLocationLatitude
        val locationLongitude = currentState.appointmentLocationLongitude
        val dateMillis = currentState.appointmentDateMillis
        val isAllDay = currentState.isAppointmentAllDay
        val hour = currentState.appointmentTimeHour
        val minute = currentState.appointmentTimeMinute

        val validationErrorRes = when {
            selectedContacts.isEmpty() -> R.string.add_appointment_name_required
            location.isEmpty() -> R.string.add_appointment_location_required
            locationLatitude == null || locationLongitude == null ->
                R.string.add_appointment_location_select_required
            dateMillis == null -> R.string.add_appointment_date_required
            !isAllDay && (hour == null || minute == null) -> R.string.add_appointment_time_required
            else -> null
        }

        if (validationErrorRes != null) {
            addHubDraft.update { current ->
                current.copy(
                    appointmentErrorMessageRes = validationErrorRes,
                    appointmentSavedMessageRes = null
                )
            }
            return
        }
        val appointmentDateMillis = if (isAllDay) {
            dateMillis ?: return
        } else {
            combineAppointmentDateTimeMillis(
                dateMillis = dateMillis ?: return,
                hour = hour ?: return,
                minute = minute ?: return
            )
        }
        val selectedLocationLatitude = locationLatitude ?: return
        val selectedLocationLongitude = locationLongitude ?: return

        viewModelScope.launch {
            addHubDraft.update { current ->
                current.copy(
                    isSavingAppointment = true,
                    appointmentErrorMessageRes = null,
                    appointmentSavedMessageRes = null
                )
            }

            runCatching {
                withTimeout(SaveTimeoutMillis) {
                    appointmentRepository.addAppointment(
                        NewAppointmentRequest(
                            name = name,
                            participantContactIds = selectedContacts.map { it.id },
                            participantContactNames = selectedContacts.map { it.name },
                            location = location,
                            locationLatitude = selectedLocationLatitude,
                            locationLongitude = selectedLocationLongitude,
                            appointmentDateMillis = appointmentDateMillis,
                            note = currentState.appointmentNote.trim().takeIf { it.isNotEmpty() },
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }.onSuccess {
                addHubDraft.update { current ->
                    current.copy(
                        selectedAppointmentContactIds = emptySet(),
                        appointmentContactQuery = "",
                        appointmentLocation = "",
                        appointmentLocationLatitude = null,
                        appointmentLocationLongitude = null,
                        appointmentDateMillis = null,
                        isAppointmentAllDay = false,
                        appointmentTimeHour = null,
                        appointmentTimeMinute = null,
                        appointmentNote = "",
                        isSavingAppointment = false,
                        appointmentErrorMessageRes = null,
                        appointmentSavedMessageRes = R.string.add_appointment_saved
                    )
                }
            }.onFailure { throwable ->
                addHubDraft.update { current ->
                    current.copy(
                        isSavingAppointment = false,
                        appointmentErrorMessageRes = throwable.toAppointmentErrorMessageRes(),
                        appointmentSavedMessageRes = null
                    )
                }
            }
        }
    }

    fun clearSavedFeedback() {
        addHubDraft.update { current ->
            current.copy(
                reflectionSavedMessageRes = null,
                memorySavedMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    companion object {
        private const val SaveTimeoutMillis = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddHubViewModel(
                    relationshipRepository = RelationshipRepositoryProvider.repository,
                    reflectionRepository = ReflectionRepositoryProvider.repository,
                    memoryRepository = MemoryRepositoryProvider.repository,
                    appointmentRepository = AppointmentRepositoryProvider.repository
                )
            }
        }
    }
}

private data class AddHubDraft(
    val selectedContactIds: Set<String> = emptySet(),
    val selectedMood: ReflectionMood = ReflectionMood.Neutral,
    val selectedFeelings: Set<ReflectionFeeling> = emptySet(),
    val selectedSources: Set<ReflectionSource> = emptySet(),
    val isSavingReflection: Boolean = false,
    val selectedMemoryContactIds: Set<String> = emptySet(),
    val memoryContactQuery: String = "",
    val memoryTitle: String = "",
    val selectedMemoryType: MemoryType = MemoryType.Meetup,
    val memoryNote: String = "",
    val memoryPhotoUri: String? = null,
    val memoryLocation: String = "",
    val memoryLocationLatitude: Double? = null,
    val memoryLocationLongitude: Double? = null,
    val isSavingMemory: Boolean = false,
    val selectedAppointmentContactIds: Set<String> = emptySet(),
    val appointmentContactQuery: String = "",
    val appointmentLocation: String = "",
    val appointmentLocationLatitude: Double? = null,
    val appointmentLocationLongitude: Double? = null,
    val appointmentDateMillis: Long? = null,
    val isAppointmentAllDay: Boolean = false,
    val appointmentTimeHour: Int? = null,
    val appointmentTimeMinute: Int? = null,
    val appointmentNote: String = "",
    val isSavingAppointment: Boolean = false,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null,
    @param:StringRes val memoryErrorMessageRes: Int? = null,
    @param:StringRes val memorySavedMessageRes: Int? = null,
    @param:StringRes val appointmentErrorMessageRes: Int? = null,
    @param:StringRes val appointmentSavedMessageRes: Int? = null
)

private data class ReflectionContactsResult(
    val contacts: List<ReflectionContactListItem> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null
)

private fun RelationshipProfile.toReflectionContactListItem(): ReflectionContactListItem {
    return ReflectionContactListItem(
        id = id,
        name = name
    )
}

private fun combineAppointmentDateTimeMillis(dateMillis: Long, hour: Int, minute: Int): Long {
    val localDate = Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    return localDate
        .atTime(hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun Throwable.toReflectionErrorMessageRes(): Int {
    return when (this) {
        is FirebaseConnectionException -> R.string.add_reflection_connection_error
        is FirebaseFirestoreException -> {
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                R.string.add_reflection_permission_error
            } else {
                R.string.add_reflection_save_error
            }
        }
        is TimeoutCancellationException -> R.string.add_reflection_save_timeout
        else -> R.string.add_reflection_save_error
    }
}

private fun Throwable.toMemoryErrorMessageRes(): Int {
    return when (this) {
        is FirebaseConnectionException -> R.string.add_interaction_connection_error
        is FirebaseFirestoreException -> {
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                R.string.add_interaction_permission_error
            } else {
                R.string.add_interaction_save_error
            }
        }
        is TimeoutCancellationException -> R.string.add_interaction_save_timeout
        else -> R.string.add_interaction_save_error
    }
}

private fun Throwable.toAppointmentErrorMessageRes(): Int {
    return when (this) {
        is FirebaseConnectionException -> R.string.add_appointment_connection_error
        is FirebaseFirestoreException -> {
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                R.string.add_appointment_permission_error
            } else {
                R.string.add_appointment_save_error
            }
        }
        is TimeoutCancellationException -> R.string.add_appointment_save_timeout
        else -> R.string.add_appointment_save_error
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}
