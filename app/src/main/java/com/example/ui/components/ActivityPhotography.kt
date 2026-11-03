package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@DrawableRes
internal fun categoryPhoto(category: String): Int = when (category) {
    "Play" -> R.drawable.connect_play
    "Meet" -> R.drawable.connect_meet
    "Learn" -> R.drawable.connect_learn
    else -> R.drawable.connect_explore
}

internal fun categoryTitle(category: String): String = when (category) {
    "Play" -> "Games & sports"
    "Explore" -> "Hikes & outdoors"
    "Meet" -> "Coffee & company"
    "Learn" -> "Learn together"
    else -> "Local experiences"
}

/** Editorial category imagery; never represented as a host's activity/venue photo. */
@Composable
fun ActivityPhotography(category: String, modifier: Modifier = Modifier, caption: String? = null) {
    Box(modifier) {
        Image(painterResource(categoryPhoto(category)), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (caption != null) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)), startY = 80f)))
            Text(caption, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
        }
    }
}
