package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Availability
import com.example.data.models.Group
import com.example.data.models.Plan
import com.example.data.models.UserProfile
import com.example.auth.AuthViewModel
import com.example.ui.auth.AuthGate
import com.example.ui.theme.*
import com.example.ui.components.AppHeader
import com.example.ui.components.ConnectBottomNavigation
import com.example.ui.components.GroupCard
import com.example.ui.components.PlanCard
import com.example.ui.screens.CreatePlanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.kathmanduDiscoverSpots

class MainActivity : ComponentActivity() {
    private val viewModel: ConnectViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AuthGate(viewModel = authViewModel) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            ConnectApp(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectApp(viewModel: ConnectViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showProfileCreator by remember { mutableStateOf(false) }

    // If profile is fully custom, edit can be toggled
    if (showProfileCreator) {
        ProfileSetupDialog(
            profile = profile ?: UserProfile(),
            onDismiss = { showProfileCreator = false },
            onSave = { name, bio, isTraveller, interests ->
                viewModel.updateProfileInfo(name, bio, isTraveller, interests)
                showProfileCreator = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Bar
            AppHeader(
                profile = profile,
                viewModel = viewModel,
                onConfigureProfile = { showProfileCreator = true }
            )

            // Switch Screen Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ConnectMintLight)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(viewModel = viewModel)
                    }
                    is Screen.CreatePlan -> {
                        CreatePlanScreen(
                            viewModel = viewModel,
                            onPlanPublished = {
                                viewModel.navigateTo(Screen.Home)
                            }
                        )
                    }
                    is Screen.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onEditProfile = { showProfileCreator = true }
                        )
                    }
                    is Screen.ChatDetail -> {
                        ChatDetailScreen(
                            viewModel = viewModel,
                            planId = screen.planId,
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )
                    }
                }
            }

            // Bottom Nav (Only visible for Home, CreatePlan and Profile)
            if (currentScreen !is Screen.ChatDetail) {
                ConnectBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    }
}

// ======================== HOME SCREEN ========================
@Composable
fun ProfileScreen(viewModel: ConnectViewModel, onEditProfile: () -> Unit) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()

    val myPublishedPlans = plans.filter { it.organizerName == (profile?.name ?: "Ayush") }
    val myJoinedPlans = plans.filter { it.isJoinedByMe }
    val mySavedPlans = plans.filter { it.isSaved }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Joined, 1 = My Hosted, 2 = Saved

    // Interactive Verification Wizard States
    var showWizard by remember { mutableStateOf(false) }
    var wizardStep by remember { mutableStateOf(1) } // 1=Phone Check, 2=SMS OTP, 3=Document selection, 4=Scanner sim, 5=Badge claim
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var idName by remember { mutableStateOf(profile?.name ?: "Ayush") }
    var docType by remember { mutableStateOf("Citizenship Certificate") }
    var docNumber by remember { mutableStateOf("") }
    var scanPhase by remember { mutableStateOf(0) } // 0=idle, 1=scanning, 2=done
    var laserPosition by remember { mutableStateOf(0f) }
    var feedbackMessage by remember { mutableStateOf("Positioning your document...") }
    var isTimerActive by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(59) }

    LaunchedEffect(profile) {
        profile?.let {
            if (idName == "Ayush" || idName.isBlank()) {
                idName = it.name
            }
        }
    }

    LaunchedEffect(isTimerActive, timeRemaining) {
        if (isTimerActive && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining -= 1
        } else if (timeRemaining == 0) {
            isTimerActive = false
        }
    }

    LaunchedEffect(scanPhase) {
        if (scanPhase == 1) {
            feedbackMessage = "Positioning document limits..."
            kotlinx.coroutines.delay(700)
            feedbackMessage = "Aligning camera viewfinder boundaries..."
            kotlinx.coroutines.delay(800)
            feedbackMessage = "Reading security security watermarks..."
            
            // Simulating laser scan line moving down
            for (pos in 0..15) {
                laserPosition = pos * 11f
                kotlinx.coroutines.delay(90)
            }
            
            feedbackMessage = "Biometric facial features comparison..."
            kotlinx.coroutines.delay(1200)
            feedbackMessage = "Securing governmental verification databases... Approved!"
            kotlinx.coroutines.delay(700)
            scanPhase = 2
            wizardStep = 5
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
    ) {
        // Visual Profile Card Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConnectMintLight)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Profile Icon Avatar Wrapper
                    Box(
                        modifier = Modifier.size(78.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(ConnectDarkGreen)
                                .border(3.dp, if (profile?.isVerified == true) Color(0xFF48BB78) else ConnectMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar picture icon",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        if (profile?.isVerified == true) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF48BB78), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified badge check",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.name ?: "Ayush",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayDark
                        )
                        if (profile?.isVerified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Local check",
                                tint = Color(0xFF48BB78),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Score stars",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile?.rating ?: 4.8} Stars Local Companion Rating",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = profile?.bio ?: "Exploring local trails and networking with sports players around Lalitpur.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = ConnectGrayMedium,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    val userInterests = (profile?.interests ?: "Hiking 🏔️, Food Walk 🥟, Futsal ⚽, Live Music 🎸")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (userInterests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "MY INTERESTS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ConnectGrayMedium,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("profile_interests_row")
                        ) {
                            items(userInterests) { interest ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(ConnectCream)
                                        .border(1.dp, ConnectDarkGreen.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = interest,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConnectDarkGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Configuration command
                    OutlinedButton(
                        onClick = { onEditProfile() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnectDarkGreen),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Config Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Trust & Verification Center Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = if (profile?.isVerified == true) Color(0xFFC6F6D5) else ConnectGrayLight,
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (profile?.isVerified == true) Color(0xFFF0FFF4) else ConnectCream
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (profile?.isVerified == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Trust emblem",
                                tint = if (profile?.isVerified == true) Color(0xFF38A169) else Color(0xFFDD6B20),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trust & Verification Center",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConnectGrayDark
                            )
                        }

                        if (profile?.isVerified == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xFF38A169))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (profile?.isVerified == true) {
                        // VERIFIED STATE: Show secure metadata information
                        Text(
                            text = "You are a Verified Kathmandu Companion! Other members planning real-life activities will see the green verified badge on your profile and hosted plans.",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SECURED DATA & TRUST METADATA:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone Verified", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Phone: ${profile?.verifiedPhone}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ConnectGrayDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "ID Match Approved", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ID Match Check: ${profile?.verifiedNationalIdName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ConnectGrayDark
                                    )
                                }
                                if (!profile?.verificationMethod.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = "Secured via", tint = Color(0xFF38A169), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Method: ${profile?.verificationMethod}",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = { viewModel.revokeUserVerification() },
                                colors = ButtonDefaults.textButtonColors(contentColor = ConnectError)
                            ) {
                                Text("Revoke Check", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (!showWizard) {
                        // UN-VERIFIED & NOT YET IN WIZARD: Show description with perks and single start Button
                        Text(
                            text = "Verify your account and phone number to earn a security badge and establish high coordination confidence.",
                            fontSize = 11.sp,
                            color = ConnectGrayMedium,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Trust advantage info",
                                tint = ConnectDarkGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Why verify?",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConnectGrayDark
                                )
                                Text(
                                    text = "Verified hosts receive 4.5x higher attendee trust. You also unlock real-time peer availability sharing.",
                                    fontSize = 9.sp,
                                    color = ConnectGrayMedium,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                showWizard = true
                                wizardStep = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier.testTag("upgrade_trust_button").align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Upgrade badge unlock", modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upgrade Trust Level", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // WIZARD IS ACTIVE: Show step-by-step progress and gorgeous interface
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ConnectGrayLight.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Progress Dots or steps summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Step $wizardStep of 4",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConnectDarkGreen
                                    )
                                    // Simulated simple step indicator dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (i in 1..4) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (wizardStep >= i) ConnectDarkGreen else ConnectGrayLight
                                                    )
                                            )
                                        }
                                    }
                                }
                                
                                LinearProgressIndicator(
                                    progress = {
                                        when (wizardStep) {
                                            1 -> 0.25f
                                            2 -> 0.50f
                                            3 -> 0.75f
                                            4 -> 0.90f
                                            else -> 1.0f
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .height(4.dp),
                                    color = ConnectDarkGreen,
                                    trackColor = ConnectGrayLight
                                )

                                when (wizardStep) {
                                    1 -> {
                                        // STEP 1: Phone submission
                                        Text(
                                            text = "Verify Phone Contact",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "Provide your phone number to authorize secure community peer coordination via SMS warnings.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { phoneNumber = it },
                                            label = { Text("Kathmandu Companion Mobile", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. +977 9812345678", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { showWizard = false }
                                            ) {
                                                Text("Exit Setup", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (phoneNumber.isNotBlank()) {
                                                        wizardStep = 2
                                                        isTimerActive = true
                                                        timeRemaining = 59
                                                    }
                                                },
                                                enabled = phoneNumber.trim().isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Send SMS Verification Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    2 -> {
                                        // STEP 2: Simulated OTP SMS Input
                                        Text(
                                            text = "Enter 6-Digit SMS Code Check",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "We sent a confirmation text to $phoneNumber. Resending allowed after timer.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        // Gorgeous Notification Toast message simulating active SMS receipt
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = ConnectMintLight),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ConnectMint)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "SMS banner",
                                                    tint = ConnectDarkGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "📱 SIMULATED SMS RECEIVED",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Text(
                                                        text = "Your safe Connect OTP Code is 552489",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectGrayDark
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "AUTOFILL",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ConnectDarkGreen,
                                                    modifier = Modifier
                                                        .background(ConnectWhite, RoundedCornerShape(4.dp))
                                                        .clickable { otpCode = "552489" }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = otpCode,
                                            onValueChange = { if (it.length <= 6) otpCode = it },
                                            label = { Text("6-Digit OTP Verification Security Code", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. 552489", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    wizardStep = 1
                                                    isTimerActive = false
                                                }
                                            ) {
                                                Text("Change Phone", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (otpCode == "552489" || otpCode.length == 6) {
                                                        wizardStep = 3
                                                    }
                                                },
                                                enabled = otpCode.trim().length >= 6,
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Verify Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Text(
                                            text = if (isTimerActive) "Resend available in ${timeRemaining}s" else "Didn't receive it? Click Resend",
                                            fontSize = 9.sp,
                                            color = ConnectGrayMedium,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(top = 4.dp)
                                                .clickable(enabled = !isTimerActive) {
                                                    isTimerActive = true
                                                    timeRemaining = 59
                                                }
                                        )
                                    }
                                    3 -> {
                                        // STEP 3: ID selection & Name parameters
                                        Text(
                                            text = "Identification Details",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark
                                        )
                                        Text(
                                            text = "Government files are scanned securely to check names of travelers or local residents on Kathmandu lists.",
                                            fontSize = 10.sp,
                                            color = ConnectGrayMedium,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        OutlinedTextField(
                                            value = idName,
                                            onValueChange = { idName = it },
                                            label = { Text("Government ID Full Legal Name", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Text(
                                            text = "Select Document Type Support:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        
                                        // Beautiful Row of Document Options
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val docs = listOf(
                                                "Citizenship Certificate" to "📜 Civil ID",
                                                "Passport" to "✈️ Passport",
                                                "Driving License" to "🚗 License"
                                            )
                                            docs.forEach { (type, label) ->
                                                val isSel = docType == type
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { docType = type }
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) ConnectDarkGreen else ConnectGrayLight,
                                                            shape = RoundedCornerShape(10.dp)
                                                        ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSel) ConnectMintLight else ConnectWhite
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) ConnectDarkGreen else ConnectGrayMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        OutlinedTextField(
                                            value = docNumber,
                                            onValueChange = { docNumber = it },
                                            label = { Text("$docType Reference Number", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. 52-01-78-01124", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ConnectDarkGreen,
                                                unfocusedBorderColor = ConnectGrayLight
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { wizardStep = 2 }) {
                                                Text("Back to SMS", color = ConnectGrayMedium, fontSize = 11.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (idName.isNotBlank() && docNumber.isNotBlank()) {
                                                        wizardStep = 4
                                                        scanPhase = 1
                                                    }
                                                },
                                                enabled = idName.trim().isNotBlank() && docNumber.trim().isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Start Identity Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    4 -> {
                                        // STEP 4: Live scanner simulator screen
                                        Text(
                                            text = "Simulating Document Secure Check",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ConnectGrayDark,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Viewfinder card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ConnectGrayDark)
                                                .border(2.dp, ConnectMint, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Mock Document Visual Card inside viewfinder
                                            Card(
                                                modifier = Modifier
                                                    .width(260.dp)
                                                    .height(150.dp)
                                                    .border(1.dp, ConnectWhite.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = ConnectCream)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "NEPAL SYSTEM GOVERNMENT ID",
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ConnectGrayDark
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(Color.Red, CircleShape)
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = "Official Document: $docType",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Name: $idName",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = ConnectGrayDark
                                                    )
                                                    Text(
                                                        text = "Ref Code: $docNumber",
                                                        fontSize = 9.sp,
                                                        color = ConnectGrayMedium
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "🔒 BIOMETRIC SECURE SCAN ACTIVE",
                                                        fontSize = 7.sp,
                                                        color = ConnectGrayMedium
                                                    )
                                                }
                                            }

                                            // Scanning Beam horizontal laser Line sliding down
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = laserPosition.dp)
                                                    .background(Color(0xFF48BB78))
                                                    .shadow(elevation = 6.dp, ambientColor = Color(0xFF48BB78), spotColor = Color(0xFF48BB78))
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Verification status feedback message and circular progress
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = ConnectDarkGreen,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = feedbackMessage,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ConnectGrayDark
                                            )
                                        }
                                    }
                                    5 -> {
                                        // STEP 5: Congratulations & Perks Claim screen
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE6F4EA)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Shield unlocked badge",
                                                    tint = Color(0xFF137333),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Text(
                                                text = "Account Checked Successfully!",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ConnectGrayDark
                                            )
                                            
                                            Text(
                                                text = "Government databases approved the ID card scan check matching for $idName using $docType.",
                                                fontSize = 10.sp,
                                                color = ConnectGrayMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                lineHeight = 13.sp
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Badges perks details display
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = ConnectMintLight),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "🔒 VERIFIED ACCOUNT BENEFITS UNLOCKED:",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ConnectDarkGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    
                                                    listOf(
                                                        "🛡️ Beautiful Green Verification Checkmark next to your name",
                                                        "🚀 Verified Host status flag auto-embedded on all hosted plans",
                                                        "🔥 High peer-coordination confidence to maximize actual real gathering attendees"
                                                    ).forEach { perk ->
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(text = "•", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConnectDarkGreen)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = perk,
                                                                fontSize = 9.sp,
                                                                color = ConnectGrayDark,
                                                                lineHeight = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.submitUserPhoneVerification(
                                                        phoneNumber = phoneNumber,
                                                        nationalIdName = "$idName ($docType Verified)",
                                                        method = docType,
                                                        onSuccess = {
                                                            showWizard = false
                                                            wizardStep = 1
                                                            scanPhase = 0
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                                                shape = RoundedCornerShape(30.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Claim Green Verified Shield Badge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sub tab options: Joined, Hosted, and Saved plans
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = ConnectDarkGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Joined (${myJoinedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Hosted (${myPublishedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Saved (${mySavedPlans.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // List views
        val currentList = when (selectedTab) {
            0 -> myJoinedPlans
            1 -> myPublishedPlans
            else -> mySavedPlans
        }
        if (currentList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "You haven't joined any activities yet. Go to Home to explore and join Kathmandu plans!"
                            1 -> "You haven't posted or hosted any plans yet."
                            else -> "No saved activities. Favorite general plans to bookmark them here."
                        },
                        fontSize = 12.sp,
                        color = ConnectGrayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(currentList, key = { it.id }) { plan ->
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


// ======================== CHAT DETAIL SCREEN ========================
@Composable
fun ChatDetailScreen(
    viewModel: ConnectViewModel,
    planId: Long,
    onBack: () -> Unit
) {
    val chatsFlow = remember(planId) { viewModel.getChatsForPlan(planId) }
    val chats by chatsFlow.collectAsStateWithLifecycle()

    var hostPlan by remember { mutableStateOf<Plan?>(null) }
    var userMessage by remember { mutableStateOf("") }

    // Fetch parent Plan to display host contact metadata
    LaunchedEffect(planId) {
        hostPlan = viewModel.getPlanById(planId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConnectMintLight)
    ) {
        // Chat Header with host name
        Surface(
            color = ConnectCream,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = ConnectDarkGreen
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Organizer Badge Visual
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ConnectDarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (hostPlan?.organizerName?.take(1) ?: "H").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Plan Host: ${hostPlan?.organizerName ?: "Lisa"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConnectGrayDark
                    )
                    Text(
                        text = hostPlan?.title ?: "Activity group discussion",
                        fontSize = 11.sp,
                        color = ConnectGrayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }

        // Message Feed List block
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chats) { chat ->
                val bubbleAlign = if (chat.isMe) Alignment.End else Alignment.Start
                val bubbleColor = if (chat.isMe) ConnectDarkGreen else ConnectCream
                val textColor = if (chat.isMe) Color.White else ConnectGrayDark

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = bubbleAlign
                ) {
                    // Small Name stamp if other members
                    if (!chat.isMe) {
                        Text(
                            text = chat.senderName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConnectGrayMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    // Rounded bubble
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (chat.isMe) 12.dp else 2.dp,
                                    bottomEnd = if (chat.isMe) 2.dp else 12.dp
                                )
                            )
                            .background(bubbleColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            text = chat.messageText,
                            fontSize = 12.sp,
                            color = textColor,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Typing toolbar
        Surface(
            color = ConnectCream,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    placeholder = { Text("Ask about dates, equipment, or details...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = ConnectDarkGreen,
                        unfocusedIndicatorColor = ConnectGrayLight
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (userMessage.isNotBlank()) {
                            viewModel.sendChatMessage(planId, userMessage)
                            userMessage = ""
                        }
                    },
                    containerColor = ConnectDarkGreen,
                    contentColor = Color.White,
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send text",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// ======================== FULL DIALOG BOXES OVERLAYS ========================

@Composable
fun ProfileSetupDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String) -> Unit
) {
    var nameInput by remember { mutableStateOf(profile.name) }
    var bioInput by remember { mutableStateOf(profile.bio) }
    var isTravellerOption by remember { mutableStateOf(profile.isTravellerMode) }

    val availableInterests = listOf(
        "Hiking 🏔️", "Food Walk 🥟", "Futsal ⚽", "Live Music 🎸", "Cafes ☕", 
        "Tech & Design 💻", "Language Exchange 🗣️", "Nightlife 🍹", "Temples & Heritage 🏛️", "Meditation 🧘"
    )

    var selectedInterests by remember {
        mutableStateOf(
            profile.interests.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        )
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                "Config My Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ConnectDarkGreen
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                Column {
                    Text("Display Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )
                }

                Column {
                    Text("Bio & Hobbies", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input"),
                        maxLines = 3
                    )
                }

                Column {
                    Text("Select My Interests", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val rows = availableInterests.chunked(2)
                    rows.forEach { rowInterests ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowInterests.forEach { interest ->
                                val isSelected = selectedInterests.contains(interest)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedInterests = if (isSelected) {
                                            selectedInterests - interest
                                        } else {
                                            selectedInterests + interest
                                        }
                                    },
                                    label = { Text(interest, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ConnectDarkGreen,
                                        selectedLabelColor = Color.White,
                                        containerColor = ConnectCream,
                                        labelColor = ConnectGrayDark
                                    ),
                                    modifier = Modifier.weight(1f).testTag("interest_chip_$interest")
                                )
                            }
                            if (rowInterests.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isTravellerOption,
                        onCheckedChange = { isTravellerOption = it },
                        colors = CheckboxDefaults.colors(checkedColor = ConnectDarkGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Active Traveller Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConnectGrayDark)
                        Text("Show traveller badge to match Kathmandu expats/hosts.", fontSize = 9.sp, color = ConnectGrayMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalInterests = selectedInterests.joinToString(", ")
                    onSave(nameInput, bioInput, isTravellerOption, finalInterests) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen),
                modifier = Modifier.testTag("apply_profile_button")
            ) {
                Text("Apply Parameters")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel", color = ConnectGrayMedium)
            }
        }
    )
}

@Composable
fun PostAvailabilityDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Coffee") } // Coffee, Sports, Hiking, Travel, Networking

    val tags = listOf("Coffee", "Sports", "Hiking", "Travel", "Networking")

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                "Go Live: I'm Free Now!",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ConnectDarkGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Let local Kathmandu groups know you have free hours right now. You’ll be pinned live in the upper row for matching!",
                    fontSize = 11.sp,
                    color = ConnectGrayMedium
                )

                Column {
                    Text("Current Context tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            val isSel = tag == selectedIcon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ConnectDarkGreen else ConnectCream)
                                    .clickable { selectedIcon = tag }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else ConnectGrayDark
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("My Status Text (Max 35 chars)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectGrayMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { if (it.length <= 35) statusText = it },
                        placeholder = { Text("e.g. Free for tea at Durbar Hill") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${statusText.length}/35 characters",
                        fontSize = 9.sp,
                        color = ConnectGrayMedium,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (statusText.isNotBlank()) {
                        onPost(statusText, selectedIcon)
                    }
                },
                enabled = statusText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConnectDarkGreen)
            ) {
                Text("Broadcast Live")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel", color = ConnectGrayMedium)
            }
        }
    )
}
