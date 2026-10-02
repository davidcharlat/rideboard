package com.example.rideboard.ui

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rideboard.RideViewModel
import com.example.rideboard.buffer.GpsBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun SetPointsScreen(
    rideViewModel: RideViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trackPoints by rideViewModel.trackPoints

    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedStartIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEndIndex by remember { mutableStateOf<Int?>(null) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    if (showConfirmDialog && selectedStartIndex != null && selectedEndIndex != null) {
        val start = minOf(selectedStartIndex!!, selectedEndIndex!!)
        val end = maxOf(selectedStartIndex!!, selectedEndIndex!!)
        val pointsCount = end - start + 1

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmer la suppression") },
            text = {
                Text("Voulez-vous supprimer $pointsCount point(s) de la trace (du point #${start + 1} au point #${end + 1}) ?\n\nLes données de distance, D+ et temps pour la suite de la sortie seront automatiquement recalculées.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isProcessing = true
                        coroutineScope.launch {
                            deletePointsAndRecalculate(
                                context = context,
                                startIndex = start,
                                endIndex = end,
                                rideViewModel = rideViewModel
                            )
                            isProcessing = false
                            selectedStartIndex = null
                            selectedEndIndex = null
                            Toast.makeText(context, "Points supprimés avec succès", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isDeleteMode) {
            // Écran principal des options de SetPoints
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gestion des points de trace",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Fonctionnalité d'ajout de points à venir", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Ajouter des points", color = Color.White, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isDeleteMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Supprimer des points", color = Color.White, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Retour", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        } else {
            // Mode suppression de points
            Box(modifier = Modifier.fillMaxSize()) {
                val mapView = remember {
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.OpenTopo)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        if (trackPoints.isNotEmpty()) {
                            val lastPoint = trackPoints.last()
                            controller.setCenter(GeoPoint(lastPoint.first, lastPoint.second))
                        }
                    }
                }

                val mainPolyline = remember {
                    Polyline().apply {
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 8f
                    }
                }

                val selectedPolyline = remember {
                    Polyline().apply {
                        outlinePaint.color = android.graphics.Color.RED
                        outlinePaint.strokeWidth = 14f
                    }
                }

                val startMarker = remember {
                    Marker(mapView).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Début"
                    }
                }

                val endMarker = remember {
                    Marker(mapView).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Fin"
                    }
                }

                // Gestion unique du cycle de vie de la MapView
                DisposableEffect(Unit) {
                    mapView.onResume()
                    onDispose {
                        mapView.onPause()
                        mapView.onDetach()
                    }
                }

                // Interaction au tap sur la carte (sans onDetach dans onDispose)
                DisposableEffect(mapView) {
                    val eventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p == null || trackPoints.isEmpty()) return false
                            val closestIdx = findClosestPointIndex(p, trackPoints)
                            if (closestIdx != null) {
                                if (selectedStartIndex == null) {
                                    selectedStartIndex = closestIdx
                                    selectedEndIndex = null
                                } else if (selectedEndIndex == null) {
                                    if (closestIdx < selectedStartIndex!!) {
                                        selectedEndIndex = selectedStartIndex
                                        selectedStartIndex = closestIdx
                                    } else {
                                        selectedEndIndex = closestIdx
                                    }
                                } else {
                                    // Nouveau choix
                                    selectedStartIndex = closestIdx
                                    selectedEndIndex = null
                                }
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }

                    val overlay = MapEventsOverlay(eventsReceiver)
                    mapView.overlays.add(overlay)

                    onDispose {
                        mapView.overlays.remove(overlay)
                    }
                }

                // Mise à jour du tracé et des sélections sur la carte
                LaunchedEffect(trackPoints, selectedStartIndex, selectedEndIndex) {
                    val geoPoints = trackPoints.map { GeoPoint(it.first, it.second) }
                    mainPolyline.safeSetPoints(geoPoints)

                    if (!mapView.overlays.contains(mainPolyline)) {
                        mapView.overlays.add(0, mainPolyline)
                    }

                    // Tracé sélectionné
                    if (selectedStartIndex != null && selectedEndIndex != null) {
                        val start = minOf(selectedStartIndex!!, selectedEndIndex!!)
                        val end = maxOf(selectedStartIndex!!, selectedEndIndex!!)
                        if (start in trackPoints.indices && end in trackPoints.indices && start <= end) {
                            val segPoints = trackPoints.subList(start, end + 1).map { GeoPoint(it.first, it.second) }
                            selectedPolyline.safeSetPoints(segPoints)

                            if (!mapView.overlays.contains(selectedPolyline)) {
                                mapView.overlays.add(selectedPolyline)
                            }
                        }
                    } else {
                        mapView.overlays.remove(selectedPolyline)
                    }

                    // Marqueur début
                    if (selectedStartIndex != null && selectedStartIndex!! in trackPoints.indices) {
                        val pt = trackPoints[selectedStartIndex!!]
                        startMarker.position = GeoPoint(pt.first, pt.second)
                        startMarker.title = "Début (#${selectedStartIndex!! + 1})"
                        if (!mapView.overlays.contains(startMarker)) {
                            mapView.overlays.add(startMarker)
                        }
                    } else {
                        mapView.overlays.remove(startMarker)
                    }

                    // Marqueur fin
                    if (selectedEndIndex != null && selectedEndIndex!! in trackPoints.indices) {
                        val pt = trackPoints[selectedEndIndex!!]
                        endMarker.position = GeoPoint(pt.first, pt.second)
                        endMarker.title = "Fin (#${selectedEndIndex!! + 1})"
                        if (!mapView.overlays.contains(endMarker)) {
                            mapView.overlays.add(endMarker)
                        }
                    } else {
                        mapView.overlays.remove(endMarker)
                    }

                    mapView.invalidate()
                }

                // 1. La Carte prend tout l'écran en arrière-plan
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Bandeau supérieur d'instructions flottant en premier plan
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val instructionText = when {
                            trackPoints.isEmpty() -> "Aucun point dans la trace."
                            selectedStartIndex == null -> "Tapez sur la carte pour choisir le PREMIER point (début de la zone à supprimer)."
                            selectedEndIndex == null -> "Premier point fixé (#${selectedStartIndex!! + 1}). Tapez sur la carte pour choisir le SECOND point."
                            else -> {
                                val start = minOf(selectedStartIndex!!, selectedEndIndex!!)
                                val end = maxOf(selectedStartIndex!!, selectedEndIndex!!)
                                "Tronçon sélectionné : point #${start + 1} à #${end + 1} (${end - start + 1} points)"
                            }
                        }
                        Text(
                            text = instructionText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3. Boutons de Zoom flottants sur le côté droit
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 12.dp)
                ) {
                    IconButton(
                        onClick = { mapView.controller.zoomIn() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom +", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(
                        onClick = { mapView.controller.zoomOut() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = Color.White)
                    }
                }

                // 4. Barre d'actions inférieure flottante
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth(0.95f)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                selectedStartIndex = null
                                selectedEndIndex = null
                            },
                            enabled = selectedStartIndex != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Réinit.")
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = selectedStartIndex != null && selectedEndIndex != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Supprimer", color = Color.White)
                        }

                        Button(
                            onClick = {
                                isDeleteMode = false
                                selectedStartIndex = null
                                selectedEndIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Retour")
                        }
                    }
                }
            }
        }

        // Overlay de traitement en cours (placé au dessus pour ne pas démonter la MapView)
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Green)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Calculs et mise à jour du fichier ride.tsv en cours...\nVeuillez patienter.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Helper extension pour appeler setPoints de manière sécurisée en évitant les crashs osmdroid.
 */
private fun Polyline.safeSetPoints(points: List<GeoPoint>) {
    try {
        setPoints(points)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Recherche de l'index du point le plus proche sur la carte en distance euclidienne lat/lon.
 */
private fun findClosestPointIndex(tapGeoPoint: GeoPoint, points: List<Pair<Double, Double>>): Int? {
    if (points.isEmpty()) return null
    var minDistanceSq = Double.MAX_VALUE
    var minIndex = -1
    for (i in points.indices) {
        val pt = points[i]
        val dLat = pt.first - tapGeoPoint.latitude
        val dLon = pt.second - tapGeoPoint.longitude
        val distSq = dLat * dLat + dLon * dLon
        if (distSq < minDistanceSq) {
            minDistanceSq = distSq
            minIndex = i
        }
    }
    return if (minIndex != -1) minIndex else null
}

/**
 * Supprime les points entre startIndex et endIndex (inclus) de ride.tsv,
 * et recalcule les données cumulées (D+, temps, distance) pour les lignes suivantes.
 */
private suspend fun deletePointsAndRecalculate(
    context: Context,
    startIndex: Int,
    endIndex: Int,
    rideViewModel: RideViewModel
) = withContext(Dispatchers.IO) {
    val rideFile = File(context.filesDir, "ride.tsv")
    if (!rideFile.exists()) return@withContext

    val rawLines = rideFile.readLines()
    val lines = rawLines.filter { it.isNotBlank() }
    if (lines.isEmpty()) return@withContext

    val validStartIndex = startIndex.coerceIn(0, lines.size - 1)
    val validEndIndex = endIndex.coerceIn(validStartIndex, lines.size - 1)

    val tempFile = File(context.filesDir, "ride_temp.tsv")
    if (tempFile.exists()) tempFile.delete()

    val linesCountBefore = validStartIndex
    val linesCountAfter = lines.size - (validEndIndex + 1)

    // Cas 1: Suppression de TOUS les points
    if (linesCountBefore == 0 && linesCountAfter == 0) {
        rideFile.writeText("")
        GpsBuffer.clear()
        withContext(Dispatchers.Main) {
            rideViewModel.clearTrack()
        }
        return@withContext
    }

    val writer = tempFile.bufferedWriter()

    // Copier les lignes précédant la zone supprimée
    for (i in 0 until validStartIndex) {
        writer.write(lines[i])
        writer.newLine()
    }

    if (linesCountAfter > 0) {
        var deltaDistance = 0.0
        var deltaDenivele = 0.0
        var deltaTime = 0.0

        if (linesCountBefore > 0) {
            // Tronçon supprimé au milieu ou à la fin
            val lastCopiedTokens = lines[validStartIndex - 1].split("\t")
            val nextTokens = lines[validEndIndex + 1].split("\t")

            val lastLat = lastCopiedTokens.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val lastLon = lastCopiedTokens.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            val lastAlt = lastCopiedTokens.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val lastDPlus = lastCopiedTokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
            val lastTimeSec = lastCopiedTokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
            val lastSpeed = lastCopiedTokens.getOrNull(6)?.toDoubleOrNull() ?: 0.0
            val lastTotalDist = lastCopiedTokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0

            val nextLat = nextTokens.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val nextLon = nextTokens.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            val nextAlt = nextTokens.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val nextDPlus = nextTokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
            val nextTimeSec = nextTokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
            val nextSpeed = nextTokens.getOrNull(6)?.toDoubleOrNull() ?: 0.0
            val nextTotalDist = nextTokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0

            // Distance euclidienne/géodésique entre le dernier point conservé et le premier point suivant
            val results = FloatArray(1)
            Location.distanceBetween(lastLat, lastLon, nextLat, nextLon, results)
            val distBetween = results[0].toDouble()

            // deltaDistance = distance ligne regardée - distance dernière ligne copiée - distance entre les deux
            deltaDistance = nextTotalDist - lastTotalDist - distBetween

            // deltaDenivele = D+ ligne regardée - D+ dernière ligne copiée - max(0, alt ligne regardée - alt dernière ligne)
            val altDiff = maxOf(0.0, nextAlt - lastAlt)
            deltaDenivele = nextDPlus - lastDPlus - altDiff

            // deltaTime = temps ligne regardée - temps dernière ligne - (distBetween / moyenne des vitesses)
            val avgSpeed = (nextSpeed + lastSpeed) / 2.0
            val timeBetween = if (avgSpeed > 0) distBetween / avgSpeed else 0.0
            val timeDiff = (nextTimeSec - lastTimeSec) - timeBetween
            deltaTime = if (timeDiff < 0.0) 0.0 else timeDiff
        } else {
            // Suppression depuis le début (linesCountBefore == 0)
            val nextTokens = lines[validEndIndex + 1].split("\t")
            deltaDistance = nextTokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0
            deltaDenivele = nextTokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
            deltaTime = nextTokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
        }

        // Recopier et recalculer chaque ligne qui vient après le tronçon supprimé
        for (i in (validEndIndex + 1) until lines.size) {
            val tokens = lines[i].split("\t").toMutableList()
            if (tokens.size >= 8) {
                val currentDPlus = tokens[4].toDoubleOrNull() ?: 0.0
                val currentTimeSec = tokens[5].toDoubleOrNull() ?: 0.0
                val currentDist = tokens[7].toDoubleOrNull() ?: 0.0

                val newDPlus = maxOf(0.0, currentDPlus - deltaDenivele)
                val newTimeSec = maxOf(0.0, currentTimeSec - deltaTime)
                val newDist = maxOf(0.0, currentDist - deltaDistance)

                tokens[4] = newDPlus.toString()
                tokens[5] = newTimeSec.toString()
                tokens[7] = newDist.toString()
            }
            writer.write(tokens.joinToString("\t"))
            writer.newLine()
        }
    }

    writer.flush()
    writer.close()

    // Remplacer ride.tsv par le nouveau fichier
    tempFile.copyTo(rideFile, overwrite = true)
    tempFile.delete()

    // Mise à jour de GpsBuffer avec les nouvelles valeurs de fin de fichier
    val updatedLines = rideFile.readLines().filter { it.isNotBlank() }
    if (updatedLines.isNotEmpty()) {
        val lastTokens = updatedLines.last().split("\t")
        if (lastTokens.size >= 8) {
            val newLastDPlus = lastTokens[4].toDoubleOrNull() ?: 0.0
            val newLastDurationSec = lastTokens[5].toDoubleOrNull() ?: 0.0
            val newLastDist = lastTokens[7].toDoubleOrNull() ?: 0.0

            val lastSample = GpsBuffer.getLast()
            if (lastSample != null) {
                lastSample.gpsPointElevationGain = newLastDPlus
                lastSample.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                lastSample.gpsPointTotalDistance = newLastDist
            }
        }
    } else {
        GpsBuffer.clear()
    }

    // Recharger la trace dans le ViewModel
    withContext(Dispatchers.Main) {
        rideViewModel.loadTrackFromTsv()
    }
}
