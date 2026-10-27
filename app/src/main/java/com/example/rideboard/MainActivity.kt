package com.example.rideboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.rideboard.altitude.AltitudeManager
import com.example.rideboard.altitude.AltitudeRequestQueue
import com.example.rideboard.altitude.database.AltitudeDatabase
import com.example.rideboard.altitude.database.AltitudeRepository
import com.example.rideboard.altitude.providers.IgnProvider
import com.example.rideboard.altitude.providers.LidarHdProvider
import com.example.rideboard.altitude.providers.SrtmProvider
import com.example.rideboard.brouter.BRouterFiles
import com.example.rideboard.brouter.CustomProfileStore
import com.example.rideboard.config.AppConfig
import com.example.rideboard.service.LocationService
import com.example.rideboard.ui.RideScreen
import com.example.rideboard.utils.CyclingSensorManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val rideViewModel: RideViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val networkGranted = permissions[Manifest.permission.ACCESS_NETWORK_STATE] ?: false
        val internetGranted = permissions[Manifest.permission.INTERNET] ?: false
        if ((fineGranted || coarseGranted) && networkGranted && internetGranted) {
            startLocationService()
        } else {
            Toast.makeText(this, "Permission de localisation, réseau et internet requise pour enregistrer vos sorties.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // <- empêche la mise en veille auto

        //lecture des donnees de HRZ, PowerZone etc...
        AppSettings.load(applicationContext)
        //copie des profils personnalisés
        CustomProfileStore.load(applicationContext)
        //lecture-copie des profiles BRouter
        BRouterFiles.installProfiles(applicationContext)

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
        AppConfig.sensorManager = CyclingSensorManager(applicationContext)

        // --- Vérification des permissions
        checkAndRequestPermissions()

        // --- Interface utilisateur
        setContent {
            RideScreen(rideViewModel = rideViewModel)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        try {
            val serviceIntent = Intent(this, LocationService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
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
