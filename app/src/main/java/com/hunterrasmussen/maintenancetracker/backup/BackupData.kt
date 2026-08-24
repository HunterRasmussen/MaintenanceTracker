package com.hunterrasmussen.maintenancetracker.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupCar(
    val id: Long,
    val nickname: String,
    val make: String,
    val model: String,
    val year: Int,
    val vin: String,
)

@Serializable
data class BackupRecord(
    val id: Long,
    val carId: Long,
    val date: String,
    val category: String,
    val location: String,
    val odometer: Int,
    val costCents: Long,
    val notes: String,
)

@Serializable
data class BackupPhoto(
    val id: Long,
    val recordId: Long,
    val fileName: String,
    val position: Int,
    val label: String = "",
)

@Serializable
data class BackupBundle(
    val version: Int = 3,
    val cars: List<BackupCar>,
    val records: List<BackupRecord>,
    val photos: List<BackupPhoto> = emptyList(),
)
