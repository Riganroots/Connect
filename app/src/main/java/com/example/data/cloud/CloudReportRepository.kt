package com.example.data.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudReportRepository(context: Context) {
    private val firestore = FirebaseApp.getApps(context).firstOrNull()
        ?.let { FirebaseFirestore.getInstance(it) }

    suspend fun reportActivity(
        reporterId: String,
        activityId: String,
        organizerId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Reporting requires a real account."))
        if (reporterId.isBlank() || activityId.isBlank() || organizerId.isBlank() ||
            reporterId == organizerId || reason !in REASONS || details.length > 1000) {
            return Result.failure(IllegalArgumentException("Please choose a valid report reason."))
        }
        return suspendCancellableCoroutine { continuation ->
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to reporterId,
                    "activityId" to activityId,
                    "reportedUserId" to organizerId,
                    "reason" to reason,
                    "details" to details.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                continuation.resume(if (task.isSuccessful) Result.success(Unit) else
                    Result.failure(task.exception ?: IllegalStateException("Report could not be submitted.")))
            }
        }
    }

    companion object {
        val REASONS = listOf("Spam or scam", "Harassment", "Unsafe activity", "Misleading information", "Other")
    }
}
