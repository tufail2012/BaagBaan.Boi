package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import com.example.util.MessagePreviewComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessagePreviewCollapsibleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMessagePreviewIsCollapsedByDefaultAndExpandsOnTap() {
        val testMessage = "Dear Farmer John, your booking SN-1234 for 500 Gala plants is confirmed."

        composeTestRule.setContent {
            MyApplicationTheme {
                MessagePreviewComponent(
                    selectedTemplate = "Booking Confirmation",
                    onSelectTemplate = {},
                    generatedMessage = testMessage,
                    isDark = false
                )
            }
        }

        // 1. Initial State: Header must be visible, but preview content must NOT be displayed
        composeTestRule.onNodeWithText("Message Preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_preview_header").assertIsDisplayed()
        composeTestRule.onNodeWithText(testMessage).assertDoesNotExist()
        composeTestRule.onNodeWithTag("preview_message_text").assertDoesNotExist()

        // 2. Tap Header to expand
        composeTestRule.onNodeWithTag("message_preview_header").performClick()
        composeTestRule.waitForIdle()

        // 3. Expanded State: Full preview content, template selection, and copy button must be visible
        composeTestRule.onNodeWithText(testMessage).assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_message_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_template_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("copy_preview_button").assertIsDisplayed()

        // 4. Tap Header again to collapse
        composeTestRule.onNodeWithTag("message_preview_header").performClick()
        composeTestRule.waitForIdle()

        // 5. Collapsed State again: Content is hidden
        composeTestRule.onNodeWithText(testMessage).assertDoesNotExist()
        composeTestRule.onNodeWithTag("preview_message_text").assertDoesNotExist()
    }
}
