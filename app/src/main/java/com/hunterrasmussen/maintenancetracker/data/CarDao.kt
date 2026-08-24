package com.hunterrasmussen.maintenancetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY nickname COLLATE NOCASE")
    fun getAll(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getById(carId: Long): Flow<Car?>

    @Query("SELECT * FROM cars")
    suspend fun getAllOnce(): List<Car>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(car: Car): Long

    @Update
    suspend fun update(car: Car)

    @Delete
    suspend fun delete(car: Car)

    @Query("DELETE FROM cars")
    suspend fun deleteAll()
}
