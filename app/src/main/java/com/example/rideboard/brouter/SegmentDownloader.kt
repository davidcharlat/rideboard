package com.example.rideboard.brouter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object SegmentDownloader {

    fun tileName(latBase: Int, lonBase: Int): String {
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

    suspend fun download(
        context: Context,
        name: String,
        force: Boolean = false,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val target = File(BRouterFiles.segmentsDir(context), name)
        if (target.exists() && !force) return@withContext true
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "$name.part")
        try {
            val conn = URL("https://brouter.de/brouter/segments4/$name").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext false
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        ensureActive()                       // permet d'annuler en cours de route
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
            // L'ancien fichier n'est supprimé qu'une fois le nouveau entièrement reçu
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        } catch (e: CancellationException) {
            tmp.delete()
            throw e
        } catch (e: Exception) {
            tmp.delete()
            false
        }
    }
}