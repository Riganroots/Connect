package com.example.data.cloud

import android.content.Context
import com.example.auth.AuthUser
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudUserProfileRepository(context: Context) {
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    val isConfigured: Boolean
        get() = firestore != null

    suspend fun ensureUserProfile(user: AuthUser): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured for this build."))

        val document = db.collection("users").document(user.uid)

        return suspendCancellableCoroutine { continuation ->
            document.get()
                .addOnSuccessListener { snapshot ->
                    if (!continuation.isActive) return@addOnSuccessListener

                    if (snapshot.exists()) {
                        document.update(
                            mapOf(
                                "email" to user.email,
                                "displayName" to user.displayName,
                                "lastLoginAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).addOnCompleteListener { updateTask ->
                            if (!continuation.isActive) return@addOnCompleteListener
                            if (updateTask.isSuccessful) {
                                continuation.resume(Result.success(Unit))
                            } else {
                                continuation.resume(
                                    Result.failure(
                                        updateTask.exception
                                            ?: IllegalStateException("Could not update the cloud profile.")
                                    )
                                )
                            }
                        }
                    } else {
                        val profile = mapOf(
                            "uid" to user.uid,
                            "email" to user.email,
                            "displayName" to (user.displayName ?: user.email?.substringBefore("@").orEmpty()),
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
                            .addOnCompleteListener { createTask ->
                                if (!continuation.isActive) return@addOnCompleteListener
                                if (createTask.isSuccessful) {
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    continuation.resume(
                                        Result.failure(
                                            createTask.exception
                                                ?: IllegalStateException("Could not create the cloud profile.")
                                        )
                                    )
                                }
                            }
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                }
        }
    }
}
