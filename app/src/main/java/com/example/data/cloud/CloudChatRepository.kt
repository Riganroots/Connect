package com.example.data.cloud

import android.content.Context
import com.example.data.models.ChatMessage
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudChatRepository(context: Context) {
    private val firestore: FirebaseFirestore? =
        FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseFirestore.getInstance(it) }

    fun observeMessages(
        activityId: String,
        localPlanId: Long,
        userId: String
    ): Flow<List<ChatMessage>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close(IllegalStateException("Firestore is not configured."))
            return@callbackFlow
        }

        val registration = db.collection(ACTIVITIES)
            .document(activityId)
            .collection(MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val text = document.getString("text").orEmpty()
                    val senderId = document.getString("senderId").orEmpty()
                    if (text.isBlank() || senderId.isBlank()) {
                        return@mapNotNull null
                    }

                    val timestamp = when (val value = document.get("createdAt")) {
                        is Timestamp -> value.toDate().time
                        else -> System.currentTimeMillis()
                    }

                    ChatMessage(
                        planId = localPlanId,
                        senderName = document.getString("senderName") ?: "Connect Member",
                        messageText = text,
                        timestamp = timestamp,
                        isMe = senderId == userId
                    )
                }

                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        activityId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore is not configured."))

        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }

        val data = mapOf<String, Any>(
            "senderId" to senderId,
            "senderName" to senderName.trim().ifBlank { "Connect Member" },
            "text" to cleanText.take(MAX_MESSAGE_LENGTH),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return db.collection(ACTIVITIES)
            .document(activityId)
            .collection(MESSAGES)
            .document()
            .set(data)
            .awaitChatResult()
    }

    private companion object {
        const val ACTIVITIES = "activities"
        const val MESSAGES = "messages"
        const val MAX_MESSAGE_LENGTH = 1000
    }
}

private suspend fun Task<Void>.awaitChatResult(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(
                    Result.failure(
                        task.exception ?: IllegalStateException("Firebase chat operation failed.")
                    )
                )
            }
        }
    }
