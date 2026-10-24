package com.example.chat.ui


import com.example.ui.components.ThemedIcon
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat.data.ChatList
import com.example.chat.model.ChatSubScreen
import com.example.chat.model.ChatTab
import com.example.chat.model.ParticipantInfo
import com.example.chat.model.UserStatusGroup
import com.example.ui.components.LocalMorphMenuBackdrop
import com.example.ui.components.MorphMenu
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import kotlin.math.roundToInt
import com.example.ui.theme.getAppDimBackgroundBrush
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatSectionScreen(
    onBack: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val themePrimary = MaterialTheme.colorScheme.primary
    val effectiveAccentColor = remember(accentColor, themePrimary, isDark, isAmoled) {
        val candidate = if ((isDark || isAmoled) && accentColor.luminance() < 0.15f && themePrimary.luminance() >= 0.15f) {
            themePrimary
        } else if (!isDark && !isAmoled && accentColor.luminance() > 0.85f && themePrimary.luminance() <= 0.85f) {
            themePrimary
        } else {
            accentColor
        }
        if ((isDark || isAmoled) && candidate.luminance() < 0.15f) {
            Color.White
        } else if (!isDark && !isAmoled && candidate.luminance() > 0.85f) {
            Color.Black
        } else {
            candidate
        }
    }

    val currentUser by chatViewModel.currentUser.collectAsState()
    val currentUid = currentUser?.uid ?: ""
    val allChats by chatViewModel.allChatsList.collectAsState()
    val chats by chatViewModel.chatsList.collectAsState()
    val archivedChats by chatViewModel.archivedChatsList.collectAsState()
    val lockedChats by chatViewModel.lockedChatsList.collectAsState()
    val lockedChatsUnlocked by chatViewModel.lockedChatsUnlocked.collectAsState()
    val isUserLoading by chatViewModel.isUserLoading.collectAsState()
    val isChatsLoading by chatViewModel.isChatsLoading.collectAsState()
    val isUsernameSetupRequired by chatViewModel.isUsernameSetupRequired.collectAsState()
    val activeTab by chatViewModel.activeTab.collectAsState()
    val activeSubScreen by chatViewModel.activeSubScreen.collectAsState()
    val toastMessage by chatViewModel.toastMessage.collectAsState()
    val inAppBanner by chatViewModel.inAppBanner.collectAsState()
    val pendingOpenChatId by chatViewModel.pendingOpenChatId.collectAsState()

    val chatLocalStates by chatViewModel.chatLocalStates.collectAsState()
    val myStatuses by chatViewModel.myStatuses.collectAsState()
    val contactStatusGroups by chatViewModel.contactStatusGroups.collectAsState()
    val nicknames by chatViewModel.nicknames.collectAsState()
    var chatsSelectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var archivedSelectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var selectionMenuAnchorRect by remember { mutableStateOf(Rect.Zero) }
    var showListSheet by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<ChatList?>(null) }
    var settingsPrivacyOpen by remember { mutableStateOf(false) }
    var showStatusComposer by remember { mutableStateOf(false) }
    var viewingStatusUserId by remember { mutableStateOf<String?>(null) }
    var showCreateGroup by remember { mutableStateOf(false) }

    var chatsStripHeightPx by remember { mutableIntStateOf(0) }
    var callsHeaderHeightPx by remember { mutableIntStateOf(0) }
    var groupsHeaderHeightPx by remember { mutableIntStateOf(0) }
    var updatesHeaderHeightPx by remember { mutableIntStateOf(0) }
    var archivedHeaderHeightPx by remember { mutableIntStateOf(0) }
    var settingsPrivacyHeaderHeightPx by remember { mutableIntStateOf(0) }

    val chatsStripHeightDp = if (chatsStripHeightPx > 0) with(density) { chatsStripHeightPx.toDp() } else 210.dp
    val callsHeaderHeightDp = if (callsHeaderHeightPx > 0) with(density) { callsHeaderHeightPx.toDp() } else 76.dp
    val groupsHeaderHeightDp = if (groupsHeaderHeightPx > 0) with(density) { groupsHeaderHeightPx.toDp() } else 76.dp
    val updatesHeaderHeightDp = if (updatesHeaderHeightPx > 0) with(density) { updatesHeaderHeightPx.toDp() } else 76.dp
    val archivedHeaderHeightDp = if (archivedHeaderHeightPx > 0) with(density) { archivedHeaderHeightPx.toDp() } else 76.dp
    val settingsPrivacyHeaderHeightDp = if (settingsPrivacyHeaderHeightPx > 0) with(density) { settingsPrivacyHeaderHeightPx.toDp() } else 76.dp

    LaunchedEffect(pendingOpenChatId, chats, allChats, isUserLoading, isChatsLoading) {
        if (pendingOpenChatId != null && !isUserLoading && !isChatsLoading) {
            chatViewModel.tryOpenPendingChat()
        }
    }

    LaunchedEffect(lockedChatsUnlocked) {
        if (chatsSelectedChatIds.isNotEmpty()) {
            chatsSelectedChatIds = emptySet()
        }
        if (showSelectionMenu) {
            showSelectionMenu = false
        }
    }

    // Keep selections clean when chat lists update
    LaunchedEffect(chats, lockedChats, lockedChatsUnlocked) {
        if (chatsSelectedChatIds.isNotEmpty()) {
            val activeList = if (lockedChatsUnlocked) lockedChats else chats
            val visibleIds = activeList.map { it.chatId }.toSet()
            val remaining = chatsSelectedChatIds.intersect(visibleIds)
            if (remaining.size != chatsSelectedChatIds.size) {
                chatsSelectedChatIds = remaining
            }
        }
    }

    LaunchedEffect(archivedChats) {
        if (archivedSelectedChatIds.isNotEmpty()) {
            val visibleIds = archivedChats.map { it.chatId }.toSet()
            val remaining = archivedSelectedChatIds.intersect(visibleIds)
            if (remaining.size != archivedSelectedChatIds.size) {
                archivedSelectedChatIds = remaining
            }
        }
    }

    val rootBgBrush = remember(isDark, isAmoled, accentColor) {
        getAppDimBackgroundBrush(accentColor, isDark = isDark, isAmoled = isAmoled)
    }

    val chatHazeState = remember { HazeState() }
    val windowBackground = MaterialTheme.colorScheme.surface
    val paintBackdrop: ContentDrawScope.() -> Unit =
        remember(windowBackground, rootBgBrush) {
            {
                drawRect(windowBackground)
                drawRect(rootBgBrush)
                drawContent()
            }
        }
    val chatBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)
    val chatNavBackdrop = remember(chatBackdrop) { chatBackdrop.asNavBackdrop() }

    val chatTabs = remember {
        listOf(
            ChatTab.CHATS,
            ChatTab.CALLS,
            ChatTab.GROUPS,
            ChatTab.UPDATES
        )
    }

    val initialTabIndex = remember {
        val idx = chatTabs.indexOf(activeTab)
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialTabIndex,
        pageCount = { chatTabs.size }
    )

    var animatingNavTargetPage by remember { mutableIntStateOf(-1) }
    var navAnimationJob by remember { mutableStateOf<Job?>(null) }

    // Synchronize tab when user finishes swiping to a new page
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && animatingNavTargetPage == -1 && pagerState.settledPage in chatTabs.indices) {
            val targetTab = chatTabs[pagerState.settledPage]
            if (activeTab != targetTab) {
                chatViewModel.selectTab(targetTab)
            }
        }
    }

    // Synchronize pager when tab is selected from outside (e.g. in-app notification banner)
    LaunchedEffect(activeTab) {
        val targetIndex = chatTabs.indexOf(activeTab)
        if (targetIndex >= 0 &&
            targetIndex != pagerState.currentPage &&
            targetIndex != pagerState.targetPage &&
            targetIndex != animatingNavTargetPage
        ) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Dismiss overlays if the user leaves the relevant tab or sub-screen
    LaunchedEffect(activeTab, activeSubScreen) {
        if (showListSheet && (activeTab != ChatTab.CHATS || activeSubScreen != ChatSubScreen.TABS)) {
            showListSheet = false
            editingList = null
        }
        if (showSelectionMenu && (activeTab != ChatTab.CHATS || activeSubScreen != ChatSubScreen.TABS)) {
            showSelectionMenu = false
        }
        if (activeSubScreen != ChatSubScreen.SETTINGS && settingsPrivacyOpen) {
            settingsPrivacyOpen = false
        }
        if (activeTab != ChatTab.UPDATES || activeSubScreen != ChatSubScreen.TABS) {
            showStatusComposer = false
            viewingStatusUserId = null
        }
        if (activeTab != ChatTab.GROUPS || activeSubScreen != ChatSubScreen.TABS) {
            showCreateGroup = false
        }
    }

    LaunchedEffect(chatsSelectedChatIds) {
        if (chatsSelectedChatIds.isEmpty() && showSelectionMenu) {
            showSelectionMenu = false
        }
    }

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
            showCreateGroup -> {
                showCreateGroup = false
            }
            viewingStatusUserId != null -> {
                viewingStatusUserId = null
            }
            showStatusComposer -> {
                showStatusComposer = false
            }
            showSelectionMenu -> {
                showSelectionMenu = false
            }
            showListSheet -> {
                showListSheet = false
                editingList = null
            }
            activeSubScreen == ChatSubScreen.CONVERSATION -> chatViewModel.navigateBackFromConversation()
            activeSubScreen == ChatSubScreen.SEARCH -> chatViewModel.navigateBackFromSearch()
            activeSubScreen == ChatSubScreen.ARCHIVED && archivedSelectedChatIds.isNotEmpty() -> {
                archivedSelectedChatIds = emptySet()
            }
            activeSubScreen == ChatSubScreen.ARCHIVED -> chatViewModel.navigateBackFromArchived()
            activeSubScreen == ChatSubScreen.SETTINGS && settingsPrivacyOpen -> {
                settingsPrivacyOpen = false
            }
            activeSubScreen == ChatSubScreen.SETTINGS -> {
                chatViewModel.navigateBackFromSettings()
            }
            activeSubScreen == ChatSubScreen.TABS && activeTab == ChatTab.CHATS && chatsSelectedChatIds.isNotEmpty() -> {
                chatsSelectedChatIds = emptySet()
            }
            activeSubScreen == ChatSubScreen.TABS && activeTab == ChatTab.CHATS && lockedChatsUnlocked -> {
                chatViewModel.setLockedChatsUnlocked(false)
            }
            else -> onBack()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.ui.components.LocalAppGlassHazeState provides chatHazeState
    ) {
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

                ChatSubScreen.ARCHIVED,
                ChatSubScreen.SETTINGS,
                ChatSubScreen.TABS -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Layer box containing ONLY scrollable page content (lists, empty states, backgrounds)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = chatHazeState)
                                .layerBackdrop(chatBackdrop)
                                .background(rootBgBrush)
                        ) {
                            when (activeSubScreen) {
                                ChatSubScreen.ARCHIVED -> {
                                    ArchivedChatsScreen(
                                        viewModel = chatViewModel,
                                        accentColor = effectiveAccentColor,
                                        selectedChatIds = archivedSelectedChatIds,
                                        onUpdateSelection = { archivedSelectedChatIds = it },
                                        topPadding = archivedHeaderHeightDp
                                    )
                                }

                                ChatSubScreen.SETTINGS -> {
                                    ChatSettingsScreen(
                                        viewModel = chatViewModel,
                                        onBackToApp = {
                                            settingsPrivacyOpen = false
                                            chatViewModel.navigateBackFromSettings()
                                        },
                                        accentColor = effectiveAccentColor,
                                        privacyOpen = settingsPrivacyOpen,
                                        onPrivacyOpenChange = { settingsPrivacyOpen = it },
                                        privacyTopPadding = settingsPrivacyHeaderHeightDp
                                    )
                                }

                                else -> {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        userScrollEnabled = !showListSheet && !lockedChatsUnlocked && !showStatusComposer && viewingStatusUserId == null && !showCreateGroup,
                                        beyondViewportPageCount = 1,
                                        key = { chatTabs.getOrElse(it) { ChatTab.CHATS }.name }
                                    ) { page ->
                                        when (chatTabs.getOrElse(page) { ChatTab.CHATS }) {
                                            ChatTab.CHATS -> {
                                                ChatsListScreen(
                                                    viewModel = chatViewModel,
                                                    accentColor = effectiveAccentColor,
                                                    selectedChatIds = chatsSelectedChatIds,
                                                    onUpdateSelection = { chatsSelectedChatIds = it },
                                                    stripHeightDp = chatsStripHeightDp
                                                )
                                            }

                                            ChatTab.CALLS -> {
                                                CallsTabScreen(
                                                    viewModel = chatViewModel,
                                                    topPadding = callsHeaderHeightDp
                                                )
                                            }

                                            ChatTab.GROUPS -> {
                                                GroupsTabScreen(
                                                    viewModel = chatViewModel,
                                                    accentColor = effectiveAccentColor,
                                                    topPadding = groupsHeaderHeightDp,
                                                    onOpenCreateGroup = { showCreateGroup = true }
                                                )
                                            }

                                            ChatTab.UPDATES -> {
                                                UpdatesTabScreen(
                                                    viewModel = chatViewModel,
                                                    accentColor = effectiveAccentColor,
                                                    topPadding = updatesHeaderHeightDp,
                                                    onOpenComposer = { showStatusComposer = true },
                                                    onOpenViewer = { group -> viewingStatusUserId = group.userId }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Sibling glass header overlays and floating buttons (outside layerBackdrop)
                        if (activeSubScreen == ChatSubScreen.ARCHIVED) {
                            ArchivedChatsTopHeaderOverlay(
                                viewModel = chatViewModel,
                                onBack = { chatViewModel.navigateBackFromArchived() },
                                backdrop = chatBackdrop,
                                selectedChatIds = archivedSelectedChatIds,
                                onUpdateSelection = { archivedSelectedChatIds = it },
                                onClearSelection = { archivedSelectedChatIds = emptySet() },
                                onHeaderHeightChanged = { archivedHeaderHeightPx = it },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .zIndex(10f)
                            )
                        } else if (activeSubScreen == ChatSubScreen.SETTINGS) {
                            if (settingsPrivacyOpen) {
                                PrivacyTopHeaderOverlay(
                                    onBack = { settingsPrivacyOpen = false },
                                    backdrop = chatBackdrop,
                                    onHeaderHeightChanged = { settingsPrivacyHeaderHeightPx = it },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .zIndex(10f)
                                )
                            }
                        } else if (activeSubScreen == ChatSubScreen.TABS) {
                            when (activeTab) {
                                ChatTab.CHATS -> {
                                    ChatsTopHeaderOverlay(
                                        viewModel = chatViewModel,
                                        accentColor = effectiveAccentColor,
                                        backdrop = chatBackdrop,
                                        selectedChatIds = chatsSelectedChatIds,
                                        onUpdateSelection = { chatsSelectedChatIds = it },
                                        onClearSelection = {
                                            showSelectionMenu = false
                                            chatsSelectedChatIds = emptySet()
                                        },
                                        onOpenSelectionMenu = { anchorRect ->
                                            selectionMenuAnchorRect = anchorRect
                                            showSelectionMenu = !showSelectionMenu
                                        },
                                        onCloseSelectionMenu = {
                                            showSelectionMenu = false
                                        },
                                        onAddListClick = {
                                            editingList = null
                                            showListSheet = true
                                        },
                                        onEditCustomList = { list ->
                                            editingList = list
                                            showListSheet = true
                                        },
                                        onStripHeightChanged = { chatsStripHeightPx = it },
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .zIndex(10f)
                                    )
                                }

                                ChatTab.CALLS -> {
                                    CallsTopHeaderOverlay(
                                        viewModel = chatViewModel,
                                        backdrop = chatBackdrop,
                                        onHeaderHeightChanged = { callsHeaderHeightPx = it },
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .zIndex(10f)
                                    )
                                    CallsFloatingCallButton(
                                        onClick = { chatViewModel.openSearch() },
                                        backdrop = chatBackdrop,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .zIndex(10f)
                                    )
                                }

                                ChatTab.UPDATES -> {
                                    UpdatesTopHeaderOverlay(
                                        viewModel = chatViewModel,
                                        backdrop = chatBackdrop,
                                        onHeaderHeightChanged = { updatesHeaderHeightPx = it },
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .zIndex(10f)
                                    )
                                    if (!showStatusComposer && viewingStatusUserId == null) {
                                        UpdatesFloatingStatusButton(
                                            onClick = { showStatusComposer = true },
                                            backdrop = chatBackdrop,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .zIndex(10f)
                                        )
                                    }
                                }

                                ChatTab.GROUPS -> {
                                    GroupsTopHeaderOverlay(
                                        viewModel = chatViewModel,
                                        backdrop = chatBackdrop,
                                        onHeaderHeightChanged = { groupsHeaderHeightPx = it },
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .zIndex(10f)
                                    )
                                    if (!showCreateGroup) {
                                        GroupsFloatingCreateButton(
                                            onClick = { showCreateGroup = true },
                                            backdrop = chatBackdrop,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .zIndex(10f)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Sibling floating bottom navigation (hidden while New/Edit list sheet, Locked chats view, Status composer/viewer, or Create Group is open)
                        if (activeSubScreen == ChatSubScreen.TABS &&
                            !showListSheet &&
                            !(activeTab == ChatTab.CHATS && lockedChatsUnlocked) &&
                            !showStatusComposer &&
                            viewingStatusUserId == null &&
                            !showCreateGroup
                        ) {
                            ChatBottomNav(
                                selectedTab = activeTab,
                                onTabSelected = { tab ->
                                    val targetIndex = chatTabs.indexOf(tab)
                                    chatViewModel.selectTab(tab)
                                    if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                                        animatingNavTargetPage = targetIndex
                                        navAnimationJob?.cancel()
                                        navAnimationJob = coroutineScope.launch {
                                            try {
                                                pagerState.scrollToPage(targetIndex)
                                            } finally {
                                                if (animatingNavTargetPage == targetIndex) {
                                                    animatingNavTargetPage = -1
                                                }
                                            }
                                        }
                                    }
                                },
                                pagerState = pagerState,
                                glassBackdrop = chatNavBackdrop,
                                hazeState = chatHazeState,
                                accentColor = effectiveAccentColor,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(10f)
                            )
                        }

                        // 4. Sibling selection overflow glass menu overlay (outside layerBackdrop)
                        val anyUnreadSelected = remember(chatsSelectedChatIds, chatLocalStates) {
                            chatsSelectedChatIds.any { chatLocalStates[it]?.markedUnread == true }
                        }
                        val allFavouriteSelected = remember(chatsSelectedChatIds, chatLocalStates) {
                            chatsSelectedChatIds.isNotEmpty() &&
                                chatsSelectedChatIds.all { chatLocalStates[it]?.favourite == true }
                        }
                        val allLockedSelected = remember(chatsSelectedChatIds, chatLocalStates) {
                            chatsSelectedChatIds.isNotEmpty() &&
                                chatsSelectedChatIds.all { chatLocalStates[it]?.locked == true }
                        }
                        val allSelectableIds = remember(chats, lockedChats, lockedChatsUnlocked) {
                            val activeList = if (lockedChatsUnlocked) lockedChats else chats
                            activeList.map { it.chatId }.toSet()
                        }
                        ChatsSelectionOverflowMenuOverlay(
                            visible = showSelectionMenu,
                            anchorRect = selectionMenuAnchorRect,
                            backdrop = chatBackdrop,
                            selectedChatIds = chatsSelectedChatIds,
                            anyUnread = anyUnreadSelected,
                            allFavourite = allFavouriteSelected,
                            allLocked = allLockedSelected,
                            allSelectableChatIds = allSelectableIds,
                            contentColor = glassContentColor(),
                            viewModel = chatViewModel,
                            onUpdateSelection = { chatsSelectedChatIds = it },
                            onClearSelection = {
                                showSelectionMenu = false
                                chatsSelectedChatIds = emptySet()
                            },
                            onDismiss = { showSelectionMenu = false },
                            modifier = Modifier.zIndex(20f)
                        )

                        // 5. Sibling New/Edit list sheet overlay above everything else with zIndex(30f)
                        if (showListSheet) {
                            ChatListSheet(
                                allChats = allChats,
                                currentUid = currentUid,
                                editingList = editingList,
                                backdrop = chatBackdrop,
                                onDismiss = {
                                    showListSheet = false
                                    editingList = null
                                },
                                onCreateList = { name, chatIds ->
                                    chatViewModel.createList(name, chatIds)
                                },
                                onUpdateList = { id, name, chatIds ->
                                    chatViewModel.updateList(id, name, chatIds)
                                },
                                onDeleteList = { id ->
                                    chatViewModel.deleteList(id)
                                },
                                modifier = Modifier.zIndex(30f)
                            )
                        }

                        // 6. Full-screen Text Status Composer overlay
                        if (showStatusComposer) {
                            TextStatusComposerOverlay(
                                viewModel = chatViewModel,
                                onDismiss = { showStatusComposer = false },
                                modifier = Modifier.zIndex(30f)
                            )
                        }

                        // 7. Full-screen Status Viewer overlay
                        val activeViewingGroup = remember(viewingStatusUserId, currentUid, currentUser, myStatuses, contactStatusGroups) {
                            val targetUid = viewingStatusUserId ?: return@remember null
                            if (targetUid == currentUid) {
                                if (myStatuses.isEmpty()) null else UserStatusGroup(
                                    userId = currentUid,
                                    username = currentUser?.username.orEmpty(),
                                    displayName = currentUser?.displayName?.ifBlank { currentUser?.username }.orEmpty(),
                                    photoUrl = currentUser?.photoUrl,
                                    statuses = myStatuses,
                                    latestTimestamp = myStatuses.lastOrNull()?.createdAt
                                )
                            } else {
                                contactStatusGroups.firstOrNull { it.userId == targetUid }
                            }
                        }
                        if (viewingStatusUserId != null) {
                            if (activeViewingGroup == null) {
                                LaunchedEffect(viewingStatusUserId) {
                                    viewingStatusUserId = null
                                }
                            } else {
                                val resolvedName = remember(activeViewingGroup, nicknames, currentUid) {
                                    if (activeViewingGroup.userId == currentUid) {
                                        "My status"
                                    } else {
                                        chatViewModel.displayNameFor(
                                            activeViewingGroup.userId,
                                            ParticipantInfo(
                                                uid = activeViewingGroup.userId,
                                                username = activeViewingGroup.username,
                                                displayName = activeViewingGroup.displayName,
                                                photoUrl = activeViewingGroup.photoUrl
                                            )
                                        )
                                    }
                                }
                                StatusViewerOverlay(
                                    group = activeViewingGroup,
                                    currentUid = currentUid,
                                    displayName = resolvedName,
                                    accentColor = effectiveAccentColor,
                                    viewModel = chatViewModel,
                                    onDismiss = { viewingStatusUserId = null },
                                    modifier = Modifier.zIndex(30f)
                                )
                            }
                        }

                        // 8. Full-screen Create Group overlay
                        if (showCreateGroup) {
                            CreateGroupOverlay(
                                viewModel = chatViewModel,
                                accentColor = effectiveAccentColor,
                                onDismiss = { showCreateGroup = false },
                                modifier = Modifier.zIndex(30f)
                            )
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
                            ThemedIcon(
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
                            if (text.isNotBlank()) {
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { chatViewModel.dismissInAppBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            ThemedIcon(
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
}

@Composable
private fun ChatsSelectionOverflowMenuOverlay(
    visible: Boolean,
    anchorRect: Rect,
    backdrop: Backdrop?,
    selectedChatIds: Set<String>,
    anyUnread: Boolean,
    allFavourite: Boolean,
    allLocked: Boolean,
    allSelectableChatIds: Set<String>,
    contentColor: Color,
    viewModel: ChatViewModel,
    onUpdateSelection: (Set<String>) -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cachedSelectedIds by remember { mutableStateOf(selectedChatIds) }
    var cachedAnyUnread by remember { mutableStateOf(anyUnread) }
    var cachedAllFavourite by remember { mutableStateOf(allFavourite) }
    var cachedAllLocked by remember { mutableStateOf(allLocked) }
    if (selectedChatIds.isNotEmpty()) {
        cachedSelectedIds = selectedChatIds
        cachedAnyUnread = anyUnread
        cachedAllFavourite = allFavourite
        cachedAllLocked = allLocked
    }

    val dividerColor = contentColor.copy(alpha = 0.12f)

    MorphMenu(
        expanded = visible,
        anchorBounds = anchorRect,
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("selection_overflow_glass_menu"),
        panelModifier = Modifier.chatHeaderGlass(
            backdrop = if (SELECTION_MENU_REAL_GLASS_ENABLED) backdrop else null,
            shape = RoundedCornerShape(20.dp),
            isMenu = true
        ),
        width = 220.dp
    ) {
        ChatSelectionOverflowMenuItems(
            cachedSelectedIds = cachedSelectedIds,
            cachedAnyUnread = cachedAnyUnread,
            cachedAllFavourite = cachedAllFavourite,
            cachedAllLocked = cachedAllLocked,
            allSelectableChatIds = allSelectableChatIds,
            contentColor = contentColor,
            dividerColor = dividerColor,
            viewModel = viewModel,
            onUpdateSelection = onUpdateSelection,
            onClearSelection = onClearSelection,
            onDismiss = onDismiss
        )
    }
}

