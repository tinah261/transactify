package com.tinah.transactify.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bornes de temps et formatage de dates, calculés dans le fuseau horaire de
 * l'appareil (Madagascar = UTC+3) plutôt qu'en UTC.
 */
object DateUtils {

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Epoch (ms) du début du jour contenant [epochMillis]. */
    fun startOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** Epoch (ms) exclusif de fin du jour contenant [epochMillis] (= début du lendemain). */
    fun endOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** Bornes [début, fin[ de la journée courante. */
    fun todayRange(
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): LongRange = startOfDay(now, zone) until endOfDay(now, zone)

    /** Bornes [début du 1er jour, fin du dernier jour[ pour une période de dates. */
    fun rangeOf(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }

    fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun formatDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}
