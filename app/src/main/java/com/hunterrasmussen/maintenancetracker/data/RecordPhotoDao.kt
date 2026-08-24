package com.hunterrasmussen.maintenancetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordPhotoDao {
    @Query("SELECT * FROM record_photos WHERE recordId = :recordId ORDER BY position")
    fun getForRecord(recordId: Long): Flow<List<RecordPhoto>>

    @Query(
        "SELECT rp.* FROM record_photos rp " +
            "INNER JOIN maintenance_records mr ON rp.recordId = mr.id " +
            "WHERE mr.carId = :carId ORDER BY rp.recordId, rp.position"
    )
    fun getForCar(carId: Long): Flow<List<RecordPhoto>>

    @Query("SELECT * FROM record_photos")
    suspend fun getAllOnce(): List<RecordPhoto>

    @Insert
    suspend fun insert(photo: RecordPhoto): Long

    @Delete
    suspend fun delete(photo: RecordPhoto)

    @Query("DELETE FROM record_photos WHERE recordId = :recordId")
    suspend fun deleteAllForRecord(recordId: Long)

    @Query("DELETE FROM record_photos")
    suspend fun deleteAll()
}
