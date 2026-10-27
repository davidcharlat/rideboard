package com.example.rideboard.ui

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.rideboard.RideViewModel
import com.example.rideboard.brouter.BRouterFiles
import com.example.rideboard.brouter.CustomProfile
import com.example.rideboard.brouter.CustomProfileStore
import com.example.rideboard.brouter.ParamType
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.io.File

private data class RouteProfile(val label: String, val file: String)

private val ROUTE_PROFILES = listOf(
    RouteProfile("Personnalisé", CustomProfile.FILE),
    RouteProfile("Vélo de route", "fastbike"),
    RouteProfile("Gravel", "gravel"),
    RouteProfile("VTT", "MTB"),
    RouteProfile("Vélo couché", "vm-forum-liegerad-schnell"),
    RouteProfile("Vélomobile", "vm-forum-velomobil-schnell"),
    RouteProfile("promenade", "trekking"),
    RouteProfile("Randonnée", "hiking-mountain"),
    RouteProfile("Voiture", "car-vario"),
    RouteProfile("Le plus court", "shortest")
)

private enum class Placing { START, END }

@Composable
private fun SelectButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(label, maxLines = 1)
        }
    }
}

private fun lengthKm(points: List<GeoPoint>): Double {
    var m = 0.0
    for (i in 1 until points.size) m += points[i - 1].distanceToAsDouble(points[i])
    return m / 1000.0
}

private fun dotMarker(mapView: MapView, p: GeoPoint, color: Int): Marker =
    Marker(mapView).apply {
        position = p
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = ShapeDrawable(OvalShape()).apply {
            setIntrinsicWidth(40)
            setIntrinsicHeight(40)
            paint.color = color
        }
        setInfoWindow(null)
    }

private fun formatDurationHm(totalS: Int?): String {
    if (totalS == null) return "--"
    val minutes = (totalS + 30) / 60
    return "%dh%02d".format(minutes / 60, minutes % 60)
}

@Composable
fun PlanRoutePage(
    rideViewModel: RideViewModel,
    currentPosition: () -> GeoPoint?,
    onBack: () -> Unit,
    onValidated: () -> Unit
) {
    val context = LocalContext.current

    val profiles = remember {
        ROUTE_PROFILES.filter { File(BRouterFiles.profilesDir(context), "${it.file}.brf").exists() }
    }
    var profile by remember { mutableStateOf(profiles.firstOrNull()) }
    var start by remember { mutableStateOf<GeoPoint?>(null) }
    var end by remember { mutableStateOf<GeoPoint?>(null) }
    var placing by remember { mutableStateOf(Placing.START) }
    var selected by remember { mutableStateOf(0) }
    var showCustom by remember { mutableStateOf(false) }

    val variants by rideViewModel.routeVariants
    val legs by rideViewModel.legs
    val planning by rideViewModel.planning
    val status by rideViewModel.planStatus
    val durations by rideViewModel.routeDurations
    val legDurations by rideViewModel.legDurations
    val legsSeconds = if (legDurations.isNotEmpty() && legDurations.all { it != null })
        legDurations.filterNotNull().sum() else null
    val lengths = remember(variants) { variants.map { lengthKm(it) } }
    val legsKm = remember(legs) { legs.sumOf { lengthKm(it) } }

    DisposableEffect(Unit) { onDispose { rideViewModel.clearPlan() } }

    // ---------------- La carte ----------------
    val initialPosition = remember { currentPosition() }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.OpenTopo)
            setMultiTouchControls(true)
            controller.setZoom(if (initialPosition != null) 12.0 else 6.0)
            controller.setCenter(initialPosition ?: GeoPoint(46.6, 2.4))
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    DisposableEffect(mapView, placing) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (placing == Placing.START) {
                    start = p
                    placing = Placing.END
                } else {
                    end = p
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        val overlay = MapEventsOverlay(receiver)
        mapView.overlays.add(0, overlay)
        onDispose { mapView.overlays.remove(overlay) }
    }

    val dynamicOverlays = remember { mutableListOf<Overlay>() }
    LaunchedEffect(start, end, variants, selected, legs) {
        mapView.overlays.removeAll(dynamicOverlays)
        dynamicOverlays.clear()

        legs.forEach { pts ->
            dynamicOverlays += Polyline().apply {
                setPoints(pts)
                outlinePaint.color = android.graphics.Color.rgb(30, 100, 255)
                outlinePaint.strokeWidth = 12f
            }
        }
        variants.forEachIndexed { i, pts ->
            if (i != selected) {
                dynamicOverlays += Polyline().apply {
                    setPoints(pts)
                    outlinePaint.color = android.graphics.Color.argb(170, 120, 120, 120)
                    outlinePaint.strokeWidth = 8f
                }
            }
        }
        variants.getOrNull(selected)?.let { pts ->
            dynamicOverlays += Polyline().apply {
                setPoints(pts)
                outlinePaint.color = android.graphics.Color.rgb(255, 140, 0)
                outlinePaint.strokeWidth = 12f
            }
        }
        start?.let { dynamicOverlays += dotMarker(mapView, it, android.graphics.Color.rgb(0, 170, 0)) }
        end?.let { dynamicOverlays += dotMarker(mapView, it, android.graphics.Color.rgb(220, 0, 0)) }

        mapView.overlays.addAll(dynamicOverlays)
        mapView.invalidate()
    }

    LaunchedEffect(variants.size) {
        if (variants.size == 1) {
            mapView.post {
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(variants[0]), true, 100)
            }
        }
    }

    // ---------------- La mise en page ----------------
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (profile?.file == CustomProfile.FILE) {
                OutlinedButton(onClick = { showCustom = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Régler…", maxLines = 1)
                }
            }
            profiles.forEach { p -> SelectButton(p.label, p == profile) { profile = p } }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectButton("Départ", placing == Placing.START, Modifier.weight(1f), enabled = legs.isEmpty()) {
                placing = Placing.START
            }
            SelectButton("Arrivée", placing == Placing.END, Modifier.weight(1f)) { placing = Placing.END }
            OutlinedButton(
                onClick = {
                    val pos = currentPosition()
                    if (pos != null) {
                        start = pos
                        placing = Placing.END
                    } else {
                        Toast.makeText(context, "Position GPS indisponible", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = legs.isEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("Ma position", maxLines = 1) }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }

        if (legs.isNotEmpty()) {
            Text("Parcours validé : %.1f km · %s (%d tronçon(s))".format(legsKm, formatDurationHm(legsSeconds), legs.size))
        }
        if (status.isNotEmpty()) Text(status)

        if (variants.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                variants.indices.forEach { i ->
                    SelectButton(
                        "Var${i + 1}: ${"%.1f".format(lengths[i])}km ${formatDurationHm(durations.getOrNull(i))}",
                        i == selected
                    ) { selected = i }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    selected = 0
                    val params =
                        if (profile!!.file == CustomProfile.FILE) CustomProfileStore.current() else emptyMap()
                    rideViewModel.computeVariants(start!!, end!!, profile!!.file, params)
                },
                enabled = !planning && start != null && end != null && profile != null,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(if (planning) "Calcul…" else "Calculer", maxLines = 1) }

            OutlinedButton(
                onClick = {
                    val leg = variants.getOrNull(selected)
                    if (leg != null) {
                        rideViewModel.commitVariant(selected)
                        start = leg.last()
                        end = null
                        placing = Placing.END
                        selected = 0
                    }
                },
                enabled = !planning && variants.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("Étape suivante", maxLines = 1) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Retour", maxLines = 1) }
            Button(
                onClick = {
                    rideViewModel.useVariant(selected)
                    onValidated()
                },
                enabled = !planning && (variants.isNotEmpty() || legs.isNotEmpty()),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("Utiliser", maxLines = 1) }
        }
        if (showCustom) CustomProfileDialog { showCustom = false }
    }
}

@Composable
private fun CustomProfileDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val values = remember { mutableStateMapOf<String, String>().apply { putAll(CustomProfileStore.current()) } }
    val elevationOn = values["consider_elevation"] == "true"

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profil personnalisé", style = MaterialTheme.typography.titleMedium)

                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var lastGroup = ""
                    CustomProfile.PARAMS.forEach { p ->
                        if (p.group != lastGroup) {
                            Text(
                                p.group,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            lastGroup = p.group
                        }

                        val wetOn = values["assume_wet_conditions"] == "true"
                        val enabled = when {
                            p.group == CustomProfile.G_ELEVATION -> p.key == "consider_elevation" || elevationOn
                            p.key.endsWith("_dry") -> !wetOn
                            p.key.endsWith("_wet") -> wetOn
                            else -> true
                        }
                        when (p.type) {
                            ParamType.BOOL -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.label)
                                    Text(p.help, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = values[p.key] == "true",
                                    onCheckedChange = { values[p.key] = it.toString() }
                                )
                            }

                            ParamType.NUMBER -> {
                                val v = (values[p.key]?.toFloatOrNull() ?: p.default.toFloat())
                                    .coerceIn(p.min, p.max)
                                Column(Modifier.alpha(if (enabled) 1f else 0.4f)) {
                                    Text("${p.label} : ${CustomProfile.formatValue(p, v)}")
                                    Text(p.help, style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = v,
                                        onValueChange = { raw ->
                                            val snapped = Math.round(raw / p.step) * p.step
                                            values[p.key] = CustomProfile.formatValue(p, snapped)
                                        },
                                        valueRange = p.min..p.max,
                                        enabled = enabled
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        values.clear()
                        CustomProfile.PARAMS.forEach { values[it.key] = it.default }
                    }) { Text("Valeurs d'origine") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        CustomProfileStore.save(context, values)
                        onDismiss()
                    }) { Text("Enregistrer") }
                }
            }
        }
    }
}
