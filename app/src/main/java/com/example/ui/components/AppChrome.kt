package com.example

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
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    var showNotifications by remember { mutableStateOf(false) }

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
                            text = "Updates about plans, reminders and trust will appear here.",
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
                                    Column(modifier = Modifier.weight(1f)) {
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

    Surface(
        color = ConnectWhite,
        contentColor = ConnectGrayDark,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connect",
                    color = ConnectDarkGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ConnectGrayMedium,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = profile?.location ?: "Kathmandu, Nepal",
                        color = ConnectGrayMedium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (profile?.isTravellerMode == true) {
                        Text(
                            text = "  •  Traveller",
                            color = ConnectDarkGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showNotifications = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ConnectCream)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .background(ConnectError, CircleShape)
                            .border(1.5.dp, ConnectWhite, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(ConnectCream)
                    .clickable(onClick = onConfigureProfile)
                    .padding(start = 5.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(ConnectDarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (profile?.name?.take(1) ?: "A").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )

                    if (profile?.isVerified == true) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(10.dp)
                                .background(Color(0xFF48BB78), CircleShape)
                                .border(1.dp, ConnectWhite, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = profile?.name ?: "Ayush",
                    color = ConnectGrayDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
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
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Discover"
                )
            },
            label = { Text("Discover", fontWeight = FontWeight.SemiBold) },
            colors = connectNavigationColors()
        )

        NavigationBarItem(
            selected = currentScreen is Screen.CreatePlan,
            onClick = { onNavigate(Screen.CreatePlan) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create a plan"
                )
            },
            label = { Text("Create", fontWeight = FontWeight.SemiBold) },
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
            label = { Text("Profile", fontWeight = FontWeight.SemiBold) },
            colors = connectNavigationColors()
        )
    }
}

@Composable
private fun connectNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ConnectWhite,
    selectedTextColor = ConnectDarkGreen,
    indicatorColor = ConnectDarkGreen,
    unselectedIconColor = ConnectGrayMedium,
    unselectedTextColor = ConnectGrayMedium
)
