package com.example.rideboard.altitude.providers

import android.util.Log
import com.example.rideboard.altitude.AltitudeProvider
import com.example.rideboard.altitude.AltitudeRequestQueue
import com.example.rideboard.altitude.QueuedAltitudeRequest
import com.example.rideboard.altitude.database.AltitudeRepository
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

class SrtmProvider(private val repository: AltitudeRepository) : AltitudeProvider {
    override val name = "SRTM"

    override suspend fun getAltitude(lat: Double, lon: Double): Double? {
        val cached = repository.getAltitudeFromDb(name, lat, lon)
        if (cached != null) return cached

        val url = "https://api.opentopodata.org/v1/srtm30m?locations=$lat,$lon"

        //debug pour tester le nombre de requetes simultanée, à enlever aussi dans le finally
        AppConfig.activeAltitudeSrtmRequests++

        val request = Request.Builder().url(url).build()
//paquet de test:
        return withContext(Dispatchers.IO) {
            try {

                AppConfig.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    /*
                implémentation alternative
                val body = response.body?.string() ?: return null
                val json = Json.parseToJsonElement(body).jsonObject
                val results = json["results"]?.jsonArray
                val altitude = results?.firstOrNull()
                    ?.jsonObject?.get("elevation")?.jsonPrimitive?.doubleOrNull
                */
                    val body = response.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val results = json.getJSONArray("results").getJSONObject(0)
                    val altitude = results.getDouble("elevation")
                    repository.insertAltitude(name, lat, lon, altitude)
                    altitude
                }
            } catch (e: Exception) {
                AltitudeRequestQueue.add(
                    QueuedAltitudeRequest(
                        providerName = name,
                        latitude = lat,
                        longitude = lon
                    )
                )
                null
            }
            finally {
                AppConfig.activeAltitudeSrtmRequests--
            }
        }
    }
}