package com.hunterrasmussen.maintenancetracker.ui.navigation

/** Sentinel used in place of a real id to mean "create a new one". */
const val NEW_ITEM_ID = -1L

object Routes {
    const val CAR_LIST = "carList"
    const val CAR_EDIT = "carEdit/{carId}"
    const val CAR_DETAIL = "carDetail/{carId}"
    const val RECORD_EDIT = "recordEdit/{carId}/{recordId}"
    const val SETTINGS = "settings"

    fun carEdit(carId: Long) = "carEdit/$carId"
    fun carDetail(carId: Long) = "carDetail/$carId"
    fun recordEdit(carId: Long, recordId: Long) = "recordEdit/$carId/$recordId"
}
