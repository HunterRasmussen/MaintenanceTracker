package com.hunterrasmussen.maintenancetracker.ui.caredit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterrasmussen.maintenancetracker.data.Car
import com.hunterrasmussen.maintenancetracker.data.CarRepository
import com.hunterrasmussen.maintenancetracker.ui.navigation.NEW_ITEM_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CarEditUiState(
    val nickname: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val vin: String = "",
) {
    val isValid: Boolean
        get() = nickname.isNotBlank() && make.isNotBlank() && model.isNotBlank() && year.toIntOrNull() != null
}

class CarEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val carRepository: CarRepository,
) : ViewModel() {

    private val carId: Long = savedStateHandle.get<Long>("carId") ?: NEW_ITEM_ID
    val isNew: Boolean = carId == NEW_ITEM_ID

    var uiState by mutableStateOf(CarEditUiState())
        private set

    init {
        if (!isNew) {
            viewModelScope.launch {
                carRepository.getCar(carId).first()?.let { car ->
                    uiState = CarEditUiState(
                        nickname = car.nickname,
                        make = car.make,
                        model = car.model,
                        year = car.year.toString(),
                        vin = car.vin,
                    )
                }
            }
        }
    }

    fun updateNickname(value: String) { uiState = uiState.copy(nickname = value) }
    fun updateMake(value: String) { uiState = uiState.copy(make = value) }
    fun updateModel(value: String) { uiState = uiState.copy(model = value) }
    fun updateYear(value: String) { uiState = uiState.copy(year = value.filter { it.isDigit() }.take(4)) }
    fun updateVin(value: String) { uiState = uiState.copy(vin = value) }

    suspend fun save(): Boolean {
        val state = uiState
        if (!state.isValid) return false
        carRepository.saveCar(
            Car(
                id = if (isNew) 0 else carId,
                nickname = state.nickname.trim(),
                make = state.make.trim(),
                model = state.model.trim(),
                year = state.year.toInt(),
                vin = state.vin.trim(),
            )
        )
        return true
    }
}
