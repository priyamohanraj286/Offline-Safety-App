package com.example.offlinesafety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single-row user table: id fixed to 1 to keep only one user
@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val contact: String
)
