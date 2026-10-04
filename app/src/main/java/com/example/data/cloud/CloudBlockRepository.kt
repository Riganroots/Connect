package com.example.data.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudBlockRepository(context: Context) {
    private val firestore = FirebaseApp.getApps(context).firstOrNull()
        ?.let { FirebaseFirestore.getInstance(it) }

    fun observeBlockedUsers(uid: String): Flow<Map<String, String>> = callbackFlow {
        val db = firestore
        if (db == null) { close(IllegalStateException("Blocking requires a real account.")); return@callbackFlow }
        val registration = db.collection("users").document(uid).collection("blockedUsers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error) else
                    trySend(snapshot?.documents.orEmpty().associate { it.id to (it.getString("displayName") ?: "Connect Member") })
            }
        awaitClose { registration.remove() }
    }

    suspend fun setBlocked(uid: String, targetUid: String, displayName: String, blocked: Boolean): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Blocking requires a real account."))
        if (uid.isBlank() || targetUid.isBlank() || uid == targetUid || targetUid.contains('/')) {
            return Result.failure(IllegalArgumentException("This user cannot be blocked."))
        }
        val ref = db.collection("users").document(uid).collection("blockedUsers").document(targetUid)
        return suspendCancellableCoroutine { continuation ->
            val task = if (blocked) ref.set(mapOf(
                "targetUid" to targetUid,
                "displayName" to displayName.trim().ifBlank { "Connect Member" }.take(80),
                "blockedAt" to FieldValue.serverTimestamp()
            )) else ref.delete()
            task.addOnCompleteListener {
                if (!continuation.isActive) return@addOnCompleteListener
                continuation.resume(if (it.isSuccessful) Result.success(Unit) else
                    Result.failure(it.exception ?: IllegalStateException("Could not update blocked users.")))
            }
        }
    }
}
