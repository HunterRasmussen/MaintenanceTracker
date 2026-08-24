package com.hunterrasmussen.maintenancetracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * All receipt photos live in the app's private files/receipts directory, which is only
 * accessible to this app and is not synced anywhere. Nothing here ever leaves the device.
 *
 * Every stored photo is kept "normalized": any EXIF rotation is baked directly into the pixels
 * right at capture/import time, and the file is left with no rotation metadata. That way every
 * consumer (thumbnails, the PDF export, the image viewer) can just decode the raw pixels and get
 * a correctly-oriented image, and rotating a photo later is a simple, reliable pixel rotation.
 */
object PhotoStorage {

    private fun receiptsDir(context: Context): File =
        File(context.filesDir, "receipts").apply { mkdirs() }

    fun receiptFile(context: Context, fileName: String): File =
        File(receiptsDir(context), fileName)

    private fun newFileName() = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"

    /** Creates an empty file for the system camera app to write into, and returns a FileProvider URI for it. */
    fun createReceiptCaptureTarget(context: Context): Pair<String, Uri> {
        val fileName = newFileName()
        val file = File(receiptsDir(context), fileName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return fileName to uri
    }

    /** Copies a picked gallery image (content:// URI) into private storage. Returns the new file name. */
    fun importPickedPhoto(context: Context, sourceUri: Uri): String? {
        val fileName = newFileName()
        val destFile = File(receiptsDir(context), fileName)
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            normalizeOrientation(destFile)
            fileName
        } catch (e: Exception) {
            destFile.delete()
            null
        }
    }

    /** Bakes any existing EXIF rotation into the pixels. Call once right after a fresh camera capture. */
    fun normalizeOrientation(context: Context, fileName: String) {
        normalizeOrientation(receiptFile(context, fileName))
    }

    private fun normalizeOrientation(file: File) {
        val degrees = readExifRotationDegrees(file)
        if (degrees != 0) rotateFile(file, degrees)
    }

    /** Rotates the stored photo by [degrees] (e.g. 90, -90, 180) and persists it, so the fix sticks everywhere. */
    fun rotatePhoto(context: Context, fileName: String, degrees: Int): Boolean {
        val file = receiptFile(context, fileName)
        if (!file.exists()) return false
        return try {
            rotateFile(file, degrees)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun rotateFile(file: File, degrees: Int) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        file.outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        rotated.recycle()
    }

    private fun readExifRotationDegrees(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun deleteReceiptFile(context: Context, fileName: String) {
        receiptFile(context, fileName).delete()
    }
}
