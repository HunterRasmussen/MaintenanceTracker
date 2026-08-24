package com.hunterrasmussen.maintenancetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_photos",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("recordId")],
)
data class RecordPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    /** File name (not full path) inside the app's private receipts/ directory. */
    val fileName: String,
    /** Display order within the record's photo list. */
    val position: Int,
    /** Optional user-supplied caption, e.g. "Odometer" or "Invoice", for when the photo isn't self-explanatory. */
    @ColumnInfo(defaultValue = "''")
    val label: String = "",
)

/** A photo attached to a record, as tracked in memory while a record is being edited. */
data class PhotoEntry(val fileName: String, val label: String = "")
