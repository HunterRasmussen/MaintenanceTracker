package com.hunterrasmussen.maintenancetracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import java.io.File

/**
 * An ImageRequest for a receipt photo file, keyed by its last-modified time so Coil reloads it
 * instead of serving a stale cached bitmap after the file is rotated in place.
 *
 * [version] is an extra, caller-supplied invalidation signal: reading a value here that changes
 * when a rotation happens (even though the file *path* never changes) is what makes Compose
 * actually recompose this call and re-read the file's current last-modified time. Without it,
 * Compose has no reason to re-run this function after a sibling composable (like the full-screen
 * viewer) rotates the file out from under an otherwise-unchanged thumbnail.
 */
@Composable
fun rememberReceiptImageRequest(file: File, version: Int = 0): ImageRequest {
    val context = LocalContext.current
    val cacheKey = "${file.path}-${file.lastModified()}-$version"
    return remember(cacheKey) {
        ImageRequest.Builder(context)
            .data(file)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }
}
