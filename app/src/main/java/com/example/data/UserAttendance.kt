package com.example.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserAttendance(
    val id: String = "",
    val workerName: String = "",
    val date: String = "",
    val status: String = "Present", // Present | Absent | Leave
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
