package com.example.rideboard.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rideboard.RideViewModel
import com.example.rideboard.config.AppConfig
import com.example.rideboard.service.LocationService
import com.example.rideboard.utils.FitExporter
import com.example.rideboard.utils.ScreenValues
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RideScreen(
    rideViewModel: RideViewModel = viewModel()
) {
    val screenValues by rideViewModel.screenValues
    val context = LocalContext.current

    var isntOver by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(AppConfig.isRecording) }

    LaunchedEffect(Unit) {
        snapshotFlow { AppConfig.isRecording }
            .collect { isRecording = it }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            RideBottomBar(
                isntOver = isntOver,
                isRecording = isRecording,
                onToggleGps = {
                    val intent = Intent(context, LocationService::class.java)
                    isntOver = !isntOver
                    if (!isntOver) {
                        FitExporter.export(context, File(context.filesDir, "ride.tsv"))
                        FitExporter.exportInGpx(context, File(context.filesDir, "ride.tsv"))
                        FitExporter.exportInTcx(context, File(context.filesDir, "ride.tsv"))
                        //context.stopService(intent)
                    }
                    else
                        context.startForegroundService(intent)


                },
                onToggleRecording = {
                    AppConfig.isRecording = !AppConfig.isRecording
                }
            )
        }
    ) { padding ->

        RideContent(
            modifier = Modifier.padding(padding),
            screenValues = screenValues
        )
    }
}

@Composable
fun RideContent(
    modifier: Modifier,
    screenValues: ScreenValues
) {
    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

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
        // Zone carte (vide pour l'instant)
        //-------------------------------------------------------

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(1.dp)
                .border(1.dp, Color.DarkGray)
        )

        //-------------------------------------------------------
        // 6 rectangles
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
                    unit = "km/h",
                    secondaryValues = listOf("Moy %.1f".format((screenValues.averageSpeed?:0.0) * 3.6),
                    "Max %.1f".format(screenValues.maxSpeed * 3.6))
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                   // title = "Date et Heure",
                    primaryValues = listOf(formatTime(currentTime)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    unit = "",
                    secondaryValues = listOf(formatDate(currentTime))

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
                    unit = "",
                    secondaryValues = listOf("%.2f km".format(screenValues.distance / 1000.0))

                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "Altitude",
                    primaryValues = listOf("%.1f m".format(screenValues.altitude)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    unit = "m",
                    secondaryValues = listOf("Min %.1f".format(screenValues.minAltitude),
                    "Max %.1f".format(screenValues.maxAltitude))
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
                    unit = "%",
                    secondaryValues = listOf("Min %.1f".format(screenValues.minSlope),
                    "Max %.1f".format(screenValues.maxSlope))
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "D+",
                    primaryValues = listOf("%.1f m".format(screenValues.elevationGain)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    unit = "m & m/s",
                    secondaryValues = listOf("dénivelé cumulé"),

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
                    unit = "%",
                    secondaryValues = listOf("direction")
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    //title = "vertical speed+",
                    primaryValues = listOf("↑: %.2f m/s".format(screenValues.verticalSpeed)),
                    primaryFont = primaryFont,
                    secondaryFont = secondaryFont,
                    unit = "m & m/s",
                    secondaryValues = listOf("%.2f ".format(screenValues.minVerticalSpeed), " %.2f".format(screenValues.maxVerticalSpeed)),

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
    unit: String,
    secondaryValues: List<String>
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .padding(2.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.DarkGray)
                .padding(2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ---------------- PRIMARY ----------------
            BoxWithConstraints {

                val widthPerValuePx = with(density) {
                    (maxWidth / max(1, primaryValues.size)).toPx()
                }

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

                val widthPerValuePx = with(density) {
                    (maxWidth / max(1,secondaryValues.size)).toPx()
                }

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
    isRecording: Boolean,
    onToggleGps: () -> Unit,
    onToggleRecording: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        Button(
            onClick = onToggleGps
        ) {
            Text(if (isntOver) "Finir" else "Démarrer GPS")
        }

        Button(
            onClick = onToggleRecording,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (isRecording) Color.Red else Color.Green
            )
        ) {
            Text(if (isRecording) "Pause" else "Enregistrer")
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


/*package com.example.rideboard.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rideboard.RideViewModel
import com.example.rideboard.config.AppConfig
import com.example.rideboard.service.LocationService
import com.example.rideboard.utils.ScreenValues

@Composable
fun RideScreen(
    rideViewModel: RideViewModel = viewModel()
) {
    val screenValues by rideViewModel.screenValues
    val context = LocalContext.current

    var isGpsActive by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(AppConfig.isRecording) }

    // Synchronisation avec AppConfig
    LaunchedEffect(Unit) {
        snapshotFlow { AppConfig.isRecording }
            .collect { isRecording = it }
    }

    Scaffold(
        bottomBar = {
            RideBottomBar(
                isGpsActive = isGpsActive,
                isRecording = isRecording,
                onToggleGps = {
                    val intent = Intent(context, LocationService::class.java)
                    if (isGpsActive) context.stopService(intent)
                    else context.startForegroundService(intent)
                    isGpsActive = !isGpsActive
                },
                onToggleRecording = {
                    AppConfig.isRecording = !AppConfig.isRecording
                }
            )
        }
    ) { padding ->

        RideContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            screenValues = screenValues
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    mainValue: String,
    unit: String,
    secondaryValues: List<String>,
)
{
    @androidx.compose.runtime.Composable {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxHeight()
        ) {

            Column(
                modifier = Modifier.padding(8.dp)
            ) {

                Text(
                    text = title,
                    fontSize = 14.sp
                )

                Text(
                    text = "$mainValue $unit",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
for (secondaryValue in secondaryValues) {
                Text(
                    text = secondaryValue,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun RideContent(
    modifier: Modifier = Modifier,
    screenValues: ScreenValues
) {
    Row(Modifier) {

        MetricCard(
            title = "Speed",
            mainValue = "%.1f".format(screenValues.speed),
            unit = "km/h",
            secondaryValues = listOf("Avg %.1f".format(screenValues.averageSpeed),
            "Max %.1f".format(screenValues.maxSpeed))
        )

        MetricCard(
            title = "vert. Spd",
            mainValue = "%.1f".format(screenValues.displayedVerticalSpeed),
            unit = "",
            secondaryValues = listOf("Avg %.1f".format(screenValues.verticalSpeed),
                "Max %.1f".format(screenValues.maxSpeed))
        )
    }










    Column(
        modifier = modifier
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        RideValue("Double1", (screenValues.screenValueDouble1).toString())
        RideValue("Double2", "%.6f".format(screenValues.screenValueDouble2))
        RideValue("Double3", "%.6f".format(screenValues.screenValueDouble3))
        RideValue("Boolean", screenValues.screenValueBoolean.toString())
        RideValue("Long",screenValues.screenValueLong.toString())
        RideValue("Alt Mnt", "%.5f m".format(screenValues.altMnt))
        RideValue("Alt Mns", "%.5f m".format(screenValues.altMns))
        RideValue("Alt GPS", "%.5f m".format(screenValues.altGps))
        RideValue("Altitude", "%.2f m".format(screenValues.altitude))
        RideValue("Stg",screenValues.screenValueString?:"no value")
        RideValue("Stg",screenValues.screenValueString2?:"no value")
        RideValue("Stg",screenValues.screenValueString3?:"no value")
        RideValue("Stg",screenValues.screenValueString4?:"no value")
        RideValue("Durée alt non Mnt", screenValues.durationAltitudeNotMnt.toString())
        RideValue("Int", screenValues.screenValueInt.toString())
        RideValue("Latitude", "%.6f".format(screenValues.latitude))
        RideValue("Longitude", "%.6f".format(screenValues.longitude))
        RideValue("Vitesse", "%.2f km/h".format(screenValues.speed))

        RideValue("Direction", "%.2f".format(screenValues.direction))
        RideValue("Accélération", "%.2f g".format(screenValues.acceleration))
        RideValue("Vitesse verticale", "%.2f m/s".format(screenValues.displayedVerticalSpeed))
        RideValue("Precision alt GPS", "%.2f m".format(screenValues.gpsAltitudeAccuracy))
        RideValue("Source alt", screenValues.altitudeSourceMntMnsOrGps)
        RideValue("Spread alt", "%.2f m".format(screenValues.spreadAltitudeAndAltitudeGps))
        RideValue("Coef spread alt", "%.2f".format(screenValues.coefficientOfSpreadAltitudeAndAltitudeGps))



        // Plus tard :
        // - Carte
        // - Graphique
        // - Stats
    }
}

@Composable
fun RideValue(label: String, value: String) {
    Text(
        text = "$label : $value",
        fontSize = 20.sp
    )
}

@Composable
fun RideBottomBar(
    isGpsActive: Boolean,
    isRecording: Boolean,
    onToggleGps: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        Button(onClick = onToggleGps) {
            Text(if (isGpsActive) "Arrêter GPS" else "Démarrer GPS")
        }

        Button(
            onClick = onToggleRecording,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else Color.Green
            )
        ) {
            Text(if (isRecording) "Pause" else "Enregistrer")
        }
    }
}
/*@Composable
fun RideScreen(rideViewModel: RideViewModel = viewModel()) {
    val screenValues = rideViewModel.screenValues.value
    val context = LocalContext.current
    var isGpsActive by remember { mutableStateOf(true)}
    LaunchedEffect(Unit) {
        snapshotFlow { AppConfig.isRecording }.collect { globalValue ->
            isRecording = globalValue
        }
    }
    */
    /* extrait de screenValues:
    val acceleration: Double = 0.0,
    val speed: Double = 0.0,
    val verticalSpeed: Double = 0.0,
    val direction: Double = 0.0,
    val altitude: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val time: Long = 0,
    val gpsAltitudeAccuracy: Double = 0.0,
    val altitudeSourceMntMnsOrGps: String = "",
    val spreadAltitudeAndAltitudeGps: Double = 0.0,
    val coefficientOfSpreadAltitudeAndAltitudeGps: Double = 0.0,
    val durationAltitudeNotMnt: Int = 0,
     */
/*
    Column {
        Text(
            text = "Latitude : %.6f".format(screenValues.latitude),
            fontSize = 28.sp
        )
        Text(
            text = "Longitude : %.6f".format(screenValues.longitude),
            fontSize = 28.sp
        )
        Text(
            text = "Vitesse : %.2f km/h".format(screenValues.speed),
            fontSize = 28.sp
        )
        Text(
            text = "Altitude : %.2f m".format(screenValues.altitude),
            fontSize = 28.sp
        )
        Text (
            text = "Direction : %.2f".format(screenValues.direction),
            fontSize = 28.sp
        )
        Text (
            text = "Accélération : %.2f g".format(screenValues.acceleration),
            fontSize = 28.sp
        )
        Text (
            text = "Vitesse verticale : %.2f m/s".format(screenValues.verticalSpeed),
            fontSize = 28.sp
        )
        Text (
            text = "precision alt GPS : %.2f m".format(screenValues.gpsAltitudeAccuracy),
            fontSize = 28.sp
        )
        Text (
            text = "source alt : %s".format(screenValues.altitudeSourceMntMnsOrGps),
            fontSize = 28.sp
        )
        Text (
            text = "spread alt : %.2f m".format(screenValues.spreadAltitudeAndAltitudeGps),
            fontSize = 28.sp
        )
        Text (
            text = "durée alt non Mnt : ".format(screenValues.durationAltitudeNotMnt),
            fontSize = 28.sp
        )
        Text (
            text = "alt Mnt : %.2f m".format(screenValues.altMnt),
            fontSize = 28.sp
        )
        Text (
            text = "alt Mns : %.2f m".format(screenValues.altMns),
            fontSize = 28.sp
        )
        Text (
            text = "alt Gps : %.2f m".format(screenValues.altGps),
            fontSize = 28.sp
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 🔘 Bouton GPS
        Button(
            onClick = {
                val intent = Intent(context, LocationService::class.java)
                if (isGpsActive) {
                    context.stopService(intent)
                } else {
                    context.startForegroundService(intent)
                }
                isGpsActive = !isGpsActive
            }
        ) {
            Text(if (isGpsActive) "Arrêter GPS" else "Démarrer GPS")
        }

        // 🔴 Bouton Enregistrement
        Button(
            onClick = {
                AppConfig.isRecording = !AppConfig.isRecording
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (AppConfig.isRecording) Color.Red else Color.Green
            )
        ) {
            Text(if (AppConfig.isRecording) "Pause" else "Enregistrer")
        }
    }
}
*/


/* ci dessous version du 18/10/25 @Composable
fun RideScreen(rideViewModel: RideViewModel = viewModel()) {
    val currentSpeedScreen = rideViewModel.currentSpeedScreen.observeAsState(0.0).value
    val maxSpeedScreen = rideViewModel.maxSpeedScreen.observeAsState(0.0).value
    val averageSpeedScreen = rideViewModel.averageSpeedScreen.observeAsState(0.0).value
    val currentDistanceScreen = rideViewModel.currentDistanceScreen.observeAsState(0.0).value
    val currentAltitudeScreen = rideViewModel.currentAltitudeScreen.observeAsState(0.0).value
    val maxAltitudeScreen = rideViewModel.maxAltitudeScreen.observeAsState(0.0).value
    val minAltitudeScreen = rideViewModel.minAltitudeScreen.observeAsState(0.0).value
    val travelTimeScreen = rideViewModel.travelTimeScreen.observeAsState(0.0).value
    val cumulativeElevationGainScreen =
        rideViewModel.cumulativeElevationGainScreen.observeAsState(0.0).value
    val currentVerticalSpeedScreen =
        rideViewModel.currentVerticalSpeedScreen.observeAsState(0.0).value
    val maxVerticalSpeedScreen = rideViewModel.maxVerticalSpeedScreen.observeAsState(0.0).value
    val minVerticalSpeedScreen = rideViewModel.minVerticalSpeedScreen.observeAsState(0.0).value
    val currentSlopeScreen = rideViewModel.currentSlopeScreen.observeAsState(0.0).value
    val maxSlopeScreen = rideViewModel.maxSlopeScreen.observeAsState(0.0).value
    val minSlopeScreen = rideViewModel.minSlopeScreen.observeAsState(0.0).value

    val context = LocalContext.current
    var isGpsActive by remember { mutableStateOf(true)}
    LaunchedEffect(Unit) {
        snapshotFlow { AppConfig.isRecording }.collect { globalValue ->
            isRecording = globalValue
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("currentSpeed : %.2f km/h".format(currentSpeedScreen), fontSize = 16.sp)
            Text("maxSpeed -lat : %.5f km/h".format(maxSpeedScreen), fontSize = 20.sp)
            Text("avSpeed -lon : %.5f km/h".format(averageSpeedScreen), fontSize = 20.sp)
            Text("currentDist -acc: %.2f km/h".format(currentDistanceScreen), fontSize = 20.sp)
            Text("currentAlt -altGPS: %.2f m".format(currentAltitudeScreen), fontSize = 20.sp)
            Text("maxAlt -altacc: %.2f m".format(maxAltitudeScreen), fontSize = 20.sp)
            Text("minAlt -altSrtm: %.2f m".format(minAltitudeScreen), fontSize = 20.sp)
            Text("travelTime -altIgn: %.2f s".format(travelTimeScreen), fontSize = 20.sp)
            Text(
                "cumElevGain -altHdMnt: %.2f m".format(cumulativeElevationGainScreen),
                fontSize = 20.sp
            )
            Text(
                "curVertSpd -altHdMns: %.2f m/s".format(currentVerticalSpeedScreen),
                fontSize = 20.sp
            )
            Text("maxVertSpd -DB SRTM: %.2f m/s".format(maxVerticalSpeedScreen), fontSize = 20.sp)
            Text("minVertSpd -DB IGN: %.2f m/s".format(minVerticalSpeedScreen), fontSize = 20.sp)
            Text("currentSlope -DB MNT: %.2f".format(currentSlopeScreen), fontSize = 20.sp)
            Text("maxSlope -DB -MNS: %.2f".format(maxSlopeScreen), fontSize = 20.sp)
            Text("minSlope -timeStamp: %.2f".format(minSlopeScreen), fontSize = 20.sp)


            // 🟦 Boutons GPS + Enregistrement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 🔘 Bouton GPS
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java)
                        if (isGpsActive) {
                            context.stopService(intent)
                        } else {
                            context.startForegroundService(intent)
                        }
                        isGpsActive = !isGpsActive
                    }
                ) {
                    Text(if (isGpsActive) "Arrêter GPS" else "Démarrer GPS")
                }

                // 🔴 Bouton Enregistrement
                Button(
                    onClick = {
                        AppConfig.isRecording = !AppConfig.isRecording
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppConfig.isRecording) Color.Red else Color.Green
                    )
                ) {
                    Text(if (AppConfig.isRecording) "Pause" else "Enregistrer")
                }
            }
        }
    }
}

 */