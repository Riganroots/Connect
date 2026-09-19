package com.example.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isFirebaseConfigured: Boolean,
    val user: AuthUser? = null,
    val isPreviewMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val canEnterApp: Boolean
        get() = user != null || isPreviewMode
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val gateway = FirebaseAuthGateway(application)

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isFirebaseConfigured = gateway.isConfigured,
            user = gateway.currentUser()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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
        gateway.signOut()
        _uiState.value = AuthUiState(
            isFirebaseConfigured = gateway.isConfigured,
            user = null
        )
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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            block()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isPreviewMode = false,
                        isLoading = false,
                        errorMessage = null
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
