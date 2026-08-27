
package com.example.rideboard

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

class RideBoardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )

        // Initialisation de la configuration d'OSMDroid
        // Très important pour le bon fonctionnement des cartes
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName

        // Optionnel : définir un dossier de cache spécifique
        val basePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")
    }
}