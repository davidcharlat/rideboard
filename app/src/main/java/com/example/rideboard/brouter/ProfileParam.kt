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
    const val G_SURFACES = "Chemins non goudronnés"

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
        bool("allow_ferries", "Autoriser les bacs", "Désactivé : les bacs sont interdits.", G_GENERAL, false),

        // ---- Dénivelé ----
        bool("consider_elevation", "Tenir compte du dénivelé",
            "Désactivé : les quatre réglages de dénivelé ci-dessous sont ignorés.", G_ELEVATION, true),
        num("uphillcost", "Coût de la montée",
            "Chaque mètre de montée compte pour autant de mètres de distance en plus. " +
                    "Ex. 10 : un détour de 10 m est accepté pour éviter 1 m de montée. 0 = montées ignorées.",
            G_ELEVATION, "15", 0f, 300f, 5f),
        num("uphillcutoff", "Seuil de pente (montée, %)",
            "Les montées de pente inférieure à ce seuil ne sont pas comptées.",
            G_ELEVATION, "0.0", 0f, 10f, 0.5f),
        num("downhillcost", "Coût de la descente",
            "Même principe pour les descentes. Sur un parcours qui revient à son point de départ, " +
                    "montées et descentes sont égales : régler l'un agit à peu près comme régler l'autre.",
            G_ELEVATION, "10", 0f, 300f, 5f),
        num("downhillcutoff", "Seuil de pente (descente, %)",
            "Les descentes de pente inférieure à ce seuil ne sont pas comptées.",
            G_ELEVATION, "8.5", 0f, 10f, 0.5f),

        // ---- Coefficients par type de voie ----
        num("cf_primary", "Routes primaires", CF_HELP, G_ROADS, "1.1", 1f, 30f, 0.1f),
        num("cf_secondary", "Routes secondaires", CF_HELP, G_ROADS, "1.0", 1f, 30f, 0.1f),
        num("cf_tertiary", "Routes tertiaires", CF_HELP, G_ROADS, "1.0", 1f, 30f, 0.1f),
        num("cf_cycleway", "Pistes cyclables", CF_HELP, G_ROADS, "1.1", 1f, 30f, 0.1f),
        num("cf_road_best", "Routes non classées : asphalte ou grade1",
            "unclassified ou road avec surface asphalt, ou tracktype grade1 sans tag de surface. 1,0 = neutre.",
            G_ROADS, "1.1", 1f, 30f, 0.1f),
        num("cf_road_hard", "Revêtement dur (réel ou supposé)",
            "Routes non classées sans information, paved, béton, grade2, état good ; autres voies et chemins à revêtement dur ; voies vélo sans information.",
            G_ROADS, "1.2", 1f, 30f, 0.1f),

        // ---- Chemins non goudronnés ----
        bool("assume_wet_conditions", "Terrain particulier",
            "Activé : les coefficients « particulier » remplacent les coefficients « normal ».", G_SURFACES, false),
        num("us_soft_dry", "Terre, gravier fin — normal",
            "Surface fine_gravel, compacted, gravel, dirt, ground, earth (si l'état n'est pas mauvais).",
            G_SURFACES, "1.6", 1f, 20f, 0.1f),
        num("us_soft_wet", "Terre, gravier fin — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "1.3", 1f, 20f, 0.1f),
        num("us_ok_dry", "Chemin en bon état — normal",
            "État good, excellent ou intermediate, tracktype grade1 ou grade2.", G_SURFACES, "1.8", 1f, 20f, 0.1f),
        num("us_ok_wet", "Chemin en bon état — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "1.5", 1f, 20f, 0.1f),
        num("us_rough_dry", "Chemin irrégulier — normal",
            "État bad, tracktype grade3, surfaces cobblestone, pebblestone, unpaved.", G_SURFACES, "2.3", 1f, 20f, 0.1f),
        num("us_rough_wet", "Chemin irrégulier — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "1.7", 1f, 20f, 0.1f),
        num("us_hard_dry", "Chemin difficile — normal",
            "État very_bad, tracktype grade4 ou grade5, surfaces sand, mud, clay, grass.", G_SURFACES, "4.0", 1f, 20f, 0.1f),
        num("us_hard_wet", "Chemin difficile — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "2.3", 1f, 20f, 0.1f),
        num("us_worst_dry", "Chemin quasi impraticable — normal",
            "État horrible, very_horrible ou impassable.", G_SURFACES, "8.0", 1f, 20f, 0.1f),
        num("us_worst_wet", "Chemin quasi impraticable — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "4.5", 1f, 20f, 0.1f),
        num("us_unknown_dry", "Chemin sans information — normal",
            "track, path ou footway sans tag de surface, d'état ni de classe.", G_SURFACES, "2.6", 1f, 20f, 0.1f),
        num("us_unknown_wet", "Chemin sans information — condition particulière", "Mêmes chemins sous conditions particulières.", G_SURFACES, "1.8", 1f, 20f, 0.1f),

        // ---- Modèle de durée ----
        num("bikerPower", "Puissance moyenne (W)",
            "Puissance moyenne du cycliste. Sert au calcul de la durée affichée.",
            G_TIME, "125", 20f, 400f, 5f),
        num("totalMass", "Masse vélo + cycliste (kg)",
            "Sert au calcul de la durée affichée.",
            G_TIME, "75", 40f, 200f, 1f),
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