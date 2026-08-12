package com.example.offlinesafety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travel")
data class Travel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trainName: String,
    val coachNumber: String,
    val seatNumber: String,
    val startTime: Long
)
