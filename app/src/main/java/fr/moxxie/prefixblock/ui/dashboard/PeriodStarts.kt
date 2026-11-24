package fr.moxxie.prefixblock.ui.dashboard

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Débuts (millisecondes epoch) des périodes des compteurs de l'accueil, dans le fuseau de [Clock].
 * La semaine commence selon la langue et la région (lundi en France).
 */
data class PeriodStarts(val today: Long, val thisWeek: Long, val thisMonth: Long) {

    companion object {
        fun of(clock: Clock, locale: Locale = Locale.getDefault()): PeriodStarts {
            val date = LocalDate.now(clock)
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            fun LocalDate.startMillis() = atStartOfDay(clock.zone).toInstant().toEpochMilli()
            return PeriodStarts(
                today = date.startMillis(),
                thisWeek = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).startMillis(),
                thisMonth = date.withDayOfMonth(1).startMillis(),
            )
        }
    }
}
