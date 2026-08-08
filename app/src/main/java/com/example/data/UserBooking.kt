package com.example.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserBooking(
    val id: String = "",
    val type: String = "Local Plants", // Local Plants | Imported Plants | Imported Rootstock | Pruning | Site Visit
    val itemName: String = "",
    val variety: String = "", // M9T337, MM111, Geneva G-41, Geneva G-11, Geneva G-214, Geneva G-969, Geneva G-35, Geneva G-979, Geneva G-890
    val season: String = "", // Summer | Winter
    val farmerName: String = "",
    val quantity: Int? = null,
    val bookingDate: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
