package com.example.rideboard.altitude.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rideboard.altitude.AltitudeEntity

@Dao
interface AltitudeDao {

    @Query("SELECT altitude FROM altitudes WHERE provider = :provider AND latitude = :latitude AND longitude = :longitude LIMIT 1")
    suspend fun getAltitude(provider: String, latitude: Double, longitude: Double): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(altitude: AltitudeEntity)

    @Query("DELETE FROM altitudes WHERE provider = :provider AND latitude = :latitude AND longitude = :longitude")
    suspend fun delete(provider: String, latitude: Double, longitude: Double)

    @Query("SELECT COUNT(*) FROM altitudes")
    suspend fun countAll(): Int
}