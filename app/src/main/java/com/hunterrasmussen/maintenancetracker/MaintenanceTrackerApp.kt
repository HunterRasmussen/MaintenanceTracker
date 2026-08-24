package com.hunterrasmussen.maintenancetracker

import android.app.Application
import com.hunterrasmussen.maintenancetracker.data.AppDatabase
import com.hunterrasmussen.maintenancetracker.data.CarRepository
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRepository

class MaintenanceTrackerApp : Application() {
    val carRepository: CarRepository by lazy {
        CarRepository(AppDatabase.getInstance(this).carDao())
    }

    val maintenanceRepository: MaintenanceRepository by lazy {
        val db = AppDatabase.getInstance(this)
        MaintenanceRepository(db.maintenanceRecordDao(), db.recordPhotoDao(), db)
    }
}
