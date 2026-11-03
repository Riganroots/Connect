package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.models.Plan
import com.example.ui.components.PlanCard
import com.example.ui.screens.DiscoveryFeed
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
        compose.onNodeWithText(walk.title).assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/design/discovery-360.png")
        compose.onNodeWithTag("feed_category_Play").performClick()
        compose.runOnIdle { assertEquals("Play", selected) }
        compose.onNodeWithTag("feed_category_Play").assertIsSelected()
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
        compose.onNodeWithText("Join this plan").performScrollTo().assertIsDisplayed()
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

    @Composable
    private fun Feed(plans: List<Plan>, category: String = "All", onCategory: (String) -> Unit = {}, onCreate: () -> Unit = {}) {
        DiscoveryFeed(plans = plans, spots = emptyList(), groups = emptyList(), availabilities = emptyList(),
            query = "", category = category, area = "All", showPlaces = false,
            activityError = null, communityError = null, onQuery = {}, onCategory = onCategory, onArea = {},
            onTab = {}, onCreate = onCreate, onJoin = {}, onSave = {}, onChat = {}, onSpot = {}, onGroup = {},
            onAvailability = {}, onSurprise = {})
    }
}
