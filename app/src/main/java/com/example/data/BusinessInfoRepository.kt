package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.util.SafeFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
    private const val PREFS_NAME = "AgriCropBusinessInfoPrefs"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_APP_CONFIG = "app_config"
    private const val DOC_BUSINESS_INFO = "business_info"

    private val _businessInfo = MutableStateFlow(BusinessInfo.DEFAULT)
    val businessInfo: StateFlow<BusinessInfo> = _businessInfo.asStateFlow()

    val currentBusinessInfo: BusinessInfo
        get() = _businessInfo.value

    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false

    private fun getPrefs(context: Context?): SharedPreferences? {
        return context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadFromPrefs(context: Context?) {
        val prefs = getPrefs(context) ?: return
        val businessName = prefs.getString("businessName", null) ?: return
        val tagline = prefs.getString("tagline", BusinessInfo.DEFAULT.tagline) ?: BusinessInfo.DEFAULT.tagline
        val address = prefs.getString("address", BusinessInfo.DEFAULT.address) ?: BusinessInfo.DEFAULT.address
        val contactsSet = prefs.getStringSet("contactNumbers", null)
        val contactNumbers = if (!contactsSet.isNullOrEmpty()) contactsSet.toList() else BusinessInfo.DEFAULT.contactNumbers
        val accountNumber = prefs.getString("accountNumber", BusinessInfo.DEFAULT.accountNumber) ?: BusinessInfo.DEFAULT.accountNumber
        val accountHolderName = prefs.getString("accountHolderName", BusinessInfo.DEFAULT.accountHolderName) ?: BusinessInfo.DEFAULT.accountHolderName
        val ifscCode = prefs.getString("ifscCode", BusinessInfo.DEFAULT.ifscCode) ?: BusinessInfo.DEFAULT.ifscCode
        val registrationNumber = prefs.getString("registrationNumber", BusinessInfo.DEFAULT.registrationNumber) ?: BusinessInfo.DEFAULT.registrationNumber

        _businessInfo.value = BusinessInfo(
            businessName = businessName,
            tagline = tagline,
            address = address,
            contactNumbers = contactNumbers,
            accountNumber = accountNumber,
            accountHolderName = accountHolderName,
            ifscCode = ifscCode,
            registrationNumber = registrationNumber
        )
    }

    private fun saveToPrefs(info: BusinessInfo, context: Context?) {
        val prefs = getPrefs(context) ?: return
        prefs.edit()
            .putString("businessName", info.businessName)
            .putString("tagline", info.tagline)
            .putString("address", info.address)
            .putStringSet("contactNumbers", info.contactNumbers.toSet())
            .putString("accountNumber", info.accountNumber)
            .putString("accountHolderName", info.accountHolderName)
            .putString("ifscCode", info.ifscCode)
            .putString("registrationNumber", info.registrationNumber)
            .apply()
    }

    private fun getDocumentRef(context: Context? = null): DocumentReference? {
        val uid = SafeFirebase.auth?.currentUser?.uid ?: return null
        val firestore: FirebaseFirestore = SafeFirebase.getDb(context) ?: return null
        return firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_APP_CONFIG).document(DOC_BUSINESS_INFO)
    }

    @Synchronized
    fun startListening(context: Context? = null) {
        loadFromPrefs(context)

        if (isListening && listenerRegistration != null) return

        val docRef = getDocumentRef(context)
        if (docRef == null) {
            Log.d(TAG, "Unauthenticated or Firestore DB unavailable, using local BusinessInfo storage.")
            return
        }

        try {
            listenerRegistration?.remove()
            isListening = true

            listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied for remote BusinessInfo; using local state.")
                    } else {
                        Log.w(TAG, "SnapshotListener error on BusinessInfo: ${error.message}")
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val parsed = BusinessInfo.fromMap(data)
                        _businessInfo.value = parsed
                        saveToPrefs(parsed, context)
                        Log.d(TAG, "Realtime BusinessInfo updated from Firestore: ${parsed.businessName}")
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    Log.i(TAG, "Document $COLLECTION_APP_CONFIG/$DOC_BUSINESS_INFO not found. Seeding with default values...")
                    docRef.set(currentBusinessInfo.toMap())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully seeded default BusinessInfo to Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to seed default BusinessInfo: ${e.message}")
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

    suspend fun saveBusinessInfo(info: BusinessInfo, context: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        saveToPrefs(info, context)
        _businessInfo.value = info

        val docRef = getDocumentRef(context)
        if (docRef != null) {
            try {
                docRef.set(info.toMap(), SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Remote sync for BusinessInfo deferred/failed: ${e.message}")
            }
        }
        Result.success(Unit)
    }
}
