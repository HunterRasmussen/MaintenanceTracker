package com.hunterrasmussen.maintenancetracker.ui.recordedit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRecord
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRepository
import com.hunterrasmussen.maintenancetracker.data.PRESET_CATEGORIES
import com.hunterrasmussen.maintenancetracker.data.PhotoEntry
import com.hunterrasmussen.maintenancetracker.ui.navigation.NEW_ITEM_ID
import com.hunterrasmussen.maintenancetracker.util.CurrencyUtils
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordEditUiState(
    val date: LocalDate = LocalDate.now(),
    val category: String = "",
    val location: String = "",
    val odometer: String = "",
    val cost: String = "",
    val notes: String = "",
    val photos: List<PhotoEntry> = emptyList(),
) {
    /** Requires at least one field to have content, rather than all of them. */
    val isValid: Boolean
        get() = (category.isNotBlank() || location.isNotBlank() || odometer.isNotBlank() ||
            cost.isNotBlank() || notes.isNotBlank() || photos.isNotEmpty()) &&
            (odometer.isBlank() || odometer.toIntOrNull() != null) &&
            (cost.isBlank() || CurrencyUtils.parseToCents(cost) != null)
}

class RecordEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    val carId: Long = checkNotNull(savedStateHandle["carId"])
    private val recordId: Long = savedStateHandle.get<Long>("recordId") ?: NEW_ITEM_ID
    val isNew: Boolean = recordId == NEW_ITEM_ID

    var uiState by mutableStateOf(RecordEditUiState())
        private set

    val categorySuggestions = maintenanceRepository.getDistinctCategories()
        .combine(kotlinx.coroutines.flow.flowOf(PRESET_CATEGORIES)) { used, presets ->
            (presets + used).distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PRESET_CATEGORIES)

    init {
        if (!isNew) {
            viewModelScope.launch {
                maintenanceRepository.getRecord(recordId).first()?.let { record ->
                    val photos = maintenanceRepository.getPhotosForRecord(recordId).first()
                        .sortedBy { it.position }
                        .map { PhotoEntry(it.fileName, it.label) }
                    uiState = RecordEditUiState(
                        date = record.date,
                        category = record.category,
                        location = record.location,
                        odometer = record.odometer.toString(),
                        cost = String.format("%.2f", record.costCents / 100.0),
                        notes = record.notes,
                        photos = photos,
                    )
                }
            }
        }
    }

    fun updateDate(value: LocalDate) { uiState = uiState.copy(date = value) }
    fun updateCategory(value: String) { uiState = uiState.copy(category = value) }
    fun updateLocation(value: String) { uiState = uiState.copy(location = value) }
    fun updateOdometer(value: String) { uiState = uiState.copy(odometer = value.filter { it.isDigit() }) }
    fun updateCost(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        uiState = uiState.copy(cost = filtered)
    }
    fun updateNotes(value: String) { uiState = uiState.copy(notes = value) }

    fun addPhoto(fileName: String) {
        uiState = uiState.copy(photos = uiState.photos + PhotoEntry(fileName))
    }

    /** Removes a photo from this record and deletes its file from disk. */
    fun removePhoto(fileName: String) {
        PhotoStorage.deleteReceiptFile(appContext, fileName)
        uiState = uiState.copy(photos = uiState.photos.filterNot { it.fileName == fileName })
    }

    /** Updates the caption for one photo, e.g. "Odometer" or "Invoice". */
    fun updatePhotoLabel(fileName: String, label: String) {
        uiState = uiState.copy(
            photos = uiState.photos.map { if (it.fileName == fileName) it.copy(label = label) else it }
        )
    }

    suspend fun save(): Boolean {
        val state = uiState
        if (!state.isValid) return false
        val costCents = if (state.cost.isBlank()) 0L else CurrencyUtils.parseToCents(state.cost) ?: return false
        val odometer = if (state.odometer.isBlank()) 0 else state.odometer.toIntOrNull() ?: return false

        val savedId = maintenanceRepository.saveRecord(
            MaintenanceRecord(
                id = if (isNew) 0 else recordId,
                carId = carId,
                date = state.date,
                category = state.category.trim(),
                location = state.location.trim(),
                odometer = odometer,
                costCents = costCents,
                notes = state.notes.trim(),
            )
        )
        maintenanceRepository.replacePhotos(if (isNew) savedId else recordId, state.photos)
        return true
    }
}
