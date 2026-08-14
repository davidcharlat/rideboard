package com.example.rideboard

import android.app.Application
import android.content.Context
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
import com.example.rideboard.config.AppConfig
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val _screenValues = mutableStateOf(ScreenValues())
    val screenValues: State<ScreenValues> = _screenValues

    init {
        viewModelScope.launch {
            AppConfig.gpsUpdatesFlow.collect {
                _screenValues.value = calculateScreenValues(GpsBuffer)
            }
        }
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