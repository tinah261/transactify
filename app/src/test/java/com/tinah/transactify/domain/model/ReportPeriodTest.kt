package com.tinah.transactify.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportPeriodTest {

    private val today = LocalDate.of(2026, 9, 11)

    @Test
    fun `TODAY couvre uniquement le jour courant`() {
        assertEquals(today..today, ReportPeriod.TODAY.toDateRange(today))
    }

    @Test
    fun `LAST_7_DAYS couvre 7 jours en incluant aujourd'hui`() {
        val range = ReportPeriod.LAST_7_DAYS.toDateRange(today)
        assertEquals(today.minusDays(6), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun `LAST_30_DAYS couvre 30 jours en incluant aujourd'hui`() {
        val range = ReportPeriod.LAST_30_DAYS.toDateRange(today)
        assertEquals(today.minusDays(29), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun `ALL_TIME n'a pas de bornes`() {
        assertNull(ReportPeriod.ALL_TIME.toDateRange(today))
    }
}
