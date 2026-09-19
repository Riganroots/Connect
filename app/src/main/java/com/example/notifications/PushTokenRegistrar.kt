package com.example.notifications

import android.content.Context
import com.example.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PushTokenRegistrar(context: Context) {
    private val appContext = context.applicationContext
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(appContext).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    suspend fun registerCurrentToken(userId: String): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("A signed-in user is required."))
        }

        val tokenResult = FirebaseMessaging.getInstance().token.awaitPushResult()
        val token = tokenResult.getOrElse { return Result.failure(it) }
        return registerToken(userId, token)
    }

    suspend fun registerToken(userId: String, token: String): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))
        if (userId.isBlank() || token.isBlank()) {
            return Result.failure(IllegalArgumentException("User and push token are required."))
        }

        val tokenId = sha256(token)
        val data = mapOf<String, Any>(
            "token" to token,
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return db.collection("users")
            .document(userId)
            .collection("devices")
            .document(tokenId)
            .set(data)
            .awaitPushUnit()
    }

    suspend fun unregisterCurrentToken(userId: String): Result<Unit> {
        val db = firestore
        val tokenResult = FirebaseMessaging.getInstance().token.awaitPushResult()
        val token = tokenResult.getOrNull()

        var firstError: Throwable? = tokenResult.exceptionOrNull()

        if (db != null && !token.isNullOrBlank() && userId.isNotBlank()) {
            val deleteResult = db.collection("users")
                .document(userId)
                .collection("devices")
                .document(sha256(token))
                .delete()
                .awaitPushUnit()

            if (deleteResult.isFailure && firstError == null) {
                firstError = deleteResult.exceptionOrNull()
            }
        }

        val invalidateResult = FirebaseMessaging.getInstance().deleteToken().awaitPushUnit()
        if (invalidateResult.isFailure && firstError == null) {
            firstError = invalidateResult.exceptionOrNull()
        }

        return firstError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

private suspend fun <T> Task<T>.awaitPushResult(): Result<T> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(task.result))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Push token operation failed.")
                    )
                )
            }
        }
    }

private suspend fun Task<Void>.awaitPushUnit(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Push token operation failed.")
                    )
                )
            }
        }
    }
