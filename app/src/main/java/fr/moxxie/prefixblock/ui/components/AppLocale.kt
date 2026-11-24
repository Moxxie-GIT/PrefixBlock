package fr.moxxie.prefixblock.ui.components

import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import fr.moxxie.prefixblock.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Langue des textes affichés (`R.string.app_locale`). Le système peut être dans une autre langue
 * (ex. anglais) : les textes retombent alors sur le français par défaut, et les dates et noms de
 * pays doivent suivre les textes plutôt que le système.
 */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.app_locale))

/**
 * Comme `pluralStringResource`, mais en choisissant la forme selon les règles de la langue des
 * textes : en français, 0 est au singulier (« 0 ajoutée »), alors que l'anglais du système le
 * mettrait au pluriel.
 */
@Composable
fun appPluralStringResource(@PluralsRes id: Int, count: Int, vararg args: Any): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = appLocale()
    val resources = remember(context, configuration, locale) {
        val localized = Configuration(configuration).apply { setLocale(locale) }
        context.createConfigurationContext(localized).resources
    }
    return resources.getQuantityString(id, count, *args)
}

/** Date et heure complètes d'un appel (`24 nov. 2026, 12:25`). */
fun formatCallDateTime(timestamp: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(timestamp).atZone(zone))

/** Heure seule si l'appel date d'aujourd'hui (`12:25`), sinon date courte (`20/11/2026`). */
fun formatCallTimeCompact(
    timestamp: Long,
    locale: Locale,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val call = Instant.ofEpochMilli(timestamp).atZone(zone)
    val sameDay = call.toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val formatter = if (sameDay) DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    else DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    return formatter.withLocale(locale).format(call)
}

/** Nom d'un pays ou territoire ISO 3166-1 dans la langue [locale] (`GB` → `Royaume-Uni`). */
fun countryName(regionCode: String, locale: Locale): String =
    Locale.Builder().setRegion(regionCode).build().getDisplayCountry(locale)
