package com.example.rideboard.service

import android.Manifest
import android.app.Application
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.traceEventStart
import androidx.core.app.NotificationCompat
import com.example.rideboard.AppEvents
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import com.example.rideboard.utils.calculateValuesForBuffer
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin


class LocationService : Service() {

    companion object {
        const val ACTION_START_UPDATES = "com.example.rideboard.action.START_UPDATES"
        const val ACTION_STOP_UPDATES = "com.example.rideboard.action.STOP_UPDATES"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()

        // Initialisation du GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMinUpdateIntervalMillis(1000L).build()

        val altitudeManager = AppConfig.altitudeManager ?: return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val sample = GpsSample(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.time,
                        accuracy = location.accuracy + max(0F, location.accuracy - 5F) + max(0F, location.accuracy - 10F),
                        altitudeGps = location.altitude,
                        altitudeAccuracy = location.verticalAccuracyMeters,
                        speedGps = location.speed,
                        speedAccuracy = location.speedAccuracyMetersPerSecond,
                        altitudeSrtm = null,
                        altitudeIgn = null,
                        altitudeLidarHdMnt = null,
                        altitudeLidarHdMns = null,
                        gpsPointSpeed = 0.0,
                        gpsPointAcceleration = 0.0,
                        gpsPointVerticalSpeed   = 0.0,
                        gpsPointDirection   = null,
                        gpsPointAltitude   = null,
                        gpsPointLatitude   = null,
                        gpsPointLongitude   = null,
                        gpsPointGpsAltitudeAccuracy   = 299.9,
                        gpsPointAltitudeSourceMntMnsOrGps = "",
                        gpsPointSpreadAltitudeAndAltitudeGps   = null,
                        gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps   = 0.0,
                        gpsPointDurationAltitudeNotMnt = 0,
                        gpsPointIsMoving = false,
                        gpsPointIsStarted = false,
                        gpsPointCumulatedGpsPrecision = 0.0,
                        gpsPointIsAltitudeGot = false,
                        gpsPointIsGpsAltitudeGot = false,
                        gpsPointUncorrectedGpsAltitude = null,
                        gpsPointDisplayedVerticalSpeed = 0.0,
                        gpsPointDisplayedAltitude = null,
                        gpsPointDisplayedSpeed = 0.0,
                        gpsPointDurationHighSpreadAltitude = 0,

                        gpsPointScreenValueDouble1 = 0.0,
                        gpsPointScreenValueDouble2 = 0.0,
                        gpsPointScreenValueDouble3 = 0.0,
                        gpsPointScreenValue = false,
                        gpsPointScreenValueString = "",
                        gpsPointScreenValueString2 = "",
                        gpsPointScreenValueString3 = "",
                        gpsPointScreenValueString4 = "",
                        gpsPointScreenValueBoolean = false,
                        gpsPointScreenValueLong = 0,
                        gpsPointScreenValueInt = 0

                    )

                    // Enregistre dans le buffer
                    GpsBuffer.add(sample)
                    correctGpsPosition1MeterToLeft(GpsBuffer)
                    processSampleForAltitudeAccuracy(sample)
                    calculateValuesForBuffer(applicationContext, GpsBuffer)
                    AppConfig.gpsUpdatesFlow.tryEmit(Unit)

                    // Lance la récupération d'altitude
                    serviceScope.launch {
                        // lorsqqu'on passait par appconfig: calculateValues(applicationContext, GpsBuffer)
                        AppEvents.emitGpsSample(sample)
                    }

                    serviceScope.launch {
                        altitudeManager.onNewGpsLocation(sample)
                    }
                }
            }
        }

        // Démarre la localisation
        startForeground(1, createNotification())
        startLocationUpdates()
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "onStartCommand reçu: ${intent?.action}")

        when (intent?.action) {
            ACTION_STOP_UPDATES -> {
                stopLocationUpdates()
                Log.d("LocationService", "Updates GPS arrêtées")
            }
            ACTION_START_UPDATES -> {
                startLocationUpdates()
                Log.d("LocationService", "Updates GPS démarrées")
            }
            else -> {
                // Comportement par défaut (démarrage initial)
                try {
                    startForeground(1, createNotification())
                    startLocationUpdates()
                } catch (e: Exception) {
                    Log.e("LocationService", "Erreur au démarrage du service", e)
                }
            }
        }

        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun createNotification(): Notification {
        val channelId = "ride_tracking_channel"
        val channel = NotificationChannel(
            channelId,
            "Ride Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Enregistrement en cours")
            .setContentText("Votre sortie est en cours d’enregistrement…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }


    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

}
fun processSampleForAltitudeAccuracy(sample: GpsSample): GpsSample {
    val last = GpsBuffer.getNthBeforeLast(2) ?: return sample

    val currAlt: Double? = sample.altitudeGps
    val lastAlt: Double? = last.altitudeGps

    var markInvalid = false

    //pour trouver pq des accuracy sont marquée 9999 à tort
    var test : Float = 0.0F

    // Règle 1 : saut brutal (> 5m)
    if (currAlt != null && lastAlt != null && abs(currAlt - lastAlt) > 6.0) {
        markInvalid = true
        test = 1F
    }
    // Règle 2 : suite d'erreurs (dernier invalide + même altitude)
    else if (
        last.altitudeAccuracy == 9999f &&
        currAlt != null && lastAlt != null &&
        currAlt == lastAlt
    ) {
        markInvalid = true
        test = 2F
    }
    // Règle 3 : blocage sur valeur incohérente (résiduel d'arrondi identique et non nul)
    else if (currAlt != null && lastAlt != null) {
        val residualSample = roundToMillimeter(currAlt) - currAlt
        // val residualLast   = roundToMillimeter(lastAlt) - lastAlt
        if (currAlt == lastAlt && residualSample != 0.0) {
            markInvalid = true
            test = 3F

            // Marquer rétroactivement l’échantillon précédent comme invalide (accuracy = 999999f)
            if (GpsBuffer.size > 1)
                GpsBuffer.getNthBeforeLast(2)?.altitudeAccuracy = (9999f - test)
            }
        }

    val finalAccuracy: Float? = if (markInvalid) (9999f-test) else sample.altitudeAccuracy
    return sample.copy(altitudeAccuracy = finalAccuracy)
}

private fun roundToMillimeter(v: Double): Double {
    // Arrondi au 0.001 m le plus proche
    return ((v+0.0002) * 1000.0).roundToInt() / 1000.0
}

fun correctGpsPosition1MeterToLeft(buffer: GpsBuffer) {
    if (buffer.size < 3) return
    val lastDirection = buffer.getNthBeforeLast(3)?.gpsPointDirection
    if (lastDirection != null) {
        val sample = buffer.getNthBeforeLast(1) ?: return
        sample.latitude += cos(lastDirection) * 0.000013 //- en roulant à gauche
        sample.longitude -= sin(lastDirection) * 0.000013 //+= idem
    }
}
/*package com.example.rideboard.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.example.rideboard.AppEvents
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import com.example.rideboard.buffer.GpsBuffer

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- Initialisations paresseuses (sécurisées) ---
    private val fusedLocationClient by lazy {
        Log.d(TAG, "Initialisation de fusedLocationClient")
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationRequest by lazy {
        Log.d(TAG, "Initialisation de locationRequest")
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // toutes les 1 seconde
        ).setMinUpdateIntervalMillis(1000L).build()
    }

    private val locationCallback by lazy {
        Log.d(TAG, "Initialisation de locationCallback")
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    handleNewLocation(location)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service créé")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service démarré (startId=$startId)")
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            Log.i(TAG, "Démarrage des mises à jour de localisation")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Erreur de permission GPS : ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue dans startLocationUpdates : ${e.message}", e)
        }
    }

    private fun handleNewLocation(location: Location) {
        serviceScope.launch {
            try {
                val sample = GpsSample(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    accuracy = location.accuracy,
                    altitudeGps = location.altitude,
                    altitudeAccuracy = location.verticalAccuracyMeters,
                    altitudeSrtm = null,
                    altitudeIgn = null,
                    altitudeLidarHdMnt = null,
                    altitudeLidarHdMns = null
                )

                GpsBuffer.add(sample)
                // Lance la récupération d'altitude
                serviceScope.launch {
                    AppConfig.altitudeManager?.onNewGpsLocation(sample)

                    // 🔥 Diffuse le sample pour que le ViewModel le reçoive
                    AppEvents.emitGpsSample(sample)
                }

                Log.d(TAG, "Nouvelle position : ${sample.latitude}, ${sample.longitude}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur dans handleNewLocation : ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        Log.w(TAG, "Service détruit — arrêt des mises à jour GPS")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression des updates : ${e.message}", e)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
*/
