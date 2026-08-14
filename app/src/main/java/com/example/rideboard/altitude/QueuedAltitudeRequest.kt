package com.example.rideboard.altitude

data class QueuedAltitudeRequest(
    val providerName: String,
    val latitude: Double,
    val longitude: Double,
    var attempts: Int = 0,
    val maxAttempts: Int = 5
)