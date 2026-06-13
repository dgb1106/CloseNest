package com.example.closenest.features.relationships

import com.example.closenest.R
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.viewmodel.RelationshipsViewModel
import com.example.closenest.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RelationshipsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads relationships sorted by name and exposes total count`() = runTest {
        val repository = FakeRelationshipRepository(
            listOf(
                relationship(id = "2", name = "Bao Nam", priority = RelationshipPriority.High),
                relationship(id = "1", name = "An Linh", priority = RelationshipPriority.Low)
            )
        )
        val viewModel = RelationshipsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.totalRelationships)
        assertEquals(listOf("An Linh", "Bao Nam"), viewModel.uiState.value.relationships.map { it.name })
    }

    @Test
    fun `search filters relationships by name email and phone`() = runTest {
        val repository = FakeRelationshipRepository(
            listOf(
                relationship(id = "1", name = "Minh Anh", email = "minh@example.com"),
                relationship(id = "2", name = "Gia Han", phoneNumber = "0988123456")
            )
        )
        val viewModel = RelationshipsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("0988")
        advanceUntilIdle()

        assertEquals(listOf("Gia Han"), viewModel.uiState.value.relationships.map { it.name })
    }

    @Test
    fun `tag selection filters and second tap clears filter`() = runTest {
        val repository = FakeRelationshipRepository(
            listOf(
                relationship(id = "1", name = "Minh Anh", tag = RelationshipTag.CloseFriend),
                relationship(id = "2", name = "Gia Han", tag = RelationshipTag.Family)
            )
        )
        val viewModel = RelationshipsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onTagSelected(RelationshipTag.Family)
        advanceUntilIdle()

        assertEquals(listOf("Gia Han"), viewModel.uiState.value.relationships.map { it.name })

        viewModel.onTagSelected(RelationshipTag.Family)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.relationships.size)
    }

    @Test
    fun `delete relationship calls repository and records deleted id`() = runTest {
        val repository = FakeRelationshipRepository(listOf(relationship(id = "rel-1")))
        val viewModel = RelationshipsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.deleteRelationship("rel-1")
        advanceUntilIdle()

        assertEquals("rel-1", repository.lastDeletedId)
        assertEquals("rel-1", viewModel.uiState.value.deletedRelationshipId)
        assertFalse(viewModel.uiState.value.isDeletingRelationship)
    }

    @Test
    fun `repository stream error exposes sync error state`() = runTest {
        val viewModel = RelationshipsViewModel(ErrorRelationshipRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(R.string.relationships_sync_error, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeRelationshipRepository(
        relationships: List<RelationshipProfile> = emptyList()
    ) : RelationshipRepository {
        private val state = MutableStateFlow(relationships)
        var lastDeletedId: String? = null

        override fun observeRelationships(): Flow<List<RelationshipProfile>> = state

        override suspend fun addRelationship(request: NewRelationshipRequest) = Unit

        override suspend fun deleteRelationship(relationshipId: String) {
            lastDeletedId = relationshipId
            state.value = state.value.filterNot { it.id == relationshipId }
        }

        override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) = Unit
    }

    private class ErrorRelationshipRepository : RelationshipRepository {
        override fun observeRelationships(): Flow<List<RelationshipProfile>> = flow {
            throw IllegalStateException("network failure")
        }

        override suspend fun addRelationship(request: NewRelationshipRequest) = Unit

        override suspend fun deleteRelationship(relationshipId: String) = Unit

        override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) = Unit
    }
}

fun relationship(
    id: String = "rel-1",
    name: String = "Minh Anh",
    tag: RelationshipTag = RelationshipTag.Friend,
    birthdayIso: String? = null,
    phoneNumber: String? = null,
    email: String? = null,
    priority: RelationshipPriority = RelationshipPriority.Medium
) = RelationshipProfile(
    id = id,
    userId = "user-1",
    name = name,
    tag = tag,
    birthdayIso = birthdayIso,
    phoneNumber = phoneNumber,
    email = email,
    interests = listOf("coffee"),
    notes = "note",
    avatarUrl = null,
    priority = priority,
    createdAtMillis = 1_000L,
    updatedAtMillis = 2_000L
)
