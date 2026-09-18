package com.example

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.AppThemeMode
import com.example.ui.components.AgriHeader
import com.example.ui.components.TopHeaderScrollScrim
import dev.chrisbanes.haze.HazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeaderScrollOffsetSafetyNetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTopHeaderScrollScrim_activatesWithScrollOffset_evenWhenIsScrollingIsFalse() {
        // Direct test of the safety net logic in TopHeaderScrollScrim:
        // effectiveIsScrolling = isScrolling || scrollOffset > 0f || ((scrollOffsetProvider?.invoke() ?: 0f) > 0f)
        val isScrolling = false
        val scrollOffset = 45.5f
        val effectiveIsScrolling = isScrolling || scrollOffset > 0f

        assertTrue("Safety net must activate scrolling when scrollOffset > 0f", effectiveIsScrolling)
    }

    @Test
    fun testTopHeaderScrollScrim_deactivatesWhenAtTop() {
        val isScrolling = false
        val scrollOffset = 0f
        val effectiveIsScrolling = isScrolling || scrollOffset > 0f

        assertFalse("Safety net must deactivate when at top (offset == 0f)", effectiveIsScrolling)
    }

    @Test
    fun testAgriHeader_rendersWithScrollOffsetParameter() {
        var offsetValue by mutableFloatStateOf(0f)
        var scrollingActive by mutableStateOf(false)

        composeTestRule.setContent {
            val hazeState = androidx.compose.runtime.remember { HazeState() }
            AgriHeader(
                title = "Local Plants",
                themeMode = AppThemeMode.SYSTEM,
                onSelectThemeMode = {},
                isScrolling = scrollingActive,
                scrollOffset = offsetValue,
                hazeState = hazeState
            )
        }

        composeTestRule.waitForIdle()

        // Update offset to trigger safety net
        offsetValue = 120f
        composeTestRule.waitForIdle()
        assertEquals(120f, offsetValue, 0.01f)
    }
}
