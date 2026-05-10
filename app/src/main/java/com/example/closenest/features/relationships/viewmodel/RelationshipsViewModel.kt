package com.example.closenest.features.relationships.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.features.relationships.model.AttentionStatus
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class RelationshipsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTag: RelationshipTag? = null,
    val relationships: List<RelationshipListItem> = emptyList(),
    val totalRelationships: Int = 0,
    val relationshipsNeedingAttention: Int = 0,
    val errorMessage: String? = null
)

data class RelationshipListItem(
    val id: String,
    val name: String,
    val initials: String,
    val tag: RelationshipTag,
    val phoneNumber: String?,
    val email: String?,
    val interests: List<String>,
    val notes: String?,
    val priority: RelationshipPriority,
    val daysSinceLastInteraction: Long?,
    val lastInteractionLabelRes: Int?,
    val attentionStatus: AttentionStatus,
    val suggestedAction: SuggestedAction
)

enum class SuggestedAction(val labelRes: Int) {
    SendCheckIn(R.string.suggested_action_check_in),
    MakeCall(R.string.suggested_action_call),
    PlanMeet(R.string.suggested_action_meet)
}

class RelationshipsViewModel(
    private val repository: RelationshipRepository
) : ViewModel() {
    private val filters = MutableStateFlow(RelationshipFilters())

    val uiState: StateFlow<RelationshipsUiState> = combine(
        repository.observeRelationships(),
        filters
    ) { relationships, currentFilters ->
        val filteredRelationships = relationships
            .sortedWith(
                compareBy<RelationshipProfile> { relationship ->
                    relationship.name.lowercase()
                }.thenByDescending { relationship ->
                    relationship.priority.value
                }
            )
            .filter { relationship ->
                val matchesQuery = currentFilters.searchQuery.isBlank() ||
                    relationship.name.contains(currentFilters.searchQuery, ignoreCase = true) ||
                    relationship.email.orEmpty().contains(currentFilters.searchQuery, ignoreCase = true) ||
                    relationship.phoneNumber.orEmpty().contains(currentFilters.searchQuery, ignoreCase = true)
                val matchesTag = currentFilters.selectedTag == null || relationship.tag == currentFilters.selectedTag
                matchesQuery && matchesTag
            }
            .map { relationship -> relationship.toListItem() }

        RelationshipsUiState(
            isLoading = false,
            searchQuery = currentFilters.searchQuery,
            selectedTag = currentFilters.selectedTag,
            relationships = filteredRelationships,
            totalRelationships = relationships.size,
            relationshipsNeedingAttention = relationships.count {
                it.toAttentionStatus() == AttentionStatus.NeedsAttention
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RelationshipsUiState()
    )

    fun onSearchQueryChanged(value: String) {
        filters.update { it.copy(searchQuery = value) }
    }

    fun onTagSelected(tag: RelationshipTag?) {
        filters.update { current ->
            current.copy(selectedTag = if (current.selectedTag == tag) null else tag)
        }
    }

    fun clearFilters() {
        filters.value = RelationshipFilters()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RelationshipsViewModel(
                    repository = RelationshipRepositoryProvider.repository
                )
            }
        }
    }
}

private data class RelationshipFilters(
    val searchQuery: String = "",
    val selectedTag: RelationshipTag? = null
)

private fun RelationshipProfile.toListItem(): RelationshipListItem {
    val attentionStatus = toAttentionStatus()
    return RelationshipListItem(
        id = id,
        name = name,
        initials = name.initials(),
        tag = tag,
        phoneNumber = phoneNumber,
        email = email,
        interests = interests,
        notes = notes,
        priority = priority,
        daysSinceLastInteraction = lastInteractionAtMillis?.daysFromNow(),
        lastInteractionLabelRes = lastInteractionType?.labelRes,
        attentionStatus = attentionStatus,
        suggestedAction = when {
            phoneNumber != null && attentionStatus == AttentionStatus.NeedsAttention &&
                priority == RelationshipPriority.High -> SuggestedAction.MakeCall
            tag == RelationshipTag.Partner || tag == RelationshipTag.CloseFriend ->
                SuggestedAction.PlanMeet
            else -> SuggestedAction.SendCheckIn
        }
    )
}

private fun RelationshipProfile.toAttentionStatus(): AttentionStatus {
    val daysSinceLastInteraction = lastInteractionAtMillis?.daysFromNow() ?: return AttentionStatus.NeedsAttention
    val threshold = (tag.followUpThresholdDays - priority.value).coerceAtLeast(3)

    return when {
        daysSinceLastInteraction <= 3 -> AttentionStatus.RecentlyConnected
        daysSinceLastInteraction <= threshold -> AttentionStatus.Warm
        else -> AttentionStatus.NeedsAttention
    }
}

private fun Long.daysFromNow(): Long {
    val elapsedMillis = System.currentTimeMillis() - this
    return TimeUnit.MILLISECONDS.toDays(elapsedMillis).coerceAtLeast(0)
}

private fun String.initials(): String = split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString(separator = "") { part ->
        part.first().uppercase()
    }
