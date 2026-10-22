package com.example.rideboard.brouter

import android.content.Context
import java.io.File
import java.util.Locale

enum class ParamType { BOOL, NUMBER }

data class ProfileParam(
    val key: String,
    val label: String,
    val help: String,
    val group: String,
    val type: ParamType,
    val default: String,
    val min: Float = 0f,
    val max: Float = 1f,
    val step: Float = 0.1f
)

object CustomProfile {
    const val FILE = "custom-bike"

    const val G_GENERAL = "Préférences"
    const val G_ELEVATION = "Dénivelé"
    const val G_ROADS = "Coefficients par type de voie"
    const val G_TIME = "Modèle de durée"

    private const val CF_HELP = "1,0 = neutre. Plus la valeur est grande, plus ce type de voie est évité."

    private fun bool(key: String, label: String, help: String, group: String, default: Boolean) =
        ProfileParam(key, label, help, group, ParamType.BOOL, default.toString())

    private fun num(
        key: String, label: String, help: String, group: String,
        default: String, min: Float, max: Float, step: Float
    ) = ProfileParam(key, label, help, group, ParamType.NUMBER, default, min, max, step)

    val PARAMS = listOf(
        // ---- Préférences ----
        num("consider_traffic", "Éviter le trafic",
            "0 = ignoré, 1 = très important. Évite les routes à fort trafic (confort, pas un gain de temps).",
            G_GENERAL, "0.1", 0f, 1f, 0.1f),
        bool("consider_noise", "Préférer les routes calmes", "Favorise les routes peu bruyantes.", G_GENERAL, false),
        bool("consider_river", "Préférer les bords d'eau", "Favorise les itinéraires le long de l'eau.", G_GENERAL, false),
        bool("consider_forest", "Préférer forêts et parcs", "Favorise les forêts et les parcs.", G_GENERAL, false),
        bool("consider_town", "Contourner les villes", "Contourne les villes quand c'est possible.", G_GENERAL, false),
        bool("allow_steps", "Autoriser les escaliers", "Désactivé : les escaliers sont interdits.", G_GENERAL, true),
        bool("allow_ferries", "Autoriser les bacs", "Désactivé : les bacs sont interdits.", G_GENERAL, true),

        // ---- Dénivelé ----
        bool("consider_elevation", "Tenir compte du dénivelé",
            "Désactivé : les quatre réglages de dénivelé ci-dessous sont ignorés.", G_ELEVATION, true),
        num("uphillcost", "Coût de la montée",
            "Chaque mètre de montée compte pour autant de mètres de distance en plus. " +
                    "Ex. 10 : un détour de 10 m est accepté pour éviter 1 m de montée. 0 = montées ignorées.",
            G_ELEVATION, "0", 0f, 300f, 5f),
        num("uphillcutoff", "Seuil de pente (montée, %)",
            "Les montées de pente inférieure à ce seuil ne sont pas comptées.",
            G_ELEVATION, "1.5", 0f, 10f, 0.5f),
        num("downhillcost", "Coût de la descente",
            "Même principe pour les descentes. Sur un parcours qui revient à son point de départ, " +
                    "montées et descentes sont égales : régler l'un agit à peu près comme régler l'autre.",
            G_ELEVATION, "60", 0f, 300f, 5f),
        num("downhillcutoff", "Seuil de pente (descente, %)",
            "Les descentes de pente inférieure à ce seuil ne sont pas comptées.",
            G_ELEVATION, "1.5", 0f, 10f, 0.5f),

        // ---- Coefficients par type de voie ----
        num("cf_primary", "Routes primaires", CF_HELP, G_ROADS, "1.2", 1f, 30f, 0.1f),
        num("cf_secondary", "Routes secondaires", CF_HELP, G_ROADS, "1.1", 1f, 30f, 0.1f),
        num("cf_tertiary", "Routes tertiaires", CF_HELP, G_ROADS, "1.0", 1f, 30f, 0.1f),
        num("cf_unclassified", "Routes non classées (goudron)", CF_HELP, G_ROADS, "1.1", 1f, 30f, 0.1f),
        num("cf_residential", "Rues résidentielles (goudron)", CF_HELP, G_ROADS, "1.2", 1f, 30f, 0.1f),
        num("cf_service", "Voies de service (goudron)", CF_HELP, G_ROADS, "1.2", 1f, 30f, 0.1f),
        num("cf_cycleway", "Pistes cyclables", CF_HELP, G_ROADS, "1.3", 1f, 30f, 0.1f),
        num("cf_unpaved", "Non goudronné (routes classées, résidentielles, service)", CF_HELP, G_ROADS, "10", 1f, 50f, 0.5f),
        num("cf_track_paved", "Chemins goudronnés", CF_HELP, G_ROADS, "2.0", 1f, 30f, 0.1f),
        num("cf_track_unpaved", "Chemins non goudronnés", CF_HELP, G_ROADS, "15.0", 1f, 50f, 0.5f),

        // ---- Modèle de durée ----
        num("bikerPower", "Puissance moyenne (W)",
            "Puissance moyenne du cycliste. Sert au calcul de la durée affichée.",
            G_TIME, "100", 20f, 400f, 5f),
        num("totalMass", "Masse vélo + cycliste (kg)",
            "Sert au calcul de la durée affichée.",
            G_TIME, "90", 40f, 200f, 1f),
        num("maxSpeed", "Vitesse maximale (km/h)",
            "Vitesse maximale du modèle de durée.",
            G_TIME, "45", 15f, 80f, 1f)
    )

    /** Texte stocké pour une valeur numérique (toujours avec un point décimal). */
    fun formatValue(p: ProfileParam, v: Float): String =
        if (p.step >= 1f) Math.round(v).toString() else String.format(Locale.US, "%.1f", v)
}

object CustomProfileStore {
    private val values = mutableMapOf<String, String>()
    private fun file(context: Context) = File(BRouterFiles.baseDir(context), "custom_profile.txt")

    fun load(context: Context) {
        values.clear()
        val f = file(context)
        if (!f.exists()) return
        f.forEachLine { line ->
            val p = line.split("=")
            if (p.size == 2) values[p[0].trim()] = p[1].trim().replace(',', '.')
        }
    }

    /** Valeurs courantes ; la valeur par défaut est utilisée pour tout réglage jamais enregistré. */
    fun current(): Map<String, String> =
        CustomProfile.PARAMS.associate { it.key to (values[it.key] ?: it.default) }

    fun save(context: Context, newValues: Map<String, String>) {
        values.clear()
        values.putAll(newValues)
        val f = file(context)
        f.parentFile?.mkdirs()
        f.writeText(newValues.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
}