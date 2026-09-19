package com.example.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.notifications.PushTokenRegistrar
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isFirebaseConfigured: Boolean,
    val user: AuthUser? = null,
    val isCloudProfileReady: Boolean = false,
    val isPreviewMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val canEnterApp: Boolean
        get() = isPreviewMode || (user != null && isCloudProfileReady)
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway = FirebaseAuthGateway(application)
    private val pushTokenRegistrar = PushTokenRegistrar(application)
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(application).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    private val initialUser = gateway.currentUser()

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isFirebaseConfigured = gateway.isConfigured,
            user = initialUser,
            isLoading = initialUser != null
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        initialUser?.let(::ensureCloudProfile)
    }

    fun signIn(email: String, password: String) {
        val validationError = validate(email, password)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError, infoMessage = null)
            return
        }
        runAuth { gateway.signIn(email.trim(), password) }
    }

    fun createAccount(email: String, password: String) {
        val validationError = validate(email, password)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError, infoMessage = null)
            return
        }
        runAuth { gateway.createAccount(email.trim(), password) }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@")) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your email address first.",
                infoMessage = null
            )
            return
        }
        if (!gateway.isConfigured) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Firebase is not configured for this build yet.",
                infoMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            gateway.sendPasswordReset(trimmedEmail)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "Password reset email sent."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage(error)
                    )
                }
        }
    }

    fun enterPreviewMode() {
        if (BuildConfig.DEBUG) {
            _uiState.value = _uiState.value.copy(
                isPreviewMode = true,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun leavePreviewMode() {
        _uiState.value = _uiState.value.copy(isPreviewMode = false)
    }

    fun signOut() {
        val userId = gateway.currentUser()?.uid

        viewModelScope.launch {
            if (!userId.isNullOrBlank()) {
                pushTokenRegistrar.unregisterCurrentToken(userId)
            }

            gateway.signOut()
            _uiState.value = AuthUiState(
                isFirebaseConfigured = gateway.isConfigured,
                user = null
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    private fun runAuth(block: suspend () -> Result<AuthUser>) {
        if (!gateway.isConfigured) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Firebase is not configured for this build yet.",
                infoMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null,
                isCloudProfileReady = false
            )

            block()
                .onSuccess(::ensureCloudProfile)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage(error)
                    )
                }
        }
    }

    private fun ensureCloudProfile(user: AuthUser) {
        val db = firestore
        if (db == null) {
            failCloudProfile("Firestore is not available in this build.")
            return
        }

        val document = db.collection("users").document(user.uid)
        document.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    document.update(
                        mapOf(
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "lastLoginAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener {
                        markCloudProfileReady(user)
                    }.addOnFailureListener { error ->
                        failCloudProfile(error.localizedMessage ?: "Could not update your cloud profile.")
                    }
                } else {
                    val profile = mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "location" to "",
                        "bio" to "",
                        "interests" to emptyList<String>(),
                        "photoUrl" to "",
                        "isVerified" to false,
                        "schemaVersion" to 1,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    )

                    document.set(profile)
                        .addOnSuccessListener {
                            markCloudProfileReady(user)
                        }
                        .addOnFailureListener { error ->
                            failCloudProfile(error.localizedMessage ?: "Could not create your cloud profile.")
                        }
                }
            }
            .addOnFailureListener { error ->
                failCloudProfile(error.localizedMessage ?: "Could not load your cloud profile.")
            }
    }

    private fun markCloudProfileReady(user: AuthUser) {
        _uiState.value = _uiState.value.copy(
            user = user,
            isCloudProfileReady = true,
            isPreviewMode = false,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun failCloudProfile(message: String) {
        gateway.signOut()
        _uiState.value = _uiState.value.copy(
            user = null,
            isCloudProfileReady = false,
            isLoading = false,
            errorMessage = "Account sign-in worked, but cloud profile setup failed. $message"
        )
    }

    private fun validate(email: String, password: String): String? = when {
        !email.trim().contains("@") -> "Enter a valid email address."
        password.length < 6 -> "Password must be at least 6 characters."
        else -> null
    }

    private fun friendlyMessage(error: Throwable): String {
        val raw = error.localizedMessage.orEmpty()
        return when {
            raw.contains("password", ignoreCase = true) && raw.contains("invalid", ignoreCase = true) ->
                "The email or password is incorrect."
            raw.contains("email address is already", ignoreCase = true) ->
                "An account already exists for this email."
            raw.contains("network", ignoreCase = true) ->
                "Could not connect. Check your internet connection and try again."
            raw.isNotBlank() -> raw
            else -> "Something went wrong. Please try again."
        }
    }
}
