package com.hunterrasmussen.maintenancetracker.data

import kotlinx.coroutines.flow.Flow

class CarRepository(private val carDao: CarDao) {
    fun getAllCars(): Flow<List<Car>> = carDao.getAll()

    fun getCar(carId: Long): Flow<Car?> = carDao.getById(carId)

    suspend fun saveCar(car: Car): Long = carDao.upsert(car)

    suspend fun updateCar(car: Car) = carDao.update(car)

    suspend fun deleteCar(car: Car) = carDao.delete(car)
}
