package com.app.campgear

import com.app.campgear.core.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test
    fun daysBetween_countsMinimumOneDay() {
        assertEquals(1, Formatters.daysBetween("2026-10-22", "2026-10-22"))
        assertEquals(3, Formatters.daysBetween("2026-10-22", "2026-10-25"))
        assertEquals(1, Formatters.daysBetween(null, "2026-10-25"))
    }

    @Test
    fun lateDays_returnsZeroWhenNotLate() {
        assertEquals(0, Formatters.lateDays("2026-10-22", "2026-10-22"))
        assertEquals(0, Formatters.lateDays("2026-10-22", "2026-10-20"))
        assertEquals(2, Formatters.lateDays("2026-10-20", "2026-10-22"))
    }

    @Test
    fun addDaysIso_movesAcrossMonthBoundary() {
        assertEquals("2026-11-01", Formatters.addDaysIso("2026-10-31", 1))
        assertEquals("2026-09-30", Formatters.addDaysIso("2026-10-01", -1))
    }

    @Test
    fun rupiah_formatsWithoutDecimals() {
        val formatted = Formatters.rupiah(50_000.0)
        assertTrue(formatted.contains("50.000"))
        assertTrue(formatted.contains("Rp"))
    }

    @Test
    fun parseIsoDate_handlesInvalidInput() {
        assertNotNull(Formatters.parseIsoDate("2026-10-22"))
        assertNull(Formatters.parseIsoDate(""))
        assertNull(Formatters.parseIsoDate(null))
    }

    @Test
    fun displayDate_handlesEmptyInput() {
        assertEquals("-", Formatters.displayDate(null))
        assertEquals("-", Formatters.displayDate(" "))
    }

    @Test fun invalidCalendarDateIsRejected() {
        assertNull(Formatters.parseIsoDate("2026-02-30"))
        assertNull(Formatters.parseIsoDate("2026-10-22junk"))
    }

    @Test fun timestamp_nanosecondsAndOffsetAreHandled() {
        assertEquals(Formatters.parseApiDateTime("2026-10-22T12:00:00.123Z"), Formatters.parseApiDateTime("2026-10-22T12:00:00.1234567Z"))
        assertEquals(Formatters.parseApiDateTime("2026-10-22T12:00:00Z"), Formatters.parseApiDateTime("2026-10-22T19:00:00+07:00"))
    }
}
