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
    val chatRequiresJoin = plan.cloudId.isNotBlank() && !plan.isJoinedByMe
    val accent = when (plan.category) {
        "Play" -> Color(0xFF9B4C22)
        "Explore" -> ConnectDarkGreen
        "Meet" -> Color(0xFF78509A)
        "Learn" -> ConnectBlue
        else -> Color(0xFF8F4E62)
    }
    val categoryIcon = when (plan.category) {
        "Play" -> Icons.Default.Star
        "Explore" -> Icons.Default.LocationOn
        "Meet" -> Icons.Default.Person
        "Learn" -> Icons.Default.Info
        else -> Icons.Default.DateRange
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(enabled = onDetails != null) { onDetails?.invoke() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        border = BorderStroke(1.dp, ConnectGrayLight)
    ) {
        ActivityPhotography(plan.category, Modifier.fillMaxWidth().height(170.dp), "${plan.category} inspiration")
        // Category colour is decoration, never a substitute for a real activity photo.
        Row(Modifier.fillMaxWidth().background(accent.copy(alpha = 0.08f)).padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(plan.category, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (plan.pricePerPerson.isNotBlank()) Text(plan.pricePerPerson, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 112.dp))
            IconButton(onClick = onSaveToggle, modifier = Modifier.size(48.dp)) {
                Icon(if (plan.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (plan.isSaved) "Unsave plan" else "Save plan",
                    tint = if (plan.isSaved) ConnectError else accent, modifier = Modifier.size(21.dp))
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(plan.title, fontSize = 23.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp,
                fontWeight = FontWeight.Bold, color = ConnectGrayDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanMetaRow(Icons.Default.DateRange, formatPlanSchedule(plan.date, plan.time))
                PlanMetaRow(Icons.Default.LocationOn, plan.location)
            }
            if (plan.description.isNotBlank()) Text(plan.description, fontSize = 14.sp, lineHeight = 21.sp,
                color = ConnectGrayMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = ConnectGrayLight)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(ConnectCream, CircleShape), contentAlignment = Alignment.Center) {
                    Text(plan.organizerName.take(1).uppercase(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.organizerName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = ConnectGrayDark, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (plan.isVerifiedOrganizer) Icon(Icons.Default.CheckCircle, contentDescription = "Verified organizer",
                            tint = ConnectDarkGreen, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                    }
                    Text("${plan.joinedCount} joined · ${plan.participantsNeeded} more wanted", fontSize = 12.sp,
                        color = ConnectGrayMedium)
                }
            }
            if (plan.isJoinedByMe) {
                Button(onClick = onChatClick, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Open group chat")
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
                TextButton(onClick = onJoinToggle, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
                    Text("Leave activity", fontSize = 13.sp, color = ConnectGrayMedium)
                }
            } else {
                Button(onClick = onJoinToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)) {
                    Text("Join this plan", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                if (!chatRequiresJoin) TextButton(onClick = onChatClick, modifier = Modifier.align(Alignment.End)) {
                    Text("Open group chat")
                }
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
