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
    val showOnlineStatus: Boolean = true,
    val readReceipts: Boolean = true,
    val blockedUsers: List<String> = emptyList(),
    val typingChatId: String? = null,
    val typingAt: Timestamp? = null
)

fun isUserConsideredOnline(isOnline: Boolean, lastSeenAt: Timestamp?): Boolean {
    if (!isOnline) return false
    if (lastSeenAt == null) return false
    val elapsed = System.currentTimeMillis() - lastSeenAt.toDate().time
    return elapsed < 90_000L
}

fun formatLastSeen(timestamp: Timestamp?): String {
    if (timestamp == null) return "offline"
    val date = timestamp.toDate()
    val nowMs = System.currentTimeMillis()
    val diffMs = nowMs - date.time

    if (diffMs in -5_000L..59_999L) {
        return "last seen just now"
    }
    val diffMinutes = diffMs / 60_000L
    if (diffMinutes in 1..59) {
        return "last seen ${diffMinutes}m ago"
    }

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
    val dateFmt = java.text.SimpleDateFormat("MMM d 'at' h:mm a", java.util.Locale.getDefault())
    return "last seen on ${dateFmt.format(date)}"
}

fun formatLastSeenCompact(timestamp: Timestamp?): String {
    if (timestamp == null) return "offline"
    val date = timestamp.toDate()
    val nowMs = System.currentTimeMillis()
    val diffMs = nowMs - date.time

    if (diffMs in -5_000L..59_999L) {
        return "last seen just now"
    }

    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { time = date }
    val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val isToday = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    if (isToday) return "last seen ${timeFmt.format(date)}"
    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        yesterday.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) return "last seen yesterday ${timeFmt.format(date)}"
    val dateFmt = java.text.SimpleDateFormat("MMM d h:mm a", java.util.Locale.getDefault())
    return "last seen ${dateFmt.format(date)}"
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
    val timestamp: Timestamp? = null,
    val messageId: String = "",
    val isDeleted: Boolean = false,
    val deletedFor: List<String> = emptyList()
)

data class ChatSummary(
    val chatId: String = "",
    val type: String = "direct",
    val name: String? = null,
    val createdBy: String? = null,
    val participantIds: List<String> = emptyList(),
    val participantInfo: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessage: ChatLastMessage? = null,
    val createdAt: Timestamp? = null,
    val ghostMode: Boolean = false,
    val blockedBy: List<String> = emptyList()
) {
    val isGroup: Boolean
        get() = type == "group"

    fun getOtherParticipant(currentUid: String): ParticipantInfo? {
        if (isGroup) return null
        val otherUid = participantIds.firstOrNull { it != currentUid } ?: return null
        return participantInfo[otherUid]
    }

    fun displayTitle(currentUid: String): String {
        if (isGroup) {
            return name?.trim()?.ifBlank { null } ?: "Group"
        }
        val other = getOtherParticipant(currentUid)
        return other?.displayName?.ifBlank { other.username }?.ifBlank { null } ?: "Member"
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
    val deletedFor: List<String> = emptyList(),
    // Message type: "text" | "call" | "image" | "video" | "document" | "voice" | "location" | "contact"
    val messageType: String = "text",
    // Call attributes
    val callType: String = "voice",   // "voice" | "video"
    val callOutcome: String = "ended", // "connected" | "missed" | "declined" | "no_answer" | "failed"
    val callDurationSeconds: Long = 0L,
    val callCallerId: String = "",
    val callCalleeId: String = "",
    // Media & file attachments ("image", "video", "document", "voice")
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val durationSeconds: Long? = null,
    val waveform: List<Int>? = null,
    // Location ("location")
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null,
    // Contact ("contact")
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null
)

enum class ChatTab(val title: String) {
    CHATS("Chats"),
    CALLS("Calls"),
    GROUPS("Groups"),
    UPDATES("Updates");

    companion object {
        fun fromNameOrChats(name: String?): ChatTab {
            if (name.isNullOrBlank()) return CHATS
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CHATS
        }
    }
}

enum class ChatSubScreen {
    TABS,
    SEARCH,
    CONVERSATION,
    ARCHIVED,
    SETTINGS
}

data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val text: String = "",
    val backgroundColor: String = "#0F766E",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val viewerIds: List<String> = emptyList()
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        val expMs = expiresAt?.toDate()?.time
        if (expMs != null) return expMs <= nowMs
        val createdMs = createdAt?.toDate()?.time ?: return false
        return (nowMs - createdMs) >= 24L * 60L * 60L * 1000L
    }
}

data class UserStatusGroup(
    val userId: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val statuses: List<StatusItem>,
    val latestTimestamp: Timestamp?
)

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
