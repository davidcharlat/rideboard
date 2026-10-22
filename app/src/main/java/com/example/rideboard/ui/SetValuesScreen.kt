package com.example.rideboard.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.rideboard.AppSettings
import com.example.rideboard.RideViewModel
import com.example.rideboard.brouter.BRouterFiles
import com.example.rideboard.brouter.RoutePlanner
import com.example.rideboard.brouter.SegmentDownloader
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.rideboard.brouter.CustomProfile
import com.example.rideboard.brouter.CustomProfileStore
import com.example.rideboard.brouter.ParamType
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

private enum class SettingsPage { MENU, ZONES, PREFERENCES, RINGING, EXPORT, DELETE, PLAN }

@Composable
fun SetValuesScreen(
    onDone: () -> Unit,
    rideViewModel: RideViewModel,
    currentPosition: GeoPoint?
) {
    var page by remember { mutableStateOf(SettingsPage.MENU) }

    BackHandler {
        page = when (page) {
            SettingsPage.MENU -> { onDone(); SettingsPage.MENU }
            SettingsPage.ZONES, SettingsPage.PREFERENCES, SettingsPage.PLAN -> SettingsPage.MENU
            SettingsPage.RINGING, SettingsPage.EXPORT, SettingsPage.DELETE -> SettingsPage.PREFERENCES
        }
    }

    when (page) {
        SettingsPage.MENU -> MenuPage(
            onZones = { page = SettingsPage.ZONES },
            onPreferences = { page = SettingsPage.PREFERENCES },
            onPlan = { page = SettingsPage.PLAN },
            onBack = onDone
        )
        SettingsPage.ZONES -> ZonesPage(onBack = { page = SettingsPage.MENU })
        SettingsPage.PREFERENCES ->
            PreferencesPage(
            onRinging = { page = SettingsPage.RINGING },
            onExport = { page = SettingsPage.EXPORT },
            onDelete = { page = SettingsPage.DELETE },
            onBack = { page = SettingsPage.MENU }
        )
        SettingsPage.RINGING -> RingingPage(onBack = { page = SettingsPage.PREFERENCES })
        SettingsPage.EXPORT -> ExportRidePage(onBack = { page = SettingsPage.PREFERENCES })
        SettingsPage.DELETE -> DeleteRidePage(onBack = { page = SettingsPage.PREFERENCES })
        SettingsPage.PLAN -> PlanRoutePage(
            rideViewModel = rideViewModel,
            currentPosition = { rideViewModel.currentPosition() },
            onBack = { page = SettingsPage.MENU },
            onValidated = onDone
        )
    }
}

// ---------------------------------------------------------------
// Page 1 : menu principal
// ---------------------------------------------------------------
@Composable
private fun MenuPage(onZones: () -> Unit, onPreferences: () -> Unit, onBack: () -> Unit, onPlan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onZones, modifier = Modifier.fillMaxWidth()) {
            Text("Définir les zones (W & bpm)")
        }
        Button(onClick = onPreferences, modifier = Modifier.fillMaxWidth()) {
            Text("Préférences")
        }
        Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Planifier itinéraire") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

// ---------------------------------------------------------------
// Page 2 : zones (champs compacts, 2 par ligne, scrollable)
// ---------------------------------------------------------------
@Composable
private fun ZonesPage(onBack: () -> Unit) {
    val context = LocalContext.current

    var p2 by remember { mutableStateOf(AppSettings.powerZone2.toString()) }
    var p3 by remember { mutableStateOf(AppSettings.powerZone3.toString()) }
    var p4 by remember { mutableStateOf(AppSettings.powerZone4.toString()) }
    var p5 by remember { mutableStateOf(AppSettings.powerZone5.toString()) }
    var h2 by remember { mutableStateOf(AppSettings.hrZone2.toString()) }
    var h3 by remember { mutableStateOf(AppSettings.hrZone3.toString()) }
    var h4 by remember { mutableStateOf(AppSettings.hrZone4.toString()) }
    var h5 by remember { mutableStateOf(AppSettings.hrZone5.toString()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zone défilante : prend la place restante, les boutons restent toujours visibles dessous
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Zones de puissance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 2 (W)", p2, { p2 = it }, Modifier.weight(1f))
                ZoneField("Zone 3 (W)", p3, { p3 = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 4 (W)", p4, { p4 = it }, Modifier.weight(1f))
                ZoneField("Zone 5 (W)", p5, { p5 = it }, Modifier.weight(1f))
            }

            Text("Zones de fréquence cardiaque", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 2 (bpm)", h2, { h2 = it }, Modifier.weight(1f))
                ZoneField("Zone 3 (bpm)", h3, { h3 = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 4 (bpm)", h4, { h4 = it }, Modifier.weight(1f))
                ZoneField("Zone 5 (bpm)", h5, { h5 = it }, Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Retour")
            }
            Button(
                onClick = {
                    p2.toIntOrNull()?.let { AppSettings.powerZone2 = it }
                    p3.toIntOrNull()?.let { AppSettings.powerZone3 = it }
                    p4.toIntOrNull()?.let { AppSettings.powerZone4 = it }
                    p5.toIntOrNull()?.let { AppSettings.powerZone5 = it }
                    h2.toIntOrNull()?.let { AppSettings.hrZone2 = it }
                    h3.toIntOrNull()?.let { AppSettings.hrZone3 = it }
                    h4.toIntOrNull()?.let { AppSettings.hrZone4 = it }
                    h5.toIntOrNull()?.let { AppSettings.hrZone5 = it }
                    AppSettings.save(context)
                    onBack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun ZoneField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

// ---------------------------------------------------------------
// Page 3 : préférences
// ---------------------------------------------------------------
@Composable
private fun PreferencesPage(
    onRinging: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sortie") }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Supprimer des sorties") }
        Button(onClick = onRinging, modifier = Modifier.fillMaxWidth()) { Text("Sonnerie") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Retour") }
    }
}

// ---------------------------------------------------------------
// Page 4 : sonnerie
// ---------------------------------------------------------------
@Composable
private fun RingingPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var ringing by remember { mutableStateOf(AppSettings.isRinging) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Émettre un son lorsqu'on s'arrête ou redémarre ?",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(if (ringing) "Oui" else "Non")
            Switch(
                checked = ringing,
                onCheckedChange = {
                    ringing = it
                    AppSettings.isRinging = it
                    AppSettings.save(context)   // sauvegarde immédiate : isRinging=1 ou 0
                }
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

@Composable
private fun DeleteRidePage(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(listRides(context)) }
    var pendingDelete by remember { mutableStateOf<List<File>?>(null) }

    RideSelectionPage(
        title = "Supprimer des sorties",
        actionLabel = "Supprimer",
        files = files,
        onAction = { pendingDelete = it },
        onBack = onBack
    )

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer définitivement ${toDelete.size} sortie(s) ?") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete.forEach { it.delete() }
                    files = listRides(context)   // rafraîchit la liste
                    pendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun ExportRidePage(onBack: () -> Unit) {
    val context = LocalContext.current
    val files = remember { listRides(context) }

    RideSelectionPage(
        title = "Exporter des sorties",
        actionLabel = "Exporter",
        files = files,
        onAction = { selectedFiles ->
            val uris = ArrayList(selectedFiles.map {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "application/octet-stream"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, "Exporter les sorties"))
        },
        onBack = onBack
    )
}


private fun listRides(context: Context): List<File> =
    File(context.filesDir, "rides")
        .listFiles { f -> f.extension == "fit" }
        ?.sortedByDescending { it.name }   // les plus récentes en premier (nom = AAMMJJHHMMSS)
        ?: emptyList()

// "260920185233.fit" -> "20/09/2026 18:52:33" (si le nom ne correspond pas, on affiche le nom brut)
private fun prettyName(file: File): String = try {
    val parsed = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).parse(file.nameWithoutExtension)
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(parsed!!)
} catch (e: Exception) {
    file.name
}

@Composable
private fun RideSelectionPage(
    title: String,
    actionLabel: String,
    files: List<File>,
    onAction: (List<File>) -> Unit,
    onBack: () -> Unit
) {
    // La sélection est réinitialisée quand la liste de fichiers change (ex. après suppression)
    var selected by remember(files) { mutableStateOf(setOf<String>()) }
    val allSelected = files.isNotEmpty() && selected.size == files.size

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (files.isEmpty()) {
            Text("Aucune sortie enregistrée.")
        } else {
            // "Tout cocher"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = {
                        selected = if (it) files.map { f -> f.name }.toSet() else emptySet()
                    }
                )
                Text("Tout sélectionner")
            }
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files, key = { it.name }) { file ->
                val checked = file.name in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) selected - file.name else selected + file.name
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selected = if (it) selected + file.name else selected - file.name
                        }
                    )
                    Column {
                        Text(prettyName(file))
                        Text(
                            "${file.length() / 1024} Ko",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Retour") }
            Button(
                onClick = { onAction(files.filter { it.name in selected }) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("$actionLabel (${selected.size})")
            }
        }
    }
}

private data class RouteProfile(val label: String, val file: String)

private val ROUTE_PROFILES = listOf(
    RouteProfile("Vélo de route", "fastbike"),
    RouteProfile("Personnalisé", CustomProfile.FILE),
    RouteProfile("Vélo couché", "vm-forum-liegerad-schnell"),
    RouteProfile("Vélomobile", "vm-forum-velomobil-schnell"),
    RouteProfile("Gravel", "gravel"),
    RouteProfile("VTT", "MTB"),
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
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) {
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

@Composable
private fun SelectButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, maxLines = 1) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, maxLines = 1) }
    }
}

@Composable
private fun PlanRoutePage(
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

        // Tronçons déjà validés : bleu
        legs.forEach { pts ->
            dynamicOverlays += Polyline().apply {
                setPoints(pts)
                outlinePaint.color = android.graphics.Color.rgb(30, 100, 255)
                outlinePaint.strokeWidth = 12f
            }
        }
        // Variantes en cours : grises, la sélectionnée en orange
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

        // Le départ n'est modifiable que tant qu'aucun tronçon n'est validé
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

        if (legs.isNotEmpty()) Text("Parcours validé : %.1f km (%d tronçon(s))".format(legsKm, legs.size))
        if (status.isNotEmpty()) Text(status)

        if (variants.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                variants.indices.forEach { i ->
                    SelectButton("Variante ${i + 1} · ${"%.1f".format(lengths[i])} km", i == selected) {
                        selected = i
                    }
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

                        // Les réglages de dénivelé sont inactifs si le dénivelé est désactivé
                        val enabled = p.group != CustomProfile.G_ELEVATION ||
                                p.key == "consider_elevation" || elevationOn

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