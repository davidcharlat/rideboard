package com.example.rideboard

import android.content.Context
import java.io.File

object BRouterFiles {
    fun baseDir(context: Context) = File(context.filesDir, "brouter")
    fun profilesDir(context: Context) = File(baseDir(context), "profiles2")
    fun segmentsDir(context: Context) = File(baseDir(context), "segments4")

    /** Copie les profils des assets vers filesDir (sans écraser un fichier existant). */
    fun installProfiles(context: Context) {
        val dir = profilesDir(context).also { it.mkdirs() }
        segmentsDir(context).mkdirs()
        context.assets.list("brouter_profiles")?.forEach { name ->
            val out = File(dir, name)
            if (!out.exists()) {
                context.assets.open("brouter_profiles/$name").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }
}