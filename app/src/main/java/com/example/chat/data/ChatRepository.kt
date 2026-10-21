package com.example.chat.data

import android.content.Context
import android.util.Log
import com.example.chat.crypto.E2eeManager
import com.example.chat.model.CallLogItem
import com.example.chat.model.ChatLastMessage
import com.example.chat.model.ChatMessage
import com.example.chat.model.ChatSummary
import com.example.chat.model.ChatUser
import com.example.chat.model.ParticipantInfo
import com.example.chat.model.PresenceInfo
import com.example.chat.model.StatusItem
import com.example.chat.model.UserPrivate
import com.example.chat.notifications.PushRelay
import com.example.util.SafeFirebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class ChatRepository(private val context: Context) {

    private val db: FirebaseFirestore?
        get() = SafeFirebase.getDb(context)

    val e2eeManager: E2eeManager = E2eeManager.getInstance(context)
    val changedPeerUids: StateFlow<Set<String>>
        get() = e2eeManager.changedPeerUids

    val keyState: StateFlow<E2eeManager.KeyState>
        get() = e2eeManager.keyState

    suspend fun ensureE2eeIdentity(uid: String): E2eeManager.KeyState {
        return e2eeManager.ensureIdentity(uid)
    }

    suspend fun claimThisDevice(uid: String): E2eeManager.KeyState {
        return e2eeManager.claimThisDevice(uid)
    }

    suspend fun fetchPeerKey(peerUid: String): E2eeManager.PeerKeyResult {
        return e2eeManager.fetchPeerKey(peerUid)
    }

    fun acceptPeerKey(peerUid: String) {
        e2eeManager.acceptNewKey(peerUid)
    }

    fun observePeerPublicKey(peerUid: String): Flow<E2eeManager.PeerKeyResult> = callbackFlow {
        val firestore = db
        val cleanPeer = peerUid.trim()
        if (firestore == null || cleanPeer.isEmpty()) {
            trySend(E2eeManager.PeerKeyResult.Missing)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection(E2eeManager.COLLECTION_PUBLIC_KEYS)
            .document(cleanPeer)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "PublicKey listener for $cleanPeer failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val pubKey = doc.getString("publicKey")?.trim().orEmpty()
                    if (pubKey.isNotEmpty()) {
                        trySend(e2eeManager.evaluateServerPeerKey(cleanPeer, pubKey))
                    } else {
                        trySend(E2eeManager.PeerKeyResult.Missing)
                    }
                } else {
                    trySend(E2eeManager.PeerKeyResult.Missing)
                }
            }
        awaitClose { listener.remove() }
    }

    private fun extractPeerUidFromDirectChatId(chatId: String, myUid: String): String {
        val cleanChatId = chatId.trim()
        val cleanMyUid = myUid.trim()
        if (cleanChatId.isEmpty() || cleanMyUid.isEmpty()) return ""
        return when {
            cleanChatId.startsWith("${cleanMyUid}_") -> cleanChatId.removePrefix("${cleanMyUid}_")
            cleanChatId.endsWith("_${cleanMyUid}") -> cleanChatId.removeSuffix("_${cleanMyUid}")
            else -> cleanChatId.split("_").firstOrNull { it.isNotBlank() && it != cleanMyUid }.orEmpty()
        }
    }

    companion object {
        private const val TAG = "ChatRepository"
        private val presenceMutex = Mutex()
        val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")

        private val LEGACY_USER_KEYS = listOf(
            "photoUrl",
            "aboutText",
            "fcmToken",
            "fcmTokens",
            "blockedUsers",
            "lastSeenAt",
            "isOnline",
            "showOnlineStatus",
            "readReceipts",
            "typingChatId",
            "typingAt",
            "lastSeenVisibility",
            "photoVisibility",
            "aboutVisibility",
            "groupAddVisibility",
            "statusMode",
            "statusUids",
            "statusVisibility",
            "statusExcept",
            "statusOnly",
            "about",
            "groupsVisibility",
            "statusExceptIds",
            "statusOnlyIds",
            "statusExcludedIds",
            "statusAllowedIds"
        )

        private val ALLOWED_PUBLIC_USER_KEYS = setOf(
            "uid",
            "username",
            "usernameLower",
            "displayName",
            "createdAt",
            "updatedAt"
        )

        fun isValidUsername(username: String): Boolean {
            return USERNAME_REGEX.matches(username.trim().lowercase())
        }

        fun getDirectChatId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }

    private fun parsePublicChatUser(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        fallbackUid: String = doc.id
    ): ChatUser? {
        if (!doc.exists()) return null
        val uid = (doc.getString("uid") ?: fallbackUid).trim()
        val username = doc.getString("username")?.trim().orEmpty()
        if (uid.isEmpty() || username.isEmpty()) return null

        return ChatUser(
            uid = uid,
            username = username,
            displayName = doc.getString("displayName") ?: "",
            createdAt = doc.getTimestamp("createdAt"),
            updatedAt = doc.getTimestamp("updatedAt")
        )
    }

    suspend fun migrateOwnUserSplit(uid: String) {
        val firestore = db ?: return
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return

        val userRef = firestore.collection("users").document(cleanUid)
        val userSnap = try {
            userRef.get().await()
        } catch (e: Exception) {
            Log.w(TAG, "migrateOwnUserSplit: failed to read users/$cleanUid: ${e.message}", e)
            return
        }

        val data = if (userSnap.exists()) userSnap.data.orEmpty() else emptyMap()
        val hasLegacyKeys = data.keys.any { it in LEGACY_USER_KEYS || it !in ALLOWED_PUBLIC_USER_KEYS }

        // 1. Copy private fields to userPrivate/{uid} or initialize defaults if missing
        try {
            val privRef = firestore.collection("userPrivate").document(cleanUid)
            val privSnap = privRef.get().await()
            val privExists = privSnap.exists()
            if (hasLegacyKeys || !privExists) {
                val privUpdates = hashMapOf<String, Any?>()

                val listTokens = (data["fcmTokens"] as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    .orEmpty()
                val singleToken = (data["fcmToken"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                val mergedTokens = (listTokens + listOfNotNull(singleToken)).distinct()
                if ("fcmTokens" in data || "fcmToken" in data) {
                    if (mergedTokens.isNotEmpty()) {
                        privUpdates["fcmTokens"] = FieldValue.arrayUnion(*mergedTokens.toTypedArray())
                    } else if (!privExists) {
                        privUpdates["fcmTokens"] = emptyList<String>()
                    }
                } else if (!privExists) {
                    privUpdates["fcmTokens"] = emptyList<String>()
                }

                if ("blockedUsers" in data) {
                    val blocked = (data["blockedUsers"] as? List<*>)
                        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty()
                    privUpdates["blockedUsers"] = blocked
                } else if (!privExists) {
                    privUpdates["blockedUsers"] = emptyList<String>()
                }

                if ("readReceipts" in data) {
                    privUpdates["readReceipts"] = data["readReceipts"] as? Boolean ?: true
                } else if (!privExists) {
                    privUpdates["readReceipts"] = true
                }

                if ("showOnlineStatus" in data) {
                    privUpdates["showOnlineStatus"] = data["showOnlineStatus"] as? Boolean ?: true
                } else if (!privExists) {
                    privUpdates["showOnlineStatus"] = true
                }

                if ("lastSeenVisibility" in data) {
                    val v = (data["lastSeenVisibility"] as? String)?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") } ?: "everyone"
                    privUpdates["lastSeenVisibility"] = v
                } else if (!privExists) {
                    privUpdates["lastSeenVisibility"] = "everyone"
                }

                if ("photoVisibility" in data) {
                    val v = (data["photoVisibility"] as? String)?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") } ?: "everyone"
                    privUpdates["photoVisibility"] = v
                } else if (!privExists) {
                    privUpdates["photoVisibility"] = "everyone"
                }

                if ("aboutVisibility" in data) {
                    val v = (data["aboutVisibility"] as? String)?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") } ?: "everyone"
                    privUpdates["aboutVisibility"] = v
                } else if (!privExists) {
                    privUpdates["aboutVisibility"] = "everyone"
                }

                if ("groupAddVisibility" in data || "groupsVisibility" in data) {
                    val raw = (data["groupAddVisibility"] as? String) ?: (data["groupsVisibility"] as? String)
                    val v = raw?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts") } ?: "everyone"
                    privUpdates["groupAddVisibility"] = v
                } else if (!privExists) {
                    privUpdates["groupAddVisibility"] = "everyone"
                }

                if ("statusMode" in data || "statusVisibility" in data) {
                    val raw = ((data["statusMode"] as? String) ?: (data["statusVisibility"] as? String))
                        ?.trim()?.lowercase()
                    val mode = when (raw) {
                        "except", "exceptuids" -> "exceptUids"
                        "only", "onlyuids" -> "onlyUids"
                        else -> "contacts"
                    }
                    privUpdates["statusMode"] = mode
                } else if (!privExists) {
                    privUpdates["statusMode"] = "contacts"
                }

                val hasStatusUidsInLegacy = "statusUids" in data ||
                    "statusExceptIds" in data || "statusOnlyIds" in data ||
                    "statusExcept" in data || "statusOnly" in data
                if (hasStatusUidsInLegacy) {
                    val mode = (privUpdates["statusMode"] as? String) ?: "contacts"
                    val rawList = when (mode) {
                        "exceptUids" -> (data["statusUids"] ?: data["statusExceptIds"] ?: data["statusExcept"]) as? List<*>
                        "onlyUids" -> (data["statusUids"] ?: data["statusOnlyIds"] ?: data["statusOnly"]) as? List<*>
                        else -> data["statusUids"] as? List<*>
                    }
                    privUpdates["statusUids"] = rawList
                        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty()
                } else if (!privExists) {
                    privUpdates["statusUids"] = emptyList<String>()
                }

                if (privUpdates.isNotEmpty()) {
                    privUpdates["updatedAt"] = FieldValue.serverTimestamp()
                    privRef.set(privUpdates, SetOptions.merge()).await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "migrateOwnUserSplit: userPrivate migration failed for $cleanUid: ${e.message}", e)
        }

        if (!userSnap.exists()) return

        // 2. Copy presence fields to presence/{uid}
        try {
            val presenceUpdates = hashMapOf<String, Any?>()
            if ("isOnline" in data) {
                presenceUpdates["isOnline"] = data["isOnline"] as? Boolean ?: false
            }
            if ("lastSeenAt" in data && data["lastSeenAt"] != null) {
                presenceUpdates["lastSeenAt"] = data["lastSeenAt"]
            }
            if ("typingChatId" in data) {
                presenceUpdates["typingChatId"] = data["typingChatId"] as? String
            }
            if ("typingAt" in data && data["typingAt"] != null) {
                presenceUpdates["typingAt"] = data["typingAt"]
            }
            if (presenceUpdates.isNotEmpty()) {
                firestore.collection("presence").document(cleanUid)
                    .set(presenceUpdates, SetOptions.merge())
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "migrateOwnUserSplit: presence migration failed for $cleanUid: ${e.message}", e)
        }

        // 3. Copy photoUrl to photos/{uid}
        try {
            val legacyPhoto = (data["photoUrl"] as? String)?.trim()
            if (!legacyPhoto.isNullOrEmpty()) {
                firestore.collection("photos").document(cleanUid)
                    .set(
                        mapOf(
                            "photoUrl" to legacyPhoto,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "migrateOwnUserSplit: photos migration failed for $cleanUid: ${e.message}", e)
        }

        // 4. Copy aboutText / about to abouts/{uid}
        try {
            val legacyAbout = ((data["aboutText"] as? String) ?: (data["about"] as? String))
                ?.trim()
                ?.take(139)
            if (!legacyAbout.isNullOrEmpty()) {
                firestore.collection("abouts").document(cleanUid)
                    .set(
                        mapOf(
                            "aboutText" to legacyAbout,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "migrateOwnUserSplit: abouts migration failed for $cleanUid: ${e.message}", e)
        }

        // 5. Remove all legacy keys from users/{uid} in a single update
        if (hasLegacyKeys) {
            try {
                val deleteMap = hashMapOf<String, Any>()
                for (key in LEGACY_USER_KEYS) {
                    deleteMap[key] = FieldValue.delete()
                }
                for (key in data.keys) {
                    if (key !in ALLOWED_PUBLIC_USER_KEYS) {
                        deleteMap[key] = FieldValue.delete()
                    }
                }
                userRef.update(deleteMap).await()
            } catch (e: Exception) {
                Log.w(TAG, "migrateOwnUserSplit: cleanup of users/$cleanUid failed: ${e.message}", e)
            }
        }
    }

    suspend fun getChatUser(uid: String): ChatUser? {
        val firestore = db ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            parsePublicChatUser(doc, uid)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user $uid: ${e.message}", e)
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
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!displayName.isNullOrBlank()) updates["displayName"] = displayName
                    val existingData = userDoc.data.orEmpty()
                    for (key in existingData.keys) {
                        if (key !in ALLOWED_PUBLIC_USER_KEYS) {
                            updates[key] = FieldValue.delete()
                        }
                    }
                    transaction.update(userRef, updates)
                } else {
                    val newUser = hashMapOf<String, Any>(
                        "uid" to uid,
                        "username" to username,
                        "displayName" to (displayName ?: ""),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(userRef, newUser)
                }
            }.await()

            if (!photoUrl.isNullOrBlank()) {
                try {
                    firestore.collection("photos").document(uid)
                        .set(
                            mapOf(
                                "photoUrl" to photoUrl,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error saving photo doc for $uid: ${e.message}", e)
                }
            }

            val chatUser = ChatUser(
                uid = uid,
                username = username,
                displayName = displayName ?: "",
                photoUrl = photoUrl,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
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
                        "updatedAt" to FieldValue.serverTimestamp()
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
                parsePublicChatUser(doc, uid)
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
                            "displayName" to currentUser.displayName
                        ),
                        otherUid to mapOf(
                            "username" to otherUser.username,
                            "displayName" to otherUser.displayName
                        )
                    ),
                    "lastMessage" to null,
                    "ghostMode" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(newChatData).await()
            } else {
                // Ensure participantInfo has updated username and displayName only
                val updates = hashMapOf<String, Any>(
                    "participantInfo.$currentUid.username" to currentUser.username,
                    "participantInfo.$currentUid.displayName" to currentUser.displayName,
                    "participantInfo.$otherUid.username" to otherUser.username,
                    "participantInfo.$otherUid.displayName" to otherUser.displayName
                )
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

        var latestSnapshot: com.google.firebase.firestore.QuerySnapshot? = null
        var mapJob: Job? = null

        fun processSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
            mapJob?.cancel()
            mapJob = launch {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val chatId = doc.id
                        val type = doc.getString("type") ?: "direct"
                        val name = doc.getString("name")
                        val createdBy = doc.getString("createdBy")
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
                                photoUrl = null
                            )
                        }.toMap()

                        @Suppress("UNCHECKED_CAST")
                        val lMsgRaw = doc.get("lastMessage") as? Map<String, Any?>
                        val lastMessage = if (lMsgRaw != null) {
                            val isDeleted = lMsgRaw["isDeleted"] as? Boolean ?: false
                            val deletedFor = (lMsgRaw["deletedFor"] as? List<*>)
                                ?.mapNotNull { item -> item as? String }
                                ?: emptyList()
                            val senderId = lMsgRaw["senderId"]?.toString() ?: ""
                            val timestamp = lMsgRaw["timestamp"] as? Timestamp
                            val messageId = lMsgRaw["messageId"]?.toString() ?: ""
                            val ciphertext = lMsgRaw["ciphertext"]?.toString()
                            val iv = lMsgRaw["iv"]?.toString()

                            val previewText = if (isDeleted) {
                                ""
                            } else if (!ciphertext.isNullOrBlank()) {
                                val peerUid = participantIds.firstOrNull { it.isNotBlank() && it != currentUid }
                                    ?: extractPeerUidFromDirectChatId(chatId, currentUid)
                                if (!e2eeManager.hasLocalPrivateKey(currentUid)) {
                                    Log.w(TAG, "Decrypt failed: no local private key (chatId=$chatId, messageId=$messageId)")
                                    E2eeManager.DecryptFailureReason.NO_LOCAL_PRIVATE_KEY.placeholder
                                } else if (type != "group" && peerUid.isNotBlank()) {
                                    when (val keyRes = e2eeManager.resolveChatKey(chatId, currentUid, peerUid)) {
                                        is E2eeManager.ChatKeyResult.NoLocalKey -> {
                                            Log.w(TAG, "Decrypt failed: no local private key (chatId=$chatId, messageId=$messageId)")
                                            E2eeManager.DecryptFailureReason.NO_LOCAL_PRIVATE_KEY.placeholder
                                        }
                                        is E2eeManager.ChatKeyResult.Changed -> {
                                            Log.w(TAG, "Decrypt failed: peer key changed (chatId=$chatId, messageId=$messageId)")
                                            E2eeManager.DecryptFailureReason.WRONG_KEY_OR_GCM_TAG.placeholder
                                        }
                                        is E2eeManager.ChatKeyResult.Missing -> {
                                            Log.w(TAG, "Decrypt failed: peer key missing (chatId=$chatId, messageId=$messageId)")
                                            E2eeManager.DecryptFailureReason.OTHER.placeholder
                                        }
                                        is E2eeManager.ChatKeyResult.Success -> {
                                            if (!iv.isNullOrBlank() && messageId.isNotBlank()) {
                                                when (
                                                    val decRes = e2eeManager.decryptTextDetailed(
                                                        chatKey = keyRes.chatKey,
                                                        chatId = chatId,
                                                        messageId = messageId,
                                                        ciphertextB64 = ciphertext,
                                                        ivB64 = iv
                                                    )
                                                ) {
                                                    is E2eeManager.DecryptResult.Success -> decRes.plaintext
                                                    is E2eeManager.DecryptResult.Failure -> {
                                                        if (decRes.reason == E2eeManager.DecryptFailureReason.WRONG_KEY_OR_GCM_TAG) {
                                                            e2eeManager.fetchPeerKey(peerUid)
                                                        }
                                                        Log.w(
                                                            TAG,
                                                            "Decrypt failed: ${decRes.reason} (${decRes.detail}) (chatId=$chatId, messageId=$messageId)"
                                                        )
                                                        decRes.reason.placeholder
                                                    }
                                                }
                                            } else {
                                                Log.w(TAG, "Decrypt failed: missing iv or messageId (chatId=$chatId, messageId=$messageId)")
                                                E2eeManager.DecryptFailureReason.OTHER.placeholder
                                            }
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Decrypt failed: invalid direct chat context (chatId=$chatId, messageId=$messageId)")
                                    E2eeManager.DecryptFailureReason.OTHER.placeholder
                                }
                            } else {
                                lMsgRaw["text"]?.toString() ?: ""
                            }

                            ChatLastMessage(
                                text = previewText,
                                senderId = senderId,
                                timestamp = timestamp,
                                messageId = messageId,
                                isDeleted = isDeleted,
                                deletedFor = deletedFor
                            )
                        } else {
                            null
                        }

                        val blockedBy = (doc.get("blockedBy") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()

                        ChatSummary(
                            chatId = chatId,
                            type = type,
                            name = name,
                            createdBy = createdBy,
                            participantIds = participantIds,
                            participantInfo = participantInfo,
                            lastMessage = lastMessage,
                            createdAt = doc.getTimestamp("createdAt"),
                            ghostMode = doc.getBoolean("ghostMode") ?: false,
                            blockedBy = blockedBy
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

        val keyAcceptedJob = launch {
            e2eeManager.keyAcceptedEvents.collectLatest {
                latestSnapshot?.let { snap -> processSnapshot(snap) }
            }
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
                    latestSnapshot = snapshot
                    processSnapshot(snapshot)
                }
            }

        awaitClose {
            keyAcceptedJob.cancel()
            mapJob?.cancel()
            listener.remove()
        }
    }

    suspend fun createGroupChat(
        currentUid: String,
        currentUser: ChatUser,
        name: String,
        members: List<ChatUser>,
        onMembersFiltered: ((allowed: List<ChatUser>, skipped: List<ChatUser>) -> Unit)? = null
    ): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (currentUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Current user ID is blank"))
        }
        val trimmedName = name.trim().take(40)
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        val validMembers = members.filter { it.uid.isNotBlank() && it.uid != currentUid }.distinctBy { it.uid }
        if (validMembers.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one member"))
        }

        return try {
            val myBlocked = currentUser.blockedUsers.toSet()
            val allowedMembers = validMembers.filter { it.uid !in myBlocked }
            val skippedMembers = validMembers.filter { it.uid in myBlocked }

            onMembersFiltered?.invoke(allowedMembers, skippedMembers)

            if (allowedMembers.isEmpty()) {
                val skippedNames = skippedMembers.joinToString(", ") {
                    it.displayName.ifBlank { "@${it.username}" }.ifBlank { "user" }
                }
                return Result.failure(
                    IllegalStateException("Couldn't add $skippedNames to group")
                )
            }

            val participantIds = (listOf(currentUid) + allowedMembers.map { it.uid }).distinct()
            val participantInfo = mutableMapOf<String, Map<String, Any?>>()
            participantInfo[currentUid] = mapOf(
                "username" to currentUser.username,
                "displayName" to currentUser.displayName
            )
            for (member in allowedMembers) {
                participantInfo[member.uid] = mapOf(
                    "username" to member.username,
                    "displayName" to member.displayName
                )
            }

            val chatRef = firestore.collection("chats").document()
            val newGroupData = hashMapOf(
                "type" to "group",
                "name" to trimmedName,
                "createdBy" to currentUid,
                "participantIds" to participantIds,
                "participantInfo" to participantInfo,
                "lastMessage" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "ghostMode" to false,
                "blockedBy" to emptyList<String>()
            )
            chatRef.set(newGroupData).await()
            Result.success(chatRef.id)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "Group creation rejected by privacy rules: ${e.message}")
                Result.failure(
                    IllegalStateException("Couldn't add one or more members due to their group privacy settings")
                )
            } else {
                Log.e(TAG, "Error creating group chat: ${e.message}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(chatId: String, currentUid: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (chatId.isBlank() || currentUid.isBlank()) {
            return Result.failure(IllegalArgumentException("chatId and currentUid must not be blank"))
        }
        return try {
            val updates = hashMapOf<String, Any>(
                "participantIds" to FieldValue.arrayRemove(currentUid),
                "participantInfo.$currentUid" to FieldValue.delete()
            )
            firestore.collection("chats").document(chatId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving group $chatId for $currentUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun renameGroup(chatId: String, newName: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))
        }
        val trimmedName = newName.trim().take(40)
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        return try {
            firestore.collection("chats")
                .document(chatId)
                .update("name", trimmedName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error renaming group $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(chatId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))
        }
        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            val messagesCol = chatRef.collection("messages")
            while (true) {
                val page = messagesCol.limit(400).get().await()
                if (page.isEmpty) break
                val batch = firestore.batch()
                for (doc in page.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            chatRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting group $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getChatMessagesFlow(
        chatId: String,
        currentUid: String,
        isGroup: Boolean = false,
        peerUid: String? = null
    ): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var latestSnapshot: com.google.firebase.firestore.QuerySnapshot? = null
        var mapJob: Job? = null
        var cachedPeerUid: String = peerUid?.trim().orEmpty()

        suspend fun resolvePeerUid(docs: List<com.google.firebase.firestore.DocumentSnapshot>): String {
            if (cachedPeerUid.isNotEmpty()) return cachedPeerUid
            val fromDocs = docs.firstNotNullOfOrNull { doc ->
                doc.getString("senderId")?.trim()?.takeIf { it.isNotEmpty() && it != currentUid }
            }
            if (!fromDocs.isNullOrEmpty()) {
                cachedPeerUid = fromDocs
                return cachedPeerUid
            }
            val fromChatId = extractPeerUidFromDirectChatId(chatId, currentUid)
            if (fromChatId.isNotEmpty()) {
                cachedPeerUid = fromChatId
                return cachedPeerUid
            }
            return try {
                val chatSnap = firestore.collection("chats").document(chatId).get().await()
                val pIds = (chatSnap.get("participantIds") as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim() }
                    .orEmpty()
                val resolved = pIds.firstOrNull { it.isNotEmpty() && it != currentUid }.orEmpty()
                cachedPeerUid = resolved
                resolved
            } catch (_: Exception) {
                ""
            }
        }

        fun processSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
            mapJob?.cancel()
            mapJob = launch {
                val hasEncryptedDocs = !isGroup && snapshot.documents.any { doc ->
                    !(doc.getBoolean("isDeleted") ?: false) && !doc.getString("ciphertext").isNullOrBlank()
                }
                val targetPeerUid = if (hasEncryptedDocs) resolvePeerUid(snapshot.documents) else ""
                val keyResult: E2eeManager.ChatKeyResult? = if (hasEncryptedDocs && targetPeerUid.isNotEmpty()) {
                    e2eeManager.resolveChatKey(chatId, currentUid, targetPeerUid)
                } else {
                    null
                }

                var refreshedPeerKeyOnFailure = false

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val isDeleted = doc.getBoolean("isDeleted") ?: false
                        val ciphertext = doc.getString("ciphertext")
                        val iv = doc.getString("iv")

                        val resolvedText = if (isDeleted) {
                            ""
                        } else if (!ciphertext.isNullOrBlank()) {
                            if (!e2eeManager.hasLocalPrivateKey(currentUid) ||
                                keyResult is E2eeManager.ChatKeyResult.NoLocalKey
                            ) {
                                Log.w(TAG, "Decrypt failed: no local private key (chatId=$chatId, messageId=${doc.id})")
                                E2eeManager.DecryptFailureReason.NO_LOCAL_PRIVATE_KEY.placeholder
                            } else {
                                when (keyResult) {
                                    is E2eeManager.ChatKeyResult.Changed -> {
                                        Log.w(TAG, "Decrypt failed: peer key changed (chatId=$chatId, messageId=${doc.id})")
                                        E2eeManager.DecryptFailureReason.WRONG_KEY_OR_GCM_TAG.placeholder
                                    }
                                    is E2eeManager.ChatKeyResult.Success -> {
                                        if (!iv.isNullOrBlank()) {
                                            when (
                                                val decRes = e2eeManager.decryptTextDetailed(
                                                    chatKey = keyResult.chatKey,
                                                    chatId = chatId,
                                                    messageId = doc.id,
                                                    ciphertextB64 = ciphertext,
                                                    ivB64 = iv
                                                )
                                            ) {
                                                is E2eeManager.DecryptResult.Success -> decRes.plaintext
                                                is E2eeManager.DecryptResult.Failure -> {
                                                    if (decRes.reason == E2eeManager.DecryptFailureReason.WRONG_KEY_OR_GCM_TAG &&
                                                        !refreshedPeerKeyOnFailure &&
                                                        targetPeerUid.isNotEmpty()
                                                    ) {
                                                        refreshedPeerKeyOnFailure = true
                                                        e2eeManager.fetchPeerKey(targetPeerUid)
                                                    }
                                                    Log.w(
                                                        TAG,
                                                        "Decrypt failed: ${decRes.reason} (${decRes.detail}) (chatId=$chatId, messageId=${doc.id})"
                                                    )
                                                    decRes.reason.placeholder
                                                }
                                            }
                                        } else {
                                            Log.w(TAG, "Decrypt failed: missing iv (chatId=$chatId, messageId=${doc.id})")
                                            E2eeManager.DecryptFailureReason.OTHER.placeholder
                                        }
                                    }
                                    else -> {
                                        Log.w(TAG, "Decrypt failed: chat key unavailable (chatId=$chatId, messageId=${doc.id})")
                                        E2eeManager.DecryptFailureReason.OTHER.placeholder
                                    }
                                }
                            }
                        } else {
                            doc.getString("text") ?: ""
                        }

                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            text = resolvedText,
                            timestamp = doc.getTimestamp("timestamp"),
                            status = doc.getString("status") ?: "sent",
                            isEdited = doc.getBoolean("isEdited") ?: false,
                            isDeleted = isDeleted,
                            deletedFor = (doc.get("deletedFor") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            messageType = doc.getString("messageType") ?: "text",
                            callType = doc.getString("callType") ?: "voice",
                            callOutcome = doc.getString("callOutcome") ?: "ended",
                            callDurationSeconds = doc.getLong("callDurationSeconds") ?: 0L,
                            callCallerId = doc.getString("callCallerId") ?: "",
                            callCalleeId = doc.getString("callCalleeId") ?: "",
                            mediaUrl = doc.getString("mediaUrl"),
                            thumbnailUrl = doc.getString("thumbnailUrl"),
                            fileName = doc.getString("fileName"),
                            fileSize = doc.getLong("fileSize"),
                            mimeType = doc.getString("mimeType"),
                            durationSeconds = doc.getLong("durationSeconds"),
                            waveform = (doc.get("waveform") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() },
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            locationAddress = doc.getString("locationAddress"),
                            contactName = doc.getString("contactName"),
                            contactPhone = doc.getString("contactPhone"),
                            contactEmail = doc.getString("contactEmail")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                val visibleMessages = messages.filter { currentUid !in it.deletedFor }
                trySend(visibleMessages)

                if (!isGroup) {
                    val toMarkDelivered = snapshot.documents.filter { doc ->
                        doc.getString("senderId") != currentUid &&
                            (doc.getString("status") ?: "sent") == "sent"
                    }
                    if (toMarkDelivered.isNotEmpty()) {
                        val batch = firestore.batch()
                        toMarkDelivered.forEach { doc ->
                            batch.update(doc.reference, "status", "delivered")
                        }
                        try {
                            batch.commit().await()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking messages as delivered in $chatId: ${e.message}", e)
                        }
                    }
                }
            }
        }

        val keyAcceptedJob = launch {
            e2eeManager.keyAcceptedEvents.collectLatest {
                latestSnapshot?.let { snap -> processSnapshot(snap) }
            }
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
                    latestSnapshot = snapshot
                    processSnapshot(snapshot)
                }
            }

        awaitClose {
            keyAcceptedJob.cancel()
            mapJob?.cancel()
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

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            val isGroup = (chatSnap.getString("type") ?: "direct") == "group"
            val msgRef = chatRef.collection("messages").document(messageId)

            @Suppress("UNCHECKED_CAST")
            val lastMsgMap = chatSnap.get("lastMessage") as? Map<String, Any?>
            val isLastMessage = lastMsgMap != null && lastMsgMap["messageId"]?.toString() == messageId

            if (!isGroup) {
                val myUid = SafeFirebase.getAuth(context)?.currentUser?.uid.orEmpty()
                val participantIds = (chatSnap.get("participantIds") as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim() }
                    .orEmpty()
                val peerUid = participantIds.firstOrNull { it.isNotEmpty() && it != myUid }
                    ?: extractPeerUidFromDirectChatId(chatId, myUid)

                if (e2eeManager.keyState.value is E2eeManager.KeyState.Inactive ||
                    e2eeManager.ensureIdentity(myUid) is E2eeManager.KeyState.Inactive
                ) {
                    return Result.failure(
                        IllegalStateException("Use this device for encryption first.")
                    )
                }

                if (e2eeManager.fetchPeerKey(peerUid) is E2eeManager.PeerKeyResult.Changed) {
                    return Result.failure(
                        IllegalStateException("Their security key changed. Open chat info and accept it.")
                    )
                }

                val chatKey = when (val keyRes = e2eeManager.resolveChatKey(chatId, myUid, peerUid)) {
                    is E2eeManager.ChatKeyResult.NoLocalKey -> {
                        return Result.failure(
                            IllegalStateException("Use this device for encryption first.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Missing -> {
                        return Result.failure(
                            IllegalStateException("This contact hasn't updated the app for encryption yet.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Changed -> {
                        return Result.failure(
                            IllegalStateException("Their security key changed. Open chat info and accept it.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Success -> keyRes.chatKey
                }

                val encrypted = e2eeManager.encryptText(
                    chatKey = chatKey,
                    chatId = chatId,
                    messageId = messageId,
                    plain = trimmed
                )

                val msgUpdates = mapOf<String, Any>(
                    "ciphertext" to encrypted.ciphertextB64,
                    "iv" to encrypted.ivB64,
                    "v" to encrypted.v,
                    "isEdited" to true,
                    "text" to FieldValue.delete()
                )
                msgRef.update(msgUpdates).await()

                if (isLastMessage) {
                    val lastMsgUpdates = mapOf<String, Any>(
                        "lastMessage.ciphertext" to encrypted.ciphertextB64,
                        "lastMessage.iv" to encrypted.ivB64,
                        "lastMessage.v" to encrypted.v,
                        "lastMessage.text" to FieldValue.delete()
                    )
                    chatRef.update(lastMsgUpdates).await()
                }
            } else {
                msgRef.update(mapOf("text" to trimmed, "isEdited" to true)).await()
                if (isLastMessage) {
                    chatRef.update("lastMessage.text", trimmed).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message $messageId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshLastMessage(chatId: String) {
        val firestore = db ?: return
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val latestSnap = chatRef.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val latestDoc = latestSnap.documents.firstOrNull()
            if (latestDoc == null) {
                chatRef.update("lastMessage", null).await()
            } else {
                val messageType = latestDoc.getString("messageType") ?: "text"
                val ciphertext = latestDoc.getString("ciphertext")
                val iv = latestDoc.getString("iv")
                val v = (latestDoc.getLong("v") ?: 1L).toInt()
                val deletedFor = (latestDoc.get("deletedFor") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val isDeleted = latestDoc.getBoolean("isDeleted") ?: false

                val lastMessageData = if (isDeleted) {
                    hashMapOf<String, Any?>(
                        "senderId" to (latestDoc.getString("senderId") ?: ""),
                        "timestamp" to latestDoc.getTimestamp("timestamp"),
                        "messageId" to latestDoc.id,
                        "isDeleted" to true,
                        "deletedFor" to deletedFor
                    )
                } else if (messageType == "text" && !ciphertext.isNullOrBlank() && !iv.isNullOrBlank()) {
                    hashMapOf<String, Any?>(
                        "ciphertext" to ciphertext,
                        "iv" to iv,
                        "v" to v,
                        "senderId" to (latestDoc.getString("senderId") ?: ""),
                        "timestamp" to latestDoc.getTimestamp("timestamp"),
                        "messageId" to latestDoc.id,
                        "isDeleted" to false,
                        "deletedFor" to deletedFor
                    )
                } else {
                    val rawText = latestDoc.getString("text") ?: ""
                    val previewText = when (messageType) {
                        "text", "call" -> rawText
                        "image" -> if (rawText.isNotBlank()) "📷 $rawText" else "📷 Photo"
                        "video" -> if (rawText.isNotBlank()) "🎥 $rawText" else "🎥 Video"
                        "document" -> "📄 ${latestDoc.getString("fileName") ?: "Document"}"
                        "voice" -> {
                            val s = latestDoc.getLong("durationSeconds") ?: 0L
                            val m = s / 60
                            val rem = s % 60
                            val durStr = String.format(java.util.Locale.getDefault(), "%d:%02d", m, rem)
                            "🎤 Voice message ($durStr)"
                        }
                        "location" -> {
                            val addr = latestDoc.getString("locationAddress")?.ifBlank { "Location" } ?: "Location"
                            "📍 $addr"
                        }
                        "contact" -> "👤 ${latestDoc.getString("contactName") ?: "Contact"}"
                        else -> rawText.ifBlank { "Attachment" }
                    }
                    hashMapOf<String, Any?>(
                        "text" to previewText,
                        "senderId" to (latestDoc.getString("senderId") ?: ""),
                        "timestamp" to latestDoc.getTimestamp("timestamp"),
                        "messageId" to latestDoc.id,
                        "isDeleted" to false,
                        "deletedFor" to deletedFor
                    )
                }
                chatRef.update("lastMessage", lastMessageData).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing lastMessage for $chatId: ${e.message}", e)
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            chatRef.collection("messages").document(messageId)
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "ciphertext" to FieldValue.delete(),
                        "iv" to FieldValue.delete(),
                        "text" to FieldValue.delete()
                    )
                )
                .await()
            refreshLastMessage(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message $messageId for everyone: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(chatId: String, messageId: String, currentUid: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            val msgRef = chatRef.collection("messages").document(messageId)
            msgRef.update("deletedFor", FieldValue.arrayUnion(currentUid)).await()
            try {
                val chatSnap = chatRef.get().await()
                val participantIds = (chatSnap.get("participantIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val msgSnap = msgRef.get().await()
                if (msgSnap.exists()) {
                    val deletedFor = (msgSnap.get("deletedFor") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    if (participantIds.isNotEmpty() && participantIds.all { it in deletedFor }) {
                        msgRef.delete().await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking/hard-deleting message $messageId on deleteMessageForMe: ${e.message}", e)
            }
            refreshLastMessage(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message $messageId for me: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteChatForMe(chatId: String, currentUid: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (chatId.isBlank() || currentUid.isBlank()) {
            return Result.failure(IllegalArgumentException("chatId and currentUid must not be blank"))
        }
        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            chatRef.update("chatDeletedFor", FieldValue.arrayUnion(currentUid)).await()

            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                val participantIds = (chatSnap.get("participantIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val chatDeletedFor = (chatSnap.get("chatDeletedFor") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()

                if (participantIds.isNotEmpty() && participantIds.all { it in chatDeletedFor }) {
                    val messagesSnap = chatRef.collection("messages").get().await()
                    for (chunk in messagesSnap.documents.chunked(400)) {
                        val batch = firestore.batch()
                        chunk.forEach { doc -> batch.delete(doc.reference) }
                        batch.commit().await()
                    }
                    chatRef.update(
                        mapOf(
                            "lastMessage" to null,
                            "chatDeletedFor" to emptyList<String>()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat $chatId for $currentUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearChatDeletedForMe(chatId: String, currentUid: String) {
        val firestore = db ?: return
        if (chatId.isBlank() || currentUid.isBlank()) return
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val snap = chatRef.get().await()
            if (!snap.exists()) return
            val chatDeletedFor = (snap.get("chatDeletedFor") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
            if (currentUid in chatDeletedFor) {
                chatRef.update("chatDeletedFor", FieldValue.arrayRemove(currentUid)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing chatDeletedFor for $currentUid in $chatId: ${e.message}", e)
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
            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                val blockedBy = (chatSnap.get("blockedBy") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (blockedBy.isNotEmpty()) {
                    return Result.failure(IllegalStateException("You can't send messages to this contact. Unblock to continue."))
                }
            }

            val messageRef = chatRef.collection("messages").document()
            val messageId = messageRef.id
            val isGroup = (chatSnap.getString("type") ?: "direct") == "group"

            val messageData: HashMap<String, Any>
            val lastMessageData: HashMap<String, Any>

            if (!isGroup) {
                val participantIds = (chatSnap.get("participantIds") as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim() }
                    .orEmpty()
                val peerUid = participantIds.firstOrNull { it.isNotEmpty() && it != senderId }
                    ?: extractPeerUidFromDirectChatId(chatId, senderId)

                if (e2eeManager.keyState.value is E2eeManager.KeyState.Inactive ||
                    e2eeManager.ensureIdentity(senderId) is E2eeManager.KeyState.Inactive
                ) {
                    return Result.failure(
                        IllegalStateException("Use this device for encryption first.")
                    )
                }

                if (e2eeManager.fetchPeerKey(peerUid) is E2eeManager.PeerKeyResult.Changed) {
                    return Result.failure(
                        IllegalStateException("Their security key changed. Open chat info and accept it.")
                    )
                }

                val chatKey = when (val keyRes = e2eeManager.resolveChatKey(chatId, senderId, peerUid)) {
                    is E2eeManager.ChatKeyResult.NoLocalKey -> {
                        return Result.failure(
                            IllegalStateException("Use this device for encryption first.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Missing -> {
                        return Result.failure(
                            IllegalStateException("This contact hasn't updated the app for encryption yet.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Changed -> {
                        return Result.failure(
                            IllegalStateException("Their security key changed. Open chat info and accept it.")
                        )
                    }
                    is E2eeManager.ChatKeyResult.Success -> keyRes.chatKey
                }

                val encrypted = e2eeManager.encryptText(
                    chatKey = chatKey,
                    chatId = chatId,
                    messageId = messageId,
                    plain = trimmed
                )

                messageData = hashMapOf(
                    "senderId" to senderId,
                    "ciphertext" to encrypted.ciphertextB64,
                    "iv" to encrypted.ivB64,
                    "v" to encrypted.v,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "status" to "sent",
                    "messageType" to "text"
                )

                lastMessageData = hashMapOf(
                    "ciphertext" to encrypted.ciphertextB64,
                    "iv" to encrypted.ivB64,
                    "v" to encrypted.v,
                    "senderId" to senderId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messageId" to messageId
                )
            } else {
                messageData = hashMapOf(
                    "senderId" to senderId,
                    "text" to trimmed,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "status" to "sent"
                )

                lastMessageData = hashMapOf(
                    "text" to trimmed,
                    "senderId" to senderId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messageId" to messageId
                )
            }

            val batch = firestore.batch()
            batch.set(messageRef, messageData)
            batch.update(chatRef, "lastMessage", lastMessageData)
            try {
                batch.update(chatRef, "chatDeletedFor", FieldValue.arrayRemove(senderId))
            } catch (e: Exception) {
                Log.e(TAG, "Error adding chatDeletedFor removal in sendMessage batch for $chatId: ${e.message}", e)
            }
            batch.commit().await()
            PushRelay.notify(chatId, messageId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message in $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendAttachmentMessage(
        chatId: String,
        senderId: String,
        messageType: String,
        textFallback: String = "",
        mediaUrl: String? = null,
        thumbnailUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        mimeType: String? = null,
        durationSeconds: Long? = null,
        waveform: List<Int>? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationAddress: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        contactEmail: String? = null
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))

        return try {
            val chatRef = firestore.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                val blockedBy = (chatSnap.get("blockedBy") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (blockedBy.isNotEmpty()) {
                    return Result.failure(IllegalStateException("You can't send messages to this contact. Unblock to continue."))
                }
            }
            val messageRef = chatRef.collection("messages").document()

            val messageData = hashMapOf<String, Any?>(
                "senderId" to senderId,
                "text" to textFallback,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "sent",
                "messageType" to messageType
            )

            mediaUrl?.let { messageData["mediaUrl"] = it }
            thumbnailUrl?.let { messageData["thumbnailUrl"] = it }
            fileName?.let { messageData["fileName"] = it }
            fileSize?.let { messageData["fileSize"] = it }
            mimeType?.let { messageData["mimeType"] = it }
            durationSeconds?.let { messageData["durationSeconds"] = it }
            waveform?.let { messageData["waveform"] = it }
            latitude?.let { messageData["latitude"] = it }
            longitude?.let { messageData["longitude"] = it }
            locationAddress?.let { messageData["locationAddress"] = it }
            contactName?.let { messageData["contactName"] = it }
            contactPhone?.let { messageData["contactPhone"] = it }
            contactEmail?.let { messageData["contactEmail"] = it }

            val previewText = when (messageType) {
                "image" -> if (textFallback.isNotBlank()) "📷 $textFallback" else "📷 Photo"
                "video" -> if (textFallback.isNotBlank()) "🎥 $textFallback" else "🎥 Video"
                "document" -> "📄 ${fileName ?: "Document"}"
                "voice" -> {
                    val s = durationSeconds ?: 0L
                    val m = s / 60
                    val rem = s % 60
                    val durStr = String.format(java.util.Locale.getDefault(), "%d:%02d", m, rem)
                    "🎤 Voice message ($durStr)"
                }
                "location" -> "📍 ${locationAddress?.ifBlank { "Location" } ?: "Location"}"
                "contact" -> "👤 ${contactName ?: "Contact"}"
                else -> textFallback.ifBlank { "Attachment" }
            }

            val lastMessageData = hashMapOf(
                "text" to previewText,
                "senderId" to senderId,
                "timestamp" to FieldValue.serverTimestamp(),
                "messageId" to messageRef.id
            )

            val batch = firestore.batch()
            batch.set(messageRef, messageData)
            batch.update(chatRef, "lastMessage", lastMessageData)
            try {
                batch.update(chatRef, "chatDeletedFor", FieldValue.arrayRemove(senderId))
            } catch (e: Exception) {
                Log.e(TAG, "Error adding chatDeletedFor removal in sendAttachmentMessage batch for $chatId: ${e.message}", e)
            }
            batch.commit().await()
            PushRelay.notify(chatId, messageRef.id)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending attachment message in $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun recordCallEvent(
        callerId: String,
        calleeId: String,
        callType: String, // "voice" | "video"
        callOutcome: String, // "connected" | "missed" | "declined" | "no_answer" | "failed"
        durationSeconds: Long,
        callerInfo: ParticipantInfo? = null,
        calleeInfo: ParticipantInfo? = null
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (callerId.isBlank() || calleeId.isBlank()) {
            return Result.failure(IllegalArgumentException("callerId and calleeId must not be blank"))
        }

        return try {
            val chatId = getDirectChatId(callerId, calleeId)
            val chatRef = firestore.collection("chats").document(chatId)

            val summaryText = when (callOutcome) {
                "connected" -> {
                    val mins = durationSeconds / 60
                    val secs = durationSeconds % 60
                    val durStr = String.format("%d:%02d", mins, secs)
                    if (callType == "video") "Video call ($durStr)" else "Voice call ($durStr)"
                }
                "missed" -> if (callType == "video") "Missed video call" else "Missed voice call"
                "declined" -> if (callType == "video") "Declined video call" else "Declined voice call"
                "no_answer" -> "No answer"
                else -> if (callType == "video") "Video call" else "Voice call"
            }

            // 1. Write inline Call Message into /chats/{chatId}/messages/
            val messageRef = chatRef.collection("messages").document()
            val messageData = hashMapOf(
                "senderId" to callerId,
                "text" to summaryText,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "delivered",
                "messageType" to "call",
                "callType" to callType,
                "callOutcome" to callOutcome,
                "callDurationSeconds" to durationSeconds,
                "callCallerId" to callerId,
                "callCalleeId" to calleeId
            )

            val lastMessageData = hashMapOf(
                "text" to summaryText,
                "senderId" to callerId,
                "timestamp" to FieldValue.serverTimestamp(),
                "messageId" to messageRef.id
            )

            val batch = firestore.batch()
            batch.set(messageRef, messageData)
            batch.update(chatRef, "lastMessage", lastMessageData)
            batch.commit().await()

            Log.i(TAG, "Recorded call log event: $summaryText in chat $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording call event: ${e.message}", e)
            Result.failure(e)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getCallLogsFlow(currentUid: String): Flow<List<CallLogItem>> {
        return getUserChatsFlow(currentUid).flatMapLatest { allChats ->
            val chats = allChats.filter { currentUid !in it.blockedBy }
            if (chats.isEmpty()) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val firestore = db
                    if (firestore == null) {
                        trySend(emptyList())
                        close()
                        return@callbackFlow
                    }

                    val registrations = mutableListOf<ListenerRegistration>()
                    val chatCallsMap = mutableMapOf<String, List<CallLogItem>>()

                    fun emitCombined() {
                        val combined = chatCallsMap.values.flatten().sortedByDescending {
                            it.startedAt?.toDate()?.time ?: 0L
                        }
                        trySend(combined)
                    }

                    for (chat in chats) {
                        val reg = firestore.collection("chats")
                            .document(chat.chatId)
                            .collection("messages")
                            .whereEqualTo("messageType", "call")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.w(TAG, "Error querying call messages for chat ${chat.chatId}: ${error.message}")
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    val logs = snapshot.documents.mapNotNull { doc ->
                                        try {
                                            val deletedFor = (doc.get("deletedFor") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                            if (currentUid in deletedFor) return@mapNotNull null

                                            val cType = doc.getString("callType") ?: "voice"
                                            val cOutcome = doc.getString("callOutcome") ?: "ended"
                                            val cDuration = doc.getLong("callDurationSeconds") ?: 0L
                                            val cCallerId = doc.getString("callCallerId") ?: doc.getString("senderId").orEmpty()
                                            val cCalleeId = doc.getString("callCalleeId") ?: ""
                                            val ts = doc.getTimestamp("timestamp")

                                            CallLogItem(
                                                id = "${chat.chatId}_${doc.id}",
                                                callId = doc.id,
                                                type = cType,
                                                callerId = cCallerId,
                                                calleeId = cCalleeId,
                                                participantIds = chat.participantIds,
                                                participantInfo = chat.participantInfo,
                                                chatId = chat.chatId,
                                                status = cOutcome,
                                                startedAt = ts,
                                                connectedAt = ts,
                                                endedAt = ts,
                                                durationSeconds = cDuration
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    chatCallsMap[chat.chatId] = logs
                                    emitCombined()
                                }
                            }
                        registrations.add(reg)
                    }

                    awaitClose {
                        registrations.forEach { it.remove() }
                    }
                }
            }
        }
    }

    suspend fun setGhostMode(chatId: String, enabled: Boolean): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        return try {
            firestore.collection("chats").document(chatId)
                .update("ghostMode", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting ghost mode for $chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReadMessages(chatId: String) {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("status", "read")
                .get()
                .await()

            if (snapshot.isEmpty) return

            for (chunk in snapshot.documents.chunked(400)) {
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            refreshLastMessage(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting read messages in $chatId: ${e.message}", e)
        }
    }

    fun observePublicUser(uid: String): Flow<ChatUser?> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing public user $uid: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    trySend(parsePublicChatUser(doc, uid))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserFlow(uid: String): Flow<ChatUser?> = observePublicUser(uid)

    fun observeMyPrivate(uid: String): Flow<UserPrivate?> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("userPrivate").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing userPrivate for $uid: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val fcmTokens = (doc.get("fcmTokens") as? List<*>)
                        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty()
                    val blockedUsers = (doc.get("blockedUsers") as? List<*>)
                        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty()
                    val readReceipts = doc.getBoolean("readReceipts") ?: true
                    val showOnlineStatus = doc.getBoolean("showOnlineStatus") ?: true
                    val lastSeenVis = doc.getString("lastSeenVisibility")?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") }
                        ?: if (showOnlineStatus) "everyone" else "nobody"
                    val photoVis = doc.getString("photoVisibility")?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") }
                        ?: "everyone"
                    val aboutVis = doc.getString("aboutVisibility")?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts", "nobody") }
                        ?: "everyone"
                    val groupAddVis = (doc.getString("groupAddVisibility") ?: doc.getString("groupsVisibility"))
                        ?.trim()?.lowercase()
                        ?.takeIf { it in setOf("everyone", "contacts") }
                        ?: "everyone"
                    val rawMode = (doc.getString("statusMode") ?: doc.getString("statusVisibility"))
                        ?.trim()?.lowercase()
                    val statusMode = when (rawMode) {
                        "except", "exceptuids" -> "exceptUids"
                        "only", "onlyuids" -> "onlyUids"
                        else -> "contacts"
                    }
                    val statusUids = ((doc.get("statusUids")
                        ?: if (statusMode == "exceptUids") doc.get("statusExceptIds")
                        else if (statusMode == "onlyUids") doc.get("statusOnlyIds")
                        else null) as? List<*>)
                        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty()
                    val mutedChats = (doc.get("mutedChats") as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = (k as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } ?: return@mapNotNull null
                            val ts = v as? Timestamp ?: return@mapNotNull null
                            key to ts
                        }
                        ?.toMap()
                        ?: emptyMap()
                    trySend(
                        UserPrivate(
                            fcmTokens = fcmTokens,
                            blockedUsers = blockedUsers,
                            readReceipts = readReceipts,
                            showOnlineStatus = showOnlineStatus && lastSeenVis != "nobody",
                            lastSeenVisibility = lastSeenVis,
                            photoVisibility = photoVis,
                            aboutVisibility = aboutVis,
                            groupAddVisibility = groupAddVis,
                            statusMode = statusMode,
                            statusUids = statusUids,
                            mutedChats = mutedChats,
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    )
                } else {
                    trySend(UserPrivate())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun setMutedUntil(myUid: String, chatId: String, endTime: Timestamp?): Result<Unit> {
        val cleanUid = myUid.trim()
        val cleanChatId = chatId.trim()
        if (cleanUid.isEmpty() || cleanChatId.isEmpty()) {
            return Result.failure(IllegalArgumentException("UID and chatId are required"))
        }
        val database = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        return try {
            val privRef = database.collection("userPrivate").document(cleanUid)
            val value: Any = endTime ?: FieldValue.delete()
            try {
                privRef.update(
                    com.google.firebase.firestore.FieldPath.of("mutedChats", cleanChatId), value,
                    com.google.firebase.firestore.FieldPath.of("updatedAt"), FieldValue.serverTimestamp()
                ).await()
            } catch (_: Exception) {
                val payload = if (endTime != null) {
                    mapOf(
                        "mutedChats" to mapOf(cleanChatId to endTime),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    mapOf(
                        "mutedChats" to emptyMap<String, Any>(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                }
                privRef.set(payload, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "setMutedUntil failed for $cleanChatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSafetyNumberForPeer(myUid: String, peerUid: String): List<String>? {
        return e2eeManager.computeSafetyNumberForPeer(myUid, peerUid)
    }

    suspend fun getMyPublicKeyB64(myUid: String): String? {
        return e2eeManager.getMyPublicKeyB64(myUid)
    }

    fun observePresence(uid: String): Flow<PresenceInfo?> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("presence").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Presence listener for $uid returned error (hidden or denied): ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    trySend(
                        PresenceInfo(
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            lastSeenAt = doc.getTimestamp("lastSeenAt"),
                            typingChatId = doc.getString("typingChatId"),
                            typingAt = doc.getTimestamp("typingAt")
                        )
                    )
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observePhoto(uid: String): Flow<String?> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("photos").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "Photo listener for $uid returned error (hidden or denied): ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    trySend(doc.getString("photoUrl")?.trim()?.takeIf { it.isNotEmpty() })
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeAbout(uid: String): Flow<String?> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("abouts").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.w(TAG, "About listener for $uid returned error (hidden or denied): ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    trySend(doc.getString("aboutText"))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun setTypingState(uid: String, chatId: String?): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("uid cannot be blank"))
        }
        return try {
            val updates = hashMapOf<String, Any?>(
                "typingChatId" to chatId,
                "typingAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("presence").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting typing state for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markOnlineIfForeground(uid: String) {
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        if (!inForeground) return
        setOnlinePresence(uid, true)
    }

    suspend fun setOnlinePresence(uid: String, online: Boolean) {
        val firestore = db ?: return
        presenceMutex.withLock {
            if (online) {
                val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED)
                if (!inForeground) return
            }
            try {
                val updates = hashMapOf<String, Any>(
                    "isOnline" to online,
                    "lastSeenAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("presence").document(uid).set(updates, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Error setting online presence for $uid: ${e.message}", e)
            }
        }
    }

    suspend fun setShowOnlineStatus(uid: String, enabled: Boolean): Result<Unit> {
        return setLastSeenVisibility(uid, if (enabled) "everyone" else "nobody")
    }

    suspend fun setLastSeenVisibility(uid: String, visibility: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        val enabled = normalized != "nobody"
        return presenceMutex.withLock {
            try {
                val privUpdates = hashMapOf<String, Any>(
                    "lastSeenVisibility" to normalized,
                    "showOnlineStatus" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("userPrivate").document(uid).set(privUpdates, SetOptions.merge()).await()
                if (!enabled) {
                    val presenceUpdates = hashMapOf<String, Any>(
                        "isOnline" to false,
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("presence").document(uid).set(presenceUpdates, SetOptions.merge()).await()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lastSeenVisibility for $uid: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun setPhotoVisibility(uid: String, visibility: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        return try {
            val updates = hashMapOf<String, Any>(
                "photoVisibility" to normalized,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userPrivate").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting photoVisibility for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setAboutVisibility(uid: String, visibility: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts", "nobody")) it else "everyone"
        }
        return try {
            val updates = hashMapOf<String, Any>(
                "aboutVisibility" to normalized,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userPrivate").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting aboutVisibility for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setStatusPrivacy(
        uid: String,
        visibility: String,
        exceptIds: List<String>,
        onlyIds: List<String>
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("contacts", "except", "only", "exceptuids", "onlyuids")) it else "contacts"
        }
        val cleanExcept = exceptIds.map { it.trim() }.filter { it.isNotEmpty() && it != uid }.distinct()
        val cleanOnly = onlyIds.map { it.trim() }.filter { it.isNotEmpty() && it != uid }.distinct()
        val statusMode = when (normalized) {
            "except", "exceptuids" -> "exceptUids"
            "only", "onlyuids" -> "onlyUids"
            else -> "contacts"
        }
        val statusUids = when (statusMode) {
            "exceptUids" -> cleanExcept
            "onlyUids" -> cleanOnly
            else -> emptyList()
        }
        return try {
            val updates = hashMapOf<String, Any>(
                "statusMode" to statusMode,
                "statusUids" to statusUids,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userPrivate").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting statusPrivacy for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setGroupsVisibility(uid: String, visibility: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val normalized = visibility.trim().lowercase().let {
            if (it in setOf("everyone", "contacts")) it else "everyone"
        }
        return try {
            val updates = hashMapOf<String, Any>(
                "groupAddVisibility" to normalized,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userPrivate").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting groupAddVisibility for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setAbout(uid: String, about: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val trimmed = about.trim().take(139)
        return try {
            val updates = hashMapOf<String, Any>(
                "aboutText" to trimmed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("abouts").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting aboutText for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setPhotoUrl(uid: String, photoUrl: String?): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("uid cannot be blank"))
        return try {
            val updates = hashMapOf<String, Any?>(
                "photoUrl" to (photoUrl?.trim() ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("photos").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting photoUrl for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setReadReceipts(uid: String, enabled: Boolean): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        return try {
            val updates = hashMapOf<String, Any>(
                "readReceipts" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userPrivate").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting readReceipts for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getNicknamesFlow(myUid: String): Flow<Map<String, String>> = callbackFlow {
        val firestore = db
        if (firestore == null || myUid.isBlank()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(myUid)
            .collection("nicknames")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to nicknames for $myUid: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.mapNotNull { doc ->
                        val nick = doc.getString("nickname")?.trim().orEmpty()
                        if (nick.isNotEmpty()) doc.id to nick else null
                    }.toMap()
                    trySend(map)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun setNickname(myUid: String, otherUid: String, nickname: String?): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (myUid.isBlank() || otherUid.isBlank()) {
            return Result.failure(IllegalArgumentException("UIDs cannot be blank"))
        }
        val docRef = firestore.collection("users").document(myUid)
            .collection("nicknames").document(otherUid)
        return try {
            val trimmed = nickname?.trim().orEmpty().take(30)
            if (trimmed.isEmpty()) {
                docRef.delete().await()
            } else {
                val data = hashMapOf<String, Any>(
                    "nickname" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting nickname for $otherUid by $myUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getBlockedUsersFlow(myUid: String): Flow<Map<String, ParticipantInfo>> = callbackFlow {
        val firestore = db
        if (firestore == null || myUid.isBlank()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(myUid)
            .collection("blocked")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to blocked users for $myUid: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.mapNotNull { doc ->
                        val otherUid = (doc.getString("uid") ?: doc.id).trim()
                        if (otherUid.isEmpty()) return@mapNotNull null
                        otherUid to ParticipantInfo(
                            uid = otherUid,
                            username = doc.getString("username") ?: "",
                            displayName = doc.getString("displayName") ?: "",
                            photoUrl = doc.getString("photoUrl")
                        )
                    }.toMap()
                    trySend(map)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun blockUser(
        myUid: String,
        otherUid: String,
        otherInfo: ParticipantInfo? = null
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (myUid.isBlank() || otherUid.isBlank()) {
            return Result.failure(IllegalArgumentException("UIDs cannot be blank"))
        }
        return try {
            val chatId = getDirectChatId(myUid, otherUid)
            val chatRef = firestore.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                chatRef.update("blockedBy", FieldValue.arrayUnion(myUid)).await()
            } else {
                chatRef.set(
                    mapOf(
                        "type" to "direct",
                        "participantIds" to listOf(myUid, otherUid),
                        "blockedBy" to listOf(myUid),
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            try {
                firestore.collection("userPrivate").document(myUid)
                    .set(
                        mapOf(
                            "blockedUsers" to FieldValue.arrayUnion(otherUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating blockedUsers array on userPrivate/$myUid: ${e.message}", e)
            }

            try {
                val blockedDoc = hashMapOf<String, Any?>(
                    "uid" to otherUid,
                    "username" to (otherInfo?.username ?: ""),
                    "displayName" to (otherInfo?.displayName ?: ""),
                    "blockedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(myUid)
                    .collection("blocked").document(otherUid)
                    .set(blockedDoc, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error writing users/$myUid/blocked/$otherUid: ${e.message}", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user $otherUid by $myUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(myUid: String, otherUid: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (myUid.isBlank() || otherUid.isBlank()) {
            return Result.failure(IllegalArgumentException("UIDs cannot be blank"))
        }
        return try {
            val chatId = getDirectChatId(myUid, otherUid)
            val chatRef = firestore.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                chatRef.update("blockedBy", FieldValue.arrayRemove(myUid)).await()
            }

            try {
                firestore.collection("userPrivate").document(myUid)
                    .set(
                        mapOf(
                            "blockedUsers" to FieldValue.arrayRemove(otherUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from blockedUsers array on userPrivate/$myUid: ${e.message}", e)
            }

            try {
                firestore.collection("users").document(myUid)
                    .collection("blocked").document(otherUid)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting users/$myUid/blocked/$otherUid: ${e.message}", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user $otherUid by $myUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveFcmToken(uid: String, token: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val cleanUid = uid.trim()
        val cleanToken = token.trim()
        if (cleanUid.isEmpty() || cleanToken.isEmpty()) {
            return Result.failure(IllegalArgumentException("uid and token cannot be blank"))
        }
        return try {
            val privRef = firestore.collection("userPrivate").document(cleanUid)
            privRef.set(
                mapOf(
                    "fcmTokens" to FieldValue.arrayUnion(cleanToken),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error saving FCM token for $cleanUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeFcmToken(uid: String, token: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val cleanUid = uid.trim()
        val cleanToken = token.trim()
        if (cleanUid.isEmpty() || cleanToken.isEmpty()) {
            return Result.failure(IllegalArgumentException("uid and token cannot be blank"))
        }
        return try {
            firestore.collection("userPrivate").document(cleanUid)
                .set(
                    mapOf(
                        "fcmTokens" to FieldValue.arrayRemove(cleanToken),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing FCM token for $cleanUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseStatusDoc(doc: com.google.firebase.firestore.DocumentSnapshot): StatusItem? {
        return try {
            val id = doc.id
            val userId = (doc.getString("userId") ?: doc.getString("uid") ?: doc.getString("authorId")).orEmpty().trim()
            val text = doc.getString("text").orEmpty().trim()
            if (userId.isEmpty() || text.isEmpty()) return null

            val username = doc.getString("username").orEmpty()
            val displayName = doc.getString("displayName").orEmpty()
            val photoUrl = doc.getString("photoUrl")?.ifBlank { null }
            val bgColorRaw = doc.get("backgroundColor") ?: doc.get("bgColor") ?: doc.get("color")
            val backgroundColor = when (bgColorRaw) {
                is String -> bgColorRaw.ifBlank { "#0F766E" }
                is Number -> String.format("#%06X", (0xFFFFFF and bgColorRaw.toInt()))
                else -> "#0F766E"
            }
            val createdAt = doc.getTimestamp("createdAt") ?: doc.getTimestamp("timestamp")
            val expiresAt = doc.getTimestamp("expiresAt")
            val viewerIds = ((doc.get("viewerIds") ?: doc.get("viewedBy")) as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()

            StatusItem(
                id = id,
                userId = userId,
                username = username,
                displayName = displayName,
                photoUrl = photoUrl,
                text = text,
                backgroundColor = backgroundColor,
                createdAt = createdAt,
                expiresAt = expiresAt,
                viewerIds = viewerIds
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing status doc ${doc.id}: ${e.message}")
            null
        }
    }

    fun getStatusesFlow(uid: String): Flow<List<StatusItem>> = callbackFlow {
        val firestore = db
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val audienceDocs = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
        val ownDocs = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

        fun emitMerged() {
            val nowMs = System.currentTimeMillis()
            val merged = audienceDocs + ownDocs
            val items = merged.values
                .mapNotNull { parseStatusDoc(it) }
                .filter { !it.isExpired(nowMs) }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            trySend(items)
        }

        val audienceListener = firestore.collection("statuses")
            .whereArrayContains("audience", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to audience statuses: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    audienceDocs.clear()
                    for (doc in snapshot.documents) {
                        audienceDocs[doc.id] = doc
                    }
                    emitMerged()
                }
            }

        val ownListener = firestore.collection("statuses")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to own statuses: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    ownDocs.clear()
                    for (doc in snapshot.documents) {
                        ownDocs[doc.id] = doc
                    }
                    emitMerged()
                }
            }

        awaitClose {
            audienceListener.remove()
            ownListener.remove()
        }
    }

    suspend fun fetchActiveStatuses(uid: String): List<StatusItem> {
        val firestore = db ?: return emptyList()
        if (uid.isBlank()) return emptyList()
        return try {
            val audienceSnapshot = firestore.collection("statuses")
                .whereArrayContains("audience", uid)
                .get()
                .await()
            val ownSnapshot = firestore.collection("statuses")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            val mergedDocs = LinkedHashMap<String, com.google.firebase.firestore.DocumentSnapshot>()
            for (doc in audienceSnapshot.documents) {
                mergedDocs[doc.id] = doc
            }
            for (doc in ownSnapshot.documents) {
                mergedDocs[doc.id] = doc
            }

            val nowMs = System.currentTimeMillis()
            mergedDocs.values
                .mapNotNull { parseStatusDoc(it) }
                .filter { !it.isExpired(nowMs) }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching active statuses: ${e.message}")
            emptyList()
        }
    }

    suspend fun postTextStatus(
        user: ChatUser,
        text: String,
        backgroundColor: String
    ): Result<StatusItem> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        val trimmed = text.trim().take(500)
        if (user.uid.isBlank() || trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Status text cannot be empty"))
        }
        return try {
            val chatsSnapshot = firestore.collection("chats")
                .whereArrayContains("participantIds", user.uid)
                .get()
                .await()

            val allContacts = chatsSnapshot.documents
                .asSequence()
                .filter { doc -> doc.getString("type") != "group" }
                .flatMap { doc ->
                    val participants = (doc.get("participantIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    participants.asSequence()
                }
                .filter { it.isNotBlank() && it != user.uid }
                .distinct()
                .toList()

            val audience = when (user.statusVisibility.trim().lowercase()) {
                "except" -> {
                    val excluded = user.statusExceptIds.toSet()
                    allContacts.filterNot { it in excluded }
                }
                "only" -> {
                    val allowed = user.statusOnlyIds.toSet()
                    allContacts.filter { it in allowed }
                }
                else -> allContacts
            }

            val docRef = firestore.collection("statuses").document()
            val nowMs = System.currentTimeMillis()
            val createdTs = Timestamp(java.util.Date(nowMs))
            val expiresTs = Timestamp(java.util.Date(nowMs + 24L * 60L * 60L * 1000L))
            val bg = backgroundColor.ifBlank { "#0F766E" }

            val data = hashMapOf<String, Any?>(
                "id" to docRef.id,
                "userId" to user.uid,
                "uid" to user.uid,
                "username" to user.username,
                "displayName" to user.displayName,
                "photoUrl" to (user.photoUrl ?: ""),
                "text" to trimmed,
                "backgroundColor" to bg,
                "type" to "text",
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresTs,
                "viewerIds" to emptyList<String>(),
                "audience" to audience
            )
            docRef.set(data).await()

            Result.success(
                StatusItem(
                    id = docRef.id,
                    userId = user.uid,
                    username = user.username,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    text = trimmed,
                    backgroundColor = bg,
                    createdAt = createdTs,
                    expiresAt = expiresTs,
                    viewerIds = emptyList()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error posting text status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is not available"))
        if (statusId.isBlank()) return Result.failure(IllegalArgumentException("statusId cannot be blank"))
        return try {
            firestore.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting status $statusId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markStatusViewedInFirestore(statusId: String, viewerUid: String) {
        val firestore = db ?: return
        if (statusId.isBlank() || viewerUid.isBlank()) return
        try {
            firestore.collection("statuses").document(statusId)
                .update("viewerIds", FieldValue.arrayUnion(viewerUid))
                .await()
        } catch (_: Exception) {
            // Ignore if security rules restrict updating another user's status doc; local prefs still track viewed state
        }
    }
}


