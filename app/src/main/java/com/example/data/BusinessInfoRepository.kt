package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.SafeFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object BusinessInfoRepository {
    private const val TAG = "BusinessInfoRepo"
    private const val COLLECTION_APP_CONFIG = "app_config"
    private const val DOC_BUSINESS_INFO = "business_info"

    private val _businessInfo = MutableStateFlow(BusinessInfo.DEFAULT)
    val businessInfo: StateFlow<BusinessInfo> = _businessInfo.asStateFlow()

    val currentBusinessInfo: BusinessInfo
        get() = _businessInfo.value

    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false

    private fun getDocumentRef(context: Context? = null): DocumentReference? {
        val firestore: FirebaseFirestore = SafeFirebase.getDb(context) ?: return null
        return firestore.collection(COLLECTION_APP_CONFIG).document(DOC_BUSINESS_INFO)
    }

    @Synchronized
    fun startListening(context: Context? = null) {
        if (isListening && listenerRegistration != null) return

        val docRef = getDocumentRef(context)
        if (docRef == null) {
            Log.w(TAG, "Firestore DB unavailable, retaining default BusinessInfo in-memory.")
            return
        }

        try {
            listenerRegistration?.remove()
            isListening = true

            listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "SnapshotListener error on $COLLECTION_APP_CONFIG/$DOC_BUSINESS_INFO: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val parsed = BusinessInfo.fromMap(data)
                        _businessInfo.value = parsed
                        Log.d(TAG, "Realtime BusinessInfo updated from Firestore: ${parsed.businessName}")
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    // Document does not exist yet: seed it with DEFAULT real values
                    Log.i(TAG, "Document $COLLECTION_APP_CONFIG/$DOC_BUSINESS_INFO not found. Seeding with default values...")
                    docRef.set(BusinessInfo.DEFAULT.toMap())
                        .addOnSuccessListener {
                            _businessInfo.value = BusinessInfo.DEFAULT
                            Log.d(TAG, "Successfully seeded default BusinessInfo to Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to seed default BusinessInfo: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach snapshot listener: ${e.message}", e)
            isListening = false
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isListening = false
    }

    suspend fun saveBusinessInfo(info: BusinessInfo, context: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val docRef = getDocumentRef(context)
            ?: return@withContext Result.failure(IllegalStateException("Firestore is not available."))

        try {
            docRef.set(info.toMap(), SetOptions.merge()).await()
            _businessInfo.value = info
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving BusinessInfo to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
