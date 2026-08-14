package com.example.rideboard.altitude.providers

import com.example.rideboard.altitude.AltitudeProvider
import com.example.rideboard.altitude.AltitudeRequestQueue
import com.example.rideboard.altitude.QueuedAltitudeRequest
import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject

class IgnProvider(private val repository: AltitudeRepository) : AltitudeProvider {
    override val name: String = "IGN"

    override suspend fun getAltitude(lat: Double, lon: Double): Double? {
        // 1. Vérifie la DB
        val cached = repository.getAltitudeFromDb(name, lat, lon)
        if (cached != null) return cached

        // 2. Construit l’URL IGN
        val url = "https://data.geopf.fr/altimetrie/1.0/calcul/alti/rest/elevation.json" +
                "?lon=$lon&lat=$lat&resource=ign_rge_alti_wld&zonly=true"

        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                // 3. Exécute la requête
                AppConfig.httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val elevationsArray = json.optJSONArray("elevations") ?: return@use null
                    if (elevationsArray.length() == 0) return@use null
                    val altitude = elevationsArray.getDouble(0)
                    /*
                autre implémantation:
                val body = response.body?.string() ?: return null
                val json = Json.parseToJsonElement(body).jsonObject
                val elevations = json["elevations"]?.jsonArray
                val altitude = elevations?.firstOrNull()?.jsonPrimitive?.doubleOrNull
                 */
                    if (altitude < -500.0) {
                        AppConfig.isIgnAvailable = false
                        return@use null
                    }
                    repository.insertAltitude(name, lat, lon, altitude)
                    altitude
                }
            } catch (e: Exception) {
                // 6. File d’attente si échec
                AltitudeRequestQueue.add(
                    QueuedAltitudeRequest(
                        providerName = name,
                        latitude = lat,
                        longitude = lon
                    )
                )
                null
            }
        }
    }
}