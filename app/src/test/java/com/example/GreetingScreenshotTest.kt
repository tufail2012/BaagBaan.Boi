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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.components.glassCardBackground
import com.example.ui.components.isAppInDarkMode

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

  @Test
  fun booking_varieties_expanded_dark_mode_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = AppThemeMode.DARK) {
        val isDark = isAppInDarkMode()
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0E13))
            .padding(16.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0).copy(alpha = 0.8f)),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(if (isDark) Color(0xFF171517) else Color(0xFFF8FAFC).copy(alpha = 0.65f))
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Booking Varieties (2)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                TextButton(
                  onClick = { },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Switch to Single", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
              }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .glassCardBackground(
                    accentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                  )
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Item #1",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                      onClick = { },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Variety Line",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/booking_varieties_expanded_dark.png")
  }
}
