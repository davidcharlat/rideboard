package com.example.rideboard

import android.content.Context
import java.io.File

object AppSettings {
    var powerZone2 = 120
    var powerZone3 = 230
    var powerZone4 = 380
    var powerZone5 = 500
    var hrZone2 = 112
    var hrZone3 = 138
    var hrZone4 = 155
    var hrZone5 = 163

    private const val FILE_NAME = "settings.txt"

    fun load(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return // garde les valeurs par défaut ci-dessus si le fichier n'existe pas encore

        file.forEachLine { line ->
            val parts = line.split("=")
            if (parts.size != 2) return@forEachLine
            val key = parts[0].trim()
            val value = parts[1].trim().toIntOrNull() ?: return@forEachLine

            when (key) {
                "powerZone2" -> powerZone2 = value
                "powerZone3" -> powerZone3 = value
                "powerZone4" -> powerZone4 = value
                "powerZone5" -> powerZone5 = value
                "hrZone2" -> hrZone2 = value
                "hrZone3" -> hrZone3 = value
                "hrZone4" -> hrZone4 = value
                "hrZone5" -> hrZone5 = value
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
        }
        file.writeText(content)
    }
}