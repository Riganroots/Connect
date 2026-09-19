package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
                        fontSize = 10.sp,
                        color = ConnectGrayMedium
                    )
                }
            }

            Text(
                text = gp.description,
                fontSize = 10.sp,
                color = ConnectGrayMedium,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(20.dp), color = ConnectCream) {
                    Text(
                        text = gp.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
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
                        modifier = Modifier.height(34.dp),
                        border = BorderStroke(1.dp, ConnectGrayLight)
                    ) {
                        Text("Joined", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    }
                } else {
                    Button(
                        onClick = onJoinToggle,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
                    ) {
                        Text("Join", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    onChatClick: () -> Unit
) {
    val chatRequiresJoin = plan.cloudId.isNotBlank() && !plan.isJoinedByMe

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ConnectWhite),
        border = BorderStroke(1.dp, ConnectGrayLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = ConnectMint) {
                            Text(
                                text = plan.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConnectDarkGreen
                            )
                        }
                        if (plan.isVerifiedOrganizer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified organizer",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Verified host", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = plan.title,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(ConnectCream)
                ) {
                    Icon(
                        imageVector = if (plan.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (plan.isSaved) "Unsave plan" else "Save plan",
                        tint = if (plan.isSaved) ConnectError else ConnectGrayMedium,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).background(ConnectDarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plan.organizerName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(plan.organizerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                Text("  ★ ${plan.organizerRating}", fontSize = 10.sp, color = ConnectGrayMedium)
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = ConnectCream) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PlanMetaRow(icon = Icons.Default.LocationOn, text = plan.location)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PlanMetaRow(
                            icon = Icons.Default.DateRange,
                            text = "${plan.date} · ${plan.time}",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = plan.pricePerPerson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectDarkGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = plan.description,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = ConnectGrayMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Person, contentDescription = null, tint = ConnectGrayMedium, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${plan.joinedCount} joined", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                Text("  •  ${plan.participantsNeeded} more wanted", fontSize = 10.sp, color = ConnectGrayMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onChatClick,
                    enabled = !chatRequiresJoin,
                    modifier = Modifier.weight(0.42f).height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ConnectGrayLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen)
                ) {
                    Icon(
                        if (chatRequiresJoin) Icons.Default.Lock else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        if (chatRequiresJoin) "Join to chat" else "Chat",
                        fontSize = if (chatRequiresJoin) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onJoinToggle,
                    modifier = Modifier.weight(0.58f).height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (plan.isJoinedByMe) ConnectCream else ConnectDarkGreen,
                        contentColor = if (plan.isJoinedByMe) ConnectDarkGreen else Color.White
                    ),
                    border = if (plan.isJoinedByMe) BorderStroke(1.dp, ConnectMint) else null
                ) {
                    Text(
                        text = if (plan.isJoinedByMe) "✓ Joined" else "Join activity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
            fontSize = 10.sp,
            color = ConnectGrayDark,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
