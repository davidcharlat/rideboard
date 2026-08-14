package com.example.rideboard.buffer

import java.util.ArrayDeque

/**
 * Buffer circulaire des échantillons GPS.
 * Stocke les points récents, et supprime les plus anciens.
 * Implémenté avec ArrayDeque pour des performances optimales.
 */
object GpsBuffer {

    private const val MAX_SIZE = 8  // Ajuste selon ta logique
    private val samples = ArrayDeque<GpsSample>()

    /**
     * Ajoute un nouvel échantillon à la fin du buffer.
     * Supprime automatiquement le plus ancien si la taille dépasse MAX_SIZE.
     */
    @Synchronized
    fun add(sample: GpsSample) {
        samples.addLast(sample)
        if (samples.size > MAX_SIZE) {
            samples.removeFirst()
        }
    }

    @Synchronized
    fun removeLast(): GpsSample? {
        return if (samples.isNotEmpty()) samples.removeLast() else null
    }
    /**
     * Supprime le plus ancien échantillon.
     */
    @Synchronized
    fun removeOldest(): GpsSample? {
        return if (samples.isNotEmpty()) samples.removeFirst() else null
    }

    /**
     * Retourne le dernier échantillon (le plus récent).
     */
    @Synchronized
    fun getLast(): GpsSample? = samples.lastOrNull()

    /**
     * Retourne l'avant-dernier échantillon.
     */
    @Synchronized
    fun getNthBeforeLast(n: Int): GpsSample? {
        if (samples.size < n) return null
        return samples.elementAt(samples.size - n )
    }

    /**
     * Retourne une copie (immuable) de tous les échantillons.
     * À éviter en continu pour des raisons de performance.
     */

    @Synchronized
    fun getAll(): List<GpsSample> = samples.toList()

    /**
     * Met à jour l’altitude d’un échantillon précis (via son objet).
     */
    @Synchronized
    fun updateAltitude(provider: String, sample : GpsSample, altitude: Double) {
        when (provider) {
            "L_MNT" -> sample.altitudeLidarHdMnt = altitude
            "SRTM" -> sample.altitudeSrtm = altitude
            "IGN" -> sample.altitudeIgn = altitude
            "L_MNS" -> sample.altitudeLidarHdMns = altitude
        }
    }

    /**
     * Supprime tout le contenu du buffer.
     */
    @Synchronized
    fun clear() {
        samples.clear()
    }

    val size: Int
        @Synchronized get() = samples.size
}
