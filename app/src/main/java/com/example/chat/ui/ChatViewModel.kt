package com.example.chat.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.data.ChatItemLocalState
import com.example.chat.data.ChatList
import com.example.chat.data.ChatListLocalPrefs
import com.example.chat.data.ChatPreferences
import com.example.chat.data.ChatRepository
import com.example.chat.data.ChatStorageManager
import com.example.chat.model.ChatMessage
import com.example.chat.model.ChatSubScreen
import com.example.chat.model.ChatSummary
import com.example.chat.model.ChatTab
import com.example.chat.model.ChatUser
import com.example.chat.model.ParticipantInfo
import com.example.chat.model.StatusItem
import com.example.chat.model.UserStatusGroup
import com.example.chat.model.canViewerSeePrivacyField
import com.example.util.SafeFirebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import android.net.Uri

sealed class ChatFilter {
    data object All : ChatFilter()
    data object Unread : ChatFilter()
    data object Favourites : ChatFilter()
    data object Groups : ChatFilter()
    data class Custom(val listId: String) : ChatFilter()
}

class ChatViewModel(
    private val context: Context,
    private val repository: ChatRepository = ChatRepository(context)
) : ViewModel() {

    private val preferences = ChatPreferences(context)
    private val localChatPrefs = ChatListLocalPrefs(context)

    private val _currentUser = MutableStateFlow<ChatUser?>(null)
    val currentUser: StateFlow<ChatUser?> = _currentUser.asStateFlow()

    private val _isUserLoading = MutableStateFlow(true)
    val isUserLoading: StateFlow<Boolean> = _isUserLoading.asStateFlow()

    private val _isUsernameSetupRequired = MutableStateFlow(false)
    val isUsernameSetupRequired: StateFlow<Boolean> = _isUsernameSetupRequired.asStateFlow()

    private val _activeTab = MutableStateFlow(ChatTab.CHATS)
    val activeTab: StateFlow<ChatTab> = _activeTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow(ChatSubScreen.TABS)
    val activeSubScreen: StateFlow<ChatSubScreen> = _activeSubScreen.asStateFlow()

    private var rawChatsList: List<ChatSummary> = emptyList()

    private val _chatLocalStates = MutableStateFlow<Map<String, ChatItemLocalState>>(localChatPrefs.loadAll())
    val chatLocalStates: StateFlow<Map<String, ChatItemLocalState>> = _chatLocalStates.asStateFlow()

    private val _selectedFilter = MutableStateFlow<ChatFilter>(ChatFilter.All)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _customLists = MutableStateFlow<List<ChatList>>(localChatPrefs.loadCustomLists())
    val customLists: StateFlow<List<ChatList>> = _customLists.asStateFlow()

    private val _allChatsList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val allChatsList: StateFlow<List<ChatSummary>> = _allChatsList.asStateFlow()

    private val _chatsList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatsList: StateFlow<List<ChatSummary>> = _chatsList.asStateFlow()

    private val _groupChats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val groupChats: StateFlow<List<ChatSummary>> = _groupChats.asStateFlow()

    private val _archivedChatsList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val archivedChatsList: StateFlow<List<ChatSummary>> = _archivedChatsList.asStateFlow()

    private val _lockedChatsList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val lockedChatsList: StateFlow<List<ChatSummary>> = _lockedChatsList.asStateFlow()
    val lockedChats: StateFlow<List<ChatSummary>> get() = lockedChatsList

    val lockedChatsUnlocked = MutableStateFlow(false)

    private val _isChatsLoading = MutableStateFlow(false)
    val isChatsLoading: StateFlow<Boolean> = _isChatsLoading.asStateFlow()

    // Real call history flow for Calls tab
    private val _callHistory = MutableStateFlow<List<com.example.chat.model.CallLogItem>>(emptyList())
    val callHistory: StateFlow<List<com.example.chat.model.CallLogItem>> = _callHistory.asStateFlow()
    private var callHistoryListenJob: Job? = null

    // Status updates state for Updates tab
    private var rawStatuses: List<StatusItem> = emptyList()
    private val _myStatuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val myStatuses: StateFlow<List<StatusItem>> = _myStatuses.asStateFlow()

    private val _contactStatusGroups = MutableStateFlow<List<UserStatusGroup>>(emptyList())
    val contactStatusGroups: StateFlow<List<UserStatusGroup>> = _contactStatusGroups.asStateFlow()

    private val _viewedStatusIds = MutableStateFlow<Set<String>>(preferences.getViewedStatusIds())
    val viewedStatusIds: StateFlow<Set<String>> = _viewedStatusIds.asStateFlow()

    private val _isPostingStatus = MutableStateFlow(false)
    val isPostingStatus: StateFlow<Boolean> = _isPostingStatus.asStateFlow()
    private var statusesListenJob: Job? = null

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ChatUser>>(emptyList())
    val searchResults: StateFlow<List<ChatUser>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Active conversation state
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeRecipient = MutableStateFlow<ParticipantInfo?>(null)
    val activeRecipient: StateFlow<ParticipantInfo?> = _activeRecipient.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _ghostModeEnabled = MutableStateFlow(false)
    val ghostModeEnabled: StateFlow<Boolean> = _ghostModeEnabled.asStateFlow()

    private val _editingMessageId = MutableStateFlow<String?>(null)
    val editingMessageId: StateFlow<String?> = _editingMessageId.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    // Operation status messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    private val _isSubmittingUsername = MutableStateFlow(false)
    val isSubmittingUsername: StateFlow<Boolean> = _isSubmittingUsername.asStateFlow()

    // In-app notification banner
    private val _inAppBanner = MutableStateFlow<Pair<String, String>?>(null)
    val inAppBanner: StateFlow<Pair<String, String>?> = _inAppBanner.asStateFlow()

    // Notifications toggle
    private val _notificationsEnabled = MutableStateFlow(preferences.isNotificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Online status (presence) preference + the open chat's live presence
    private val _showOnlineStatus = MutableStateFlow(preferences.isOnlineStatusEnabled)
    val showOnlineStatus: StateFlow<Boolean> = _showOnlineStatus.asStateFlow()

    // Read receipts preference
    private val _readReceipts = MutableStateFlow(preferences.isReadReceiptsEnabled)
    val readReceipts: StateFlow<Boolean> = _readReceipts.asStateFlow()

    private val _recipientPresence = MutableStateFlow<ChatUser?>(null)
    val recipientPresence: StateFlow<ChatUser?> = _recipientPresence.asStateFlow()

    private val _recipientTyping = MutableStateFlow(false)
    val recipientTyping: StateFlow<Boolean> = _recipientTyping.asStateFlow()

    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val nicknames: StateFlow<Map<String, String>> = _nicknames.asStateFlow()

    private val _blockedUsersMap = MutableStateFlow<Map<String, ParticipantInfo>>(emptyMap())
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _blockedContacts = MutableStateFlow<List<ParticipantInfo>>(emptyList())
    val blockedContacts: StateFlow<List<ParticipantInfo>> = _blockedContacts.asStateFlow()

    private val _userProfiles = MutableStateFlow<Map<String, ChatUser>>(emptyMap())
    val userProfiles: StateFlow<Map<String, ChatUser>> = _userProfiles.asStateFlow()

    private val _contactUserIds = MutableStateFlow<Set<String>>(emptySet())
    val contactUserIds: StateFlow<Set<String>> = _contactUserIds.asStateFlow()

    private val _directContacts = MutableStateFlow<List<ParticipantInfo>>(emptyList())
    val directContacts: StateFlow<List<ParticipantInfo>> = _directContacts.asStateFlow()

    private val _isActiveChatBlockedByMe = MutableStateFlow(false)
    val isActiveChatBlockedByMe: StateFlow<Boolean> = _isActiveChatBlockedByMe.asStateFlow()

    private val _isActiveChatBlockedByOther = MutableStateFlow(false)
    val isActiveChatBlockedByOther: StateFlow<Boolean> = _isActiveChatBlockedByOther.asStateFlow()

    private val _pendingOpenChatId = MutableStateFlow<String?>(null)
    val pendingOpenChatId: StateFlow<String?> = _pendingOpenChatId.asStateFlow()

    val changedPeerUids: StateFlow<Set<String>> = repository.changedPeerUids

    private val _mutedChats = MutableStateFlow<Map<String, com.google.firebase.Timestamp>>(emptyMap())
    val mutedChats: StateFlow<Map<String, com.google.firebase.Timestamp>> = _mutedChats.asStateFlow()

    fun acceptNewKey(peerUid: String) {
        repository.acceptPeerKey(peerUid)
    }

    suspend fun getSafetyNumberForPeer(peerUid: String): List<String>? {
        val myUid = _currentUser.value?.uid?.trim().orEmpty()
        if (myUid.isEmpty() || peerUid.isBlank()) return null
        return repository.getSafetyNumberForPeer(myUid, peerUid.trim())
    }

    fun isChatMuted(chatId: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val clean = chatId.trim()
        if (clean.isEmpty()) return false
        val remoteTs = _mutedChats.value[clean]
        if (remoteTs != null) {
            return remoteTs.toDate().time > nowMs
        }
        return _chatLocalStates.value[clean]?.isMuted(nowMs) == true
    }

    fun setMutedUntil(chatId: String, endTime: com.google.firebase.Timestamp?) {
        val cleanChatId = chatId.trim()
        if (cleanChatId.isEmpty()) return
        val localMs = when {
            endTime == null -> 0L
            endTime.seconds >= 4102444800L -> Long.MAX_VALUE
            else -> endTime.toDate().time
        }
        _mutedChats.update { current ->
            if (endTime != null) current + (cleanChatId to endTime) else current - cleanChatId
        }
        _chatLocalStates.value = localChatPrefs.updateChats(setOf(cleanChatId)) {
            it.copy(mutedUntil = localMs)
        }
        refreshVisibleChatsList()
        val myUid = _currentUser.value?.uid?.trim().orEmpty()
        if (myUid.isNotEmpty()) {
            viewModelScope.launch {
                repository.setMutedUntil(myUid, cleanChatId, endTime)
            }
        }
    }

    private var hasLoadedChatsOnce = false
    private var rawCallHistory: List<com.example.chat.model.CallLogItem> = emptyList()
    private val fetchingBlockedUserProfiles = mutableSetOf<String>()
    private val knownGroupChatIds = mutableSetOf<String>()
    private val selfRemovedGroupChatIds = mutableSetOf<String>()
    private val userProfileListeners = mutableMapOf<String, Job>()

    private var chatsListenJob: Job? = null
    private var nicknamesListenJob: Job? = null
    private var blockedListenJob: Job? = null
    private var messagesListenJob: Job? = null
    private var markReadJob: Job? = null
    private var searchJob: Job? = null
    private var presenceListenJob: Job? = null
    private var peerKeyListenJob: Job? = null
    private var presenceHeartbeatJob: Job? = null
    private var typingIdleJob: Job? = null
    private var lastTypingWriteMs: Long = 0L
    private var lastTypingChatId: String? = null
    private var recipientTypingExpiryJob: Job? = null

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            maybeMarkActiveChatMessagesAsRead()
        } else if (event == Lifecycle.Event.ON_STOP) {
            lockedChatsUnlocked.value = false
            stopTyping()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        checkUserSession()
    }

    private fun startPresenceHeartbeat(uid: String) {
        presenceHeartbeatJob?.cancel()
        if (!_showOnlineStatus.value) return
        presenceHeartbeatJob = viewModelScope.launch {
            while (isActive) {
                // Only mark online while the app is really in the foreground.
                // Without this check, the heartbeat keeps the user "online" after the app is minimized.
                val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED)
                if (_showOnlineStatus.value && inForeground) {
                    repository.markOnlineIfForeground(uid)
                }
                delay(30_000L)
            }
        }
    }

    private fun stopPresenceHeartbeat() {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
    }

    private fun syncFcmToken(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val pending = preferences.pendingFcmToken
            if (!pending.isNullOrBlank()) {
                repository.saveFcmToken(uid, pending)
                    .onSuccess {
                        if (preferences.pendingFcmToken == pending) {
                            preferences.pendingFcmToken = null
                        }
                    }
                    .onFailure { e ->
                        Log.w(TAG, "Failed to save pending FCM token: ${e.message}", e)
                    }
            }
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    repository.saveFcmToken(uid, token).onFailure { e ->
                        Log.w(TAG, "Failed to save FCM token: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get FCM token: ${e.message}", e)
            }
        }
    }

    fun setPendingOpenChatId(chatId: String?) {
        _pendingOpenChatId.value = chatId?.trim()?.ifEmpty { null }
    }

    fun clearPendingOpenChatId() {
        _pendingOpenChatId.value = null
    }

    fun tryOpenPendingChat() {
        val targetChatId = _pendingOpenChatId.value ?: return
        if (_isUserLoading.value || _isChatsLoading.value || !hasLoadedChatsOnce) return
        if (_currentUser.value == null) return
        val targetChat = _chatsList.value.firstOrNull { it.chatId == targetChatId }
            ?: _groupChats.value.firstOrNull { it.chatId == targetChatId }
            ?: _allChatsList.value.firstOrNull { it.chatId == targetChatId }
            ?: _lockedChatsList.value.firstOrNull { it.chatId == targetChatId }
        _pendingOpenChatId.value = null
        if (targetChat != null) {
            _activeTab.value = if (targetChat.isGroup) ChatTab.GROUPS else ChatTab.CHATS
            if (_chatLocalStates.value[targetChatId]?.locked == true && !lockedChatsUnlocked.value) {
                return
            }
            openChatFromSummary(targetChat)
        }
    }

    fun checkUserSession() {
        val authUser = SafeFirebase.getAuth(context)?.currentUser
        if (authUser == null) {
            chatsListenJob?.cancel()
            callHistoryListenJob?.cancel()
            statusesListenJob?.cancel()
            nicknamesListenJob?.cancel()
            blockedListenJob?.cancel()
            peerKeyListenJob?.cancel()
            clearUserProfileListeners()
            hasLoadedChatsOnce = false
            rawStatuses = emptyList()
            _groupChats.value = emptyList()
            _myStatuses.value = emptyList()
            _contactStatusGroups.value = emptyList()
            _nicknames.value = emptyMap()
            _blockedUsersMap.value = emptyMap()
            _blockedUserIds.value = emptySet()
            _blockedContacts.value = emptyList()
            _userProfiles.value = emptyMap()
            _contactUserIds.value = emptySet()
            _directContacts.value = emptyList()
            _currentUser.value = null
            _isUserLoading.value = false
            _isUsernameSetupRequired.value = false
            return
        }

        viewModelScope.launch {
            _isUserLoading.value = true
            repository.migrateOwnUserSplit(authUser.uid)
            val existing = repository.getChatUser(authUser.uid)
            if (existing != null && existing.username.isNotBlank()) {
                repository.ensureE2eeIdentity(authUser.uid)
                _currentUser.value = existing
                _userProfiles.update { it + (existing.uid to existing) }
                _isUsernameSetupRequired.value = false
                preferences.lastUid = authUser.uid
                preferences.lastUsername = existing.username
                syncFcmToken(authUser.uid)
                startPresenceHeartbeat(authUser.uid)
                (context.applicationContext as? android.app.Application)?.let { app ->
                    com.example.chat.call.ZegoCallManager.init(app, authUser.uid, existing.username)
                }
                startListeningToChats(authUser.uid)
            } else {
                _currentUser.value = null
                _isUsernameSetupRequired.value = true
            }
            _isUserLoading.value = false
        }
    }

    fun registerUsername(username: String) {
        val authUser = SafeFirebase.getAuth(context)?.currentUser ?: return
        val trimmed = username.trim().lowercase()
        if (!ChatRepository.isValidUsername(trimmed)) {
            _usernameError.value = "Username must be 3-20 characters: lowercase letters, numbers, or underscores only."
            return
        }

        viewModelScope.launch {
            _isSubmittingUsername.value = true
            _usernameError.value = null
            val result = repository.registerUsername(
                uid = authUser.uid,
                rawUsername = trimmed,
                displayName = authUser.displayName,
                photoUrl = authUser.photoUrl?.toString()
            )
            result.onSuccess { user ->
                repository.migrateOwnUserSplit(user.uid)
                repository.ensureE2eeIdentity(user.uid)
                _currentUser.value = user
                _isUsernameSetupRequired.value = false
                _toastMessage.value = "Welcome, @${user.username}!"
                preferences.lastUid = user.uid
                preferences.lastUsername = user.username
                syncFcmToken(user.uid)
                startPresenceHeartbeat(user.uid)
                (context.applicationContext as? android.app.Application)?.let { app ->
                    com.example.chat.call.ZegoCallManager.init(app, user.uid, user.username)
                }
                startListeningToChats(user.uid)
            }.onFailure { err ->
                _usernameError.value = err.message ?: "Failed to set username"
            }
            _isSubmittingUsername.value = false
        }
    }

    fun changeUsername(newUsername: String, onComplete: (Boolean) -> Unit) {
        val current = _currentUser.value ?: return
        val trimmed = newUsername.trim().lowercase()

        if (!ChatRepository.isValidUsername(trimmed)) {
            _usernameError.value = "Username must be 3-20 characters: lowercase letters, numbers, or underscores only."
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isSubmittingUsername.value = true
            _usernameError.value = null
            val result = repository.changeUsername(current.uid, current.username, trimmed)
            result.onSuccess { updatedUsername ->
                _currentUser.value = current.copy(username = updatedUsername)
                _toastMessage.value = "Username updated to @$updatedUsername"
                preferences.lastUsername = updatedUsername
                syncFcmToken(current.uid)
                (context.applicationContext as? android.app.Application)?.let { app ->
                    com.example.chat.call.ZegoCallManager.init(app, current.uid, updatedUsername)
                }
                startListeningToChats(current.uid)
                onComplete(true)
            }.onFailure { err ->
                _usernameError.value = err.message ?: "Failed to change username"
                onComplete(false)
            }
            _isSubmittingUsername.value = false
        }
    }

    fun updateAbout(newAbout: String, onComplete: (Boolean) -> Unit = {}) {
        val current = _currentUser.value ?: run {
            onComplete(false)
            return
        }
        val trimmed = newAbout.trim().take(139)
        _currentUser.update { it?.copy(about = trimmed) }
        _userProfiles.update { profiles ->
            val existing = profiles[current.uid] ?: current
            profiles + (current.uid to existing.copy(about = trimmed))
        }
        viewModelScope.launch {
            repository.setAbout(current.uid, trimmed)
                .onSuccess {
                    _toastMessage.value = "About updated"
                    onComplete(true)
                }
                .onFailure { err ->
                    _toastMessage.value = "Failed to update about: ${err.message ?: "unknown error"}"
                    onComplete(false)
                }
        }
    }

    private fun clearUserProfileListeners() {
        userProfileListeners.values.forEach { it.cancel() }
        userProfileListeners.clear()
    }

    private fun observeMergedOwnUser(uid: String): Flow<ChatUser?> {
        return combine(
            repository.observePublicUser(uid),
            repository.observeMyPrivate(uid),
            repository.observePresence(uid),
            repository.observePhoto(uid),
            repository.observeAbout(uid)
        ) { publicUser, priv, presence, photoUrl, aboutText ->
            val base = publicUser ?: _currentUser.value?.takeIf { it.uid == uid } ?: return@combine null
            val lastSeenVis = priv?.lastSeenVisibility ?: "everyone"
            val showOnline = (priv?.showOnlineStatus ?: true) && lastSeenVis != "nobody"
            val statusVis = when (priv?.statusMode) {
                "except", "exceptUids" -> "except"
                "only", "onlyUids" -> "only"
                else -> "contacts"
            }
            val statusExcept = if (statusVis == "except") priv?.statusUids.orEmpty() else emptyList()
            val statusOnly = if (statusVis == "only") priv?.statusUids.orEmpty() else emptyList()

            if (priv != null) {
                _mutedChats.value = priv.mutedChats
                val currentLocal = _chatLocalStates.value
                val toSync = mutableMapOf<String, Long>()
                for ((cid, ts) in priv.mutedChats) {
                    val targetMs = if (ts.seconds >= 4102444800L) Long.MAX_VALUE else ts.toDate().time
                    if (currentLocal[cid]?.mutedUntil != targetMs) {
                        toSync[cid] = targetMs
                    }
                }
                for ((cid, state) in currentLocal) {
                    if (state.mutedUntil != 0L && cid !in priv.mutedChats) {
                        toSync[cid] = 0L
                    }
                }
                if (toSync.isNotEmpty()) {
                    var updated = currentLocal
                    for ((cid, targetMs) in toSync) {
                        updated = localChatPrefs.updateChats(setOf(cid)) {
                            it.copy(mutedUntil = targetMs)
                        }
                    }
                    _chatLocalStates.value = updated
                }
            }

            base.copy(
                photoUrl = photoUrl,
                fcmTokens = priv?.fcmTokens.orEmpty(),
                blockedUsers = priv?.blockedUsers.orEmpty(),
                readReceipts = priv?.readReceipts ?: true,
                showOnlineStatus = showOnline,
                lastSeenVisibility = lastSeenVis,
                photoVisibility = priv?.photoVisibility ?: "everyone",
                aboutVisibility = priv?.aboutVisibility ?: "everyone",
                groupsVisibility = priv?.groupAddVisibility ?: "everyone",
                statusVisibility = statusVis,
                statusExceptIds = statusExcept,
                statusOnlyIds = statusOnly,
                isOnline = presence?.isOnline ?: false,
                lastSeenAt = presence?.lastSeenAt,
                typingChatId = presence?.typingChatId,
                typingAt = presence?.typingAt,
                about = aboutText.orEmpty()
            )
        }
    }

    private fun observeMergedOtherUser(uid: String): Flow<ChatUser?> {
        return combine(
            repository.observePublicUser(uid),
            repository.observePresence(uid),
            repository.observePhoto(uid),
            repository.observeAbout(uid)
        ) { publicUser, presence, photoUrl, aboutText ->
            val base = publicUser ?: _userProfiles.value[uid] ?: ChatUser(uid = uid)
            if (base.username.isBlank() && publicUser == null && presence == null && photoUrl == null && aboutText == null) {
                return@combine null
            }
            val hasPresence = presence != null
            val hasPhoto = !photoUrl.isNullOrBlank()
            val hasAbout = !aboutText.isNullOrBlank()
            base.copy(
                photoUrl = photoUrl,
                isOnline = presence?.isOnline ?: false,
                lastSeenAt = presence?.lastSeenAt,
                typingChatId = presence?.typingChatId,
                typingAt = presence?.typingAt,
                showOnlineStatus = hasPresence,
                lastSeenVisibility = if (hasPresence) "everyone" else "nobody",
                photoVisibility = if (hasPhoto) "everyone" else "nobody",
                about = aboutText.orEmpty(),
                aboutVisibility = if (hasAbout) "everyone" else "nobody"
            )
        }
    }

    private fun syncUserProfileListeners() {
        val currentUid = _currentUser.value?.uid.orEmpty()
        val targetUids = buildSet {
            if (currentUid.isNotBlank()) add(currentUid)
            for (chat in rawChatsList) {
                for (pid in chat.participantIds) {
                    if (pid.isNotBlank()) add(pid)
                }
            }
            for (status in rawStatuses) {
                if (status.userId.isNotBlank()) add(status.userId)
            }
            for (searchUser in _searchResults.value) {
                if (searchUser.uid.isNotBlank()) add(searchUser.uid)
            }
        }

        val toRemove = userProfileListeners.keys.filter { it !in targetUids }
        for (uid in toRemove) {
            userProfileListeners.remove(uid)?.cancel()
        }

        for (uid in targetUids) {
            if (uid in userProfileListeners) continue
            userProfileListeners[uid] = viewModelScope.launch {
                val flow = if (uid == currentUid) {
                    observeMergedOwnUser(uid)
                } else {
                    observeMergedOtherUser(uid)
                }
                flow
                    .catch { Log.w(TAG, "Error observing profile for $uid: ${it.message}") }
                    .collectLatest { user ->
                        if (user != null) {
                            _userProfiles.update { it + (uid to user) }
                            if (uid == _currentUser.value?.uid) {
                                _currentUser.value = user
                                val effectiveShowOnline = user.showOnlineStatus && user.lastSeenVisibility != "nobody"
                                if (_showOnlineStatus.value != effectiveShowOnline) {
                                    preferences.isOnlineStatusEnabled = effectiveShowOnline
                                    _showOnlineStatus.value = effectiveShowOnline
                                    if (effectiveShowOnline) {
                                        startPresenceHeartbeat(uid)
                                    } else {
                                        stopPresenceHeartbeat()
                                    }
                                }
                                if (_readReceipts.value != user.readReceipts) {
                                    preferences.isReadReceiptsEnabled = user.readReceipts
                                    _readReceipts.value = user.readReceipts
                                }
                                refreshBlockedState()
                            }
                            refreshVisibleStatuses()
                        }
                    }
            }
        }
    }

    fun isDirectContact(otherUid: String): Boolean {
        val clean = otherUid.trim()
        if (clean.isEmpty()) return false
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (clean == currentUid) return true
        if (clean in _contactUserIds.value) return true
        return rawChatsList.any { !it.isGroup && clean in it.participantIds }
    }

    fun canSeeUserLastSeen(otherUser: ChatUser?): Boolean {
        if (otherUser == null) return false
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (otherUser.uid.isNotBlank() && otherUser.uid == currentUid) return true
        if (!_showOnlineStatus.value || _currentUser.value?.lastSeenVisibility == "nobody") return false
        if (_currentUser.value?.lastSeenVisibility == "contacts" && !isDirectContact(otherUser.uid)) return false
        if (isUserOrChatBlocked(otherUser.uid)) return false
        if (!otherUser.showOnlineStatus) return false
        return canViewerSeePrivacyField(
            visibility = otherUser.lastSeenVisibility,
            isSelf = false,
            isContact = isDirectContact(otherUser.uid)
        )
    }

    fun canSeeUserPhoto(otherUid: String, otherUser: ChatUser? = _userProfiles.value[otherUid]): Boolean {
        val clean = otherUid.trim()
        if (clean.isEmpty()) return false
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (clean == currentUid) return true
        if (isUserOrChatBlocked(clean)) return false
        val resolved = _userProfiles.value[clean] ?: otherUser
        return !resolved?.photoUrl.isNullOrBlank()
    }

    fun visiblePhotoUrl(
        otherUid: String,
        fallbackUrl: String?,
        otherUser: ChatUser? = _userProfiles.value[otherUid]
    ): String? {
        val clean = otherUid.trim()
        if (clean.isEmpty()) return null
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (clean == currentUid) {
            return _currentUser.value?.photoUrl?.ifBlank { null }
        }
        if (isUserOrChatBlocked(clean)) return null
        val resolved = _userProfiles.value[clean] ?: otherUser
        return resolved?.photoUrl?.ifBlank { null }
    }

    fun canSeeUserAbout(otherUid: String, otherUser: ChatUser? = _userProfiles.value[otherUid]): Boolean {
        val clean = otherUid.trim()
        if (clean.isEmpty()) return false
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (clean == currentUid) return true
        if (isUserOrChatBlocked(clean)) return false
        val resolved = _userProfiles.value[clean] ?: otherUser
        return !resolved?.about?.trim().isNullOrEmpty()
    }

    fun visibleAbout(
        otherUid: String,
        otherUser: ChatUser? = _userProfiles.value[otherUid]
    ): String? {
        val clean = otherUid.trim()
        if (clean.isEmpty()) return null
        val currentUid = _currentUser.value?.uid.orEmpty()
        if (clean == currentUid) {
            return _currentUser.value?.about?.trim()?.ifEmpty { null }
        }
        if (isUserOrChatBlocked(clean)) return null
        val resolved = _userProfiles.value[clean] ?: otherUser
        return resolved?.about?.trim()?.ifEmpty { null }
    }

    private fun refreshBlockedState() {
        val currentUid = _currentUser.value?.uid.orEmpty()
        val fromSubcollection = _blockedUsersMap.value
        val fromUserDoc = _currentUser.value?.blockedUsers.orEmpty()
        val fromChats = mutableMapOf<String, ParticipantInfo>()
        if (currentUid.isNotEmpty()) {
            for (chat in rawChatsList) {
                if (chat.isGroup) continue
                if (currentUid in chat.blockedBy) {
                    val otherUid = chat.participantIds.firstOrNull { it != currentUid }.orEmpty()
                    if (otherUid.isNotEmpty()) {
                        val info = chat.getOtherParticipant(currentUid) ?: ParticipantInfo(uid = otherUid)
                        fromChats[otherUid] = info
                    }
                }
            }
        }

        val combinedIds = buildSet {
            addAll(fromSubcollection.keys)
            addAll(fromUserDoc)
            addAll(fromChats.keys)
            remove("")
            if (currentUid.isNotEmpty()) remove(currentUid)
        }
        _blockedUserIds.value = combinedIds

        val contacts = combinedIds.map { uid ->
            val chatInfo = fromChats[uid]
                ?: rawChatsList.firstOrNull { uid in it.participantIds }?.participantInfo?.get(uid)
            val subInfo = fromSubcollection[uid]
            val merged = ParticipantInfo(
                uid = uid,
                username = chatInfo?.username?.ifBlank { null } ?: subInfo?.username.orEmpty(),
                displayName = chatInfo?.displayName?.ifBlank { null } ?: subInfo?.displayName.orEmpty(),
                photoUrl = chatInfo?.photoUrl?.ifBlank { null } ?: subInfo?.photoUrl
            )
            if (merged.username.isBlank() && merged.displayName.isBlank() && uid !in fetchingBlockedUserProfiles) {
                fetchingBlockedUserProfiles.add(uid)
                viewModelScope.launch {
                    val fetched = repository.getChatUser(uid)
                    if (fetched != null) {
                        _blockedUsersMap.update { current ->
                            if (uid in _blockedUserIds.value) {
                                current + (uid to ParticipantInfo(
                                    uid = uid,
                                    username = fetched.username,
                                    displayName = fetched.displayName,
                                    photoUrl = fetched.photoUrl
                                ))
                            } else {
                                current
                            }
                        }
                        refreshBlockedState()
                    }
                }
            }
            merged
        }.sortedBy { displayNameFor(it.uid, it).lowercase() }

        _blockedContacts.value = contacts
        refreshActiveChatBlockFlags()
    }

    private fun refreshActiveChatBlockFlags() {
        val currentUid = _currentUser.value?.uid.orEmpty()
        val activeChatId = _activeChatId.value
        val activeChat = if (!activeChatId.isNullOrBlank()) {
            rawChatsList.firstOrNull { it.chatId == activeChatId }
        } else null
        if (activeChat?.isGroup == true) {
            _isActiveChatBlockedByMe.value = false
            _isActiveChatBlockedByOther.value = false
            return
        }
        val activeOtherUid = _activeRecipient.value?.uid.orEmpty()
        val otherUid = activeOtherUid.ifBlank {
            activeChat?.participantIds?.firstOrNull { it != currentUid }.orEmpty()
        }

        val byMe = (otherUid.isNotEmpty() && otherUid in _blockedUserIds.value) ||
            (currentUid.isNotEmpty() && activeChat?.blockedBy?.contains(currentUid) == true)
        val byOther = (otherUid.isNotEmpty() && activeChat?.blockedBy?.contains(otherUid) == true) ||
            (currentUid.isNotEmpty() && _recipientPresence.value?.blockedUsers?.contains(currentUid) == true)

        _isActiveChatBlockedByMe.value = byMe
        _isActiveChatBlockedByOther.value = byOther
        if (byMe || byOther) {
            stopTyping()
            recipientTypingExpiryJob?.cancel()
            recipientTypingExpiryJob = null
            _recipientTyping.value = false
        }
    }

    private fun refreshVisibleCallHistory() {
        val currentUid = _currentUser.value?.uid.orEmpty()
        val blockedIds = _blockedUserIds.value
        _callHistory.value = rawCallHistory.filter { log ->
            val otherUid = if (log.callerId == currentUid) log.calleeId else log.callerId
            val chat = rawChatsList.firstOrNull { it.chatId == log.chatId }
            val blockedInChat = currentUid.isNotEmpty() && chat?.blockedBy?.contains(currentUid) == true
            otherUid !in blockedIds && !blockedInChat
        }
    }

    private fun refreshVisibleChatsList() {
        for (chat in rawChatsList) {
            if (chat.isGroup) {
                knownGroupChatIds.add(chat.chatId)
            }
        }
        refreshBlockedState()
        val currentUid = _currentUser.value?.uid.orEmpty()
        val blockedIds = _blockedUserIds.value
        val directContactMap = LinkedHashMap<String, ParticipantInfo>()
        val allDirectContactIds = mutableSetOf<String>()
        for (chat in rawChatsList) {
            if (chat.isGroup) continue
            val otherUid = chat.participantIds.firstOrNull { it != currentUid }.orEmpty()
            if (otherUid.isNotBlank()) {
                allDirectContactIds.add(otherUid)
                val isBlocked = (currentUid.isNotEmpty() && currentUid in chat.blockedBy) ||
                    (otherUid in blockedIds)
                if (!isBlocked && otherUid !in directContactMap) {
                    val info = chat.getOtherParticipant(currentUid) ?: ParticipantInfo(uid = otherUid)
                    directContactMap[otherUid] = info
                }
            }
        }
        _contactUserIds.value = allDirectContactIds
        _directContacts.value = directContactMap.values
            .sortedBy { displayNameFor(it.uid, it).lowercase() }

        syncUserProfileListeners()

        val states = _chatLocalStates.value
        val nonHidden = rawChatsList.filter { chat ->
            if (!chat.isGroup) {
                val otherUid = chat.participantIds.firstOrNull { it != currentUid }.orEmpty()
                val isBlockedByMe = (currentUid.isNotEmpty() && currentUid in chat.blockedBy) ||
                    (otherUid.isNotEmpty() && otherUid in blockedIds)
                if (isBlockedByMe) return@filter false
            }

            val state = states[chat.chatId]
            if (state == null) {
                true
            } else {
                val lastMsgTimeMs = chat.lastMessage?.timestamp?.toDate()?.time ?: 0L
                !state.isHidden(lastMsgTimeMs)
            }
        }
        val lockedList = nonHidden
            .filter { chat -> states[chat.chatId]?.locked == true }
            .sortedByDescending { chat -> states[chat.chatId]?.pinned == true }
        _lockedChatsList.value = lockedList

        val unlockedNonHidden = nonHidden
            .filter { chat -> states[chat.chatId]?.locked != true }

        val allNonArchived = unlockedNonHidden
            .filter { chat -> states[chat.chatId]?.archived != true }
            .sortedByDescending { chat -> states[chat.chatId]?.pinned == true }
        _allChatsList.value = allNonArchived
        _groupChats.value = allNonArchived.filter { chat -> chat.type == "group" }

        val filter = _selectedFilter.value
        _chatsList.value = when (filter) {
            ChatFilter.All -> allNonArchived
            ChatFilter.Unread -> allNonArchived.filter { chat -> states[chat.chatId]?.markedUnread == true }
            ChatFilter.Favourites -> allNonArchived.filter { chat -> states[chat.chatId]?.favourite == true }
            ChatFilter.Groups -> allNonArchived.filter { chat -> chat.type == "group" }
            is ChatFilter.Custom -> {
                val listIds = _customLists.value.find { it.id == filter.listId }?.chatIds?.toSet().orEmpty()
                allNonArchived.filter { chat -> chat.chatId in listIds }
            }
        }
        _archivedChatsList.value = unlockedNonHidden
            .filter { chat -> states[chat.chatId]?.archived == true }
            .sortedByDescending { chat -> states[chat.chatId]?.pinned == true }

        val activeId = _activeChatId.value
        if (hasLoadedChatsOnce &&
            !activeId.isNullOrBlank() &&
            _activeSubScreen.value == ChatSubScreen.CONVERSATION &&
            activeId in knownGroupChatIds &&
            activeId !in selfRemovedGroupChatIds &&
            _chatsList.value.none { it.chatId == activeId } &&
            rawChatsList.none { it.chatId == activeId }
        ) {
            knownGroupChatIds.remove(activeId)
            navigateBackFromConversation()
            _toastMessage.value = "This group was deleted"
        }

        if (_searchResults.value.isNotEmpty() && currentUid.isNotEmpty()) {
            _searchResults.value = _searchResults.value.filterNot { user ->
                isUserInLockedChat(user.uid, currentUid)
            }
        }
        refreshVisibleCallHistory()
        refreshVisibleStatuses()
    }

    private fun refreshVisibleStatuses() {
        val currentUid = _currentUser.value?.uid.orEmpty()
        val blockedIds = _blockedUserIds.value
        val profiles = _userProfiles.value
        val nowMs = System.currentTimeMillis()
        val active = rawStatuses.filter { status ->
            !status.isExpired(nowMs) && status.userId !in blockedIds
        }

        val mine = if (currentUid.isNotEmpty()) {
            active.filter { it.userId == currentUid }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        } else {
            emptyList()
        }
        _myStatuses.value = mine

        val othersByUser = active
            .filter { it.userId.isNotEmpty() && it.userId != currentUid }
            .filter { status ->
                val chat = rawChatsList.firstOrNull { !it.isGroup && status.userId in it.participantIds }
                val blockedInChat = currentUid.isNotEmpty() && chat?.blockedBy?.contains(currentUid) == true
                !blockedInChat
            }
            .groupBy { it.userId }

        val groups = othersByUser.map { (userId, items) ->
            val sortedItems = items.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            val latest = sortedItems.last()
            val chatParticipant = rawChatsList
                .firstOrNull { userId in it.participantIds }
                ?.participantInfo
                ?.get(userId)
            val authorProfile = profiles[userId]
            val resolvedUsername = latest.username.ifBlank {
                authorProfile?.username?.ifBlank { null } ?: chatParticipant?.username.orEmpty()
            }
            val resolvedDisplayName = latest.displayName.ifBlank {
                authorProfile?.displayName?.ifBlank { null } ?: chatParticipant?.displayName.orEmpty()
            }
            val rawPhoto = authorProfile?.photoUrl?.ifBlank { null }
                ?: latest.photoUrl?.ifBlank { null }
                ?: chatParticipant?.photoUrl?.ifBlank { null }
            val resolvedPhoto = visiblePhotoUrl(userId, rawPhoto, authorProfile)
            val sanitizedItems = if (resolvedPhoto == null) {
                sortedItems.map { it.copy(photoUrl = null) }
            } else {
                sortedItems
            }
            UserStatusGroup(
                userId = userId,
                username = resolvedUsername,
                displayName = resolvedDisplayName,
                photoUrl = resolvedPhoto,
                statuses = sanitizedItems,
                latestTimestamp = latest.createdAt
            )
        }.sortedByDescending { it.latestTimestamp?.toDate()?.time ?: 0L }

        _contactStatusGroups.value = groups
    }

    fun refreshStatuses() {
        refreshVisibleStatuses()
        val uid = _currentUser.value?.uid ?: return
        if (uid.isBlank()) return
        viewModelScope.launch {
            val fresh = repository.fetchActiveStatuses(uid)
            rawStatuses = fresh
            syncUserProfileListeners()
            refreshVisibleStatuses()
        }
    }

    fun postTextStatus(
        text: String,
        backgroundColor: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = _currentUser.value ?: run {
            onComplete(false)
            return
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _toastMessage.value = "Status text cannot be empty"
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isPostingStatus.value = true
            val result = repository.postTextStatus(user, trimmed, backgroundColor)
            _isPostingStatus.value = false
            result.onSuccess { created ->
                rawStatuses = (rawStatuses.filterNot { it.id == created.id } + created)
                refreshVisibleStatuses()
                _toastMessage.value = "Status posted"
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.value = "Failed to post status: ${err.message ?: "unknown error"}"
                onComplete(false)
            }
        }
    }

    fun deleteStatus(statusId: String) {
        if (statusId.isBlank()) return
        rawStatuses = rawStatuses.filterNot { it.id == statusId }
        refreshVisibleStatuses()
        viewModelScope.launch {
            repository.deleteStatus(statusId).onFailure { err ->
                _toastMessage.value = "Failed to delete status: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun markStatusViewed(statusId: String) {
        if (statusId.isBlank()) return
        _viewedStatusIds.value = preferences.markStatusViewed(statusId)
        val uid = _currentUser.value?.uid.orEmpty()
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                repository.markStatusViewedInFirestore(statusId, uid)
            }
        }
    }

    private fun startListeningToChats(uid: String) {
        chatsListenJob?.cancel()
        _isChatsLoading.value = true
        chatsListenJob = viewModelScope.launch {
            repository.getUserChatsFlow(uid)
                .catch {
                    _isChatsLoading.value = false
                }
                .collectLatest { list ->
                    _isChatsLoading.value = false
                    val previousList = rawChatsList
                    rawChatsList = list
                    hasLoadedChatsOnce = true
                    refreshVisibleChatsList()

                    // In-app notification check for foreground messages
                    if (previousList.isNotEmpty() && preferences.isNotificationsEnabled) {
                        for (chat in list) {
                            if (!chat.isGroup) {
                                val otherUid = chat.participantIds.firstOrNull { it != uid }.orEmpty()
                                if (chat.blockedBy.isNotEmpty() || otherUid in _blockedUserIds.value) continue
                            }
                            val last = chat.lastMessage ?: continue
                            val prevChat = previousList.find { it.chatId == chat.chatId }
                            val prevTime = prevChat?.lastMessage?.timestamp?.toDate()?.time ?: 0L
                            val currTime = last.timestamp?.toDate()?.time ?: 0L

                            // If new message from other user and not currently inside this conversation
                            if (currTime > prevTime && last.senderId != uid && _activeChatId.value != chat.chatId) {
                                val chatState = _chatLocalStates.value[chat.chatId]
                                if (isChatMuted(chat.chatId)) continue
                                if (chatState?.locked == true) {
                                    _inAppBanner.value = Pair("New message", "")
                                } else if (chat.isGroup) {
                                    val groupTitle = chat.displayTitle(uid)
                                    val sInfo = chat.participantInfo[last.senderId]
                                    val sName = sInfo?.displayName?.ifBlank { sInfo.username }?.ifBlank { null } ?: "Member"
                                    _inAppBanner.value = Pair(groupTitle, "$sName: ${last.text}")
                                } else {
                                    val other = chat.getOtherParticipant(uid)
                                    val senderName = other?.displayName?.ifBlank { "@${other.username}" } ?: "New Message"
                                    _inAppBanner.value = Pair(senderName, last.text)
                                }
                                break
                            }
                        }
                    }
                }
        }

        callHistoryListenJob?.cancel()
        callHistoryListenJob = viewModelScope.launch {
            repository.getCallLogsFlow(uid)
                .catch { Log.w("ChatViewModel", "Error in call history flow: ${it.message}") }
                .collectLatest { logs ->
                    rawCallHistory = logs
                    refreshVisibleCallHistory()
                }
        }

        nicknamesListenJob?.cancel()
        _nicknames.value = emptyMap()
        nicknamesListenJob = viewModelScope.launch {
            repository.getNicknamesFlow(uid)
                .catch { Log.e("ChatViewModel", "Error in nicknames flow: ${it.message}", it) }
                .collectLatest { map ->
                    _nicknames.value = map
                    refreshBlockedState()
                }
        }

        blockedListenJob?.cancel()
        _blockedUsersMap.value = emptyMap()
        blockedListenJob = viewModelScope.launch {
            repository.getBlockedUsersFlow(uid)
                .catch { Log.e("ChatViewModel", "Error in blocked users flow: ${it.message}", it) }
                .collectLatest { map ->
                    _blockedUsersMap.value = map
                    refreshVisibleChatsList()
                }
        }

        statusesListenJob?.cancel()
        statusesListenJob = viewModelScope.launch {
            repository.getStatusesFlow(uid)
                .catch { Log.w("ChatViewModel", "Error in statuses flow: ${it.message}") }
                .collectLatest { items ->
                    rawStatuses = items
                    syncUserProfileListeners()
                    refreshVisibleStatuses()
                }
        }
    }

    fun displayNameFor(otherUid: String, info: ParticipantInfo?): String {
        val nick = _nicknames.value[otherUid]?.trim().orEmpty()
        if (nick.isNotEmpty()) return nick
        val disp = info?.displayName?.trim().orEmpty()
        if (disp.isNotEmpty()) return disp
        val uname = info?.username?.trim().orEmpty()
        if (uname.isNotEmpty()) return uname
        return "Member"
    }

    fun saveNickname(otherUid: String, text: String) {
        val myUid = _currentUser.value?.uid ?: return
        val trimmed = text.trim()
        viewModelScope.launch {
            repository.setNickname(myUid, otherUid, trimmed).onFailure { err ->
                _toastMessage.value = "Failed to save nickname: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun togglePinChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        val currentStates = _chatLocalStates.value
        val shouldPin = chatIds.any { currentStates[it]?.pinned != true }
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(pinned = shouldPin)
        }
        refreshVisibleChatsList()
    }

    fun blockChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        val currentUid = _currentUser.value?.uid ?: return
        val targets = mutableListOf<Pair<String, ParticipantInfo>>()
        for (chatId in chatIds) {
            val chat = rawChatsList.firstOrNull { it.chatId == chatId }
            if (chat?.isGroup == true) continue
            val otherUid = chat?.participantIds?.firstOrNull { it != currentUid }
                ?: chatId.split("_").firstOrNull { it.isNotBlank() && it != currentUid }
                .orEmpty()
            if (otherUid.isNotEmpty()) {
                val info = chat?.getOtherParticipant(currentUid) ?: ParticipantInfo(uid = otherUid)
                targets.add(otherUid to info)
            }
        }
        if (targets.isEmpty()) return

        val blockedIdsToAdd = targets.map { it.first }.toSet()
        rawChatsList = rawChatsList.map { chat ->
            if (!chat.isGroup && chat.chatId in chatIds && currentUid !in chat.blockedBy) {
                chat.copy(blockedBy = chat.blockedBy + currentUid)
            } else {
                chat
            }
        }
        _blockedUsersMap.update { current ->
            val mutable = current.toMutableMap()
            targets.forEach { (uid, info) -> mutable[uid] = info }
            mutable
        }
        _currentUser.update { user ->
            user?.copy(blockedUsers = (user.blockedUsers + blockedIdsToAdd).distinct())
        }
        refreshVisibleChatsList()

        viewModelScope.launch {
            for ((otherUid, info) in targets) {
                repository.blockUser(currentUid, otherUid, info).onFailure { err ->
                    _toastMessage.value = "Failed to block: ${err.message ?: "unknown error"}"
                }
            }
        }
    }

    fun blockUser(otherUid: String, otherInfo: ParticipantInfo? = null) {
        val currentUid = _currentUser.value?.uid ?: return
        if (otherUid.isBlank() || otherUid == currentUid) return
        val chatId = ChatRepository.getDirectChatId(currentUid, otherUid)
        val resolvedInfo = otherInfo
            ?: rawChatsList.firstOrNull { it.chatId == chatId && !it.isGroup }?.getOtherParticipant(currentUid)
            ?: _activeRecipient.value?.takeIf { it.uid == otherUid }
            ?: ParticipantInfo(uid = otherUid)

        rawChatsList = rawChatsList.map { chat ->
            if (!chat.isGroup && chat.chatId == chatId && currentUid !in chat.blockedBy) {
                chat.copy(blockedBy = chat.blockedBy + currentUid)
            } else {
                chat
            }
        }
        _blockedUsersMap.update { it + (otherUid to resolvedInfo) }
        _currentUser.update { user ->
            user?.copy(blockedUsers = (user.blockedUsers + otherUid).distinct())
        }
        refreshVisibleChatsList()

        viewModelScope.launch {
            repository.blockUser(currentUid, otherUid, resolvedInfo).onFailure { err ->
                _toastMessage.value = "Failed to block: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun unblockUser(otherUid: String) {
        val currentUid = _currentUser.value?.uid ?: return
        if (otherUid.isBlank()) return
        val chatId = ChatRepository.getDirectChatId(currentUid, otherUid)

        rawChatsList = rawChatsList.map { chat ->
            if (!chat.isGroup && (chat.chatId == chatId || otherUid in chat.participantIds)) {
                chat.copy(blockedBy = chat.blockedBy - currentUid)
            } else {
                chat
            }
        }
        _blockedUsersMap.update { it - otherUid }
        _currentUser.update { user ->
            user?.copy(blockedUsers = user.blockedUsers - otherUid)
        }
        refreshVisibleChatsList()

        viewModelScope.launch {
            repository.unblockUser(currentUid, otherUid).onFailure { err ->
                _toastMessage.value = "Failed to unblock: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun isActiveChatBlocked(): Boolean {
        val activeChatId = _activeChatId.value
        if (!activeChatId.isNullOrBlank()) {
            val activeChat = rawChatsList.firstOrNull { it.chatId == activeChatId }
            if (activeChat?.isGroup == true) return false
        }
        return _isActiveChatBlockedByMe.value || _isActiveChatBlockedByOther.value
    }

    fun isUserOrChatBlocked(otherUid: String): Boolean {
        if (otherUid.isBlank()) return false
        if (otherUid in _blockedUserIds.value) return true
        val currentUid = _currentUser.value?.uid.orEmpty()
        val chatId = if (currentUid.isNotEmpty()) ChatRepository.getDirectChatId(currentUid, otherUid) else ""
        val chat = rawChatsList.firstOrNull { !it.isGroup && (it.chatId == chatId || otherUid in it.participantIds) }
        return chat?.blockedBy?.isNotEmpty() == true
    }

    fun hideDeletedChatsLocally(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val groupChatIds = rawChatsList
            .filter { it.chatId in chatIds && it.isGroup }
            .map { it.chatId }
            .toSet()
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(hiddenAt = now, pinned = false, archived = false)
        }
        refreshVisibleChatsList()
        val currentUid = _currentUser.value?.uid ?: return
        val directChatIds = chatIds - groupChatIds
        if (directChatIds.isNotEmpty()) {
            viewModelScope.launch {
                for (chatId in directChatIds) {
                    val chat = rawChatsList.firstOrNull { it.chatId == chatId }
                    if (chat?.isGroup == true) continue
                    repository.deleteChatForMe(chatId, currentUid)
                }
            }
        }
    }

    fun leaveGroup(chatId: String) {
        if (chatId.isBlank()) return
        val currentUid = _currentUser.value?.uid ?: return
        selfRemovedGroupChatIds.add(chatId)
        knownGroupChatIds.remove(chatId)
        val now = System.currentTimeMillis()
        _chatLocalStates.value = localChatPrefs.updateChats(setOf(chatId)) {
            it.copy(hiddenAt = now, pinned = false, archived = false)
        }
        rawChatsList = rawChatsList.filterNot { it.chatId == chatId }
        if (_activeChatId.value == chatId) {
            navigateBackFromConversation()
        }
        refreshVisibleChatsList()
        viewModelScope.launch {
            repository.leaveGroup(chatId, currentUid).onFailure { err ->
                selfRemovedGroupChatIds.remove(chatId)
                _toastMessage.value = "Failed to leave group: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun renameGroup(chatId: String, newName: String) {
        if (chatId.isBlank()) return
        val trimmedName = newName.trim().take(40)
        if (trimmedName.isBlank()) {
            _toastMessage.value = "Group name cannot be empty"
            return
        }
        viewModelScope.launch {
            repository.renameGroup(chatId, trimmedName)
                .onSuccess {
                    _toastMessage.value = "Group name updated"
                }
                .onFailure { err ->
                    _toastMessage.value = "Failed to rename group: ${err.message ?: "unknown error"}"
                }
        }
    }

    fun deleteGroup(chatId: String) {
        if (chatId.isBlank()) return
        selfRemovedGroupChatIds.add(chatId)
        viewModelScope.launch {
            repository.deleteGroup(chatId)
                .onSuccess {
                    knownGroupChatIds.remove(chatId)
                    rawChatsList = rawChatsList.filterNot { it.chatId == chatId }
                    refreshVisibleChatsList()
                    if (_activeChatId.value == chatId) {
                        navigateBackFromConversation()
                    }
                    _toastMessage.value = "Group deleted"
                }
                .onFailure { err ->
                    selfRemovedGroupChatIds.remove(chatId)
                    _toastMessage.value = "Failed to delete group: ${err.message ?: "unknown error"}"
                }
        }
    }

    fun muteChats(chatIds: Set<String>, mutedUntil: Long) {
        if (chatIds.isEmpty()) return
        val endTime = if (mutedUntil == Long.MAX_VALUE) {
            com.google.firebase.Timestamp(4102444800L, 0)
        } else {
            com.google.firebase.Timestamp(java.util.Date(mutedUntil))
        }
        _mutedChats.update { current ->
            val next = current.toMutableMap()
            for (cid in chatIds) {
                next[cid] = endTime
            }
            next
        }
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(mutedUntil = mutedUntil)
        }
        refreshVisibleChatsList()
        val myUid = _currentUser.value?.uid?.trim().orEmpty()
        if (myUid.isNotEmpty()) {
            viewModelScope.launch {
                for (cid in chatIds) {
                    repository.setMutedUntil(myUid, cid, endTime)
                }
            }
        }
    }

    fun unmuteChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        _mutedChats.update { current ->
            val next = current.toMutableMap()
            for (cid in chatIds) {
                next.remove(cid)
            }
            next
        }
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(mutedUntil = 0L)
        }
        refreshVisibleChatsList()
        val myUid = _currentUser.value?.uid?.trim().orEmpty()
        if (myUid.isNotEmpty()) {
            viewModelScope.launch {
                for (cid in chatIds) {
                    repository.setMutedUntil(myUid, cid, null)
                }
            }
        }
    }

    fun lockChats(chatIds: Set<String>, locked: Boolean = true) {
        if (chatIds.isEmpty()) return
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(locked = locked)
        }
        refreshVisibleChatsList()
    }

    fun unlockChats(chatIds: Set<String>) {
        lockChats(chatIds, false)
    }

    fun setLockedChatsUnlocked(unlocked: Boolean) {
        lockedChatsUnlocked.value = unlocked
    }

    fun openLockedChats() {
        lockedChatsUnlocked.value = true
    }

    fun closeLockedChats() {
        lockedChatsUnlocked.value = false
    }

    fun navigateBackFromLockedChats() {
        lockedChatsUnlocked.value = false
    }

    fun archiveChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(archived = true)
        }
        refreshVisibleChatsList()
    }

    fun unarchiveChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(archived = false)
        }
        refreshVisibleChatsList()
    }

    fun markChatsAsRead(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(markedUnread = false)
        }
        refreshVisibleChatsList()
        val currentUid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            for (chatId in chatIds) {
                val chat = rawChatsList.firstOrNull { it.chatId == chatId }
                if (chat?.isGroup == true) continue
                repository.markMessagesAsRead(chatId, currentUid)
            }
        }
    }

    fun markChatsAsUnread(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(markedUnread = true)
        }
        refreshVisibleChatsList()
    }

    fun toggleFavouriteChats(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        val currentStates = _chatLocalStates.value
        val allFavourite = chatIds.all { currentStates[it]?.favourite == true }
        _chatLocalStates.value = localChatPrefs.updateChats(chatIds) {
            it.copy(favourite = !allFavourite)
        }
        refreshVisibleChatsList()
    }

    fun selectFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
        refreshVisibleChatsList()
    }

    fun createList(name: String, chatIds: List<String>): Boolean {
        val trimmed = name.trim().take(30)
        if (trimmed.isEmpty() || chatIds.isEmpty()) return false
        val current = _customLists.value
        if (current.any { it.name.equals(trimmed, ignoreCase = true) }) {
            _toastMessage.value = "A list with this name already exists"
            return false
        }
        val newList = ChatList(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            chatIds = chatIds.distinct()
        )
        val updated = current + newList
        _customLists.value = updated
        localChatPrefs.saveCustomLists(updated)
        refreshVisibleChatsList()
        return true
    }

    fun updateList(id: String, name: String, chatIds: List<String>): Boolean {
        val trimmed = name.trim().take(30)
        if (trimmed.isEmpty() || chatIds.isEmpty()) return false
        val current = _customLists.value
        if (current.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) {
            _toastMessage.value = "A list with this name already exists"
            return false
        }
        val updated = current.map { existing ->
            if (existing.id == id) {
                ChatList(id = id, name = trimmed, chatIds = chatIds.distinct())
            } else {
                existing
            }
        }
        _customLists.value = updated
        localChatPrefs.saveCustomLists(updated)
        refreshVisibleChatsList()
        return true
    }

    fun deleteList(id: String) {
        val updated = _customLists.value.filterNot { it.id == id }
        _customLists.value = updated
        localChatPrefs.saveCustomLists(updated)
        val currentFilter = _selectedFilter.value
        if (currentFilter is ChatFilter.Custom && currentFilter.listId == id) {
            _selectedFilter.value = ChatFilter.All
        }
        refreshVisibleChatsList()
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun dismissInAppBanner() {
        _inAppBanner.value = null
    }

    fun selectTab(tab: ChatTab) {
        _activeTab.value = tab
        _activeSubScreen.value = ChatSubScreen.TABS
        if (tab == ChatTab.UPDATES) {
            refreshStatuses()
        }
    }

    fun openSettings() {
        _activeSubScreen.value = ChatSubScreen.SETTINGS
    }

    fun navigateBackFromSettings() {
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    fun closeSettings() {
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    fun openSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _activeSubScreen.value = ChatSubScreen.SEARCH
    }

    fun openArchived() {
        _activeSubScreen.value = ChatSubScreen.ARCHIVED
    }

    fun navigateBackFromArchived() {
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    private fun isUserInLockedChat(otherUid: String, currentUid: String): Boolean {
        if (otherUid.isBlank() || currentUid.isBlank()) return false
        val states = _chatLocalStates.value
        val directChatId = ChatRepository.getDirectChatId(currentUid, otherUid)
        if (states[directChatId]?.locked == true) return true
        return rawChatsList.any { chat ->
            !chat.isGroup && otherUid in chat.participantIds && states[chat.chatId]?.locked == true
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val currentUid = _currentUser.value?.uid ?: return
        val trimmed = query.trim().lowercase()

        searchJob?.cancel()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchUsers(trimmed, currentUid)
            _searchResults.value = results.filterNot { user ->
                isUserInLockedChat(user.uid, currentUid)
            }
            syncUserProfileListeners()
            _isSearching.value = false
        }
    }

    fun createGroup(name: String, members: List<ChatUser>, onDone: (String?) -> Unit = {}) {
        val current = _currentUser.value ?: run {
            _toastMessage.value = "Please sign in to create a group"
            onDone(null)
            return
        }
        val trimmedName = name.trim().take(40)
        if (trimmedName.isBlank()) {
            _toastMessage.value = "Group name cannot be empty"
            onDone(null)
            return
        }
        val validMembers = members.filter { it.uid.isNotBlank() && it.uid != current.uid }.distinctBy { it.uid }
        if (validMembers.isEmpty()) {
            _toastMessage.value = "Select at least one member"
            onDone(null)
            return
        }

        viewModelScope.launch {
            var filteredAllowed: List<ChatUser> = validMembers
            var filteredSkipped: List<ChatUser> = emptyList()
            val result = repository.createGroupChat(
                currentUid = current.uid,
                currentUser = current,
                name = trimmedName,
                members = validMembers,
                onMembersFiltered = { allowed, skipped ->
                    filteredAllowed = allowed
                    filteredSkipped = skipped
                }
            )
            result.onSuccess { chatId ->
                val actualMembers = filteredAllowed.ifEmpty { validMembers }
                if (filteredSkipped.isNotEmpty()) {
                    val skippedNames = filteredSkipped.joinToString(", ") {
                        it.displayName.ifBlank { "@${it.username}" }
                    }
                    _toastMessage.value = "Skipped $skippedNames due to their group privacy settings"
                }
                val participantIds = (listOf(current.uid) + actualMembers.map { it.uid }).distinct()
                val participantInfo = buildMap {
                    put(
                        current.uid,
                        ParticipantInfo(
                            uid = current.uid,
                            username = current.username,
                            displayName = current.displayName,
                            photoUrl = null
                        )
                    )
                    for (member in actualMembers) {
                        put(
                            member.uid,
                            ParticipantInfo(
                                uid = member.uid,
                                username = member.username,
                                displayName = member.displayName,
                                photoUrl = null
                            )
                        )
                    }
                }
                val newSummary = ChatSummary(
                    chatId = chatId,
                    type = "group",
                    name = trimmedName,
                    createdBy = current.uid,
                    participantIds = participantIds,
                    participantInfo = participantInfo,
                    lastMessage = null,
                    createdAt = com.google.firebase.Timestamp.now(),
                    ghostMode = false,
                    blockedBy = emptyList()
                )
                knownGroupChatIds.add(chatId)
                if (rawChatsList.none { it.chatId == chatId }) {
                    rawChatsList = listOf(newSummary) + rawChatsList
                    refreshVisibleChatsList()
                }
                val summaryToOpen = rawChatsList.firstOrNull { it.chatId == chatId } ?: newSummary
                openChatFromSummary(summaryToOpen)
                onDone(chatId)
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Failed to create group"
                onDone(null)
            }
        }
    }

    fun openDirectChatWithUser(otherUser: ChatUser) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.getOrCreateDirectChat(
                currentUid = current.uid,
                currentUser = current,
                otherUid = otherUser.uid,
                otherUser = otherUser
            )
            result.onSuccess { chatId ->
                stopTyping()
                recipientTypingExpiryJob?.cancel()
                recipientTypingExpiryJob = null
                _recipientTyping.value = false
                _activeChatId.value = chatId
                _activeRecipient.value = ParticipantInfo(
                    uid = otherUser.uid,
                    username = otherUser.username,
                    displayName = otherUser.displayName,
                    photoUrl = null
                )
                _activeSubScreen.value = ChatSubScreen.CONVERSATION
                _ghostModeEnabled.value = false
                refreshActiveChatBlockFlags()
                startListeningToMessages(chatId, isGroup = false)
                startObservingRecipientPresence(otherUser.uid)
                launch {
                    repository.clearChatDeletedForMe(chatId, current.uid)
                }
                if (_showOnlineStatus.value) {
                    repository.markOnlineIfForeground(current.uid)
                }
            }.onFailure { err ->
                _toastMessage.value = "Failed to start chat: ${err.message}"
            }
        }
    }

    fun openChatFromSummary(summary: ChatSummary) {
        val currentUid = _currentUser.value?.uid ?: return
        val isGroup = summary.isGroup
        val other = if (isGroup) null else (summary.getOtherParticipant(currentUid) ?: return)
        if (_chatLocalStates.value[summary.chatId]?.markedUnread == true) {
            _chatLocalStates.value = localChatPrefs.updateChats(listOf(summary.chatId)) {
                it.copy(markedUnread = false)
            }
            refreshVisibleChatsList()
        }
        stopTyping()
        presenceListenJob?.cancel()
        peerKeyListenJob?.cancel()
        recipientTypingExpiryJob?.cancel()
        recipientTypingExpiryJob = null
        _recipientTyping.value = false
        _recipientPresence.value = null
        _activeChatId.value = summary.chatId
        _activeRecipient.value = other
        _activeSubScreen.value = ChatSubScreen.CONVERSATION
        _ghostModeEnabled.value = if (isGroup) false else summary.ghostMode
        refreshActiveChatBlockFlags()
        startListeningToMessages(summary.chatId, isGroup = isGroup)
        if (!isGroup && other != null) {
            startObservingRecipientPresence(other.uid)
            viewModelScope.launch {
                repository.clearChatDeletedForMe(summary.chatId, currentUid)
            }
        }
        if (_showOnlineStatus.value) {
            viewModelScope.launch {
                repository.markOnlineIfForeground(currentUid)
            }
        }
    }

    private fun startObservingRecipientPresence(uid: String) {
        presenceListenJob?.cancel()
        peerKeyListenJob?.cancel()
        recipientTypingExpiryJob?.cancel()
        recipientTypingExpiryJob = null
        _recipientTyping.value = false
        _recipientPresence.value = null
        peerKeyListenJob = viewModelScope.launch {
            repository.observePeerPublicKey(uid)
                .catch { Log.w(TAG, "Error observing peer public key for $uid: ${it.message}") }
                .collectLatest { }
        }
        presenceListenJob = viewModelScope.launch {
            observeMergedOtherUser(uid).collectLatest { user ->
                _recipientPresence.value = user
                if (user != null) {
                    _userProfiles.update { it + (uid to user) }
                }
                refreshActiveChatBlockFlags()
                val activeChatId = _activeChatId.value
                val currentUid = _currentUser.value?.uid.orEmpty()
                val activeChat = if (!activeChatId.isNullOrBlank()) {
                    rawChatsList.firstOrNull { it.chatId == activeChatId }
                } else null
                val otherUid = _activeRecipient.value?.uid?.ifBlank { null }
                    ?: activeChat?.participantIds?.firstOrNull { it != currentUid }.orEmpty()
                val isBlocked = _isActiveChatBlockedByMe.value ||
                    _isActiveChatBlockedByOther.value ||
                    (otherUid.isNotEmpty() && otherUid in _blockedUserIds.value) ||
                    (currentUid.isNotEmpty() && activeChat?.blockedBy?.contains(currentUid) == true) ||
                    (otherUid.isNotEmpty() && activeChat?.blockedBy?.contains(otherUid) == true)

                val nowMs = System.currentTimeMillis()
                val typingAtMs = user?.typingAt?.toDate()?.time
                val remainingMs = if (typingAtMs != null) (typingAtMs + 6_000L) - nowMs else 0L
                val isTyping = user != null &&
                    !activeChatId.isNullOrBlank() &&
                    user.typingChatId == activeChatId &&
                    typingAtMs != null &&
                    remainingMs > 0L &&
                    canSeeUserLastSeen(user) &&
                    !isBlocked

                recipientTypingExpiryJob?.cancel()
                recipientTypingExpiryJob = null
                if (isTyping) {
                    _recipientTyping.value = true
                    val waitMs = remainingMs.coerceAtMost(6_000L)
                    recipientTypingExpiryJob = viewModelScope.launch {
                        delay(waitMs)
                        _recipientTyping.value = false
                    }
                } else {
                    _recipientTyping.value = false
                }
            }
        }
    }

    fun setShowOnlineStatus(enabled: Boolean) {
        setLastSeenVisibility(if (enabled) "everyone" else "nobody")
    }

    fun setLastSeenVisibility(visibility: String) {
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        val enabled = normalized != "nobody"
        preferences.isOnlineStatusEnabled = enabled
        _showOnlineStatus.value = enabled
        if (!enabled) {
            stopTyping()
        }
        val uid = _currentUser.value?.uid ?: return
        _currentUser.update { it?.copy(lastSeenVisibility = normalized, showOnlineStatus = enabled) }
        _userProfiles.update { profiles ->
            val existing = profiles[uid] ?: _currentUser.value
            if (existing != null) profiles + (uid to existing.copy(lastSeenVisibility = normalized, showOnlineStatus = enabled)) else profiles
        }
        if (enabled) {
            startPresenceHeartbeat(uid)
        } else {
            stopPresenceHeartbeat()
        }
        viewModelScope.launch {
            repository.setLastSeenVisibility(uid, normalized).onFailure { err ->
                _toastMessage.value = "Failed to update last seen privacy: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun setPhotoVisibility(visibility: String) {
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        val uid = _currentUser.value?.uid ?: return
        _currentUser.update { it?.copy(photoVisibility = normalized) }
        _userProfiles.update { profiles ->
            val existing = profiles[uid] ?: _currentUser.value
            if (existing != null) profiles + (uid to existing.copy(photoVisibility = normalized)) else profiles
        }
        viewModelScope.launch {
            repository.setPhotoVisibility(uid, normalized).onFailure { err ->
                _toastMessage.value = "Failed to update profile photo privacy: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun setAboutVisibility(visibility: String) {
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        val uid = _currentUser.value?.uid ?: return
        _currentUser.update { it?.copy(aboutVisibility = normalized) }
        _userProfiles.update { profiles ->
            val existing = profiles[uid] ?: _currentUser.value
            if (existing != null) profiles + (uid to existing.copy(aboutVisibility = normalized)) else profiles
        }
        viewModelScope.launch {
            repository.setAboutVisibility(uid, normalized).onFailure { err ->
                _toastMessage.value = "Failed to update about privacy: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun setStatusPrivacy(
        visibility: String,
        exceptIds: List<String> = _currentUser.value?.statusExceptIds.orEmpty(),
        onlyIds: List<String> = _currentUser.value?.statusOnlyIds.orEmpty()
    ) {
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("contacts", "except", "only")) it else "contacts"
        }
        val cleanExcept = exceptIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val cleanOnly = onlyIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val uid = _currentUser.value?.uid ?: return
        _currentUser.update {
            it?.copy(
                statusVisibility = normalized,
                statusExceptIds = cleanExcept,
                statusOnlyIds = cleanOnly
            )
        }
        _userProfiles.update { profiles ->
            val existing = profiles[uid] ?: _currentUser.value
            if (existing != null) {
                profiles + (uid to existing.copy(
                    statusVisibility = normalized,
                    statusExceptIds = cleanExcept,
                    statusOnlyIds = cleanOnly
                ))
            } else profiles
        }
        viewModelScope.launch {
            repository.setStatusPrivacy(uid, normalized, cleanExcept, cleanOnly).onFailure { err ->
                _toastMessage.value = "Failed to update status privacy: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun setGroupsVisibility(visibility: String) {
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts")) it else "everyone"
        }
        val uid = _currentUser.value?.uid ?: return
        _currentUser.update { it?.copy(groupsVisibility = normalized) }
        _userProfiles.update { profiles ->
            val existing = profiles[uid] ?: _currentUser.value
            if (existing != null) profiles + (uid to existing.copy(groupsVisibility = normalized)) else profiles
        }
        viewModelScope.launch {
            repository.setGroupsVisibility(uid, normalized).onFailure { err ->
                _toastMessage.value = "Failed to update groups privacy: ${err.message ?: "unknown error"}"
            }
        }
    }

    fun setReadReceipts(enabled: Boolean) {
        preferences.isReadReceiptsEnabled = enabled
        _readReceipts.value = enabled
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            repository.setReadReceipts(uid, enabled)
        }
        if (enabled) {
            maybeMarkActiveChatMessagesAsRead()
        }
    }

    private fun maybeMarkActiveChatMessagesAsRead() {
        val chatId = _activeChatId.value ?: return
        if (_activeSubScreen.value != ChatSubScreen.CONVERSATION) return
        val activeChat = rawChatsList.firstOrNull { it.chatId == chatId }
        if (activeChat?.isGroup == true) return
        val currentUid = _currentUser.value?.uid ?: return
        val msgs = _messages.value
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        if (inForeground &&
            _readReceipts.value &&
            msgs.any { it.senderId != currentUid && it.status != "read" }
        ) {
            markReadJob?.cancel()
            markReadJob = viewModelScope.launch {
                delay(300L)
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                    _readReceipts.value &&
                    _activeChatId.value == chatId
                ) {
                    repository.markMessagesAsRead(chatId, currentUid)
                }
            }
        }
    }

    private fun startListeningToMessages(chatId: String, isGroup: Boolean = false) {
        messagesListenJob?.cancel()
        markReadJob?.cancel()
        _messages.value = emptyList()
        val currentUid = _currentUser.value?.uid ?: return
        val isGroupChat = isGroup || rawChatsList.firstOrNull { it.chatId == chatId }?.isGroup == true
        val peerUid = if (isGroupChat) null else _activeRecipient.value?.uid

        messagesListenJob = viewModelScope.launch {
            repository.getChatMessagesFlow(
                chatId = chatId,
                currentUid = currentUid,
                isGroup = isGroupChat,
                peerUid = peerUid
            ).collectLatest { msgs ->
                _messages.value = msgs
                if (!isGroupChat) {
                    val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                        .isAtLeast(Lifecycle.State.STARTED)
                    if (inForeground &&
                        _readReceipts.value &&
                        msgs.any { it.senderId != currentUid && it.status != "read" }
                    ) {
                        delay(300L)
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                            _readReceipts.value
                        ) {
                            repository.markMessagesAsRead(chatId, currentUid)
                        }
                    }
                }
            }
        }
    }

    private fun stopTyping() {
        typingIdleJob?.cancel()
        typingIdleJob = null
        val prevChatId = lastTypingChatId ?: return
        lastTypingChatId = null
        lastTypingWriteMs = 0L
        val currentUid = _currentUser.value?.uid?.ifBlank { null }
            ?: SafeFirebase.getAuth(context)?.currentUser?.uid
            ?: preferences.lastUid
        if (!currentUid.isNullOrBlank() && prevChatId.isNotEmpty()) {
            viewModelScope.launch(NonCancellable) {
                repository.setTypingState(currentUid, null)
            }
        }
    }

    fun updateMessageInput(input: String) {
        _messageInput.value = input
        val activeChatId = _activeChatId.value
        val currentUid = _currentUser.value?.uid
        val isGroupChat = !activeChatId.isNullOrBlank() &&
            rawChatsList.firstOrNull { it.chatId == activeChatId }?.isGroup == true
        if (input.isBlank() ||
            activeChatId.isNullOrBlank() ||
            currentUid.isNullOrBlank() ||
            isGroupChat ||
            isActiveChatBlocked() ||
            !_showOnlineStatus.value
        ) {
            stopTyping()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastTypingWriteMs >= 3_000L || lastTypingChatId != activeChatId) {
            lastTypingChatId = activeChatId
            lastTypingWriteMs = now
            viewModelScope.launch {
                repository.setTypingState(currentUid, activeChatId)
            }
        }

        typingIdleJob?.cancel()
        typingIdleJob = viewModelScope.launch {
            delay(4_000L)
            stopTyping()
        }
    }

    fun sendMessage() {
        stopTyping()
        val text = _messageInput.value.trim()
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (text.isEmpty()) return
        if (isActiveChatBlocked()) {
            _toastMessage.value = "You can't send messages to this contact. Unblock to continue."
            return
        }
        val isGroupChat = rawChatsList.firstOrNull { it.chatId == chatId }?.isGroup == true
        val peerUid = if (isGroupChat) null else _activeRecipient.value?.uid
        if (!isGroupChat && !peerUid.isNullOrBlank() && peerUid in changedPeerUids.value) {
            val peerName = displayNameFor(peerUid, _activeRecipient.value)
            _toastMessage.value = "$peerName's security key changed. Accept the new key to continue."
            return
        }

        _messageInput.value = ""
        viewModelScope.launch {
            _isSendingMessage.value = true
            val result = repository.sendMessage(chatId, currentUid, text)
            result.onFailure { err ->
                if (!peerUid.isNullOrBlank() && peerUid in changedPeerUids.value && _messageInput.value.isEmpty()) {
                    _messageInput.value = text
                }
                _toastMessage.value = "Failed to send: ${err.message}"
            }
            _isSendingMessage.value = false
        }
    }

    fun startEditingMessage(message: ChatMessage) {
        _editingMessageId.value = message.id
        _messageInput.value = message.text
    }

    fun cancelEditingMessage() {
        _editingMessageId.value = null
        _messageInput.value = ""
    }

    fun saveEditedMessage() {
        val chatId = _activeChatId.value ?: return
        val messageId = _editingMessageId.value ?: return
        val text = _messageInput.value.trim()
        if (text.isEmpty()) return
        val isGroupChat = rawChatsList.firstOrNull { it.chatId == chatId }?.isGroup == true
        val peerUid = if (isGroupChat) null else _activeRecipient.value?.uid
        if (!isGroupChat && !peerUid.isNullOrBlank() && peerUid in changedPeerUids.value) {
            val peerName = displayNameFor(peerUid, _activeRecipient.value)
            _toastMessage.value = "$peerName's security key changed. Accept the new key to continue."
            return
        }
        _messageInput.value = ""
        _editingMessageId.value = null
        viewModelScope.launch {
            repository.editMessage(chatId, messageId, text).onFailure { err ->
                _toastMessage.value = "Failed to edit: ${err.message}"
            }
        }
    }

    fun deleteMessageForEveryone(messageId: String) {
        val chatId = _activeChatId.value ?: return
        if (_editingMessageId.value == messageId) {
            cancelEditingMessage()
        }
        _messages.update { list ->
            list.map { if (it.id == messageId) it.copy(isDeleted = true, text = "") else it }
        }
        viewModelScope.launch {
            repository.deleteMessageForEveryone(chatId, messageId).onFailure { err ->
                _toastMessage.value = "Failed to delete: ${err.message}"
            }
        }
    }

    fun deleteMessageForMe(messageId: String) {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (_editingMessageId.value == messageId) {
            cancelEditingMessage()
        }
        _messages.update { list -> list.filterNot { it.id == messageId } }
        viewModelScope.launch {
            repository.deleteMessageForMe(chatId, messageId, currentUid).onFailure { err ->
                _toastMessage.value = "Failed to delete: ${err.message}"
            }
        }
    }

    fun toggleGhostMode(enabled: Boolean) {
        val chatId = _activeChatId.value ?: return
        val activeChat = rawChatsList.firstOrNull { it.chatId == chatId }
        if (activeChat?.isGroup == true) return
        _ghostModeEnabled.value = enabled
        viewModelScope.launch {
            repository.setGhostMode(chatId, enabled)
        }
    }

    // Active upload progress by message or temporary ID: progress in 0f..1f
    private val _uploadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Float>> = _uploadProgress.asStateFlow()

    // Failed uploads map: ID -> retry action
    private val _failedUploads = MutableStateFlow<Map<String, () -> Unit>>(emptyMap())
    val failedUploads: StateFlow<Map<String, () -> Unit>> = _failedUploads.asStateFlow()

    fun uploadAndSendMedia(mediaType: String, fileUri: Uri, context: Context, caption: String = "") {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (isActiveChatBlocked()) {
            _toastMessage.value = "You can't send messages to this contact. Unblock to continue."
            return
        }
        val pendingId = "pending_${System.currentTimeMillis()}"

        val executeUpload: () -> Unit = {
            _failedUploads.update { it - pendingId }
            _uploadProgress.update { it + (pendingId to 0.05f) }

            viewModelScope.launch {
                val uploadCategory = when (mediaType) {
                    "video" -> "videos"
                    "document" -> "documents"
                    else -> "images"
                }

                val uploadRes = ChatStorageManager.uploadFile(
                    chatId = chatId,
                    mediaType = uploadCategory,
                    fileUri = fileUri,
                    context = context,
                    onProgress = { prog ->
                        _uploadProgress.update { it + (pendingId to prog) }
                    }
                )

                uploadRes.fold(
                    onSuccess = { downloadUrl ->
                        _uploadProgress.update { it - pendingId }
                        val (fileName, fileSize) = ChatStorageManager.queryFileDetails(context, fileUri)
                        val mime = ChatStorageManager.getMimeType(context, fileUri)

                        repository.sendAttachmentMessage(
                            chatId = chatId,
                            senderId = currentUid,
                            messageType = mediaType,
                            textFallback = caption,
                            mediaUrl = downloadUrl,
                            fileName = fileName,
                            fileSize = fileSize,
                            mimeType = mime
                        )
                    },
                    onFailure = { err ->
                        Log.e(TAG, "Media upload failed: ${err.message}", err)
                        _uploadProgress.update { it - pendingId }
                        _toastMessage.value = "Upload failed. Please tap retry."
                    }
                )
            }
        }

        executeUpload()
    }

    fun uploadAndSendVoiceNote(file: File, waveform: List<Int>, durationSeconds: Long) {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (isActiveChatBlocked()) {
            try { file.delete() } catch (_: Exception) {}
            _toastMessage.value = "You can't send messages to this contact. Unblock to continue."
            return
        }
        val pendingId = "voice_${System.currentTimeMillis()}"

        val executeUpload: () -> Unit = {
            _failedUploads.update { it - pendingId }
            _uploadProgress.update { it + (pendingId to 0.05f) }

            viewModelScope.launch {
                try {
                    val bytes = file.readBytes()
                    val fileName = file.name
                    val uploadRes = ChatStorageManager.uploadBytes(
                        chatId = chatId,
                        mediaType = "voice",
                        fileName = fileName,
                        bytes = bytes,
                        mimeType = "audio/mp4",
                        context = context,
                        onProgress = { prog ->
                            _uploadProgress.update { it + (pendingId to prog) }
                        }
                    )

                    uploadRes.fold(
                        onSuccess = { downloadUrl ->
                            _uploadProgress.update { it - pendingId }
                            try { file.delete() } catch (_: Exception) {}
                            repository.sendAttachmentMessage(
                                chatId = chatId,
                                senderId = currentUid,
                                messageType = "voice",
                                mediaUrl = downloadUrl,
                                durationSeconds = durationSeconds,
                                waveform = waveform,
                                fileSize = bytes.size.toLong(),
                                mimeType = "audio/mp4"
                            )
                        },
                        onFailure = { err ->
                            Log.e(TAG, "Voice note upload failed: ${err.message}", err)
                            _uploadProgress.update { it - pendingId }
                            _toastMessage.value = "Voice note upload failed"
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading voice note: ${e.message}", e)
                    _uploadProgress.update { it - pendingId }
                }
            }
        }

        executeUpload()
    }

    fun sendLocation(latitude: Double, longitude: Double, address: String) {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (isActiveChatBlocked()) {
            _toastMessage.value = "You can't send messages to this contact. Unblock to continue."
            return
        }
        viewModelScope.launch {
            repository.sendAttachmentMessage(
                chatId = chatId,
                senderId = currentUid,
                messageType = "location",
                latitude = latitude,
                longitude = longitude,
                locationAddress = address,
                textFallback = address
            )
        }
    }

    fun sendContact(name: String, phone: String) {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (isActiveChatBlocked()) {
            _toastMessage.value = "You can't send messages to this contact. Unblock to continue."
            return
        }
        viewModelScope.launch {
            repository.sendAttachmentMessage(
                chatId = chatId,
                senderId = currentUid,
                messageType = "contact",
                contactName = name,
                contactPhone = phone,
                textFallback = name
            )
        }
    }

    fun notifyCallsComingSoon() {
        _toastMessage.value = "Calls are coming soon"
    }

    fun navigateBackFromConversation() {
        stopTyping()
        val chatId = _activeChatId.value
        val wasGhostMode = _ghostModeEnabled.value
        val isGroup = chatId != null && rawChatsList.firstOrNull { it.chatId == chatId }?.isGroup == true
        if (chatId != null && wasGhostMode && !isGroup) {
            viewModelScope.launch {
                repository.deleteReadMessages(chatId)
            }
        }
        _activeChatId.value = null
        _activeRecipient.value = null
        _ghostModeEnabled.value = false
        messagesListenJob?.cancel()
        markReadJob?.cancel()
        presenceListenJob?.cancel()
        peerKeyListenJob?.cancel()
        recipientTypingExpiryJob?.cancel()
        recipientTypingExpiryJob = null
        _recipientTyping.value = false
        _recipientPresence.value = null
        _messages.value = emptyList()
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    fun navigateBackFromSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.isNotificationsEnabled = enabled
        _notificationsEnabled.value = enabled
        _toastMessage.value = if (enabled) "Chat notifications enabled" else "Chat notifications silenced"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearUsernameError() {
        _usernameError.value = null
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        val uid = _currentUser.value?.uid
            ?: SafeFirebase.getAuth(context)?.currentUser?.uid
            ?: preferences.lastUid
        stopTyping()
        stopPresenceHeartbeat()
        com.example.chat.call.ZegoCallManager.unInit()
        preferences.lastUid = null
        preferences.lastUsername = null
        preferences.pendingFcmToken = null
        chatsListenJob?.cancel()
        callHistoryListenJob?.cancel()
        statusesListenJob?.cancel()
        nicknamesListenJob?.cancel()
        blockedListenJob?.cancel()
        clearUserProfileListeners()
        hasLoadedChatsOnce = false
        knownGroupChatIds.clear()
        selfRemovedGroupChatIds.clear()
        rawStatuses = emptyList()
        _groupChats.value = emptyList()
        _myStatuses.value = emptyList()
        _contactStatusGroups.value = emptyList()
        _pendingOpenChatId.value = null
        _nicknames.value = emptyMap()
        _blockedUsersMap.value = emptyMap()
        _blockedUserIds.value = emptySet()
        _blockedContacts.value = emptyList()
        _userProfiles.value = emptyMap()
        _contactUserIds.value = emptySet()
        _directContacts.value = emptyList()
        _isActiveChatBlockedByMe.value = false
        _isActiveChatBlockedByOther.value = false
        _lockedChatsList.value = emptyList()
        lockedChatsUnlocked.value = false
        _currentUser.value = null
        _activeChatId.value = null
        _activeRecipient.value = null
        presenceListenJob?.cancel()
        peerKeyListenJob?.cancel()
        recipientTypingExpiryJob?.cancel()
        recipientTypingExpiryJob = null
        _recipientTyping.value = false
        markReadJob?.cancel()
        _recipientPresence.value = null
        _messages.value = emptyList()
        viewModelScope.launch(NonCancellable) {
            if (!uid.isNullOrBlank()) {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    if (!token.isNullOrBlank()) {
                        repository.removeFcmToken(uid, token)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove FCM token on sign-out: ${e.message}", e)
                }
                try {
                    repository.setOnlinePresence(uid, false)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set offline presence on sign-out: ${e.message}", e)
                }
            }
            onComplete?.invoke()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        stopPresenceHeartbeat()
        clearUserProfileListeners()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}

class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
