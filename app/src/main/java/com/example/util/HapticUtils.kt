package com.example.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Attaches subtle haptic feedback whenever a user scrolls through items in a LazyListState.
 */
@Composable
fun LazyListState.rememberScrollHapticFeedback() {
    val haptic = LocalHapticFeedback.current
    val firstIndex by remember { derivedStateOf { firstVisibleItemIndex } }

    LaunchedEffect(firstIndex) {
        if (isScrollInProgress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

/**
 * Attaches subtle haptic feedback whenever a user scrolls through pixels in a ScrollState.
 */
@Composable
fun ScrollState.rememberScrollHapticFeedback(stepPx: Int = 100) {
    val haptic = LocalHapticFeedback.current
    val step by remember { derivedStateOf { value / stepPx } }

    LaunchedEffect(step) {
        if (isScrollInProgress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

/**
 * Attaches subtle haptic feedback when changing pages in a PagerState.
 */
@Composable
fun PagerState.rememberPagerHapticFeedback() {
    val haptic = LocalHapticFeedback.current
    val page by remember { derivedStateOf { currentPage } }

    LaunchedEffect(page) {
        if (isScrollInProgress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}
