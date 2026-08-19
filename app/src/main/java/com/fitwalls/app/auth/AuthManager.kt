package com.fitwalls.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): Result<AuthResult> {
        try {
            // Use the default web client id provided by the google-services.json plugin
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val webClientId = if (resId != 0) context.getString(resId) else "YOUR_WEB_CLIENT_ID"
            
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            return handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Sign-in failed", e)
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Unknown error", e)
            return Result.failure(e)
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Result<AuthResult> {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                return Result.success(authResult)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("AuthManager", "Received an invalid google id token response", e)
                return Result.failure(e)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        } else {
            return Result.failure(Exception("Unexpected credential type"))
        }
    }
    
    fun getCurrentUser() = auth.currentUser
    
    fun signOut() {
        auth.signOut()
    }
}
