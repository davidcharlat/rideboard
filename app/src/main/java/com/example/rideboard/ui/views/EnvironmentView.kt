package com.example.rideboard.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.ui.computeFontSize
import com.example.rideboard.ui.formatDuration
import com.example.rideboard.utils.ScreenValues
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt


@Composable
fun EnvironmentView(
    screenValues: ScreenValues
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
                .weight(1.8f)
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
                    with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val labelFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(" Conditions météo "),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxLabelFont,
                    minFontSize = 6.sp
                )

                val textToPrint = if (screenValues.outTemperature == null) " -- " else " t°: %.1f°C".format(screenValues.outTemperature)
                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row()
                    {
                        Text(
                            text = " conditions météo ",
                            color = Color.Gray,
                            fontSize = labelFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = textToPrint,
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
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val buttonSize = 20.dp
                    val gap = 4.dp

                    // Largeur disponible pour UN texte : un tiers de la ligne, moins le bouton et l'espace
                    val thirdTextWidthPx = with(density) { (maxWidth / 3 - buttonSize - gap).toPx() }
                    val maxValueFont = with(density) { (maxHeight.toPx() * 0.9f).toSp() }

                    val textMin = screenValues.minOutTemperature?.let { "min: %.1f°".format(it) } ?: " "
                    val textAvg = screenValues.avgOutTemperature?.let { "moy: %.1f°".format(it) } ?: " "
                    val textMax = screenValues.maxOutTemperature?.let { "max: %.1f°".format(it) } ?: " "

                    // La police doit convenir aux trois textes : on retient la plus grande qui les fait tous tenir
                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(textMin, textAvg, textMax),
                        availableWidthPx = thirdTextWidthPx,
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatWithReset(textMin, valueFont, "MinOutTemperature", Modifier.weight(1f))
                        StatWithReset(textAvg, valueFont, "AverageTemperature", Modifier.weight(1f))
                        StatWithReset(textMax, valueFont, "MaxOutTemperature", Modifier.weight(1f))
                    }
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
                    val text = if (screenValues.outHumidity == null)" " else "humidité: %.0f%% ".format(screenValues.outHumidity)

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(text),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

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
                    val valueWetBulb = calculateWetBulbTemperature(screenValues.outTemperature, screenValues.outHumidity)
                    val text = if (valueWetBulb == null)" "
                        else "t° bulbe humide: $valueWetBulb°C "
                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(text),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

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
                    val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                    val valueDewPoint = calculateDewPoint(screenValues.outTemperature, screenValues.outHumidity)
                    val text = if (valueDewPoint == null)" "
                        else "point de rosée: $valueDewPoint°C "

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(text),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

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
                    val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                    val valueDewPoint = calculateDewPoint(screenValues.outTemperature, screenValues.outHumidity)
                    val text = if (screenValues.environmentBattery == null)" " else "    niveau de batterie: ${screenValues.environmentBattery}%   "

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(text),
                        availableWidthPx = max(0.5f, availableWidthPx - 100),
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun StatWithReset(
    text: String,
    fontSize: TextUnit,
    resetKey: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false)   // le bouton est mesuré d'abord : il ne peut pas être poussé dehors
        )
        Spacer(Modifier.width(4.dp))
        Button(
            onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = resetKey },
            modifier = Modifier.size(20.dp),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
        ) {
            Text("0")
        }
    }
}

fun calculateDewPoint(temperature: Float?, humidite: Float?): Int? {
    if (temperature == null || humidite == null) return null
    val a = 17.62
    val b = 243.12
    val gamma = ln(humidite / 100.0) + (a * temperature) / (b + temperature)
    return (0.4 + (b * gamma) / (a - gamma)).toInt()
}

fun calculateWetBulbTemperature(temperature: Float?, humidite: Float?): Int? {
    if (temperature == null || humidite == null) return null
    return (temperature * atan(0.151977 * sqrt(humidite + 8.313659)) +
            atan(temperature + humidite) -
            atan(humidite.toDouble() - 1.676331) +
            0.00391838 * (humidite).pow(1.5f) * atan(0.023101 * humidite) -
            4.686035).toInt()
}