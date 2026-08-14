package com.example.rideboard.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.rideboard.altitude.AltitudeManager
import com.example.rideboard.altitude.AltitudeProvider
import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.altitude.providers.IgnProvider
import com.example.rideboard.altitude.providers.LidarHdProvider
import com.example.rideboard.altitude.providers.SrtmProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AppConfig {
    // Variables globales
    var isLidarHdAvailable: Boolean = true
    var isIgnAvailable: Boolean = true
    var isRecording by mutableStateOf(true)
    lateinit var repository: AltitudeRepository
    lateinit var providers: Map<String, AltitudeProvider>
    var altitudeManager: AltitudeManager? = null
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    val gpsUpdatesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    //var elevationGain: Float = 0F
    //var elapsedTime: Float = 0F
    /* var gpsPointTime :Long = 0
    var gpsPointSpeed :Double = 0.0
    var gpsPointAcceleration :Double = 0.0
    var gpsPointVerticalSpeed :Double = 0.0
    var gpsPointDirection: Double? = null
    var gpsPointAltitude: Double? = null
    var gpsPointLatitude: Double? = null
    var gpsPointLongitude: Double? = null
    var gpsPointGpsAltitudeAccuracy: Double = 299.9
    var gpsPointAltitudeSourceMntMnsOrGps: String = ""
    var gpsPointSpreadAltitudeAndAltitudeGps: Double? = null
    var gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps: Double = 0.0
    var gpsPointDurationAltitudeNotMnt: Int = 0
    var gpsPointIsMoving: Boolean = false
    var gpsPointIsStarted: Boolean = false
    var gpsPointCumulatedGpsPrecision: Double = 0.0
    var gpsPointIsAltitudeGot: Boolean = false
    var gpsPointIsGpsAltitudeGot: Boolean = false
    var gpsPointUncorrectedGpsAltitude: Double? = null
    var gpsPointDisplayedVerticalSpeed: Double = 0.0
    var gpsPointDisplayedAltitude:Double? = null
    var gpsPointDisplayedSpeed:Double = 0.0
    var gpsPointDurationHighSpreadAltitude: Int = 0

    var gpsPointScreenValueDouble1: Double? = 0.0
    var gpsPointScreenValueDouble2: Double? = 0.0
    var gpsPointScreenValueDouble3: Double? = 0.0
    var gpsPointScreenValue: Boolean? = false
    var gpsPointScreenValueString: String? = ""
    var gpsPointScreenValueString2: String? = ""
    var gpsPointScreenValueString3: String? = ""
    var gpsPointScreenValueString4: String? = ""
    var gpsPointScreenValueBoolean: Boolean? = false
    var gpsPointScreenValueLong: Long? = 0
    var gpsPointScreenValueInt: Int? = 0*/

    /*

     */

    //debug:
    var activeAltitudeSrtmRequests: Int = 0
    var activeAltitudeLidarRequests: Int = 0

}