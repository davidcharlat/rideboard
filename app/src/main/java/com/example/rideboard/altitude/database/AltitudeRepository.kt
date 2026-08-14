package com.example.rideboard.altitude.database

import com.example.rideboard.altitude.database.AltitudeDao
import com.example.rideboard.altitude.AltitudeEntity

class AltitudeRepository(private val dao: AltitudeDao) {

    suspend fun getAltitudeFromDb(provider: String, lat: Double, lon: Double): Double? {
        return dao.getAltitude(provider, lat, lon)
    }

    suspend fun insertAltitude(provider: String, latitude: Double, longitude: Double, altitude: Double) {
        dao.insert(AltitudeEntity(provider, latitude, longitude, altitude))
    }

    suspend fun deleteAltitude(provider: String, latitude: Double, longitude: Double) {
        dao.delete(provider, latitude, longitude)
    }

    suspend fun countAll(): Int {
        return dao.countAll()
    }
}