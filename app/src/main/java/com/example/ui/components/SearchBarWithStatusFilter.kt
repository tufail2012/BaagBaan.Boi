package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

val PAYMENT_STATUS_FILTER_OPTIONS = listOf(
    "All Records",
    "Payments Cleared",
    "Payments Pending",
    "Advance Paid"
)

/**
 * Shared SearchBar with Frosted Liquid Glass floating capsule search field
 * and active/inactive frosted glass filter chips.
 */
@Composable
fun SearchBarWithStatusFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search by farmer name, phone, serial...",
    isDark: Boolean = isAppInDarkMode(),
    testTagPrefix: String = "record_search",
    hazeState: HazeState? = LocalAppGlassHazeState.current
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isFilterActive = selectedFilter != "All Records"
    val capsuleShape = RoundedCornerShape(percent = 50)
    val filterScrollState = rememberScrollState()

    // Specification: Search field floating capsule with rgba(255, 255, 255, 0.25) tinted background and 0.3 rim
    val searchBgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                Color(0xFF0F172A).copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.20f)
            )
        )
    }
    val searchRimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.45f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.15f)
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Target Component 1: Search Field Floating Capsule
            /* CSS glassmorphism:
             * background: rgba(255, 255, 255, 0.25);
             * -webkit-backdrop-filter: blur(12px);
             * backdrop-filter: blur(12px);
             * border: 1px solid rgba(255, 255, 255, 0.3);
             * box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.05);
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = capsuleShape,
                        spotColor = Color.Black.copy(alpha = 0.05f),
                        ambientColor = Color.Black.copy(alpha = 0.02f)
                    )
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = dev.chrisbanes.haze.HazeStyle(
                                    blurRadius = 12.dp,
                                    tints = listOf(
                                        dev.chrisbanes.haze.HazeTint(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f)
                                        )
                                    ),
                                    backgroundColor = Color.Transparent
                                )
                            )
                        } else Modifier
                    )
                    .clip(capsuleShape)
                    .background(searchBgBrush, shape = capsuleShape)
                    .border(BorderStroke(1.dp, searchRimBrush), shape = capsuleShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = placeholderText,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("${testTagPrefix}_input")
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    VoiceSearchIconButton(
                        onQueryChange = onSearchQueryChange,
                        accentColor = MaterialTheme.colorScheme.primary,
                        isDark = isDark,
                        buttonSize = 30.dp,
                        iconSize = 16.dp,
                        testTag = "${testTagPrefix}_voice_search_btn"
                    )
                }
            }

            // Filter Dropdown Anchor Button - Exclusive access to payment status filters
            Box {
                val filterButtonShape = RoundedCornerShape(percent = 50)
                val filterButtonBgBrush = if (isFilterActive) {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.25f),
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.15f)
                        )
                    )
                } else {
                    searchBgBrush
                }

                /* CSS equivalent:
                 * -webkit-backdrop-filter: blur(12px);
                 * backdrop-filter: blur(12px);
                 * border: 1px solid rgba(255, 255, 255, 0.3);
                 * box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.05);
                 */
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = filterButtonShape,
                            spotColor = Color.Black.copy(alpha = 0.05f),
                            ambientColor = Color.Black.copy(alpha = 0.02f)
                        )
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeEffect(
                                    state = hazeState,
                                    style = dev.chrisbanes.haze.HazeStyle(
                                        blurRadius = 12.dp,
                                        tints = listOf(
                                            dev.chrisbanes.haze.HazeTint(
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f)
                                            )
                                        ),
                                        backgroundColor = Color.Transparent
                                    )
                                )
                            } else Modifier
                        )
                        .clip(filterButtonShape)
                        .background(filterButtonBgBrush, shape = filterButtonShape)
                        .border(
                            if (isFilterActive) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            } else {
                                BorderStroke(1.dp, searchRimBrush)
                            },
                            shape = filterButtonShape
                        )
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 12.dp)
                        .testTag("${testTagPrefix}_filter_dropdown_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Records",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)),
                            modifier = Modifier.size(18.dp)
                        )
                        if (isFilterActive) {
                            Text(
                                text = when (selectedFilter) {
                                    "Payments Cleared" -> "Cleared"
                                    "Payments Pending" -> "Pending"
                                    "Advance Paid" -> "Advance"
                                    else -> selectedFilter
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("${testTagPrefix}_filter_menu")
                ) {
                    Text(
                        text = "Filter Records",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    PAYMENT_STATUS_FILTER_OPTIONS.forEach { option ->
                        val isSelected = selectedFilter == option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onFilterSelected(option)
                                dropdownExpanded = false
                            },
                            modifier = Modifier.testTag("${testTagPrefix}_filter_option_${option.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }
    }
}
