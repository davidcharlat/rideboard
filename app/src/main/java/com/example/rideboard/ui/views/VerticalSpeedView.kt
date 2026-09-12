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
import com.example.rideboard.utils.ScreenValues
import kotlin.math.max


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
                            text = "        / %.2f ".format(screenValues.maxVerticalSpeed15),
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
