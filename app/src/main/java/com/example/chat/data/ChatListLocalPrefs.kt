package com.example.chat.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ChatList(
    val id: String,
    val name: String,
    val chatIds: List<String>
)

data class ChatItemLocalState(
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val favourite: Boolean = false,
    val mutedUntil: Long = 0L,
    val hiddenAt: Long = 0L,
    val markedUnread: Boolean = false,
    val locked: Boolean = false
) {
    fun isMuted(nowMs: Long = System.currentTimeMillis()): Boolean {
        return mutedUntil == Long.MAX_VALUE || mutedUntil > nowMs
    }

    fun isHidden(lastMessageTimeMs: Long): Boolean {
        return hiddenAt > 0L && lastMessageTimeMs <= hiddenAt
    }
}

class ChatListLocalPrefs(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): Map<String, ChatItemLocalState> {
        val raw = prefs.getString(KEY_CHAT_STATES_JSON, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, ChatItemLocalState>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val chatId = keys.next()
                val obj = root.optJSONObject(chatId) ?: continue
                result[chatId] = ChatItemLocalState(
                    pinned = obj.optBoolean("pinned", false),
                    archived = obj.optBoolean("archived", false),
                    favourite = obj.optBoolean("favourite", false),
                    mutedUntil = obj.optLong("mutedUntil", 0L),
                    hiddenAt = obj.optLong("hiddenAt", 0L),
                    markedUnread = obj.optBoolean("markedUnread", false),
                    locked = obj.optBoolean("locked", false)
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveAll(states: Map<String, ChatItemLocalState>) {
        val root = JSONObject()
        for ((chatId, state) in states) {
            val obj = JSONObject().apply {
                put("pinned", state.pinned)
                put("archived", state.archived)
                put("favourite", state.favourite)
                put("mutedUntil", state.mutedUntil)
                put("hiddenAt", state.hiddenAt)
                put("markedUnread", state.markedUnread)
                put("locked", state.locked)
            }
            root.put(chatId, obj)
        }
        prefs.edit().putString(KEY_CHAT_STATES_JSON, root.toString()).apply()
    }

    fun updateChats(
        chatIds: Collection<String>,
        transform: (ChatItemLocalState) -> ChatItemLocalState
    ): Map<String, ChatItemLocalState> {
        val current = loadAll().toMutableMap()
        for (id in chatIds) {
            val existing = current[id] ?: ChatItemLocalState()
            current[id] = transform(existing)
        }
        saveAll(current)
        return current
    }

    fun loadCustomLists(): List<ChatList> {
        val raw = prefs.getString(KEY_CUSTOM_LISTS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<ChatList>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty()) continue
                val idsArray = obj.optJSONArray("chatIds")
                val chatIds = mutableListOf<String>()
                if (idsArray != null) {
                    for (j in 0 until idsArray.length()) {
                        val cid = idsArray.optString(j, "").trim()
                        if (cid.isNotEmpty()) {
                            chatIds.add(cid)
                        }
                    }
                }
                result.add(ChatList(id = id, name = name, chatIds = chatIds))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomLists(lists: List<ChatList>) {
        val array = JSONArray()
        for (list in lists) {
            val idsArray = JSONArray()
            for (cid in list.chatIds) {
                idsArray.put(cid)
            }
            val obj = JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("chatIds", idsArray)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_LISTS_JSON, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "baagbaan_chat_list_local_prefs"
        private const val KEY_CHAT_STATES_JSON = "chat_item_local_states_json"
        private const val KEY_CUSTOM_LISTS_JSON = "chat_custom_lists_json"
    }
}
