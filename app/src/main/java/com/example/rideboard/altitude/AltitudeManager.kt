package com.example.rideboard.altitude

import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import com.example.rideboard.utils.roundToNearestStep


/**
 * Gère la logique : DB -> Buffer / Providers -> DB -> Buffer
 */
class AltitudeManager(
    private val repository: AltitudeRepository,
    private val providers: Map<String, AltitudeProvider>
) {
    suspend fun onNewGpsLocation(sample: GpsSample) {
        // 1⃣ Pour chaque provider, on tente d’obtenir l’altitude
        getSrtmAltitudeAndFillBuffer(sample)
        getIgnAndLidarHdAltitudeAndFillBuffer(sample)
    }

    /**
     * SRTM
     */
    suspend fun getSrtmAltitudeAndFillBuffer(sample: GpsSample) {
        val latitude = sample.latitude
        val longitude = sample.longitude
        val lat = roundToNearestStep(latitude,5000)
        val lon = roundToNearestStep(longitude,5000)

        val source = "SRTM"
        val cached = repository.getAltitudeFromDb(source, lat, lon)
        if (cached != null) {
            GpsBuffer.updateAltitude(source, sample, cached)
            try {
                AltitudeRequestQueue.processOne(AppConfig.repository,providers[source])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val altitude = providers[source]?.getAltitude(lat, lon)
        if (altitude != null) {
            repository.insertAltitude(source, lat, lon, altitude)
            GpsBuffer.updateAltitude(source, sample, altitude)
        }
    }

    /**
     * IGN + LIDAR
     */
    suspend fun getIgnAndLidarHdAltitudeAndFillBuffer(sample: GpsSample) {
        val latitude = sample.latitude
        val longitude = sample.longitude
        val lat = roundToNearestStep(latitude, 40000)
        val lon = roundToNearestStep(longitude, 40000)
        // Vérifie d'abord la DB
        val foundLidarMntInDb = repository.getAltitudeFromDb("L_MNT", lat, lon)
        val foundLidarMnsInDb = repository.getAltitudeFromDb("L_MNS", lat, lon)

        if (foundLidarMnsInDb != null && foundLidarMntInDb != null) {
            GpsBuffer.updateAltitude("L_MNT", sample, foundLidarMntInDb)
            GpsBuffer.updateAltitude("L_MNS", sample, foundLidarMnsInDb)
            AppConfig.isLidarHdAvailable = true
            getIgnAltitudeAndFillBuffer(sample)
            return
        }

        // Sinon → fetch internet pour LIDAR
        if (AppConfig.isLidarHdAvailable) {
            val altitudeMnt = providers["L_MNT"]?.getAltitude(lat, lon)
            val altitudeMns = providers["L_MNS"]?.getAltitude(lat, lon)

            if (altitudeMns != null && altitudeMnt != null) {
                repository.insertAltitude("L_MNT", lat, lon, altitudeMnt)
                GpsBuffer.updateAltitude("L_MNT", sample, altitudeMnt)
                repository.insertAltitude("L_MNS", lat, lon, altitudeMns)
                GpsBuffer.updateAltitude("L_MNS", sample, altitudeMns)
                return
            }
        }
        getIgnAltitudeAndFillBuffer(sample)
    }

    /**
     * IGN seul
     */
    private suspend fun getIgnAltitudeAndFillBuffer(sample: GpsSample) {
        val source = "IGN"
        val latitude = sample.latitude
        val longitude = sample.longitude
        val lat = roundToNearestStep(latitude,20000)
        val lon = roundToNearestStep(longitude,20000)
        val cached = repository.getAltitudeFromDb(source, lat, lon)
        if (cached != null) {
            GpsBuffer.updateAltitude (source, sample, cached)
            try {
                AltitudeRequestQueue.processOne(AppConfig.repository,providers[source])
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val loadedAltitude = providers[source]?.getAltitude(lat, lon)
        if (loadedAltitude != null) {
            repository.insertAltitude(source, lat, lon, loadedAltitude)
            GpsBuffer.updateAltitude(source, sample, loadedAltitude)
        }
    }
}