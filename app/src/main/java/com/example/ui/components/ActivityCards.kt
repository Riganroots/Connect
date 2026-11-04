package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Group
import com.example.data.models.Plan
import com.example.ui.theme.*

@Composable
fun GroupCard(gp: Group, onJoinToggle: () -> Unit) {
    Card(
        modifier = Modifier.width(244.dp).heightIn(min = 148.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        border = BorderStroke(1.dp, ConnectGrayLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(34.dp).background(ConnectMint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (gp.category) {
                            "Play" -> Icons.Default.Star
                            "Explore" -> Icons.Default.LocationOn
                            "Learn" -> Icons.Default.Info
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = ConnectDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gp.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (gp.isMember) "Joined community" else "Community",
                        fontSize = 13.sp,
                        color = ConnectGrayMedium
                    )
                }
            }

            Text(
                text = gp.description,
                fontSize = 13.sp,
                color = ConnectGrayMedium,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(20.dp), color = ConnectCream) {
                    Text(
                        text = gp.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConnectDarkGreen
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (gp.isMember) {
                    OutlinedButton(
                        onClick = onJoinToggle,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, ConnectGrayLight)
                    ) {
                        Text("Joined", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    }
                } else {
                    Button(
                        onClick = onJoinToggle,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
                    ) {
                        Text("Join", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
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
    onChatClick: () -> Unit,
    onDetails: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(enabled = onDetails != null) { onDetails?.invoke() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        border = BorderStroke(1.dp, ConnectGrayLight)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(plan.category + if (plan.isJoinedByMe) " · Joined" else "", color = ConnectDarkGreen,
                        style = MaterialTheme.typography.labelMedium)
                    Text(plan.title, style = MaterialTheme.typography.titleMedium, color = ConnectGrayDark,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onSaveToggle, modifier = Modifier.size(48.dp)) {
                    Icon(if (plan.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (plan.isSaved) "Unsave plan" else "Save plan",
                        tint = if (plan.isSaved) ConnectError else ConnectGrayMedium, modifier = Modifier.size(21.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                PlanMetaRow(Icons.Default.DateRange, formatPlanSchedule(plan.date, plan.time))
                PlanMetaRow(Icons.Default.LocationOn, plan.location)
            }
            HorizontalDivider(color = ConnectGrayLight)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(ConnectCream, CircleShape), contentAlignment = Alignment.Center) {
                    Text(plan.organizerName.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ConnectDarkGreen)
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(plan.organizerName, fontSize = 13.sp, color = ConnectGrayDark, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${plan.joinedCount} joined", fontSize = 12.sp, color = ConnectGrayMedium)
                }
                Text(plan.pricePerPerson.ifBlank { "Ask host" }, fontSize = 13.sp, color = ConnectGrayDark,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp))
            }
            if (onDetails != null) {
                OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ConnectGrayLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen)) {
                    Text(if (plan.isJoinedByMe) "View your plan" else "View plan")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else if (plan.isJoinedByMe) {
                Button(onClick = onChatClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)) { Text("Open group chat") }
                TextButton(onClick = onJoinToggle, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Leave activity", color = ConnectGrayMedium)
                }
            } else {
                Button(onClick = onJoinToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)) { Text("Join this plan") }
            }
        }
    }
}

@Composable
private fun PlanMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ConnectDarkGreen, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = ConnectGrayDark,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


private fun formatPlanSchedule(date: String, time: String): String {
    val cleanDate = date.trim()
        .replace(Regex("\\s+"), " ")
        .replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }

    val rawTime = time.trim().replace(Regex("\\s+"), " ")
    val cleanTime = Regex("^(\\d{1,2})\\s+(\\d{2})$")
        .matchEntire(rawTime)
        ?.let { match -> "${match.groupValues[1]}:${match.groupValues[2]}" }
        ?: rawTime

    return listOf(cleanDate, cleanTime)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}
