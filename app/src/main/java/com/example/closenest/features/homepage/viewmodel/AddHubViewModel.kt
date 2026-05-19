package com.example.closenest.features.homepage.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.core.network.FirebaseConnectionException
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.example.closenest.features.homepage.model.ReflectionContactSnapshot
import com.example.closenest.features.homepage.repository.ReflectionRepository
import com.example.closenest.features.homepage.repository.ReflectionRepositoryProvider
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

data class AddHubUiState(
    val isLoadingContacts: Boolean = true,
    val contacts: List<ReflectionContactListItem> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val showReflectionSheet: Boolean = false,
    val selectedMood: ReflectionMood = ReflectionMood.Neutral,
    val selectedFeelings: Set<ReflectionFeeling> = emptySet(),
    val selectedSources: Set<ReflectionSource> = emptySet(),
    val isSavingReflection: Boolean = false,
    @param:StringRes val contactsErrorMessageRes: Int? = null,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null
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

class AddHubViewModel(
    private val relationshipRepository: RelationshipRepository,
    private val reflectionRepository: ReflectionRepository
) : ViewModel() {
    private val reflectionDraft = MutableStateFlow(ReflectionDraft())
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
        reflectionDraft
    ) { result, draft ->
        val visibleContactIds = result.contacts.map { contact -> contact.id }.toSet()
        val selectedContactIds = draft.selectedContactIds.intersect(visibleContactIds)

        AddHubUiState(
            isLoadingContacts = false,
            contacts = result.contacts,
            selectedContactIds = selectedContactIds,
            showReflectionSheet = draft.showReflectionSheet,
            selectedMood = draft.selectedMood,
            selectedFeelings = draft.selectedFeelings,
            selectedSources = draft.selectedSources,
            isSavingReflection = draft.isSavingReflection,
            contactsErrorMessageRes = result.errorMessageRes,
            reflectionErrorMessageRes = draft.reflectionErrorMessageRes,
            reflectionSavedMessageRes = draft.reflectionSavedMessageRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddHubUiState()
    )

    fun onContactToggled(contactId: String) {
        reflectionDraft.update { current ->
            current.copy(
                selectedContactIds = current.selectedContactIds.toggle(contactId),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun openReflectionSheet() {
        reflectionDraft.update { current ->
            current.copy(
                showReflectionSheet = true,
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun closeReflectionSheet() {
        reflectionDraft.update { current ->
            current.copy(
                showReflectionSheet = false,
                isSavingReflection = false
            )
        }
    }

    fun onMoodSelected(mood: ReflectionMood) {
        reflectionDraft.update { current ->
            current.copy(
                selectedMood = mood,
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onFeelingToggled(feeling: ReflectionFeeling) {
        reflectionDraft.update { current ->
            current.copy(
                selectedFeelings = current.selectedFeelings.toggle(feeling),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun onSourceToggled(source: ReflectionSource) {
        reflectionDraft.update { current ->
            current.copy(
                selectedSources = current.selectedSources.toggle(source),
                reflectionSavedMessageRes = null,
                reflectionErrorMessageRes = null
            )
        }
    }

    fun completeReflection() {
        val currentState = uiState.value
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
            reflectionDraft.update { current ->
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
                reflectionDraft.update { current ->
                    current.copy(
                        selectedContactIds = emptySet(),
                        showReflectionSheet = false,
                        selectedMood = ReflectionMood.Neutral,
                        selectedFeelings = emptySet(),
                        selectedSources = emptySet(),
                        isSavingReflection = false,
                        reflectionSavedMessageRes = R.string.add_reflection_saved,
                        reflectionErrorMessageRes = null
                    )
                }
            }.onFailure { throwable ->
                reflectionDraft.update { current ->
                    current.copy(
                        isSavingReflection = false,
                        reflectionErrorMessageRes = when (throwable) {
                            is FirebaseConnectionException -> R.string.add_reflection_connection_error
                            is FirebaseFirestoreException -> {
                                if (throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                    R.string.add_reflection_permission_error
                                } else {
                                    R.string.add_reflection_save_error
                                }
                            }
                            is TimeoutCancellationException -> R.string.add_reflection_save_timeout
                            else -> R.string.add_reflection_save_error
                        },
                        reflectionSavedMessageRes = null
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
                    reflectionRepository = ReflectionRepositoryProvider.repository
                )
            }
        }
    }
}

private data class ReflectionDraft(
    val selectedContactIds: Set<String> = emptySet(),
    val showReflectionSheet: Boolean = false,
    val selectedMood: ReflectionMood = ReflectionMood.Neutral,
    val selectedFeelings: Set<ReflectionFeeling> = emptySet(),
    val selectedSources: Set<ReflectionSource> = emptySet(),
    val isSavingReflection: Boolean = false,
    @param:StringRes val reflectionErrorMessageRes: Int? = null,
    @param:StringRes val reflectionSavedMessageRes: Int? = null
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

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}
