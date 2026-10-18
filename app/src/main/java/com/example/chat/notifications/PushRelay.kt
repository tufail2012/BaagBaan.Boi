package com.example.chat.notifications

import android.util.Log
import com.example.util.SafeFirebase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object PushRelay {

    const val PUSH_RELAY_BASE_URL = ""

    private const val TAG = "PushRelay"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val relayScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fire-and-forget notification trigger to the Cloudflare Worker relay (`POST /notify`).
     * Returns immediately and silently when [PUSH_RELAY_BASE_URL] is blank.
     */
    fun notify(chatId: String, messageId: String) {
        val baseUrl = PUSH_RELAY_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isEmpty()) return

        val cleanChatId = chatId.trim()
        val cleanMessageId = messageId.trim()
        if (cleanChatId.isEmpty() || cleanMessageId.isEmpty()) return

        relayScope.launch {
            try {
                val user = SafeFirebase.auth?.currentUser ?: return@launch
                val tokenResult = user.getIdToken(false).await()
                val idToken = tokenResult?.token?.trim()
                if (idToken.isNullOrEmpty()) return@launch

                val bodyJson = JSONObject()
                    .put("chatId", cleanChatId)
                    .put("messageId", cleanMessageId)
                    .toString()

                val request = Request.Builder()
                    .url("$baseUrl/notify")
                    .header("Authorization", "Bearer $idToken")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(
                            TAG,
                            "PushRelay /notify returned HTTP ${response.code} for chatId=$cleanChatId messageId=$cleanMessageId"
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "PushRelay notify error for chatId=$cleanChatId: ${t.message}")
            }
        }
    }
}
