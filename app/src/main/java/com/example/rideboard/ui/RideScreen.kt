package com.example.rideboard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rideboard.RideViewModel
import com.example.rideboard.config.AppConfig
import com.example.rideboard.service.LocationService
import com.example.rideboard.utils.FitExporter
import com.example.rideboard.utils.ScreenValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri

// pour la maps
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.rideboard.R
import androidx.core.content.ContextCompat
import kotlin.math.asin
import kotlin.math.atan2

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity


@Composable
fun RideScreen(
    rideViewModel: RideViewModel = viewModel()
) {
    val screenValues by rideViewModel.screenValues
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isntOver by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(AppConfig.isRecording) }
    var isToggleBlocked by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showStravaUpload by remember { mutableStateOf(false) }

    if (isExporting) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.Green)
                Spacer(Modifier.height(16.dp))
                Text("Génération du fichier .fit...", color = Color.White)
            }
        }
        return
    }

    if (showStravaUpload) {
        //LaunchedEffect(Unit) { FitExporter.exportInGpx(context, File(context.filesDir, "ride.tsv")) }
        StravaUploadScreen(
            onContinueRide = {
                showStravaUpload = false
                isntOver = true // relance le tracking GPS si besoin
            },
            onExportElsewhere = {
                val fitFile = File(context.getExternalFilesDir(null), "ride.fit")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    fitFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export .fit"))
            },
            onDone = {
                // L'utilisateur confirme manuellement, ou upload déjà détecté automatiquement
               // (context as? Activity)?.finish()
                (File(context.filesDir, "gps_debug.txt")).writeText("")
                (File(context.filesDir, "ride.tsv")).writeText("")
                (File(context.filesDir, "ride.fit")).delete()
                (File(context.filesDir, "ride.gpx")).delete()
                (File(context.filesDir, "ride.tcx")).delete()
                context.stopService(Intent(context, LocationService::class.java))
                (context as? Activity)?.finish()
            }
        )
        return
    }


    LaunchedEffect(Unit) {
        snapshotFlow { AppConfig.isRecording }
            .collect { isRecording = it }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            RideBottomBar(
                isntOver = isntOver,
                isToggleBlocked = isToggleBlocked,
                isRecording = isRecording,
                onToggleGps = {
                    if (isToggleBlocked) {} else {
                        val intent = Intent(context, LocationService::class.java)
                        isntOver = !isntOver

                        if (!isntOver) {
                            isExporting = true
                            scope.launch(Dispatchers.IO) {
                                FitExporter.export(
                                    context,
                                    File(context.filesDir, "ride.tsv")
                                )
                                isExporting = false
                                showStravaUpload = true
                            }

                            // la suite c'était avant le test d'export vers strava
                            //FitExporter.export(context, File(context.filesDir, "ride.tsv"))
                            //FitExporter.exportInGpx(context, File(context.filesDir, "ride.tsv"))
                            //FitExporter.exportInTcx(context, File(context.filesDir, "ride.tsv"))
                            //context.stopService(intent)
                        } else
                            context.startForegroundService(intent)
                    }
                },
                onToggleBlocked = {
                    isToggleBlocked = !isToggleBlocked
                   // AppConfig.isToggleBlocked = !AppConfig.isToggleBlocked
                },
                onToggleRecording = {
                    if (!isToggleBlocked) {
                        AppConfig.isRecording = !AppConfig.isRecording
                        val action =
                            if (AppConfig.isRecording) LocationService.ACTION_START_UPDATES else LocationService.ACTION_STOP_UPDATES
                        val serviceIntent = Intent(context, LocationService::class.java).apply {
                            this.action = action
                        }
                        // On envoie l'action au service déjà lancé
                        context.startService(serviceIntent)
                    }
                }
            )
        }
    ) { padding ->

        RideContent(
            modifier = Modifier.padding(padding),
            screenValues = screenValues,
            isToggleBlocked = isToggleBlocked
        )
    }
}

@Composable
fun RideContent(
    modifier: Modifier,
    screenValues: ScreenValues,
    isToggleBlocked: Boolean
) {
    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    var zoomedCard by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000.milliseconds)
        }
    }
    val configuration = LocalConfiguration.current
    //val primFont = android.view.WindowMetrics.getBounds
    val primaryFont = (configuration.screenHeightDp/25).sp
    val secondaryFont = (configuration.screenHeightDp/40).sp
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        //-------------------------------------------------------
        // Zone carte ou ResetView
        //-------------------------------------------------------

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(1.dp)
                .border(1.dp, Color.DarkGray)
        ) {
            if (zoomedCard == null) {
                MapScreen(
                    latitude = screenValues.latitude ?: 0.0,
                    longitude = screenValues.longitude ?: 0.0,
                    direction = screenValues.direction ?: (3.1416 / 2.0),
                    isToggleBlocked = isToggleBlocked,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else if (zoomedCard == "Speed")
            {
                SpecificView(
                    cardName = zoomedCard!!,
                    screenValues = screenValues,
                    currentTime = currentTime,
                    numberOfValues = 3,
                    resetBoutons = listOf(0,2,1,0,0,0)
                )
            }
            else if (zoomedCard == "DurationDistance" || zoomedCard == "Elevation")
            {
                SpecificView(
                    cardName = zoomedCard!!,
                    screenValues = screenValues,
                    currentTime = currentTime,
                    numberOfValues = 2,
                    resetBoutons = listOf(2,2,0,0,0,0)
                )
            }
            else if (zoomedCard == "Altitude" || zoomedCard == "Slope")
            {
                SpecificView(
                    cardName = zoomedCard!!,
                    screenValues = screenValues,
                    currentTime = currentTime,
                    numberOfValues = 3,
                    resetBoutons = listOf(0,1,1,0,0,0)
                )
            }
            else if (zoomedCard == "VerticalSpeed")
            {
                VerticalSpeedView(
                    cardName = zoomedCard!!,
                    screenValues = screenValues,
                    currentTime = currentTime,
                    numberOfValues = 7,
                    resetBoutons = listOf(0,1,1,0,0,0,0,0,0)
                )
            }
            else{
                SpecificView(
                    cardName = zoomedCard!!,
                    screenValues = screenValues,
                    currentTime = currentTime,
                    numberOfValues = 3,
                    resetBoutons = listOf(0,0,0,0,0,0,0)
                )
            }
        }

        //-------------------------------------------------------
        // 8 rectangles
        //-------------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f)
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Vitesse",
                    primaryValues = listOf("%.1f km/h".format(screenValues.speed * 3.6)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("Moy %.1f".format((screenValues.averageSpeed?:0.0) * 3.6),
                    "Max %.1f".format(screenValues.maxSpeed * 3.6)),
                    onClick = { zoomedCard = "Speed" }
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                   // title = "Date et Heure",
                    primaryValues = listOf(formatTime(currentTime)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf(formatDate(currentTime)),
                    onClick = { zoomedCard = "Time" }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Durée / Distance",
                    primaryValues = listOf(formatDuration(screenValues.durationSeconds.toInt())),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("%.2f km".format(screenValues.distance / 1000.0)),
                    onClick = { zoomedCard = "DurationDistance" }
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Altitude",
                    primaryValues = listOf("%.1f m".format(screenValues.altitude)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("Min %.1f".format(screenValues.minAltitude),
                    "Max %.1f".format(screenValues.maxAltitude)),
                    onClick = { zoomedCard = "Altitude" }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Pente",
                    primaryValues = listOf("%.1f ".format(screenValues.slope) + " %  " + screenValues.screenValueString),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("Min %.1f".format(screenValues.minSlope),
                    "Max %.1f".format(screenValues.maxSlope)),
                    onClick = { zoomedCard = "Slope" }
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "D+",
                    primaryValues = listOf("%.1f m".format(screenValues.elevationGain)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("dénivelé cumulé"),
                    onClick = { zoomedCard = "Elevation" }
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Direction",
                    primaryValues = listOf(screenValues.directionString),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("direction"),
                    onClick = { 
                        if (zoomedCard != null) zoomedCard = null 
                        else zoomedCard = "Direction" 
                    }
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "vertical speed+",
                    primaryValues = listOf("↑: %.2f m/s".format(screenValues.verticalSpeed)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    secondaryValues = listOf("%.2f ".format(screenValues.minVerticalSpeed), " %.2f".format(screenValues.maxVerticalSpeed)),
                    onClick = { zoomedCard = "VerticalSpeed" }
                    )
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier,
    primaryValues: List<String>,
    primaryFont: TextUnit,
    secondaryFont: TextUnit,
    secondaryValues: List<String>,
    onClick: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .padding(0.dp)
            .fillMaxHeight(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.DarkGray)
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
        ) {

            // ---------------- PRIMARY ----------------
            BoxWithConstraints {
                val widthPerValuePx = this.constraints.maxWidth.toFloat() / max(1, primaryValues.size)

                val font = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = primaryValues,
                    availableWidthPx = widthPerValuePx,
                    maxFontSize = primaryFont,
                    minFontSize = secondaryFont
                )

                Row(Modifier.fillMaxWidth()) {

                    primaryValues.forEach {

                        Text(
                            text = it,
                            fontSize = font,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }

            // ---------------- SECONDARY ----------------
            BoxWithConstraints {
                val widthPerValuePx = this.constraints.maxWidth.toFloat() / max(1, secondaryValues.size)

                val font = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = secondaryValues,
                    availableWidthPx = widthPerValuePx,
                    maxFontSize = secondaryFont,
                    minFontSize = 6.sp
                )

                Row(Modifier.fillMaxWidth()) {

                    secondaryValues.forEach {

                        Text(
                            text = it,
                            fontSize = font,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideBottomBar(
    isntOver: Boolean,
    isToggleBlocked: Boolean,
    isRecording: Boolean,
    onToggleGps: () -> Unit,
    onToggleBlocked: () -> Unit,
    onToggleRecording: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .height(34.dp)
    ) {
        val buttonWidth = maxWidth * 0.25f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleGps,
                modifier = Modifier.width(buttonWidth),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = if (isntOver) "Finir" else "Démarrer GPS",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onToggleBlocked,
                modifier = Modifier.size(20.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isToggleBlocked) Color.Red else Color.Green
                )
            ) { }

            Button(
                onClick = onToggleRecording,
                modifier = Modifier.width(buttonWidth),
                contentPadding = PaddingValues(horizontal = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else Color.Green
                )
            ) {
                Text(
                    text = if (isRecording) "Pause" else "Enregistrer",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("EEE dd/MM/yy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
fun formatDuration(seconds: Int): String {
    val duration = Duration.ofSeconds(seconds.toLong())

    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val secs = duration.seconds % 60

    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, secs)
    else
        "%02d:%02d".format(minutes, secs)
}

fun formatTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun computeFontSize(
    textMeasurer: TextMeasurer,
    values: List<String>,
    availableWidthPx: Float,
    maxFontSize: TextUnit,
    minFontSize: TextUnit
): TextUnit {

    var font = maxFontSize

    while (font.value >= minFontSize.value) {

        val ok = values.all { value ->

            val result = textMeasurer.measure(
                text = value,
                style = TextStyle(fontSize = font)
            )

            result.size.width <= availableWidthPx
        }

        if (ok) return font

        font = (font.value - 2).sp // plus stable que -1.sp
    }

    return minFontSize
}

@Composable
fun AutoSizeRow(
    values: List<String>,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    maxFontSize: TextUnit = 22.sp,
    minFontSize: TextUnit = 8.sp,
    fontWeight: FontWeight? = null
) {
    var fontSize by remember(values) { mutableStateOf(maxFontSize) }
    var overflow by remember(values) { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = when (values.size) {
            1 -> Arrangement.Center
            2 -> Arrangement.SpaceBetween
            else -> Arrangement.SpaceEvenly
        }
    ) {

        values.forEach { value ->

            Text(
                text = value,
                modifier = Modifier.weight(1f),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,

                onTextLayout = { result ->
                    if (result.hasVisualOverflow)
                        overflow = true
                }
            )
        }
    }

    LaunchedEffect(overflow) {
        if (overflow && fontSize > minFontSize) {
            overflow = false
            fontSize = (fontSize.value - 1).sp
        }
    }
}

@Composable
fun MapScreen(
    latitude: Double,
    longitude: Double,
    direction: Double, // -pi..pi, 0=est, pi/2=nord
    isToggleBlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.OpenTopo)  //MAPNIK
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)

                        // mapOrientation reste à sa valeur par défaut (0 = nord en haut)
                    }
                }

    LaunchedEffect(isToggleBlocked) {
        mapView.setMultiTouchControls(!isToggleBlocked)
    }
    val positionMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_position_arrow)
            setFlat(false) // reste orientée vers le haut de l'écran, ne suit PAS la rotation de la carte
            setInfoWindow(null) // pas de popup au clic
        }
    }

    DisposableEffect(mapView, positionMarker) {
        mapView.overlays.add(positionMarker)
        onDispose { mapView.overlays.remove(positionMarker) }
    }

    var autoRecenter by remember { mutableStateOf(true) }

    LaunchedEffect(latitude, longitude, direction, autoRecenter) {
        val compassBearing = mathAngleToCompassBearing(direction)

        positionMarker.position = GeoPoint(latitude, longitude)
        positionMarker.rotation = compassBearing.toFloat() // la flèche pointe selon le cap réel
        mapView.invalidate()

        if (autoRecenter) {
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    LaunchedEffect(latitude, longitude, direction, autoRecenter) {
        // val compassBearing = mathAngleToCompassBearing(direction)
        // mapView.mapOrientation = compassBearing.toFloat() // la carte tourne selon le cap réel

        positionMarker.position = GeoPoint(latitude, longitude)
        mapView.invalidate() // force le redessin

        if (autoRecenter) {
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    DisposableEffect(mapView, positionMarker) {
        mapView.overlays.add(positionMarker)
        onDispose { mapView.overlays.remove(positionMarker) }
    }


    DisposableEffect(mapView, isToggleBlocked) {
        val touchListener = View.OnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (isToggleBlocked) return true // bloque tout geste simple (drag)
            if (event.action == MotionEvent.ACTION_MOVE) {
                autoRecenter = false
            }
            return false
        })
        mapView.setOnTouchListener(touchListener)
        onDispose { mapView.setOnTouchListener(null) }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            IconButton(onClick = { if (!isToggleBlocked) mapView.controller.zoomIn() }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }
            IconButton(onClick = { if (!isToggleBlocked) mapView.controller.zoomOut() }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }
        }

        if (!autoRecenter) {
            IconButton(
                onClick = { if (!isToggleBlocked) autoRecenter = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recentrer")
            }
        }

        // Flèches de déplacement : la carte étant fixe (nord en haut),
        // elles redeviennent Nord/Sud/Est/Ouest, pas relatives au cap
  /*      val panStep = 30.0
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            IconButton(onClick = {
                autoRecenter = false
                val c = mapView.mapCenter
                mapView.controller.setCenter(destinationPoint(c.latitude, c.longitude, 0.0, panStep))
            }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Nord") }

            Row {
                IconButton(onClick = {
                    autoRecenter = false
                    val c = mapView.mapCenter
                    mapView.controller.setCenter(destinationPoint(c.latitude, c.longitude, 270.0, panStep))
                }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Ouest") }

                IconButton(onClick = {
                    autoRecenter = false
                    val c = mapView.mapCenter
                    mapView.controller.setCenter(destinationPoint(c.latitude, c.longitude, 90.0, panStep))
                }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Est") }
            }

            IconButton(onClick = {
                autoRecenter = false
                val c = mapView.mapCenter
                mapView.controller.setCenter(destinationPoint(c.latitude, c.longitude, 180.0, panStep))
            }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sud") }
        }*/
    }
}

//test pour expoter le fit:
fun shareFitFile(context: Context) {

    val fitFile = File(context.filesDir, "ride.fit")

    if (!fitFile.exists()) {
        return
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        fitFile
    )

    val intent = Intent(Intent.ACTION_SEND).apply {

        type = "application/octet-stream"

        putExtra(Intent.EXTRA_STREAM, uri)

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Partager l'activité")
    )
}

fun mathAngleToCompassBearing(angleRad: Double): Float {
    val angleDeg = Math.toDegrees(angleRad)
    val bearing =  angleDeg - 90.0
    return ((bearing % 360.0 + 360.0) % 360.0).toFloat()
}

// --- Calcul d'un point de destination à partir d'un cap et d'une distance (formule sphérique) ---
fun destinationPoint(lat: Double, lon: Double, bearingDeg: Double, distanceMeters: Double): GeoPoint {
    val earthRadius = 6371000.0
    val bearingRad = Math.toRadians(bearingDeg)
    val lat1 = Math.toRadians(lat)
    val lon1 = Math.toRadians(lon)

    val lat2 = asin(
        sin(lat1) * cos(distanceMeters / earthRadius) +
                cos(lat1) * sin(distanceMeters / earthRadius) * cos(bearingRad)
    )
    val lon2 = lon1 + atan2(
        sin(bearingRad) * sin(distanceMeters / earthRadius) * cos(lat1),
        cos(distanceMeters / earthRadius) - sin(lat1) * sin(lat2)
    )

    return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
}

@Composable
fun SpecificView(
    cardName: String,
    screenValues: ScreenValues,
    currentTime: Long,
    numberOfValues: Int,
    resetBoutons: List<Int>

) {
    val items = when (cardName) {
        "Speed" -> listOf(
            "Vitesse" to "%.1f km/h".format(screenValues.speed * 3.6) to "Speed",
            "Moyenne" to "%.2f km/h".format((screenValues.averageSpeed ?: 0.0) * 3.6) to "averageSpeed",
            "Maximum" to "%.1f km/h".format(screenValues.maxSpeed * 3.6) to "maxSpeed"
        )
        "Time" -> listOf(
            "Heure" to formatTime(currentTime) to "Time",
            "Date" to formatDate(currentTime) to "Date"
        )
        "DurationDistance" -> listOf(
            "Durée" to formatDuration(screenValues.durationSeconds.toInt()) to "Duration",
            "Distance" to "%.2f km".format(screenValues.distance / 1000.0) to "Distance"
        )
        "Altitude" -> listOf(
            "Altitude" to "%.1f m".format(screenValues.altitude) to "Altitude",
            "Min" to "%.1f m".format(screenValues.minAltitude) to "minAltitude",
            "Max" to "%.1f m".format(screenValues.maxAltitude) to "maxAltitude"
        )
        "Slope" -> listOf(
            "Pente" to "%.1f %%".format(screenValues.slope) to "Slope",
            "Min" to "%.1f %%".format(screenValues.minSlope) to "minSlope",
            "Max" to "%.1f %%".format(screenValues.maxSlope) to "maxSlope"
        )
        "Elevation" -> listOf(
            "D+" to "%.1f m".format(screenValues.elevationGain) to "ElevationGain"
        )
        "Direction" -> listOf(
            "Direction" to screenValues.directionString to "Direction"
        )
        "VerticalSpeed" -> listOf(
            "V. Speed" to "%.2f m/s".format(screenValues.verticalSpeed) to "VerticalSpeed",
            "Min" to "%.2f m/s".format(screenValues.minVerticalSpeed) to "minVerticalSpeed",
            "Max" to "%.2f m/s".format(screenValues.maxVerticalSpeed) to "maxVerticalSpeed",
            "Sprint" to "%.2f m/s".format(screenValues.verticalSpeed4) to "verticalSpeed4",
            "Résistance" to "%.2f m/s".format(screenValues.verticalSpeed15) to "verticalSpeed15",
            "seuil" to "%.2f m/s".format(screenValues.verticalSpeed125) to "verticalSpeed125",
            "endurance" to "%.2f m/s".format(screenValues.verticalSpeed1000) to "verticalSpeed1000"
        )
        else -> emptyList()
    }

        val textMeasurer = rememberTextMeasurer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { index, (labels, resetValue) ->
                val (label, displayValue) = labels
                val weight = if (index == 0) 2f else 1f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight) // chaque rectangle occupe une part égale de la hauteur restante sauf le 1er
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                        //.border(1.dp, Color.Gray)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Column (
                        modifier = Modifier.width(25.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (resetBoutons[index] > 1)
                            Button(
                                onClick = {
                                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = resetValue + "Renewed"
                                },
                                modifier = Modifier.size(20.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0CA33))
                            ) { Text("T")
                            }
                    }

                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                        // Taille max bornée à la fois par la hauteur du bloc et par la largeur du texte
                        val maxLabelFont = with(LocalDensity.current) { (availableHeightPx * 0.3f).toSp() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }

                        val labelFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("  $label  "),
                            availableWidthPx = availableWidthPx,
                            maxFontSize = maxLabelFont,
                            minFontSize = 6.sp
                        )
                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("  $displayValue  "),
                            availableWidthPx = availableWidthPx,
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )

                        Column  (
                            horizontalAlignment = Alignment.CenterHorizontally
                        ){
                            Text(
                                text = "$label  ",
                                color = Color.Gray,
                                fontSize = labelFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "  $displayValue  ",
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column (
                        modifier = Modifier.width(25.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (resetBoutons[index] > 0)
                            Button(
                                onClick = {
                                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = resetValue
                                },
                                modifier = Modifier.size(20.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                            ) { Text("0")
                            }
                    }


                }
            }
        }
    }

@Composable
fun VerticalSpeedView(
    cardName: String,
    screenValues: ScreenValues,
    currentTime: Long,
    numberOfValues: Int,
    resetBoutons: List<Int>

) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                // Taille max bornée à la fois par la hauteur du bloc et par la largeur du texte
                val maxLabelFont =
                    with(LocalDensity.current) { (availableHeightPx * 0.3f).toSp() }
                val maxValueFont =
                    with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }

                val labelFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf("  vitesse verticale  "),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxLabelFont,
                    minFontSize = 6.sp
                ) * 1.2f
                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf("  %.2f m/s  ".format(screenValues.verticalSpeed)),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                ) * 0.85f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "vitesse verticale",
                        color = Color.Gray,
                        fontSize = labelFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "  %.2f m/s  ".format(screenValues.verticalSpeed),
                        color = Color.Gray,
                        fontSize = valueFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                // Taille max bornée à la fois par la hauteur du bloc et par la largeur du texte
                val maxValueFont =
                    with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf("  %.2f m/s  ".format(screenValues.verticalSpeed4)),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "  %.2f m/s  ".format(screenValues.verticalSpeed4),
                        color = Color.White,
                        fontSize = valueFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(25.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = "minVerticalSpeed"
                    },
                    modifier = Modifier.size(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) { Text("0")
                }
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //vitesse min
                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    val maxValueFont =
                        with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(
                            "%.2f   ".format(screenValues.minVerticalSpeed) + "x: %.2f ".format(
                                screenValues.maxVerticalSpeed
                            )
                        ),
                        availableWidthPx = availableWidthPx,
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "  %.2f ".format(screenValues.minVerticalSpeed),
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "  %.2f ".format(screenValues.maxVerticalSpeed),
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.width(25.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = "maxVerticalSpeed"
                    },
                    modifier = Modifier.size(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) { Text("0")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    val maxValueFont =
                        with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(
                            "Résistance: %.2f".format(screenValues.verticalSpeed15) + "          / %.2f ".format(
                                screenValues.maxVerticalSpeed15
                            )
                        ),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Résistance:    %.2f".format(screenValues.verticalSpeed15),
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "        / %.2f     ".format(screenValues.maxVerticalSpeed15),
                            color = Color(0xFFFFCF00),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }
                Column(
                    modifier = Modifier.width(25.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = "maxVerticalSpeed15"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) { Text("0")
                    }
                }
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    val maxValueFont =
                        with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(
                            "Résistance: %.2f".format(screenValues.verticalSpeed15) + "          / %.2f ".format(
                                screenValues.maxVerticalSpeed15
                            )
                        ),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Seuil:               %.2f ".format(screenValues.verticalSpeed125),
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "       / %.2f ".format(screenValues.maxVerticalSpeed125),
                            color = Color(0xFFFFCF00),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.width(25.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = "maxVerticalSpeed125"
                    },
                    modifier = Modifier.size(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) { Text("0")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                //.border(1.dp, Color.Gray)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    val maxValueFont =
                        with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(
                            "Résistance: %.2f".format(screenValues.verticalSpeed15) + "          / %.2f ".format(
                                screenValues.maxVerticalSpeed15
                            )
                        ),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Endurance:     %.2f ".format(screenValues.verticalSpeed1000),
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "       / %.2f  ".format(screenValues.maxVerticalSpeed1000),
                            color = Color(0xFFFFBF00),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.width(25.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset = "maxVerticalSpeed1000"
                    },
                    modifier = Modifier.size(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) { Text("0")
                }
            }
        }

    }
        }





//*/