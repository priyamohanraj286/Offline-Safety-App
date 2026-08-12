package com.example.offlinesafety.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TravelDao {
    @Insert
    suspend fun insert(travel: Travel)

    @Query("SELECT * FROM travel ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestTravel(): Travel?

    @Query("DELETE FROM travel")
    suspend fun clearAll()
}
