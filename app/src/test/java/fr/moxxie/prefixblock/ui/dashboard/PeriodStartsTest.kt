package fr.moxxie.prefixblock.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class PeriodStartsTest {

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    // Mardi 24 novembre 2026, midi UTC.
    private val tuesdayNoonUtc = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `week starts on Monday in France`() {
        val starts = PeriodStarts.of(tuesdayNoonUtc, Locale.FRANCE)
        assertEquals(millis("2026-11-24T00:00:00Z"), starts.today)
        assertEquals(millis("2026-11-23T00:00:00Z"), starts.thisWeek)
        assertEquals(millis("2026-11-01T00:00:00Z"), starts.thisMonth)
    }

    @Test
    fun `week starts on Sunday in the United States`() {
        assertEquals(millis("2026-11-22T00:00:00Z"), PeriodStarts.of(tuesdayNoonUtc, Locale.US).thisWeek)
    }

    @Test
    fun `periods follow the local time zone`() {
        // 2 h UTC le 1er décembre = 22 h le 30 novembre en Guadeloupe (UTC-4).
        val clock = Clock.fixed(Instant.parse("2026-12-01T02:00:00Z"), ZoneId.of("America/Guadeloupe"))
        val starts = PeriodStarts.of(clock, Locale.FRANCE)
        assertEquals(millis("2026-11-30T04:00:00Z"), starts.today)
        assertEquals(millis("2026-11-30T04:00:00Z"), starts.thisWeek) // lundi 30 novembre
        assertEquals(millis("2026-11-01T04:00:00Z"), starts.thisMonth)
    }
}
