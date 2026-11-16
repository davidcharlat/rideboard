package com.example.rideboard

import android.content.Context
import java.io.File
import java.util.Locale

object AppSettings {
    var powerZone2 = 120
    var powerZone3 = 230
    var powerZone4 = 380
    var powerZone5 = 500
    var hrZone2 = 112
    var hrZone3 = 138
    var hrZone4 = 155
    var hrZone5 = 163
    var isRinging = false
    @Volatile var envRescanSeconds = 30       // pause entre deux lectures du thermomètre (s)
    @Volatile var tempCorrection = 0f         // correction ajoutée à la température d'ambiance (°C)
    var mapHeadingUp = false          // true : la carte tourne et la flèche pointe vers le haut
    private const val FILE_NAME = "settings.txt"

    fun load(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return

        file.forEachLine { line ->
            val parts = line.split("=")
            if (parts.size != 2) return@forEachLine
            val key = parts[0].trim()
            val raw = parts[1].trim().replace(',', '.')
            val intValue = raw.toIntOrNull()
            val floatValue = raw.toFloatOrNull()

            when (key) {
                "powerZone2" -> intValue?.let { powerZone2 = it }
                "powerZone3" -> intValue?.let { powerZone3 = it }
                "powerZone4" -> intValue?.let { powerZone4 = it }
                "powerZone5" -> intValue?.let { powerZone5 = it }
                "hrZone2" -> intValue?.let { hrZone2 = it }
                "hrZone3" -> intValue?.let { hrZone3 = it }
                "hrZone4" -> intValue?.let { hrZone4 = it }
                "hrZone5" -> intValue?.let { hrZone5 = it }
                "isRinging" -> intValue?.let { isRinging = (it == 1) }
                "envRescanSeconds" -> intValue?.let { envRescanSeconds = it.coerceIn(5, 600) }
                "tempCorrection" -> floatValue?.let { tempCorrection = it.coerceIn(-20f, 20f) }
                "mapHeadingUp" -> intValue?.let { mapHeadingUp = (it == 1) }
            }
        }
    }

    fun save(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        val content = buildString {
            appendLine("powerZone2=$powerZone2")
            appendLine("powerZone3=$powerZone3")
            appendLine("powerZone4=$powerZone4")
            appendLine("powerZone5=$powerZone5")
            appendLine("hrZone2=$hrZone2")
            appendLine("hrZone3=$hrZone3")
            appendLine("hrZone4=$hrZone4")
            appendLine("hrZone5=$hrZone5")
            appendLine("isRinging=${if (isRinging) 1 else 0}")
            appendLine("envRescanSeconds=$envRescanSeconds")
            appendLine("tempCorrection=${String.format(Locale.US, "%.1f", tempCorrection)}")
        }
        file.writeText(content)
    }
}