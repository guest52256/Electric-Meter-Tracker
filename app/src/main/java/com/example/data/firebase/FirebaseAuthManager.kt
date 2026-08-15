package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthManager(private val context: Context) {

    private val tag = "FirebaseAuthManager"
    val webClientId = "965439409911-episp0b26hvfnun6gniqe2hiqrh1l623.apps.googleusercontent.com"

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _currentUser.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            Log.d(tag, "Auth state changed. User: ${firebaseAuth.currentUser?.email}")
        }
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun getUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    fun getUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getIdToken(): String? = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext null
            val result = user.getIdToken(false).await()
            result.token
        } catch (e: Exception) {
            Log.w(tag, "Failed to get ID token: ${e.message}")
            null
        }
    }

    /**
     * Sign in using Credential Manager with Google ID Token
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                    Log.d(tag, "Google Sign-In successful for user: ${user.email} (${user.uid})")
                    return@withContext Result.success(user)
                } else {
                    val error = Exception("Firebase authentication returned empty user")
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                    return@withContext Result.failure(error)
                }
            } else {
                val error = Exception("Unexpected credential type: ${credential.type}")
                _authState.value = AuthState.Error(error.message ?: "Invalid credential type")
                return@withContext Result.failure(error)
            }
        } catch (e: GetCredentialException) {
            Log.w(tag, "Credential Manager Google Sign-In error: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(tag, "Google Sign-In failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            Result.failure(e)
        }
    }

    /**
     * Sign in anonymously or with email/password fallback if Google Play Services is unavailable
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
                Log.d(tag, "Anonymous Firebase sign-in successful: ${user.uid}")
                Result.success(user)
            } else {
                val error = Exception("Anonymous sign-in returned null")
                _authState.value = AuthState.Error("Sign in failed")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.w(tag, "Anonymous sign-in exception: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Idle
            Log.d(tag, "User signed out")
        } catch (e: Exception) {
            Log.w(tag, "Sign out error: ${e.message}")
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
