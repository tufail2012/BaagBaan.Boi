package com.example.chat.data

import android.content.Context
import android.util.Log
import com.example.chat.model.ChatLastMessage
import com.example.chat.model.ChatMessage
import com.example.chat.model.ChatSummary
import com.example.chat.model.ChatUser
import com.example.chat.model.ParticipantInfo
import com.example.util.SafeFirebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val context: Context) {

    private val db: FirebaseFirestore?
        get() = SafeFirebase.getDb(context)

    companion object {
        private const val TAG = "ChatRepository"
        val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")

        fun isValidUsername(username: String): Boolean {
            return USERNAME_REGEX.matches(username.trim().lowercase())
        }

        fun getDirectChatId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }

    suspend fun getChatUser(uid: String): ChatUser? {
        val firestore = db ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val username = doc.getString("username")
                if (!username.isNullOrBlank()) {
                    ChatUser(
                        uid = uid,
                        username = username,
                        displayName = doc.getString("displayName") ?: "",
                        photoUrl = doc.getString("photoUrl"),
                        fcmToken = doc.getString("fcmToken"),
                        lastSeenAt = doc.getTimestamp("lastSeenAt"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user $uid: ${e.message}", e)
            null
        }
    }

    suspend fun registerUsername(
        uid: String,
        rawUsername: String,
        displayName: String?,
        photoUrl: String?
    ): Result<ChatUser> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val username = rawUsername.trim().lowercase()

        if (!isValidUsername(username)) {
            return Result.failure(IllegalArgumentException("Username must be 3-20 characters and contain only lowercase letters, numbers, and underscores."))
        }

        return try {
            val usernameRef = firestore.collection("usernames").document(username)
            val userRef = firestore.collection("users").document(uid)

            firestore.runTransaction { transaction ->
                val usernameDoc = transaction.get(usernameRef)
                val userDoc = transaction.get(userRef)

                if (usernameDoc.exists()) {
                    val ownerUid = usernameDoc.getString("uid")
                    if (ownerUid != uid) {
                        throw IllegalStateException("Username '$username' is already taken.")
                    }
                }

                // Reserve username
                transaction.set(usernameRef, mapOf("uid" to uid))

                if (userDoc.exists()) {
                    val updates = hashMapOf<String, Any>(
                        "username" to username,
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                    if (!displayName.isNullOrBlank()) updates["displayName"] = displayName
                    if (!photoUrl.isNullOrBlank()) updates["photoUrl"] = photoUrl
                    transaction.update(userRef, updates)
                } else {
                    val newUser = hashMapOf(
                        "uid" to uid,
                        "username" to username,
                        "displayName" to (displayName ?: ""),
                        "photoUrl" to (photoUrl ?: ""),
                        "fcmToken" to null,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(userRef, newUser)
                }
            }.await()

            val chatUser = ChatUser(
                uid = uid,
                username = username,
                displayName = displayName ?: "",
                photoUrl = photoUrl,
                lastSeenAt = Timestamp.now(),
                createdAt = Timestamp.now()
            )
            Result.success(chatUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering username: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun changeUsername(
        uid: String,
        oldUsername: String,
        newRawUsername: String
    ): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val newUsername = newRawUsername.trim().lowercase()
        val old = oldUsername.trim().lowercase()

        if (newUsername == old) {
            return Result.success(newUsername)
        }

        if (!isValidUsername(newUsername)) {
            return Result.failure(IllegalArgumentException("Username must be 3-20 characters and contain only lowercase letters, numbers, and underscores."))
        }

        return try {
            val oldRef = firestore.collection("usernames").document(old)
            val newRef = firestore.collection("usernames").document(newUsername)
            val userRef = firestore.collection("users").document(uid)

            firestore.runTransaction { transaction ->
                val newDoc = transaction.get(newRef)
                if (newDoc.exists()) {
                    val existingUid = newDoc.getString("uid")
                    if (existingUid != uid) {
                        throw IllegalStateException("Username '$newUsername' is already taken.")
                    }
                }

                if (old.isNotEmpty()) {
                    transaction.delete(oldRef)
                }
                transaction.set(newRef, mapOf("uid" to uid))
                transaction.update(
                    userRef,
                    mapOf(
                        "username" to newUsername,
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(newUsername)
        } catch (e: Exception) {
            Log.e(TAG, "Error changing username: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String, currentUid: String): List<ChatUser> {
        val firestore = db ?: return emptyList()
        val cleaned = query.trim().lowercase()
        if (cleaned.isEmpty()) return emptyList()

        return try {
            val endStr = cleaned + "\uf8ff"
            val snapshot = firestore.collection("users")
                .whereGreaterThanOrEqualTo("username", cleaned)
                .whereLessThanOrEqualTo("username", endStr)
                .limit(25)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                if (uid == currentUid) return@mapNotNull null
                val username = doc.getString("username") ?: return@mapNotNull null
                ChatUser(
                    uid = uid,
                    username = username,
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    lastSeenAt = doc.getTimestamp("lastSeenAt"),
                    createdAt = doc.getTimestamp("createdAt")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getOrCreateDirectChat(
        currentUid: String,
        currentUser: ChatUser,
        otherUid: String,
        otherUser: ChatUser
    ): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val chatId = getDirectChatId(currentUid, otherUid)
        val chatRef = firestore.collection("chats").document(chatId)

        return try {
            val doc = chatRef.get().await()
            if (!doc.exists()) {
                val newChatData = hashMapOf(
                    "type" to "direct",
                    "participantIds" to listOf(currentUid, otherUid),
                    "participantInfo" to mapOf(
                        currentUid to mapOf(
                            "username" to currentUser.username,
                            "displayName" to currentUser.displayName,
                            "photoUrl" to (currentUser.photoUrl ?: "")
                        ),
                        otherUid to mapOf(
                            "username" to otherUser.username,
                            "displayName" to otherUser.displayName,
                            "photoUrl" to (otherUser.photoUrl ?: "")
                        )
                    ),
                    "lastMessage" to null,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(newChatData).await()
            } else {
                // Ensure participantInfo is updated if either changed their username/photo
                val updates = hashMapOf<String, Any>(
                    "participantInfo.$currentUid.username" to currentUser.username,
                    "participantInfo.$currentUid.displayName" to currentUser.displayName,
                    "participantInfo.$otherUid.username" to otherUser.username,
                    "participantInfo.$otherUid.displayName" to otherUser.displayName
                )
                currentUser.photoUrl?.let { updates["participantInfo.$currentUid.photoUrl"] = it }
                otherUser.photoUrl?.let { updates["participantInfo.$otherUid.photoUrl"] = it }
                chatRef.update(updates)
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating direct chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getUserChatsFlow(currentUid: String): Flow<List<ChatSummary>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("chats")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen to user chats failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val chatId = doc.id
                            val type = doc.getString("type") ?: "direct"
                            @Suppress("UNCHECKED_CAST")
                            val participantIds = doc.get("participantIds") as? List<String> ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val pInfoRaw = doc.get("participantInfo") as? Map<String, Any?> ?: emptyMap()
                            val participantInfo = pInfoRaw.mapNotNull { (k, v) ->
                                @Suppress("UNCHECKED_CAST")
                                val map = v as? Map<String, Any?> ?: return@mapNotNull null
                                k to ParticipantInfo(
                                    uid = k,
                                    username = map["username"]?.toString() ?: "",
                                    displayName = map["displayName"]?.toString() ?: "",
                                    photoUrl = map["photoUrl"]?.toString()
                                )
                            }.toMap()

                            @Suppress("UNCHECKED_CAST")
                            val lMsgRaw = doc.get("lastMessage") as? Map<String, Any?>
                            val lastMessage = lMsgRaw?.let {
                                ChatLastMessage(
                                    text = it["text"]?.toString() ?: "",
                                    senderId = it["senderId"]?.toString() ?: "",
                                    timestamp = it["timestamp"] as? Timestamp
                                )
                            }

                            ChatSummary(
                                chatId = chatId,
                                type = type,
                                participantIds = participantIds,
                                participantInfo = participantInfo,
                                lastMessage = lastMessage,
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chat doc ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    // Sort client-side by lastMessage timestamp descending
                    val sorted = list.sortedWith { a, b ->
                        val timeA = a.lastMessage?.timestamp?.toDate()?.time ?: a.createdAt?.toDate()?.time ?: 0L
                        val timeB = b.lastMessage?.timestamp?.toDate()?.time ?: b.createdAt?.toDate()?.time ?: 0L
                        timeB.compareTo(timeA)
                    }

                    trySend(sorted)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    fun getChatMessagesFlow(chatId: String, currentUid: String): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages in chat $chatId: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChatMessage(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: "",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getTimestamp("timestamp"),
                                status = doc.getString("status") ?: "sent"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(messages)

                    val toMarkDelivered = snapshot.documents.filter { doc ->
                        doc.getString("senderId") != currentUid &&
                            (doc.getString("status") ?: "sent") == "sent"
                    }
                    if (toMarkDelivered.isNotEmpty()) {
                        val batch = firestore.batch()
                        toMarkDelivered.forEach { doc ->
                            batch.update(doc.reference, "status", "delivered")
                        }
                        batch.commit()
                    }
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun markMessagesAsRead(chatId: String, currentUid: String) {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .await()

            val unread = snapshot.documents.filter { doc ->
                doc.getString("senderId") != currentUid &&
                    (doc.getString("status") ?: "sent") != "read"
            }
            if (unread.isEmpty()) return

            val batch = firestore.batch()
            unread.forEach { doc ->
                batch.update(doc.reference, "status", "read")
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read in $chatId: ${e.message}", e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        text: String
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)

        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            val messageRef = chatRef.collection("messages").document()

            val messageData = hashMapOf(
                "senderId" to senderId,
                "text" to trimmed,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "sent"
            )

            val lastMessageData = hashMapOf(
                "text" to trimmed,
                "senderId" to senderId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            val batch = firestore.batch()
            batch.set(messageRef, messageData)
            batch.update(chatRef, "lastMessage", lastMessageData)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message in $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
