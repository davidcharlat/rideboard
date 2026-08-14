package com.example.rideboard.altitude

import com.example.rideboard.buffer.GpsBuffer
interface AltitudeProvider {
    val name: String
    suspend fun getAltitude(lat: Double, lon: Double): Double?
}