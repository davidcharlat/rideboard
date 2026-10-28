package com.example.rideboard.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
            val textToPrint = "Puissance: ${screenValues.power ?: "--"} W"

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
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
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
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z2: " + formatDuration((screenValues.durationPowerZ2 / 1000).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
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
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z3: " + formatDuration((screenValues.durationPowerZ3 / 1000).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
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
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z4: " + formatDuration((screenValues.durationPowerZ4 / 1000).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
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
                        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z5: " + formatDuration((screenValues.durationPowerZ5 / 1000).toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
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
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ5"
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
            val textToPrint = "Puissance: ${screenValues.power ?: "--"} W"

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

        // Ligne 2: pedalSmoothness (Left / Right) - 2 cases
        Row(
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
                val textToPrint = "Fluidité G: ${screenValues.pedalSmoothnessLeft?.let { "%.1f %%".format(it) } ?: "--"}"
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
                val textToPrint = "Fluidité D: ${screenValues.pedalSmoothnessRight?.let { "%.1f %%".format(it) } ?: "--"}"
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
        Row(
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
                val textToPrint = "Efficacité G: ${screenValues.torqueEffectivenessLeft?.let { "%.1f %%".format(it) } ?: "--"}"
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
                val textToPrint = "Efficacité D: ${screenValues.torqueEffectivenessRight?.let { "%.1f %%".format(it) } ?: "--"}"
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

        // Ligne 4: powerCadence
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Cadence Pédales: ${screenValues.powerCadence?.let { "%.0f rpm".format(it) } ?: "--"}"

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

        // Ligne 5: powerTorque
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Couple: ${screenValues.powerTorque?.let { "%.1f N.m".format(it) } ?: "--"}"

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
}
