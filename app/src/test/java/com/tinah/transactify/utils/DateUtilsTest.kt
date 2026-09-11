package com.tinah.transactify.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateUtilsTest {

    private val zone = ZoneId.of("Indian/Antananarivo") // UTC+3, fuseau réel du cash point

    @Test
    fun `boundsOrAllTime renvoie 0 a MAX_VALUE quand la periode est nulle`() {
        val (start, end) = DateUtils.boundsOrAllTime(null, zone)
        assertEquals(0L, start)
        assertEquals(Long.MAX_VALUE, end)
    }

    @Test
    fun `boundsOrAllTime couvre exactement les jours demandes`() {
        val day = LocalDate.of(2026, 9, 11)
        val (start, end) = DateUtils.boundsOrAllTime(day..day, zone)

        assertEquals(24 * 60 * 60 * 1000L, end - start)
        assertEquals(day, DateUtils.toLocalDate(start, zone))
        // la borne de fin est exclusive : la dernière ms de la journée est end - 1
        assertEquals(day, DateUtils.toLocalDate(end - 1, zone))
        assertEquals(day.plusDays(1), DateUtils.toLocalDate(end, zone))
    }

    @Test
    fun `toLocalDate respecte le fuseau plutot que UTC`() {
        // 21h30 le 10/09 a Madagascar (UTC+3) = 18h30 UTC, toujours le 10 dans les deux cas ici
        val epoch = LocalDate.of(2026, 9, 10).atStartOfDay(zone).plusHours(21).plusMinutes(30).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 9, 10), DateUtils.toLocalDate(epoch, zone))

        // 1h du matin le 11/09 a Madagascar = 22h UTC le 10/09 -> UTC dirait "10", le fuseau local doit dire "11"
        val justAfterMidnight = LocalDate.of(2026, 9, 11).atStartOfDay(zone).plusHours(1).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 9, 11), DateUtils.toLocalDate(justAfterMidnight, zone))
    }
}
