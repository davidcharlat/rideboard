package com.example.rideboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.rideboard.config.AppConfig
import com.example.rideboard.utils.ScreenValues
import com.example.rideboard.utils.SensorConnectionStatus
import com.example.rideboard.utils.SensorType

@Composable
fun SensorView(
    screenValues: ScreenValues,
    onConnectClick: (SensorType) -> Unit,
    onSensorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSelectionDialog by remember { mutableStateOf(false) }

    // Liste des capteurs actuellement sélectionnés pour affichage dans la barre
    val selectedSensors = remember {
        mutableStateListOf<SensorType>()
    }

    val sensorManager = AppConfig.sensorManager

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zone des capteurs sélectionnés (se partagent équitablement la largeur disponible)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedSensors.isEmpty()) {
                    Text(
                        text = " ",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    selectedSensors.forEach { sensorType ->
                        val status = sensorManager?.sensorStatuses?.get(sensorType) ?: SensorConnectionStatus.DISCONNECTED
                        val (icon, valueStr, clickKey) = when (sensorType) {
                            SensorType.HEART_RATE -> Triple("\u2665", screenValues.correctedHeartRate?.toString(), "heartRate")
                            SensorType.POWER -> Triple("\u26A1", screenValues.power?.toString(), "power")
                            SensorType.CADENCE -> Triple("\u21BB", screenValues.cadence?.toString(), "cadence")
                            SensorType.ENVIRONMENT -> {
                                val temp = screenValues.outTemperature
                                val hum = screenValues.outHumidity
                                val envStr = when {
                                    temp != null && hum != null -> "%.1f° %.0f%%".format(temp, hum)
                                    temp != null -> "%.1f°".format(temp)
                                    hum != null -> "%.0f%%".format(hum)
                                    else -> null
                                }
                                Triple("\u26C5", envStr, "environment")
                            }
                        }

                        SensorItem(
                            modifier = Modifier.weight(1f),
                            icon = icon,
                            value = valueStr,
                            status = status,
                            onConnect = { onConnectClick(sensorType) },
                            onClick = { onSensorClick(clickKey) }
                        )
                    }
                }
            }

            // Bouton "+" tout à droite qui ouvre la fenêtre de choix
            IconButton(
                onClick = { showSelectionDialog = true },
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Sélectionner les capteurs",
                    tint = Color.White
                )
            }
        }
    }

    // Fenêtre / Bulle de choix des capteurs
    if (showSelectionDialog) {
        SensorSelectionDialog(
            screenValues = screenValues,
            selectedSensors = selectedSensors,
            onToggleSensor = { sensorType ->
                if (selectedSensors.contains(sensorType)) {
                    selectedSensors.remove(sensorType)
                } else {
                    selectedSensors.add(sensorType)
                    // Déclenche automatiquement la recherche par scan
                    onConnectClick(sensorType)
                }
            },
            onDismiss = { showSelectionDialog = false }
        )
    }
}

@Composable
fun SensorItem(
    modifier: Modifier,
    icon: String,
    value: String?,
    status: SensorConnectionStatus,
    onConnect: () -> Unit,
    onClick: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    // Couleur du petit cercle d'état
    val statusColor = when (status) {
        SensorConnectionStatus.CONNECTED -> Color.Green
        SensorConnectionStatus.SCANNING -> Color.Yellow
        SensorConnectionStatus.LOST -> Color.Red
        SensorConnectionStatus.DISCONNECTED -> Color.DarkGray
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val stringToPrint = if (value != null) " $icon $value " else " $icon "
        val availableWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val availableHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val maxValueFont = with(LocalDensity.current) { (availableHeightPx * 0.7f).toSp() }

        val valueFont = computeFontSize(
            textMeasurer = textMeasurer,
            values = listOf(stringToPrint),
            availableWidthPx = availableWidthPx * 0.75f,
            maxFontSize = maxValueFont,
            minFontSize = 8.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Petit cercle de couleur pour l'état (remplace le bouton "+")
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
                    .clickable { onConnect() }
            )

            Text(
                text = stringToPrint,
                color = if (value != null) Color.Green else Color.White,
                fontSize = valueFont,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SensorSelectionDialog(
    screenValues: ScreenValues,
    selectedSensors: List<SensorType>,
    onToggleSensor: (SensorType) -> Unit,
    onDismiss: () -> Unit
) {
    val sensorManager = AppConfig.sensorManager

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222).copy(alpha = 0.95f),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Entête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capteurs disponibles",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Liste des 4 capteurs
                SensorType.values().forEach { sensorType ->
                    val isSelected = selectedSensors.contains(sensorType)
                    val status = sensorManager?.sensorStatuses?.get(sensorType) ?: SensorConnectionStatus.DISCONNECTED

                    val (icon, label, valueStr) = when (sensorType) {
                        SensorType.HEART_RATE -> Triple("\u2665", "Fréquence cardiaque", screenValues.correctedHeartRate?.let { "$it bpm" } ?: "--")
                        SensorType.POWER -> Triple("\u26A1", "Puissance", screenValues.power?.let { "$it W" } ?: "--")
                        SensorType.CADENCE -> Triple("\u21BB", "Cadence", screenValues.cadence?.let { "$it rpm" } ?: "--")
                        SensorType.ENVIRONMENT -> {
                            val temp = screenValues.outTemperature
                            val hum = screenValues.outHumidity
                            val str = when {
                                temp != null && hum != null -> "%.1f°C / %.0f%%".format(temp, hum)
                                temp != null -> "%.1f°C".format(temp)
                                hum != null -> "%.0f%%".format(hum)
                                else -> "--"
                            }
                            Triple("\u26C5", "Temp. / Humidité", str)
                        }
                    }

                    val statusColor = when (status) {
                        SensorConnectionStatus.CONNECTED -> Color.Green
                        SensorConnectionStatus.SCANNING -> Color.Yellow
                        SensorConnectionStatus.LOST -> Color.Red
                        SensorConnectionStatus.DISCONNECTED -> Color.Gray
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF333333) else Color(0xFF1A1A1A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSensor(sensorType) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Petit cercle de statut
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(statusColor, CircleShape)
                                )

                                Text(
                                    text = "$icon $label",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = valueStr,
                                    color = if (valueStr != "--") Color.Green else Color.Gray,
                                    fontSize = 14.sp
                                )

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSensor(sensorType) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.Green,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
