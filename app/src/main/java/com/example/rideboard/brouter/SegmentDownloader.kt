package com.example.rideboard.brouter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object SegmentDownloader {

    private fun tileName(latBase: Int, lonBase: Int): String {
        val ew = if (lonBase >= 0) "E" else "W"
        val ns = if (latBase >= 0) "N" else "S"
        return "$ew${abs(lonBase)}_$ns${abs(latBase)}.rd5"
    }

    /** Tuiles couvrant la boîte départ/arrivée élargie d'une marge (en degrés). */
    fun tilesFor(lat1: Double, lon1: Double, lat2: Double, lon2: Double, margin: Double = 0.5): Set<String> {
        val minLat = min(lat1, lat2) - margin
        val maxLat = max(lat1, lat2) + margin
        val minLon = min(lon1, lon2) - margin
        val maxLon = max(lon1, lon2) + margin
        val tiles = mutableSetOf<String>()
        var lat = (floor(minLat / 5.0) * 5).toInt()
        while (lat <= maxLat) {
            var lon = (floor(minLon / 5.0) * 5).toInt()
            while (lon <= maxLon) {
                tiles += tileName(lat, lon)
                lon += 5
            }
            lat += 5
        }
        return tiles
    }

    suspend fun download(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(BRouterFiles.segmentsDir(context), name)
        if (target.exists()) return@withContext true
        val tmp = File(target.parentFile, "$name.part")
        try {
            val conn = URL("https://brouter.de/brouter/segments4/$name").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (conn.responseCode != 200) return@withContext false
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            tmp.renameTo(target)
        } catch (e: Exception) {
            tmp.delete()
            false
        }
    }
}