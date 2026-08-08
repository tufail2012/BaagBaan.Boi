package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserFirestoreRepository {

    private val db get() = com.example.util.SafeFirebase.db
    private val auth get() = com.example.util.SafeFirebase.auth

    companion object {
        private const val TAG = "UserFirestoreRepo"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_ATTENDANCE = "attendance"
    }

    private fun getCurrentUid(): String? {
        return auth?.currentUser?.uid
    }

    // =========================================================================
    // BOOKINGS (users/{uid}/bookings/{bookingId})
    // =========================================================================

    fun getBookingsFlow(targetUid: String? = null): Flow<List<UserBooking>> = callbackFlow {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_BOOKINGS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied fetching user bookings for $uid: ${error.message}")
                    } else {
                        Log.e(TAG, "Error fetching user bookings for $uid: ${error.message}")
                    }
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val bookings = snapshot.documents.mapNotNull { doc ->
                        val b = doc.toObject(UserBooking::class.java)
                        b?.copy(id = doc.id)
                    }
                    trySend(bookings)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun saveBooking(booking: UserBooking, targetUid: String? = null): Result<String> {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            return Result.failure(IllegalStateException("User is not authenticated or Firestore unavailable"))
        }

        return try {
            val collectionRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_BOOKINGS)

            val docRef = if (booking.id.isNotEmpty()) {
                collectionRef.document(booking.id)
            } else {
                collectionRef.document()
            }

            val bookingId = docRef.id
            val map = hashMapOf(
                "id" to bookingId,
                "type" to booking.type,
                "itemName" to booking.itemName,
                "variety" to booking.variety,
                "season" to booking.season,
                "farmerName" to booking.farmerName,
                "quantity" to booking.quantity,
                "bookingDate" to booking.bookingDate,
                "notes" to booking.notes,
                "createdAt" to if (booking.createdAt > 0) booking.createdAt else System.currentTimeMillis()
            )

            docRef.set(map).await()
            Log.d(TAG, "Successfully saved user booking $bookingId for $uid")
            Result.success(bookingId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving booking for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteBooking(bookingId: String, targetUid: String? = null): Result<Unit> {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            return Result.failure(IllegalStateException("User is not authenticated or Firestore unavailable"))
        }

        return try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted booking $bookingId for $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting booking $bookingId for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    // =========================================================================
    // ATTENDANCE (users/{uid}/attendance/{attendanceId})
    // =========================================================================

    fun getAttendanceFlow(targetUid: String? = null): Flow<List<UserAttendance>> = callbackFlow {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_ATTENDANCE)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied fetching user attendance for $uid: ${error.message}")
                    } else {
                        Log.e(TAG, "Error fetching user attendance for $uid: ${error.message}")
                    }
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        val a = doc.toObject(UserAttendance::class.java)
                        a?.copy(id = doc.id)
                    }
                    trySend(records)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun saveAttendance(attendance: UserAttendance, targetUid: String? = null): Result<String> {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            return Result.failure(IllegalStateException("User is not authenticated or Firestore unavailable"))
        }

        return try {
            val collectionRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_ATTENDANCE)

            val docRef = if (attendance.id.isNotEmpty()) {
                collectionRef.document(attendance.id)
            } else {
                collectionRef.document()
            }

            val attendanceId = docRef.id
            val map = hashMapOf(
                "id" to attendanceId,
                "workerName" to attendance.workerName,
                "date" to attendance.date,
                "status" to attendance.status,
                "notes" to attendance.notes,
                "createdAt" to if (attendance.createdAt > 0) attendance.createdAt else System.currentTimeMillis()
            )

            docRef.set(map).await()
            Log.d(TAG, "Successfully saved attendance record $attendanceId for $uid")
            Result.success(attendanceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving attendance for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(attendanceId: String, targetUid: String? = null): Result<Unit> {
        val uid = targetUid ?: getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            return Result.failure(IllegalStateException("User is not authenticated or Firestore unavailable"))
        }

        return try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_ATTENDANCE)
                .document(attendanceId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted attendance $attendanceId for $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting attendance $attendanceId for $uid: ${e.message}")
            Result.failure(e)
        }
    }
}
