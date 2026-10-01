package com.example.rideboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rideboard.AppSettings

@Composable
fun SetValuesScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var powerZone2Text by remember { mutableStateOf(AppSettings.powerZone2.toString()) }
    var powerZone3Text by remember { mutableStateOf(AppSettings.powerZone3.toString()) }
    var powerZone4Text by remember { mutableStateOf(AppSettings.powerZone4.toString()) }
    var powerZone5Text by remember { mutableStateOf(AppSettings.powerZone5.toString()) }
    var hrZone2Text by remember { mutableStateOf(AppSettings.hrZone2.toString()) }
    var hrZone3Text by remember { mutableStateOf(AppSettings.hrZone3.toString()) }
    var hrZone4Text by remember { mutableStateOf(AppSettings.hrZone4.toString()) }
    var hrZone5Text by remember { mutableStateOf(AppSettings.hrZone5.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Zones de puissance", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = powerZone2Text,
            onValueChange = { powerZone2Text = it },
            label = { Text("Zone 2 (W)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = powerZone3Text,
            onValueChange = { powerZone3Text = it },
            label = { Text("Zone 3 (W)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = powerZone4Text,
            onValueChange = { powerZone4Text = it },
            label = { Text("Zone 4 (W)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = powerZone5Text,
            onValueChange = { powerZone5Text = it },
            label = { Text("Zone 5 (W)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text("Zones de fréquence cardiaque", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = hrZone2Text,
            onValueChange = { hrZone2Text = it },
            label = { Text("Zone 2 (bpm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = hrZone3Text,
            onValueChange = { hrZone3Text = it },
            label = { Text("Zone 3 (bpm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = hrZone4Text,
            onValueChange = { hrZone4Text = it },
            label = { Text("Zone 4 (bpm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = hrZone5Text,
            onValueChange = { hrZone5Text = it },
            label = { Text("Zone 5 (bpm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.weight(1f))


        Button(
            onClick = {
                powerZone2Text.toIntOrNull()?.let { AppSettings.powerZone2 = it }
                powerZone3Text.toIntOrNull()?.let { AppSettings.powerZone3 = it }
                powerZone4Text.toIntOrNull()?.let { AppSettings.powerZone4 = it }
                powerZone5Text.toIntOrNull()?.let { AppSettings.powerZone5 = it }
                hrZone2Text.toIntOrNull()?.let { AppSettings.hrZone2 = it }
                hrZone3Text.toIntOrNull()?.let { AppSettings.hrZone3 = it }
                hrZone4Text.toIntOrNull()?.let { AppSettings.hrZone4 = it }
                hrZone5Text.toIntOrNull()?.let { AppSettings.hrZone5 = it }
                AppSettings.save(context) // <- écrit le fichier avec les nouvelles valeurs
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }
    }
}
