package com.example.closenest.features.auth.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Đăng nhập thất bại.")))
                }
            }
    }

    override fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(Result.failure(task.exception ?: Exception("Tạo tài khoản thất bại.")))
                    return@addOnCompleteListener
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("$firstName $lastName".trim())
                    .build()

                auth.currentUser
                    ?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener {
                        onResult(Result.success(Unit))
                    }
                    ?: onResult(Result.success(Unit))
            }
    }

    override fun logout() {
        auth.signOut()
    }
}

object AuthRepositoryProvider {
    private var instance: AuthRepository? = null

    fun getInstance(): AuthRepository {
        return instance ?: FirebaseAuthRepository(FirebaseAuth.getInstance()).also {
            instance = it
        }
    }
}
