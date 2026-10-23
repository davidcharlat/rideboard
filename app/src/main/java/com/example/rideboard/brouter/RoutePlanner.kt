package com.example.rideboard.brouter

import android.content.Context
import android.util.Log
import btools.router.FormatGpx
import btools.router.RoutingContext
import btools.router.RoutingEngine
import btools.router.RoutingParamCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File

class RoutePlanner(private val context: Context) {

    suspend fun plan(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        profileName: String,
        alternativeIdx: Int = 0,
        profileParams: Map<String, String> = emptyMap()
    ): Result<Pair<List<GeoPoint>, Int?>> = withContext(Dispatchers.Default) {
        try {
            val profileFile = File(BRouterFiles.profilesDir(context), "$profileName.brf")
            val segmentDir = BRouterFiles.segmentsDir(context)
            //Log.d("ROUTE_TEST", "profil=${profileFile.absolutePath} existe=${profileFile.exists()}")
            //Log.d("ROUTE_TEST", "segments=${segmentDir.list()?.toList()}")

            val rc = RoutingContext()
            rc.localFunction = profileFile.absolutePath

            // Même chemin que l'API officielle : "lon,lat|lon,lat"
            val collector = RoutingParamCollector()
            val waypoints = collector.getWayPointList("$startLon,$startLat|$endLon,$endLat")
            Log.d("ROUTE_TEST", "waypoints=${waypoints.size}")
            collector.setParams(rc, waypoints, mapOf("alternativeidx" to alternativeIdx.toString()))
            val clean = profileParams.mapValues {
                when (val v = it.value.trim().replace(',', '.')) {
                    "true" -> "1"
                    "false" -> "0"
                    else -> v
                }
            }
            Log.d("ROUTE", "profileParams=$clean")
            if (clean.isNotEmpty()) collector.setProfileParams(rc, HashMap(clean))
            val engine = RoutingEngine(
                null, null, segmentDir, waypoints, rc,
                RoutingEngine.BROUTER_ENGINEMODE_ROUTING
            )
            engine.quite = true
            engine.doRun(90_000L)   // 60 s, comme le worker

            Log.d("ROUTE_TEST", "errorMessage=${engine.errorMessage}")
            engine.errorMessage?.let { return@withContext Result.failure(Exception(it)) }

            val track = engine.foundTrack
                ?: return@withContext Result.failure(Exception("foundTrack = null"))

            val gpx = FormatGpx(rc).format(track)
            Log.d("ROUTE", gpx.lineSequence().drop(1).firstOrNull() ?: "")

            val tagRegex = Regex("<trkpt([^>]*)>(?:\\s*<ele>([^<]*)</ele>)?")
            val latRegex = Regex("lat=\"([^\"]+)\"")
            val lonRegex = Regex("lon=\"([^\"]+)\"")

            var lastEle = 0.0
            val points = tagRegex.findAll(gpx).mapNotNull { m ->
                val attrs = m.groupValues[1]
                val lat = latRegex.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull()
                val lon = lonRegex.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull()
                if (lat == null || lon == null) return@mapNotNull null
                val ele = m.groupValues[2].toDoubleOrNull() ?: lastEle   // altitude absente : on reprend la précédente
                lastEle = ele
                GeoPoint(lat, lon, ele)
            }.toList()
            val seconds = parseDurationSeconds(gpx)
            Log.d("ROUTE", "durée du modèle = $seconds s")
            Result.success(points to seconds)
        } catch (e: Throwable) {
            Log.e("ROUTE_TEST", "Exception", e)
            Result.failure(e)
        }
    }
}


private fun parseDurationSeconds(gpx: String): Int? {
    val header = gpx.lineSequence().firstOrNull { "track-length" in it } ?: return null
    val m = Regex("""time=(?:(\d+)h)?\s*(?:(\d+)m)?\s*(?:(\d+)s)?""").find(header) ?: return null
    val h = m.groupValues[1].toIntOrNull()
    val mi = m.groupValues[2].toIntOrNull()
    val s = m.groupValues[3].toIntOrNull()
    if (h == null && mi == null && s == null) return null
    return (h ?: 0) * 3600 + (mi ?: 0) * 60 + (s ?: 0)
}
