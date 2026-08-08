package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "advance_payments",
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["workerId"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workerId"])
    ]
)
data class AdvancePayment(
    @PrimaryKey(autoGenerate = true)
    val paymentId: Long = 0,
    val workerId: Long,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
