package com.hunterrasmussen.maintenancetracker.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterrasmussen.maintenancetracker.MaintenanceTrackerApp
import com.hunterrasmussen.maintenancetracker.backup.BackupManager
import com.hunterrasmussen.maintenancetracker.data.AppDatabase
import kotlinx.coroutines.launch

class SettingsViewModel(app: MaintenanceTrackerApp) : ViewModel() {

    private val backupManager = BackupManager(app, AppDatabase.getInstance(app))

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var isWorking by mutableStateOf(false)
        private set

    fun clearStatus() { statusMessage = null }

    fun exportBackup(destination: Uri) {
        isWorking = true
        viewModelScope.launch {
            val result = backupManager.export(destination)
            statusMessage = if (result.isSuccess) {
                "Backup saved successfully."
            } else {
                "Export failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
            }
            isWorking = false
        }
    }

    fun importBackup(source: Uri) {
        isWorking = true
        viewModelScope.launch {
            val result = backupManager.import(source)
            statusMessage = if (result.isSuccess) {
                "Backup restored successfully."
            } else {
                "Import failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
            }
            isWorking = false
        }
    }
}
