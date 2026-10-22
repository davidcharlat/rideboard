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

    private fun setRoute(points: List<GeoPoint>) {
        route = PlannedRoute(points)
        routeStart = 0
        _plannedRoute.value = points
    }

    fun clearRoute() {
        route = null
        routeStart = 0
        _plannedRoute.value = emptyList()
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
            }
        }
    }


    class PlannedRoute(val points: List<GeoPoint>) {

        // cum[i] = longueur du tracé entre le point 0 et le point i, en mètres
        private val cum = DoubleArray(points.size).also { c ->
            for (i in 1 until points.size) {
                c[i] = c[i - 1] + points[i - 1].distanceToAsDouble(points[i])
            }
        }

        /** Premier indice dans [from, to) à moins de thresholdM de la position, ou -1. */
        fun findFirstWithin(lat: Double, lon: Double, from: Int, to: Int, thresholdM: Double): Int {
            val here = GeoPoint(lat, lon)
            var i = from
            while (i < to) {
                val d = here.distanceToAsDouble(points[i])
                if (d < thresholdM) return i
                // Les points situés à moins de (d - thresholdM) mètres de tracé ne peuvent pas convenir
                val target = cum[i] + (d - thresholdM)
                var j = i + 1
                while (j < to && cum[j] < target) j++
                i = j
            }
            return -1
        }

        /** Premier indice (>= from) dont la longueur de tracé dépasse cum[from] + lengthM. */
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

    private fun routeFile() = File(getApplication<Application>().filesDir, "brouter/route.tsv")

    private suspend fun saveRoute(points: List<GeoPoint>) = withContext(Dispatchers.IO) {
        val f = routeFile()
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, "route.tsv.tmp")
        tmp.bufferedWriter().use { w ->
            points.forEach { w.write("${it.latitude}\t${it.longitude}\n") }
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
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
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
                    val points = result.getOrNull()
                    if (points.isNullOrEmpty()) {
                        Log.d("ROUTE", "variante $idx : ${result.exceptionOrNull()?.message}")
                        if (idx == 0) {
                            _planStatus.value = "Échec : ${result.exceptionOrNull()?.message ?: "aucun itinéraire"}"
                            return@launch
                        }
                        break
                    }
                    if (_routeVariants.value.none { it == points }) {
                        _routeVariants.value = _routeVariants.value + listOf(points)
                    }
                }
                _planStatus.value = "${_routeVariants.value.size} itinéraire(s) trouvé(s)"
            } finally {
                _planning.value = false
            }
        }
    }

    fun commitVariant(index: Int) {
        val leg = _routeVariants.value.getOrNull(index) ?: return
        _legs.value = _legs.value + listOf(leg)
        clearVariants()
    }

    fun clearPlan() {
        _legs.value = emptyList()
        clearVariants()
    }


    fun clearVariants() {
        _routeVariants.value = emptyList()
        _planStatus.value = ""
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


/* ci dessous version du 18/10/25class

RideViewModel : ViewModel() {

    init {
        viewModelScope.launch {
            AppEvents.gpsSamplesFlow.collect { sample ->
                onNewSample(sample)
            }
        }
    }
    private val _currentSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val currentSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _currentSpeedScreen

    private val _maxSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val maxSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _maxSpeedScreen

    private val _averageSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val averageSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _averageSpeedScreen

    private val _currentDistanceScreen = androidx.lifecycle.MutableLiveData<Double>()
    val currentDistanceScreen: androidx.lifecycle.LiveData<Double> get() = _currentDistanceScreen

    private val _currentAltitudeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val currentAltitudeScreen: androidx.lifecycle.LiveData<Double> get() = _currentAltitudeScreen

    private val _maxAltitudeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val maxAltitudeScreen: androidx.lifecycle.LiveData<Double> get() = _maxAltitudeScreen

    private val _minAltitudeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val minAltitudeScreen: androidx.lifecycle.LiveData<Double> get() = _minAltitudeScreen

    private val _travelTimeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val travelTimeScreen: androidx.lifecycle.LiveData<Double> get() = _travelTimeScreen

    private val _cumulativeElevationGainScreen = androidx.lifecycle.MutableLiveData<Double>()
    val cumulativeElevationGainScreen: androidx.lifecycle.LiveData<Double> get() = _cumulativeElevationGainScreen

    private val _currentVerticalSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val currentVerticalSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _currentVerticalSpeedScreen

    private val _maxVerticalSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val maxVerticalSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _maxVerticalSpeedScreen

    private val _minVerticalSpeedScreen = androidx.lifecycle.MutableLiveData<Double>()
    val minVerticalSpeedScreen: androidx.lifecycle.LiveData<Double> get() = _minVerticalSpeedScreen

    private val _currentSlopeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val currentSlopeScreen: androidx.lifecycle.LiveData<Double> get() = _currentSlopeScreen

    private val _maxSlopeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val maxSlopeScreen: androidx.lifecycle.LiveData<Double> get() = _maxSlopeScreen

    private val _minSlopeScreen = androidx.lifecycle.MutableLiveData<Double>()
    val minSlopeScreen: androidx.lifecycle.LiveData<Double> get() = _minSlopeScreen

    fun updateCurrentSpeedScreen(newSpeed: Double) {
        _currentSpeedScreen.postValue(newSpeed)
    }

    fun updateMaxSpeedScreen(newSpeed: Double) {
        _maxSpeedScreen.postValue(newSpeed)
    }

    fun updateAverageSpeedScreen(newSpeed: Double) {
        _averageSpeedScreen.postValue(newSpeed)
    }

    fun updateCurrentDistanceScreen(newDistance: Double) {
        _currentDistanceScreen.postValue(newDistance)
    }

    fun updateCurrentAltitudeScreen(newAltitude: Double) {
        _currentAltitudeScreen.postValue(newAltitude)
    }

    fun updateMaxAltitudeScreen(newAltitude: Double) {
        _maxAltitudeScreen.postValue(newAltitude)
    }

    fun updateMinAltitudeScreen(newAltitude: Double) {
        _minAltitudeScreen.postValue(newAltitude)
    }

    fun updateTravelTimeScreen(newTime: Double) {
        _travelTimeScreen.postValue(newTime)
    }

    fun updateCumulativeElevationGainScreen(newGain: Double) {
        _cumulativeElevationGainScreen.postValue(newGain)
    }

    fun updateCurrentVerticalSpeedScreen(newSpeed: Double) {
        _currentVerticalSpeedScreen.postValue(newSpeed)
    }

    fun updateMaxVerticalSpeedScreen(newSpeed: Double) {
        _maxVerticalSpeedScreen.postValue(newSpeed)
    }

    fun updateMinVerticalSpeedScreen(newSpeed: Double) {
        _minVerticalSpeedScreen.postValue(newSpeed)
    }

    fun updateCurrentSlopeScreen(newSlope: Double) {
        _currentSlopeScreen.postValue(newSlope)
    }

    fun updateMaxSlopeScreen(newSlope: Double) {
        _maxSlopeScreen.postValue(newSlope)
    }

    fun updateMinSlopeScreen(newSlope: Double) {
        _minSlopeScreen.postValue(newSlope)
    }

    fun onNewSample(sample: GpsSample) {
        viewModelScope.launch(Dispatchers.Default) {
            // Vitesse
            calculateCurrentSpeedScreen(GpsBuffer)?.let { updateCurrentSpeedScreen(it) }
            calculateMaxSpeedScreen(GpsBuffer)?.let { updateMaxSpeedScreen(it) }
            calculateAverageSpeedScreen(GpsBuffer)?.let { updateAverageSpeedScreen(it) }

            // Distance
            calculateCurrentDistanceScreen(GpsBuffer)?.let { updateCurrentDistanceScreen(it) }

            // Altitude
            calculateCurrentAltitudeScreen(GpsBuffer)?.let { updateCurrentAltitudeScreen(it) }
            calculateMaxAltitudeScreen(GpsBuffer)?.let { updateMaxAltitudeScreen(it) }
            calculateMinAltitudeScreen(GpsBuffer)?.let { updateMinAltitudeScreen(it) }

            // Temps
            calculateTravelTimeScreen(GpsBuffer)?.let { updateTravelTimeScreen(it) }

            // Dénivelé cumulé
            calculateCumulativeElevationGainScreen(GpsBuffer)?.let {
                updateCumulativeElevationGainScreen(it)
            }

            // Vitesses verticales
            calculateCurrentVerticalSpeedScreen(GpsBuffer)?.let {
                updateCurrentVerticalSpeedScreen(it)
            }
            calculateMaxVerticalSpeedScreen(GpsBuffer)?.let { updateMaxVerticalSpeedScreen(it) }
            calculateMinVerticalSpeedScreen(GpsBuffer)?.let { updateMinVerticalSpeedScreen(it) }

            // Pentes
            calculateCurrentSlopeScreen(GpsBuffer)?.let { updateCurrentSlopeScreen(it) }
            calculateMaxSlopeScreen(GpsBuffer)?.let { updateMaxSlopeScreen(it) }
            calculateMinSlopeScreen(GpsBuffer)?.let { updateMinSlopeScreen(it) }
        }
    }

}

 */