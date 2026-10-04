package com.example.chat.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat.model.ChatSubScreen
import com.example.chat.model.ChatTab
import com.example.ui.animation.IosMotion
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.example.ui.theme.getAppDimBackgroundBrush
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun ChatSectionScreen(
    onBack: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val currentUser by chatViewModel.currentUser.collectAsState()
    val isUserLoading by chatViewModel.isUserLoading.collectAsState()
    val isUsernameSetupRequired by chatViewModel.isUsernameSetupRequired.collectAsState()
    val activeTab by chatViewModel.activeTab.collectAsState()
    val activeSubScreen by chatViewModel.activeSubScreen.collectAsState()
    val toastMessage by chatViewModel.toastMessage.collectAsState()
    val inAppBanner by chatViewModel.inAppBanner.collectAsState()

    // Handle toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearToast()
        }
    }

    // Auto dismiss in-app message banner after 4.5 seconds
    LaunchedEffect(inAppBanner) {
        if (inAppBanner != null) {
            delay(4500)
            chatViewModel.dismissInAppBanner()
        }
    }

    // Hardware/gesture Back handling
    BackHandler {
        when {
            activeSubScreen == ChatSubScreen.CONVERSATION -> chatViewModel.navigateBackFromConversation()
            activeSubScreen == ChatSubScreen.SEARCH -> chatViewModel.navigateBackFromSearch()
            else -> onBack()
        }
    }

    val rootBgBrush = remember(isDark, isAmoled, accentColor) {
        getAppDimBackgroundBrush(accentColor, isDark = isDark, isAmoled = isAmoled)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rootBgBrush)
    ) {
        if (isUserLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (isUsernameSetupRequired) {
            // One-time username setup flow
            UsernameSetupScreen(
                viewModel = chatViewModel,
                onBack = onBack,
                accentColor = accentColor
            )
        } else {
            // Main Chat Experience with its OWN Scaffold & Bottom Nav
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    // Only show bottom navigation on main tabs, not inside active search or conversation
                    if (activeSubScreen == ChatSubScreen.TABS) {
                        ChatBottomNav(
                            selectedTab = activeTab,
                            onTabSelected = { chatViewModel.selectTab(it) },
                            accentColor = accentColor,
                            hazeState = hazeState,
                            backdrop = backdrop
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (activeSubScreen) {
                        ChatSubScreen.SEARCH -> {
                            UserSearchScreen(
                                viewModel = chatViewModel,
                                onBack = { chatViewModel.navigateBackFromSearch() },
                                accentColor = accentColor
                            )
                        }

                        ChatSubScreen.CONVERSATION -> {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onBack = { chatViewModel.navigateBackFromConversation() },
                                accentColor = accentColor
                            )
                        }

                        ChatSubScreen.TABS -> {
                            AnimatedContent(
                                targetState = activeTab,
                                transitionSpec = {
                                    val isReduced = IosMotion.isReducedMotion(context)
                                    IosMotion.tabCrossfade(isReduced)
                                },
                                label = "ChatTabTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { currentTab ->
                                when (currentTab) {
                                    ChatTab.CHATS -> {
                                        ChatsListScreen(
                                            viewModel = chatViewModel,
                                            onBackToApp = onBack,
                                            accentColor = accentColor
                                        )
                                    }

                                    ChatTab.CALLS -> {
                                        CallsTabScreen(
                                            viewModel = chatViewModel,
                                            onBackToApp = onBack,
                                            accentColor = accentColor
                                        )
                                    }

                                    ChatTab.GROUPS -> {
                                        ChatPlaceholderTabScreen(
                                            title = "Groups",
                                            subtitle = "Join regional grower associations and crop consultation groups to discuss techniques and market trends.",
                                            icon = Icons.Default.Groups,
                                            onBack = onBack,
                                            accentColor = accentColor
                                        )
                                    }

                                    ChatTab.SETTINGS -> {
                                        ChatSettingsScreen(
                                            viewModel = chatViewModel,
                                            onBackToApp = onBack,
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Foreground In-App Notification Banner Toast
        AnimatedVisibility(
            visible = inAppBanner != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(50f)
        ) {
            inAppBanner?.let { (sender, text) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            chatViewModel.dismissInAppBanner()
                            // Clicking takes user to Chats list
                            chatViewModel.selectTab(ChatTab.CHATS)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sender,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { chatViewModel.dismissInAppBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
