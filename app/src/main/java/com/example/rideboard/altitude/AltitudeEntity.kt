package com.example.rideboard.altitude

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "altitudes",
    primaryKeys = ["provider", "latitude", "longitude"]
)
data class AltitudeEntity(
    val provider: String,        // SRTM, IGN, L_MNT, L_MNS
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)