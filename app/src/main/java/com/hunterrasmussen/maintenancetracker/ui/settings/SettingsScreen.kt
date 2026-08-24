package com.hunterrasmussen.maintenancetracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hunterrasmussen.maintenancetracker.ui.AppViewModelProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { pendingImportUri = it } }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            snackbarMessage = it
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "This app never connects to the internet. All of your cars, maintenance " +
                    "records, and receipt photos live only on this device. Use the buttons " +
                    "below to save or restore a backup file yourself -- for example, to a " +
                    "USB drive, SD card, or another app you use for file storage.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text("Export", style = MaterialTheme.typography.titleMedium)
            Text(
                "Creates a single .zip file with all your data and receipt photos.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                enabled = !viewModel.isWorking,
                onClick = {
                    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    exportLauncher.launch("maintenance_tracker_backup_$today.zip")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export Backup")
            }

            HorizontalDivider()

            Text("Import", style = MaterialTheme.typography.titleMedium)
            Text(
                "Restores data from a previously exported backup file. This replaces all " +
                    "data currently in the app.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                enabled = !viewModel.isWorking,
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import Backup")
            }

            if (viewModel.isWorking) {
                CircularProgressIndicator()
            }

            snackbarMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing this backup will permanently replace every car, maintenance " +
                        "record, and receipt photo currently in the app. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importBackup(uri)
                    pendingImportUri = null
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }
}
