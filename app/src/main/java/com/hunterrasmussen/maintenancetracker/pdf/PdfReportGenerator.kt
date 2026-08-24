package com.hunterrasmussen.maintenancetracker.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.hunterrasmussen.maintenancetracker.data.Car
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRecord
import com.hunterrasmussen.maintenancetracker.data.PhotoEntry
import com.hunterrasmussen.maintenancetracker.util.CurrencyUtils
import com.hunterrasmussen.maintenancetracker.util.PhotoStorage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a maintenance history report for a single car directly to a PDF using the platform's
 * built-in PdfDocument API (no third-party library needed). Runs entirely offline: it only reads
 * from the app's private receipt photos and writes to a Uri the user picked via SAF.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 40f
    private val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val MAX_IMAGE_HEIGHT = 220f

    suspend fun generate(
        context: Context,
        car: Car,
        records: List<MaintenanceRecord>,
        photosByRecordId: Map<Long, List<PhotoEntry>>,
        destination: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocument()
            var pageNumber = 1
            var page = document.startPage(newPageInfo(pageNumber))
            var canvas = page.canvas
            var y = MARGIN

            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = Color.BLACK }
            val subtitlePaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }
            val headingPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK }
            val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
            val captionPaint = Paint().apply { textSize = 10f; textSkewX = -0.25f; color = Color.DKGRAY }
            val dividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

            fun newPage() {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(newPageInfo(pageNumber))
                canvas = page.canvas
                y = MARGIN
            }

            fun ensureSpace(height: Float) {
                if (y + height > PAGE_HEIGHT - MARGIN) {
                    newPage()
                }
            }

            // Draws one line of text treating `y` as the top of the line (not the baseline),
            // using the paint's real font metrics so lines never overlap regardless of text size,
            // then advances `y` to the top of the next line plus some breathing room.
            fun drawLine(text: String, paint: Paint, extraSpacing: Float = 4f) {
                val fm = paint.fontMetrics
                val baseline = y - fm.ascent
                canvas.drawText(text, MARGIN, baseline, paint)
                y = baseline + fm.descent + extraSpacing
            }

            drawLine(car.nickname, titlePaint, extraSpacing = 8f)
            drawLine("${car.year} ${car.make} ${car.model}", subtitlePaint)
            if (car.vin.isNotBlank()) {
                drawLine("VIN: ${car.vin}", subtitlePaint)
            }
            val totalCents = records.sumOf { it.costCents }
            drawLine(
                "${records.size} record${if (records.size == 1) "" else "s"} · " +
                    "${CurrencyUtils.formatCents(totalCents)} total spent",
                subtitlePaint,
            )
            drawLine(
                "Generated ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                subtitlePaint,
                extraSpacing = 12f,
            )
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += 20f

            val sortedRecords = records.sortedByDescending { it.date }

            for (record in sortedRecords) {
                ensureSpace(90f)

                val headingFm = headingPaint.fontMetrics
                val headingBaseline = y - headingFm.ascent
                canvas.drawText(record.category, MARGIN, headingBaseline, headingPaint)
                val costText = CurrencyUtils.formatCents(record.costCents)
                val costWidth = headingPaint.measureText(costText)
                canvas.drawText(costText, PAGE_WIDTH - MARGIN - costWidth, headingBaseline, headingPaint)
                y = headingBaseline + headingFm.descent + 6f

                drawLine(
                    record.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " · ${record.location}",
                    bodyPaint,
                )
                drawLine("Odometer: ${record.odometer} mi", bodyPaint)

                if (record.notes.isNotBlank()) {
                    for (line in wrapText(record.notes, bodyPaint, CONTENT_WIDTH)) {
                        ensureSpace(16f)
                        drawLine(line, bodyPaint, extraSpacing = 2f)
                    }
                }

                for (photo in photosByRecordId[record.id].orEmpty()) {
                    val file = PhotoStorage.receiptFile(context, photo.fileName)
                    if (!file.exists()) continue
                    decodeSampledBitmap(file, 800, 800)?.let { bitmap ->
                        val scale = minOf(CONTENT_WIDTH / bitmap.width, MAX_IMAGE_HEIGHT / bitmap.height, 1f)
                        val drawWidth = bitmap.width * scale
                        val drawHeight = bitmap.height * scale
                        val captionFm = captionPaint.fontMetrics
                        val captionHeight = if (photo.label.isNotBlank()) (captionFm.descent - captionFm.ascent) + 4f else 0f
                        ensureSpace(drawHeight + captionHeight + 16f)
                        y += 8f
                        if (photo.label.isNotBlank()) {
                            drawLine(photo.label, captionPaint, extraSpacing = 4f)
                        }
                        canvas.drawBitmap(bitmap, null, RectF(MARGIN, y, MARGIN + drawWidth, y + drawHeight), null)
                        y += drawHeight + 8f
                        bitmap.recycle()
                    }
                }

                y += 12f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                y += 20f
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(destination)?.use { out ->
                document.writeTo(out)
            } ?: error("Could not open destination for writing")

            document.close()
        }
    }

    private fun newPageInfo(pageNumber: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var inSampleSize = 1
        val height = boundsOptions.outHeight
        val width = boundsOptions.outWidth
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }
}
