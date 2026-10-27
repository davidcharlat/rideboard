package com.example.rideboard.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.rideboard.brouter.SegmentDownloader
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class SettingsPage { MENU, ZONES, PREFERENCES, RINGING, EXPORT, DELETE, NAV_FILES }

@Composable
fun SetValuesScreen(
    onDone: () -> Unit,
    rideViewModel: RideViewModel? = null,
    currentPosition: GeoPoint? = null
) {
    var page by remember { mutableStateOf(SettingsPage.MENU) }

    BackHandler {
        page = when (page) {
            SettingsPage.MENU -> { onDone(); SettingsPage.MENU }
            SettingsPage.ZONES, SettingsPage.PREFERENCES -> SettingsPage.MENU
            SettingsPage.RINGING, SettingsPage.EXPORT, SettingsPage.DELETE, SettingsPage.NAV_FILES -> SettingsPage.PREFERENCES
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (page) {
            SettingsPage.MENU -> MenuPage(
                onZones = { page = SettingsPage.ZONES },
                onPreferences = { page = SettingsPage.PREFERENCES },
                onBack = onDone
            )
            SettingsPage.ZONES -> ZonesPage(onBack = { page = SettingsPage.MENU })
            SettingsPage.PREFERENCES -> PreferencesPage(
                onRinging = { page = SettingsPage.RINGING },
                onExport = { page = SettingsPage.EXPORT },
                onDelete = { page = SettingsPage.DELETE },
                onNavFiles = { page = SettingsPage.NAV_FILES },
                onBack = { page = SettingsPage.MENU }
            )
            SettingsPage.RINGING -> RingingPage(onBack = { page = SettingsPage.PREFERENCES })
            SettingsPage.EXPORT -> ExportRidePage(onBack = { page = SettingsPage.PREFERENCES })
            SettingsPage.DELETE -> DeleteRidePage(onBack = { page = SettingsPage.PREFERENCES })
            SettingsPage.NAV_FILES -> NavFilesPage(
                currentPosition = currentPosition,
                onBack = { page = SettingsPage.PREFERENCES }
            )
        }
    }
}

// ---------------------------------------------------------------
// Page 1 : menu principal
// ---------------------------------------------------------------
@Composable
private fun MenuPage(onZones: () -> Unit, onPreferences: () -> Unit, onBack: () -> Unit) {
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
    onNavFiles: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sortie") }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Supprimer des sorties") }
        Button(onClick = onRinging, modifier = Modifier.fillMaxWidth()) { Text("Sonnerie") }
        Button(onClick = onNavFiles, modifier = Modifier.fillMaxWidth()) { Text("Actualiser les fichiers de navigation") }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (ringing) "Oui" else "Non")
            Switch(
                checked = ringing,
                onCheckedChange = {
                    ringing = it
                    AppSettings.isRinging = it
                    AppSettings.save(context)
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
                    files = listRides(context)
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
        ?.sortedByDescending { it.name }
        ?: emptyList()

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

private class TileGridOverlay : Overlay() {
    var downloaded: Set<String> = emptySet()
    var selected: Set<String> = emptySet()

    private val gridLine = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
        color = android.graphics.Color.argb(170, 70, 70, 70)
    }
    private val downloadedFill = Paint().apply {
        style = Paint.Style.FILL; color = android.graphics.Color.argb(110, 90, 90, 90)
    }
    private val selectedFill = Paint().apply {
        style = Paint.Style.FILL; color = android.graphics.Color.argb(120, 30, 100, 255)
    }
    private val selectedLine = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true
        color = android.graphics.Color.rgb(30, 100, 255)
    }
    private val label = Paint().apply {
        color = android.graphics.Color.BLACK; textSize = 32f
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    private val topLeft = Point()
    private val bottomRight = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val box = mapView.boundingBox
        val proj = mapView.projection

        val latFrom = (floor(max(box.latSouth, -85.0) / 5.0) * 5).toInt()
        val latTo = (floor(min(box.latNorth, 84.9) / 5.0) * 5).toInt()
        val lonFrom = (floor(max(box.lonWest, -180.0) / 5.0) * 5).toInt()
        val lonTo = (floor(min(box.lonEast, 179.9) / 5.0) * 5).toInt()

        var latBase = latFrom
        while (latBase <= latTo) {
            var lonBase = lonFrom
            while (lonBase <= lonTo) {
                val name = SegmentDownloader.tileName(latBase, lonBase)
                proj.toPixels(GeoPoint(latBase + 5.0, lonBase.toDouble()), topLeft)
                proj.toPixels(GeoPoint(latBase.toDouble(), lonBase + 5.0), bottomRight)
                val l = topLeft.x.toFloat(); val t = topLeft.y.toFloat()
                val r = bottomRight.x.toFloat(); val b = bottomRight.y.toFloat()

                if (name in selected) canvas.drawRect(l, t, r, b, selectedFill)
                else if (name in downloaded) canvas.drawRect(l, t, r, b, downloadedFill)
                canvas.drawRect(l, t, r, b, if (name in selected) selectedLine else gridLine)

                if (r - l > 120f) {
                    canvas.drawText(name.removeSuffix(".rd5"), (l + r) / 2f, (t + b) / 2f, label)
                }
                lonBase += 5
            }
            latBase += 5
        }
    }

    /** Nom du fichier correspondant à un point de la carte, ou null hors limites. */
    fun tileAt(p: GeoPoint): String? {
        if (p.latitude < -85.0 || p.latitude >= 85.0 || p.longitude < -180.0 || p.longitude >= 180.0) return null
        val latBase = (floor(p.latitude / 5.0) * 5).toInt()
        val lonBase = (floor(p.longitude / 5.0) * 5).toInt()
        return SegmentDownloader.tileName(latBase, lonBase)
    }
}

private fun listDownloadedTiles(context: Context): Set<String> =
    BRouterFiles.segmentsDir(context)
        .listFiles { f -> f.extension == "rd5" }
        ?.map { it.name }
        ?.toSet()
        ?: emptySet()

@Composable
private fun NavFilesPage(currentPosition: GeoPoint?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloaded by remember { mutableStateOf(listDownloadedTiles(context)) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }

    // ---------------- La carte ----------------
    val overlay = remember { TileGridOverlay() }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.OpenTopo)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(6.0)
            controller.setCenter(currentPosition ?: GeoPoint(46.6, 2.4))
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    DisposableEffect(mapView) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (downloading) return true
                val name = overlay.tileAt(p) ?: return true
                selected = if (name in selected) selected - name else selected + name
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        val events = MapEventsOverlay(receiver)
        mapView.overlays.add(events)
        mapView.overlays.add(overlay)
        onDispose {
            mapView.overlays.remove(events)
            mapView.overlays.remove(overlay)
        }
    }

    LaunchedEffect(downloaded, selected) {
        overlay.downloaded = downloaded
        overlay.selected = selected
        mapView.invalidate()
    }

    // ---------------- Le téléchargement ----------------
    fun startDownload() {
        val names = selected.sorted()
        downloading = true
        status = ""
        job = scope.launch {
            val failed = mutableListOf<String>()
            var cancelled = false
            try {
                names.forEachIndexed { i, name ->
                    var lastUpdate = 0L
                    val ok = SegmentDownloader.download(context, name, force = true) { done, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250) {
                            lastUpdate = now
                            progress = if (total > 0) done.toFloat() / total else 0f
                            status = "Téléchargement de ${name.removeSuffix(".rd5")} (${i + 1}/${names.size}) — " +
                                    Formatter.formatShortFileSize(context, done) +
                                    (if (total > 0) " / " + Formatter.formatShortFileSize(context, total) else "")
                        }
                    }
                    if (!ok) failed += name
                    downloaded = listDownloadedTiles(context)
                }
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            } finally {
                downloading = false
                progress = 0f
                selected = selected - names.filter { it !in failed }.toSet()
                status = when {
                    cancelled -> "Téléchargement annulé."
                    failed.isEmpty() -> "${names.size} fichier(s) à jour."
                    else -> "Échec : ${failed.joinToString { it.removeSuffix(".rd5") }}. " +
                            "Le fichier n'existe peut-être pas sur le serveur (zone en mer), ou la connexion a été perdue."
                }
            }
        }
    }

    // ---------------- La mise en page ----------------
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Fichiers de navigation", style = MaterialTheme.typography.titleMedium)

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }

        Text(
            "Gris : déjà téléchargé · Bleu : sélectionné. Touche une case pour la (dé)sélectionner.",
            style = MaterialTheme.typography.bodySmall
        )

        if (downloading) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        if (status.isNotEmpty()) Text(status)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (downloading) {
                OutlinedButton(onClick = { job?.cancel() }, modifier = Modifier.weight(1f)) {
                    Text("Annuler le téléchargement", maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Retour", maxLines = 1) }
                Button(
                    onClick = { confirm = true },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Télécharger (${selected.size})", maxLines = 1) }
            }
        }
    }

    if (confirm) {
        val free = Formatter.formatShortFileSize(context, context.filesDir.usableSpace)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Télécharger ${selected.size} fichier(s) ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selected.sorted().joinToString(", ") { it.removeSuffix(".rd5") })
                    Text("Les fichiers déjà présents seront remplacés. Chaque fichier peut peser plus de 100 Mo : le Wi-Fi est recommandé.")
                    Text("Espace libre : $free")
                }
            },
            confirmButton = {
                TextButton(onClick = { confirm = false; startDownload() }) { Text("Télécharger") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Annuler") }
            }
        )
    }
}