package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class UserProfile(
    @PrimaryKey val id: String = "ayush",
    val name: String = "Ayush",
    val location: String = "Kathmandu, Nepal",
    val isTravellerMode: Boolean = false,
    val rating: Double = 4.8,
    val bio: String = "Young professional based in Kathmandu. Up for local food tours, early morning trails, and weekend futsal. Let's Connect!",
    // Verification Status
    val isVerified: Boolean = false,
    val verifiedPhone: String = "",
    val verifiedNationalIdName: String = "",
    val verificationMethod: String = "" // "Phone", "ID"
)

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Play", "Explore", "Meet", "Learn", "Experience"
    val location: String,
    val date: String,
    val time: String,
    val pricePerPerson: String,
    val participantsNeeded: Int,
    val description: String,
    val organizerName: String,
    val organizerRating: Double = 4.7,
    val joinedCount: Int = 1,
    val isJoinedByMe: Boolean = false,
    val isSaved: Boolean = false,
    // Trust score and host verification flag
    val isVerifiedOrganizer: Boolean = false
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val membersCount: Int,
    val category: String,
    val isMember: Boolean = false
)

@Entity(tableName = "availability")
data class Availability(
    @PrimaryKey val userName: String,
    val statusText: String,
    val iconType: String, // "Coffee", "Sports", "Hiking", "Travel", "Networking"
    val timeAgo: String = "Just now",
    val isCurrentUser: Boolean = false,
    val isUserVerified: Boolean = false
)

@Entity(tableName = "chats")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val planId: Long,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val systemCategory: String, // "Reminder", "Recommendation", "Verification", "Welcome"
    val activityId: Long? = null
)
