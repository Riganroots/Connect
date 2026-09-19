package com.example.data.cloud

import android.content.Context
import com.example.data.models.Availability
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudCommunityRepository(context: Context) {
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    fun observeGroupMemberships(userId: String): Flow<Set<String>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close(IllegalStateException("Firestore is not configured."))
            return@callbackFlow
        }

        val registration = db.collection(USERS)
            .document(userId)
            .collection(GROUP_MEMBERSHIPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.documents.orEmpty().map { it.id }.toSet())
            }

        awaitClose { registration.remove() }
    }

    suspend fun toggleGroupMembership(groupId: String, userId: String): Result<Boolean> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val membership = db.collection(USERS)
            .document(userId)
            .collection(GROUP_MEMBERSHIPS)
            .document(groupId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(membership)
            if (snapshot.exists()) {
                transaction.delete(membership)
                false
            } else {
                transaction.set(
                    membership,
                    mapOf(
                        "groupId" to groupId,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }
        }.awaitCommunityResult()
    }

    fun observeAvailability(currentUserId: String): Flow<List<Availability>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close(IllegalStateException("Firestore is not configured."))
            return@callbackFlow
        }

        val registration = db.collection(AVAILABILITY)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val rows = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val userId = document.getString("userId").orEmpty()
                    val userName = document.getString("userName").orEmpty()
                    val statusText = document.getString("statusText").orEmpty()
                    val iconType = document.getString("iconType").orEmpty()
                    val updatedAt = (document.get("updatedAt") as? Timestamp)?.toDate()?.time ?: now
                    val expiresAt = (document.get("expiresAt") as? Timestamp)?.toDate()?.time ?: 0L

                    if (
                        userId.isBlank() ||
                        userName.isBlank() ||
                        statusText.isBlank() ||
                        expiresAt <= now
                    ) {
                        return@mapNotNull null
                    }

                    Availability(
                        userId = userId,
                        userName = if (userId == currentUserId) "$userName (You)" else userName,
                        statusText = statusText,
                        iconType = iconType,
                        timeAgo = relativeTime(updatedAt, now),
                        isCurrentUser = userId == currentUserId,
                        isUserVerified = false,
                        updatedAtMillis = updatedAt
                    )
                }

                trySend(rows)
            }

        awaitClose { registration.remove() }
    }

    suspend fun setAvailableNow(
        userId: String,
        userName: String,
        statusText: String,
        iconType: String
    ): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val cleanStatus = statusText.trim().take(MAX_STATUS_LENGTH)
        if (cleanStatus.isBlank()) {
            return Result.failure(IllegalArgumentException("Availability status cannot be empty."))
        }

        val data = mapOf<String, Any>(
            "userId" to userId,
            "userName" to userName.trim().ifBlank { "Connect Member" }.take(MAX_NAME_LENGTH),
            "statusText" to cleanStatus,
            "iconType" to iconType,
            "isVerified" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
            "expiresAt" to Timestamp(Date(System.currentTimeMillis() + AVAILABILITY_LIFETIME_MS))
        )

        return db.collection(AVAILABILITY)
            .document(userId)
            .set(data)
            .awaitCommunityUnit()
    }

    suspend fun removeAvailableNow(userId: String): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        return db.collection(AVAILABILITY)
            .document(userId)
            .delete()
            .awaitCommunityUnit()
    }

    private fun relativeTime(timestamp: Long, now: Long): String {
        val minutes = ((now - timestamp).coerceAtLeast(0L) / 60_000L).toInt()
        return when {
            minutes < 1 -> "Just now"
            minutes == 1 -> "1m ago"
            minutes < 60 -> "${minutes}m ago"
            else -> "${minutes / 60}h ago"
        }
    }

    private companion object {
        const val USERS = "users"
        const val GROUP_MEMBERSHIPS = "groupMemberships"
        const val AVAILABILITY = "availability"
        const val MAX_STATUS_LENGTH = 35
        const val MAX_NAME_LENGTH = 80
        const val AVAILABILITY_LIFETIME_MS = 2 * 60 * 60 * 1000L
    }
}

private suspend fun <T> Task<T>.awaitCommunityResult(): Result<T> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(task.result))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Firebase community operation failed.")
                    )
                )
            }
        }
    }

private suspend fun Task<Void>.awaitCommunityUnit(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Firebase community operation failed.")
                    )
                )
            }
        }
    }
