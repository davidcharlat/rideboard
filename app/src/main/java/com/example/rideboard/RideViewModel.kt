package com.example.rideboard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.utils.ScreenValues
import com.example.rideboard.utils.calculateScreenValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.rideboard.brouter.RoutePlanner
import com.example.rideboard.brouter.SegmentDownloader
import com.example.rideboard.config.AppConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val _screenValues = mutableStateOf(ScreenValues())
    val screenValues: State<ScreenValues> = _screenValues

    private val _trackPoints = mutableStateOf<List<Pair<Double, Double>>>(emptyList())
    val trackPoints: State<List<Pair<Double, Double>>> = _trackPoints

    private var route: PlannedRoute? = null
    private var routeStart = 0

    data class RouteRemaining(val meters: Double, val ascentM: Double)

    private val ASCENT_THRESHOLD_M = 0.0

    private val _routeRemaining = mutableStateOf<RouteRemaining?>(null)
    val routeRemaining: State<RouteRemaining?> = _routeRemaining

    private fun publishRemaining() {
        val r = route
        _routeRemaining.value =
            if (r == null) null else RouteRemaining(r.remainingMeters(routeStart), r.remainingAscent(routeStart))
    }

    private fun setRoute(points: List<GeoPoint>) {
        route = PlannedRoute(points, ASCENT_THRESHOLD_M)
        routeStart = 0
        _plannedRoute.value = points
        publishRemaining()
    }

    fun clearRoute() {
        route = null
        routeStart = 0
        _plannedRoute.value = emptyList()
        publishRemaining()
    }

    private fun updateRouteProgress(lat: Double, lon: Double) {
        val r = route ?: return
        val threshold = 30.0

        // 1) recherche dans une fenêtre de 2 km de tracé devant la position atteinte
        val nearEnd = r.indexAfter(routeStart, 2000.0)
        var idx = r.findFirstWithin(lat, lon, routeStart, nearEnd, threshold)
        // 2) sinon, recherche sur tout le reste du tracé
        if (idx < 0) idx = r.findFirstWithin(lat, lon, nearEnd, r.points.size, threshold)

        if (idx > routeStart) {
            if (idx >= r.points.size - 1) {
                clearRoute()   // arrivé
            } else {
                routeStart = idx
                _plannedRoute.value = r.points.subList(idx, r.points.size).toList()
                publishRemaining()
            }
        }
    }


    class PlannedRoute(val points: List<GeoPoint>, ascentThresholdM: Double = 0.0) {

        private val cum = DoubleArray(points.size)      // distance cumulée depuis le point 0 (m)
        private val ascent = DoubleArray(points.size)   // D+ cumulé depuis le point 0 (m)

        init {
            var ref = points.firstOrNull()?.altitude ?: 0.0
            for (i in 1 until points.size) {
                cum[i] = cum[i - 1] + points[i - 1].distanceToAsDouble(points[i])
                ascent[i] = ascent[i - 1]
                val d = points[i].altitude - ref
                if (d > 0 && d >= ascentThresholdM) {
                    ascent[i] += d
                    ref = points[i].altitude
                } else if (d < 0 && -d >= ascentThresholdM) {
                    ref = points[i].altitude
                }
            }
        }

        fun remainingMeters(from: Int): Double = if (points.isEmpty()) 0.0 else cum.last() - cum[from]
        fun remainingAscent(from: Int): Double = if (points.isEmpty()) 0.0 else ascent.last() - ascent[from]

        fun findFirstWithin(lat: Double, lon: Double, from: Int, to: Int, thresholdM: Double): Int {
            val here = GeoPoint(lat, lon)
            var i = from
            while (i < to) {
                val d = here.distanceToAsDouble(points[i])
                if (d < thresholdM) return i
                val target = cum[i] + (d - thresholdM)
                var j = i + 1
                while (j < to && cum[j] < target) j++
                i = j
            }
            return -1
        }

        fun indexAfter(from: Int, lengthM: Double): Int {
            var j = from
            while (j < points.size && cum[j] < cum[from] + lengthM) j++
            return j
        }
    }

    private val _plannedRoute = mutableStateOf<List<GeoPoint>>(emptyList())
    val plannedRoute: State<List<GeoPoint>> = _plannedRoute

    init {
        loadTrackFromTsv()
        viewModelScope.launch {
            AppConfig.gpsUpdatesFlow.collect {
                val newValues = calculateScreenValues(GpsBuffer)
                _screenValues.value = newValues

                val lat = newValues.latitude
                val lon = newValues.longitude
                // On ajoute le nouveau point à la trace si les coordonnées sont valides
                if (lat != null && lon != null) {
                    _trackPoints.value += Pair(lat, lon)
                    //et on efface la plannedRoute si besoin
                    updateRouteProgress(lat, lon)
                }
            }
        }
    }
    private val _routeVariants = mutableStateOf<List<List<GeoPoint>>>(emptyList())
    val routeVariants: State<List<List<GeoPoint>>> = _routeVariants

    private val _planning = mutableStateOf(false)
    val planning: State<Boolean> = _planning

    private val _planStatus = mutableStateOf("")
    val planStatus: State<String> = _planStatus

    private val _legs = mutableStateOf<List<List<GeoPoint>>>(emptyList())
    val legs: State<List<List<GeoPoint>>> = _legs

    private val _routeDurations = mutableStateOf<List<Int?>>(emptyList())
    val routeDurations: State<List<Int?>> = _routeDurations

    private val _legDurations = mutableStateOf<List<Int?>>(emptyList())
    val legDurations: State<List<Int?>> = _legDurations

    private fun routeFile() = File(getApplication<Application>().filesDir, "brouter/route.tsv")

    private suspend fun saveRoute(points: List<GeoPoint>) = withContext(Dispatchers.IO) {
        val f = routeFile()
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, "route.tsv.tmp")
        tmp.bufferedWriter().use { w ->
            points.forEach { w.write("${it.latitude}\t${it.longitude}\t${it.altitude}\n") }
        }
        tmp.renameTo(f)
    }

    /** Recharge le parcours sauvegardé, comme s'il venait d'être calculé (effacement remis à zéro). */
    fun loadSavedRoute(onMissing: () -> Unit) {
        viewModelScope.launch {
            val points = withContext(Dispatchers.IO) {
                val f = routeFile()
                if (!f.exists()) emptyList<GeoPoint>() else f.readLines().mapNotNull { line ->
                    val p = line.split("\t")
                    val lat = p.getOrNull(0)?.toDoubleOrNull()
                    val lon = p.getOrNull(1)?.toDoubleOrNull()
                    val ele = p.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                    if (lat != null && lon != null) GeoPoint(lat, lon, ele) else null
                }
            }
            if (points.size >= 2) setRoute(points) else onMissing()
        }
    }

    fun computeVariants(start: GeoPoint, end: GeoPoint, profile: String, profileParams: Map<String, String> = emptyMap()) {
        if (_planning.value) return
        viewModelScope.launch {
            _planning.value = true
            _routeVariants.value = emptyList()
            _routeDurations.value = emptyList()
            try {
                val app = getApplication<Application>()

                val tiles = SegmentDownloader.tilesFor(start.latitude, start.longitude, end.latitude, end.longitude)
                for (tile in tiles) {
                    _planStatus.value = "Téléchargement de $tile…"
                    if (!SegmentDownloader.download(app, tile)) {
                        _planStatus.value = "Échec du téléchargement de $tile"
                        return@launch
                    }
                }

                val planner = RoutePlanner(app)
                for (idx in 0..3) {
                    _planStatus.value = "Calcul de l'itinéraire ${idx + 1}…"
                    val result = planner.plan(start.latitude, start.longitude, end.latitude, end.longitude, profile, idx, profileParams)
                    val pair = result.getOrNull()
                    if (pair == null || pair.first.isEmpty()) {
                        Log.d("ROUTE", "variante $idx : ${result.exceptionOrNull()?.message}")
                        if (idx == 0) {
                            _planStatus.value = "Échec : ${result.exceptionOrNull()?.message ?: "aucun itinéraire"}"
                            return@launch
                        }
                        break
                    }
                    val (points, seconds) = pair
                    if (_routeVariants.value.none { it == points }) {
                        _routeVariants.value = _routeVariants.value + listOf(points)
                        _routeDurations.value = _routeDurations.value + listOf(seconds)
                    }
                }
                _planStatus.value = "${_routeVariants.value.size} itinéraire(s) trouvé(s)"
            } finally {
                _planning.value = false
            }
        }
    }

    fun clearVariants() {
        _routeVariants.value = emptyList()
        _routeDurations.value = emptyList()
        _planStatus.value = ""
    }

    fun commitVariant(index: Int) {
        val leg = _routeVariants.value.getOrNull(index) ?: return
        _legs.value = _legs.value + listOf(leg)
        _legDurations.value = _legDurations.value + listOf(_routeDurations.value.getOrNull(index))
        clearVariants()
    }

    fun clearPlan() {
        _legs.value = emptyList()
        _legDurations.value = emptyList()
        clearVariants()
    }

    fun useVariant(index: Int) {
        val all = _legs.value + listOfNotNull(_routeVariants.value.getOrNull(index))
        if (all.isEmpty()) return
        // on retire le premier point des tronçons suivants (il double le dernier du précédent)
        val joined = all.flatMapIndexed { i, leg -> if (i == 0) leg else leg.drop(1) }
        setRoute(joined)
        viewModelScope.launch { saveRoute(joined) }   // voir le point 4
        clearPlan()
    }

    fun toggleNav(onMissing: () -> Unit) {
        if (_plannedRoute.value.isNotEmpty()) clearRoute() else loadSavedRoute(onMissing)
    }

    fun loadTrackFromTsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, "ride.tsv")
            if (file.exists()) {
                try {
                    val points = mutableListOf<Pair<Double, Double>>()
                    file.forEachLine { line ->
                        val tokens = line.split("\t")
                        if (tokens.size >= 3) {
                            val lat = tokens[1].toDoubleOrNull()
                            val lon = tokens[2].toDoubleOrNull()
                            if (lat != null && lon != null) {
                                points.add(Pair(lat, lon))
                            }
                        }
                    }
                    launch(Dispatchers.Main) {
                        _trackPoints.value = points
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun currentPosition(): GeoPoint? {
        val p = GpsBuffer.getLast() ?: return null
        val lat = p.latitude
        val lon = p.longitude
        return GeoPoint(lat, lon)
    }

    fun clearTrack() {
        _trackPoints.value = emptyList()
    }
}
