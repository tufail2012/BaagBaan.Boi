package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import org.junit.Assert.assertTrue
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

    @Test
    fun testBidirectionalSync_DragPillUpdatesPagerState() {
        val pages = listOf("Local Plants", "Imported", "Rootstocks", "Site Visit", "Pruning", "Garden Planning")
        var selectedCategory by mutableStateOf("Local Plants")
        var recordedPage by mutableStateOf(0)

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

            LaunchedEffect(pagerState.currentPage) {
                recordedPage = pagerState.currentPage
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val coroutineScope = rememberCoroutineScope()
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Text(text = "Page: ${pages[page]}")
                }

                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        val targetIndex = pages.indexOfFirst { it.equals(category, ignoreCase = true) }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    },
                    hazeState = hazeState,
                    pagerState = pagerState
                )
            }
        }

        // Drag pill from nav_local to the right
        composeTestRule.onNodeWithTag("nav_local").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(250f, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        // Pager should have moved from page 0 and selectedCategory should be updated
        assertTrue("Expected pager to switch away from page 0", recordedPage > 0)
        assertTrue("Expected category to switch away from Local Plants", selectedCategory != "Local Plants")
    }

    @Test
    fun testBidirectionalSync_TapPillScrollsPager() {
        val pages = listOf("Local Plants", "Imported", "Rootstocks", "Site Visit", "Pruning", "Garden Planning")
        var selectedCategory by mutableStateOf("Local Plants")
        var recordedPage by mutableStateOf(0)

        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

            LaunchedEffect(pagerState.currentPage) {
                recordedPage = pagerState.currentPage
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val coroutineScope = rememberCoroutineScope()
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Text(text = "Page: ${pages[page]}")
                }

                AgriBottomNav(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        val targetIndex = pages.indexOfFirst { it.equals(category, ignoreCase = true) }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    },
                    hazeState = hazeState,
                    pagerState = pagerState
                )
            }
        }

        composeTestRule.onNodeWithTag("nav_site_visit").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Site Visit", selectedCategory)
        assertEquals(3, recordedPage)
    }
}
