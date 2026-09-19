package com.example.data.cloud

import android.content.Context
import com.example.data.models.Plan
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
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

        val registration = db.collection(ACTIVITIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val plans = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val title = document.getString("title").orEmpty()
                    if (title.isBlank()) return@mapNotNull null

                    val participantIds = (document.get("participantIds") as? List<*>)
                        .orEmpty()
                        .filterIsInstance<String>()

                    Plan(
                        cloudId = document.id,
                        organizerId = document.getString("organizerId").orEmpty(),
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
                        joinedCount = participantIds.size,
                        isJoinedByMe = participantIds.contains(userId),
                        isSaved = false,
                        isVerifiedOrganizer = document.getBoolean("isVerifiedOrganizer") ?: false
                    )
                }

                trySend(plans)
            }

        awaitClose { registration.remove() }
    }

    suspend fun createActivity(plan: Plan, organizerId: String): Result<String> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val document = db.collection(ACTIVITIES).document()
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
            "participantIds" to listOf(organizerId),
            "isVerifiedOrganizer" to plan.isVerifiedOrganizer,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return document.set(data).awaitResult().map { document.id }
    }

    suspend fun toggleJoin(activityId: String, userId: String): Result<Boolean> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val reference = db.collection(ACTIVITIES).document(activityId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            val organizerId = snapshot.getString("organizerId").orEmpty()
            val participantIds = (snapshot.get("participantIds") as? List<*>)
                .orEmpty()
                .filterIsInstance<String>()
                .toMutableList()

            if (organizerId == userId) {
                return@runTransaction true
            }

            val isCurrentlyJoined = participantIds.contains(userId)
            if (isCurrentlyJoined) {
                participantIds.removeAll { it == userId }
            } else {
                participantIds.add(userId)
            }

            transaction.update(
                reference,
                mapOf(
                    "participantIds" to participantIds.distinct(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            !isCurrentlyJoined
        }.awaitResult()
    }

    private companion object {
        const val ACTIVITIES = "activities"
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
