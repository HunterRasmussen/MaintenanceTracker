package com.hunterrasmussen.maintenancetracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hunterrasmussen.maintenancetracker.data.PhotoEntry
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import java.io.File

/**
 * A full-screen, swipeable photo viewer with pinch-to-zoom/pan, a rotate button that persists the
 * rotation to disk, and an editable caption for when it's not obvious what a photo is. Pager
 * swiping is disabled while the current photo is zoomed in, so pan gestures don't fight
 * page-change gestures.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerDialog(
    photos: List<PhotoEntry>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onPhotoChanged: () -> Unit = {},
    onLabelChanged: (fileName: String, label: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var rotationTick by remember { mutableIntStateOf(0) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentZoom <= 1f,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val fileName = photos[page].fileName
                val file = remember(fileName, rotationTick) { PhotoStorage.receiptFile(context, fileName) }
                ZoomableImage(
                    file = file,
                    isActivePage = page == pagerState.currentPage,
                    onZoomChanged = { zoom -> if (page == pagerState.currentPage) currentZoom = zoom },
                )
            }

            val current = photos[pagerState.currentPage]
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            PhotoStorage.rotatePhoto(context, photos[pagerState.currentPage].fileName, 90)
                            rotationTick++
                            onPhotoChanged()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }
                }
                OutlinedTextField(
                    value = current.label,
                    onValueChange = { onLabelChanged(current.fileName, it) },
                    placeholder = { Text("Add a title, e.g. \"Odometer\" or \"Invoice\"") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            if (photos.size > 1) {
                Text(
                    "${pagerState.currentPage + 1} / ${photos.size}",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    file: File,
    isActivePage: Boolean,
    onZoomChanged: (Float) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale, isActivePage) {
        if (isActivePage) onZoomChanged(scale)
    }

    AsyncImage(
        model = rememberReceiptImageRequest(file),
        contentDescription = "Receipt photo",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = (containerSize.width * (newScale - 1) / 2f).coerceAtLeast(0f)
                    val maxY = (containerSize.height * (newScale - 1) / 2f).coerceAtLeast(0f)
                    scale = newScale
                    offset = if (newScale <= 1f) {
                        Offset.Zero
                    } else {
                        Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    )
}
