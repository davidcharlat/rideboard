package com.example.rideboard.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.ui.computeFontSize
import com.example.rideboard.ui.formatDuration
import com.example.rideboard.utils.ScreenValues
import kotlin.math.max

@Composable
fun PowerView(
    screenValues: ScreenValues,
) {
    var currentScreen by remember { mutableStateOf(1) }

    if (currentScreen == 1) {
        PowerViewScreen1(
            screenValues = screenValues,
            onToggleScreen = { currentScreen = 2 }
        )
    } else {
        PowerViewScreen2(
            screenValues = screenValues,
            onToggleScreen = { currentScreen = 1 }
        )
    }
}

@Composable
private fun PowerViewScreen1(
    screenValues: ScreenValues,
    onToggleScreen: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val batText = if (screenValues.powerBattery != null) " (${screenValues.powerBattery}%)" else ""
            val textToPrint = "Pow: ${screenValues.power ?: "--"} W$batText"

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx * 0.8f,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )
            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onToggleScreen,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("2", fontSize = 12.sp, color = Color.White)
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Moy: %.0f W".format(screenValues.avgPower ?: 0.0)
            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 24.dp)
            )

            Button(
                onClick = {
                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                        "AvgPower"
                },
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
            ) {
                Text("0")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.6f)
            ) {
                // ligne power4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "Spt: %.0f".format(screenValues.screenPower4)
                        val textToPrint2 = " /%.0f".format(screenValues.maxScreenPower4)

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("$textToPrint1 $textToPrint2"),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint1,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = textToPrint2,
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower4"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // ligne power15
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "15s:%.0f".format(screenValues.screenPower15)
                        val textToPrint2 = " /%.0f".format(screenValues.maxScreenPower15)

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("$textToPrint1 $textToPrint2"),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint1,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = textToPrint2,
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower15"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // ligne power125
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "2'   : %.0f".format(screenValues.screenPower125)
                        val textToPrint2 = " /%.0f".format(screenValues.maxScreenPower125)

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("$textToPrint1 $textToPrint2"),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint1,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = textToPrint2,
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower125"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // ligne power1000
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "15' : %.0f".format(screenValues.screenPower1000)
                        val textToPrint2 = " /%.0f".format(screenValues.maxScreenPower1000)

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf("$textToPrint1 $textToPrint2"),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint1,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = textToPrint2,
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }
                    }

                    Button(
                        onClick = {
                            GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower1000"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }
            }

            // 2eme colonne : zones de puissance
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.4f)
            ) {
                // Power Z2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }
                        val textToPrint = "Z2: " + formatDuration((screenValues.durationPowerZ2).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.7f, availableWidthPx - 50),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )

                        Text(
                            text = textToPrint,
                            color = Color(0xFF00C853),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ2"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // Power Z3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }
                        val textToPrint = "Z3: " + formatDuration((screenValues.durationPowerZ3).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.7f, availableWidthPx - 50),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )

                        Text(
                            text = textToPrint,
                            color = Color(0xFFFFD600),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ3"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // Power Z4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }
                        val textToPrint = "Z4: " + formatDuration((screenValues.durationPowerZ4).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.7f, availableWidthPx - 50),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )

                        Text(
                            text = textToPrint,
                            color = Color(0xFFFF6D00),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ4"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }

                // Power Z5
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }
                        val textToPrint = "Z5: " + formatDuration((screenValues.durationPowerZ5).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.7f, availableWidthPx - 50),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )

                        Text(
                            text = textToPrint,
                            color = Color(0xFFDD2C00),
                            fontSize = valueFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            GpsBuffer.getLast()?.gpsPointStringToReset = "DurationPowerZ5"
                        },
                        modifier = Modifier.size(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                    ) {
                        Text("0")
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerViewScreen2(
    screenValues: ScreenValues,
    onToggleScreen: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Ligne 1: Puissance + Bouton de bascule vers écran 1
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Power: ${screenValues.power ?: "--"} W"

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx * 0.8f,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onToggleScreen,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("1", fontSize = 12.sp, color = Color.White)
            }
        }

        // Ligne 2: powerCadence
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val cadence = screenValues.cadence
            val textToPrint = "Cadence: ${cadence?.let { "%d rpm".format(it) } ?: "--"}"

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        //ligne 2 bis avg et  max cadence
        Row(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Moitié gauche : avgCadence
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val buttonSpace = 28.dp

                val availableWidthPx = with(LocalDensity.current) { (maxWidth - buttonSpace).toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                val textToPrint = if (screenValues.averageCadence == null) " " else " avg: %.1f  ".format(screenValues.averageCadence)

                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Text(
                    text = textToPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = buttonSpace),
                    color = Color.Yellow,
                    fontSize = valueFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "AverageCadence" },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterStart),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) {
                    Text("0")
                }
            }

            // Moitié droite : maxCadence
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val buttonSpace = 28.dp

                val availableWidthPx = with(LocalDensity.current) { (maxWidth - buttonSpace).toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                val textToPrint = if (screenValues.maxCadence == null) " " else "  max: ${screenValues.maxCadence}  "

                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Text(
                    text = textToPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = buttonSpace),
                    color = Color.Yellow,
                    fontSize = valueFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "MaxCadence" },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterEnd),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) {
                    Text("0")
                }
            }
        }

        // Ligne 2 ter: powerCadence
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = " ${screenValues.totalRevolutions} tours de pédales "

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "TotalRevolutions" },
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
            ) {
                Text("0")
            }
        }


        /*BoxWithConstraints(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val buttonSize = 20.dp
            val buttonSpace = 28.dp // 20 dp pour le bouton + marge éventuelle

            val availableWidthPx = with(LocalDensity.current) { (maxWidth - buttonSpace).toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint =
                if (screenValues.maxCadence == null) " "
                else "max: ${screenValues.maxCadence}"

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = buttonSpace)
                    .align(Alignment.Center),
                color = Color.Yellow,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "MaxCadence" },
                modifier = Modifier
                    .size(buttonSize)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
            ) {
                Text("0")
            }
        }*/


        // Ligne 3: powerBalance
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = screenValues.smoothedPowerPercentRightLeft?.let {
                val gauche = it / 2.0
                "%.1f %%  balance G/D %.1f %%".format(gauche, 100.0 - gauche)
            } ?: " "
            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Ligne 4: pedalSmoothness (Left / Right) - 2 cases
        /*Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                val textToPrint = if (screenValues.pedalSmoothnessLeft == null) " " else "Fluidité G: ${screenValues.pedalSmoothnessLeft.let { "%.0f %%".format(it) }}"
                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Text(
                    text = textToPrint,
                    color = Color.White,
                    fontSize = valueFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                val textToPrint = if (screenValues.pedalSmoothnessRight == null) " " else "Fluidité D: ${screenValues.pedalSmoothnessRight.let { "%.0f %%".format(it) }}"
                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

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

        // Ligne 3: torqueEffectiveness (Left / Right) - 2 cases
        */

        //couple


        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val value = run {
                var cadence = if (screenValues.cadence == null) null else (screenValues.cadence).toFloat()
                if (cadence != null) cadence = max(10f,cadence)
                if (screenValues.power == null || cadence == null) null else screenValues.power.toDouble() * 9.6 / cadence.toDouble()  // *9.6 +-= *60/Pi
            }
            val textToPrint =if (value == null) " " else "Couple: ${value.let { "%.1f N.m".format(it) }}"

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                color = Color.White,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }



        // Ligne 4 bis : avg et maxpowerTorque
        Row(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Moitié gauche : avgTorque
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val buttonSpace = 28.dp

                val availableWidthPx = with(LocalDensity.current) { (maxWidth - buttonSpace).toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                val textToPrint = if (screenValues.averagePowerTorque == null) " " else " avg: %.1f  ".format(screenValues.averagePowerTorque)

                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Text(
                    text = textToPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = buttonSpace),
                    color = Color.Yellow,
                    fontSize = valueFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "AveragePowerTorque" },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterStart),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) {
                    Text("0")
                }
            }

            // Moitié droite : maxTorque
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val buttonSpace = 28.dp

                val availableWidthPx = with(LocalDensity.current) { (maxWidth - buttonSpace).toPx() }
                val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                val textToPrint = if (screenValues.maxPowerTorque == null) " " else " avg: %.1f  ".format(screenValues.maxPowerTorque)

                val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }

                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf(textToPrint),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                )

                Text(
                    text = textToPrint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = buttonSpace),
                    color = Color.Yellow,
                    fontSize = valueFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "MaxPowerTorque" },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterEnd),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
                ) {
                    Text("0")
                }
            }
        }

        //energie dépensée
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val buttonSize = 20.dp
            val buttonSpace = 28.dp // 20 dp pour le bouton + marge éventuelle

            val availableWidthPx = with(LocalDensity.current) {
                (maxWidth - buttonSpace).toPx()
            }
            val availableHeightPx = with(LocalDensity.current) {
                maxHeight.toPx()
            }
            val textToPrint =" Energie dépensée ${screenValues.totalPower / 1000} Kj "

            val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.65f).toSp() }
            val valueFont = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(textToPrint),
                availableWidthPx = availableWidthPx,
                maxFontSize = maxValueFont,
                minFontSize = 8.sp
            )

            Text(
                text = textToPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = buttonSpace)
                    .align(Alignment.Center),
                color = Color.Yellow,
                fontSize = valueFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { GpsBuffer.getLast()?.gpsPointStringToReset = "TotalPower" },
                modifier = Modifier
                    .size(buttonSize)
                    .align(Alignment.CenterEnd),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF5700))
            ) {
                Text("0")
            }
        }
    }
}
