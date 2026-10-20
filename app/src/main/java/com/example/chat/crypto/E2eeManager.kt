package com.example.chat.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.util.SafeFirebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class E2eeManager(context: Context) {

    private val appContext = context.applicationContext

    private val keyPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_KEYS, Context.MODE_PRIVATE)
    }

    private val pinPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_PINS, Context.MODE_PRIVATE)
    }

    private val db: FirebaseFirestore?
        get() = SafeFirebase.getDb(appContext)

    private val identityMutex = Mutex()
    private val keyResolveMutex = Mutex()

    private val identityCache = ConcurrentHashMap<String, IdentityKeyMaterial>()
    private val chatKeyCache = ConcurrentHashMap<String, ByteArray>()
    private val chatPeerMap = ConcurrentHashMap<String, String>()
    private val latestServerKeys = ConcurrentHashMap<String, String>()

    private val _changedPeerUids = MutableStateFlow<Set<String>>(emptySet())
    val changedPeerUids: StateFlow<Set<String>> = _changedPeerUids.asStateFlow()

    private val _keyAcceptedEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val keyAcceptedEvents: SharedFlow<String> = _keyAcceptedEvents.asSharedFlow()

    private data class IdentityKeyMaterial(
        val privateKey: PrivateKey,
        val publicKey: PublicKey,
        val publicKeyB64: String
    )

    sealed class PeerKeyResult {
        data object Missing : PeerKeyResult()
        data class Ok(val publicKeyB64: String) : PeerKeyResult()
        data class Changed(val newKeyB64: String) : PeerKeyResult()
    }

    sealed class ChatKeyResult {
        data class Success(val chatKey: ByteArray) : ChatKeyResult()
        data object Missing : ChatKeyResult()
        data class Changed(val newKeyB64: String) : ChatKeyResult()
    }

    data class EncryptedPayload(
        val ciphertextB64: String,
        val ivB64: String,
        val v: Int = PROTOCOL_VERSION
    )

    suspend fun ensureIdentity(uid: String) {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return

        identityMutex.withLock {
            val existingLocal = loadIdentityFromPrefs(cleanUid)
            val firestore = db
            if (existingLocal == null) {
                val generated = generateAndStoreIdentity(cleanUid) ?: return
                identityCache[cleanUid] = generated
                if (firestore != null) {
                    writePublicKeyDoc(firestore, cleanUid, generated.publicKeyB64)
                }
            } else {
                identityCache[cleanUid] = existingLocal
                if (firestore != null) {
                    try {
                        val snap = firestore.collection(COLLECTION_PUBLIC_KEYS)
                            .document(cleanUid)
                            .get()
                            .await()
                        val remoteKey = snap.getString("publicKey")?.trim()
                        if (!snap.exists() || remoteKey != existingLocal.publicKeyB64) {
                            writePublicKeyDoc(firestore, cleanUid, existingLocal.publicKeyB64)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "ensureIdentity: failed checking publicKeys/$cleanUid: ${e.message}", e)
                    }
                }
            }
        }
    }

    private suspend fun writePublicKeyDoc(
        firestore: FirebaseFirestore,
        uid: String,
        publicKeyB64: String
    ) {
        try {
            val data = mapOf(
                "publicKey" to publicKeyB64,
                "algorithm" to ALGORITHM,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_PUBLIC_KEYS)
                .document(uid)
                .set(data)
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing publicKeys/$uid: ${e.message}", e)
        }
    }

    suspend fun fetchPeerKey(peerUid: String): PeerKeyResult {
        val cleanPeer = peerUid.trim()
        if (cleanPeer.isEmpty()) return PeerKeyResult.Missing
        val firestore = db ?: return PeerKeyResult.Missing

        val snap = try {
            firestore.collection(COLLECTION_PUBLIC_KEYS)
                .document(cleanPeer)
                .get()
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "fetchPeerKey: failed reading publicKeys/$cleanPeer: ${e.message}", e)
            val pinned = pinPrefs.getString(cleanPeer, null)?.trim()
            return if (!pinned.isNullOrEmpty() && cleanPeer !in _changedPeerUids.value) {
                PeerKeyResult.Ok(pinned)
            } else {
                PeerKeyResult.Missing
            }
        }

        if (!snap.exists()) {
            return PeerKeyResult.Missing
        }

        val serverKeyB64 = snap.getString("publicKey")?.trim().orEmpty()
        if (serverKeyB64.isEmpty()) {
            return PeerKeyResult.Missing
        }

        return evaluateServerPeerKey(cleanPeer, serverKeyB64)
    }

    fun evaluateServerPeerKey(peerUid: String, serverKeyB64: String): PeerKeyResult {
        val cleanPeer = peerUid.trim()
        val cleanKey = serverKeyB64.trim()
        if (cleanPeer.isEmpty() || cleanKey.isEmpty()) return PeerKeyResult.Missing

        latestServerKeys[cleanPeer] = cleanKey
        val pinnedKeyB64 = pinPrefs.getString(cleanPeer, null)?.trim()

        return when {
            pinnedKeyB64.isNullOrEmpty() -> {
                pinPrefs.edit().putString(cleanPeer, cleanKey).apply()
                _changedPeerUids.update { it - cleanPeer }
                PeerKeyResult.Ok(cleanKey)
            }
            pinnedKeyB64 == cleanKey -> {
                _changedPeerUids.update { it - cleanPeer }
                PeerKeyResult.Ok(cleanKey)
            }
            else -> {
                invalidateCachedChatKeysForPeer(cleanPeer)
                _changedPeerUids.update { it + cleanPeer }
                PeerKeyResult.Changed(cleanKey)
            }
        }
    }

    fun acceptNewKey(peerUid: String) {
        val cleanPeer = peerUid.trim()
        if (cleanPeer.isEmpty()) return

        val serverKey = latestServerKeys[cleanPeer]
        if (!serverKey.isNullOrEmpty()) {
            pinPrefs.edit().putString(cleanPeer, serverKey).apply()
        } else {
            pinPrefs.edit().remove(cleanPeer).apply()
        }
        invalidateCachedChatKeysForPeer(cleanPeer)
        _changedPeerUids.update { it - cleanPeer }
        _keyAcceptedEvents.tryEmit(cleanPeer)
    }

    private fun invalidateCachedChatKeysForPeer(peerUid: String) {
        for ((chatId, mappedPeer) in chatPeerMap.entries) {
            if (mappedPeer == peerUid || chatId.contains(peerUid)) {
                chatKeyCache.remove(chatId)
            }
        }
        val toRemove = chatKeyCache.keys().toList().filter { it.contains(peerUid) }
        for (chatId in toRemove) {
            chatKeyCache.remove(chatId)
        }
    }

    suspend fun resolveChatKey(
        chatId: String,
        myUid: String,
        peerUid: String
    ): ChatKeyResult {
        val cleanChatId = chatId.trim()
        val cleanMyUid = myUid.trim()
        val cleanPeerUid = peerUid.trim()
        if (cleanChatId.isEmpty() || cleanMyUid.isEmpty() || cleanPeerUid.isEmpty()) {
            return ChatKeyResult.Missing
        }

        chatPeerMap[cleanChatId] = cleanPeerUid

        if (cleanPeerUid in _changedPeerUids.value) {
            val changedKey = latestServerKeys[cleanPeerUid].orEmpty()
            return ChatKeyResult.Changed(changedKey)
        }

        chatKeyCache[cleanChatId]?.let { cached ->
            return ChatKeyResult.Success(cached)
        }

        return keyResolveMutex.withLock {
            if (cleanPeerUid in _changedPeerUids.value) {
                return@withLock ChatKeyResult.Changed(latestServerKeys[cleanPeerUid].orEmpty())
            }
            chatKeyCache[cleanChatId]?.let { cached ->
                return@withLock ChatKeyResult.Success(cached)
            }

            val myIdentity = getOrCreateLocalIdentity(cleanMyUid)
                ?: return@withLock ChatKeyResult.Missing

            when (val peerRes = fetchPeerKey(cleanPeerUid)) {
                is PeerKeyResult.Missing -> ChatKeyResult.Missing
                is PeerKeyResult.Changed -> ChatKeyResult.Changed(peerRes.newKeyB64)
                is PeerKeyResult.Ok -> {
                    val derived = deriveChatKey(
                        myPrivateKey = myIdentity.privateKey,
                        peerPublicKeyB64 = peerRes.publicKeyB64,
                        myUid = cleanMyUid,
                        peerUid = cleanPeerUid
                    ) ?: return@withLock ChatKeyResult.Missing

                    chatKeyCache[cleanChatId] = derived
                    ChatKeyResult.Success(derived)
                }
            }
        }
    }

    fun encryptText(
        chatKey: ByteArray,
        chatId: String,
        messageId: String,
        plain: String
    ): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val keySpec = SecretKeySpec(chatKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val aad = "$chatId|$messageId".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        val ciphertextBytes = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextB64 = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            v = PROTOCOL_VERSION
        )
    }

    fun decryptText(
        chatKey: ByteArray,
        chatId: String,
        messageId: String,
        ciphertextB64: String,
        ivB64: String
    ): String? {
        return try {
            if (chatId.isBlank() || messageId.isBlank() || ciphertextB64.isBlank() || ivB64.isBlank()) {
                return null
            }
            val iv = Base64.decode(ivB64.trim(), Base64.NO_WRAP)
            if (iv.size != GCM_IV_LENGTH_BYTES) {
                return null
            }
            val ciphertextBytes = Base64.decode(ciphertextB64.trim(), Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val keySpec = SecretKeySpec(chatKey, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val aad = "$chatId|$messageId".toByteArray(Charsets.UTF_8)
            cipher.updateAAD(aad)

            val plainBytes = cipher.doFinal(ciphertextBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun getOrCreateLocalIdentity(uid: String): IdentityKeyMaterial? {
        identityCache[uid]?.let { return it }
        ensureIdentity(uid)
        return identityCache[uid]
    }

    private fun deriveChatKey(
        myPrivateKey: PrivateKey,
        peerPublicKeyB64: String,
        myUid: String,
        peerUid: String
    ): ByteArray? {
        return try {
            val peerPubBytes = Base64.decode(peerPublicKeyB64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("EC")
            val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerPubBytes))

            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(myPrivateKey)
            keyAgreement.doPhase(peerPublicKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            val sortedUids = listOf(myUid, peerUid).sorted().joinToString("|")
            val infoBytes = "baagbaan-e2ee-v1|$sortedUids".toByteArray(Charsets.UTF_8)

            hkdfSha256(
                ikm = sharedSecret,
                salt = ByteArray(0),
                info = infoBytes,
                length = CHAT_KEY_LENGTH_BYTES
            )
        } catch (e: Exception) {
            Log.w(TAG, "deriveChatKey failed for $myUid and $peerUid: ${e.message}", e)
            null
        }
    }

    private fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int
    ): ByteArray {
        // HKDF-Extract(salt, IKM): RFC 5869 uses HashLen zeros when salt is empty
        val actualSalt = if (salt.isEmpty()) ByteArray(HMAC_SHA256_BYTES) else salt
        val extractMac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        extractMac.init(SecretKeySpec(actualSalt, HMAC_SHA256_ALGORITHM))
        val prk = extractMac.doFinal(ikm)

        // HKDF-Expand(PRK, info, length)
        val expandMac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        expandMac.init(SecretKeySpec(prk, HMAC_SHA256_ALGORITHM))

        val okm = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            expandMac.reset()
            expandMac.update(t)
            expandMac.update(info)
            expandMac.update(counter.toByte())
            t = expandMac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, toCopy)
            offset += toCopy
            counter++
        }
        return okm
    }

    private fun getOrCreateWrapKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(WRAP_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            WRAP_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun generateAndStoreIdentity(uid: String): IdentityKeyMaterial? {
        return try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec(EC_CURVE))
            val keyPair: KeyPair = kpg.generateKeyPair()

            val pkcs8Bytes = keyPair.private.encoded
            val x509Bytes = keyPair.public.encoded
            val publicKeyB64 = Base64.encodeToString(x509Bytes, Base64.NO_WRAP)

            val wrapKey = getOrCreateWrapKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrapKey)
            val wrapIv = cipher.iv
            val wrappedBytes = cipher.doFinal(pkcs8Bytes)

            val wrappedCiphertextB64 = Base64.encodeToString(wrappedBytes, Base64.NO_WRAP)
            val wrapIvB64 = Base64.encodeToString(wrapIv, Base64.NO_WRAP)

            keyPrefs.edit()
                .putString(prefKeyCiphertext(uid), wrappedCiphertextB64)
                .putString(prefKeyIv(uid), wrapIvB64)
                .putString(prefKeyPublic(uid), publicKeyB64)
                .putString(KEY_WRAPPED_CIPHERTEXT_DEFAULT, wrappedCiphertextB64)
                .putString(KEY_WRAPPED_IV_DEFAULT, wrapIvB64)
                .putString(KEY_PUBLIC_DEFAULT, publicKeyB64)
                .putString(KEY_OWNER_UID, uid)
                .apply()

            IdentityKeyMaterial(
                privateKey = keyPair.private,
                publicKey = keyPair.public,
                publicKeyB64 = publicKeyB64
            )
        } catch (e: Exception) {
            Log.e(TAG, "generateAndStoreIdentity failed for $uid: ${e.message}", e)
            null
        }
    }

    private fun loadIdentityFromPrefs(uid: String): IdentityKeyMaterial? {
        return try {
            val defaultOwner = keyPrefs.getString(KEY_OWNER_UID, null)
            val wrappedB64 = keyPrefs.getString(prefKeyCiphertext(uid), null)
                ?: if (defaultOwner == uid || defaultOwner == null) {
                    keyPrefs.getString(KEY_WRAPPED_CIPHERTEXT_DEFAULT, null)
                } else null
            val ivB64 = keyPrefs.getString(prefKeyIv(uid), null)
                ?: if (defaultOwner == uid || defaultOwner == null) {
                    keyPrefs.getString(KEY_WRAPPED_IV_DEFAULT, null)
                } else null
            val pubB64 = keyPrefs.getString(prefKeyPublic(uid), null)
                ?: if (defaultOwner == uid || defaultOwner == null) {
                    keyPrefs.getString(KEY_PUBLIC_DEFAULT, null)
                } else null

            if (wrappedB64.isNullOrBlank() || ivB64.isNullOrBlank() || pubB64.isNullOrBlank()) {
                return null
            }

            val wrapKey = getOrCreateWrapKey()
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val wrappedBytes = Base64.decode(wrappedB64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val pkcs8Bytes = cipher.doFinal(wrappedBytes)

            val keyFactory = KeyFactory.getInstance("EC")
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes))
            val publicKey = keyFactory.generatePublic(
                X509EncodedKeySpec(Base64.decode(pubB64, Base64.NO_WRAP))
            )

            IdentityKeyMaterial(
                privateKey = privateKey,
                publicKey = publicKey,
                publicKeyB64 = pubB64.trim()
            )
        } catch (e: Exception) {
            Log.w(TAG, "loadIdentityFromPrefs failed for $uid: ${e.message}", e)
            null
        }
    }

    private fun prefKeyCiphertext(uid: String) = "wrapped_priv_ct_$uid"
    private fun prefKeyIv(uid: String) = "wrapped_priv_iv_$uid"
    private fun prefKeyPublic(uid: String) = "public_key_$uid"

    companion object {
        private const val TAG = "E2eeManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val WRAP_KEY_ALIAS = "baagbaan_e2ee_wrap_v1"
        private const val EC_CURVE = "secp256r1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"
        private const val HMAC_SHA256_BYTES = 32
        private const val CHAT_KEY_LENGTH_BYTES = 32
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PROTOCOL_VERSION = 1

        const val ALGORITHM = "ECDH-P256-HKDF-AESGCM-v1"
        const val COLLECTION_PUBLIC_KEYS = "publicKeys"

        private const val PREFS_KEYS = "e2ee_keys"
        private const val PREFS_PINS = "e2ee_pins"

        private const val KEY_WRAPPED_CIPHERTEXT_DEFAULT = "wrapped_private_key"
        private const val KEY_WRAPPED_IV_DEFAULT = "wrapped_private_iv"
        private const val KEY_PUBLIC_DEFAULT = "public_key"
        private const val KEY_OWNER_UID = "owner_uid"

        @Volatile
        private var instance: E2eeManager? = null

        fun getInstance(context: Context): E2eeManager {
            return instance ?: synchronized(this) {
                instance ?: E2eeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
