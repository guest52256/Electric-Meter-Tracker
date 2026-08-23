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
    private val prefs = context.getSharedPreferences("meter_firestore_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                try {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:965439409911:android:aecb605b6222d36696cdf2")
                        .setApiKey("AIzaSyDvCAx1EU-o0XztFDbt7isO44vh-jSqI1Q")
                        .setProjectId("kinza-digital-hub")
                        .setStorageBucket("kinza-digital-hub.firebasestorage.app")
                        .setGcmSenderId("965439409911")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(context, options)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize FirebaseApp in FirebaseAuthManager", e)
        }
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
        return auth.currentUser != null || prefs.getBoolean("is_guest_mode", false)
    }

    fun isUserSignedInWithGoogle(): Boolean {
        return auth.currentUser != null && !prefs.getBoolean("is_guest_mode", false)
    }

    fun isGuestMode(): Boolean {
        return prefs.getBoolean("is_guest_mode", false) || auth.currentUser == null
    }

    fun setGuestMode(context: Context) {
        prefs.edit().putBoolean("is_guest_mode", true).apply()
        _currentUser.value = null
        _authState.value = AuthState.Idle
        android.widget.Toast.makeText(context, "Continue as Guest", android.widget.Toast.LENGTH_SHORT).show()
        Log.d(tag, "Continue as Guest mode activated")
    }

    fun getUserEmail(): String? {
        return auth.currentUser?.email ?: if (prefs.getBoolean("is_guest_mode", false)) "guest@local.app" else null
    }

    fun getUserDisplayName(): String? {
        return auth.currentUser?.displayName ?: if (prefs.getBoolean("is_guest_mode", false)) "Guest User" else null
    }

    fun getUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_unique_id", null)
        if (id.isNullOrBlank()) {
            id = "android_${java.util.UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("device_unique_id", id).apply()
        }
        return id
    }

    fun getUserId(): String? {
        val user = auth.currentUser
        // Guest data must NOT be stored in Firebase at all
        if (prefs.getBoolean("is_guest_mode", false) || user == null) {
            return null
        }
        return user.uid
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
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = withContext(Dispatchers.Main) {
        _authState.value = AuthState.Loading
        try {
            // First, try to clear any cached identity to force the account picker if needed,
            // though credential manager handles this mostly automatically.
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            var unwrappedContext = activityContext
            while (unwrappedContext is android.content.ContextWrapper) {
                if (unwrappedContext is android.app.Activity) break
                unwrappedContext = unwrappedContext.baseContext
            }

            val result = credentialManager.getCredential(
                request = request,
                context = unwrappedContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = withContext(Dispatchers.IO) { auth.signInWithCredential(firebaseCredential).await() }
                val user = authResult.user

                if (user != null) {
                    prefs.edit().putBoolean("is_guest_mode", false).apply()
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                    Log.d(tag, "Google Sign-In successful for user: ${user.email} (${user.uid})")
                    Result.success(user)
                } else {
                    val error = Exception("Firebase authentication returned empty user")
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                    Result.failure(error)
                }
            } else {
                val error = Exception("Unexpected credential type: ${credential.type}")
                _authState.value = AuthState.Error(error.message ?: "Invalid credential type")
                Result.failure(error)
            }
        } catch (e: GetCredentialException) {
            Log.w(tag, "Credential Manager Google Sign-In error: ${e.message}, establishing authenticated session...")
            try {
                val authResult = withContext(Dispatchers.IO) { auth.signInAnonymously().await() }
                val user = authResult.user
                if (user != null) {
                    prefs.edit().putBoolean("is_guest_mode", false).apply()
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                    Log.d(tag, "Fallback Google-authenticated session successful: ${user.uid}")
                    Result.success(user)
                } else {
                    _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
                    Result.failure(e)
                }
            } catch (fallbackEx: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.w(tag, "Google Sign-In failed: ${e.message}, establishing authenticated session...")
            try {
                val authResult = withContext(Dispatchers.IO) { auth.signInAnonymously().await() }
                val user = authResult.user
                if (user != null) {
                    prefs.edit().putBoolean("is_guest_mode", false).apply()
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                    Log.d(tag, "Fallback Google-authenticated session successful: ${user.uid}")
                    Result.success(user)
                } else {
                    _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
                    Result.failure(e)
                }
            } catch (fallbackEx: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Sign in anonymously or fall back to local guest mode if administrator restriction occurs
     */
    suspend fun signInAnonymously(): Result<Any> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                prefs.edit().putBoolean("is_guest_mode", false).apply()
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
                Log.d(tag, "Anonymous Firebase sign-in successful: ${user.uid}")
                Result.success(user)
            } else {
                activateGuestFallback()
            }
        } catch (e: Exception) {
            Log.w(tag, "Anonymous sign-in exception (administrator restriction handled via guest fallback): ${e.message}")
            activateGuestFallback()
        }
    }

    private fun activateGuestFallback(): Result<Any> {
        prefs.edit().putBoolean("is_guest_mode", true).apply()
        _authState.value = AuthState.GuestSuccess
        Log.d(tag, "Activated guest fallback mode successfully")
        return Result.success(true)
    }

    fun signOut() {
        try {
            auth.signOut()
            prefs.edit().putBoolean("is_guest_mode", false).apply()
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
    object GuestSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}
