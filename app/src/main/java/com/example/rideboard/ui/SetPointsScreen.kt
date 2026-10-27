package com.example.rideboard.ui

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rideboard.RideViewModel
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
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
import java.util.Locale

private enum class ScreenMode {
    MAIN, DELETE, ADD, PLAN
}

private enum class AddStep {
    SELECT_PIVOT, ADDING_POINTS
}

private data class AddedPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speedKmh: Double
)

private data class RideLineData(
    val lineIndex: Int,
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val dPlus: Double,
    val durationSec: Double,
    val speed: Double,
    val totalDist: Double,
    val rawTokens: List<String>
)

@Composable
fun SetPointsScreen(
    rideViewModel: RideViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trackPoints by rideViewModel.trackPoints

    val isRecording = AppConfig.isRecording
    var screenMode by remember { mutableStateOf(if (isRecording) ScreenMode.PLAN else ScreenMode.MAIN) }

    // --- Variables pour le mode Suppression ---
    var deleteStartIndex by remember { mutableStateOf<Int?>(null) }
    var deleteEndIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // --- Variables pour le mode Ajout ---
    var addStep by remember { mutableStateOf(AddStep.SELECT_PIVOT) }
    var pivotIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingBefore by remember { mutableStateOf(true) }
    var showDirectionDialog by remember { mutableStateOf(false) }

    val addedPoints = remember { mutableStateListOf<AddedPoint>() }
    var rideLinesData by remember { mutableStateOf<List<RideLineData>>(emptyList()) }
    var currentLocationTarget by remember { mutableStateOf<RideLineData?>(null) }

    var pendingTapGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showSpeedPromptDialog by remember { mutableStateOf(false) }
    var proposedSpeedText by remember { mutableStateOf("15.0") }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var showAddConfirmDialog by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }

    // Charger les données détaillées du TSV lorsque l'on rentre en mode Ajout/suppression
    LaunchedEffect(screenMode) {
        if (screenMode == ScreenMode.ADD || screenMode == ScreenMode.DELETE) {
            withContext(Dispatchers.IO) {
                rideLinesData = loadRideLinesData(context)
            }
        }
    }

    // --- Dialogue de confirmation pour la Suppression ---
    if (showDeleteConfirmDialog && deleteStartIndex != null && deleteEndIndex != null) {
        val start = minOf(deleteStartIndex!!, deleteEndIndex!!)
        val end = maxOf(deleteStartIndex!!, deleteEndIndex!!)
        val pointsCount = end - start + 1

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmer la suppression") },
            text = {
                Text("Voulez-vous supprimer $pointsCount point(s) de la trace (du point #${start + 1} au point #${end + 1}) ?\n\nLes données de distance, D+ et temps pour la suite de la sortie seront automatiquement recalculées.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isProcessing = true
                        coroutineScope.launch {
                            deletePointsAndRecalculate(
                                context = context,
                                startIndex = start,
                                endIndex = end,
                                rideViewModel = rideViewModel
                            )
                            isProcessing = false
                            deleteStartIndex = null
                            deleteEndIndex = null
                            Toast.makeText(context, "Points supprimés avec succès", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // --- Dialogue de choix de direction (AVANT / APRÈS) translucide ---
    if (showDirectionDialog && pivotIndex != null) {
        val idx = pivotIndex!!
        val isBoundary = (idx == 0 || idx == trackPoints.size - 1)

        AlertDialog(
            onDismissRequest = {
                showDirectionDialog = false
                pivotIndex = null
            },
            containerColor = Color(0xFF222222).copy(alpha = 0.88f),
            title = {
                Text("Point d'ancrage #${idx + 1}", color = Color.White, fontSize = 18.sp)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isBoundary) {
                        if (idx == 0) {
                            Text(
                                "Premier point de la trace sélectionné.\nLes nouveaux points seront ajoutés AVANT le début de la sortie.",
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                "Dernier point de la trace sélectionné.\nLes nouveaux points seront ajoutés APRÈS la fin de la sortie jusqu'à votre position actuelle.",
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            "Où voulez-vous ajouter de nouveaux points par rapport à ce point ?",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isBoundary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isAddingBefore = true
                                    showDirectionDialog = false
                                    addStep = AddStep.ADDING_POINTS
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                            ) {
                                Text("AVANT", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isAddingBefore = false
                                    showDirectionDialog = false
                                    if (idx == rideLinesData.size - 1) {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            val curLat = GpsBuffer.getLast()?.latitude
                                                ?: rideViewModel.screenValues.value.latitude
                                                ?: rideLinesData.last().lat
                                            val curLon = GpsBuffer.getLast()?.longitude
                                                ?: rideViewModel.screenValues.value.longitude
                                                ?: rideLinesData.last().lon
                                            val curTs = System.currentTimeMillis()
                                            val curAlt = fetchAltitudeForPoint(curLat, curLon, curTs, rideLinesData.last().alt)
                                                ?: rideLinesData.last().alt

                                            currentLocationTarget = RideLineData(
                                                lineIndex = rideLinesData.size,
                                                timestamp = curTs,
                                                lat = curLat,
                                                lon = curLon,
                                                alt = curAlt,
                                                dPlus = 0.0,
                                                durationSec = 0.0,
                                                speed = 0.0,
                                                totalDist = 0.0,
                                                rawTokens = emptyList()
                                            )
                                            isProcessing = false
                                            addStep = AddStep.ADDING_POINTS
                                        }
                                    } else {
                                        addStep = AddStep.ADDING_POINTS
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                            ) {
                                Text("APRÈS", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val addingBeforeChoice = (idx == 0)
                                isAddingBefore = addingBeforeChoice
                                showDirectionDialog = false

                                if (!addingBeforeChoice && idx == rideLinesData.size - 1) {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        val curLat = GpsBuffer.getLast()?.latitude
                                            ?: rideViewModel.screenValues.value.latitude
                                            ?: rideLinesData.last().lat
                                        val curLon = GpsBuffer.getLast()?.longitude
                                            ?: rideViewModel.screenValues.value.longitude
                                            ?: rideLinesData.last().lon
                                        val curTs = System.currentTimeMillis()
                                        val curAlt = fetchAltitudeForPoint(curLat, curLon, curTs, rideLinesData.last().alt)
                                            ?: rideLinesData.last().alt

                                        currentLocationTarget = RideLineData(
                                            lineIndex = rideLinesData.size,
                                            timestamp = curTs,
                                            lat = curLat,
                                            lon = curLon,
                                            alt = curAlt,
                                            dPlus = 0.0,
                                            durationSec = 0.0,
                                            speed = 0.0,
                                            totalDist = 0.0,
                                            rawTokens = emptyList()
                                        )
                                        isProcessing = false
                                        addStep = AddStep.ADDING_POINTS
                                    }
                                } else {
                                    addStep = AddStep.ADDING_POINTS
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (idx == 0) Color(0xFF1976D2) else Color(0xFF388E3C))
                        ) {
                            Text(if (idx == 0) "Confirmer (AVANT)" else "Confirmer (APRÈS)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            showDirectionDialog = false
                            pivotIndex = null
                        },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Annuler / Changer", color = Color.White)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // --- Dialogue translucide de saisie de la vitesse moyenne souhaitée ---
    if (showSpeedPromptDialog && pendingTapGeoPoint != null && pivotIndex != null) {
        val tapGeo = pendingTapGeoPoint!!

        val refPoint = if (addedPoints.isNotEmpty()) {
            Pair(addedPoints.last().latitude, addedPoints.last().longitude)
        } else if (pivotIndex!! in rideLinesData.indices) {
            Pair(rideLinesData[pivotIndex!!].lat, rideLinesData[pivotIndex!!].lon)
        } else null

        val distToRef = if (refPoint != null) {
            val res = FloatArray(1)
            Location.distanceBetween(refPoint.first, refPoint.second, tapGeo.latitude, tapGeo.longitude, res)
            res[0].toDouble()
        } else 0.0

        AlertDialog(
            onDismissRequest = { showSpeedPromptDialog = false },
            containerColor = Color(0xFF222222).copy(alpha = 0.88f),
            title = { Text("Nouveau point (${if (isAddingBefore) "AVANT" else "APRÈS"})", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Distance du point de référence : ${String.format(Locale.US, "%.1f", distToRef)} m",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Vitesse moyenne souhaitée (km/h) :", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val cur = proposedSpeedText.toDoubleOrNull() ?: 15.0
                                if (cur > 1.0) {
                                    proposedSpeedText = String.format(Locale.US, "%.1f", cur - 1.0)
                                }
                            },
                            modifier = Modifier.background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1 km/h", tint = Color.White)
                        }

                        OutlinedTextField(
                            value = proposedSpeedText,
                            onValueChange = { proposedSpeedText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                val cur = proposedSpeedText.toDoubleOrNull() ?: 15.0
                                proposedSpeedText = String.format(Locale.US, "%.1f", cur + 1.0)
                            },
                            modifier = Modifier.background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1 km/h", tint = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val enteredSpeedKmh = proposedSpeedText.toDoubleOrNull()
                        if (enteredSpeedKmh == null || enteredSpeedKmh <= 0.0) {
                            Toast.makeText(context, "Veuillez entrer une vitesse valide > 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        showSpeedPromptDialog = false

                        coroutineScope.launch {
                            val refTimestamp = if (addedPoints.isNotEmpty()) {
                                addedPoints.last().timestamp
                            } else if (pivotIndex!! in rideLinesData.indices) {
                                rideLinesData[pivotIndex!!].timestamp
                            } else System.currentTimeMillis()

                            val refAlt = if (addedPoints.isNotEmpty()) {
                                addedPoints.last().altitude
                            } else if (pivotIndex!! in rideLinesData.indices) {
                                rideLinesData[pivotIndex!!].alt
                            } else null

                            val speedMs = enteredSpeedKmh / 3.6
                            val deltaSec = if (speedMs > 0) distToRef / speedMs else 0.0
                            val deltaMs = (deltaSec * 1000.0).toLong()

                            val newTimestamp = if (isAddingBefore) {
                                refTimestamp - deltaMs
                            } else {
                                refTimestamp + deltaMs
                            }

                            val targetPoint = if (isAddingBefore) {
                                if (pivotIndex!! > 0) rideLinesData[pivotIndex!! - 1] else null
                            } else {
                                if (pivotIndex!! < rideLinesData.size - 1) rideLinesData[pivotIndex!! + 1] else currentLocationTarget
                            }

                            var isInconsistent = false
                            var reason = ""

                            if (targetPoint != null) {
                                if (isAddingBefore && newTimestamp <= targetPoint.timestamp) {
                                    isInconsistent = true
                                    reason = "Le timestamp calculé est antérieur au point précédent. La vitesse choisie est trop faible."
                                } else if (!isAddingBefore && newTimestamp >= targetPoint.timestamp) {
                                    isInconsistent = true
                                    reason = "Le timestamp calculé est postérieur à la cible. La vitesse choisie est trop faible."
                                } else {
                                    val remRes = FloatArray(1)
                                    Location.distanceBetween(tapGeo.latitude, tapGeo.longitude, targetPoint.lat, targetPoint.lon, remRes)
                                    val remDistMeters = remRes[0].toDouble()
                                    val remTimeSec = if (isAddingBefore) {
                                        (newTimestamp - targetPoint.timestamp) / 1000.0
                                    } else {
                                        (targetPoint.timestamp - newTimestamp) / 1000.0
                                    }

                                    if (remTimeSec <= 0.0 || (remDistMeters / remTimeSec) > 30.0) {
                                        val reqKmh = if (remTimeSec > 0) (remDistMeters / remTimeSec) * 3.6 else 999.0
                                        isInconsistent = true
                                        reason = "La vitesse moyenne nécessaire pour fermer le trajet restant serait de ${String.format(Locale.US, "%.1f", reqKmh)} km/h (> 108 km/h). Ajustez la vitesse."
                                    }
                                }
                            }

                            if (isInconsistent) {
                                warningMessage = reason
                            } else {
                                isProcessing = true
                                val altitude = fetchAltitudeForPoint(
                                    lat = tapGeo.latitude,
                                    lon = tapGeo.longitude,
                                    timestamp = newTimestamp,
                                    fallbackAlt = refAlt
                                )
                                isProcessing = false

                                addedPoints.add(
                                    AddedPoint(
                                        timestamp = newTimestamp,
                                        latitude = tapGeo.latitude,
                                        longitude = tapGeo.longitude,
                                        altitude = altitude,
                                        speedKmh = enteredSpeedKmh
                                    )
                                )
                                pendingTapGeoPoint = null
                                Toast.makeText(context, "Point #${addedPoints.size} ajouté !", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showSpeedPromptDialog = false
                    pendingTapGeoPoint = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    // --- Dialogue d'avertissement en cas d'incohérence ---
    if (warningMessage != null) {
        AlertDialog(
            onDismissRequest = { warningMessage = null },
            title = { Text("Avertissement de vitesse") },
            text = { Text(warningMessage!!) },
            confirmButton = {
                Button(onClick = { warningMessage = null }) {
                    Text("Compris")
                }
            }
        )
    }

    // --- Dialogue de confirmation pour enregistrer les ajouts de points ---
    if (showAddConfirmDialog && pivotIndex != null && addedPoints.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showAddConfirmDialog = false },
            title = { Text("Enregistrer les ajouts") },
            text = { Text("Voulez-vous enregistrer ${addedPoints.size} nouveau(x) point(s) dans la trace ?\n\nLe fichier ride.tsv sera mis à jour et les données cumulées recalculées.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAddConfirmDialog = false
                        isProcessing = true
                        coroutineScope.launch {
                            val finalAddedList = addedPoints.toMutableList()
                            if (currentLocationTarget != null) {
                                val lastPt = if (finalAddedList.isNotEmpty()) {
                                    Pair(finalAddedList.last().latitude, finalAddedList.last().longitude)
                                } else {
                                    Pair(rideLinesData[pivotIndex!!].lat, rideLinesData[pivotIndex!!].lon)
                                }
                                val lastTs = if (finalAddedList.isNotEmpty()) {
                                    finalAddedList.last().timestamp
                                } else {
                                    rideLinesData[pivotIndex!!].timestamp
                                }

                                val distRes = FloatArray(1)
                                Location.distanceBetween(lastPt.first, lastPt.second, currentLocationTarget!!.lat, currentLocationTarget!!.lon, distRes)
                                val distM = distRes[0].toDouble()
                                val dtSec = maxOf(0.1, (currentLocationTarget!!.timestamp - lastTs) / 1000.0)
                                val speedKmh = (distM / dtSec) * 3.6

                                finalAddedList.add(
                                    AddedPoint(
                                        timestamp = currentLocationTarget!!.timestamp,
                                        latitude = currentLocationTarget!!.lat,
                                        longitude = currentLocationTarget!!.lon,
                                        altitude = currentLocationTarget!!.alt,
                                        speedKmh = speedKmh
                                    )
                                )
                            }

                            addPointsAndRecalculate(
                                context = context,
                                pivotIndex = pivotIndex!!,
                                isAddingBefore = isAddingBefore,
                                addedPoints = finalAddedList,
                                rideViewModel = rideViewModel
                            )
                            isProcessing = false
                            addedPoints.clear()
                            pivotIndex = null
                            currentLocationTarget = null
                            addStep = AddStep.SELECT_PIVOT
                            screenMode = ScreenMode.MAIN
                            Toast.makeText(context, "Mise à jour réussie !", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("Enregistrer", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showAddConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // --- Structure globale de l'interface ---
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        when (screenMode) {
            ScreenMode.MAIN -> {
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
                        Button(
                            onClick = { screenMode = ScreenMode.PLAN },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                        ) {
                            Text("Planifier itinéraire", color = Color.White, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Gestion des points de trace",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { screenMode = ScreenMode.ADD },
                            enabled = !isRecording,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Ajouter des points", color = Color.White, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { screenMode = ScreenMode.DELETE },
                            enabled = !isRecording,
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
            }

            ScreenMode.PLAN -> {
                PlanRoutePage(
                    rideViewModel = rideViewModel,
                    currentPosition = { rideViewModel.currentPosition() },
                    onBack = {
                        if (isRecording) {
                            onDone()
                        } else {
                            screenMode = ScreenMode.MAIN
                        }
                    },
                    onValidated = onDone
                )
            }

            ScreenMode.DELETE -> {
                // --- Mode Suppression de points ---
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

                    DisposableEffect(Unit) {
                        mapView.onResume()
                        onDispose {
                            mapView.onPause()
                            mapView.onDetach()
                        }
                    }

                    DisposableEffect(mapView) {
                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                if (p == null || trackPoints.isEmpty()) return false
                                val closestIdx = findClosestPointIndex(p, trackPoints)
                                if (closestIdx != null) {
                                    if (deleteStartIndex == null) {
                                        deleteStartIndex = closestIdx
                                        deleteEndIndex = null
                                    } else if (deleteEndIndex == null) {
                                        if (closestIdx < deleteStartIndex!!) {
                                            deleteEndIndex = deleteStartIndex
                                            deleteStartIndex = closestIdx
                                        } else {
                                            deleteEndIndex = closestIdx
                                        }
                                    } else {
                                        deleteStartIndex = closestIdx
                                        deleteEndIndex = null
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

                    LaunchedEffect(trackPoints, deleteStartIndex, deleteEndIndex) {
                        val geoPoints = trackPoints.map { GeoPoint(it.first, it.second) }
                        mainPolyline.safeSetPoints(geoPoints)

                        if (!mapView.overlays.contains(mainPolyline)) {
                            mapView.overlays.add(0, mainPolyline)
                        }

                        if (deleteStartIndex != null && deleteEndIndex != null) {
                            val start = minOf(deleteStartIndex!!, deleteEndIndex!!)
                            val end = maxOf(deleteStartIndex!!, deleteEndIndex!!)
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

                        if (deleteStartIndex != null && deleteStartIndex!! in trackPoints.indices) {
                            val pt = trackPoints[deleteStartIndex!!]
                            startMarker.position = GeoPoint(pt.first, pt.second)
                            startMarker.title = "Début (#${deleteStartIndex!! + 1})"
                            if (!mapView.overlays.contains(startMarker)) {
                                mapView.overlays.add(startMarker)
                            }
                        } else {
                            mapView.overlays.remove(startMarker)
                        }

                        if (deleteEndIndex != null && deleteEndIndex!! in trackPoints.indices) {
                            val pt = trackPoints[deleteEndIndex!!]
                            endMarker.position = GeoPoint(pt.first, pt.second)
                            endMarker.title = "Fin (#${deleteEndIndex!! + 1})"
                            if (!mapView.overlays.contains(endMarker)) {
                                mapView.overlays.add(endMarker)
                            }
                        } else {
                            mapView.overlays.remove(endMarker)
                        }

                        mapView.invalidate()
                    }

                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize()
                    )

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
                                deleteStartIndex == null -> "Tapez sur la carte pour choisir le PREMIER point (début de la zone à supprimer)."
                                deleteEndIndex == null -> "Premier point fixé (#${deleteStartIndex!! + 1}). Tapez sur la carte pour choisir le SECOND point."
                                else -> {
                                    val start = minOf(deleteStartIndex!!, deleteEndIndex!!)
                                    val end = maxOf(deleteStartIndex!!, deleteEndIndex!!)
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

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 12.dp)
                    ) {
                        IconButton(
                            onClick = { mapView.controller.zoomIn() },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom +", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { mapView.controller.zoomOut() },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = Color.White)
                        }
                    }

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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    deleteStartIndex = null
                                    deleteEndIndex = null
                                },
                                enabled = deleteStartIndex != null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Réinit.", textAlign = TextAlign.Center, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = deleteStartIndex != null && deleteEndIndex != null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Supprimer", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    screenMode = ScreenMode.MAIN
                                    deleteStartIndex = null
                                    deleteEndIndex = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Retour", textAlign = TextAlign.Center, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            ScreenMode.ADD -> {
                // --- Mode Ajout de points ---
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

                    val mainPolylinePart1 = remember {
                        Polyline().apply {
                            outlinePaint.color = android.graphics.Color.BLUE
                            outlinePaint.strokeWidth = 8f
                        }
                    }

                    val mainPolylinePart2 = remember {
                        Polyline().apply {
                            outlinePaint.color = android.graphics.Color.BLUE
                            outlinePaint.strokeWidth = 8f
                        }
                    }

                    val addedPolyline = remember {
                        Polyline().apply {
                            outlinePaint.color = android.graphics.Color.CYAN
                            outlinePaint.strokeWidth = 12f
                        }
                    }

                    val pivotMarker = remember {
                        Marker(mapView).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Point d'ancrage"
                        }
                    }

                    val currentPosMarker = remember {
                        Marker(mapView).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Position actuelle"
                        }
                    }

                    DisposableEffect(Unit) {
                        mapView.onResume()
                        onDispose {
                            mapView.onPause()
                            mapView.onDetach()
                        }
                    }

                    DisposableEffect(mapView, addStep, trackPoints) {
                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                if (p == null) return false
                                if (addStep == AddStep.SELECT_PIVOT) {
                                    if (trackPoints.isEmpty()) return false
                                    val idx = findClosestPointIndex(p, trackPoints)
                                    if (idx != null) {
                                        pivotIndex = idx
                                        showDirectionDialog = true
                                    }
                                } else if (addStep == AddStep.ADDING_POINTS) {
                                    pendingTapGeoPoint = p
                                    showSpeedPromptDialog = true
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

                    LaunchedEffect(trackPoints, pivotIndex, addStep, isAddingBefore, addedPoints.size, currentLocationTarget) {
                        // Gestion de l'effacement du tronçon remplacé sur la polyline principale
                        if (addStep == AddStep.ADDING_POINTS && pivotIndex != null && pivotIndex!! in trackPoints.indices) {
                            val idx = pivotIndex!!
                            if (isAddingBefore && idx > 0) {
                                val part1 = trackPoints.subList(0, idx).map { GeoPoint(it.first, it.second) }
                                mainPolylinePart1.safeSetPoints(part1)
                                if (!mapView.overlays.contains(mainPolylinePart1)) mapView.overlays.add(0, mainPolylinePart1)

                                val part2 = trackPoints.subList(idx, trackPoints.size).map { GeoPoint(it.first, it.second) }
                                mainPolylinePart2.safeSetPoints(part2)
                                if (!mapView.overlays.contains(mainPolylinePart2)) mapView.overlays.add(0, mainPolylinePart2)
                            } else if (!isAddingBefore && idx < trackPoints.size - 1) {
                                val part1 = trackPoints.subList(0, idx + 1).map { GeoPoint(it.first, it.second) }
                                mainPolylinePart1.safeSetPoints(part1)
                                if (!mapView.overlays.contains(mainPolylinePart1)) mapView.overlays.add(0, mainPolylinePart1)

                                val part2 = trackPoints.subList(idx + 1, trackPoints.size).map { GeoPoint(it.first, it.second) }
                                mainPolylinePart2.safeSetPoints(part2)
                                if (!mapView.overlays.contains(mainPolylinePart2)) mapView.overlays.add(0, mainPolylinePart2)
                            } else {
                                val geoPoints = trackPoints.map { GeoPoint(it.first, it.second) }
                                mainPolylinePart1.safeSetPoints(geoPoints)
                                if (!mapView.overlays.contains(mainPolylinePart1)) mapView.overlays.add(0, mainPolylinePart1)
                                mapView.overlays.remove(mainPolylinePart2)
                            }
                        } else {
                            val geoPoints = trackPoints.map { GeoPoint(it.first, it.second) }
                            mainPolylinePart1.safeSetPoints(geoPoints)
                            if (!mapView.overlays.contains(mainPolylinePart1)) mapView.overlays.add(0, mainPolylinePart1)
                            mapView.overlays.remove(mainPolylinePart2)
                        }

                        // Afficher le marqueur d'ancrage
                        if (pivotIndex != null && pivotIndex!! in trackPoints.indices) {
                            val pt = trackPoints[pivotIndex!!]
                            pivotMarker.position = GeoPoint(pt.first, pt.second)
                            pivotMarker.title = "Ancrage (#${pivotIndex!! + 1})"
                            if (!mapView.overlays.contains(pivotMarker)) {
                                mapView.overlays.add(pivotMarker)
                            }
                        } else {
                            mapView.overlays.remove(pivotMarker)
                        }

                        // Afficher la position actuelle si on ajoute après le dernier point
                        if (currentLocationTarget != null) {
                            currentPosMarker.position = GeoPoint(currentLocationTarget!!.lat, currentLocationTarget!!.lon)
                            currentPosMarker.title = "Position actuelle"
                            if (!mapView.overlays.contains(currentPosMarker)) {
                                mapView.overlays.add(currentPosMarker)
                            }
                        } else {
                            mapView.overlays.remove(currentPosMarker)
                        }

                        // Tracé des points ajoutés
                        if (pivotIndex != null && pivotIndex!! in trackPoints.indices && addedPoints.isNotEmpty()) {
                            val pivotPt = trackPoints[pivotIndex!!]
                            val ptsList = mutableListOf(GeoPoint(pivotPt.first, pivotPt.second))
                            ptsList.addAll(addedPoints.map { GeoPoint(it.latitude, it.longitude) })
                            addedPolyline.safeSetPoints(ptsList)

                            if (!mapView.overlays.contains(addedPolyline)) {
                                mapView.overlays.add(addedPolyline)
                            }
                        } else {
                            mapView.overlays.remove(addedPolyline)
                        }

                        mapView.invalidate()
                    }

                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bandeau d'instructions supérieur
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
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
                            val minSpeedReq = if (addStep == AddStep.ADDING_POINTS && pivotIndex != null) {
                                calculateMinSpeedRequired(rideLinesData, pivotIndex!!, isAddingBefore, addedPoints, currentLocationTarget)
                            } else null

                            val instructionText = when (addStep) {
                                AddStep.SELECT_PIVOT -> "Tapez sur la carte pour choisir le point de référence (point d'ancrage)."
                                AddStep.ADDING_POINTS -> {
                                    val dirStr = if (isAddingBefore) "AVANT (ordre chrono inversé)" else "APRÈS (ordre chrono)"
                                    val targetLabel = if (!isAddingBefore && pivotIndex == rideLinesData.size - 1) "position actuelle" else "trajet restant"
                                    val speedStr = if (minSpeedReq != null && minSpeedReq > 0.0) {
                                        "Vitesse min. pour rejoindre $targetLabel : ${String.format(Locale.US, "%.1f", minSpeedReq)} km/h"
                                    } else "Vitesse min. restante : N/A"

                                    "Ajout $dirStr (#${addedPoints.size} ajoutés)\n$speedStr"
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

                    // Zoom
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 90.dp, end = 12.dp)
                    ) {
                        IconButton(
                            onClick = { mapView.controller.zoomIn() },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom +", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { mapView.controller.zoomOut() },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = Color.White)
                        }
                    }

                    // Barre d'actions inférieure équilibrée
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (addedPoints.isNotEmpty()) {
                                        addedPoints.removeAt(addedPoints.size - 1)
                                    }
                                },
                                enabled = addedPoints.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Annuler\ndernier", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 14.sp)
                            }

                            Button(
                                onClick = { showAddConfirmDialog = true },
                                enabled = addedPoints.isNotEmpty() || currentLocationTarget != null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                            ) {
                                Text("Terminer", textAlign = TextAlign.Center, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    screenMode = ScreenMode.MAIN
                                    addStep = AddStep.SELECT_PIVOT
                                    pivotIndex = null
                                    currentLocationTarget = null
                                    addedPoints.clear()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Retour", textAlign = TextAlign.Center, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Overlay de traitement en cours
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
                        text = "Calculs et mise à jour de ride.tsv en cours...\nVeuillez patienter.",
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
 * Récupère l'altitude pour un point donné en s'appuyant sur AltitudeManager.
 */
private suspend fun fetchAltitudeForPoint(
    lat: Double,
    lon: Double,
    timestamp: Long,
    fallbackAlt: Double?
): Double? {
    val sample = GpsSample(
        timestamp = timestamp,
        latitude = lat,
        longitude = lon,
        accuracy = 5f,
        speedGps = 0f,
        speedAccuracy = 0f
    )
    val altManager = AppConfig.altitudeManager
    if (altManager != null) {
        try {
            altManager.onNewGpsLocation(sample)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return sample.altitudeLidarHdMnt
        ?: sample.altitudeIgn
        ?: sample.altitudeSrtm
        ?: fallbackAlt
}

/**
 * Charge les données détaillées de chaque ligne de ride.tsv
 */
private fun loadRideLinesData(context: Context): List<RideLineData> {
    val rideFile = File(context.filesDir, "ride.tsv")
    if (!rideFile.exists()) return emptyList()
    val lines = rideFile.readLines().filter { it.isNotBlank() }
    val result = mutableListOf<RideLineData>()
    lines.forEachIndexed { index, line ->
        val tokens = line.split("\t")
        if (tokens.size >= 8) {
            val ts = tokens[0].toLongOrNull() ?: 0L
            val lat = tokens[1].toDoubleOrNull() ?: 0.0
            val lon = tokens[2].toDoubleOrNull() ?: 0.0
            val alt = tokens[3].toDoubleOrNull() ?: 0.0
            val dPlus = tokens[4].toDoubleOrNull() ?: 0.0
            val durSec = tokens[5].toDoubleOrNull() ?: 0.0
            val speed = tokens[6].toDoubleOrNull() ?: 0.0
            val dist = tokens[7].toDoubleOrNull() ?: 0.0
            result.add(
                RideLineData(
                    lineIndex = index,
                    timestamp = ts,
                    lat = lat,
                    lon = lon,
                    alt = alt,
                    dPlus = dPlus,
                    durationSec = durSec,
                    speed = speed,
                    totalDist = dist,
                    rawTokens = tokens
                )
            )
        }
    }
    return result
}

/**
 * Calcule la vitesse moyenne minimale requise (en km/h) pour fermer le trajet restant.
 */
private fun calculateMinSpeedRequired(
    rideLines: List<RideLineData>,
    pivotIndex: Int,
    isAddingBefore: Boolean,
    addedPoints: List<AddedPoint>,
    currentLocationTarget: RideLineData? = null
): Double? {
    if (rideLines.isEmpty() || pivotIndex !in rideLines.indices) return null

    val refLat: Double
    val refLon: Double
    val refTimestamp: Long

    if (addedPoints.isNotEmpty()) {
        val lastAdded = addedPoints.last()
        refLat = lastAdded.latitude
        refLon = lastAdded.longitude
        refTimestamp = lastAdded.timestamp
    } else {
        val pivot = rideLines[pivotIndex]
        refLat = pivot.lat
        refLon = pivot.lon
        refTimestamp = pivot.timestamp
    }

    val targetPoint: RideLineData? = if (isAddingBefore) {
        if (pivotIndex > 0) rideLines[pivotIndex - 1] else null
    } else {
        if (pivotIndex < rideLines.size - 1) {
            rideLines[pivotIndex + 1]
        } else {
            currentLocationTarget
        }
    }

    if (targetPoint == null) return 0.0

    val distResults = FloatArray(1)
    Location.distanceBetween(refLat, refLon, targetPoint.lat, targetPoint.lon, distResults)
    val distMeters = distResults[0].toDouble()

    val timeDeltaSec = if (isAddingBefore) {
        (refTimestamp - targetPoint.timestamp) / 1000.0
    } else {
        (targetPoint.timestamp - refTimestamp) / 1000.0
    }

    if (timeDeltaSec <= 0.0) return 0.0
    val minSpeedMs = distMeters / timeDeltaSec
    return minSpeedMs * 3.6
}

/**
 * Insère les points ajoutés dans ride.tsv et recalcule les données cumulées (D+, temps, distance).
 */
private suspend fun addPointsAndRecalculate(
    context: Context,
    pivotIndex: Int,
    isAddingBefore: Boolean,
    addedPoints: List<AddedPoint>,
    rideViewModel: RideViewModel
) = withContext(Dispatchers.IO) {
    val rideFile = File(context.filesDir, "ride.tsv")
    if (!rideFile.exists() || addedPoints.isEmpty()) return@withContext

    val rawLines = rideFile.readLines()
    val lines = rawLines.filter { it.isNotBlank() }
    if (lines.isEmpty() || pivotIndex !in lines.indices) return@withContext

    val sortedAdded = addedPoints.sortedBy { it.timestamp }

    val tempFile = File(context.filesDir, "ride_temp.tsv")
    if (tempFile.exists()) tempFile.delete()

    val writer = tempFile.bufferedWriter()

    val lastCopiedIndex = if (isAddingBefore) pivotIndex - 1 else pivotIndex

    // 1. Copier les lignes initiales précédant le point d'insertion
    for (i in 0..lastCopiedIndex) {
        if (i in lines.indices) {
            writer.write(lines[i])
            writer.newLine()
        }
    }

    // Récupérer les données cumulées de référence
    var prevLat: Double
    var prevLon: Double
    var prevAlt: Double
    var prevDPlus: Double
    var prevTimeSec: Double
    var prevDist: Double
    var prevTimestamp: Long

    if (lastCopiedIndex >= 0 && lastCopiedIndex in lines.indices) {
        val lastTokens = lines[lastCopiedIndex].split("\t")
        prevTimestamp = lastTokens.getOrNull(0)?.toLongOrNull() ?: sortedAdded.first().timestamp
        prevLat = lastTokens.getOrNull(1)?.toDoubleOrNull() ?: sortedAdded.first().latitude
        prevLon = lastTokens.getOrNull(2)?.toDoubleOrNull() ?: sortedAdded.first().longitude
        prevAlt = lastTokens.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        prevDPlus = lastTokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
        prevTimeSec = lastTokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
        prevDist = lastTokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0
    } else {
        val firstPt = sortedAdded.first()
        prevTimestamp = firstPt.timestamp
        prevLat = firstPt.latitude
        prevLon = firstPt.longitude
        prevAlt = firstPt.altitude ?: 0.0
        prevDPlus = 0.0
        prevTimeSec = 0.0
        prevDist = 0.0
    }

    // 2. Insérer les nouveaux points et calculer leurs valeurs cumulées
    var lastAddedSpeedMs = 0.0
    for (pt in sortedAdded) {
        val distRes = FloatArray(1)
        Location.distanceBetween(prevLat, prevLon, pt.latitude, pt.longitude, distRes)
        val distMeters = distRes[0].toDouble()

        val ptAlt = pt.altitude ?: prevAlt
        val altDiff = maxOf(0.0, ptAlt - prevAlt)
        val newDPlus = prevDPlus + altDiff

        val dtSec = maxOf(0.0, (pt.timestamp - prevTimestamp) / 1000.0)
        val maxTimeInc = distMeters / 0.5
        val clampedDtSec = minOf(dtSec, maxTimeInc).coerceAtLeast(0.0)
        val newTimeSec = prevTimeSec + clampedDtSec

        val newDist = prevDist + distMeters
        val speedMs = if (clampedDtSec > 0.0) distMeters / clampedDtSec else (pt.speedKmh / 3.6)
        lastAddedSpeedMs = speedMs

        val lineStr = "${pt.timestamp}\t${pt.latitude}\t${pt.longitude}\t$ptAlt\t$newDPlus\t$newTimeSec\t$speedMs\t$newDist\tnull\tnull\tnull"
        writer.write(lineStr)
        writer.newLine()

        prevLat = pt.latitude
        prevLon = pt.longitude
        prevAlt = ptAlt
        prevDPlus = newDPlus
        prevTimeSec = newTimeSec
        prevDist = newDist
        prevTimestamp = pt.timestamp
    }

    // 3. Recalculer les lignes d'origine ultérieures
    val firstSubsequentIndex = lastCopiedIndex + 1
    if (firstSubsequentIndex in lines.indices) {
        val nextTokens = lines[firstSubsequentIndex].split("\t")
        val nextLat = nextTokens.getOrNull(1)?.toDoubleOrNull() ?: prevLat
        val nextLon = nextTokens.getOrNull(2)?.toDoubleOrNull() ?: prevLon
        val nextAlt = nextTokens.getOrNull(3)?.toDoubleOrNull() ?: prevAlt
        val nextDPlus = nextTokens.getOrNull(4)?.toDoubleOrNull() ?: prevDPlus
        val nextTimeSec = nextTokens.getOrNull(5)?.toDoubleOrNull() ?: prevTimeSec
        val nextSpeed = nextTokens.getOrNull(6)?.toDoubleOrNull() ?: 0.0
        val nextTotalDist = nextTokens.getOrNull(7)?.toDoubleOrNull() ?: prevDist

        val distRes = FloatArray(1)
        Location.distanceBetween(prevLat, prevLon, nextLat, nextLon, distRes)
        val distBetween = distRes[0].toDouble()

        val deltaDistance = nextTotalDist - prevDist - distBetween
        val altDiff = maxOf(0.0, nextAlt - prevAlt)
        val deltaDenivele = nextDPlus - prevDPlus - altDiff

        val avgSpeed = (nextSpeed + lastAddedSpeedMs) / 2.0
        val timeBetween = distBetween / minOf(avgSpeed, 1.0)
        val timeDiff = (nextTimeSec - prevTimeSec) - timeBetween
        val deltaTime = if (timeDiff < 0.0) 0.0 else timeDiff

        for (i in firstSubsequentIndex until lines.size) {
            val tokens = lines[i].split("\t").toMutableList()
            if (tokens.size >= 8) {
                val currentDPlus = tokens[4].toDoubleOrNull() ?: 0.0
                val currentTimeSec = tokens[5].toDoubleOrNull() ?: 0.0
                val currentDist = tokens[7].toDoubleOrNull() ?: 0.0

                val updatedDPlus = maxOf(0.0, currentDPlus - deltaDenivele)
                val updatedTimeSec = maxOf(0.0, currentTimeSec - deltaTime)
                val updatedDist = maxOf(0.0, currentDist - deltaDistance)

                tokens[4] = updatedDPlus.toString()
                tokens[5] = updatedTimeSec.toString()
                tokens[7] = updatedDist.toString()
            }
            writer.write(tokens.joinToString("\t"))
            writer.newLine()
        }
    }

    writer.flush()
    writer.close()

    // Remplacer ride.tsv
    tempFile.copyTo(rideFile, overwrite = true)
    tempFile.delete()

    // Mise à jour de GpsBuffer
    val updatedLines = rideFile.readLines().filter { it.isNotBlank() }
    if (updatedLines.isNotEmpty()) {
        val lastTokens = updatedLines.last().split("\t")
        if (lastTokens.size >= 8) {
            val newLastDPlus = lastTokens[4].toDoubleOrNull() ?: 0.0
            val newLastDurationSec = lastTokens[5].toDoubleOrNull() ?: 0.0
            val newLastDist = lastTokens[7].toDoubleOrNull() ?: 0.0
            val newLastLatitude = lastTokens[1].toDoubleOrNull() ?: 0.0
            val newLastLongitude = lastTokens[2].toDoubleOrNull() ?: 0.0
            val newLastAltitude = lastTokens[3].toDoubleOrNull() ?: 0.0


            val sample1 = GpsBuffer.getLast()
            val sample2= GpsBuffer.getNthBeforeLast(2)
            val sample3 = GpsBuffer.getNthBeforeLast(3)
            if (sample1 != null) {
                sample1.gpsPointLatitude = newLastLatitude
                sample1.gpsPointLongitude = newLastLongitude
                sample1.gpsPointAltitude = newLastAltitude
                sample1.gpsPointElevationGain = newLastDPlus
                sample1.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample1.gpsPointTotalDistance = newLastDist
                sample1.gpsPointAltForElevationGain = newLastAltitude
            }
            if (sample2 != null) {
                sample2.gpsPointLatitude = newLastLatitude
                sample2.gpsPointLongitude = newLastLongitude
                sample2.gpsPointAltitude = newLastAltitude
                sample2.gpsPointElevationGain = newLastDPlus
                sample2.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample2.gpsPointTotalDistance = newLastDist
                sample2.gpsPointAltForElevationGain = newLastAltitude
            }
            if (sample3 != null) {
                sample3.gpsPointLatitude = newLastLatitude
                sample3.gpsPointLongitude = newLastLongitude
                sample3.gpsPointAltitude = newLastAltitude
                sample3.gpsPointElevationGain = newLastDPlus
                sample3.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample3.gpsPointTotalDistance = newLastDist
                sample3.gpsPointAltForElevationGain = newLastAltitude
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
            val timeBetween = distBetween / minOf(avgSpeed, 1.0)
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

                val updatedDPlus = maxOf(0.0, currentDPlus - deltaDenivele)
                val updatedTimeSec = maxOf(0.0, currentTimeSec - deltaTime)
                val updatedDist = maxOf(0.0, currentDist - deltaDistance)

                tokens[4] = updatedDPlus.toString()
                tokens[5] = updatedTimeSec.toString()
                tokens[7] = updatedDist.toString()
            }
            writer.write(tokens.joinToString("\t"))
            writer.newLine()
        }
    }

    writer.flush()
    writer.close()

    // Remplacer ride.tsv
    tempFile.copyTo(rideFile, overwrite = true)
    tempFile.delete()

    // Mise à jour de GpsBuffer
    val updatedLines = rideFile.readLines().filter { it.isNotBlank() }
    if (updatedLines.isNotEmpty()) {
        val lastTokens = updatedLines.last().split("\t")
        if (lastTokens.size >= 8) {
            val newLastDPlus = lastTokens[4].toDoubleOrNull() ?: 0.0
            val newLastDurationSec = lastTokens[5].toDoubleOrNull() ?: 0.0
            val newLastDist = lastTokens[7].toDoubleOrNull() ?: 0.0
            val newLastLatitude = lastTokens[1].toDoubleOrNull() ?: 0.0
            val newLastLongitude = lastTokens[2].toDoubleOrNull() ?: 0.0
            val newLastAltitude = lastTokens[3].toDoubleOrNull() ?: 0.0


            val sample1 = GpsBuffer.getLast()
            val sample2= GpsBuffer.getNthBeforeLast(2)
            val sample3 = GpsBuffer.getNthBeforeLast(3)
            if (sample1 != null) {
                sample1.gpsPointLatitude = newLastLatitude
                sample1.gpsPointLongitude = newLastLongitude
                sample1.gpsPointAltitude = newLastAltitude
                sample1.gpsPointElevationGain = newLastDPlus
                sample1.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample1.gpsPointTotalDistance = newLastDist
                sample1.gpsPointAltForElevationGain = newLastAltitude
            }
            if (sample2 != null) {
                sample2.gpsPointLatitude = newLastLatitude
                sample2.gpsPointLongitude = newLastLongitude
                sample2.gpsPointAltitude = newLastAltitude
                sample2.gpsPointElevationGain = newLastDPlus
                sample2.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample2.gpsPointTotalDistance = newLastDist
                sample2.gpsPointAltForElevationGain = newLastAltitude
            }
            if (sample3 != null) {
                sample3.gpsPointLatitude = newLastLatitude
                sample3.gpsPointLongitude = newLastLongitude
                sample3.gpsPointAltitude = newLastAltitude
                sample3.gpsPointElevationGain = newLastDPlus
                sample3.gpsPointDurationTime = (newLastDurationSec * 1000.0).toLong()
                sample3.gpsPointTotalDistance = newLastDist
                sample3.gpsPointAltForElevationGain = newLastAltitude
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
