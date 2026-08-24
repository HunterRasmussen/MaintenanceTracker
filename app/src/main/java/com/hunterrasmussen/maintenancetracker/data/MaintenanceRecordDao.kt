package com.hunterrasmussen.maintenancetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {
    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC, id DESC")
    fun getForCar(carId: Long): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE id = :recordId")
    fun getById(recordId: Long): Flow<MaintenanceRecord?>

    @Query("SELECT DISTINCT category FROM maintenance_records ORDER BY category COLLATE NOCASE")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM maintenance_records")
    suspend fun getAllOnce(): List<MaintenanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: MaintenanceRecord): Long

    @Update
    suspend fun update(record: MaintenanceRecord)

    @Delete
    suspend fun delete(record: MaintenanceRecord)

    @Query("DELETE FROM maintenance_records")
    suspend fun deleteAll()
}
