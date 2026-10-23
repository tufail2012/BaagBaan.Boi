package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AppThemeMode
import com.example.ui.components.GlassRefractToggle
import com.example.ui.components.ThemeColoursDialog
import com.example.ui.theme.LiquidGlassPreference
import com.example.ui.theme.LiquidGlassSettings
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BAAGBAAN BOI", appName)
  }

  @Test
  fun `render ThemeColoursDialog and exercise LiquidGlassSettingsScreen and GlassRefractToggle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    LiquidGlassPreference.init(context)
    LiquidGlassSettings.init(context)

    composeTestRule.setContent {
      MyApplicationTheme {
        var toggleChecked by remember { mutableStateOf(false) }
        GlassRefractToggle(
          checked = toggleChecked,
          onCheckedChange = { toggleChecked = it },
          modifier = Modifier.testTag("test_refract_toggle")
        )
        ThemeColoursDialog(
          themeMode = AppThemeMode.DARK,
          selectedColorHex = "#D32F2F",
          onSelectThemeMode = {},
          onSelectColorHex = {},
          onDismissRequest = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("liquid_glass_settings_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("liquid_glass_main_screen").assertIsDisplayed()

    // Exercise inline GlassSlider on main page
    composeTestRule.onNodeWithTag("liquid_glass_blur_strength_slider").performTouchInput {
      swipeRight()
    }
    composeTestRule.waitForIdle()

    // Open Advanced page
    composeTestRule.onNodeWithTag("row_advanced_glass_settings").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("liquid_glass_advanced_screen").assertIsDisplayed()

    // Open Vibrancy value dialog and exercise slider + Done
    composeTestRule.onNodeWithTag("row_vibrancy").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("glass_value_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("glass_value_dialog_slider").performTouchInput {
      swipeRight()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("glass_value_dialog_done").performClick()
    composeTestRule.waitForIdle()

    // Open Surface tint color picker dialog and exercise Grid, Spectrum, Sliders tabs
    composeTestRule.onNodeWithTag("row_surface_tint").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("glass_color_picker_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("color_picker_tab_spectrum").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("color_picker_tab_sliders").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("color_picker_reset").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("color_picker_done").performClick()
    composeTestRule.waitForIdle()

    // Open Glass style dialog
    composeTestRule.onNodeWithTag("row_glass_style").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("glass_style_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("glass_style_option_liquid").performClick()
    composeTestRule.waitForIdle()

    // Toggle switches on Advanced page (row tap and switch tap/drag in both directions)
    val initialChromatic = LiquidGlassSettings.chromaticAberration
    composeTestRule.onNodeWithTag("row_chromatic_aberration").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(!initialChromatic, LiquidGlassSettings.chromaticAberration)
    composeTestRule.onNodeWithTag("row_chromatic_aberration").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(initialChromatic, LiquidGlassSettings.chromaticAberration)

    composeTestRule.onNodeWithTag("row_adaptive_contrast").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("row_text_and_icon_color").performScrollTo().assertIsDisplayed()
  }
}
