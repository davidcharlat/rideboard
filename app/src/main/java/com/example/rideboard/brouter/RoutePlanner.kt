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
    ): Result<List<GeoPoint>> = withContext(Dispatchers.Default) {
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

            val latRegex = Regex("lat=\"([^\"]+)\"")
            val lonRegex = Regex("lon=\"([^\"]+)\"")
            val points = Regex("<trkpt[^>]*>").findAll(gpx).mapNotNull { m ->
                val lat = latRegex.find(m.value)?.groupValues?.get(1)?.toDoubleOrNull()
                val lon = lonRegex.find(m.value)?.groupValues?.get(1)?.toDoubleOrNull()
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            }.toList()

            Result.success(points)
        } catch (e: Throwable) {
            Log.e("ROUTE_TEST", "Exception", e)
            Result.failure(e)
        }
    }
}

