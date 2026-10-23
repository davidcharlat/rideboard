package com.example.rideboard.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.rideboard.AppSettings
import com.example.rideboard.RideViewModel
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private enum class SettingsPage { MENU, ZONES, PREFERENCES, RINGING, EXPORT, DELETE }

@Composable
fun SetValuesScreen(
    onDone: () -> Unit,
    rideViewModel: RideViewModel? = null,
    currentPosition: GeoPoint? = null
) {
    var page by remember { mutableStateOf(SettingsPage.MENU) }

    BackHandler {
        page = when (page) {
            SettingsPage.MENU -> { onDone(); SettingsPage.MENU }
            SettingsPage.ZONES, SettingsPage.PREFERENCES -> SettingsPage.MENU
            SettingsPage.RINGING, SettingsPage.EXPORT, SettingsPage.DELETE -> SettingsPage.PREFERENCES
        }
    }

    when (page) {
        SettingsPage.MENU -> MenuPage(
            onZones = { page = SettingsPage.ZONES },
            onPreferences = { page = SettingsPage.PREFERENCES },
            onBack = onDone
        )
        SettingsPage.ZONES -> ZonesPage(onBack = { page = SettingsPage.MENU })
        SettingsPage.PREFERENCES -> PreferencesPage(
            onRinging = { page = SettingsPage.RINGING },
            onExport = { page = SettingsPage.EXPORT },
            onDelete = { page = SettingsPage.DELETE },
            onBack = { page = SettingsPage.MENU }
        )
        SettingsPage.RINGING -> RingingPage(onBack = { page = SettingsPage.PREFERENCES })
        SettingsPage.EXPORT -> ExportRidePage(onBack = { page = SettingsPage.PREFERENCES })
        SettingsPage.DELETE -> DeleteRidePage(onBack = { page = SettingsPage.PREFERENCES })
    }
}

// ---------------------------------------------------------------
// Page 1 : menu principal
// ---------------------------------------------------------------
@Composable
private fun MenuPage(onZones: () -> Unit, onPreferences: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onZones, modifier = Modifier.fillMaxWidth()) {
            Text("Définir les zones (W & bpm)")
        }
        Button(onClick = onPreferences, modifier = Modifier.fillMaxWidth()) {
            Text("Préférences")
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

// ---------------------------------------------------------------
// Page 2 : zones (champs compacts, 2 par ligne, scrollable)
// ---------------------------------------------------------------
@Composable
private fun ZonesPage(onBack: () -> Unit) {
    val context = LocalContext.current

    var p2 by remember { mutableStateOf(AppSettings.powerZone2.toString()) }
    var p3 by remember { mutableStateOf(AppSettings.powerZone3.toString()) }
    var p4 by remember { mutableStateOf(AppSettings.powerZone4.toString()) }
    var p5 by remember { mutableStateOf(AppSettings.powerZone5.toString()) }
    var h2 by remember { mutableStateOf(AppSettings.hrZone2.toString()) }
    var h3 by remember { mutableStateOf(AppSettings.hrZone3.toString()) }
    var h4 by remember { mutableStateOf(AppSettings.hrZone4.toString()) }
    var h5 by remember { mutableStateOf(AppSettings.hrZone5.toString()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Zones de puissance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 2 (W)", p2, { p2 = it }, Modifier.weight(1f))
                ZoneField("Zone 3 (W)", p3, { p3 = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 4 (W)", p4, { p4 = it }, Modifier.weight(1f))
                ZoneField("Zone 5 (W)", p5, { p5 = it }, Modifier.weight(1f))
            }

            Text("Zones de fréquence cardiaque", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 2 (bpm)", h2, { h2 = it }, Modifier.weight(1f))
                ZoneField("Zone 3 (bpm)", h3, { h3 = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneField("Zone 4 (bpm)", h4, { h4 = it }, Modifier.weight(1f))
                ZoneField("Zone 5 (bpm)", h5, { h5 = it }, Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Retour")
            }
            Button(
                onClick = {
                    p2.toIntOrNull()?.let { AppSettings.powerZone2 = it }
                    p3.toIntOrNull()?.let { AppSettings.powerZone3 = it }
                    p4.toIntOrNull()?.let { AppSettings.powerZone4 = it }
                    p5.toIntOrNull()?.let { AppSettings.powerZone5 = it }
                    h2.toIntOrNull()?.let { AppSettings.hrZone2 = it }
                    h3.toIntOrNull()?.let { AppSettings.hrZone3 = it }
                    h4.toIntOrNull()?.let { AppSettings.hrZone4 = it }
                    h5.toIntOrNull()?.let { AppSettings.hrZone5 = it }
                    AppSettings.save(context)
                    onBack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun ZoneField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

// ---------------------------------------------------------------
// Page 3 : préférences
// ---------------------------------------------------------------
@Composable
private fun PreferencesPage(
    onRinging: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sortie") }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Supprimer des sorties") }
        Button(onClick = onRinging, modifier = Modifier.fillMaxWidth()) { Text("Sonnerie") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Retour") }
    }
}

// ---------------------------------------------------------------
// Page 4 : sonnerie
// ---------------------------------------------------------------
@Composable
private fun RingingPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var ringing by remember { mutableStateOf(AppSettings.isRinging) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Émettre un son lorsqu'on s'arrête ou redémarre ?",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (ringing) "Oui" else "Non")
            Switch(
                checked = ringing,
                onCheckedChange = {
                    ringing = it
                    AppSettings.isRinging = it
                    AppSettings.save(context)
                }
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

@Composable
private fun DeleteRidePage(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(listRides(context)) }
    var pendingDelete by remember { mutableStateOf<List<File>?>(null) }

    RideSelectionPage(
        title = "Supprimer des sorties",
        actionLabel = "Supprimer",
        files = files,
        onAction = { pendingDelete = it },
        onBack = onBack
    )

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer définitivement ${toDelete.size} sortie(s) ?") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete.forEach { it.delete() }
                    files = listRides(context)
                    pendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun ExportRidePage(onBack: () -> Unit) {
    val context = LocalContext.current
    val files = remember { listRides(context) }

    RideSelectionPage(
        title = "Exporter des sorties",
        actionLabel = "Exporter",
        files = files,
        onAction = { selectedFiles ->
            val uris = ArrayList(selectedFiles.map {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "application/octet-stream"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, "Exporter les sorties"))
        },
        onBack = onBack
    )
}

private fun listRides(context: Context): List<File> =
    File(context.filesDir, "rides")
        .listFiles { f -> f.extension == "fit" }
        ?.sortedByDescending { it.name }
        ?: emptyList()

private fun prettyName(file: File): String = try {
    val parsed = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).parse(file.nameWithoutExtension)
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(parsed!!)
} catch (e: Exception) {
    file.name
}

@Composable
private fun RideSelectionPage(
    title: String,
    actionLabel: String,
    files: List<File>,
    onAction: (List<File>) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember(files) { mutableStateOf(setOf<String>()) }
    val allSelected = files.isNotEmpty() && selected.size == files.size

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (files.isEmpty()) {
            Text("Aucune sortie enregistrée.")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = {
                        selected = if (it) files.map { f -> f.name }.toSet() else emptySet()
                    }
                )
                Text("Tout sélectionner")
            }
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files, key = { it.name }) { file ->
                val checked = file.name in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) selected - file.name else selected + file.name
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selected = if (it) selected + file.name else selected - file.name
                        }
                    )
                    Column {
                        Text(prettyName(file))
                        Text(
                            "${file.length() / 1024} Ko",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Retour") }
            Button(
                onClick = { onAction(files.filter { it.name in selected }) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("$actionLabel (${selected.size})")
            }
        }
    }
}
