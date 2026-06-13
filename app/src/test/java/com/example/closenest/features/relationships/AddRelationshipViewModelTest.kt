package com.example.closenest.features.relationships

import com.example.closenest.R
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.features.relationships.viewmodel.AddRelationshipViewModel
import com.example.closenest.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddRelationshipViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank name shows validation error and skips repository`() = runTest {
        val repository = FakeRelationshipRepository()
        val viewModel = AddRelationshipViewModel(repository, editRelationshipId = null)

        viewModel.saveRelationship()

        assertTrue(viewModel.uiState.value.nameError)
        assertEquals(R.string.add_relationship_name_error, viewModel.uiState.value.errorMessageRes)
        assertNull(repository.lastAddedRequest)
    }

    @Test
    fun `invalid email shows validation error`() = runTest {
        val repository = FakeRelationshipRepository()
        val viewModel = AddRelationshipViewModel(repository, editRelationshipId = null)
        viewModel.onNameChanged("Minh Anh")
        viewModel.onEmailChanged("invalid-email")

        viewModel.saveRelationship()

        assertTrue(viewModel.uiState.value.emailError)
        assertEquals(R.string.add_relationship_email_error, viewModel.uiState.value.errorMessageRes)
        assertNull(repository.lastAddedRequest)
    }

    @Test
    fun `valid add request is trimmed and saved`() = runTest {
        val repository = FakeRelationshipRepository()
        val viewModel = AddRelationshipViewModel(repository, editRelationshipId = null)
        viewModel.onNameChanged(" Minh Anh ")
        viewModel.onTagChanged(RelationshipTag.CloseFriend)
        viewModel.onEmailChanged(" minh@example.com ")
        viewModel.onPhoneNumberChanged(" 0901234567 ")
        viewModel.onBirthdayChanged("12/11/2003")
        viewModel.onInterestsChanged("Android, Coffee, ")
        viewModel.onNotesChanged(" Likes short calls ")
        viewModel.onPriorityChanged(RelationshipPriority.High)

        viewModel.saveRelationship()

        val request = repository.lastAddedRequest
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals("Minh Anh", request?.name)
        assertEquals(RelationshipTag.CloseFriend, request?.tag)
        assertEquals("minh@example.com", request?.email)
        assertEquals("0901234567", request?.phoneNumber)
        assertEquals("2003-11-12", request?.birthdayIso)
        assertEquals(listOf("Android", "Coffee"), request?.interests)
        assertEquals("Likes short calls", request?.notes)
        assertEquals(RelationshipPriority.High, request?.priority)
    }

    @Test
    fun `edit mode loads existing profile and update saves by id`() = runTest {
        val repository = FakeRelationshipRepository(listOf(relationship(id = "rel-1", name = "Old Name")))
        val viewModel = AddRelationshipViewModel(repository, editRelationshipId = "rel-1")

        assertEquals("Old Name", viewModel.uiState.value.name)

        viewModel.onNameChanged("New Name")
        viewModel.saveRelationship()

        assertEquals("rel-1", repository.lastUpdatedId)
        assertEquals("New Name", repository.lastUpdatedRequest?.name)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `repository failure shows unknown save error without crash`() = runTest {
        val repository = FakeRelationshipRepository(addFailure = IllegalStateException("raw backend failure"))
        val viewModel = AddRelationshipViewModel(repository, editRelationshipId = null)
        viewModel.onNameChanged("Minh Anh")

        viewModel.saveRelationship()

        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(R.string.add_relationship_unknown_error, viewModel.uiState.value.errorMessageRes)
    }

    private class FakeRelationshipRepository(
        relationships: List<RelationshipProfile> = emptyList(),
        private val addFailure: Throwable? = null
    ) : RelationshipRepository {
        private val state = MutableStateFlow(relationships)
        var lastAddedRequest: NewRelationshipRequest? = null
        var lastUpdatedId: String? = null
        var lastUpdatedRequest: NewRelationshipRequest? = null

        override fun observeRelationships(): Flow<List<RelationshipProfile>> = state

        override suspend fun addRelationship(request: NewRelationshipRequest) {
            addFailure?.let { throw it }
            lastAddedRequest = request
        }

        override suspend fun deleteRelationship(relationshipId: String) = Unit

        override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) {
            lastUpdatedId = relationshipId
            lastUpdatedRequest = request
        }
    }
}
