package com.hunterrasmussen.maintenancetracker.ui.carlist

import androidx.lifecycle.ViewModel
import com.hunterrasmussen.maintenancetracker.data.CarRepository

class CarListViewModel(carRepository: CarRepository) : ViewModel() {
    val cars = carRepository.getAllCars()
}
