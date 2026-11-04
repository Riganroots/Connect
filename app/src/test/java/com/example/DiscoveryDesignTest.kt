package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.models.Plan
import com.example.ui.components.PlanCard
import com.example.ui.components.ConnectTopBar
import com.example.ui.components.ConnectBottomNavigation
import com.example.ui.viewmodel.Screen
import com.example.ui.theme.ConnectWhite
import com.example.ui.screens.MemberPlansScreen
import com.example.ui.auth.AuthScreen
import com.example.data.models.UserProfile
import com.example.ui.screens.DiscoveryFeed
import com.example.ui.screens.PlanDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [34])
class DiscoveryDesignTest {
    @get:Rule val compose = createComposeRule()

    // Fixture only. Never injected into the live feed or presented as real user activity.
    private val walk = Plan(id = 1, cloudId = "test-walk", title = "A slow Saturday in Shivapuri",
        category = "Explore", location = "Budhanilkantha gate", date = "Saturday, 7 Nov", time = "7:00 AM",
        pricePerPerson = "Cost shared", participantsNeeded = 4, description = "An easy morning walk, fresh air and a cup of tea on the way back.",
        organizerName = "Aarav", organizerRating = 0.0, joinedCount = 2)

    @Test fun feedRendersAndCategorySelectionWorks() {
        var selected by mutableStateOf("All")
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize()) {
                    Feed(listOf(walk), selected, { selected = it })
                }
            }
        }
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/discovery-360.png")
        compose.onNodeWithText("Outdoors").performClick()
        compose.runOnIdle { assertEquals("Explore", selected) }
        compose.onNodeWithTag("feed_category_Explore").assertIsSelected()
        compose.onNodeWithText(walk.title).performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/discovery-activity-360.png")
    }

    @Test fun emptyFeedOffersWorkingCreateAction() {
        var creates = 0
        compose.setContent { MyApplicationTheme(darkTheme = false) { Feed(emptyList(), onCreate = { creates++ }) } }
        compose.onNodeWithText("Make a plan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, creates) }
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/discovery-empty-360.png")
    }

    @Test fun largeTextFeedRendersOnNarrowPhone() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                    Surface(Modifier.fillMaxSize()) { Feed(listOf(walk)) }
                }
            }
        }
        compose.onNodeWithText("View plan").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/discovery-large-text-360.png")
    }

    @Test fun joiningAndLeavingHaveDistinctActions() {
        var joins = 0
        var chats = 0
        var joined by mutableStateOf(false)
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlanCard(walk.copy(isJoinedByMe = joined), { joins++; joined = !joined }, {}, { chats++ })
            }
        }
        compose.onNodeWithText("Open group chat").assertDoesNotExist()
        compose.onNodeWithText("Join this plan").performClick()
        compose.onNodeWithText("Open group chat").performClick()
        compose.onNodeWithText("Leave activity").performClick()
        compose.runOnIdle { assertEquals(2, joins); assertEquals(1, chats) }
    }

    @Test fun detailShowsHostMeetingPointAndWorkingActions() {
        var joins = 0
        var maps = 0
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlanDetailScreen(walk, {}, {}, { joins++ }, {}, { maps++ })
            }
        }
        compose.onNodeWithText("Your host").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/activity-detail-360.png")
        compose.onNodeWithText("Join plan").performClick()
        compose.onNodeWithText("Open in Maps").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, joins); assertEquals(1, maps) }
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/activity-meeting-point-360.png")
    }

    @Test fun tappingActivityOpensDetailPage() {
        compose.setContent { MyApplicationTheme(darkTheme = false) { Feed(listOf(walk)) } }
        compose.onNodeWithText(walk.title).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back to discovery").assertIsDisplayed()
        compose.onNodeWithText("Join plan").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to discovery").performClick()
        compose.onNodeWithText("Join plan").assertDoesNotExist()
    }

    @Test fun completeDiscoveryScreen() {
        val coffee = walk.copy(id = 2, cloudId = "test-coffee", title = "Coffee & a conversation", category = "Meet", location = "Jhamsikhel", date = "Sunday, 8 Nov", time = "10:00 AM", organizerName = "Srijana", joinedCount = 3, pricePerPerson = "Pay your own")
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                Frame { Feed(listOf(walk, coffee)) }
            }
        }
        compose.onNodeWithText("Discover").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/complete-discover-360.png")
    }

    @Test fun completeMemberScreenAndHostingAction() {
        var creates = 0
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                Frame(Screen.Profile) {
                    MemberPlansScreen(UserProfile(name = "Aarav", bio = "Weekend walks, coffee and new conversations."),
                        listOf(walk.copy(isJoinedByMe = true, isSaved = true)), "member", {}, {}, { creates++ }, {}, {}, {}, {})
                }
            }
        }
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/complete-my-plans-360.png")
        compose.onNodeWithText("Hosting (0)").performClick()
        compose.onNodeWithText("Create a plan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, creates) }
    }

    @Test fun completeSignInScreen() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                AuthScreen(true, false, null, null, { _, _ -> }, { _, _ -> }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Password").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/complete-sign-in-360.png")
    }

    @Composable
    private fun Frame(screen: Screen = Screen.Home, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().background(ConnectWhite)) {
            ConnectTopBar("Kathmandu, Nepal", "Aarav", false, {}, {})
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            ConnectBottomNavigation(screen, {})
        }
    }

    @Composable
    private fun Feed(plans: List<Plan>, category: String = "All", onCategory: (String) -> Unit = {}, onCreate: () -> Unit = {}) {
        DiscoveryFeed(plans = plans, spots = emptyList(), groups = emptyList(), availabilities = emptyList(),
            query = "", category = category, area = "All", showPlaces = false,
            activityError = null, communityError = null, onQuery = {}, onCategory = onCategory, onArea = {},
            onTab = {}, onCreate = onCreate, onJoin = {}, onSave = {}, onChat = {}, onSpot = {}, onGroup = {},
            onAvailability = {}, onSurprise = {})
    }
}
