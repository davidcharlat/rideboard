package com.example.rideboard.altitude.providers

import com.example.rideboard.altitude.AltitudeProvider
import com.example.rideboard.altitude.AltitudeRequestQueue
import com.example.rideboard.altitude.QueuedAltitudeRequest
import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.config.AppConfig
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LidarHdProvider(
    private val repository: AltitudeRepository,
    private val which: String // "MNT" ou "MNS"
) : AltitudeProvider {

    // Expose une source distincte pour chaque variante
    override val name: String = when (which.uppercase()) {
        "MNS" -> "L_MNS"
        "MNT" -> "L_MNT"
        else -> "L_${which.uppercase()}"
    }

    override suspend fun getAltitude(lat: Double, lon: Double): Double? {
        // 1) DB
        val cached = repository.getAltitudeFromDb(name, lat, lon)
        if (cached != null) return cached

        // 2) URL IGN LIDAR (measures=true pour obtenir MNT/MNS)
        val url = "https://data.geopf.fr/altimetrie/1.0/calcul/alti/rest/elevation.json" +
                "?lon=$lon&lat=$lat&resource=ign_lidar_hd_mnx_mono_wld&zonly=false&measures=true"

        //debug pour tester le nombre de requetes simultanée, à enlever aussi dans le finally
        AppConfig.activeAltitudeLidarRequests++

        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO){
            try {

                AppConfig.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    val json = Json.parseToJsonElement(body).jsonObject

                    // structure: {"elevations":[ { "lon":..., "lat":..., "z":460.85, "acc":"...", "measures":[ { "z":460.85, "title":"...MNT..." }, {...MNS...} ] } ]}
                    val elevationsArray = json["elevations"]?.jsonArray ?: return@use null
                    val first = elevationsArray.firstOrNull()?.jsonObject ?: return@use null

                    // try to find measures array
                    val measures = first["measures"]?.jsonArray

                    var altitudeAnswered: Double? = null

                    if (measures != null) {
                        // cherche dans measures selon which
                        val desired = measures.firstOrNull { m ->
                            val title = m.jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: ""
                            title.contains(which, ignoreCase = true)
                        }
                        if (desired != null) {
                            altitudeAnswered = desired.jsonObject["z"]?.jsonPrimitive?.doubleOrNull
                        }
                    }

                    // fallback : si pas de measures, utiliser "z" (général)
                    if (altitudeAnswered == null) {
                        altitudeAnswered = first["z"]?.jsonPrimitive?.doubleOrNull
                    }

                    if (altitudeAnswered != null) {
                        if (altitudeAnswered < -500.0 ) {
                            AppConfig.isLidarHdAvailable = false
                            return@use null
                        }
                        repository.insertAltitude(name, lat, lon, altitudeAnswered)
                    }

                    altitudeAnswered
                }
            } catch (e: Exception) {
                // en cas d'erreur réseau, on queue la demande structurée
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
                AppConfig.activeAltitudeLidarRequests--
            }
        }
    }
}