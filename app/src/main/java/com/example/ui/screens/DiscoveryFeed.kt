package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.components.ActivityPhotography
import com.example.ui.components.categoryTitle
import com.example.ui.components.GroupCard
import com.example.ui.components.PlanCard
import com.example.ui.theme.*

/** Stateless feed shared by the live screen and native layout/interaction checks. */
@Composable
fun DiscoveryFeed(
    plans: List<Plan>, spots: List<DiscoverSpot>, groups: List<Group>,
    availabilities: List<Availability>, query: String, category: String, area: String,
    showPlaces: Boolean, activityError: String?, communityError: String?,
    onQuery: (String) -> Unit, onCategory: (String) -> Unit, onArea: (String) -> Unit,
    onTab: (Boolean) -> Unit, onCreate: () -> Unit, onJoin: (Plan) -> Unit,
    onSave: (Plan) -> Unit, onChat: (Plan) -> Unit, onSpot: (DiscoverSpot) -> Unit,
    onGroup: (Group) -> Unit, onAvailability: () -> Unit, onSurprise: () -> Unit
) {
    var showAreas by remember { mutableStateOf(false) }
    var selectedPlanId by remember { mutableStateOf<Long?>(null) }
    plans.firstOrNull { it.id == selectedPlanId }?.let { plan ->
        PlanDetailDialog(plan, onDismiss = { selectedPlanId = null },
            onSave = { onSave(plan) }, onJoin = { onJoin(plan) },
            onChat = { selectedPlanId = null; onChat(plan) })
    }
    if (showAreas) {
        AlertDialog(
            onDismissRequest = { showAreas = false },
            title = { Text("Where do you want to meet?") },
            text = {
                Column {
                    listOf("All", "Thamel", "Patan", "Boudha", "Baluwatar", "Budhanilkantha").forEach { option ->
                        TextButton(
                            onClick = { onArea(option); showAreas = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(if (option == "All") "All of Kathmandu" else option, modifier = Modifier.weight(1f))
                            if (option == area) Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAreas = false }) { Text("Done") } }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ConnectMintLight).testTag("discovery_feed"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                placeholder = { Text(if (showPlaces) "Search places" else "Search activities", fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("discovery_search"),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = ConnectWhite, focusedContainerColor = ConnectWhite,
                    focusedTextColor = ConnectGrayDark, unfocusedTextColor = ConnectGrayDark,
                    focusedPlaceholderColor = ConnectGrayMedium, unfocusedPlaceholderColor = ConnectGrayMedium,
                    unfocusedBorderColor = ConnectGrayLight, focusedBorderColor = ConnectDarkGreen
                )
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                FeedTab("Activities", !showPlaces, { onTab(false) }, Modifier.weight(1f))
                FeedTab("Places", showPlaces, { onTab(true) }, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { showAreas = true }, shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("area_filter"),
                    border = BorderStroke(1.dp, ConnectGrayLight)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Area", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        if (!showPlaces && (category != "All" || query.isNotBlank())) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Play", "Explore", "Meet", "Learn", "Experience")) { option ->
                        FilterChip(
                            selected = option == category, onClick = { onCategory(option) },
                            label = { Text(if (option == "All") "All plans" else option, fontSize = 14.sp) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("feed_category_$option"),
                            shape = RoundedCornerShape(24.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ConnectDarkGreen, selectedLabelColor = ConnectWhite,
                                containerColor = ConnectWhite, labelColor = ConnectGrayMedium)
                        )
                    }
                }
            }
        }
        if (!showPlaces && query.isBlank() && category == "All") {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listOf("Explore", "Meet", "Play", "Learn")) { option ->
                        Card(onClick = { onCategory(option) }, modifier = Modifier.width(116.dp).height(92.dp),
                            shape = RoundedCornerShape(12.dp)) {
                            ActivityPhotography(option, Modifier.fillMaxSize(), categoryTitle(option))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (showPlaces) "Places to explore" else "Plans around you", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                    Text(if (area == "All") "Around Kathmandu" else "In $area", fontSize = 13.sp, color = ConnectGrayMedium)
                }
                if (showPlaces) TextButton(onClick = onSurprise) { Text("Pick for me") }
            }
        }
        if (!showPlaces && activityError != null) {
            item { FeedNotice("Activities couldn't be refreshed", activityError) }
        }
        if (query.isNotBlank() || area != "All" || (!showPlaces && category != "All")) {
            item {
                TextButton(onClick = { onQuery(""); onArea("All"); onCategory("All") },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("reset_feed_filters")) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Clear search and filters", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        if (showPlaces) {
            if (spots.isEmpty()) {
                item { FeedEmpty("No places match yet", "Try another area or clear your search.", "Clear filters") {
                    onQuery(""); onArea("All")
                } }
            }
            items(spots, key = { "spot_${it.id}" }) { spot ->
                OutlinedCard(onClick = { onSpot(spot) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ConnectGrayLight),
                    colors = CardDefaults.outlinedCardColors(containerColor = ConnectWhite)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = ConnectMint, shape = RoundedCornerShape(16.dp)) {
                            Text(spot.emoji, fontSize = 26.sp, modifier = Modifier.padding(14.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(spot.section, fontSize = 12.sp, color = ConnectDarkGreen)
                            Text(spot.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                            Text(spot.location, fontSize = 14.sp, color = ConnectGrayMedium)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = ConnectGrayMedium)
                    }
                }
            }
        } else {
            if (plans.isEmpty() && activityError == null) {
                item { FeedEmpty(
                    if (query.isBlank() && category == "All" && area == "All") "Start something small" else "No matching plans yet",
                    "Invite someone for a coffee, a game or a walk. Your first plan starts here.", "Make a plan", onCreate
                ) }
            }
            items(plans, key = { "plan_${it.id}" }) { plan ->
                PlanCard(plan, { onJoin(plan) }, { onSave(plan) }, { onChat(plan) }, onDetails = { selectedPlanId = plan.id })
            }
            item {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Free to meet?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = onAvailability) {
                        Text(if (availabilities.any { it.isCurrentUser }) "Go offline" else "I'm available")
                    }
                }
            }
            if (communityError != null) item { FeedNotice("Community couldn't be refreshed", communityError) }
            if (availabilities.isNotEmpty()) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availabilities, key = { it.userId }) { AvailabilityPill(it) }
                }
            }
            if (groups.isNotEmpty()) {
                item {
                    Text("Find your circle", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark,
                        modifier = Modifier.padding(20.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(groups, key = { it.id }) { group -> GroupCard(group) { onGroup(group) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = if (selected) ConnectGrayDark else ConnectGrayMedium)) {
            Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) ConnectDarkGreen else ConnectGrayLight))
    }
}

@Composable
private fun FeedNotice(title: String, detail: String) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = ConnectCream, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = ConnectGrayDark, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 14.sp, color = ConnectGrayMedium)
        }
    }
}

@Composable
private fun FeedEmpty(title: String, detail: String, action: String, onAction: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = ConnectWhite, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, ConnectGrayLight)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Person, contentDescription = null, tint = ConnectDarkGreen, modifier = Modifier.size(32.dp))
            Text(title, fontSize = 23.sp, lineHeight = 28.sp, color = ConnectGrayDark, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 15.sp, lineHeight = 22.sp, color = ConnectGrayMedium)
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) { Text(action) }
        }
    }
}
