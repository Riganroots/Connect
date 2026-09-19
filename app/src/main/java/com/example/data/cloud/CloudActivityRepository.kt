package com.example.data.cloud

import android.content.Context
import com.example.data.models.Plan
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudActivityRepository(context: Context) {
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    fun observeActivities(userId: String): Flow<List<Plan>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close(IllegalStateException("Firestore is not configured."))
            return@callbackFlow
        }

        var activityDocuments: List<DocumentSnapshot> = emptyList()
        var joinedActivityIds: Set<String> = emptySet()
        var savedActivityIds: Set<String> = emptySet()

        fun emitCurrentState() {
            val plans = activityDocuments.mapNotNull { document ->
                val title = document.getString("title").orEmpty()
                if (title.isBlank()) return@mapNotNull null

                val organizerId = document.getString("organizerId").orEmpty()

                Plan(
                    cloudId = document.id,
                    organizerId = organizerId,
                    title = title,
                    category = document.getString("category") ?: "Meet",
                    location = document.getString("location").orEmpty(),
                    date = document.getString("date").orEmpty(),
                    time = document.getString("time") ?: "Anytime",
                    pricePerPerson = document.getString("pricePerPerson") ?: "Free",
                    participantsNeeded = (document.getLong("participantsNeeded") ?: 0L).toInt(),
                    description = document.getString("description").orEmpty(),
                    organizerName = document.getString("organizerName") ?: "Connect Member",
                    organizerRating = document.getDouble("organizerRating") ?: 0.0,
                    joinedCount = (document.getLong("joinedCount") ?: 1L).toInt(),
                    isJoinedByMe = organizerId == userId || joinedActivityIds.contains(document.id),
                    isSaved = savedActivityIds.contains(document.id),
                    isVerifiedOrganizer = document.getBoolean("isVerifiedOrganizer") ?: false
                )
            }

            trySend(plans)
        }

        val activityRegistration = db.collection(ACTIVITIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                activityDocuments = snapshot?.documents.orEmpty()
                emitCurrentState()
            }

        val membershipRegistration = db.collection(USERS)
            .document(userId)
            .collection(JOINED_ACTIVITIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                joinedActivityIds = snapshot?.documents.orEmpty().map { it.id }.toSet()
                emitCurrentState()
            }

        val savedRegistration = db.collection(USERS)
            .document(userId)
            .collection(SAVED_ACTIVITIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                savedActivityIds = snapshot?.documents.orEmpty().map { it.id }.toSet()
                emitCurrentState()
            }

        awaitClose {
            activityRegistration.remove()
            membershipRegistration.remove()
            savedRegistration.remove()
        }
    }

    suspend fun createActivity(plan: Plan, organizerId: String): Result<String> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val activity = db.collection(ACTIVITIES).document()
        val membership = db.collection(USERS)
            .document(organizerId)
            .collection(JOINED_ACTIVITIES)
            .document(activity.id)

        val data = hashMapOf<String, Any>(
            "organizerId" to organizerId,
            "organizerName" to plan.organizerName,
            "organizerRating" to plan.organizerRating,
            "title" to plan.title,
            "category" to plan.category,
            "location" to plan.location,
            "date" to plan.date,
            "time" to plan.time,
            "pricePerPerson" to plan.pricePerPerson,
            "participantsNeeded" to plan.participantsNeeded,
            "description" to plan.description,
            "joinedCount" to 1,
            "isVerifiedOrganizer" to plan.isVerifiedOrganizer,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(activity, data)
        batch.set(
            membership,
            mapOf(
                "activityId" to activity.id,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )

        return batch.commit().awaitResult().map { activity.id }
    }

    suspend fun toggleJoin(activityId: String, userId: String): Result<Boolean> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val activity = db.collection(ACTIVITIES).document(activityId)
        val membership = db.collection(USERS)
            .document(userId)
            .collection(JOINED_ACTIVITIES)
            .document(activityId)

        return db.runTransaction { transaction ->
            val activitySnapshot = transaction.get(activity)
            val membershipSnapshot = transaction.get(membership)

            val organizerId = activitySnapshot.getString("organizerId").orEmpty()
            if (organizerId == userId) {
                return@runTransaction true
            }

            val joinedCount = (activitySnapshot.getLong("joinedCount") ?: 1L).toInt()

            if (membershipSnapshot.exists()) {
                transaction.delete(membership)
                transaction.update(
                    activity,
                    mapOf(
                        "joinedCount" to maxOf(1, joinedCount - 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                false
            } else {
                transaction.set(
                    membership,
                    mapOf(
                        "activityId" to activityId,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    activity,
                    mapOf(
                        "joinedCount" to joinedCount + 1,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }
        }.awaitResult()
    }

    suspend fun toggleSaved(activityId: String, userId: String): Result<Boolean> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val saved = db.collection(USERS)
            .document(userId)
            .collection(SAVED_ACTIVITIES)
            .document(activityId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(saved)
            if (snapshot.exists()) {
                transaction.delete(saved)
                false
            } else {
                transaction.set(
                    saved,
                    mapOf(
                        "activityId" to activityId,
                        "savedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }
        }.awaitResult()
    }

    private companion object {
        const val ACTIVITIES = "activities"
        const val USERS = "users"
        const val JOINED_ACTIVITIES = "joinedActivities"
        const val SAVED_ACTIVITIES = "savedActivities"
    }
}

private suspend fun <T> Task<T>.awaitResult(): Result<T> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(task.result))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Firebase operation failed.")
                    )
                )
            }
        }
    }
