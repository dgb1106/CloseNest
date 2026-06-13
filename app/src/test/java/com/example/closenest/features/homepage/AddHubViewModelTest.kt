package com.example.closenest.features.homepage

import com.example.closenest.R
import com.example.closenest.features.homepage.model.AppointmentItem
import com.example.closenest.features.homepage.model.MemoryItem
import com.example.closenest.features.homepage.model.MoodDayEntry
import com.example.closenest.features.homepage.model.NewAppointmentRequest
import com.example.closenest.features.homepage.model.NewMemoryRequest
import com.example.closenest.features.homepage.model.NewReflectionRequest
import com.example.closenest.features.homepage.repository.AppointmentRepository
import com.example.closenest.features.homepage.repository.MemoryRepository
import com.example.closenest.features.homepage.repository.ReflectionRepository
import com.example.closenest.features.homepage.viewmodel.AddHubViewModel
import com.example.closenest.features.homepage.viewmodel.MemoryType
import com.example.closenest.features.homepage.viewmodel.ReflectionFeeling
import com.example.closenest.features.homepage.viewmodel.ReflectionMood
import com.example.closenest.features.homepage.viewmodel.ReflectionSource
import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.repository.RelationshipRepository
import com.example.closenest.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddHubViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads contacts sorted by name`() = runTest {
        val viewModel = viewModel(
            relationships = listOf(relationship(id = "2", name = "Bao Nam"), relationship(id = "1", name = "An Linh"))
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingContacts)
        assertEquals(listOf("An Linh", "Bao Nam"), viewModel.uiState.value.contacts.map { it.name })
    }

    @Test
    fun `valid reflection maps selected contact mood feelings and sources`() = runTest {
        val reflectionRepository = FakeReflectionRepository()
        val viewModel = viewModel(reflectionRepository = reflectionRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onContactToggled("rel-1")
        viewModel.onMoodSelected(ReflectionMood.Pleasant)
        viewModel.onFeelingToggled(ReflectionFeeling.Grateful)
        viewModel.onSourceToggled(ReflectionSource.Friends)
        viewModel.completeReflection()
        advanceUntilIdle()

        val request = reflectionRepository.lastRequest
        assertEquals(R.string.add_reflection_saved, viewModel.uiState.value.reflectionSavedMessageRes)
        assertEquals(listOf("rel-1"), request?.interactedContacts?.map { it.id })
        assertEquals(ReflectionMood.Pleasant.storageValue, request?.mood)
        assertEquals(listOf(ReflectionFeeling.Grateful.storageValue), request?.feelings)
        assertEquals(listOf(ReflectionSource.Friends.storageValue), request?.sources)
    }

    @Test
    fun `memory requires selected contact before saving`() = runTest {
        val memoryRepository = FakeMemoryRepository()
        val viewModel = viewModel(memoryRepository = memoryRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMemoryTitleChanged("Coffee meetup")
        viewModel.saveMemory()
        advanceUntilIdle()

        assertEquals(R.string.add_interaction_contact_required, viewModel.uiState.value.memoryErrorMessageRes)
        assertTrue(memoryRepository.requests.isEmpty())
    }

    @Test
    fun `valid memory saves one request per selected contact`() = runTest {
        val memoryRepository = FakeMemoryRepository()
        val viewModel = viewModel(memoryRepository = memoryRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMemoryContactToggled("rel-1")
        viewModel.onMemoryContactToggled("rel-2")
        viewModel.onMemoryTitleChanged(" Coffee meetup ")
        viewModel.onMemoryTypeSelected(MemoryType.Meetup)
        viewModel.onMemoryNoteChanged(" Had lunch ")
        viewModel.onMemoryPhotoChanged("content://photo")
        viewModel.onMemoryLocationSelected("Cafe", 10.1, 106.7)
        viewModel.saveMemory()
        advanceUntilIdle()

        assertEquals(R.string.add_interaction_saved, viewModel.uiState.value.memorySavedMessageRes)
        assertEquals(2, memoryRepository.requests.size)
        assertEquals(listOf("rel-2", "rel-1"), memoryRepository.requests.map { it.contactId })
        assertEquals("Coffee meetup", memoryRepository.requests.first().title)
        assertEquals("Had lunch", memoryRepository.requests.first().note)
        assertEquals("Cafe", memoryRepository.requests.first().location)
    }

    @Test
    fun `appointment requires selected location coordinates`() = runTest {
        val appointmentRepository = FakeAppointmentRepository()
        val viewModel = viewModel(appointmentRepository = appointmentRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAppointmentContactToggled("rel-1")
        viewModel.onAppointmentLocationChanged("Cafe")
        viewModel.onAppointmentDateSelected(1_800_000_000_000L)
        viewModel.onAppointmentTimeSelected(9, 30)
        viewModel.saveAppointment()
        advanceUntilIdle()

        assertEquals(R.string.add_appointment_location_select_required, viewModel.uiState.value.appointmentErrorMessageRes)
        assertEquals(0, appointmentRepository.addCalls)
    }

    @Test
    fun `valid appointment saves request and exposes saved appointment`() = runTest {
        val appointmentRepository = FakeAppointmentRepository()
        val viewModel = viewModel(appointmentRepository = appointmentRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAppointmentContactToggled("rel-1")
        viewModel.onAppointmentLocationSelected("Cafe", 10.1, 106.7)
        viewModel.onAppointmentDateSelected(1_800_000_000_000L)
        viewModel.onAppointmentTimeSelected(9, 30)
        viewModel.onAppointmentNoteChanged("Bring notes")
        viewModel.saveAppointment()
        advanceUntilIdle()

        assertEquals(R.string.add_appointment_saved, viewModel.uiState.value.appointmentSavedMessageRes)
        assertNotNull(viewModel.uiState.value.savedAppointment)
        assertEquals("Minh Anh", appointmentRepository.lastRequest?.name)
        assertEquals(listOf("rel-1"), appointmentRepository.lastRequest?.participantContactIds)
        assertEquals("Bring notes", appointmentRepository.lastRequest?.note)
    }

    @Test
    fun `memory repository failure reports save error without leaving loading state`() = runTest {
        val memoryRepository = FakeMemoryRepository(failure = IllegalStateException("raw storage failure"))
        val viewModel = viewModel(memoryRepository = memoryRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMemoryContactToggled("rel-1")
        viewModel.onMemoryTitleChanged("Coffee")
        viewModel.saveMemory()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSavingMemory)
        assertEquals(R.string.add_interaction_save_error, viewModel.uiState.value.memoryErrorMessageRes)
    }

    private fun viewModel(
        relationships: List<RelationshipProfile> = listOf(
            relationship(id = "rel-1", name = "Minh Anh"),
            relationship(id = "rel-2", name = "Bao Nam")
        ),
        reflectionRepository: FakeReflectionRepository = FakeReflectionRepository(),
        memoryRepository: FakeMemoryRepository = FakeMemoryRepository(),
        appointmentRepository: FakeAppointmentRepository = FakeAppointmentRepository()
    ) = AddHubViewModel(
        relationshipRepository = FakeRelationshipRepository(relationships),
        reflectionRepository = reflectionRepository,
        memoryRepository = memoryRepository,
        appointmentRepository = appointmentRepository
    )

    private class FakeRelationshipRepository(
        relationships: List<RelationshipProfile>
    ) : RelationshipRepository {
        private val state = MutableStateFlow(relationships)

        override fun observeRelationships(): Flow<List<RelationshipProfile>> = state

        override suspend fun addRelationship(request: NewRelationshipRequest) = Unit

        override suspend fun deleteRelationship(relationshipId: String) = Unit

        override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) = Unit
    }

    private class FakeReflectionRepository : ReflectionRepository {
        var lastRequest: NewReflectionRequest? = null

        override suspend fun addReflection(request: NewReflectionRequest) {
            lastRequest = request
        }

        override suspend fun getRecentMoods(days: Int): Result<List<MoodDayEntry>> =
            Result.success(emptyList())
    }

    private class FakeMemoryRepository(
        private val failure: Throwable? = null
    ) : MemoryRepository {
        val requests = mutableListOf<NewMemoryRequest>()

        override suspend fun addMemory(request: NewMemoryRequest) {
            failure?.let { throw it }
            requests += request
        }

        override suspend fun getMemories(): List<MemoryItem> = emptyList()
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        var addCalls = 0
        var lastRequest: NewAppointmentRequest? = null

        override suspend fun addAppointment(request: NewAppointmentRequest): AppointmentItem {
            addCalls++
            lastRequest = request
            return AppointmentItem(
                id = "appt-1",
                name = request.name,
                participantContactIds = request.participantContactIds,
                participantContactNames = request.participantContactNames,
                location = request.location,
                locationLatitude = request.locationLatitude,
                locationLongitude = request.locationLongitude,
                appointmentDateMillis = request.appointmentDateMillis,
                dateKey = "2027-01-15",
                note = request.note,
                createdAtMillis = request.createdAtMillis
            )
        }

        override suspend fun countUpcomingAppointments(todayMillis: Long): Int = 0

        override suspend fun getUpcomingAppointments(todayMillis: Long): List<AppointmentItem> = emptyList()

        override suspend fun deleteAppointment(appointmentId: String) = Unit
    }
}

private fun relationship(
    id: String,
    name: String
) = RelationshipProfile(
    id = id,
    userId = "user-1",
    name = name,
    tag = RelationshipTag.Friend,
    birthdayIso = null,
    phoneNumber = null,
    email = null,
    interests = emptyList(),
    notes = null,
    avatarUrl = null,
    priority = RelationshipPriority.Medium,
    createdAtMillis = 1_000L,
    updatedAtMillis = 2_000L
)
