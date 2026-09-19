package com.example.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)

class FirebaseAuthGateway(context: Context) {
    private val firebaseApp: FirebaseApp? = FirebaseApp.getApps(context).firstOrNull()
    private val auth: FirebaseAuth? = firebaseApp?.let(FirebaseAuth::getInstance)

    val isConfigured: Boolean
        get() = auth != null

    fun currentUser(): AuthUser? = auth?.currentUser?.toAuthUser()

    suspend fun signIn(email: String, password: String): Result<AuthUser> {
        val firebaseAuth = auth
            ?: return Result.failure(IllegalStateException("Firebase is not configured for this build."))
        return firebaseAuth.signInWithEmailAndPassword(email, password).awaitUser()
    }

    suspend fun createAccount(email: String, password: String): Result<AuthUser> {
        val firebaseAuth = auth
            ?: return Result.failure(IllegalStateException("Firebase is not configured for this build."))
        return firebaseAuth.createUserWithEmailAndPassword(email, password).awaitUser()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val firebaseAuth = auth
            ?: return Result.failure(IllegalStateException("Firebase is not configured for this build."))

        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(
                            Result.failure(
                                task.exception ?: IllegalStateException("Could not send password reset email.")
                            )
                        )
                    }
                }
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}

private suspend fun Task<AuthResult>.awaitUser(): Result<AuthUser> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                val user = task.result?.user
                if (user != null) {
                    continuation.resume(Result.success(user.toAuthUser()))
                } else {
                    continuation.resume(Result.failure(IllegalStateException("Authentication completed without a user.")))
                }
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Authentication failed.")
                    )
                )
            }
        }
    }

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName
)
