package com.example.closenest.features.relationships.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.R
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class RelationshipsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTag: RelationshipTag? = null,
    val relationships: List<RelationshipListItem> = emptyList(),
    val totalRelationships: Int = 0,
    val errorMessageRes: Int? = null
)

data class RelationshipListItem(
    val id: String,
    val name: String,
    val initials: String,
    val tag: RelationshipTag,
    val birthdayIso: String?,
    val phoneNumber: String?,
    val email: String?,
    val interests: List<String>,
    val notes: String?,
    val priority: RelationshipPriority
)

class RelationshipsViewModel(
    private val repository: RelationshipRepository
) : ViewModel() {
    private val filters = MutableStateFlow(RelationshipFilters())
    private val relationshipResults = repository.observeRelationships()
        .map { relationships ->
            RelationshipRepositoryResult(relationships = relationships)
        }
        .catch {
            emit(RelationshipRepositoryResult(errorMessageRes = R.string.relationships_sync_error))
        }

    val uiState: StateFlow<RelationshipsUiState> = combine(
        relationshipResults,
        filters
    ) { repositoryResult, currentFilters ->
        if (repositoryResult.errorMessageRes != null) {
            return@combine RelationshipsUiState(
                isLoading = false,
                searchQuery = currentFilters.searchQuery,
                selectedTag = currentFilters.selectedTag,
                errorMessageRes = repositoryResult.errorMessageRes
            )
        }

        val allRelationships = repositoryResult.relationships
        val filteredRelationships = allRelationships
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
            totalRelationships = allRelationships.size
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

private data class RelationshipRepositoryResult(
    val relationships: List<RelationshipProfile> = emptyList(),
    val errorMessageRes: Int? = null
)

private fun RelationshipProfile.toListItem(): RelationshipListItem {
    return RelationshipListItem(
        id = id,
        name = name,
        initials = name.initials(),
        tag = tag,
        birthdayIso = birthdayIso,
        phoneNumber = phoneNumber,
        email = email,
        interests = interests,
        notes = notes,
        priority = priority
    )
}

private fun String.initials(): String = split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString(separator = "") { part ->
        part.first().uppercase()
    }
