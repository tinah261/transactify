package com.tinah.transactify.utils

import java.text.NumberFormat
import java.util.Locale

/** Formatage monétaire en ariary (Ar), séparateur de milliers français. */
object MoneyFormatter {

    private val format: NumberFormat = NumberFormat.getInstance(Locale.FRENCH).apply {
        maximumFractionDigits = 0
        isGroupingUsed = true
    }

    /** Ex. `125000.0` → `"125 000 Ar"`. */
    fun format(amount: Double): String = "${format.format(amount)} Ar"

    /** Ex. `0.03` → `"3 %"`. */
    fun formatRate(rate: Double): String {
        val percent = NumberFormat.getInstance(Locale.FRENCH).apply {
            maximumFractionDigits = 2
        }.format(rate * 100)
        return "$percent %"
    }
}
