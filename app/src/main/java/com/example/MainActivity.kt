package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.data.models.Availability
import com.example.data.models.Group
import com.example.data.models.Plan
import com.example.data.models.UserProfile
import com.example.auth.AuthViewModel
import com.example.notifications.ConnectNotificationCenter
import com.example.notifications.PushTokenRegistrar
import com.example.ui.auth.AuthGate
import com.example.ui.theme.*
import com.example.ui.components.AppHeader
import com.example.ui.components.ConnectBottomNavigation
import com.example.ui.components.GroupCard
import com.example.ui.components.PlanCard
import com.example.ui.components.ReportActivityButton
import com.example.ui.components.BlockUserButton
import com.example.ui.components.BlockedUsersManager
import com.example.ui.components.DeleteAccountButton
import com.example.ui.screens.CreatePlanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.kathmanduDiscoverSpots

class MainActivity : ComponentActivity() {
    private val viewModel: ConnectViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val pushTokenRegistrar by lazy { PushTokenRegistrar(applicationContext) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notifications are optional; the app remains usable if permission is denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ConnectNotificationCenter.createChannel(this)
        setContent {
            MyApplicationTheme {
                AuthGate(viewModel = authViewModel) { session ->
                    LaunchedEffect(session.userId, session.isPreviewMode) {
                        viewModel.activateUser(
                            userId = session.userId,
                            suggestedName = session.suggestedName,
                            isPreviewMode = session.isPreviewMode
                        )

                        if (!session.isPreviewMode) {
                            pushTokenRegistrar.registerCurrentToken(session.userId)
                            requestNotificationPermissionIfNeeded()
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            ConnectApp(viewModel = viewModel, onSignOut = authViewModel::signOut,
                                onDeleteAccount = { password, result ->
                                    authViewModel.requestAccountDeletion(password, viewModel::endSession, result)
                                })
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val preferences = getSharedPreferences("connect_notification_preferences", MODE_PRIVATE)
        val alreadyRequested = preferences.getBoolean("permission_requested", false)
        if (alreadyRequested) return

        preferences.edit().putBoolean("permission_requested", true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun ConnectApp(viewModel: ConnectViewModel, onSignOut: () -> Unit,
    onDeleteAccount: (String, (String?) -> Unit) -> Unit = { _, result -> result("Deletion is unavailable.") }) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showProfileCreator by remember { mutableStateOf(false) }

    BackHandler(enabled = showProfileCreator || currentScreen !is Screen.Home) {
        if (showProfileCreator) {
            showProfileCreator = false
        } else {
            viewModel.navigateTo(Screen.Home)
        }
    }

    // If profile is fully custom, edit can be toggled
    if (showProfileCreator) {
        ProfileSetupDialog(
            profile = profile ?: UserProfile(),
            onDismiss = { if (!viewModel.isSavingProfile.value) showProfileCreator = false },
            saving = viewModel.isSavingProfile.collectAsStateWithLifecycle().value,
            error = viewModel.profileError.collectAsStateWithLifecycle().value,
            onSave = { name, bio, isTraveller, interests ->
                viewModel.updateProfileInfo(name, bio, isTraveller, interests) { showProfileCreator = false }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Bar
            if (currentScreen !is Screen.ChatDetail) {
                AppHeader(
                    profile = profile,
                    viewModel = viewModel,
                    onConfigureProfile = { showProfileCreator = true }
                )
            }

            // Switch Screen Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ConnectMintLight)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(viewModel = viewModel)
                    }
                    is Screen.CreatePlan -> {
                        CreatePlanScreen(
                            viewModel = viewModel,
                            onPlanPublished = {
                                viewModel.navigateTo(Screen.Home)
                            }
                        )
                    }
                    is Screen.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onEditProfile = { showProfileCreator = true },
                            onSignOut = onSignOut,
                            onDeleteAccount = onDeleteAccount
                        )
                    }
                    is Screen.ChatDetail -> {
                        ChatDetailScreen(
                            viewModel = viewModel,
                            planId = screen.planId,
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )
                    }
                }
            }

            // Bottom Nav (Only visible for Home, CreatePlan and Profile)
            if (currentScreen !is Screen.ChatDetail) {
                ConnectBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    }
}

// ======================== HOME SCREEN ========================
@Composable
fun ProfileScreen(viewModel: ConnectViewModel, onEditProfile: () -> Unit, onSignOut: () -> Unit,
    onDeleteAccount: (String, (String?) -> Unit) -> Unit = { _, result -> result("Deletion is unavailable.") }) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val plans by viewModel.profilePlans.collectAsStateWithLifecycle()
    val uid by viewModel.currentUserId.collectAsStateWithLifecycle()
    com.example.ui.screens.MemberPlansScreen(profile, plans, uid, onEditProfile, onSignOut,
        onCreate = { viewModel.navigateTo(Screen.CreatePlan) }, onDiscover = { viewModel.navigateTo(Screen.Home) },
        onSave = { viewModel.toggleSavePlan(it.id) }, onJoin = { viewModel.toggleJoinPlan(it.id) },
        onChat = { viewModel.navigateTo(Screen.ChatDetail(it.id)) }) {
        if (uid != "preview-user") {
            BlockedUsersManager(viewModel)
            if (BuildConfig.SERVER_FUNCTIONS_ENABLED) DeleteAccountButton(onDeleteAccount)
            else Text("Testing build: push notifications and account deletion are unavailable.", color = ConnectGrayMedium, fontSize = 13.sp)
        }
    }
}


// ======================== CHAT DETAIL SCREEN ========================
@Composable
fun ChatDetailScreen(
    viewModel: ConnectViewModel,
    planId: Long,
    onBack: () -> Unit
) {
    val chatsFlow = remember(planId) { viewModel.getChatsForPlan(planId) }
    val chats by chatsFlow.collectAsStateWithLifecycle()
    val chatError by viewModel.cloudChatError.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val blockError by viewModel.blockError.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var hostPlan by remember(planId) { mutableStateOf<Plan?>(null) }
    var userMessage by rememberSaveable(planId) { mutableStateOf("") }

    // Fetch parent Plan to display host contact metadata
    LaunchedEffect(planId) {
        hostPlan = viewModel.getPlanById(planId)
    }

    LaunchedEffect(chats.size) {
        if (chats.isNotEmpty()) {
            listState.animateScrollToItem(chats.lastIndex)
        }
    }

    if (hostPlan?.let { it.organizerId in blockedUsers } == true) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("This activity's host is blocked.")
            BlockUserButton(viewModel, hostPlan!!.organizerId, hostPlan!!.organizerName)
            TextButton(onClick = onBack) { Text("Back to activities") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
            .imePadding()
    ) {
        // Chat Header with host name
        Surface(
            color = ConnectWhite,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = ConnectDarkGreen
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Organizer Badge Visual
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ConnectDarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (hostPlan?.organizerName?.take(1) ?: "H").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hostPlan?.title ?: "Activity chat",
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark
                    )
                    Text(
                        text = "Group chat · Hosted by ${hostPlan?.organizerName ?: "Connect member"}",
                        fontSize = 14.sp,
                        color = ConnectGrayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ReportActivityButton(viewModel, hostPlan, planId)
                if (hostPlan?.cloudId?.isNotBlank() == true) {
                    BlockUserButton(viewModel, hostPlan!!.organizerId, hostPlan!!.organizerName)
                }
            }
        }

        blockError?.let { Text(it, modifier = Modifier.padding(12.dp)) }
        if (chatError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFFCC80))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(17.dp)
                    )
                    Column {
                        Text(
                            text = "Chat temporarily unavailable",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = chatError ?: "Please try again.",
                            fontSize = 13.sp,
                            color = ConnectGrayMedium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Message Feed List block
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = ConnectGrayMedium,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No messages yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectGrayDark
                            )
                            Text(
                                text = "Start the activity conversation.",
                                fontSize = 14.sp,
                                color = ConnectGrayMedium
                            )
                        }
                    }
                }
            }

            items(chats) { chat ->
                val bubbleAlign = if (chat.isMe) Alignment.End else Alignment.Start
                val bubbleColor = if (chat.isMe) ConnectDarkGreen else ConnectCream
                val textColor = if (chat.isMe) Color.White else ConnectGrayDark

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = bubbleAlign
                ) {
                    // Small Name stamp if other members
                    if (!chat.isMe) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.senderName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                        BlockUserButton(viewModel, chat.senderId, chat.senderName)
                        }
                    }

                    // Rounded bubble
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (chat.isMe) 12.dp else 2.dp,
                                    bottomEnd = if (chat.isMe) 2.dp else 12.dp
                                )
                            )
                            .background(bubbleColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = chat.messageText,
                            fontSize = 16.sp,
                            color = textColor,
                            lineHeight = 23.sp
                        )
                    }
                }
            }
        }

        // Typing toolbar
        Surface(
            color = ConnectCream,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { value ->
                        if (value.length <= 1000) {
                            userMessage = value
                        }
                    },
                    placeholder = { Text("Message the group", fontSize = 16.sp) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = ConnectDarkGreen,
                        unfocusedIndicatorColor = ConnectGrayLight
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    enabled = userMessage.isNotBlank(),
                    onClick = {
                        if (userMessage.isNotBlank()) {
                            viewModel.sendChatMessage(planId, userMessage)
                            userMessage = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ConnectDarkGreen,
                        contentColor = ConnectWhite
                    ),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send text",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// ======================== FULL DIALOG BOXES OVERLAYS ========================

@Composable
fun ProfileSetupDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String) -> Unit,
    saving: Boolean = false,
    error: String? = null
) {
    var nameInput by remember { mutableStateOf(profile.name) }
    var bioInput by remember { mutableStateOf(profile.bio) }
    var isTravellerOption by remember { mutableStateOf(profile.isTravellerMode) }

    val availableInterests = listOf(
        "Hiking 🏔️", "Food Walk 🥟", "Futsal ⚽", "Live Music 🎸", "Cafes ☕", 
        "Tech & Design 💻", "Language Exchange 🗣️", "Nightlife 🍹", "Temples & Heritage 🏛️", "Meditation 🧘"
    )

    var selectedInterests by remember {
        mutableStateOf(
            profile.interests.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        )
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                "Config My Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ConnectDarkGreen
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                Column {
                    Text("Display Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )
                }

                Column {
                    Text("Bio & Hobbies", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input"),
                        maxLines = 3
                    )
                }

                Column {
                    Text("Select My Interests", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val rows = availableInterests.chunked(2)
                    rows.forEach { rowInterests ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowInterests.forEach { interest ->
                                val isSelected = selectedInterests.contains(interest)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedInterests = if (isSelected) {
                                            selectedInterests - interest
                                        } else {
                                            selectedInterests + interest
                                        }
                                    },
                                    label = { Text(interest, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ConnectDarkGreen,
                                        selectedLabelColor = Color.White,
                                        containerColor = ConnectCream,
                                        labelColor = ConnectGrayDark
                                    ),
                                    modifier = Modifier.weight(1f).testTag("interest_chip_$interest")
                                )
                            }
                            if (rowInterests.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isTravellerOption,
                        onCheckedChange = { isTravellerOption = it },
                        colors = CheckboxDefaults.colors(checkedColor = ConnectDarkGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Active Traveller Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                        Text("Show traveller badge to match Kathmandu expats/hosts.", fontSize = 9.sp, color = ConnectGrayMedium)
                    }
                }
            }
        },
        confirmButton = {
            Column {
                error?.let { Text(it, color = ConnectError) }
                if (saving) Text("Saving profile… Stay connected.", fontSize = 12.sp)
            Button(
                enabled = !saving,
                onClick = { 
                    val finalInterests = selectedInterests.joinToString(", ")
                    onSave(nameInput, bioInput, isTravellerOption, finalInterests) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                modifier = Modifier.testTag("apply_profile_button")
            ) {
                Text(if (saving) "Saving…" else "Save profile")
            }
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = { onDismiss() }) {
                Text("Cancel", color = ConnectGrayMedium)
            }
        }
    )
}

@Composable
fun PostAvailabilityDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Coffee") } // Coffee, Sports, Hiking, Travel, Networking

    val tags = listOf("Coffee", "Sports", "Hiking", "Travel", "Networking")

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                "Go Live: I'm Free Now!",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ConnectDarkGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Let local Kathmandu groups know you have free hours right now. You’ll be pinned live in the upper row for matching!",
                    fontSize = 11.sp,
                    color = ConnectGrayMedium
                )

                Column {
                    Text("Current Context tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            val isSel = tag == selectedIcon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ConnectDarkGreen else ConnectCream)
                                    .clickable { selectedIcon = tag }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else ConnectGrayDark
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("My Status Text (Max 35 chars)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { if (it.length <= 35) statusText = it },
                        placeholder = { Text("e.g. Free for tea at Durbar Hill") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${statusText.length}/35 characters",
                        fontSize = 9.sp,
                        color = ConnectGrayMedium,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (statusText.isNotBlank()) {
                        onPost(statusText, selectedIcon)
                    }
                },
                enabled = statusText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
            ) {
                Text("Broadcast Live")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel", color = ConnectGrayMedium)
            }
        }
    )
}
