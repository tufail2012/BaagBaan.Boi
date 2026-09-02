package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.AppThemeMode
import com.example.ui.components.AgriHeader
import com.example.ui.components.AgriSegmentedControl
import com.example.ui.components.PruningSubTabs
import com.example.ui.components.RootstockSubTabs
import com.example.ui.theme.MyApplicationTheme
import dev.chrisbanes.haze.HazeState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { Text("BAAGBAAN BOI") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun local_plants_header_light_mode_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.LIGHT) {
        val hazeState = HazeState()
        AgriHeader(
          title = "Local Plants",
          themeMode = AppThemeMode.LIGHT,
          onSelectThemeMode = {},
          hazeState = hazeState
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/local_plants_header_light.png")
  }

  @Test
  fun local_plants_header_dark_mode_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DARK) {
        val hazeState = HazeState()
        AgriHeader(
          title = "Local Plants",
          themeMode = AppThemeMode.DARK,
          onSelectThemeMode = {},
          hazeState = hazeState
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/local_plants_header_dark.png")
  }

  @Test
  fun pruning_subtabs_light_mode_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.LIGHT) {
        val hazeState = HazeState()
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
          Column {
            AgriSegmentedControl(
              selectedMode = 0,
              onModeSelected = {},
              hazeState = hazeState,
              recordsLabel = "Records (12)"
            )
            PruningSubTabs(
              selectedSubTab = "Summer Pruning",
              onSelectSubTab = {}
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/pruning_subtabs_glass_light.png")
  }

  @Test
  fun pruning_subtabs_dark_mode_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DARK) {
        val hazeState = HazeState()
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
          Column {
            AgriSegmentedControl(
              selectedMode = 0,
              onModeSelected = {},
              hazeState = hazeState,
              recordsLabel = "Records (12)"
            )
            PruningSubTabs(
              selectedSubTab = "Summer Pruning",
              onSelectSubTab = {}
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/pruning_subtabs_glass_dark.png")
  }

  @Test
  fun rootstock_subtabs_glass_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.LIGHT) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
          RootstockSubTabs(
            selectedSubTab = "M9-T337",
            selectedGenevaOption = null,
            onSelectSubTab = { _, _ -> }
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/rootstock_subtabs_glass_light.png")
  }
}
