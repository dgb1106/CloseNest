package com.example.closenest.features.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import com.example.closenest.features.profile.repository.ProfileRepository
import com.example.closenest.features.profile.viewmodel.ProfileViewModel
import com.example.closenest.testutil.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUpFirebase() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext<Context>(),
                FirebaseOptions.Builder()
                    .setApplicationId("1:123456789:android:test")
                    .setApiKey("test-api-key")
                    .setProjectId("test-project")
                    .build()
            )
        }
    }

    @After
    fun tearDownFirebase() {
        FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).forEach { app ->
            app.delete()
        }
    }

    @Test
    fun `loads profile state from repository`() = runTest {
        val repository = FakeProfileRepository(user = userProfile())
        val viewModel = ProfileViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Linh Nguyen", viewModel.uiState.value.user?.name)
        assertEquals(1, viewModel.uiState.value.recentRelationships.size)
    }

    @Test
    fun `account menu opens editable account detail with current user data`() = runTest {
        val repository = FakeProfileRepository(user = userProfile())
        val viewModel = ProfileViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMenuItemClicked("account")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAccountDetail)
        assertEquals("Linh", viewModel.uiState.value.accountEditFirstName)
        assertEquals("Nguyen", viewModel.uiState.value.accountEditLastName)
        assertEquals("linh@example.com", viewModel.uiState.value.accountEditEmail)
    }

    @Test
    fun `save account changes delegates updated profile and exits edit mode`() = runTest {
        val repository = FakeProfileRepository(user = userProfile())
        val viewModel = ProfileViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onShowAccountDetail()
        viewModel.toggleAccountEditMode()
        viewModel.updateAccountFirstName("Anh")
        viewModel.updateAccountPhone("0900000000")
        viewModel.saveAccountChanges()
        advanceUntilIdle()

        assertEquals("Anh", repository.lastUpdatedUser?.firstName)
        assertEquals("0900000000", repository.lastUpdatedUser?.phoneNumber)
        assertFalse(viewModel.uiState.value.isEditingAccount)
    }

    @Test
    fun `logout menu and confirm call repository logout`() = runTest {
        val repository = FakeProfileRepository(user = userProfile())
        val viewModel = ProfileViewModel(repository)

        viewModel.onMenuItemClicked("logout")
        assertTrue(viewModel.showLogoutDialog.value)

        viewModel.onConfirmLogout()
        advanceUntilIdle()

        assertTrue(repository.logoutCalled)
    }

    private class FakeProfileRepository(
        user: UserProfile
    ) : ProfileRepository {
        private val profileState = MutableStateFlow(
            ProfileUiState(
                isLoading = false,
                user = user,
                recentRelationships = listOf(RelationshipQuickPreview(id = "rel-1", name = "Minh Anh"))
            )
        )
        var lastUpdatedUser: UserProfile? = null
        var logoutCalled = false

        override fun observeCurrentUser(): Flow<UserProfile?> = MutableStateFlow(profileState.value.user)

        override fun observeProfileUiState(): Flow<ProfileUiState> = profileState

        override suspend fun logout(): Result<Unit> {
            logoutCalled = true
            return Result.success(Unit)
        }

        override suspend fun updateProfile(user: UserProfile): Result<Unit> {
            lastUpdatedUser = user
            profileState.value = profileState.value.copy(user = user)
            return Result.success(Unit)
        }
    }
}

private fun userProfile() = UserProfile(
    uid = "user-1",
    firstName = "Linh",
    lastName = "Nguyen",
    email = "linh@example.com",
    phoneNumber = "0912345678",
    gender = "Female",
    streakCount = 3
)
