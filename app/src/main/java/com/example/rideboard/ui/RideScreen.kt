package com.example.rideboard.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.core.content.FileProvider
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
import androidx.core.net.toUri

@Composable
fun RideScreen(
    rideViewModel: RideViewModel = viewModel()
) {
    val screenValues by rideViewModel.screenValues
    val context = LocalContext.current

    var isntOver by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(AppConfig.isRecording) }
    var showStravaUpload by remember {
        mutableStateOf(false)
    }

    if (showStravaUpload) {
        LaunchedEffect(Unit) {
            FitExporter.exportInGpx(context, File(context.filesDir, "ride.tsv"))
        }
        StravaUploadScreen()
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
                isRecording = isRecording,
                onToggleGps = {
                    val intent = Intent(context, LocationService::class.java)
                    isntOver = !isntOver

                    if (!isntOver) {
                        showStravaUpload = true
                        FitExporter.export(
                            context,
                            File(context.filesDir, "ride.tsv")
                        )

                        //les 4 lignes suivantes pour ouvrir l'import de strava
                        /*val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.strava.com/upload/select")
                        )*/

                        //context.startActivity(intent)

                        //shareFitFile(context) //<- ca c'est pour partager le fichier fit


                        // la suite c'était avant le test d'export vers strava
                        //FitExporter.export(context, File(context.filesDir, "ride.tsv"))
                        //FitExporter.exportInGpx(context, File(context.filesDir, "ride.tsv"))
                        //FitExporter.exportInTcx(context, File(context.filesDir, "ride.tsv"))
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
// test pour exporter le fit:
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
//*/