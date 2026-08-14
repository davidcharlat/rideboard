package com.example.rideboard.utils

import kotlin.math.*
import java.util.Locale

/**
 * Arrondit à l'entier multiple du 'stepieme' le plus proche. (si step = 20, arrondit à 0.05 près)
 */
fun roundToNearestStep(value: Double, step: Int): Double {
    val quant = round(value * step) / step
    return fixPrecisionToStep(quant, step)
}

/**
 * Limite les artefacts de flottants (ex: 0.00019999997).
 */
private fun fixPrecisionToStep(value: Double, step: Int): Double {
    if (step <= 0.0) return value
    val stepInverse = 1.0 / step
    val decimals = max(0, ceil(-log10(stepInverse)).toInt())
    val s = String.format(Locale.US, "%.${decimals}f", value)
    return s.toDouble()
}