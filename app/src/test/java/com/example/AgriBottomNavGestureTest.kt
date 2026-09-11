package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.ui.components.AgriBottomNav
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalHazeMaterialsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgriBottomNavGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTapSwitchesCategory() {
        var selectedCategory by mutableStateOf("Local Plants")

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    hazeState = hazeState
                )
            }
        }

        composeTestRule.onNodeWithTag("nav_site_visit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_site_visit").performClick()

        assertEquals("Site Visit", selectedCategory)
    }

    @Test
    fun testDragSwitchesCategory() {
        var selectedCategory by mutableStateOf("Local Plants")

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    hazeState = hazeState
                )
            }
        }

        // Drag right from nav_local across to other tabs
        composeTestRule.onNodeWithTag("nav_local").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(250f, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        assert(selectedCategory != "Local Plants")
    }

    @Test
    fun testDragLeftSwitchesCategory() {
        var selectedCategory by mutableStateOf("Garden Planning")

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    hazeState = hazeState
                )
            }
        }

        // Drag left from right-most tab
        composeTestRule.onNodeWithTag("nav_garden_planning").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-300f, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        assert(selectedCategory != "Garden Planning")
    }

    @Test
    fun testDragClampsAtBounds() {
        var selectedCategory by mutableStateOf("Local Plants")

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxSize()) {
                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    hazeState = hazeState
                )
            }
        }

        // Drag far beyond right bound (e.g. 5000px)
        composeTestRule.onNodeWithTag("nav_local").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(5000f, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        // Should snap to last tab without crash or overflow
        assertEquals("Garden Planning", selectedCategory)
    }
}
