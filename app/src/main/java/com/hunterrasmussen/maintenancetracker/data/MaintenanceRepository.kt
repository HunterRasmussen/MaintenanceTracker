package com.hunterrasmussen.maintenancetracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(
    private val recordDao: MaintenanceRecordDao,
    private val photoDao: RecordPhotoDao,
    private val database: AppDatabase,
) {
    fun getRecordsForCar(carId: Long): Flow<List<MaintenanceRecord>> = recordDao.getForCar(carId)

    fun getRecord(recordId: Long): Flow<MaintenanceRecord?> = recordDao.getById(recordId)

    fun getDistinctCategories(): Flow<List<String>> = recordDao.getDistinctCategories()

    suspend fun getAllRecordsOnce(): List<MaintenanceRecord> = recordDao.getAllOnce()

    suspend fun saveRecord(record: MaintenanceRecord): Long = recordDao.upsert(record)

    suspend fun updateRecord(record: MaintenanceRecord) = recordDao.update(record)

    suspend fun deleteRecord(record: MaintenanceRecord) = recordDao.delete(record)

    fun getPhotosForRecord(recordId: Long): Flow<List<RecordPhoto>> = photoDao.getForRecord(recordId)

    fun getPhotosForCar(carId: Long): Flow<List<RecordPhoto>> = photoDao.getForCar(carId)

    suspend fun getAllPhotosOnce(): List<RecordPhoto> = photoDao.getAllOnce()

    /** Replaces every photo attached to [recordId] with [photos], in order. */
    suspend fun replacePhotos(recordId: Long, photos: List<PhotoEntry>) {
        database.withTransaction {
            photoDao.deleteAllForRecord(recordId)
            photos.forEachIndexed { index, photo ->
                photoDao.insert(
                    RecordPhoto(recordId = recordId, fileName = photo.fileName, position = index, label = photo.label)
                )
            }
        }
    }
}
