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
    //var isToggleBlocked: Boolean = false
    var isLidarHdAvailable: Boolean = true
    var isIgnAvailable: Boolean = true
    var isRecording by mutableStateOf(true)
    var mapZoom: Double = 14.9
    lateinit var repository: AltitudeRepository
    lateinit var providers: Map<String, AltitudeProvider>
    var altitudeManager: AltitudeManager? = null
    var sensorManager: com.example.rideboard.utils.CyclingSensorManager? = null
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    val gpsUpdatesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)


    //debug:
    //var activeAltitudeSrtmRequests: Int = 0
    //var activeAltitudeLidarRequests: Int = 0

}