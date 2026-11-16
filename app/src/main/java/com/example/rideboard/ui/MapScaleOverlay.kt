package com.example.rideboard.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Barre d'échelle : un trait qui commence par "0" et se termine par la distance qu'il représente
 * (par exemple "0 ———— 500 m"). La longueur est recalculée à chaque dessin de la carte, donc elle suit
 * le zoom et la latitude.
 */
class MapScaleOverlay : Overlay() {

    /** Longueur maximale du trait, en fraction de la largeur de la carte (le trait fait entre 40 % et 100 % de cette valeur). */
    var maxFractionOfWidth = 0.5f

    /** Espace à réserver en bas de la carte (barre de capteurs posée dessus), en pixels. */
    var bottomInsetPx = 0f

    /** true : le trait finit en bas à droite ; false : il commence en bas à gauche. */
    var alignRight = true

    private val linePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.SQUARE
    }
    private val haloPaint = Paint().apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.SQUARE
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val textHaloPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.STROKE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val w = mapView.width
        val h = mapView.height
        if (w <= 0 || h <= 0) return

        val d = mapView.resources.displayMetrics.density
        val margin = if (alignRight) 32f * d else 16f * d    // marge plus large à droite : l'étiquette est centrée sur le bout du trait
        val tick = 6f * d
        val y = h - bottomInsetPx - 14f * d                    // ordonnée du trait
        if (y < tick + 20f * d) return

        // Mètres par pixel, mesurés sur 100 px à la hauteur du trait (la latitude change l'échelle)
        val proj = mapView.projection
        val cx = w / 2
        val a = proj.fromPixels(cx - 50, y.toInt())
        val b = proj.fromPixels(cx + 50, y.toInt())
        val meters = GeoPoint(a.latitude, a.longitude).distanceToAsDouble(GeoPoint(b.latitude, b.longitude))
        if (meters <= 0.0) return
        val metersPerPx = meters / 100.0

        // Plus grande distance "ronde" (1, 2 ou 5 × 10^n) dont le trait tient dans la longueur maximale
        val niceMeters = niceDistance(metersPerPx * w * maxFractionOfWidth)
        val barPx = (niceMeters / metersPerPx).toFloat()

        val x0 = if (alignRight) w - margin - barPx else margin
        val x1 = x0 + barPx

        linePaint.strokeWidth = 2.5f * d
        haloPaint.strokeWidth = 5f * d
        textPaint.textSize = 13f * d
        textHaloPaint.textSize = 13f * d
        textHaloPaint.strokeWidth = 3.5f * d

        canvas.save()
        canvas.rotate(-mapView.mapOrientation, w / 2f, h / 2f)   // annule la rotation de la carte : on dessine dans le repère de l'écran
        // Trait et deux repères, avec un halo blanc dessous pour rester lisible sur la carte
        for (paint in listOf(haloPaint, linePaint)) {
            canvas.drawLine(x0, y, x1, y, paint)
            canvas.drawLine(x0, y - tick, x0, y, paint)
            canvas.drawLine(x1, y - tick, x1, y, paint)
        }

        val textY = y - tick - 4f * d
        drawLabel(canvas, "0", x0, textY)
        drawLabel(canvas, formatDistance(niceMeters), x1, textY)
        canvas.restore()
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float) {
        canvas.drawText(text, x, y, textHaloPaint)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun niceDistance(maxMeters: Double): Double {
        if (maxMeters <= 0.0) return 1.0
        val base = 10.0.pow(floor(log10(maxMeters)))
        return listOf(5.0, 2.0, 1.0).map { it * base }.first { it <= maxMeters }
    }

    private fun formatDistance(meters: Double): String = when {
        meters >= 1000.0 -> "${(meters / 1000.0).toInt()} km"
        meters >= 1.0 -> "${meters.toInt()} m"
        else -> "${(meters * 100.0).toInt()} cm"
    }
}