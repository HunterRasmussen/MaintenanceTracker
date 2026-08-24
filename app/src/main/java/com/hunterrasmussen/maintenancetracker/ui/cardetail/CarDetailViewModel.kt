package com.hunterrasmussen.maintenancetracker.ui.cardetail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterrasmussen.maintenancetracker.data.Car
import com.hunterrasmussen.maintenancetracker.data.CarRepository
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRecord
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRepository
import com.hunterrasmussen.maintenancetracker.data.PhotoEntry
import com.hunterrasmussen.maintenancetracker.pdf.PdfReportGenerator
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CarDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    val carId: Long = checkNotNull(savedStateHandle["carId"])

    val car = carRepository.getCar(carId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records = maintenanceRepository.getRecordsForCar(carId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photosByRecord = maintenanceRepository.getPhotosForCar(carId)
        .map { photos -> photos.groupBy { it.recordId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredRecords = combine(records, _searchQuery) { allRecords, query ->
        if (query.isBlank()) {
            allRecords
        } else {
            allRecords.filter {
                it.category.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            val photos = maintenanceRepository.getPhotosForRecord(record.id).first()
            maintenanceRepository.deleteRecord(record)
            photos.forEach { PhotoStorage.deleteReceiptFile(appContext, it.fileName) }
        }
    }

    fun deleteCar(car: Car, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val recordsForCar = maintenanceRepository.getRecordsForCar(car.id).first()
            val photos = maintenanceRepository.getPhotosForCar(car.id).first()
            carRepository.deleteCar(car)
            photos.forEach { PhotoStorage.deleteReceiptFile(appContext, it.fileName) }
            onDeleted()
        }
    }

    fun exportPdf(destination: Uri, onResult: (Result<Unit>) -> Unit) {
        val currentCar = car.value ?: return
        val currentRecords = records.value
        viewModelScope.launch {
            val photosByRecordId = maintenanceRepository.getPhotosForCar(carId).first()
                .groupBy({ it.recordId }, { PhotoEntry(it.fileName, it.label) })
            onResult(PdfReportGenerator.generate(appContext, currentCar, currentRecords, photosByRecordId, destination))
        }
    }
}
