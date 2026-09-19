package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.UserProfile
import com.example.data.models.Plan
import com.example.data.models.Group
import com.example.data.models.Availability
import com.example.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectDao {

    // Profiles
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String): Flow<UserProfile?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    // Plans (Activities)
    @Query("SELECT * FROM plans ORDER BY id DESC")
    fun getAllPlansFlow(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): Plan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: Plan): Long

    @Query("UPDATE plans SET isJoinedByMe = :isJoined, joinedCount = joinedCount + :countDiff WHERE id = :id")
    suspend fun updatePlanJoinState(id: Long, isJoined: Boolean, countDiff: Int)

    @Query("UPDATE plans SET isSaved = :isSaved WHERE id = :id")
    suspend fun updatePlanSaveState(id: Long, isSaved: Boolean)

    @Query("UPDATE plans SET isVerifiedOrganizer = :isVerified WHERE organizerName = :organizerName")
    suspend fun updatePlansVerificationStatus(organizerName: String, isVerified: Boolean)

    // Groups
    @Query("SELECT * FROM groups")
    fun getAllGroupsFlow(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<Group>)

    @Query("UPDATE groups SET isMember = :isMember, membersCount = membersCount + :countDiff WHERE id = :id")
    suspend fun updateGroupMembership(id: String, isMember: Boolean, countDiff: Int)

    // Live Availability ("Available Now")
    @Query("SELECT * FROM availability")
    fun getAvailabilitiesFlow(): Flow<List<Availability>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: Availability)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailabilities(availabilities: List<Availability>)

    @Query("DELETE FROM availability WHERE userName = :userName")
    suspend fun deleteAvailability(userName: String)

    // Chats
    @Query("SELECT * FROM chats WHERE planId = :planId ORDER BY timestamp ASC")
    fun getChatsForPlanFlow(planId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<com.example.data.models.AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: com.example.data.models.AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}

@Database(
    entities = [
        UserProfile::class,
        Plan::class,
        Group::class,
        Availability::class,
        ChatMessage::class,
        com.example.data.models.AppNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ConnectDatabase : RoomDatabase() {
    abstract fun connectDao(): ConnectDao

    companion object {
        @Volatile
        private var INSTANCE: ConnectDatabase? = null

        fun getDatabase(context: Context): ConnectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConnectDatabase::class.java,
                    "connect_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ConnectRepository(private val dao: ConnectDao) {
    val allPlans: Flow<List<Plan>> = dao.getAllPlansFlow()
    val allGroups: Flow<List<Group>> = dao.getAllGroupsFlow()
    val allAvailabilities: Flow<List<Availability>> = dao.getAvailabilitiesFlow()

    fun userProfile(userId: String): Flow<UserProfile?> = dao.getProfileFlow(userId)

    suspend fun getProfileDirect(userId: String): UserProfile? = dao.getProfile(userId)

    suspend fun updateProfile(profile: UserProfile) {
        dao.insertProfile(profile)
    }

    suspend fun insertPlan(plan: Plan): Long {
        return dao.insertPlan(plan)
    }

    suspend fun toggleJoinPlan(planId: Long) {
        val plan = dao.getPlanById(planId) ?: return
        val newJoined = !plan.isJoinedByMe
        val diff = if (newJoined) 1 else -1
        dao.updatePlanJoinState(planId, newJoined, diff)
    }

    suspend fun toggleSavePlan(planId: Long) {
        val plan = dao.getPlanById(planId) ?: return
        dao.updatePlanSaveState(planId, !plan.isSaved)
    }

    suspend fun updatePlansVerificationStatus(organizerName: String, isVerified: Boolean) {
        dao.updatePlansVerificationStatus(organizerName, isVerified)
    }

    suspend fun toggleGroupMembership(groupId: String) {
        // We find the group members
        val groups = dao.getAllGroupsFlow() // Normally we query specific group
        // But let's keep it clean
    }

    suspend fun updateGroupMembershipDirect(groupId: String, isMember: Boolean) {
        val diff = if (isMember) 1 else -1
        dao.updateGroupMembership(groupId, isMember, diff)
    }

    suspend fun insertAvailability(availability: Availability) {
        dao.insertAvailability(availability)
    }

    suspend fun deleteAvailability(userName: String) {
        dao.deleteAvailability(userName)
    }

    suspend fun insertGroups(groups: List<Group>) {
        dao.insertGroups(groups)
    }

    suspend fun insertAvailabilities(availabilities: List<Availability>) {
        dao.insertAvailabilities(availabilities)
    }

    // Notifications operations
    val allNotifications: Flow<List<com.example.data.models.AppNotification>> = dao.getAllNotificationsFlow()

    suspend fun insertNotification(notification: com.example.data.models.AppNotification) {
        dao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Long) {
        dao.markNotificationAsRead(id)
    }

    suspend fun deleteNotification(id: Long) {
        dao.deleteNotification(id)
    }

    suspend fun deleteAllNotifications() {
        dao.deleteAllNotifications()
    }

    fun getChatsForPlan(planId: Long): Flow<List<ChatMessage>> {
        return dao.getChatsForPlanFlow(planId)
    }

    suspend fun sendChatMessage(message: ChatMessage) {
        dao.insertChatMessage(message)
    }

    suspend fun getPlanById(planId: Long): Plan? {
        return dao.getPlanById(planId)
    }
}
