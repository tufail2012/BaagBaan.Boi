package com.example.chat.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.data.ChatPreferences
import com.example.chat.data.ChatRepository
import com.example.chat.data.ChatStorageManager
import com.example.chat.model.ChatMessage
import com.example.chat.model.ChatSubScreen
import com.example.chat.model.ChatSummary
import com.example.chat.model.ChatTab
import com.example.chat.model.ChatUser
import com.example.chat.model.ParticipantInfo
import com.example.util.SafeFirebase
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import android.net.Uri

class ChatViewModel(
    private val context: Context,
    private val repository: ChatRepository = ChatRepository(context)
) : ViewModel() {

    private val preferences = ChatPreferences(context)

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

    private val _chatsList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatsList: StateFlow<List<ChatSummary>> = _chatsList.asStateFlow()

    private val _isChatsLoading = MutableStateFlow(false)
    val isChatsLoading: StateFlow<Boolean> = _isChatsLoading.asStateFlow()

    // Real call history flow for Calls tab
    private val _callHistory = MutableStateFlow<List<com.example.chat.model.CallLogItem>>(emptyList())
    val callHistory: StateFlow<List<com.example.chat.model.CallLogItem>> = _callHistory.asStateFlow()
    private var callHistoryListenJob: Job? = null

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

    private val _recipientPresence = MutableStateFlow<ChatUser?>(null)
    val recipientPresence: StateFlow<ChatUser?> = _recipientPresence.asStateFlow()

    private var chatsListenJob: Job? = null
    private var messagesListenJob: Job? = null
    private var searchJob: Job? = null
    private var presenceListenJob: Job? = null

    init {
        checkUserSession()
    }

    fun checkUserSession() {
        val authUser = SafeFirebase.getAuth(context)?.currentUser
        if (authUser == null) {
            _currentUser.value = null
            _isUserLoading.value = false
            _isUsernameSetupRequired.value = false
            return
        }

        viewModelScope.launch {
            _isUserLoading.value = true
            val existing = repository.getChatUser(authUser.uid)
            if (existing != null && existing.username.isNotBlank()) {
                _currentUser.value = existing
                _isUsernameSetupRequired.value = false
                preferences.lastUid = authUser.uid
                preferences.lastUsername = existing.username
                preferences.isOnlineStatusEnabled = existing.showOnlineStatus
                _showOnlineStatus.value = existing.showOnlineStatus
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
                _currentUser.value = user
                _isUsernameSetupRequired.value = false
                _toastMessage.value = "Welcome, @${user.username}!"
                preferences.lastUid = user.uid
                preferences.lastUsername = user.username
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
                (context.applicationContext as? android.app.Application)?.let { app ->
                    com.example.chat.call.ZegoCallManager.init(app, current.uid, updatedUsername)
                }
                onComplete(true)
            }.onFailure { err ->
                _usernameError.value = err.message ?: "Failed to change username"
                onComplete(false)
            }
            _isSubmittingUsername.value = false
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
                    val previousList = _chatsList.value
                    _chatsList.value = list

                    // In-app notification check for foreground messages
                    if (previousList.isNotEmpty() && preferences.isNotificationsEnabled) {
                        for (chat in list) {
                            val last = chat.lastMessage ?: continue
                            val prevChat = previousList.find { it.chatId == chat.chatId }
                            val prevTime = prevChat?.lastMessage?.timestamp?.toDate()?.time ?: 0L
                            val currTime = last.timestamp?.toDate()?.time ?: 0L

                            // If new message from other user and not currently inside this conversation
                            if (currTime > prevTime && last.senderId != uid && _activeChatId.value != chat.chatId) {
                                val other = chat.getOtherParticipant(uid)
                                val senderName = other?.displayName?.ifBlank { "@${other.username}" } ?: "New Message"
                                _inAppBanner.value = Pair(senderName, last.text)
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
                    _callHistory.value = logs
                }
        }
    }

    fun dismissInAppBanner() {
        _inAppBanner.value = null
    }

    fun selectTab(tab: ChatTab) {
        _activeTab.value = tab
        _activeSubScreen.value = ChatSubScreen.TABS
    }

    fun openSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _activeSubScreen.value = ChatSubScreen.SEARCH
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
            _searchResults.value = results
            _isSearching.value = false
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
                _activeChatId.value = chatId
                _activeRecipient.value = ParticipantInfo(
                    uid = otherUser.uid,
                    username = otherUser.username,
                    displayName = otherUser.displayName,
                    photoUrl = otherUser.photoUrl
                )
                _activeSubScreen.value = ChatSubScreen.CONVERSATION
                _ghostModeEnabled.value = false
                startListeningToMessages(chatId)
                startObservingRecipientPresence(otherUser.uid)
            }.onFailure { err ->
                _toastMessage.value = "Failed to start chat: ${err.message}"
            }
        }
    }

    fun openChatFromSummary(summary: ChatSummary) {
        val currentUid = _currentUser.value?.uid ?: return
        val other = summary.getOtherParticipant(currentUid) ?: return
        _activeChatId.value = summary.chatId
        _activeRecipient.value = other
        _activeSubScreen.value = ChatSubScreen.CONVERSATION
        _ghostModeEnabled.value = summary.ghostMode
        startListeningToMessages(summary.chatId)
        startObservingRecipientPresence(other.uid)
    }

    private fun startObservingRecipientPresence(uid: String) {
        presenceListenJob?.cancel()
        _recipientPresence.value = null
        presenceListenJob = viewModelScope.launch {
            repository.getUserFlow(uid).collectLatest { user ->
                _recipientPresence.value = user
            }
        }
    }

    fun setShowOnlineStatus(enabled: Boolean) {
        preferences.isOnlineStatusEnabled = enabled
        _showOnlineStatus.value = enabled
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            repository.setShowOnlineStatus(uid, enabled)
        }
    }

    private fun startListeningToMessages(chatId: String) {
        messagesListenJob?.cancel()
        _messages.value = emptyList()
        val currentUid = _currentUser.value?.uid ?: return

        messagesListenJob = viewModelScope.launch {
            repository.getChatMessagesFlow(chatId, currentUid).collectLatest { msgs ->
                _messages.value = msgs
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            repository.markMessagesAsRead(chatId, currentUid)
        }
    }

    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    fun sendMessage() {
        val text = _messageInput.value.trim()
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        if (text.isEmpty()) return

        _messageInput.value = ""
        viewModelScope.launch {
            _isSendingMessage.value = true
            val result = repository.sendMessage(chatId, currentUid, text)
            result.onFailure { err ->
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
        viewModelScope.launch {
            repository.deleteMessageForEveryone(chatId, messageId).onFailure { err ->
                _toastMessage.value = "Failed to delete: ${err.message}"
            }
        }
    }

    fun deleteMessageForMe(messageId: String) {
        val chatId = _activeChatId.value ?: return
        val currentUid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            repository.deleteMessageForMe(chatId, messageId, currentUid).onFailure { err ->
                _toastMessage.value = "Failed to delete: ${err.message}"
            }
        }
    }

    fun toggleGhostMode(enabled: Boolean) {
        val chatId = _activeChatId.value ?: return
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
        val chatId = _activeChatId.value
        val wasGhostMode = _ghostModeEnabled.value
        if (chatId != null && wasGhostMode) {
            viewModelScope.launch {
                repository.deleteReadMessages(chatId)
            }
        }
        _activeChatId.value = null
        _activeRecipient.value = null
        _ghostModeEnabled.value = false
        messagesListenJob?.cancel()
        presenceListenJob?.cancel()
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

    fun logout() {
        com.example.chat.call.ZegoCallManager.unInit()
        preferences.lastUid = null
        preferences.lastUsername = null
        _currentUser.value = null
        _activeChatId.value = null
        _activeRecipient.value = null
        presenceListenJob?.cancel()
        _recipientPresence.value = null
        _messages.value = emptyList()
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
