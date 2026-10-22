package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.example.ui.theme.CustomBackgroundPreference
import com.example.ui.theme.LiquidGlassConfig
import com.example.ui.theme.LiquidGlassSettings
import com.example.ui.theme.LocalLiquidGlassConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PreviewNavItem(
    val title: String,
    val icon: ImageVector,
)

private val PreviewNavItems = listOf(
    PreviewNavItem("Local", Icons.Outlined.LocalFlorist),
    PreviewNavItem("Imported", Icons.Default.LocalShipping),
    PreviewNavItem("Rootstocks", Icons.Default.Spa),
    PreviewNavItem("Site Visit", Icons.Outlined.Assignment),
)

private val PreviewGradientColors = listOf(
    Color(0xFFFF2D55),
    Color(0xFF5856D6),
    Color(0xFF32ADE6),
    Color(0xFF34C759),
)

/**
 * Live 190dp preview card for Liquid Glass settings.
 *
 * Background is recorded by a layer backdrop; the bottom tab pill and search circle
 * are siblings of that background layer inside the outer Box so they refract it in real time.
 */
@Composable
fun LiquidGlassPreview(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    config: LiquidGlassConfig = LiquidGlassSettings.resolveConfig(isDark),
) {
    val context = LocalContext.current
    val customVersion = if (isDark) {
        CustomBackgroundPreference.darkVersion
    } else {
        CustomBackgroundPreference.lightVersion
    }

    var customBitmap by remember(customVersion, isDark) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(customVersion, isDark) {
        customBitmap = if (customVersion > 0L) {
            withContext(Dispatchers.IO) {
                val file = if (isDark) {
                    CustomBackgroundPreference.darkFile(context)
                } else {
                    CustomBackgroundPreference.lightFile(context)
                }
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }
        } else {
            null
        }
    }

    val previewBackdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()
    val pillShape = RoundedCornerShape(percent = 50)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .testTag("liquid_glass_preview"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val panelWidth = maxWidth.coerceAtLeast(1.dp)

        // Recorded background layer (sibling of pill and search circle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
        ) {
            val bitmap = customBitmap
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(panelWidth * 1.8f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(panelWidth * 1.8f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = PreviewGradientColors,
                                    start = Offset.Zero,
                                    end = Offset.Infinite,
                                )
                            )
                    )
                }
            }
        }

        // Foreground sibling: pill row (85% width, 16dp bottom padding, IntrinsicSize.Max height, 8dp gap)
        CompositionLocalProvider(LocalLiquidGlassConfig provides config) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 16.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tab pill: glass modifier from LiquidGlassConfig, weight 1, 4dp padding, tabs spaced evenly
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .navLiquidGlass(shape = pillShape, backdrop = previewBackdrop)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewNavItems.take(4).forEachIndexed { index, item ->
                        val isSelected = index == 0
                        val itemTint = if (isSelected) config.glassTextColor else Color.White

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .clip(pillShape)
                                            .background(
                                                color = config.selectionPillColor.copy(
                                                    alpha = config.selectionPillOpacity.coerceIn(0f, 1f)
                                                ),
                                                shape = pillShape,
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = itemTint,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                color = itemTint,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Search circle: same height as the pill, square, glass modifier from LiquidGlassConfig
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .navLiquidGlass(shape = pillShape, backdrop = previewBackdrop),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = config.glassTextColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
