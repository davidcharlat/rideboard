package com.example.rideboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.rideboard.altitude.AltitudeManager
import com.example.rideboard.altitude.AltitudeRequestQueue
import com.example.rideboard.altitude.database.AltitudeDatabase
import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.altitude.providers.IgnProvider
import com.example.rideboard.altitude.providers.LidarHdProvider
import com.example.rideboard.altitude.providers.SrtmProvider
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.config.AppConfig
import com.example.rideboard.service.LocationService
import com.example.rideboard.ui.RideScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val rideViewModel: RideViewModel by viewModels()

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET
    ])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Base de données
        val db = Room.databaseBuilder(
            applicationContext,
            AltitudeDatabase::class.java,
            "altitude_db"
        ).build()

        val repository = AltitudeRepository(db.altitudeDao())
        val providers = mapOf(
            "SRTM" to SrtmProvider(repository),
            "IGN" to IgnProvider(repository),
            "L_MNT" to LidarHdProvider(repository, "MNT"),
            "L_MNS" to LidarHdProvider(repository, "MNS")
        )
        val altitudeManager = AppConfig.altitudeManager ?: AltitudeManager(repository, providers).also {
            AppConfig.altitudeManager = it
        }

        // Stocker dans AppConfig pour tout le projet
        AppConfig.repository = repository
        AppConfig.providers = providers
        AppConfig.altitudeManager = altitudeManager

        // --- Démarrage du service de localisation
        val serviceIntent = Intent(this, LocationService::class.java)
        startForegroundService(serviceIntent)

        // --- Interface utilisateur
        setContent {
            RideScreen(rideViewModel = rideViewModel)
        }
    }

    override fun onPause() {
        super.onPause()

        // Traitement des requêtes d’altitude en attente
        lifecycleScope.launch {
            try {
                AltitudeRequestQueue.processAll(AppConfig.repository, AppConfig.providers)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
