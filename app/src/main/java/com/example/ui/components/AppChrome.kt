package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.UserProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun AppHeader(
    profile: UserProfile?,
    viewModel: ConnectViewModel,
    onConfigureProfile: () -> Unit
) {
    val notificationRows by viewModel.allNotifications.collectAsStateWithLifecycle()
    val currentUid by viewModel.currentUserId.collectAsStateWithLifecycle()
    // Also filter the StateFlow replay while the new account's Room query starts.
    val notifications = notificationRows.filter { it.ownerId == currentUid }
    var showNotifications by remember(currentUid) { mutableStateOf(false) }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = ConnectDarkGreen
                    )
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark
                    )
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ConnectDarkGreen.copy(alpha = 0.45f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You're all caught up",
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                        Text(
                            text = "Updates about activities, chat and communities will appear here.",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notification ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ConnectCream),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val icon = when (notification.systemCategory) {
                                        "Reminder" -> Icons.Default.DateRange
                                        "Verification" -> Icons.Default.CheckCircle
                                        "Recommendation" -> Icons.Default.LocationOn
                                        else -> Icons.Default.Notifications
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = ConnectDarkGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                                        Text(
                                            text = notification.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notification.description,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            color = ConnectGrayMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNotification(notification.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete notification",
                                            tint = ConnectGrayMedium,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Done", color = ConnectDarkGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllNotifications() }) {
                        Text("Clear all", color = ConnectError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = ConnectMintLight
        )
    }

    ConnectTopBar(
        location = profile?.location?.takeIf { it.isNotBlank() } ?: "Kathmandu, Nepal",
        name = profile?.name ?: "Connect Member",
        hasNotifications = notifications.isNotEmpty(),
        onNotifications = { showNotifications = true }, onProfile = onConfigureProfile
    )
}

@Composable
fun ConnectTopBar(location: String, name: String, hasNotifications: Boolean,
    onNotifications: () -> Unit, onProfile: () -> Unit) {
    Surface(color = ConnectWhite) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("connect", color = ConnectGrayDark, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ConnectGrayMedium, modifier = Modifier.size(12.dp))
                    Text(location, color = ConnectGrayMedium, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 3.dp))
                }
            }
            Box {
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = ConnectGrayDark, modifier = Modifier.size(22.dp))
                }
                if (hasNotifications) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(6.dp).background(ConnectError, CircleShape))
            }
            IconButton(onClick = onProfile) {
                Box(Modifier.size(32.dp).background(ConnectMint, CircleShape), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = ConnectDarkGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
    NavigationBar(
        containerColor = ConnectWhite,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .background(ConnectWhite)
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Activities"
                )
            },
            label = { Text("Discover", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = connectNavigationColors()
        )

        NavigationBarItem(
            selected = currentScreen is Screen.CreatePlan,
            onClick = { onNavigate(Screen.CreatePlan) },
            icon = {
                Box(Modifier.size(40.dp).background(ConnectDarkGreen, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Create a plan", tint = ConnectWhite, modifier = Modifier.size(22.dp))
                }
            },
            label = { Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = connectNavigationColors()
        )

        NavigationBarItem(
            selected = currentScreen is Screen.Profile,
            onClick = { onNavigate(Screen.Profile) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("My plans", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = connectNavigationColors()
        )
    }
}

@Composable
private fun connectNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ConnectDarkGreen,
    selectedTextColor = ConnectDarkGreen,
    indicatorColor = Color.Transparent,
    unselectedIconColor = ConnectGrayMedium,
    unselectedTextColor = ConnectGrayMedium
)
