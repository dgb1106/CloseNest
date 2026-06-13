package com.example.closenest.features.auth

import com.example.closenest.features.auth.model.UserDocument
import com.example.closenest.features.auth.repository.AuthRepository
import com.example.closenest.features.auth.viewmodel.AuthViewModel
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

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank credentials show validation message and do not call repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onLogin("", "")

        assertEquals("Vui lòng nhập đầy đủ email và mật khẩu.", viewModel.uiState.value.message)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `login trims email and updates logged in state on success`() = runTest {
        val repository = FakeAuthRepository(loginResult = Result.success(Unit))
        val viewModel = AuthViewModel(repository)

        viewModel.onLogin("  user@example.com  ", "password123")

        assertEquals("user@example.com", repository.lastLoginEmail)
        assertEquals("password123", repository.lastLoginPassword)
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.message)
    }

    @Test
    fun `login failure shows friendly error without raw exception`() = runTest {
        val repository = FakeAuthRepository(loginResult = Result.failure(IllegalStateException("API_KEY=secret")))
        val viewModel = AuthViewModel(repository)

        viewModel.onLogin("user@example.com", "wrong-password")

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "Sai thông tin đăng nhập. Vui lòng kiểm tra lại email và mật khẩu.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `google login success updates logged in state`() = runTest {
        val repository = FakeAuthRepository(googleLoginResult = Result.success(Unit))
        val viewModel = AuthViewModel(repository)

        viewModel.loginWithGoogle("id-token")

        assertEquals("id-token", repository.lastGoogleToken)
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logout clears session state`() = runTest {
        val repository = FakeAuthRepository(initialLoggedIn = true)
        val viewModel = AuthViewModel(repository)

        viewModel.onLogout()

        assertTrue(repository.logoutCalled)
        assertFalse(viewModel.uiState.value.isLoggedIn)
    }

    private class FakeAuthRepository(
        initialLoggedIn: Boolean = false,
        private val loginResult: Result<Unit> = Result.success(Unit),
        private val registerResult: Result<Unit> = Result.success(Unit),
        private val googleLoginResult: Result<Unit> = Result.success(Unit)
    ) : AuthRepository {
        private val auth = MutableStateFlow(initialLoggedIn)
        var loginCalls = 0
        var lastLoginEmail: String? = null
        var lastLoginPassword: String? = null
        var lastGoogleToken: String? = null
        var lastRegisteredUser: UserDocument? = null
        var lastRegisteredPassword: String? = null
        var logoutCalled = false

        override val authState: Flow<Boolean> = auth

        override fun isLoggedIn(): Boolean = auth.value

        override suspend fun login(email: String, password: String): Result<Unit> {
            loginCalls++
            lastLoginEmail = email
            lastLoginPassword = password
            loginResult.onSuccess { auth.value = true }
            return loginResult
        }

        override suspend fun register(user: UserDocument, password: String): Result<Unit> {
            lastRegisteredUser = user
            lastRegisteredPassword = password
            return registerResult
        }

        override suspend fun getUserDocument(uid: String): Result<UserDocument> =
            Result.success(UserDocument(uid = uid))

        override suspend fun loginWithGoogle(idToken: String): Result<Unit> {
            lastGoogleToken = idToken
            googleLoginResult.onSuccess { auth.value = true }
            return googleLoginResult
        }

        override fun logout() {
            logoutCalled = true
            auth.value = false
        }
    }
}
