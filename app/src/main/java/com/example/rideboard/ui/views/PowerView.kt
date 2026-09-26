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
import kotlin.Double
import kotlin.math.max


@Composable
fun PowerView(
    screenValues: ScreenValues,
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Puissance: ${screenValues.power ?: "--"} W     "

            // Taille max bornée à la fois par la hauteur du bloc et par la largeur du texte
            val maxValueFont =
                with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

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
        } // fin 1ere ligne
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth()  // <- indispensable : sans ça, "toute la ligne" n'a pas de sens
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val textToPrint = "Moy: %.0f W".format(screenValues.avgPower?: 0.0)
            val maxValueFont =
                with(LocalDensity.current) { (availableHeightPx * 0.9f).toSp() }

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
                    .padding(end = 24.dp) // <- réserve la place pour le bouton (20.dp + marge)
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
        }// fin de la box with constraints de la 2e ligne power
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {    //1ere colonne de la partie basse
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.6f)
            ) {
                //ligne power4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "Sprt: %.0f".format(screenValues.screenPower4)
                        val textToPrint2 = "  / %.0f ".format(screenValues.maxScreenPower4)

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

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower4"
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
                } //fin de la ligne power4

                //ligne power15
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "Rstc: %.0f".format(screenValues.screenPower15)
                        val textToPrint2 = "  / %.0f ".format(screenValues.maxScreenPower15)

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

                    }//fin de la box avec les constraints de la ligne 15
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower15"
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
                } //fin de la ligne power15 (là4)

                //ligne power125
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "Vo2:  %.0f".format(screenValues.screenPower125)
                        val textToPrint2 = "  / %.0f ".format(screenValues.maxScreenPower125)

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

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower125"
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
                } //fin de la ligne power125 (là4)

                //ligne power1000
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint1 = "ftp:   %.0f".format(screenValues.screenPower1000)
                        val textToPrint2 = "  / %.0f ".format(screenValues.maxScreenPower1000)

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

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "MaxPower1000"
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
                } //fin de la ligne power1000

            }//fin de la 1ere colonne de la partie basse
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.4f)
            ) {
                //ligne power4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z2: "+ formatDuration(screenValues.durationPowerZ2.toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint," "),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = " ",
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ2"
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
                } //fin de la ligne Z2

                //ligne Z3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z3: "+ formatDuration(screenValues.durationPowerZ3.toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint," "),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = " ",
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ3"
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
                } //fin de la ligne z3
                //ligne z4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z4: "+ formatDuration(screenValues.durationPowerZ4.toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint," "),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = " ",
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }

                    }//fin de la box avec les constraints de la ligne z4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ4"
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
                } //fin de la ligne power z4

                //ligne z5
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texte à gauche
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                        val maxValueFont =
                            with(LocalDensity.current) { (availableHeightPx * 0.6f).toSp() }
                        val textToPrint = "Z5: "+ formatDuration(screenValues.durationPowerZ5.toInt())

                        val valueFont = computeFontSize(
                            textMeasurer = textMeasurer,
                            values = listOf(textToPrint," "),
                            availableWidthPx = max(0.5f, availableWidthPx - 100),
                            maxFontSize = maxValueFont,
                            minFontSize = 8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = textToPrint,
                                color = Color.White,
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = " ",
                                color = Color(0xFFFFCF00),
                                fontSize = valueFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Left
                            )
                        }

                    }//fin de la box avec les constraints de la ligne power4
                    // Bouton tout à droite
                    Button(
                        onClick = {
                            com.example.rideboard.buffer.GpsBuffer.getLast()?.gpsPointStringToReset =
                                "DurationPowerZ5"
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
                } //fin de la ligne power1000
            }//fin de la 2è colonne
        }//fin de la row, partie basse de l'écran
    } //fin 1ere colonne de PowerView
}//fin de powerView

