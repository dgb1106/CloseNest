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
import com.example.closenest.features.homepage.model.NewInteractionLogRequest
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.example.closenest.features.homepage.model.ReflectionContactSnapshot
import com.example.closenest.features.homepage.repository.AppointmentRepository
import com.example.closenest.features.homepage.repository.AppointmentRepositoryProvider
import com.example.closenest.features.homepage.repository.InteractionLogRepository
import com.example.closenest.features.homepage.repository.InteractionLogRepositoryProvider
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

data class AddHubUiState(
    val isLoadingContacts: Boolean = true,
    val contacts: List<ReflectionContactListItem> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val selectedMood: ReflectionMood = ReflectionMood.Neutral,
    val selectedFeelings: Set<ReflectionFeeling> = emptySet(),
    val selectedSources: Set<ReflectionSource> = emptySet(),
    val isSavingReflection: Boolean = false,
    val selectedInteractionContactId: String? = null,
    val selectedInteractionType: InteractionLogType = InteractionLogType.Message,
    val interactionNote: String = "",
    val isSavingInteractionLog: Boolean = false,
    val appointmentName: String = "",
    val appointmentLocation: String = "",
    val appointmentDateMillis: Long? = null,
    val isSavingAppointment: Boolean = false,
    @param:StringRes val contactsErrorMessageRes: Int? = null,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null,
    @param:StringRes val interactionErrorMessageRes: Int? = null,
    @param:StringRes val interactionSavedMessageRes: Int? = null,
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

enum class InteractionLogType(
    val storageValue: String,
    @param:StringRes val labelRes: Int
) {
    Message("message", R.string.add_interaction_type_message),
    Call("call", R.string.add_interaction_type_call),
    Meetup("meetup", R.string.add_interaction_type_meetup),
    Support("support", R.string.add_interaction_type_support),
    Other("other", R.string.add_interaction_type_other)
}

val InteractionLogTypeOptions = listOf(
    InteractionLogType.Message,
    InteractionLogType.Call,
    InteractionLogType.Meetup,
    InteractionLogType.Support,
    InteractionLogType.Other
)

class AddHubViewModel(
    private val relationshipRepository: RelationshipRepository,
    private val reflectionRepository: ReflectionRepository,
    private val interactionLogRepository: InteractionLogRepository,
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
        val selectedInteractionContactId = draft.selectedInteractionContactId
            ?.takeIf { contactId -> contactId in visibleContactIds }

        AddHubUiState(
            isLoadingContacts = false,
            contacts = result.contacts,
            selectedContactIds = selectedContactIds,
            selectedMood = draft.selectedMood,
            selectedFeelings = draft.selectedFeelings,
            selectedSources = draft.selectedSources,
            isSavingReflection = draft.isSavingReflection,
            selectedInteractionContactId = selectedInteractionContactId,
            selectedInteractionType = draft.selectedInteractionType,
            interactionNote = draft.interactionNote,
            isSavingInteractionLog = draft.isSavingInteractionLog,
            appointmentName = draft.appointmentName,
            appointmentLocation = draft.appointmentLocation,
            appointmentDateMillis = draft.appointmentDateMillis,
            isSavingAppointment = draft.isSavingAppointment,
            contactsErrorMessageRes = result.errorMessageRes,
            reflectionErrorMessageRes = draft.reflectionErrorMessageRes,
            reflectionSavedMessageRes = draft.reflectionSavedMessageRes,
            interactionErrorMessageRes = draft.interactionErrorMessageRes,
            interactionSavedMessageRes = draft.interactionSavedMessageRes,
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

    fun startInteractionLog() {
        addHubDraft.update { current ->
            current.copy(
                interactionErrorMessageRes = null,
                interactionSavedMessageRes = null
            )
        }
    }

    fun onInteractionContactSelected(contactId: String) {
        addHubDraft.update { current ->
            current.copy(
                selectedInteractionContactId = contactId,
                interactionErrorMessageRes = null,
                interactionSavedMessageRes = null
            )
        }
    }

    fun onInteractionTypeSelected(type: InteractionLogType) {
        addHubDraft.update { current ->
            current.copy(
                selectedInteractionType = type,
                interactionErrorMessageRes = null,
                interactionSavedMessageRes = null
            )
        }
    }

    fun onInteractionNoteChanged(note: String) {
        addHubDraft.update { current ->
            current.copy(
                interactionNote = note,
                interactionErrorMessageRes = null,
                interactionSavedMessageRes = null
            )
        }
    }

    fun saveInteractionLog() {
        val currentState = uiState.value
        if (currentState.isSavingInteractionLog) return

        val selectedContact = currentState.contacts
            .firstOrNull { contact -> contact.id == currentState.selectedInteractionContactId }

        if (selectedContact == null) {
            addHubDraft.update { current ->
                current.copy(
                    interactionErrorMessageRes = R.string.add_interaction_contact_required,
                    interactionSavedMessageRes = null
                )
            }
            return
        }

        viewModelScope.launch {
            addHubDraft.update { current ->
                current.copy(
                    isSavingInteractionLog = true,
                    interactionErrorMessageRes = null,
                    interactionSavedMessageRes = null
                )
            }

            runCatching {
                withTimeout(SaveTimeoutMillis) {
                    interactionLogRepository.addInteractionLog(
                        NewInteractionLogRequest(
                            contactId = selectedContact.id,
                            contactName = selectedContact.name,
                            type = currentState.selectedInteractionType.storageValue,
                            note = currentState.interactionNote.trim().takeIf { note -> note.isNotEmpty() },
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }.onSuccess {
                addHubDraft.update { current ->
                    current.copy(
                        selectedInteractionContactId = null,
                        selectedInteractionType = InteractionLogType.Message,
                        interactionNote = "",
                        isSavingInteractionLog = false,
                        interactionErrorMessageRes = null,
                        interactionSavedMessageRes = R.string.add_interaction_saved
                    )
                }
            }.onFailure { throwable ->
                addHubDraft.update { current ->
                    current.copy(
                        isSavingInteractionLog = false,
                        interactionErrorMessageRes = throwable.toInteractionErrorMessageRes(),
                        interactionSavedMessageRes = null
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

    fun onAppointmentNameChanged(name: String) {
        addHubDraft.update { current ->
            current.copy(
                appointmentName = name,
                appointmentErrorMessageRes = null,
                appointmentSavedMessageRes = null
            )
        }
    }

    fun onAppointmentLocationChanged(location: String) {
        addHubDraft.update { current ->
            current.copy(
                appointmentLocation = location,
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

    fun saveAppointment() {
        val currentState = uiState.value
        if (currentState.isSavingAppointment) return

        val name = currentState.appointmentName.trim()
        val location = currentState.appointmentLocation.trim()
        val dateMillis = currentState.appointmentDateMillis

        val validationErrorRes = when {
            name.isEmpty() -> R.string.add_appointment_name_required
            location.isEmpty() -> R.string.add_appointment_location_required
            dateMillis == null -> R.string.add_appointment_date_required
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
        val appointmentDateMillis = dateMillis ?: return

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
                            location = location,
                            appointmentDateMillis = appointmentDateMillis,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }.onSuccess {
                addHubDraft.update { current ->
                    current.copy(
                        appointmentName = "",
                        appointmentLocation = "",
                        appointmentDateMillis = null,
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

    companion object {
        private const val SaveTimeoutMillis = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddHubViewModel(
                    relationshipRepository = RelationshipRepositoryProvider.repository,
                    reflectionRepository = ReflectionRepositoryProvider.repository,
                    interactionLogRepository = InteractionLogRepositoryProvider.repository,
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
    val selectedInteractionContactId: String? = null,
    val selectedInteractionType: InteractionLogType = InteractionLogType.Message,
    val interactionNote: String = "",
    val isSavingInteractionLog: Boolean = false,
    val appointmentName: String = "",
    val appointmentLocation: String = "",
    val appointmentDateMillis: Long? = null,
    val isSavingAppointment: Boolean = false,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null,
    @param:StringRes val interactionErrorMessageRes: Int? = null,
    @param:StringRes val interactionSavedMessageRes: Int? = null,
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

private fun Throwable.toInteractionErrorMessageRes(): Int {
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
