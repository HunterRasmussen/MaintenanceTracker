package com.hunterrasmussen.maintenancetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nickname: String,
    val make: String,
    val model: String,
    val year: Int,
    val vin: String = "",
)
