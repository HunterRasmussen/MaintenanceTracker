package com.hunterrasmussen.maintenancetracker.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.hunterrasmussen.maintenancetracker.data.AppDatabase
import com.hunterrasmussen.maintenancetracker.data.Car
import com.hunterrasmussen.maintenancetracker.data.MaintenanceRecord
import com.hunterrasmussen.maintenancetracker.data.RecordPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports/imports the entire local database plus receipt photos to a single .zip file that the
 * user picks a location for via the system file picker (Storage Access Framework). This is the
 * only way data ever leaves the app's private storage, and it never touches the network -- it's
 * purely a file the user controls (e.g. to copy to a USB drive or another folder they back up).
 */
class BackupManager(private val context: Context, private val database: AppDatabase) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun receiptsDir(): File = File(context.filesDir, "receipts").apply { mkdirs() }

    suspend fun export(destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cars = database.carDao().getAllOnce()
            val records = database.maintenanceRecordDao().getAllOnce()
            val photos = database.recordPhotoDao().getAllOnce()

            val bundle = BackupBundle(
                cars = cars.map {
                    BackupCar(it.id, it.nickname, it.make, it.model, it.year, it.vin)
                },
                records = records.map {
                    BackupRecord(
                        id = it.id,
                        carId = it.carId,
                        date = it.date.toString(),
                        category = it.category,
                        location = it.location,
                        odometer = it.odometer,
                        costCents = it.costCents,
                        notes = it.notes,
                    )
                },
                photos = photos.map {
                    BackupPhoto(
                        id = it.id,
                        recordId = it.recordId,
                        fileName = it.fileName,
                        position = it.position,
                        label = it.label,
                    )
                },
            )

            val outputStream = context.contentResolver.openOutputStream(destination)
                ?: error("Could not open destination for writing")

            outputStream.use { rawOut ->
                ZipOutputStream(rawOut).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(json.encodeToString(bundle).toByteArray())
                    zip.closeEntry()

                    val photoNames = photos.map { it.fileName }.distinct()
                    for (name in photoNames) {
                        val file = File(receiptsDir(), name)
                        if (!file.exists()) continue
                        zip.putNextEntry(ZipEntry("photos/$name"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /**
     * Merges the contents of the given backup file into the current data -- existing cars,
     * records, and photos are left untouched. Every imported row gets a freshly assigned id
     * (backup ids can't be trusted not to collide with what's already on the device), and any
     * imported car whose nickname matches an existing or already-imported one gets " (2)", " (3)",
     * etc. appended so both are kept distinguishable.
     */
    suspend fun import(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            var bundle: BackupBundle? = null
            val importedPhotoDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }

            try {
                val inputStream = context.contentResolver.openInputStream(source)
                    ?: error("Could not open backup file")

                inputStream.use { rawIn ->
                    ZipInputStream(rawIn).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            when {
                                entry.name == "data.json" -> {
                                    bundle = json.decodeFromString(BackupBundle.serializer(), zip.readBytes().decodeToString())
                                }
                                entry.name.startsWith("photos/") -> {
                                    val name = entry.name.removePrefix("photos/")
                                    if (name.isNotBlank()) {
                                        File(importedPhotoDir, name).outputStream().use { out -> zip.copyTo(out) }
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                val data = bundle ?: error("Backup file did not contain any data")
                val receipts = receiptsDir()

                // Copy imported photo files in first, renaming on any filename collision with
                // an existing file, so the DB rows below can reference their final names.
                val fileNameMap = mutableMapOf<String, String>()
                for (name in data.photos.map { it.fileName }.distinct()) {
                    val sourceFile = File(importedPhotoDir, name)
                    if (!sourceFile.exists()) continue
                    val destName = uniquePhotoFileName(name, receipts)
                    sourceFile.copyTo(File(receipts, destName), overwrite = false)
                    fileNameMap[name] = destName
                }

                database.withTransaction {
                    val takenNicknames = database.carDao().getAllOnce()
                        .mapTo(mutableSetOf()) { it.nickname.trim().lowercase() }

                    val carIdMap = mutableMapOf<Long, Long>()
                    data.cars.forEach { c ->
                        val nickname = uniqueNickname(c.nickname, takenNicknames)
                        val newId = database.carDao().upsert(
                            Car(id = 0, nickname = nickname, make = c.make, model = c.model, year = c.year, vin = c.vin)
                        )
                        carIdMap[c.id] = newId
                    }

                    val recordIdMap = mutableMapOf<Long, Long>()
                    data.records.forEach { r ->
                        val newCarId = carIdMap[r.carId] ?: return@forEach
                        val newId = database.maintenanceRecordDao().upsert(
                            MaintenanceRecord(
                                id = 0,
                                carId = newCarId,
                                date = LocalDate.parse(r.date),
                                category = r.category,
                                location = r.location,
                                odometer = r.odometer,
                                costCents = r.costCents,
                                notes = r.notes,
                            )
                        )
                        recordIdMap[r.id] = newId
                    }

                    data.photos.forEach { p ->
                        val newRecordId = recordIdMap[p.recordId] ?: return@forEach
                        val fileName = fileNameMap[p.fileName] ?: return@forEach
                        database.recordPhotoDao().insert(
                            RecordPhoto(
                                id = 0,
                                recordId = newRecordId,
                                fileName = fileName,
                                position = p.position,
                                label = p.label,
                            )
                        )
                    }
                }

                Unit
            } finally {
                importedPhotoDir.deleteRecursively()
            }
        }
    }

    /** Appends " (2)", " (3)", etc. until [desired] doesn't collide with anything in [taken] (case-insensitive). */
    private fun uniqueNickname(desired: String, taken: MutableSet<String>): String {
        var candidate = desired
        var counter = 2
        while (candidate.trim().lowercase() in taken) {
            candidate = "$desired ($counter)"
            counter++
        }
        taken.add(candidate.trim().lowercase())
        return candidate
    }

    /** Renames [desired] to e.g. "name_2.jpg" until it no longer collides with a file already in [dir]. */
    private fun uniquePhotoFileName(desired: String, dir: File): String {
        if (!File(dir, desired).exists()) return desired
        val extension = desired.substringAfterLast('.', "")
        val base = desired.substringBeforeLast('.')
        var counter = 2
        var candidate: String
        do {
            candidate = if (extension.isNotEmpty()) "${base}_$counter.$extension" else "${base}_$counter"
            counter++
        } while (File(dir, candidate).exists())
        return candidate
    }
}
