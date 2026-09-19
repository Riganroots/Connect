package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudActivityRepository
import com.example.data.cloud.CloudChatRepository
import com.example.data.cloud.CloudCommunityRepository
import com.example.data.database.ConnectDatabase
import com.example.data.database.ConnectRepository
import com.example.data.models.Availability
import com.example.data.models.ChatMessage
import com.example.data.models.Group
import com.example.data.models.Plan
import com.example.data.models.UserProfile
import com.example.data.models.DiscoverSpot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object CreatePlan : Screen()
    object Profile : Screen()
    data class ChatDetail(val planId: Long) : Screen()
}

class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConnectRepository
    private val cloudActivityRepository: CloudActivityRepository
    private val cloudChatRepository: CloudChatRepository
    private val cloudCommunityRepository: CloudCommunityRepository
    private var cloudActivityJob: Job? = null
    private var groupMembershipJob: Job? = null
    private var availabilityJob: Job? = null
    private val chatStates = mutableMapOf<Long, MutableStateFlow<List<ChatMessage>>>()
    private val chatJobs = mutableMapOf<Long, Job>()

    private val activeUserId = MutableStateFlow(PREVIEW_USER_ID)
    val currentUserId: StateFlow<String> = activeUserId
    val cloudActivityError = MutableStateFlow<String?>(null)
    val cloudChatError = MutableStateFlow<String?>(null)
    val cloudCommunityError = MutableStateFlow<String?>(null)

    // Screen navigation state
    val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    // Form states for Create Plan
    val formTitle = MutableStateFlow("")
    val formCategory = MutableStateFlow("Play") // Play, Explore, Meet, Learn, Experience
    val formLocation = MutableStateFlow("")
    val formDate = MutableStateFlow("")
    val formTime = MutableStateFlow("")
    val formPrice = MutableStateFlow("Free")
    val formParticipantsNeeded = MutableStateFlow("4")
    val formDescription = MutableStateFlow("")

    // Search and filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All") // "All", "Play", "Explore", "Meet", "Learn", "Experience"
    val selectedNeighborhood = MutableStateFlow("All") // "All", "Thamel", "Patan", "Boudha", "Baluwatar", "Budhanilkantha"

    // Live state streams
    val userProfile: StateFlow<UserProfile?>
    val allPlans: StateFlow<List<Plan>>
    val allGroups: StateFlow<List<Group>>
    val allAvailabilities: StateFlow<List<Availability>>
    val allNotifications: StateFlow<List<com.example.data.models.AppNotification>>
    val discoverSpots: StateFlow<List<DiscoverSpot>>

    init {
        val database = ConnectDatabase.getDatabase(application)
        repository = ConnectRepository(database.connectDao())
        cloudActivityRepository = CloudActivityRepository(application)
        cloudChatRepository = CloudChatRepository(application)
        cloudCommunityRepository = CloudCommunityRepository(application)

        val sessionPlans = combine(repository.allPlans, activeUserId) { plans, userId ->
            if (userId == PREVIEW_USER_ID) {
                plans.filter { it.cloudId.isBlank() }
            } else {
                plans.filter { it.cloudId.isNotBlank() }
            }
        }

        // Combine session-scoped plans with search, category and neighborhood filters.
        allPlans = combine(
            sessionPlans,
            searchQuery,
            selectedCategory,
            selectedNeighborhood
        ) { plans, query, cat, neighborhood ->
            plans.filter { plan ->
                val matchesQuery = plan.title.contains(query, ignoreCase = true) ||
                        plan.location.contains(query, ignoreCase = true) ||
                        plan.description.contains(query, ignoreCase = true)
                val matchesCategory = cat == "All" || plan.category.equals(cat, ignoreCase = true)
                
                val matchesNeighborhood = if (neighborhood == "All") {
                    true
                } else {
                    val locationText = plan.location.lowercase()
                    val descriptionText = plan.description.lowercase()
                    val titleText = plan.title.lowercase()
                    
                    when (neighborhood) {
                        "Thamel" -> locationText.contains("thamel") || descriptionText.contains("thamel") || titleText.contains("thamel")
                        "Patan" -> locationText.contains("patan") || locationText.contains("jhamsikhel") || locationText.contains("lalitpur") ||
                                   descriptionText.contains("patan") || descriptionText.contains("jhamsikhel") || descriptionText.contains("lalitpur") ||
                                   titleText.contains("patan") || titleText.contains("jhamsikhel") || titleText.contains("lalitpur")
                        "Boudha" -> locationText.contains("boudha") || descriptionText.contains("boudha") || titleText.contains("boudha")
                        "Baluwatar" -> locationText.contains("baluwatar") || descriptionText.contains("baluwatar") || titleText.contains("baluwatar")
                        "Budhanilkantha" -> locationText.contains("budhanilkantha") || locationText.contains("shivapuri") ||
                                            descriptionText.contains("budhanilkantha") || descriptionText.contains("shivapuri") ||
                                            titleText.contains("budhanilkantha") || titleText.contains("shivapuri")
                        else -> locationText.contains(neighborhood.lowercase()) || descriptionText.contains(neighborhood.lowercase()) || titleText.contains(neighborhood.lowercase())
                    }
                }
                
                matchesQuery && matchesCategory && matchesNeighborhood
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        userProfile = activeUserId
            .flatMapLatest { userId -> repository.userProfile(userId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        allGroups = repository.allGroups
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAvailabilities = combine(repository.allAvailabilities, activeUserId) { rows, userId ->
            if (userId == PREVIEW_USER_ID) {
                rows.filter { it.userId.startsWith(PREVIEW_ROW_PREFIX) }
            } else {
                rows.filterNot { it.userId.startsWith(PREVIEW_ROW_PREFIX) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allNotifications = repository.allNotifications
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        discoverSpots = combine(
            searchQuery,
            selectedNeighborhood
        ) { query, neighborhood ->
            kathmanduDiscoverSpots.filter { spot ->
                val matchesQuery = query.isBlank() || spot.title.contains(query, ignoreCase = true) ||
                        spot.location.contains(query, ignoreCase = true) ||
                        spot.description.contains(query, ignoreCase = true) ||
                        spot.whyVisit.contains(query, ignoreCase = true) ||
                        spot.highlight.contains(query, ignoreCase = true)
                
                val matchesNeighborhood = if (neighborhood == "All") {
                    true
                } else {
                    val locationText = spot.location.lowercase()
                    val neighborhoodText = neighborhood.lowercase()
                    if (neighborhoodText == "patan") {
                        locationText.contains("patan") || locationText.contains("jhamsikhel") || locationText.contains("lalitpur")
                    } else if (neighborhoodText == "budhanilkantha") {
                        locationText.contains("budhanilkantha") || locationText.contains("shivapuri")
                    } else {
                        locationText.contains(neighborhoodText)
                    }
                }
                
                matchesQuery && matchesNeighborhood
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), kathmanduDiscoverSpots)

        // Seed data asynchronously if empty
        viewModelScope.launch {
            seedDatabaseIfEmpty()
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        // Seed plans if none exist
        val plansExist = repository.allPlans.first().isNotEmpty()
        if (!plansExist) {
            val initialPlans = listOf(
                Plan(
                    title = "Saturday Budhanilkantha to Shivapuri Peak Hike",
                    category = "Explore",
                    location = "Shivapuri National Park Entrance, Budhanilkantha",
                    date = "Saturday, Jun 6",
                    time = "7:00 AM",
                    pricePerPerson = "Rs. 250 (Ticket fee)",
                    participantsNeeded = 8,
                    description = "We’re hiking up Shivapuri Peak! Breath-taking views of the Kathmandu valley and fresh pine air. Meet at Budhanilkantha gateway. Bring water, comfortable shoes, and your camera!",
                    organizerName = "Lisa",
                    organizerRating = 4.9,
                    joinedCount = 4,
                    isJoinedByMe = false
                ),
                Plan(
                    title = "Futsal Friendly Match (5v5 - All Welcome)",
                    category = "Play",
                    location = "Dhuku Futsal, Baluwatar",
                    date = "Tonight",
                    time = "7:30 PM",
                    pricePerPerson = "Rs. 300 (Pitch division)",
                    participantsNeeded = 3,
                    description = "Casual friendly match. Booking is confirmed, we just need 3 more players to complete equivalent teams. High spirit, no aggressive sliding or tackles allowed!",
                    organizerName = "Pemba",
                    organizerRating = 4.8,
                    joinedCount = 7,
                    isJoinedByMe = true
                ),
                Plan(
                    title = "Nepali & English Conversation exchange",
                    category = "Learn",
                    location = "Lalitpur Coffee Project, Jhamsikhel",
                    date = "Thursday, Jun 4",
                    time = "5:00 PM",
                    pricePerPerson = "Free (Self beverage purchase)",
                    participantsNeeded = 12,
                    description = "A friendly evening of language trading! Practise speaking natural English, conversational Nepali, or sweet Newari. Perfect for solo travellers, students, and active expats.",
                    organizerName = "Saman",
                    organizerRating = 4.7,
                    joinedCount = 6,
                    isJoinedByMe = false
                ),
                Plan(
                    title = "Hidden Thamel Jhol Momo & street trail",
                    category = "Experience",
                    location = "Garden of Dreams Entrance, Thamel",
                    date = "Friday, Jun 5",
                    time = "6:00 PM",
                    pricePerPerson = "Rs. 500 (Bring change for food)",
                    participantsNeeded = 6,
                    description = "We're hopping around Thamel's best-kept local momo secrets! Explored places are fully hygienic, serving delicious jhol momo, crispy kothey, and buff momos.",
                    organizerName = "Niraj",
                    organizerRating = 4.6,
                    joinedCount = 3,
                    isJoinedByMe = false
                ),
                Plan(
                    title = "Local Tech, Design & Expat network Hour",
                    category = "Meet",
                    location = "District Lounge, Jhamsikhel",
                    date = "Next Wednesday",
                    time = "6:30 PM",
                    pricePerPerson = "Rs. 500 (First drink included)",
                    participantsNeeded = 15,
                    description = "Local lounge meetup for entrepreneurs, developers, independent builders, and expats in Kathmandu. Find your travel partner or business associate here!",
                    organizerName = "Alok",
                    organizerRating = 4.9,
                    joinedCount = 11,
                    isJoinedByMe = false
                )
            )
            for (p in initialPlans) {
                repository.insertPlan(p)
            }
        }

        // Seed groups if none exist
        val groupsExist = repository.allGroups.first().isNotEmpty()
        if (!groupsExist) {
            val initialGroups = listOf(
                Group(
                    id = "futsal_group",
                    name = "Kathmandu Futsal Players",
                    description = "Weekly friendly sports, court booking shares, and recreational games in Kathmandu hub.",
                    membersCount = 142,
                    category = "Play",
                    isMember = false
                ),
                Group(
                    id = "hiking_group",
                    name = "Hiking Buddies Nepal",
                    description = "Weekend ridge hikes, overnight camps, and trails in Budhanilkantha, Kakani, and Champadevi.",
                    membersCount = 236,
                    category = "Explore",
                    isMember = true
                ),
                Group(
                    id = "coffee_group",
                    name = "Coffee Connect Kathmandu",
                    description = "Informal morning talks, startup updates, and cafe exploration in Jhamsikhel and Thamel.",
                    membersCount = 94,
                    category = "Meet",
                    isMember = false
                ),
                Group(
                    id = "nepal_explorers",
                    name = "Kathmandu Expats & Solo Travellers",
                    description = "Linking solo travellers, students, expats, and active guides in the capital city.",
                    membersCount = 118,
                    category = "Experience",
                    isMember = true
                )
            )
            repository.insertGroups(initialGroups)
        }

        // Seed live peer availability if none exist
        val availabilityExist = repository.allAvailabilities.first().isNotEmpty()
        if (!availabilityExist) {
            val initialAvailabilities = listOf(
                Availability(
                    userId = "preview-saman",
                    userName = "Saman",
                    statusText = "Free for Coffee in Jhamsikhel",
                    iconType = "Coffee",
                    timeAgo = "3m ago",
                    isCurrentUser = false
                ),
                Availability(
                    userId = "preview-lisa",
                    userName = "Lisa",
                    statusText = "Exploring Shivapuri trail",
                    iconType = "Hiking",
                    timeAgo = "11m ago",
                    isCurrentUser = false
                ),
                Availability(
                    userId = "preview-pemba",
                    userName = "Pemba",
                    statusText = "Ready for indoor futsal",
                    iconType = "Sports",
                    timeAgo = "19m ago",
                    isCurrentUser = false
                ),
                Availability(
                    userId = "preview-niraj",
                    userName = "Niraj",
                    statusText = "Let's explore Thamel street eats!",
                    iconType = "Travel",
                    timeAgo = "25m ago",
                    isCurrentUser = false
                ),
                Availability(
                    userId = "preview-alok",
                    userName = "Alok",
                    statusText = "Free for tech/startup ideas review",
                    iconType = "Networking",
                    timeAgo = "50m ago",
                    isCurrentUser = false
                )
            )
            repository.insertAvailabilities(initialAvailabilities)
        }

        // Seed default notification items to educate on trust & preferences
        val notificationExist = repository.allNotifications.first().isNotEmpty()
        if (!notificationExist) {
            val initialNotifications = listOf(
                com.example.data.models.AppNotification(
                    title = "🔒 Complete Your trust Check",
                    description = "Verify your account with a cell phone number/ID to showcase a green verified checkmark and build maximum trust in real-life meetups.",
                    systemCategory = "Verification",
                    timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
                ),
                com.example.data.models.AppNotification(
                    title = "🌲 Saturday Shivapuri Hike Near Kathmandu",
                    description = "Recommendation based on location preferences: Budhanilkantha/Shivapuri meetup has 4 empty seats. Tap 'Join Plan' now to participate!",
                    systemCategory = "Recommendation",
                    timestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
                )
            )
            for (notif in initialNotifications) {
                repository.insertNotification(notif)
            }
        }
    }

    // Toggle Join Action
    fun toggleJoinPlan(planId: Long) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planId) ?: return@launch
            val userId = activeUserId.value

            val joined = if (plan.cloudId.isNotBlank() && userId != PREVIEW_USER_ID) {
                if (plan.organizerId == userId) {
                    true
                } else {
                    cloudActivityRepository.toggleJoin(plan.cloudId, userId)
                        .onFailure { cloudActivityError.value = it.localizedMessage ?: "Could not update activity membership." }
                        .getOrElse { return@launch }
                }
            } else {
                repository.toggleJoinPlan(planId)
                repository.getPlanById(planId)?.isJoinedByMe ?: return@launch
            }

            cloudActivityError.value = null
            repository.insertNotification(
                com.example.data.models.AppNotification(
                    title = if (joined) "📅 Joined: ${plan.title}" else "👋 Left Plan: ${plan.title}",
                    description = if (joined) {
                        "Keep ${plan.date} at ${plan.time} on your calendar. Host: ${plan.organizerName}. Meeting point: ${plan.location}."
                    } else {
                        "You left this activity and your spot is available to the community again."
                    },
                    systemCategory = "Reminder",
                    activityId = planId
                )
            )
        }
    }

    fun activateUser(
        userId: String,
        suggestedName: String,
        isPreviewMode: Boolean
    ) {
        val stableId = userId.ifBlank { PREVIEW_USER_ID }
        activeUserId.value = stableId
        cloudActivityJob?.cancel()
        cloudActivityError.value = null
        chatJobs.values.forEach { it.cancel() }
        chatJobs.clear()
        chatStates.clear()
        cloudChatError.value = null
        groupMembershipJob?.cancel()
        availabilityJob?.cancel()
        cloudCommunityError.value = null

        if (!isPreviewMode) {
            cloudActivityJob = viewModelScope.launch {
                repository.clearCloudPlans()
                cloudActivityRepository.observeActivities(stableId)
                    .catch { error ->
                        cloudActivityError.value = error.localizedMessage ?: "Could not load activities."
                    }
                    .collect { plans ->
                        cloudActivityError.value = null
                        repository.syncCloudPlans(plans)
                    }
            }

            groupMembershipJob = viewModelScope.launch {
                repository.syncGroupMemberships(emptySet())
                cloudCommunityRepository.observeGroupMemberships(stableId)
                    .catch { error ->
                        cloudCommunityError.value = error.localizedMessage ?: "Could not load community memberships."
                    }
                    .collect { groupIds ->
                        cloudCommunityError.value = null
                        repository.syncGroupMemberships(groupIds)
                    }
            }

            availabilityJob = viewModelScope.launch {
                repository.clearCloudAvailabilities()
                cloudCommunityRepository.observeAvailability(stableId)
                    .catch { error ->
                        cloudCommunityError.value = error.localizedMessage ?: "Could not load live availability."
                    }
                    .collect { rows ->
                        cloudCommunityError.value = null
                        repository.syncCloudAvailabilities(rows)
                    }
            }
        } else {
            viewModelScope.launch {
                repository.syncGroupMemberships(PREVIEW_GROUP_MEMBERSHIPS)
            }
        }

        viewModelScope.launch {
            val existing = repository.getProfileDirect(stableId)
            if (existing == null) {
                repository.updateProfile(
                    UserProfile(
                        id = stableId,
                        name = if (isPreviewMode) "Preview User" else suggestedName.ifBlank { "Connect Member" },
                        location = "Kathmandu, Nepal",
                        isTravellerMode = false,
                        rating = if (isPreviewMode) 4.8 else 0.0,
                        bio = if (isPreviewMode) {
                            "Preview profile for testing Connect features on this device."
                        } else {
                            ""
                        },
                        interests = if (isPreviewMode) {
                            "Hiking 🏔️, Food Walk 🥟, Futsal ⚽"
                        } else {
                            ""
                        }
                    )
                )
            }
        }
    }

    // Toggle Saved Status
    fun toggleSavePlan(planId: Long) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planId) ?: return@launch
            val userId = activeUserId.value

            if (plan.cloudId.isNotBlank() && userId != PREVIEW_USER_ID) {
                cloudActivityRepository.toggleSaved(plan.cloudId, userId)
                    .onFailure {
                        cloudActivityError.value = it.localizedMessage ?: "Could not update saved activity."
                    }
                    .onSuccess {
                        cloudActivityError.value = null
                    }
            } else {
                repository.toggleSavePlan(planId)
            }
        }
    }

    // Community membership: private Firestore state for real users, local state in Preview Mode.
    fun toggleGroupMembership(groupId: String, currentMember: Boolean) {
        viewModelScope.launch {
            val userId = activeUserId.value
            if (userId == PREVIEW_USER_ID) {
                repository.setGroupMembership(groupId, !currentMember)
            } else {
                cloudCommunityRepository.toggleGroupMembership(groupId, userId)
                    .onFailure {
                        cloudCommunityError.value = it.localizedMessage ?: "Could not update community membership."
                    }
                    .onSuccess {
                        cloudCommunityError.value = null
                    }
            }
        }
    }

    // Shared Available Now for real users; device-only in Preview Mode.
    fun setUserAvailableNow(status: String, selectedIconType: String) {
        viewModelScope.launch {
            val userId = activeUserId.value
            val profileName = userProfile.value?.name ?: "Connect Member"

            if (userId == PREVIEW_USER_ID) {
                repository.insertAvailability(
                    Availability(
                        userId = PREVIEW_USER_ID,
                        userName = "$profileName (You)",
                        statusText = status.take(35),
                        iconType = selectedIconType,
                        timeAgo = "Just now",
                        isCurrentUser = true,
                        isUserVerified = false,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                cloudCommunityRepository.setAvailableNow(
                    userId = userId,
                    userName = profileName,
                    statusText = status,
                    iconType = selectedIconType
                )
                    .onFailure {
                        cloudCommunityError.value = it.localizedMessage ?: "Could not go live."
                    }
                    .onSuccess {
                        cloudCommunityError.value = null
                    }
            }
        }
    }

    fun removeUserAvailableNow() {
        viewModelScope.launch {
            val userId = activeUserId.value
            if (userId == PREVIEW_USER_ID) {
                repository.deleteAvailability(PREVIEW_USER_ID)
            } else {
                cloudCommunityRepository.removeAvailableNow(userId)
                    .onFailure {
                        cloudCommunityError.value = it.localizedMessage ?: "Could not go offline."
                    }
                    .onSuccess {
                        cloudCommunityError.value = null
                    }
            }
        }
    }

    // Update profile settings
    fun updateProfileInfo(name: String, bio: String, isTraveller: Boolean, interests: String) {
        viewModelScope.launch {
            val p = userProfile.value ?: UserProfile(id = activeUserId.value, name = "Connect Member")
            repository.updateProfile(
                p.copy(
                    name = name.ifBlank { "Connect Member" },
                    bio = bio,
                    isTravellerMode = isTraveller,
                    interests = interests
                )
            )
        }
    }

    // Activity chat: Firestore for real accounts, Room for Preview Mode.
    fun getChatsForPlan(planId: Long): StateFlow<List<ChatMessage>> {
        chatStates[planId]?.let { return it }

        cloudChatError.value = null
        val state = MutableStateFlow<List<ChatMessage>>(emptyList())
        chatStates[planId] = state

        chatJobs[planId] = viewModelScope.launch {
            val plan = repository.getPlanById(planId)
            if (plan == null) {
                cloudChatError.value = "Activity not found."
                return@launch
            }

            val userId = activeUserId.value
            if (plan.cloudId.isNotBlank() && userId != PREVIEW_USER_ID) {
                cloudChatRepository.observeMessages(
                    activityId = plan.cloudId,
                    localPlanId = planId,
                    userId = userId
                )
                    .catch { error ->
                        cloudChatError.value = error.localizedMessage ?: "Could not load activity chat."
                    }
                    .collect { messages ->
                        cloudChatError.value = null
                        state.value = messages
                    }
            } else {
                val localFlow = repository.getChatsForPlan(planId)

                if (localFlow.first().isEmpty()) {
                    repository.sendChatMessage(
                        ChatMessage(
                            planId = planId,
                            senderName = plan.organizerName,
                            messageText = "Hey! Thanks for expressing interest in \"${plan.title}\". We plan to gather near ${plan.location}. Let me know if you are coming!",
                            isMe = false,
                            timestamp = System.currentTimeMillis() - 60000
                        )
                    )
                }

                localFlow.collect { messages ->
                    state.value = messages
                }
            }
        }

        return state
    }

    fun sendChatMessage(planId: Long, text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        viewModelScope.launch {
            val plan = repository.getPlanById(planId) ?: return@launch
            val userId = activeUserId.value
            val senderName = userProfile.value?.name ?: "Connect Member"

            if (plan.cloudId.isNotBlank() && userId != PREVIEW_USER_ID) {
                cloudChatRepository.sendMessage(
                    activityId = plan.cloudId,
                    senderId = userId,
                    senderName = senderName,
                    text = cleanText
                )
                    .onFailure {
                        cloudChatError.value = it.localizedMessage ?: "Could not send message."
                    }
                    .onSuccess {
                        cloudChatError.value = null
                    }
            } else {
                repository.sendChatMessage(
                    ChatMessage(
                        planId = planId,
                        senderName = senderName,
                        messageText = cleanText,
                        isMe = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Publish Activity Plan
    fun publishActivityPlan(onSuccess: () -> Unit) {
        val title = formTitle.value
        val category = formCategory.value
        val location = formLocation.value
        val date = formDate.value
        val time = formTime.value
        val price = formPrice.value
        val participantsStr = formParticipantsNeeded.value
        val description = formDescription.value

        if (title.isBlank() || location.isBlank() || date.isBlank() || description.isBlank()) {
            return // simple validation
        }

        val neededInt = participantsStr.toIntOrNull() ?: 4

        viewModelScope.launch {
            val selfProfile = userProfile.value
            val isVerifiedUser = selfProfile?.isVerified ?: false
            val userId = activeUserId.value
            val p = Plan(
                organizerId = userId,
                title = title,
                category = category,
                location = location,
                date = date,
                time = time.ifBlank { "Anytime" },
                pricePerPerson = price.ifBlank { "Free" },
                participantsNeeded = neededInt,
                description = description,
                organizerName = selfProfile?.name ?: "Connect Member",
                organizerRating = selfProfile?.rating ?: 0.0,
                joinedCount = 1,
                isJoinedByMe = true,
                isVerifiedOrganizer = if (userId == PREVIEW_USER_ID) isVerifiedUser else false
            )

            val planId = if (userId == PREVIEW_USER_ID) {
                repository.insertPlan(p)
            } else {
                val cloudId = cloudActivityRepository.createActivity(p, userId)
                    .onFailure { cloudActivityError.value = it.localizedMessage ?: "Could not publish activity." }
                    .getOrElse { return@launch }

                cloudActivityError.value = null
                repository.insertPlan(p.copy(cloudId = cloudId))
            }

            // Insert system notification log
            repository.insertNotification(
                com.example.data.models.AppNotification(
                    title = "🚀 Your activity is live!",
                    description = "Successfully published '$title' for the location $location. Nearby locals will run across this recommendation.",
                    systemCategory = "Recommendation",
                    activityId = planId
                )
            )

            // Clear form
            formTitle.value = ""
            formCategory.value = "Play"
            formLocation.value = ""
            formDate.value = ""
            formTime.value = ""
            formPrice.value = "Free"
            formParticipantsNeeded.value = "4"
            formDescription.value = ""

            onSuccess()
        }
    }

    // Notifications state controls
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }

    // Verify User Account via Phone Check / simple ID submission
    fun submitUserPhoneVerification(phoneNumber: String, nationalIdName: String, method: String = "Phone & ID Check", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile(id = activeUserId.value, name = "Connect Member")
            val updated = current.copy(
                isVerified = true,
                verifiedPhone = phoneNumber,
                verifiedNationalIdName = nationalIdName.ifBlank { "National ID Verified" },
                verificationMethod = method
            )
            repository.updateProfile(updated)

            // Prototype verification must never affect real cloud trust state.
            if (activeUserId.value == PREVIEW_USER_ID) {
                repository.updatePlansVerificationStatus(updated.name, true)
                repository.insertAvailability(
                    Availability(
                        userId = PREVIEW_USER_ID,
                        userName = "${updated.name} (You)",
                        statusText = "Ready to meet - Preview verification",
                        iconType = "Coffee",
                        isCurrentUser = true,
                        isUserVerified = true,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }

            // Insert matching Success Notification for complete trust transparency
            repository.insertNotification(
                com.example.data.models.AppNotification(
                    title = "🛡️ Trust Verified Successfully!",
                    description = "Identity check complete using $method. You have earned a custom green Shield Badge next to your profile to build community trust.",
                    systemCategory = "Verification"
                )
            )
            onSuccess()
        }
    }

    fun revokeUserVerification() {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile(id = activeUserId.value, name = "Connect Member")
            val updated = current.copy(
                isVerified = false,
                verifiedPhone = "",
                verifiedNationalIdName = "",
                verificationMethod = ""
            )
            repository.updateProfile(updated)
            
            if (activeUserId.value == PREVIEW_USER_ID) {
                repository.updatePlansVerificationStatus(updated.name, false)
                repository.deleteAvailability(PREVIEW_USER_ID)
            }

            repository.insertNotification(
                com.example.data.models.AppNotification(
                    title = "⚠️ Verification Status Reset",
                    description = "Your phone verification status has been detached. Re-verify anytime from the Trust Center in your profile tab.",
                    systemCategory = "Verification"
                )
            )
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    fun prefillPlan(title: String, location: String, category: String, description: String) {
        formTitle.value = title
        formLocation.value = location
        formCategory.value = category
        formDescription.value = description
        currentScreen.value = Screen.CreatePlan
    }

    suspend fun getPlanById(planId: Long): Plan? {
        return repository.getPlanById(planId)
    }
}

val kathmanduDiscoverSpots = listOf(
    // 1. Hidden Gems
    DiscoverSpot(
        id = "gem_taragaon",
        title = "Taragaon Museum",
        description = "A stunning modernist red brick design masterpiece preserving Kathmandu's artistic history.",
        section = "Hidden Gems",
        location = "Boudha",
        rating = 4.8,
        highlight = "Modernist Brick Architecture",
        emoji = "🏛️",
        bestTime = "Afternoon 2 PM - 5 PM",
        whyVisit = "Extremely quiet courtyard, brilliant photo archives, and beautiful circular windows."
    ),
    DiscoverSpot(
        id = "gem_garden_secret",
        title = "Secret Alcove in Garden of Dreams",
        description = "The historic neo-classical garden's quiet corner, tucked away behind ivy arches.",
        section = "Hidden Gems",
        location = "Thamel",
        rating = 4.7,
        highlight = "Ivy Archways & Cafe",
        emoji = "🌸",
        bestTime = "Evening 4 PM - 6 PM",
        whyVisit = "Listen to gentle waterfalls and enjoy peaceful coffee away from bustling Thamel lanes."
    ),
    DiscoverSpot(
        id = "gem_swayambhu_back",
        title = "Swayambhu Back Forest Trails",
        description = "A quiet, winding staircase pathway going up the rear moss-covered forest of Monkey Temple.",
        section = "Hidden Gems",
        location = "Swayambhu",
        rating = 4.9,
        highlight = "Mossy Steps & Pines",
        emoji = "🌲",
        bestTime = "Early Morning 6 AM",
        whyVisit = "Fresh pine scent, zero tourist crowds, and mystical local shrines surrounded by deer."
    ),
    DiscoverSpot(
        id = "gem_chhauni",
        title = "Chhauni Museum Green Lawn",
        description = "Lush emerald grounds surrounding ancient stone shrines and bronze sculpture gallery.",
        section = "Hidden Gems",
        location = "Chhauni",
        rating = 4.6,
        highlight = "Bronze Art Heritage",
        emoji = "🏺",
        bestTime = "Morning 10 AM - Noon",
        whyVisit = "Walk under centuries-old trees with tranquil birdsong and historical stone architecture."
    ),

    // 2. Food & Cafes
    DiscoverSpot(
        id = "food_java_patan",
        title = "Himalayan Java Cafe Patan",
        description = "Beautiful multi-story brick wood lounge directly overlooking traditional Newari alleys.",
        section = "Food & Cafes",
        location = "Patan",
        rating = 4.7,
        highlight = "Double Shot Cortado",
        emoji = "☕",
        bestTime = "Anytime",
        whyVisit = "Great high-speed internet, premium local single-origin beans, and cozy windows for people-watching."
    ),
    DiscoverSpot(
        id = "food_letrio",
        title = "Le Trio Jhamsikhel",
        description = "Trendy modern eatery famous for introducing delicious spicy Newari jhol momos in a clean vibe.",
        section = "Food & Cafes",
        location = "Jhamsikhel",
        rating = 4.8,
        highlight = "Jhol Momos & Potato Baskets",
        emoji = "🥟",
        bestTime = "Lunch 12 PM - 2 PM",
        whyVisit = "Mouthwatering cold sesame broth momos and a relaxed outdoor garden terrace."
    ),
    DiscoverSpot(
        id = "food_rosemary",
        title = "Rosemary Kitchen",
        description = "An organic courtyard restaurant in a quiet Thamel lane serving superb local-European fusion.",
        section = "Food & Cafes",
        location = "Thamel",
        rating = 4.9,
        highlight = "Nepali Fusion Thali",
        emoji = "🍽️",
        bestTime = "Dinner 7 PM - 9 PM",
        whyVisit = "Outstanding hospitality, candle-lit dining, and ingredients sourced directly from village farms."
    ),
    DiscoverSpot(
        id = "food_bhaktapur_khaja",
        title = "Traditional Newari Khaja Ghar",
        description = "An authentic clay-stove eatery serving spicy barbecued meats and traditional Newari plates.",
        section = "Food & Cafes",
        location = "Bhaktapur",
        rating = 4.6,
        highlight = "Samay Baji Platter & Chyang",
        emoji = "🥢",
        bestTime = "Lunch 1 PM - 3 PM",
        whyVisit = "Taste wood-fired spiced buff (Choila), flattened rice, and creamy local sweet curd (Juju Dhau)."
    ),

    // 3. Nightlife / Clubs / Pubs
    DiscoverSpot(
        id = "night_lod",
        title = "Lord of the Drinks (LOD)",
        description = "Global top-100 ranked club with futuristic kinetic lighting and international DJ nights.",
        section = "Nightlife / Clubs / Pubs",
        location = "Thamel",
        rating = 4.9,
        highlight = "Kinetic Lighting & DJs",
        emoji = "🕺",
        bestTime = "Friday Night 10 PM+",
        whyVisit = "Massive multi-level dancefloor, explosive sound, and unmatched high-energy crowd."
    ),
    DiscoverSpot(
        id = "night_jazz",
        title = "Jazz Upstairs",
        description = "Legendary dimly lit wood pub hosting Kathmandu's finest live acoustic and jazz jam sessions.",
        section = "Nightlife / Clubs / Pubs",
        location = "Jhamsikhel",
        rating = 4.8,
        highlight = "Live Jazz Jam & Newari Snacks",
        emoji = "🎷",
        bestTime = "Saturday 8 PM+",
        whyVisit = "Vintage jazz posters, casual low-seating tables, and amazing local musicians jamming together."
    ),
    DiscoverSpot(
        id = "night_purplehaze",
        title = "Purple Haze Rock Bar",
        description = "Kathmandu's premier heavy rock arena. High ceilings, huge stage, and hard-rock tribute acts.",
        section = "Nightlife / Clubs / Pubs",
        location = "Thamel",
        rating = 4.8,
        highlight = "Local Rock Tribute Bands",
        emoji = "🎸",
        bestTime = "Nightly 9 PM+",
        whyVisit = "Headbang to iconic classic rock covers, delicious pub food, and electric stadium-like energy."
    ),
    DiscoverSpot(
        id = "night_trisara",
        title = "Trisara Lounge",
        description = "Beautiful open-air botanical garden restaurant with daily live acoustic artists and firepits.",
        section = "Nightlife / Clubs / Pubs",
        location = "Durbar Marg",
        rating = 4.7,
        highlight = "Acoustic Firepit Nights",
        emoji = "🔥",
        bestTime = "Weekend Evening 7 PM+",
        whyVisit = "Cozy and premium, perfect for chatting with friends over live light covers of global hits."
    ),

    // 4. Weekend Trips / Hiking
    DiscoverSpot(
        id = "trip_nagarkot",
        title = "Nagarkot Sunrise Ridge Hike",
        description = "Walk along the lush pine-forested ridge to see the sun rising over the Mt. Everest range.",
        section = "Weekend Trips",
        location = "Nagarkot",
        rating = 4.9,
        highlight = "Mt. Everest Horizon View",
        emoji = "🏔️",
        bestTime = "Saturday 5:30 AM",
        whyVisit = "Breathtaking pink skies, panoramic mountain vistas, and fresh mountain air away from city dust."
    ),
    DiscoverSpot(
        id = "trip_shivapuri",
        title = "Shivapuri Peak National Park Trek",
        description = "Challenging forest trail winding past mossy trees, monasteries, and the Bagdwar water spring.",
        section = "Weekend Trips",
        location = "Shivapuri",
        rating = 4.8,
        highlight = "Bagdwar Holy Water Source",
        emoji = "🥾",
        bestTime = "Saturday 8:00 AM",
        whyVisit = "Trek through dense subtropical oak forests, hear local birds, and stand atop the 2,730m peak."
    ),
    DiscoverSpot(
        id = "trip_chandragiri",
        title = "Chandragiri Cable Car & Summit",
        description = "Soar above the clouds on a scenic cable car ride to a majestic hilltop Shiva temple.",
        section = "Weekend Trips",
        location = "Chandragiri",
        rating = 4.7,
        highlight = "Cable Car above Clouds",
        emoji = "🚡",
        bestTime = "Morning 9:00 AM",
        whyVisit = "Stunning bird's eye view of Kathmandu Valley flanked by snowcapped Himalayan peaks."
    ),
    DiscoverSpot(
        id = "trip_champadevi",
        title = "Champadevi Forest Trail",
        description = "An uphill ridge hike starting near Pharping and leading to a peaceful mountaintop Buddhist stupa.",
        section = "Weekend Trips",
        location = "Pharping",
        rating = 4.7,
        highlight = "Pine Ridge Buddhist Stupa",
        emoji = "🧗",
        bestTime = "Sunday 7:30 AM",
        whyVisit = "Enchanting incense smells, colorful prayer flags blowing, and spectacular valley rim photography."
    ),

    // 5. Popular Activities / Things to Do
    DiscoverSpot(
        id = "act_boudha_kora",
        title = "Boudha Stupa Sunset Kora",
        description = "Join thousands of devotees walking around the glowing white hemispherical dome.",
        section = "Popular Activities",
        location = "Boudha",
        rating = 4.9,
        highlight = "Glowing Dome & Mantra Chants",
        emoji = "☸️",
        bestTime = "Sunset 5:30 PM - 7:00 PM",
        whyVisit = "Smell organic butter lamps, hear deep Buddhist horns, and see prayer wheels spin as evening falls."
    ),
    DiscoverSpot(
        id = "act_patan_walk",
        title = "Patan Golden Temple Secret Tour",
        description = "Explore medieval courtyard brick paths leading into a gilded 12th-century monastery.",
        section = "Popular Activities",
        location = "Patan",
        rating = 4.8,
        highlight = "12th Century Gilded Monastery",
        emoji = "🕌",
        bestTime = "Morning 10 AM",
        whyVisit = "Admire metalwork scriptures, feed traditional holy tortoises, and feel ancient spiritual vibes."
    ),
    DiscoverSpot(
        id = "act_pottery",
        title = "Bhaktapur Pottery Hands-On Workshop",
        description = "Learn the traditional art of molding black clay on a massive wooden spinning wheel.",
        section = "Popular Activities",
        location = "Bhaktapur",
        rating = 4.7,
        highlight = "Traditional Clay Shaping",
        emoji = "🏺",
        bestTime = "Afternoon 1 PM - 3 PM",
        whyVisit = "Work with local master artisans, dry your customized clay pot in the sun, and take it home!"
    ),
    DiscoverSpot(
        id = "act_jhamsikhel_murals",
        title = "Jhamsikhel Art & Murals Exploration",
        description = "Walk through quirky alleys lined with social-commentary street art and design boutiques.",
        section = "Popular Activities",
        location = "Jhamsikhel",
        rating = 4.6,
        highlight = "Street Murals & Independent Galleries",
        emoji = "🎨",
        bestTime = "Afternoon 3 PM - 5 PM",
        whyVisit = "Stunning photogenic backdrops, local artisanal shops, and modern hipster bars to drop into."
    )
)



private const val PREVIEW_USER_ID = "preview-user"
private const val PREVIEW_ROW_PREFIX = "preview-"
private val PREVIEW_GROUP_MEMBERSHIPS = setOf("hiking_group", "nepal_explorers")
