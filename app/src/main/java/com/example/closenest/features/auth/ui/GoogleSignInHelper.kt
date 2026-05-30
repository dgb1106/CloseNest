package com.example.closenest.features.auth.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.closenest.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Helper for Credential Manager based Google Sign-In
 * Handles getting ID Token from Google using Credential Manager
 */
suspend fun getGoogleIdToken(context: Context): Result<String> {
    return try {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        val idToken = when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                (credential as GoogleIdTokenCredential).idToken
            }
            else -> null
        }

        if (idToken != null) {
            Result.success(idToken)
        } else {
            Result.failure(Exception("Failed to extract ID Token from credential"))
        }
    } catch (e: GetCredentialException) {
        Result.failure(Exception("Credential Manager error: ${e.localizedMessage}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
