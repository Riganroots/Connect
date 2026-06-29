package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.models.Availability
import com.example.data.models.Group
import com.example.data.models.Plan
import com.example.data.models.UserProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.kathmanduDiscoverSpots

class MainActivity : ComponentActivity() {
    private val viewModel: ConnectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ConnectApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectApp(viewModel: ConnectViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showProfileCreator by remember { mutableStateOf(false) }

    // If profile is fully custom, edit can be toggled
    if (showProfileCreator) {
        ProfileSetupDialog(
            profile = profile ?: UserProfile(),
            onDismiss = { showProfileCreator = false },
            onSave = { name, bio, isTraveller, interests ->
                viewModel.updateProfileInfo(name, bio, isTraveller, interests)
                showProfileCreator = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Bar
            AppHeader(
                profile = profile,
                viewModel = viewModel,
                onConfigureProfile = { showProfileCreator = true }
            )

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
                            onEditProfile = { showProfileCreator = true }
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

@Composable
fun AppHeader(
    profile: UserProfile?,
    viewModel: ConnectViewModel,
    onConfigureProfile: () -> Unit
) {
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    var showNotifDrawer by remember { mutableStateOf(false) }

    if (showNotifDrawer) {
        AlertDialog(
            onDismissRequest = { showNotifDrawer = false },
            confirmButton = {
                TextButton(onClick = { showNotifDrawer = false }) {
                    Text("Close", color = ConnectDarkGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllNotifications() }) {
                        Text("Clear All", color = ConnectError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts list",
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notifications & Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No notifications logo",
                                tint = ConnectGrayMedium.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "All Caught Up!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectGrayDark
                            )
                            Text(
                                text = "Calendar reminders and trust updates will list here when they occur.",
                                fontSize = 11.sp,
                                color = ConnectGrayMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notifications) { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ConnectCream),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val icon = when (notif.systemCategory) {
                                            "Reminder" -> Icons.Default.DateRange
                                            "Verification" -> Icons.Default.CheckCircle
                                            "Recommendation" -> Icons.Default.LocationOn
                                            else -> Icons.Default.Notifications
                                        }
                                        val tint = when (notif.systemCategory) {
                                            "Reminder" -> ConnectBlue
                                            "Verification" -> Color(0xFF48BB78)
                                            "Recommendation" -> ConnectGold
                                            else -> ConnectGrayMedium
                                        }

                                        Row(modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = notif.systemCategory,
                                                tint = tint,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(top = 1.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = notif.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ConnectGrayDark
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = notif.description,
                                                    fontSize = 10.sp,
                                                    color = ConnectGrayMedium,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteNotification(notif.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear single alert",
                                                tint = ConnectGrayMedium.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = ConnectMintLight
        )
    }

    Surface(
        color = ConnectDarkGreen,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Connect",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectMint,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "“I have a plan. Who wants to join?”",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = ConnectMint.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Notification Icon Bell
                    Box(modifier = Modifier.wrapContentSize()) {
                        IconButton(
                            onClick = { showNotifDrawer = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ConnectMidGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Show Connect Alerts",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (notifications.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .background(Color(0xFFE53E3E), CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    // Mini Profile Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ConnectMidGreen)
                            .clickable { onConfigureProfile() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(if (profile?.isVerified == true) Color(0xFF48BB78) else Color.Green, CircleShape)
                                .border(1.dp, ConnectWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile?.isVerified == true) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified Local check",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile?.name ?: "Ayush",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-bar with active location & active traveler pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin icon",
                        tint = ConnectMint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = profile?.location ?: "Kathmandu, Nepal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (profile?.isTravellerMode == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ConnectMint.copy(alpha = 0.25f))
                            .border(1.dp, ConnectMint, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Star guide icon",
                                tint = ConnectMint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Traveller Mode",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectMint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        tonalElevation = 6.dp
    ) {
        NavigationBar(
            containerColor = Color.White,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = currentScreen is Screen.Home,
                onClick = { onNavigate(Screen.Home) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Explore Hub"
                    )
                },
                label = { Text("Discover", fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ConnectWhite,
                    selectedTextColor = ConnectDarkGreen,
                    indicatorColor = ConnectDarkGreen,
                    unselectedIconColor = ConnectGrayMedium,
                    unselectedTextColor = ConnectGrayMedium
                )
            )

            NavigationBarItem(
                selected = currentScreen is Screen.CreatePlan,
                onClick = { onNavigate(Screen.CreatePlan) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Plan"
                    )
                },
                label = { Text("Post Plan", fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ConnectWhite,
                    selectedTextColor = ConnectDarkGreen,
                    indicatorColor = ConnectDarkGreen,
                    unselectedIconColor = ConnectGrayMedium,
                    unselectedTextColor = ConnectGrayMedium
                )
            )

            NavigationBarItem(
                selected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My profile"
                    )
                },
                label = { Text("My Profile", fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ConnectWhite,
                    selectedTextColor = ConnectDarkGreen,
                    indicatorColor = ConnectDarkGreen,
                    unselectedIconColor = ConnectGrayMedium,
                    unselectedTextColor = ConnectGrayMedium
                )
            )
        }
    }
}

// ======================== HOME SCREEN ========================
// ======================== HOME SCREEN ========================
@Composable
fun HomeScreen(viewModel: ConnectViewModel) {
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val availabilities by viewModel.allAvailabilities.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeNeighborhood by viewModel.selectedNeighborhood.collectAsStateWithLifecycle()
    val filteredSpots by viewModel.discoverSpots.collectAsStateWithLifecycle()

    var showAvailabilityPost by remember { mutableStateOf(false) }
    var subTabSelected by remember { mutableStateOf(0) } // 0 = Discover Guide, 1 = Live Community Feed
    
    var selectedSpotDetail by remember { mutableStateOf<com.example.data.models.DiscoverSpot?>(null) }
    var showSurpriseDialog by remember { mutableStateOf(false) }
    var surpriseSpot by remember { mutableStateOf<com.example.data.models.DiscoverSpot?>(null) }

    if (showAvailabilityPost) {
        PostAvailabilityDialog(
            onDismiss = { showAvailabilityPost = false },
            onPost = { text, iconType ->
                viewModel.setUserAvailableNow(text, iconType)
                showAvailabilityPost = false
            }
        )
    }

    // Spot Details Dialog
    selectedSpotDetail?.let { spot ->
        AlertDialog(
            onDismissRequest = { selectedSpotDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(ConnectMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(spot.emoji, fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = spot.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "📍 " + spot.location,
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RATING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = ConnectGold, modifier = Modifier.size(14.dp))
                                Text(spot.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                            }
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("BEST TIME TO VISIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                            Text(spot.bestTime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                        }
                    }

                    Column {
                        Text("HIGHLIGHT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ConnectCream, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(spot.highlight, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                        }
                    }

                    Column {
                        Text("DESCRIPTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                        Text(
                            text = spot.description,
                            fontSize = 12.sp,
                            color = ConnectGrayDark,
                            lineHeight = 16.sp
                        )
                    }

                    Column {
                        Text("INSIDER SECRET / WHY VISIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                        Text(
                            text = spot.whyVisit,
                            fontSize = 12.sp,
                            color = ConnectGrayDark,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.prefillPlan(
                            title = "Meetup at ${spot.title}",
                            location = "${spot.title}, ${spot.location}",
                            category = "Experience",
                            description = "Hey everyone! Let's meet up and discover ${spot.title} together in ${spot.location}.\n\nHighlight of this spot: ${spot.highlight}.\nHope to see you there!"
                        )
                        selectedSpotDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                    modifier = Modifier.testTag("host_at_spot_button")
                ) {
                    Text("Host Meetup Here 📅")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSpotDetail = null }) {
                    Text("Close", color = ConnectGrayMedium)
                }
            }
        )
    }

    // "I'm Bored" Surprise Suggestion Dialog
    if (showSurpriseDialog && surpriseSpot != null) {
        val spot = surpriseSpot!!
        AlertDialog(
            onDismissRequest = { showSurpriseDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎉 SURPRISE SELECTION 🎉", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ConnectDarkGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(ConnectMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(spot.emoji, fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = spot.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectGrayDark
                            )
                            Text(
                                text = "📍 " + spot.location,
                                fontSize = 11.sp,
                                color = ConnectGrayMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ConnectCream, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("INSIDER HIGHLIGHT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                            Text(spot.highlight, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("WHY VISIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                            Text(spot.whyVisit, fontSize = 11.sp, color = ConnectGrayDark, lineHeight = 15.sp)
                        }
                    }

                    Text(
                        text = spot.description,
                        fontSize = 12.sp,
                        color = ConnectGrayDark,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.prefillPlan(
                            title = "Meetup at ${spot.title}",
                            location = "${spot.title}, ${spot.location}",
                            category = "Experience",
                            description = "Hey everyone! Let's meet up and discover ${spot.title} together in ${spot.location}.\n\nHighlight of this spot: ${spot.highlight}.\nHope to see you there!"
                        )
                        showSurpriseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                    modifier = Modifier.testTag("surprise_host_button")
                ) {
                    Text("Host Meetup Here 📅")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSurpriseDialog = false }) {
                    Text("Nice, Thanks! 👍", color = ConnectGrayMedium)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Search Bar Field
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ConnectCream),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search sports, hiking, cafes in Kathmandu...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        // 1.5 Sub-tabs Selection
        item {
            TabRow(
                selectedTabIndex = subTabSelected,
                containerColor = Color.Transparent,
                contentColor = ConnectDarkGreen,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = subTabSelected == 0,
                    onClick = { subTabSelected = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔍 Explore Guide", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ConnectDarkGreen,
                    unselectedContentColor = ConnectGrayMedium
                )
                Tab(
                    selected = subTabSelected == 1,
                    onClick = { subTabSelected = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📅 Community Feed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ConnectDarkGreen,
                    unselectedContentColor = ConnectGrayMedium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1.8 Global Neighborhood row selector
        item {
            Text(
                text = "Neighborhood Filter",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = ConnectGrayDark
            )

            NeighborhoodRow(
                activeNeighborhood = activeNeighborhood,
                onSelected = { viewModel.selectedNeighborhood.value = it }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (subTabSelected == 0) {
            // ======================== EXPLORE GUIDE SUBTAB ========================
            
            // A. I'm Bored / Surprise Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("bored_banner_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ConnectCream),
                    border = BorderStroke(1.dp, ConnectDarkGreen.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bored in Kathmandu today? 🥱",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Can't decide what to do? Let our magic guide pick a surprise spot or local activity for you instantly!",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val spots = filteredSpots
                                if (spots.isNotEmpty()) {
                                    surpriseSpot = spots.random()
                                    showSurpriseDialog = true
                                } else {
                                    // Fallback to global list if filtered is empty
                                    surpriseSpot = kathmanduDiscoverSpots.random()
                                    showSurpriseDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("surprise_me_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("✨ Surprise Me!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // B. Smart Suggestions (Time-of-day dynamic recommendation engine)
            item {
                val calendar = remember { java.util.Calendar.getInstance() }
                val hour = remember { calendar.get(java.util.Calendar.HOUR_OF_DAY) }

                // Determine dynamic suggestion based on real hour
                val (timeTag, timeEmoji, smartSpots) = remember(hour, filteredSpots) {
                    when (hour) {
                        in 5..11 -> Triple("Morning Exploration", "☀️", filteredSpots.filter { it.bestTime.lowercase().contains("morning") || it.id == "food_java_patan" })
                        in 12..16 -> Triple("Afternoon Chill & Cafes", "☕", filteredSpots.filter { it.bestTime.lowercase().contains("afternoon") || it.bestTime.lowercase().contains("lunch") })
                        in 17..23 -> Triple("Nightlife & Evening Vibes", "🌙", filteredSpots.filter { it.bestTime.lowercase().contains("evening") || it.bestTime.lowercase().contains("night") })
                        else -> Triple("Late Night Adventures", "🌌", filteredSpots.filter { it.section.contains("Nightlife") })
                    }
                }

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Smart Suggestions $timeEmoji",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ConnectMint)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = timeTag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConnectDarkGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val displaySpots = if (smartSpots.isNotEmpty()) smartSpots else filteredSpots.take(3)
                    if (displaySpots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(ConnectCream, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching smart spots. Try choosing 'All Areas'!", fontSize = 11.sp, color = ConnectGrayMedium)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("smart_suggestions_row")
                        ) {
                            items(displaySpots) { spot ->
                                SmartSpotCard(spot = spot, onClick = { selectedSpotDetail = spot })
                            }
                        }
                    }
                }
            }

            // C. Explore Guide categorized horizontal lists
            val sections = listOf(
                "Hidden Gems" to "💎 Hidden Gems Near You",
                "Food & Cafes" to "🥟 Food & Cafes",
                "Nightlife / Clubs / Pubs" to "🎸 Nightlife / Clubs / Pubs",
                "Weekend Trips" to "🏔️ Weekend Trips & Hiking",
                "Popular Activities" to "☸️ Popular Activities & Cultural Sites"
            )

            sections.forEach { (sectionName, sectionHeader) ->
                item {
                    val sectionSpots = filteredSpots.filter { it.section == sectionName }
                    if (sectionSpots.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = sectionHeader,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectGrayDark,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("section_row_${sectionName.replace(" ", "_")}")
                            ) {
                                items(sectionSpots) { spot ->
                                    DiscoverSpotCard(spot = spot, onClick = { selectedSpotDetail = spot })
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ======================== COMMUNITY FEED SUBTAB ========================

            // 2. Categories List Selector
            item {
                Text(
                    text = "Categories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = ConnectGrayDark
                )

                CategoryRow(
                    activeCategory = activeCategory,
                    onSelected = { viewModel.selectedCategory.value = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2.5. Kathmandu Interactive Activity Map
            item {
                KathmanduActivityMap(
                    plans = plans,
                    selectedNeighborhood = activeNeighborhood,
                    onSelected = { viewModel.selectedNeighborhood.value = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Live "Available Now" Panel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Available Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                    }

                    val hasMyAvailability = availabilities.any { it.isCurrentUser }
                    if (hasMyAvailability) {
                        viewModel.removeUserAvailableNow()
                    } else {
                        TextButton(onClick = { showAvailabilityPost = true }) {
                            Text("+ Go Live", color = ConnectDarkGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (availabilities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ConnectMintLight, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nobody is live right now. Be the first to shout!",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = ConnectGrayMedium
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(availabilities) { av ->
                            AvailabilityPill(av = av)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Hot Recommended Communities/Groups
            item {
                Text(
                    text = "Tribes & Groups in Kathmandu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = ConnectGrayDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { gp ->
                        GroupCard(gp = gp, onJoinToggle = {
                            viewModel.toggleGroupMembership(gp.id, gp.isMember)
                        })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Activity Feed header
            item {
                val selectedAreaLabel = when (activeNeighborhood) {
                    "All" -> "All Areas"
                    "Patan" -> "Patan & Jhamsikhel"
                    else -> activeNeighborhood
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeCategory == "All") "Plans in $selectedAreaLabel" else "$activeCategory in $selectedAreaLabel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeCategory != "All" || activeNeighborhood != "All") {
                        TextButton(
                            onClick = {
                                viewModel.selectedCategory.value = "All"
                                viewModel.selectedNeighborhood.value = "All"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Text(
                                text = "Reset Filters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectDarkGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. Plans list or Empty placeholders
            if (plans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Empty list",
                                tint = ConnectGrayMedium,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active plans found for limit filters.",
                                color = ConnectGrayDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Be a host! Tap \"Post Plan\" in bottom navigator to publish your plan.",
                                color = ConnectGrayMedium,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(plans) { plan ->
                    PlanCard(
                        plan = plan,
                        onJoinToggle = { viewModel.toggleJoinPlan(plan.id) },
                        onSaveToggle = { viewModel.toggleSavePlan(plan.id) },
                        onChatClick = { viewModel.navigateTo(Screen.ChatDetail(plan.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverSpotCard(spot: com.example.data.models.DiscoverSpot, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(160.dp)
            .clickable { onClick() }
            .testTag("discover_spot_card_${spot.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ConnectGrayLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Emoji Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ConnectMint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(spot.emoji, fontSize = 18.sp)
                }
                
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = ConnectGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = spot.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark
                    )
                }
            }
            
            Column {
                Text(
                    text = spot.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ConnectGrayDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = spot.location,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayMedium
                    )
                }
            }
            
            // Highlight text banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConnectCream, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = spot.highlight,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectDarkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SmartSpotCard(spot: com.example.data.models.DiscoverSpot, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(130.dp)
            .clickable { onClick() }
            .testTag("smart_spot_card_${spot.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectDarkGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(spot.emoji, fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = spot.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📍 " + spot.location,
                            fontSize = 10.sp,
                            color = ConnectMint
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = ConnectGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = spot.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = spot.description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ Best: ${spot.bestTime}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectMint
                )
                Text(
                    text = "Explore ➔",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun KathmanduActivityMap(
    plans: List<Plan>,
    selectedNeighborhood: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("kathmandu_map_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, ConnectGrayLight)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Map icon",
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Interactive Kathmandu Activity Map",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "Visualize active plans across Kathmandu valley",
                            fontSize = 10.sp,
                            color = ConnectGrayMedium
                        )
                    }
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Toggle map view" else "Toggle map view",
                        tint = ConnectGrayMedium
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE2EFE2),
                                        Color(0xFFF4F8F4)
                                    )
                                )
                            )
                            .border(1.dp, ConnectGrayLight.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    ) {
                        val width = constraints.maxWidth
                        val height = constraints.maxHeight
                        
                        val widthDp = with(androidx.compose.ui.platform.LocalDensity.current) { width.toDp() }
                        val heightDp = with(androidx.compose.ui.platform.LocalDensity.current) { height.toDp() }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // 1. Draw Northern Mountain peaks (Shivapuri Hills) behind Budhanilkantha
                            val mtColor = Color(0xFFD0DFD0)
                            
                            val peak1Path = Path().apply {
                                moveTo(0f, size.height * 0.12f)
                                lineTo(size.width * 0.2f, size.height * 0.02f)
                                lineTo(size.width * 0.45f, size.height * 0.15f)
                                lineTo(0f, size.height * 0.15f)
                                close()
                            }
                            drawPath(peak1Path, color = mtColor)
                            
                            val peak2Path = Path().apply {
                                moveTo(size.width * 0.35f, size.height * 0.15f)
                                lineTo(size.width * 0.55f, size.height * 0.01f)
                                lineTo(size.width * 0.8f, size.height * 0.18f)
                                lineTo(size.width * 0.35f, size.height * 0.18f)
                                close()
                            }
                            drawPath(peak2Path, color = mtColor)
                            
                            val peak3Path = Path().apply {
                                moveTo(size.width * 0.70f, size.height * 0.18f)
                                lineTo(size.width * 0.88f, size.height * 0.05f)
                                lineTo(size.width, size.height * 0.14f)
                                lineTo(size.width * 0.70f, size.height * 0.14f)
                                close()
                            }
                            drawPath(peak3Path, color = mtColor)
                            
                            val snowcapPath = Path().apply {
                                moveTo(size.width * 0.55f, size.height * 0.01f)
                                lineTo(size.width * 0.51f, size.height * 0.05f)
                                lineTo(size.width * 0.55f, size.height * 0.07f)
                                lineTo(size.width * 0.59f, size.height * 0.05f)
                                close()
                            }
                            drawPath(snowcapPath, color = Color.White)
                            
                            // 2. Draw Bagmati River (Flowing between central/north Kathmandu and Patan in south)
                            val riverColor = Color(0xFFA5C3E8)
                            val riverPath = Path().apply {
                                moveTo(size.width * 0.9f, size.height * 0.55f)
                                cubicTo(
                                    size.width * 0.7f, size.height * 0.58f,
                                    size.width * 0.52f, size.height * 0.52f,
                                    size.width * 0.48f, size.height * 0.62f
                                )
                                cubicTo(
                                    size.width * 0.45f, size.height * 0.70f,
                                    size.width * 0.28f, size.height * 0.72f,
                                    size.width * 0.1f, size.height * 0.85f
                                )
                            }
                            drawPath(
                                path = riverPath,
                                color = riverColor,
                                style = Stroke(width = 6f, join = StrokeJoin.Round)
                            )
                            
                            // 3. Draw Ring Road (Dashed stylized circle containing the urban core)
                            val ringColor = ConnectGrayMedium.copy(alpha = 0.20f)
                            drawCircle(
                                color = ringColor,
                                radius = size.width * 0.38f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.46f, size.height * 0.52f),
                                style = Stroke(
                                    width = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                )
                            )
                            
                            // 4. Draw abstract primary grid roads
                            val roadColor = ConnectGrayLight.copy(alpha = 0.7f)
                            drawLine(
                                color = roadColor,
                                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.52f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.50f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = roadColor,
                                start = androidx.compose.ui.geometry.Offset(size.width * 0.48f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width * 0.46f, size.height),
                                strokeWidth = 3f
                            )
                        }
                        
                        val pinConfigs = listOf(
                            Triple("Thamel", "🍹", Pair(0.32f, 0.48f)),
                            Triple("Baluwatar", "⚽", Pair(0.50f, 0.40f)),
                            Triple("Patan", "🏛️", Pair(0.38f, 0.74f)),
                            Triple("Boudha", "☸️", Pair(0.75f, 0.45f)),
                            Triple("Budhanilkantha", "🏔️", Pair(0.54f, 0.22f))
                        )
                        
                        pinConfigs.forEach { (id, emoji, coords) ->
                            val isSelected = id == selectedNeighborhood
                            val count = plans.count { plan ->
                                val loc = plan.location.lowercase()
                                val desc = plan.description.lowercase()
                                val title = plan.title.lowercase()
                                when (id) {
                                    "Thamel" -> loc.contains("thamel") || desc.contains("thamel") || title.contains("thamel")
                                    "Patan" -> loc.contains("patan") || loc.contains("jhamsikhel") || loc.contains("lalitpur") ||
                                               desc.contains("patan") || desc.contains("jhamsikhel") || desc.contains("lalitpur") ||
                                               title.contains("patan") || title.contains("jhamsikhel") || title.contains("lalitpur")
                                    "Boudha" -> loc.contains("boudha") || desc.contains("boudha") || title.contains("boudha")
                                    "Baluwatar" -> loc.contains("baluwatar") || desc.contains("baluwatar") || title.contains("baluwatar")
                                    "Budhanilkantha" -> loc.contains("budhanilkantha") || loc.contains("shivapuri") ||
                                                        desc.contains("budhanilkantha") || desc.contains("shivapuri") ||
                                                        title.contains("budhanilkantha") || title.contains("shivapuri")
                                    else -> false
                                }
                            }
                            
                            val pinWidth = 46.dp
                            val pinHeight = 50.dp
                            
                            val leftOffset = widthDp * coords.first - (pinWidth / 2f)
                            val topOffset = heightDp * coords.second - (pinHeight / 1.1f)
                            
                            Box(
                                modifier = Modifier
                                    .size(width = pinWidth, height = pinHeight)
                                    .offset(x = leftOffset, y = topOffset)
                                    .testTag("map_pin_$id")
                            ) {
                                if (isSelected || count > 0) {
                                    val pulseColor = if (isSelected) ConnectDarkGreen.copy(alpha = 0.25f) else ConnectMint.copy(alpha = 0.4f)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .align(Alignment.BottomCenter)
                                            .offset(y = (-6).dp)
                                            .clip(CircleShape)
                                            .background(pulseColor)
                                    )
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onSelected(id) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ConnectDarkGreen else ConnectWhite)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) ConnectGold else ConnectDarkGreen,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 15.sp
                                        )
                                        
                                        if (count > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 4.dp, y = (-4).dp)
                                                    .clip(CircleShape)
                                                    .background(ConnectGold)
                                                    .border(0.5.dp, ConnectWhite, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = count.toString(),
                                                    color = ConnectGrayDark,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = ConnectDarkGreen,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .offset(y = (-5).dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val mapInfoList = listOf(
                        Triple("Thamel", "Thamel", "Vibrant nightlife, cafes & travel base"),
                        Triple("Baluwatar", "Baluwatar", "Premium central area & sports centers"),
                        Triple("Patan", "Patan & Jhamsikhel", "Heritage stupas, crafts & organic cafes"),
                        Triple("Boudha", "Boudha", "Peaceful vibes around sacred stupa"),
                        Triple("Budhanilkantha", "Budhanilkantha", "Foothills hiking & serene nature")
                    )
                    
                    val activeMetadata = mapInfoList.find { it.first == selectedNeighborhood }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(ConnectCream, RoundedCornerShape(10.dp))
                            .border(1.dp, ConnectGrayLight, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info detail",
                            tint = ConnectDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (activeMetadata != null) {
                                val c = plans.count { plan ->
                                    val loc = plan.location.lowercase()
                                    val desc = plan.description.lowercase()
                                    val title = plan.title.lowercase()
                                    when (activeMetadata.first) {
                                        "Thamel" -> loc.contains("thamel") || desc.contains("thamel") || title.contains("thamel")
                                        "Patan" -> loc.contains("patan") || loc.contains("jhamsikhel") || loc.contains("lalitpur") ||
                                                   desc.contains("patan") || desc.contains("jhamsikhel") || desc.contains("lalitpur") ||
                                                   title.contains("patan") || title.contains("jhamsikhel") || title.contains("lalitpur")
                                        "Boudha" -> loc.contains("boudha") || desc.contains("boudha") || title.contains("boudha")
                                        "Baluwatar" -> loc.contains("baluwatar") || desc.contains("baluwatar") || title.contains("baluwatar")
                                        "Budhanilkantha" -> loc.contains("budhanilkantha") || loc.contains("shivapuri") ||
                                                            desc.contains("budhanilkantha") || desc.contains("shivapuri") ||
                                                            title.contains("budhanilkantha") || title.contains("shivapuri")
                                        else -> false
                                    }
                                }
                                Text(
                                    text = "Focused: ${activeMetadata.second}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = "${activeMetadata.third} • $c active plans here.",
                                    fontSize = 10.sp,
                                    color = ConnectGrayMedium
                                )
                            } else {
                                Text(
                                    text = "📍 Showing All Kathmandu Areas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = "Tap any pins on the map above to zoom-filter activities in that neighborhood.",
                                    fontSize = 10.sp,
                                    color = ConnectGrayMedium
                                )
                            }
                        }
                        if (selectedNeighborhood != "All") {
                            TextButton(
                                onClick = { onSelected("All") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.heightIn(min = 28.dp)
                            ) {
                                Text(
                                    text = "Clear Filter",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectDarkGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeighborhoodRow(activeNeighborhood: String, onSelected: (String) -> Unit) {
    val neighborhoods = listOf(
        "All" to "📍 All Areas",
        "Thamel" to "🍹 Thamel",
        "Patan" to "🏛️ Patan & Jhamsikhel",
        "Boudha" to "☸️ Boudha",
        "Baluwatar" to "⚽ Baluwatar",
        "Budhanilkantha" to "🏔️ Budhanilkantha"
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("neighborhood_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(neighborhoods) { (id, label) ->
            val isSelected = id == activeNeighborhood
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(id) },
                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ConnectDarkGreen,
                    selectedLabelColor = Color.White,
                    containerColor = ConnectCream,
                    labelColor = ConnectGrayDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = Color.Transparent,
                    borderColor = ConnectGrayLight,
                    selectedBorderWidth = 1.dp,
                    enabled = true,
                    selected = isSelected
                ),
                modifier = Modifier.testTag("neighborhood_chip_$id")
            )
        }
    }
}

@Composable
fun CategoryRow(activeCategory: String, onSelected: (String) -> Unit) {
    val categories = listOf("All", "Play", "Explore", "Meet", "Learn", "Experience")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == activeCategory
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(cat) },
                label = { Text(cat, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ConnectDarkGreen,
                    selectedLabelColor = Color.White,
                    containerColor = ConnectCream,
                    labelColor = ConnectGrayDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = Color.Transparent,
                    borderColor = ConnectGrayLight,
                    selectedBorderWidth = 1.dp,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun AvailabilityPill(av: Availability) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (av.isCurrentUser) ConnectMint else ConnectCream
        ),
        modifier = Modifier
            .width(180.dp)
            .border(
                1.dp,
                if (av.isCurrentUser) ConnectDarkGreen else ConnectGrayLight,
                RoundedCornerShape(20.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Static category visual circle
                    val categoryColor = when (av.iconType) {
                        "Coffee" -> Color(0xFF8B5A2B)
                        "Sports" -> Color(0xFF2E7D32)
                        "Hiking" -> Color(0xFFEF6C00)
                        "Travel" -> Color(0xFF1565C0)
                        "Networking" -> Color(0xFF6A1B9A)
                        else -> ConnectLightGreen
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = av.userName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(90.dp)
                    )
                }

                Text(
                    text = av.timeAgo,
                    fontSize = 9.sp,
                    color = ConnectGrayMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = av.statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ConnectGrayDark,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GroupCard(gp: Group, onJoinToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .height(130.dp)
            .border(1.dp, ConnectGrayLight, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gp.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectDarkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ConnectMint)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gp.category,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectDarkGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = gp.description,
                    fontSize = 10.sp,
                    color = ConnectGrayMedium,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${gp.membersCount} active locals",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConnectGrayDark
                )

                Button(
                    onClick = { onJoinToggle() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gp.isMember) ConnectGrayLight else ConnectDarkGreen,
                        contentColor = if (gp.isMember) ConnectGrayDark else ConnectWhite
                    ),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (gp.isMember) "Joined" else "Connect",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: Plan,
    onJoinToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, ConnectGrayLight, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Plan Title Bar & Favorite trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Category pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ConnectMint)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = ConnectDarkGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Organizer Name + Rating info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "by ${plan.organizerName} (★ ${plan.organizerRating})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ConnectGrayMedium
                            )
                            if (plan.isVerifiedOrganizer) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE6F4EA))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified Organizer Badge",
                                            tint = Color(0xFF137333),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Verified",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF137333)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = plan.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark,
                        lineHeight = 20.sp
                    )
                }

                IconButton(
                    onClick = { onSaveToggle() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (plan.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save activity icon",
                        tint = if (plan.isSaved) ConnectError else ConnectGrayMedium,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Parameters Table
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConnectCream, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin icon",
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = plan.location,
                        fontSize = 12.sp,
                        color = ConnectGrayDark,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Clock calendar icon",
                            tint = ConnectDarkGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${plan.date} · ${plan.time}",
                            fontSize = 12.sp,
                            color = ConnectGrayDark,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = plan.pricePerPerson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description summary
            Text(
                text = plan.description,
                fontSize = 12.sp,
                color = ConnectGrayMedium,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Join state metrics & interactive Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slots left indicators
                Column {
                    Text(
                        text = "${plan.joinedCount} people joining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectDarkGreen
                    )
                    Text(
                        text = "Needs ${plan.participantsNeeded} more to happen",
                        fontSize = 10.sp,
                        color = ConnectGrayMedium
                    )
                }

                // Interactive Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Chat with host button
                    OutlinedButton(
                        onClick = { onChatClick() },
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Chat icon",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Main Join decision trigger
                    Button(
                        onClick = { onJoinToggle() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (plan.isJoinedByMe) ConnectMidGreen else ConnectDarkGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (plan.isJoinedByMe) "✓ Joined" else "Join Plan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}


// ======================== CREATE PLAN SCREEN ========================
@Composable
fun CreatePlanScreen(viewModel: ConnectViewModel, onPlanPublished: () -> Unit) {
    val title by viewModel.formTitle.collectAsStateWithLifecycle()
    val category by viewModel.formCategory.collectAsStateWithLifecycle()
    val location by viewModel.formLocation.collectAsStateWithLifecycle()
    val date by viewModel.formDate.collectAsStateWithLifecycle()
    val time by viewModel.formTime.collectAsStateWithLifecycle()
    val price by viewModel.formPrice.collectAsStateWithLifecycle()
    val participants by viewModel.formParticipantsNeeded.collectAsStateWithLifecycle()
    val description by viewModel.formDescription.collectAsStateWithLifecycle()

    var validationError by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Banner header
        item {
            Column {
                Text(
                    text = "Post Your Plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ConnectDarkGreen
                )
                Text(
                    text = "Tell Kathmandu locals what you want to do. Turn thoughts into live plans!",
                    fontSize = 12.sp,
                    color = ConnectGrayMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = ConnectGrayLight)
            }
        }

        // Title input
        item {
            Column {
                Text(
                    text = "Plan Title *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectGrayDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.formTitle.value = it },
                    placeholder = { Text("e.g. Saturday morning Futsal, Cafe study hour") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = ConnectDarkGreen,
                        unfocusedIndicatorColor = ConnectGrayLight,
                        focusedContainerColor = ConnectCream,
                        unfocusedContainerColor = ConnectCream
                    )
                )
            }
        }

        // Category dropdown styled chips
        item {
            Column {
                Text(
                    text = "Category *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectGrayDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val options = listOf("Play", "Explore", "Meet", "Learn", "Experience")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { opt ->
                        val isSelected = opt == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ConnectDarkGreen else ConnectCream)
                                .clickable { viewModel.formCategory.value = opt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ConnectGrayDark
                            )
                        }
                    }
                }
            }
        }

        // Location input
        item {
            Column {
                Text(
                    text = "Meeting Location *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectGrayDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { viewModel.formLocation.value = it },
                    placeholder = { Text("e.g. Jhamsikhel Coffee bar, Dhuku Futsal") },
                    modifier = Modifier.fillMaxWidth().testTag("form_location_input"),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = ConnectDarkGreen,
                        unfocusedIndicatorColor = ConnectGrayLight,
                        focusedContainerColor = ConnectCream,
                        unfocusedContainerColor = ConnectCream
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Suggest:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayMedium
                    )
                    val neighborhoodSuggestions = listOf("Thamel", "Patan", "Jhamsikhel", "Boudha", "Baluwatar", "Budhanilkantha")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(neighborhoodSuggestions) { sug ->
                            val isAlreadyIn = location.contains(sug, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAlreadyIn) ConnectMint else ConnectCream)
                                    .border(1.dp, if (isAlreadyIn) ConnectDarkGreen else ConnectGrayLight, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (!isAlreadyIn) {
                                            val newLocation = if (location.isBlank()) sug else "$location, $sug"
                                            viewModel.formLocation.value = newLocation
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("suggest_location_$sug")
                            ) {
                                Text(
                                    text = sug,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAlreadyIn) ConnectDarkGreen else ConnectGrayDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Parallel fields: Date & Time
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { viewModel.formDate.value = it },
                        placeholder = { Text("e.g. Saturday, June 6") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = ConnectDarkGreen,
                            unfocusedIndicatorColor = ConnectGrayLight,
                            focusedContainerColor = ConnectCream,
                            unfocusedContainerColor = ConnectCream
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { viewModel.formTime.value = it },
                        placeholder = { Text("e.g. 7:30 PM") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = ConnectDarkGreen,
                            unfocusedIndicatorColor = ConnectGrayLight,
                            focusedContainerColor = ConnectCream,
                            unfocusedContainerColor = ConnectCream
                        )
                    )
                }
            }
        }

        // Parallel fields: Price per Head & slots count
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Price per person",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { viewModel.formPrice.value = it },
                        placeholder = { Text("e.g. Rs. 300 / Free") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = ConnectDarkGreen,
                            unfocusedIndicatorColor = ConnectGrayLight,
                            focusedContainerColor = ConnectCream,
                            unfocusedContainerColor = ConnectCream
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "People needed *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectGrayDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = participants,
                        onValueChange = { viewModel.formParticipantsNeeded.value = it },
                        placeholder = { Text("e.g. 4") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = ConnectDarkGreen,
                            unfocusedIndicatorColor = ConnectGrayLight,
                            focusedContainerColor = ConnectCream,
                            unfocusedContainerColor = ConnectCream
                        )
                    )
                }
            }
        }

        // Description
        item {
            Column {
                Text(
                    text = "Plan Description & Rules *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConnectGrayDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.formDescription.value = it },
                    placeholder = { Text("Describe what we will do, where we will assemble, and expected costs or preparations of players/partners.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = ConnectDarkGreen,
                        unfocusedIndicatorColor = ConnectGrayLight,
                        focusedContainerColor = ConnectCream,
                        unfocusedContainerColor = ConnectCream
                    )
                )
            }
        }

        // Verification validation banner
        if (validationError.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConnectError.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError,
                        color = ConnectError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Publish Button action
        item {
            Button(
                onClick = {
                    if (title.isBlank() || location.isBlank() || date.isBlank() || description.isBlank()) {
                        validationError = "Please fill in all starred (*) fields to publish your activity."
                    } else {
                        validationError = ""
                        viewModel.publishActivityPlan(onSuccess = {
                            onPlanPublished()
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Publish check icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publish Activity Plan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}


// ======================== MY PROFILE SCREEN ========================
@Composable
fun ProfileScreen(viewModel: ConnectViewModel, onEditProfile: () -> Unit) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()

    val myPublishedPlans = plans.filter { it.organizerName == (profile?.name ?: "Ayush") }
    val myJoinedPlans = plans.filter { it.isJoinedByMe }
    val mySavedPlans = plans.filter { it.isSaved }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Joined, 1 = My Hosted, 2 = Saved

    // Interactive Verification Wizard States
    var showWizard by remember { mutableStateOf(false) }
    var wizardStep by remember { mutableStateOf(1) } // 1=Phone Check, 2=SMS OTP, 3=Document selection, 4=Scanner sim, 5=Badge claim
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var idName by remember { mutableStateOf(profile?.name ?: "Ayush") }
    var docType by remember { mutableStateOf("Citizenship Certificate") }
    var docNumber by remember { mutableStateOf("") }
    var scanPhase by remember { mutableStateOf(0) } // 0=idle, 1=scanning, 2=done
    var laserPosition by remember { mutableStateOf(0f) }
    var feedbackMessage by remember { mutableStateOf("Positioning your document...") }
    var isTimerActive by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(59) }

    LaunchedEffect(profile) {
        profile?.let {
            if (idName == "Ayush" || idName.isBlank()) {
                idName = it.name
            }
        }
    }

    LaunchedEffect(isTimerActive, timeRemaining) {
        if (isTimerActive && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining -= 1
        } else if (timeRemaining == 0) {
            isTimerActive = false
        }
    }

    LaunchedEffect(scanPhase) {
        if (scanPhase == 1) {
            feedbackMessage = "Positioning document limits..."
            kotlinx.coroutines.delay(700)
            feedbackMessage = "Aligning camera viewfinder boundaries..."
            kotlinx.coroutines.delay(800)
            feedbackMessage = "Reading security security watermarks..."
            
            // Simulating laser scan line moving down
            for (pos in 0..15) {
                laserPosition = pos * 11f
                kotlinx.coroutines.delay(90)
            }
            
            feedbackMessage = "Biometric facial features comparison..."
            kotlinx.coroutines.delay(1200)
            feedbackMessage = "Securing governmental verification databases... Approved!"
            kotlinx.coroutines.delay(700)
            scanPhase = 2
            wizardStep = 5
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
    ) {
        // Visual Profile Card Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConnectMintLight)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Profile Icon Avatar Wrapper
                    Box(
                        modifier = Modifier.size(78.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(ConnectDarkGreen)
                                .border(3.dp, if (profile?.isVerified == true) Color(0xFF48BB78) else ConnectMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar picture icon",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        if (profile?.isVerified == true) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF48BB78), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified badge check",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.name ?: "Ayush",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark
                        )
                        if (profile?.isVerified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Local check",
                                tint = Color(0xFF48BB78),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Score stars",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile?.rating ?: 4.8} Stars Local Companion Rating",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = profile?.bio ?: "Exploring local trails and networking with sports players around Lalitpur.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = ConnectGrayMedium,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    val userInterests = (profile?.interests ?: "Hiking 🏔️, Food Walk 🥟, Futsal ⚽, Live Music 🎸")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (userInterests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "MY INTERESTS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayMedium,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("profile_interests_row")
                        ) {
                            items(userInterests) { interest ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(ConnectCream)
                                        .border(1.dp, ConnectDarkGreen.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = interest,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConnectDarkGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Configuration command
                    OutlinedButton(
                        onClick = { onEditProfile() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Config Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Trust & Verification Center Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = if (profile?.isVerified == true) Color(0xFFC6F6D5) else ConnectGrayLight,
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (profile?.isVerified == true) Color(0xFFF0FFF4) else ConnectCream
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (profile?.isVerified == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Trust emblem",
                                tint = if (profile?.isVerified == true) Color(0xFF38A169) else Color(0xFFDD6B20),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trust & Verification Center",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConnectGrayDark
                            )
                        }

                        if (profile?.isVerified == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xFF38A169))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (profile?.isVerified == true) {
                        // VERIFIED STATE: Show secure metadata information
                        Text(
                            text = "You are a Verified Kathmandu Companion! Other members planning real-life activities will see the green verified badge on your profile and hosted plans.",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SECURED DATA & TRUST METADATA:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone Verified", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Phone: ${profile?.verifiedPhone}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ConnectGrayDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "ID Match Approved", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ID Match Check: ${profile?.verifiedNationalIdName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ConnectGrayDark
                                    )
                                }
                                if (!profile?.verificationMethod.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = "Secured via", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Method: ${profile?.verificationMethod}",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = { viewModel.revokeUserVerification() },
                                colors = ButtonDefaults.textButtonColors(contentColor = ConnectError)
                            ) {
                                Text("Revoke Check", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (!showWizard) {
                        // UN-VERIFIED & NOT YET IN WIZARD: Show description with perks and single start Button
                        Text(
                            text = "Verify your account and phone number to earn a security badge and establish high coordination confidence.",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Trust advantage info",
                                tint = ConnectDarkGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Why verify?",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = "Verified hosts receive 4.5x higher attendee trust. You also unlock real-time peer availability sharing.",
                                    fontSize = 9.sp,
                                    color = ConnectGrayMedium,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                showWizard = true
                                wizardStep = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier.testTag("upgrade_trust_button").align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Upgrade badge unlock", modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upgrade Trust Level", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // WIZARD IS ACTIVE: Show step-by-step progress and gorgeous interface
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ConnectGrayLight.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Progress Dots or steps summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Step $wizardStep of 4",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConnectDarkGreen
                                    )
                                    // Simulated simple step indicator dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (i in 1..4) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (wizardStep >= i) ConnectDarkGreen else ConnectGrayLight
                                                    )
                                            )
                                        }
                                    }
                                }
                                
                                LinearProgressIndicator(
                                    progress = {
                                        when (wizardStep) {
                                            1 -> 0.25f
                                            2 -> 0.50f
                                            3 -> 0.75f
                                            4 -> 0.90f
                                            else -> 1.0f
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .height(4.dp),
                                    color = ConnectDarkGreen,
                                    trackColor = ConnectGrayLight
                                )

                                when (wizardStep) {
                                    1 -> {
                                        // STEP 1: Phone submission
                                        Text(
                                            text = "Verify Phone Contact",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "Provide your phone number to authorize secure community peer coordination via SMS warnings.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { phoneNumber = it },
                                            label = { Text("Kathmandu Companion Mobile", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. +977 9812345678", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { showWizard = false }
                                            ) {
                                                Text("Exit Setup", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (phoneNumber.isNotBlank()) {
                                                        wizardStep = 2
                                                        isTimerActive = true
                                                        timeRemaining = 59
                                                    }
                                                },
                                                enabled = phoneNumber.trim().isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Send SMS Verification Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    2 -> {
                                        // STEP 2: Simulated OTP SMS Input
                                        Text(
                                            text = "Enter 6-Digit SMS Code Check",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "We sent a confirmation text to $phoneNumber. Resending allowed after timer.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        // Gorgeous Notification Toast message simulating active SMS receipt
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = ConnectMintLight),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ConnectMint)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "SMS banner",
                                                    tint = ConnectDarkGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "📱 SIMULATED SMS RECEIVED",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Text(
                                                        text = "Your safe Connect OTP Code is 552489",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectGrayDark
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "AUTOFILL",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ConnectDarkGreen,
                                                    modifier = Modifier
                                                        .background(ConnectWhite, RoundedCornerShape(4.dp))
                                                        .clickable { otpCode = "552489" }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = otpCode,
                                            onValueChange = { if (it.length <= 6) otpCode = it },
                                            label = { Text("6-Digit OTP Verification Security Code", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. 552489", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    wizardStep = 1
                                                    isTimerActive = false
                                                }
                                            ) {
                                                Text("Change Phone", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (otpCode == "552489" || otpCode.length == 6) {
                                                        wizardStep = 3
                                                    }
                                                },
                                                enabled = otpCode.trim().length >= 6,
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Verify Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Text(
                                            text = if (isTimerActive) "Resend available in ${timeRemaining}s" else "Didn't receive it? Click Resend",
                                            fontSize = 9.sp,
                                            color = ConnectGrayMedium,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(top = 4.dp)
                                                .clickable(enabled = !isTimerActive) {
                                                    isTimerActive = true
                                                    timeRemaining = 59
                                                }
                                        )
                                    }
                                    3 -> {
                                        // STEP 3: ID selection & Name parameters
                                        Text(
                                            text = "Identification Details",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "Government files are scanned securely to check names of travelers or local residents on Kathmandu lists.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        OutlinedTextField(
                                            value = idName,
                                            onValueChange = { idName = it },
                                            label = { Text("Government ID Full Legal Name", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Text(
                                            text = "Select Document Type Support:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        
                                        // Beautiful Row of Document Options
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val docs = listOf(
                                                "Citizenship Certificate" to "📜 Civil ID",
                                                "Passport" to "✈️ Passport",
                                                "Driving License" to "🚗 License"
                                            )
                                            docs.forEach { (type, label) ->
                                                val isSel = docType == type
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { docType = type }
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) ConnectDarkGreen else ConnectGrayLight,
                                                            shape = RoundedCornerShape(10.dp)
                                                        ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSel) ConnectMintLight else ConnectWhite
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) ConnectDarkGreen else ConnectGrayMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        OutlinedTextField(
                                            value = docNumber,
                                            onValueChange = { docNumber = it },
                                            label = { Text("$docType Reference Number", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. 52-01-78-01124", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { wizardStep = 2 }) {
                                                Text("Back to SMS", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (idName.isNotBlank() && docNumber.isNotBlank()) {
                                                        wizardStep = 4
                                                        scanPhase = 1
                                                    }
                                                },
                                                enabled = idName.trim().isNotBlank() && docNumber.trim().isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Start Identity Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    4 -> {
                                        // STEP 4: Live scanner simulator screen
                                        Text(
                                            text = "Simulating Document Secure Check",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Viewfinder card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ConnectGrayDark)
                                                .border(2.dp, ConnectMint, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Mock Document Visual Card inside viewfinder
                                            Card(
                                                modifier = Modifier
                                                    .width(260.dp)
                                                    .height(150.dp)
                                                    .border(1.dp, ConnectWhite.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = ConnectCream)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "NEPAL SYSTEM GOVERNMENT ID",
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ConnectGrayDark
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(Color.Red, CircleShape)
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = "Official Document: $docType",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Name: $idName",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = ConnectGrayDark
                                                    )
                                                    Text(
                                                        text = "Ref Code: $docNumber",
                                                        fontSize = 9.sp,
                                                        color = ConnectGrayMedium
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "🔒 BIOMETRIC SECURE SCAN ACTIVE",
                                                        fontSize = 7.sp,
                                                        color = ConnectGrayMedium
                                                    )
                                                }
                                            }

                                            // Scanning Beam horizontal laser Line sliding down
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = laserPosition.dp)
                                                    .background(Color(0xFF48BB78))
                                                    .shadow(elevation = 6.dp, ambientColor = Color(0xFF48BB78), spotColor = Color(0xFF48BB78))
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Verification status feedback message and circular progress
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = ConnectDarkGreen,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = feedbackMessage,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ConnectGrayDark
                                            )
                                        }
                                    }
                                    5 -> {
                                        // STEP 5: Congratulations & Perks Claim screen
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE6F4EA)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Shield unlocked badge",
                                                    tint = Color(0xFF137333),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Text(
                                                text = "Account Checked Successfully!",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ConnectGrayDark
                                            )
                                            
                                            Text(
                                                text = "Government databases approved the ID card scan check matching for $idName using $docType.",
                                                fontSize = 10.sp,
                                                color = ConnectGrayMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                lineHeight = 13.sp
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Badges perks details display
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = ConnectMintLight),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "🔒 VERIFIED ACCOUNT BENEFITS UNLOCKED:",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    
                                                    listOf(
                                                        "🛡️ Beautiful Green Verification Checkmark next to your name",
                                                        "🚀 Verified Host status flag auto-embedded on all hosted plans",
                                                        "🔥 High peer-coordination confidence to maximize actual real gathering attendees"
                                                    ).forEach { perk ->
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(text = "•", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = perk,
                                                                fontSize = 9.sp,
                                                                color = ConnectGrayDark,
                                                                lineHeight = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.submitUserPhoneVerification(
                                                        phoneNumber = phoneNumber,
                                                        nationalIdName = "$idName ($docType Verified)",
                                                        method = docType,
                                                        onSuccess = {
                                                            showWizard = false
                                                            wizardStep = 1
                                                            scanPhase = 0
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(30.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Claim Green Verified Shield Badge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sub tab options: Joined, Hosted, and Saved plans
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = ConnectDarkGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Joined (${myJoinedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Hosted (${myPublishedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Saved (${mySavedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // List views
        val currentList = when (selectedTab) {
            0 -> myJoinedPlans
            1 -> myPublishedPlans
            else -> mySavedPlans
        }
        if (currentList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "You haven't joined any activities yet. Go to Home to explore and join Kathmandu plans!"
                            1 -> "You haven't posted or hosted any plans yet."
                            else -> "No saved activities. Favorite general plans to bookmark them here."
                        },
                        fontSize = 12.sp,
                        color = ConnectGrayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(currentList, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    onJoinToggle = { viewModel.toggleJoinPlan(plan.id) },
                    onSaveToggle = { viewModel.toggleSavePlan(plan.id) },
                    onChatClick = { viewModel.navigateTo(Screen.ChatDetail(plan.id)) }
                )
            }
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

    var hostPlan by remember { mutableStateOf<Plan?>(null) }
    var userMessage by remember { mutableStateOf("") }

    // Fetch parent Plan to display host contact metadata
    LaunchedEffect(planId) {
        hostPlan = viewModel.getPlanById(planId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
    ) {
        // Chat Header with host name
        Surface(
            color = ConnectCream,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
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

                Column {
                    Text(
                        text = "Plan Host: ${hostPlan?.organizerName ?: "Lisa"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark
                    )
                    Text(
                        text = hostPlan?.title ?: "Activity group discussion",
                        fontSize = 11.sp,
                        color = ConnectGrayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }

        // Message Feed List block
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        Text(
                            text = chat.senderName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
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
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            text = chat.messageText,
                            fontSize = 12.sp,
                            color = textColor,
                            lineHeight = 16.sp
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
                    onValueChange = { userMessage = it },
                    placeholder = { Text("Ask about dates, equipment, or details...") },
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

                FloatingActionButton(
                    onClick = {
                        if (userMessage.isNotBlank()) {
                            viewModel.sendChatMessage(planId, userMessage)
                            userMessage = ""
                        }
                    },
                    containerColor = ConnectDarkGreen,
                    contentColor = Color.White,
                    modifier = Modifier.size(46.dp),
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
    onSave: (String, String, Boolean, String) -> Unit
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
            Button(
                onClick = { 
                    val finalInterests = selectedInterests.joinToString(", ")
                    onSave(nameInput, bioInput, isTravellerOption, finalInterests) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                modifier = Modifier.testTag("apply_profile_button")
            ) {
                Text("Apply Parameters")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
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
