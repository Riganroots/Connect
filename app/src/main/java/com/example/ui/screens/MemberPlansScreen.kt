package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Plan
import com.example.data.models.UserProfile
import com.example.ui.components.PlanCard
import com.example.ui.theme.*

@Composable
fun MemberPlansScreen(profile: UserProfile?, plans: List<Plan>, uid: String,
    onEditProfile: () -> Unit, onSignOut: () -> Unit, onCreate: () -> Unit, onDiscover: () -> Unit,
    onSave: (Plan) -> Unit, onJoin: (Plan) -> Unit, onChat: (Plan) -> Unit,
    accountContent: @Composable ColumnScope.() -> Unit = {}) {
    val preview = uid == "preview-user"
    var selectedTab by remember(uid) { mutableStateOf(0) }
    var settings by remember(uid) { mutableStateOf(false) }
    var selectedPlanId by remember(uid) { mutableStateOf<Long?>(null) }
    val hosted = plans.filter { if (it.cloudId.isNotBlank()) it.organizerId == uid else it.organizerName == profile?.name }
    val joined = plans.filter { it.isJoinedByMe }
    val saved = plans.filter { it.isSaved }
    val visiblePlans = when (selectedTab) { 0 -> joined; 1 -> hosted; else -> saved }
    plans.firstOrNull { it.id == selectedPlanId }?.let { plan ->
        com.example.ui.screens.PlanDetailDialog(plan, { selectedPlanId = null },
            { onSave(plan) }, { onJoin(plan) },
            { selectedPlanId = null; onChat(plan) })
    }
    LazyColumn(Modifier.fillMaxSize().background(ConnectMintLight), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("My plans", style = MaterialTheme.typography.headlineMedium, color = ConnectGrayDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(ConnectMint, CircleShape), contentAlignment = Alignment.Center) {
                        Text((profile?.name ?: "C").take(1).uppercase(), fontSize = 20.sp, color = ConnectDarkGreen, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(profile?.name ?: "Connect member", style = MaterialTheme.typography.titleMedium, color = ConnectGrayDark)
                        Text(profile?.location ?: "Kathmandu, Nepal", style = MaterialTheme.typography.bodyMedium, color = ConnectGrayMedium)
                    }
                    TextButton(onClick = onEditProfile) { Text("Edit", color = ConnectDarkGreen) }
                }
                if (!profile?.bio.isNullOrBlank()) Text(profile!!.bio, style = MaterialTheme.typography.bodyMedium, color = ConnectGrayMedium)
                else Text("Add a little about yourself so your group knows who they're meeting.", style = MaterialTheme.typography.bodyMedium, color = ConnectGrayMedium)
            }
        }
        item {
            TabRow(selectedTabIndex = selectedTab, containerColor = ConnectWhite, contentColor = ConnectDarkGreen) {
                listOf("Joined" to joined.size, "Hosting" to hosted.size, "Saved" to saved.size).forEachIndexed { index, (label, count) ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = {
                        Text("$label ($count)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (visiblePlans.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when (selectedTab) { 0 -> "Your next plan belongs here"; 1 -> "Bring a few people together"; else -> "Keep a plan for later" },
                    style = MaterialTheme.typography.titleLarge, color = ConnectGrayDark)
                Text(when (selectedTab) { 0 -> "Join an activity and find the details and group chat here."; 1 -> "Start with a coffee, a game or a walk."; else -> "Tap the heart on an activity to save it here." },
                    style = MaterialTheme.typography.bodyMedium, color = ConnectGrayMedium)
                Button(onClick = { if (selectedTab == 1) onCreate() else onDiscover() },
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)) {
                    Text(if (selectedTab == 1) "Create a plan" else "Discover activities")
                }
            }
        }
        items(visiblePlans, key = { it.id }) { plan ->
            PlanCard(plan, { onJoin(plan) }, { onSave(plan) },
                { onChat(plan) }, onDetails = { selectedPlanId = plan.id })
        }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                TextButton(onClick = { settings = !settings }, modifier = Modifier.fillMaxWidth()) {
                    Text("Account", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, color = ConnectGrayMedium)
                    Icon(if (settings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Toggle account settings", tint = ConnectGrayMedium)
                }
                if (settings) {
                    accountContent()
                    TextButton(onClick = onSignOut) { Text(if (preview) "Exit preview" else "Sign out", color = ConnectGrayMedium) }
                }
            }
        }
    }
}


