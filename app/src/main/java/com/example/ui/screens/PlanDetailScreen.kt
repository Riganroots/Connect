package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Plan
import com.example.ui.components.ActivityPhotography
import com.example.ui.theme.*

@Composable
fun PlanDetailScreen(plan: Plan, onBack: () -> Unit, onSave: () -> Unit,
    onJoin: () -> Unit, onChat: () -> Unit, onMaps: () -> Unit) {
    Column(Modifier.fillMaxSize().background(ConnectWhite).safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    ActivityPhotography(plan.category, Modifier.fillMaxSize(), "${plan.category} inspiration · Category photo")
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ConnectWhite) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to discovery", tint = ConnectGrayDark) }
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = CircleShape, color = ConnectWhite) {
                            IconButton(onClick = onSave) {
                                Icon(if (plan.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (plan.isSaved) "Unsave plan" else "Save plan",
                                    tint = if (plan.isSaved) ConnectError else ConnectGrayDark)
                            }
                        }
                    }
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plan.category.uppercase(), color = ConnectDarkGreen, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                        Text(plan.title, fontSize = 28.sp, lineHeight = 33.sp, letterSpacing = (-0.5).sp,
                            fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                        Text("${plan.joinedCount} joined · ${plan.participantsNeeded} more wanted", color = ConnectGrayMedium, fontSize = 14.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = ConnectCream) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(ConnectMint, CircleShape), contentAlignment = Alignment.Center) {
                                Text(plan.organizerName.take(1).uppercase(), color = ConnectDarkGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Your host", color = ConnectGrayMedium, fontSize = 12.sp)
                                Text(plan.organizerName, color = ConnectGrayDark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (plan.isVerifiedOrganizer) Icon(Icons.Default.CheckCircle, contentDescription = "Verified host", tint = ConnectDarkGreen)
                        }
                    }
                    DetailSection("The plan") { Text(plan.description, color = ConnectGrayMedium, fontSize = 16.sp, lineHeight = 24.sp) }
                    HorizontalDivider(color = ConnectGrayLight)
                    DetailSection("When we're meeting") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = ConnectDarkGreen)
                            Text(listOf(plan.date, plan.time).filter { it.isNotBlank() }.joinToString(" · "),
                                color = ConnectGrayDark, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                    DetailSection("Where to meet") {
                        Text(plan.location, color = ConnectGrayDark, fontSize = 16.sp)
                        Text("Confirm the exact meeting point with your host in group chat.", color = ConnectGrayMedium, fontSize = 14.sp, lineHeight = 20.sp)
                        OutlinedButton(onClick = onMaps, enabled = plan.location.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Maps")
                        }
                    }
                    if (plan.isJoinedByMe) TextButton(onClick = onJoin, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Leave this plan", color = ConnectGrayMedium)
                    }
                }
            }
        }
        Surface(color = ConnectWhite, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Cost per person", color = ConnectGrayMedium, fontSize = 12.sp)
                    Text(plan.pricePerPerson.ifBlank { "Ask host" }, color = ConnectGrayDark, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = if (plan.isJoinedByMe) onChat else onJoin, shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                    modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(if (plan.isJoinedByMe) "Group chat" else "Join plan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = ConnectGrayDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        content()
    }
}
