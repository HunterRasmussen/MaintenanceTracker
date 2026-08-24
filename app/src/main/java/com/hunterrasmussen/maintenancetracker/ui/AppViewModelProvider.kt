package com.hunterrasmussen.maintenancetracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hunterrasmussen.maintenancetracker.MaintenanceTrackerApp
import com.hunterrasmussen.maintenancetracker.ui.caredit.CarEditViewModel
import com.hunterrasmussen.maintenancetracker.ui.cardetail.CarDetailViewModel
import com.hunterrasmussen.maintenancetracker.ui.carlist.CarListViewModel
import com.hunterrasmussen.maintenancetracker.ui.recordedit.RecordEditViewModel
import com.hunterrasmussen.maintenancetracker.ui.settings.SettingsViewModel

fun CreationExtras.maintenanceTrackerApp(): MaintenanceTrackerApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MaintenanceTrackerApp)

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            CarListViewModel(maintenanceTrackerApp().carRepository)
        }
        initializer {
            CarEditViewModel(createSavedStateHandle(), maintenanceTrackerApp().carRepository)
        }
        initializer {
            CarDetailViewModel(
                createSavedStateHandle(),
                maintenanceTrackerApp(),
                maintenanceTrackerApp().carRepository,
                maintenanceTrackerApp().maintenanceRepository,
            )
        }
        initializer {
            RecordEditViewModel(
                createSavedStateHandle(),
                maintenanceTrackerApp(),
                maintenanceTrackerApp().maintenanceRepository,
            )
        }
        initializer {
            SettingsViewModel(maintenanceTrackerApp())
        }
    }
}
