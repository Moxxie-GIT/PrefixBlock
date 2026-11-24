package fr.moxxie.prefixblock.rules

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource
import java.util.Locale

/**
 * Numéro appelant après normalisation.
 *
 * @property raw numéro tel que présenté par Android.
 * @property international forme internationale « + chiffres » (ex. `+870773111632`), ou `null`
 * si le numéro ne peut pas être interprété sans ambiguïté. Ce n'est pas forcément un numéro
 * attribué : les numéros usurpés sont conservés tels quels pour pouvoir être filtrés.
 * @property countryCode indicatif pays détecté (ex. `870`, `1` pour tout le plan nord-américain),
 * ou `null` s'il est inconnu.
 */
data class NormalizedNumber(
    val raw: String,
    val international: String?,
    val countryCode: Int?,
) {
    /** Chaîne soumise aux expressions régulières : forme internationale, sinon chiffres bruts. */
    val matchKey: String
        get() = international ?: raw.filter { it in '0'..'9' }
}

/**
 * Normalise les numéros présentés par Android, avec Google libphonenumber (métadonnées
 * embarquées, aucun accès réseau).
 *
 * Stratégie, de la plus sûre à la moins sûre :
 * 1. `+…` : forme internationale explicite, conservée telle quelle (séparateurs retirés).
 * 2. Préfixe international (`00` en Europe, `011` aux États-Unis…) ou format national :
 *    interprétés selon la région par défaut (pays du réseau mobile). Un numéro national doit
 *    avoir une longueur plausible pour cette région.
 * 3. Chiffres seuls non interprétables dans la région : retenus comme numéro international
 *    sans `+` **uniquement** s'ils forment un numéro valide.
 * 4. Sinon : non résolu ([NormalizedNumber.international] = `null`). Les règles de préfixe ne
 *    s'appliquent pas, l'appel passe (fail-open).
 *
 * Thread-safe. La première utilisation charge des métadonnées : prévoir un préchauffage.
 */
class PhoneNumberNormalizer {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * @param raw numéro présenté (peut contenir espaces, tirets, points, parenthèses).
     * @param defaultRegion code pays ISO 3166-1 alpha-2 du réseau (ex. `"gp"`), insensible à la casse.
     * @return `null` si aucun numéro n'est présenté (absent ou masqué).
     */
    fun normalize(raw: String?, defaultRegion: String?): NormalizedNumber? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.filterNot { it.isWhitespace() || it in VISUAL_SEPARATORS }
        if (!DIALABLE.matches(cleaned)) return unresolved(raw)

        if (cleaned.startsWith('+')) return explicitInternational(raw, cleaned.substring(1))

        val region = defaultRegion?.uppercase(Locale.ROOT)?.takeIf { it in util.supportedRegions }
        if (region != null) {
            parseInRegion(raw, cleaned, region)?.let { return it }
        } else if (cleaned.startsWith("00")) {
            return explicitInternational(raw, cleaned.substring(2))
        }
        return inferInternational(raw, cleaned) ?: unresolved(raw)
    }

    /**
     * Préchargement des métadonnées les plus utiles pour que le premier appel filtré ne paie
     * pas ce coût dans le délai imparti par Android.
     */
    fun warmUp(defaultRegion: String?) {
        normalize("+33100000000", defaultRegion)
        normalize("0000000000", defaultRegion)
    }

    private fun explicitInternational(raw: String, digits: String): NormalizedNumber {
        if (digits.isEmpty()) return unresolved(raw)
        return NormalizedNumber(raw, "+$digits", countryCodeOf(digits))
    }

    /**
     * Indicatif pays en tête de [digits]. Les indicatifs UIT forment un code préfixe (aucun n'est
     * le début d'un autre) : le premier connu sur 1 à 3 chiffres est le bon.
     */
    private fun countryCodeOf(digits: String): Int? =
        (1..minOf(MAX_COUNTRY_CODE_DIGITS, digits.length))
            .map { digits.substring(0, it).toInt() }
            .firstOrNull { it != 0 && util.getRegionCodeForCountryCode(it) != UNKNOWN_REGION }

    private fun parseInRegion(raw: String, cleaned: String, region: String): NormalizedNumber? {
        val parsed = parseOrNull(cleaned, region) ?: return null
        val accepted = when (parsed.countryCodeSource) {
            // Préfixe international explicite (00, 011…) : aussi fiable qu'un « + ».
            CountryCodeSource.FROM_NUMBER_WITH_IDD -> true
            // Format national, ou indicatif de la région sans « + » : longueur plausible exigée.
            CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN,
            CountryCodeSource.FROM_DEFAULT_COUNTRY,
            -> util.isPossibleNumber(parsed)
            else -> false
        }
        return if (accepted) resolved(raw, parsed) else null
    }

    private fun inferInternational(raw: String, cleaned: String): NormalizedNumber? {
        if (cleaned.length < MIN_INFERRED_DIGITS) return null
        val parsed = parseOrNull("+$cleaned", UNKNOWN_REGION) ?: return null
        return if (util.isValidNumber(parsed)) resolved(raw, parsed) else null
    }

    private fun parseOrNull(text: String, region: String): PhoneNumber? =
        try {
            util.parseAndKeepRawInput(text, region)
        } catch (_: NumberParseException) {
            null
        }

    private fun resolved(raw: String, number: PhoneNumber) =
        NormalizedNumber(raw, util.format(number, PhoneNumberFormat.E164), number.countryCode)

    private fun unresolved(raw: String) = NormalizedNumber(raw, international = null, countryCode = null)

    internal companion object {
        /** Séparateurs visuels courants dans un numéro affiché. */
        const val VISUAL_SEPARATORS = "-.()/"

        private val DIALABLE = Regex("""\+?[0-9]{1,20}""")
        private const val UNKNOWN_REGION = "ZZ"
        private const val MAX_COUNTRY_CODE_DIGITS = 3
        private const val MIN_INFERRED_DIGITS = 8
    }
}
