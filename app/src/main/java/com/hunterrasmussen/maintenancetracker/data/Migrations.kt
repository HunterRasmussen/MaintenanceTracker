package com.hunterrasmussen.maintenancetracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 stored a single optional `photoFileName` column directly on maintenance_records. v2 moves
 * that into its own `record_photos` table so a record can have any number of photos. Any existing
 * single photo is preserved as position 0 in the new table -- nothing is deleted.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Recreating maintenance_records below requires foreign key checks to be off for the
        // duration of the rebuild; Room re-enables them the next time the database is opened.
        database.execSQL("PRAGMA foreign_keys=OFF")

        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `record_photos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recordId` INTEGER NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recordId`) REFERENCES `maintenance_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_record_photos_recordId` ON `record_photos` (`recordId`)"
        )

        // Preserve any existing single photo before the source column is dropped below.
        database.execSQL(
            "INSERT INTO record_photos (recordId, fileName, position) " +
                "SELECT id, photoFileName, 0 FROM maintenance_records WHERE photoFileName IS NOT NULL"
        )

        database.execSQL("ALTER TABLE maintenance_records RENAME TO maintenance_records_old")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `maintenance_records` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`carId` INTEGER NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`location` TEXT NOT NULL, " +
                "`odometer` INTEGER NOT NULL, " +
                "`costCents` INTEGER NOT NULL, " +
                "`notes` TEXT NOT NULL, " +
                "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
            "INSERT INTO maintenance_records (id, carId, date, category, location, odometer, costCents, notes) " +
                "SELECT id, carId, date, category, location, odometer, costCents, notes FROM maintenance_records_old"
        )
        database.execSQL("DROP TABLE maintenance_records_old")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_maintenance_records_carId` ON `maintenance_records` (`carId`)"
        )
    }
}

/** v3 adds an optional caption per photo, e.g. "Odometer" or "Invoice". */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE record_photos ADD COLUMN label TEXT NOT NULL DEFAULT ''")
    }
}
