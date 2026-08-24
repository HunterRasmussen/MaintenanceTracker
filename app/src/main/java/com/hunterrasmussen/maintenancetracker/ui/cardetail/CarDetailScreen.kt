package com.hunterrasmussen.maintenancetracker.ui.cardetail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRecord
import com.hunterrasmussen.maintenancetracker.data.RecordPhoto
import com.hunterrasmussen.maintenancetracker.ui.AppViewModelProvider
import com.hunterrasmussen.maintenancetracker.ui.components.rememberReceiptImageRequest
import com.hunterrasmussen.maintenancetracker.util.CurrencyUtils
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    onBack: () -> Unit,
    onEditCar: (Long) -> Unit,
    onAddRecord: (Long) -> Unit,
    onEditRecord: (Long, Long) -> Unit,
    onCarDeleted: () -> Unit,
    viewModel: CarDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val car by viewModel.car.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val photosByRecord by viewModel.photosByRecord.collectAsStateWithLifecycle()
    var showDeleteCarDialog by remember { mutableStateOf(false) }
    var recordPendingDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    BackHandler(enabled = isSearching) {
        isSearching = false
        viewModel.updateSearchQuery("")
    }

    LaunchedEffect(isSearching) {
        if (isSearching) searchFocusRequester.requestFocus()
    }

    // Photos can be rotated in place from the edit screen; bump this on every resume (e.g. when
    // navigating back here) so thumbnails re-read the file instead of showing a stale composition.
    var photoVersion by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) photoVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportPdf(uri) { result ->
                val message = if (result.isSuccess) {
                    "PDF exported"
                } else {
                    "Export failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            placeholder = { Text("Search category, location, notes") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                        )
                    } else {
                        Text(car?.nickname ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            viewModel.updateSearchQuery("")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSearching) "Close search" else "Back",
                        )
                    }
                },
                actions = {
                    if (isSearching) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search records")
                        }
                        IconButton(onClick = {
                            val safeName = (car?.nickname ?: "vehicle").replace(Regex("[^A-Za-z0-9_-]"), "_")
                            exportPdfLauncher.launch("${safeName}_maintenance_report.pdf")
                        }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
                        }
                        IconButton(onClick = { onEditCar(viewModel.carId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit car")
                        }
                        IconButton(onClick = { showDeleteCarDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete car")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddRecord(viewModel.carId) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add maintenance record")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            car?.let { c ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${c.year} ${c.make} ${c.model}", style = MaterialTheme.typography.titleMedium)
                        if (c.vin.isNotBlank()) {
                            Text("VIN: ${c.vin}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "${records.size} maintenance record${if (records.size == 1) "" else "s"} · " +
                                CurrencyUtils.formatCents(records.sumOf { it.costCents }) + " total",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text(
                        "No maintenance recorded yet. Tap the + button to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else if (filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text(
                        "No records match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        RecordRow(
                            record = record,
                            photos = photosByRecord[record.id].orEmpty(),
                            photoVersion = photoVersion,
                            onClick = { onEditRecord(viewModel.carId, record.id) },
                            onDelete = { recordPendingDelete = record },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteCarDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCarDialog = false },
            title = { Text("Delete this car?") },
            text = { Text("This will permanently delete the car and all of its maintenance records and receipt photos.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCarDialog = false
                    car?.let { viewModel.deleteCar(it, onCarDeleted) }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCarDialog = false }) { Text("Cancel") }
            },
        )
    }

    recordPendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Delete this record?") },
            text = { Text("This will permanently delete this maintenance record and its receipt photos, if any.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    recordPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun RecordRow(
    record: MaintenanceRecord,
    photos: List<RecordPhoto>,
    photoVersion: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Card(onClick = onClick, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val firstPhoto = photos.minByOrNull { it.position }
            if (firstPhoto != null) {
                Box(modifier = Modifier.size(56.dp)) {
                    AsyncImage(
                        model = rememberReceiptImageRequest(
                            PhotoStorage.receiptFile(context, firstPhoto.fileName),
                            version = photoVersion,
                        ),
                        contentDescription = "Receipt photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    if (photos.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 6.dp)),
                        ) {
                            Text(
                                "${photos.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null)
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(record.category, style = MaterialTheme.typography.titleSmall)
                Text(
                    record.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " · ${record.location}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${record.odometer} mi · ${CurrencyUtils.formatCents(record.costCents)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete record")
            }
        }
    }
}
