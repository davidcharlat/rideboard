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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rideboard.ui.computeFontSize
import com.example.rideboard.ui.formatDuration
import com.example.rideboard.utils.ScreenValues
import kotlin.math.max


@Composable
fun HeartRateView(
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
                    values = listOf("  rythme cardiaque  "),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxLabelFont,
                    minFontSize = 6.sp
                ) * 1.2f
                val valueFont = computeFontSize(
                    textMeasurer = textMeasurer,
                    values = listOf("${screenValues.heartRate ?: "--"} bpm"),
                    availableWidthPx = availableWidthPx,
                    maxFontSize = maxValueFont,
                    minFontSize = 8.sp
                ) * 0.85f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row()
                    {
                    Text(
                        text = "  rythme cardiaque  ",
                        color = Color.Gray,
                        fontSize = labelFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                        Button(
                            onClick = {
                            },
                            modifier = Modifier.size(25.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCCCCC))
                        ) {
                            Text("...")
                        }
                }
                    Text(
                        text = "${screenValues.heartRate ?: "--"} bpm",
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
                //rythme min
                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                    val maxValueFont =
                        with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }
                    val textMin =
                        if ((screenValues.minHR ?: 0) == 0) "min: -- " else "${screenValues.minHR ?: "--"} bpm"
                    val textMax =
                        if ((screenValues.maxHR ?: 0) == 0) "max: -- " else "${screenValues.maxHR ?: "--"} bpm"
                    val textAvg =
                        if ((screenValues.avgHR ?: 0.0) < 1.0) "moy: -- " else screenValues.heartRate?.let { "%.0f bpm".format(it) } ?: "-- bpm"
                    val text = "$textMin         $textMax         $textAvg     "

                    val valueFont = computeFontSize(
                        textMeasurer = textMeasurer,
                        values = listOf(text),
                        availableWidthPx = availableWidthPx,
                        maxFontSize = maxValueFont,
                        minFontSize = 8.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        )
                        {
                            Text(
                                text = textMin,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                        "MinHR"
                                },
                                modifier = Modifier.size(20.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFBF5700
                                    )
                                )
                            ) {
                                Text("0")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        )
                        {
                            Text(
                                text = "    $textAvg",
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                        "AvgHR"
                                },
                                modifier = Modifier.size(20.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFBF5700
                                    )
                                )
                            ) {
                                Text("0")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        )
                        {
                            Text(
                                text = "    $textMax",
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                        "MaxHR"
                                },
                                modifier = Modifier.size(20.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFBF5700
                                    )
                                )
                            ) {
                                Text("0")
                            }
                        }
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
                    val text = "durée Z2: " + formatDuration(screenValues.durationHRZ2)

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
            Button(
                onClick = {
                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                        "DurationHRZ2"
                },
                modifier = Modifier.size(20.dp),
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
                    val text = "durée Z3: " + formatDuration(screenValues.durationHRZ3)

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
            Button(
                onClick = {
                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                        "DurationHRZ3"
                },
                modifier = Modifier.size(20.dp),
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
                    val text = "durée Z4: " + formatDuration(screenValues.durationHRZ4)

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
            Button(
                onClick = {
                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                        "DurationHRZ4"
                },
                modifier = Modifier.size(20.dp),
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
                    val text = "durée Z5: " + formatDuration(screenValues.durationHRZ5)

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
            Button(
                onClick = {
                    com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                        "DurationHRZ5"
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