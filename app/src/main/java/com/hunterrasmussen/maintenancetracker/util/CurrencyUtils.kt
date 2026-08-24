package com.hunterrasmussen.maintenancetracker.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter get() = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun formatCents(cents: Long): String = formatter.format(cents / 100.0)

    /** Parses free-form user input like "42.50" or "42" into cents. Returns null if not a valid amount. */
    fun parseToCents(input: String): Long? {
        val trimmed = input.trim().replace("$", "")
        if (trimmed.isEmpty()) return null
        val value = trimmed.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100)
    }
}
