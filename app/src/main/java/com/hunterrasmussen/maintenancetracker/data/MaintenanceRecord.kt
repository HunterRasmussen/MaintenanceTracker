package com.hunterrasmussen.maintenancetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("carId")],
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val date: LocalDate,
    val category: String,
    val location: String,
    val odometer: Int,
    val costCents: Long,
    val notes: String = "",
)
