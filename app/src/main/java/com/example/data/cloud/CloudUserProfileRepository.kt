package com.example.data.cloud

import android.content.Context
import com.example.data.models.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CloudUserProfileRepository(context: Context) {
    private val firestore = FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    fun observeProfile(uid: String): Flow<UserProfile> = callbackFlow {
        val db = firestore
        if (db == null) { close(IllegalStateException("Firestore is not configured.")); return@callbackFlow }
        val listener = db.collection("users").document(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            // A queued offline edit is not yet a confirmed cloud profile.
            if (snapshot == null || !snapshot.exists() || snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
            trySend(UserProfile(
                id = uid,
                name = snapshot.getString("displayName").orEmpty().ifBlank { "Connect Member" },
                bio = snapshot.getString("bio").orEmpty(),
                location = snapshot.getString("location").orEmpty().ifBlank { "Kathmandu, Nepal" },
                interests = (snapshot.get("interests") as? List<*>)?.filterIsInstance<String>()?.joinToString(", ").orEmpty(),
                isVerified = snapshot.getBoolean("isVerified") ?: false
            ))
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveProfile(uid: String, name: String, bio: String, interests: List<String>): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore is not configured."))
        // Update only editable fields; never replace identity, creation time or trusted verification.
        return suspendCancellableCoroutine { continuation ->
            db.collection("users").document(uid).update(mapOf(
                "displayName" to name, "bio" to bio, "interests" to interests,
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp()
            )).addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                continuation.resume(if (task.isSuccessful) Result.success(Unit) else
                    Result.failure(task.exception ?: IllegalStateException("Could not save profile.")))
            }
        }
    }
}
