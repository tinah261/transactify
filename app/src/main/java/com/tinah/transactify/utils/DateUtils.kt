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

    /** [LocalDate] (fuseau de l'appareil) correspondant à [epochMillis]. */
    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * Bornes epoch `[start, endExclusive[` pour [dateRange], ou « toutes dates »
     * (`0`, [Long.MAX_VALUE]) si `null`. Centralise une conversion utilisée par
     * tous les filtres par période (transactions, rapports).
     */
    fun boundsOrAllTime(
        dateRange: ClosedRange<LocalDate>?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> {
        if (dateRange == null) return 0L to Long.MAX_VALUE
        val range = rangeOf(dateRange.start, dateRange.endInclusive, zone)
        return range.first to (range.last + 1)
    }
}
