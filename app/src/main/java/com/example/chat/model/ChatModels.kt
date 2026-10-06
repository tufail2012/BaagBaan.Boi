package com.example.chat.model

import com.google.firebase.Timestamp

data class ChatUser(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val fcmToken: String? = null,
    val lastSeenAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val isOnline: Boolean = false,
    val showOnlineStatus: Boolean = true
)

fun formatLastSeen(timestamp: Timestamp?): String {
    if (timestamp == null) return "offline"
    val date = timestamp.toDate()
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { time = date }
    val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val isToday = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    if (isToday) return "last seen today at ${timeFmt.format(date)}"
    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        yesterday.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) return "last seen yesterday at ${timeFmt.format(date)}"
    val dateFmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    return "last seen on ${dateFmt.format(date)}"
}

data class ParticipantInfo(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
)

data class ChatLastMessage(
    val text: String = "",
    val senderId: String = "",
    val timestamp: Timestamp? = null
)

data class ChatSummary(
    val chatId: String = "",
    val type: String = "direct",
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessage: ChatLastMessage? = null,
    val createdAt: Timestamp? = null,
    val ghostMode: Boolean = false
) {
    fun getOtherParticipant(currentUid: String): ParticipantInfo? {
        val otherUid = participantIds.firstOrNull { it != currentUid } ?: return null
        return participantInfo[otherUid]
    }
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val status: String = "sent",
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedFor: List<String> = emptyList()
)

enum class ChatTab(val title: String) {
    CHATS("Chats"),
    CALLS("Calls"),
    GROUPS("Groups"),
    SETTINGS("Settings")
}

enum class ChatSubScreen {
    TABS,
    SEARCH,
    CONVERSATION
}

data class CallLogItem(
    val id: String = "",
    val callId: String = "",
    val type: String = "voice", // "voice" | "video"
    val callerId: String = "",
    val calleeId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, ParticipantInfo> = emptyMap(),
    val chatId: String? = null,
    val status: String = "ringing", // "ringing" | "answered" | "declined" | "missed" | "ended"
    val startedAt: Timestamp? = null,
    val connectedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val durationSeconds: Long? = null
) {
    fun getOtherParticipant(currentUid: String): ParticipantInfo? {
        val otherUid = participantIds.firstOrNull { it != currentUid } ?: return null
        return participantInfo[otherUid]
    }
}
