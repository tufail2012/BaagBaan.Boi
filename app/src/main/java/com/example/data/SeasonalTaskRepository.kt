package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.notifications.NotificationHelper
import com.example.util.SafeFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object SeasonalTaskRepository {
    private const val TAG = "SeasonalTaskRepo"
    private const val PREFS_NAME = "AgriCropSeasonalTasksPrefs"
    private const val KEY_TASKS_JSON = "seasonal_tasks_json"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_APP_CONFIG = "app_config"
    private const val DOC_SEASONAL_REMINDERS = "seasonal_reminders"

    private val _tasks = MutableStateFlow<List<SeasonalTask>>(SeasonalTask.DEFAULT_SEEDS)
    val tasks: StateFlow<List<SeasonalTask>> = _tasks.asStateFlow()

    val currentTasks: List<SeasonalTask>
        get() = _tasks.value

    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false

    private fun getPrefs(context: Context?): SharedPreferences? {
        return context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadFromPrefs(context: Context?) {
        val prefs = getPrefs(context) ?: return
        val rawJson = prefs.getString(KEY_TASKS_JSON, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(rawJson)
                val list = mutableListOf<SeasonalTask>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (!obj.isNull(key)) {
                            map[key] = obj.get(key)
                        }
                    }
                    list.add(SeasonalTask.fromMap(map))
                }
                if (list.isNotEmpty()) {
                    _tasks.value = list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cached seasonal tasks: ${e.message}")
            }
        }
    }

    private fun saveToPrefs(taskList: List<SeasonalTask>, context: Context?) {
        val prefs = getPrefs(context) ?: return
        try {
            val jsonArray = JSONArray()
            taskList.forEach { task ->
                val obj = JSONObject()
                task.toMap().forEach { (k, v) ->
                    obj.put(k, v ?: JSONObject.NULL)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_TASKS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving seasonal tasks to prefs: ${e.message}")
        }
    }

    private fun getDocumentRef(context: Context? = null): DocumentReference? {
        val uid = SafeFirebase.auth?.currentUser?.uid ?: return null
        val firestore: FirebaseFirestore = SafeFirebase.getDb(context) ?: return null
        return firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_APP_CONFIG).document(DOC_SEASONAL_REMINDERS)
    }

    @Synchronized
    fun startListening(context: Context? = null) {
        loadFromPrefs(context)
        rescheduleAllAlarms(context?.applicationContext)

        if (isListening && listenerRegistration != null) return

        val docRef = getDocumentRef(context)
        if (docRef == null) {
            Log.d(TAG, "Unauthenticated or Firestore DB unavailable, using local SeasonalTask storage.")
            return
        }

        try {
            listenerRegistration?.remove()
            isListening = true

            listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied for remote SeasonalTasks; using local state.")
                    } else {
                        Log.w(TAG, "SnapshotListener error on SeasonalTasks: ${error.message}")
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val rawList = snapshot.get("tasks") as? List<Map<String, Any?>>
                    if (rawList != null) {
                        val parsed = rawList.map { SeasonalTask.fromMap(it) }
                        _tasks.value = parsed
                        saveToPrefs(parsed, context)
                        rescheduleAllAlarms(context?.applicationContext)
                        Log.d(TAG, "Realtime SeasonalTasks updated from Firestore (${parsed.size} tasks)")
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    Log.i(TAG, "Document $COLLECTION_APP_CONFIG/$DOC_SEASONAL_REMINDERS not found. Seeding with default values...")
                    val payload = mapOf("tasks" to currentTasks.map { it.toMap() })
                    docRef.set(payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully seeded default SeasonalTasks to Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to seed default SeasonalTasks: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach snapshot listener: ${e.message}")
            isListening = false
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isListening = false
    }

    suspend fun saveTasks(newTasks: List<SeasonalTask>, context: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        saveToPrefs(newTasks, context)
        _tasks.value = newTasks
        rescheduleAllAlarms(context?.applicationContext)

        val docRef = getDocumentRef(context)
        if (docRef != null) {
            try {
                val payload = mapOf("tasks" to newTasks.map { it.toMap() })
                docRef.set(payload, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Remote sync for SeasonalTasks deferred/failed: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun addOrUpdateTask(task: SeasonalTask, context: Context? = null): Result<Unit> {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
        } else {
            current.add(task)
        }
        return saveTasks(current, context)
    }

    suspend fun deleteTask(taskId: String, context: Context? = null): Result<Unit> {
        val current = _tasks.value.toMutableList()
        val targetTask = current.firstOrNull { it.id == taskId }
        if (targetTask != null) {
            if (context != null) {
                NotificationHelper.cancelAlarmNotification(context, targetTask.id.hashCode())
            }
            current.removeAll { it.id == taskId }
            return saveTasks(current, context)
        }
        return Result.success(Unit)
    }

    fun rescheduleAllAlarms(context: Context?) {
        if (context == null) return
        val taskList = _tasks.value
        Log.d(TAG, "Rescheduling alarms for ${taskList.size} seasonal tasks...")

        taskList.forEach { task ->
            val notificationId = task.id.hashCode()
            if (task.isEnabled && task.reminderMonth != null && task.reminderMonth in 1..12 &&
                task.reminderDay != null && task.reminderDay in 1..31
            ) {
                val triggerAtMillis = NotificationHelper.computeNextSeasonalTriggerMillis(
                    month = task.reminderMonth,
                    day = task.reminderDay,
                    hour = 9,
                    minute = 0
                )
                val monthName = java.text.DateFormatSymbols().months[(task.reminderMonth - 1).coerceIn(0, 11)]
                val title = "🌾 ${task.title.ifBlank { task.category }}"
                val message = if (task.notes.isNotBlank()) {
                    "${task.notes} (Annual reminder: $monthName ${task.reminderDay})"
                } else {
                    "Annual orchard reminder for $monthName ${task.reminderDay}."
                }

                NotificationHelper.scheduleAlarmNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    message = message,
                    triggerAtMillis = triggerAtMillis,
                    isSeasonal = true,
                    taskId = task.id,
                    month = task.reminderMonth,
                    day = task.reminderDay
                )
                Log.d(TAG, "Scheduled alarm for task '${task.title}' on month=${task.reminderMonth}, day=${task.reminderDay} at timestamp=$triggerAtMillis")
            } else {
                NotificationHelper.cancelAlarmNotification(context, notificationId)
            }
        }
    }
}
