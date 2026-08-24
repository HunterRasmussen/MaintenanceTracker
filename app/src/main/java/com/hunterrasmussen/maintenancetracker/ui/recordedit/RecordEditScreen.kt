package com.hunterrasmussen.maintenancetracker.ui.recordedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hunterrasmussen.maintenancetracker.ui.AppViewModelProvider
import com.hunterrasmussen.maintenancetracker.ui.components.PhotoViewerDialog
import com.hunterrasmussen.maintenancetracker.ui.components.rememberReceiptImageRequest
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecordEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.uiState
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingCaptureFileName by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var photoVersion by remember { mutableIntStateOf(0) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val fileName = pendingCaptureFileName
        pendingCaptureFileName = null
        if (success && fileName != null) {
            PhotoStorage.normalizeOrientation(context, fileName)
            viewModel.addPhoto(fileName)
        } else if (fileName != null) {
            PhotoStorage.deleteReceiptFile(context, fileName)
        }
    }

    val pickPhotosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val fileName = withContext(Dispatchers.IO) { PhotoStorage.importPickedPhoto(context, uri) }
                    if (fileName != null) viewModel.addPhoto(fileName)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add Maintenance" else "Edit Maintenance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = state.isValid,
                        onClick = { scope.launch { if (viewModel.save()) onDone() } },
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(state.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))) },
            )

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = {
                        viewModel.updateCategory(it)
                        categoryMenuExpanded = true
                    },
                    label = { Text("Category") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                )
                val filtered = categorySuggestions.filter {
                    state.category.isBlank() || it.contains(state.category, ignoreCase = true)
                }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        filtered.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    viewModel.updateCategory(suggestion)
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Location") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.odometer,
                onValueChange = viewModel::updateOdometer,
                label = { Text("Odometer (mi)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.cost,
                onValueChange = viewModel::updateCost,
                label = { Text("Cost") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Receipt photos", style = MaterialTheme.typography.titleSmall)

            if (state.photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.photos, key = { _, photo -> photo.fileName }) { index, photo ->
                        Box(modifier = Modifier.size(96.dp)) {
                            AsyncImage(
                                model = rememberReceiptImageRequest(
                                    PhotoStorage.receiptFile(context, photo.fileName),
                                    version = photoVersion,
                                ),
                                contentDescription = "Receipt photo ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewerIndex = index },
                            )
                            if (photo.label.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                ) {
                                    Text(
                                        photo.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.removePhoto(photo.fileName) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val (fileName, uri) = PhotoStorage.createReceiptCaptureTarget(context)
                    pendingCaptureFileName = fileName
                    takePictureLauncher.launch(uri)
                }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Text(" Take Photo")
                }
                OutlinedButton(onClick = {
                    pickPhotosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(" Choose Photos")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    viewerIndex?.let { index ->
        PhotoViewerDialog(
            photos = state.photos,
            initialIndex = index,
            onDismiss = { viewerIndex = null },
            onPhotoChanged = { photoVersion++ },
            onLabelChanged = viewModel::updatePhotoLabel,
        )
    }
}
