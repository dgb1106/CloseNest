package com.example.closenest.features.auth

import com.example.closenest.features.auth.model.AuthRoute
import com.example.closenest.features.auth.model.UserDocument
import com.example.closenest.features.auth.repository.AuthRepository
import com.example.closenest.features.auth.viewmodel.RegisterViewModel
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

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank required fields show validation error and skip repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = RegisterViewModel(repository)

        viewModel.register()

        assertEquals("Vui lòng điền đầy đủ thông tin", viewModel.uiState.value.errorMessage)
        assertNull(repository.lastRegisteredUser)
    }

    @Test
    fun `invalid email is rejected before repository call`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = validRegisterViewModel(repository)
        viewModel.updateEmail("invalid-email")

        viewModel.register()

        assertEquals("Email không đúng định dạng", viewModel.uiState.value.errorMessage)
        assertNull(repository.lastRegisteredUser)
    }

    @Test
    fun `mismatched password is rejected before repository call`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = validRegisterViewModel(repository)
        viewModel.updateConfirmPassword("different")

        viewModel.register()

        assertEquals("Mật khẩu không khớp", viewModel.uiState.value.errorMessage)
        assertNull(repository.lastRegisteredUser)
    }

    @Test
    fun `valid registration trims user fields and reports success`() = runTest {
        val repository = FakeAuthRepository(registerResult = Result.success(Unit))
        val viewModel = validRegisterViewModel(repository)

        viewModel.register()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals("Linh", repository.lastRegisteredUser?.firstName)
        assertEquals("Nguyen", repository.lastRegisteredUser?.lastName)
        assertEquals("linh@example.com", repository.lastRegisteredUser?.email)
        assertEquals("0912345678", repository.lastRegisteredUser?.phoneNumber)
        assertEquals("password123", repository.lastRegisteredPassword)
    }

    @Test
    fun `registration failure surfaces friendly repository message`() = runTest {
        val repository = FakeAuthRepository(registerResult = Result.failure(IllegalStateException("Email already used")))
        val viewModel = validRegisterViewModel(repository)

        viewModel.register()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("Email already used", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register success handled returns to login route`() = runTest {
        val viewModel = RegisterViewModel(FakeAuthRepository())
        viewModel.navigateToRegister()

        viewModel.onRegisterSuccessHandled()

        assertEquals(AuthRoute.Login, viewModel.uiState.value.authRoute)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    private fun validRegisterViewModel(repository: FakeAuthRepository): RegisterViewModel =
        RegisterViewModel(repository).apply {
            updateFirstName(" Linh ")
            updateLastName(" Nguyen ")
            updateEmail(" linh@example.com ")
            updatePhoneNumber("0912345678")
            updatePassword("password123")
            updateConfirmPassword("password123")
        }

    private class FakeAuthRepository(
        private val registerResult: Result<Unit> = Result.success(Unit)
    ) : AuthRepository {
        private val auth = MutableStateFlow(false)
        var lastRegisteredUser: UserDocument? = null
        var lastRegisteredPassword: String? = null

        override val authState: Flow<Boolean> = auth

        override fun isLoggedIn(): Boolean = auth.value

        override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)

        override suspend fun register(user: UserDocument, password: String): Result<Unit> {
            lastRegisteredUser = user
            lastRegisteredPassword = password
            return registerResult
        }

        override suspend fun getUserDocument(uid: String): Result<UserDocument> =
            Result.success(UserDocument(uid = uid))

        override suspend fun loginWithGoogle(idToken: String): Result<Unit> = Result.success(Unit)

        override fun logout() {
            auth.value = false
        }
    }
}
