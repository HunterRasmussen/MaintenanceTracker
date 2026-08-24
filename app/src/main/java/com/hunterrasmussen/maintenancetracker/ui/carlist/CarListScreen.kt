package com.hunterrasmussen.maintenancetracker.ui.carlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hunterrasmussen.maintenancetracker.data.Car
import com.hunterrasmussen.maintenancetracker.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListScreen(
    onAddCar: () -> Unit,
    onOpenCar: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CarListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cars") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCar) {
                Icon(Icons.Filled.Add, contentDescription = "Add car")
            }
        },
    ) { padding ->
        if (cars.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(cars, key = { it.id }) { car ->
                    CarRow(car = car, onClick = { onOpenCar(car.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                "No cars yet. Tap + to add your first car.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CarRow(car: Car, onClick: () -> Unit) {
    Card(onClick = onClick, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        ListItem(
            headlineContent = { Text(car.nickname) },
            supportingContent = { Text("${car.year} ${car.make} ${car.model}") },
            leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
        )
    }
}
