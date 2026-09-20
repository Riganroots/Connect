package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PostAvailabilityDialog
import com.example.data.models.Availability
import com.example.data.models.Plan
import com.example.ui.components.GroupCard
import com.example.ui.components.PlanCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.kathmanduDiscoverSpots

@Composable
fun HomeScreen(viewModel: ConnectViewModel) {
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val availabilities by viewModel.allAvailabilities.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeNeighborhood by viewModel.selectedNeighborhood.collectAsStateWithLifecycle()
    val filteredSpots by viewModel.discoverSpots.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val cloudActivityError by viewModel.cloudActivityError.collectAsStateWithLifecycle()
    val cloudCommunityError by viewModel.cloudCommunityError.collectAsStateWithLifecycle()

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
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ConnectDarkGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hi, ${profile?.name?.substringBefore(" ") ?: "there"} 👋",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectMint.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "What do you want to do today?",
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Discover a place, join a local plan, or create your own.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = ConnectMint.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.navigateTo(Screen.CreatePlan) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ConnectMint,
                            contentColor = ConnectDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (cloudActivityError != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFCC80))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Cloud activities are temporarily unavailable",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConnectGrayDark
                            )
                            Text(
                                text = cloudActivityError ?: "Check your connection and try again.",
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                color = ConnectGrayMedium
                            )
                        }
                    }
                }
            }
        }

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
                    placeholder = { Text("Search plans, places or activities") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = ConnectGrayMedium
                                )
                            }
                        }
                    },
                    singleLine = true,
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
                            Text("Explore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Text("Community", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    TextButton(
                        onClick = {
                            if (hasMyAvailability) {
                                viewModel.removeUserAvailableNow()
                            } else {
                                showAvailabilityPost = true
                            }
                        }
                    ) {
                        Text(
                            text = if (hasMyAvailability) "Go Offline" else "+ Go Live",
                            color = if (hasMyAvailability) ConnectError else ConnectDarkGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (cloudCommunityError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                    text = "Community sync temporarily unavailable",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = cloudCommunityError ?: "Please try again.",
                                    fontSize = 9.sp,
                                    color = ConnectGrayMedium,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }

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
                    text = "Communities & Groups",
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
                                text = "No activities found yet.",
                                color = ConnectGrayDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Create the first activity or change your search and filters.",
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
                            text = "Kathmandu Area Filter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "Tap an area marker to filter nearby activities",
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
                                    text = "Area: ${activeMetadata.second}",
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
                                    text = "📍 Showing all Kathmandu areas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = "Tap an area marker above to filter activities in that neighborhood.",
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

