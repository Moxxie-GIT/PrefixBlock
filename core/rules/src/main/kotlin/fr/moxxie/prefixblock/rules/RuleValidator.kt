package fr.moxxie.prefixblock.rules

import java.util.regex.PatternSyntaxException

sealed interface PatternValidation {
    /** @property pattern motif normalisé, à enregistrer tel quel dans [Rule.pattern]. */
    data class Valid(val pattern: String) : PatternValidation

    data class Invalid(val error: PatternError) : PatternValidation
}

enum class PatternError {
    EMPTY,

    /** Un préfixe s'écrit au format international : `+` (ou `00`) suivi de chiffres. */
    PREFIX_FORMAT,
    PREFIX_TOO_LONG,

    /** Le numéro saisi ne peut pas être interprété. */
    NUMBER_FORMAT,
    REGEX_SYNTAX,
    REGEX_TOO_LONG,

    /** Expression de blocage qui toucherait des numéros de tous les pays. */
    REGEX_TOO_BROAD,
}

/**
 * Valide et normalise la saisie d'une règle avant enregistrement.
 *
 * Garde-fou : une expression régulière de **blocage** qui correspond à des numéros de tous les
 * continents est refusée. PrefixBlock ne doit jamais bloquer tous les inconnus par accident.
 *
 * Note : sur Android, `Regex` s'appuie sur ICU, dont la syntaxe diffère marginalement de celle
 * de la JVM. Les constructions courantes (classes, groupes, alternatives, quantificateurs) sont
 * identiques.
 */
class RuleValidator(private val normalizer: PhoneNumberNormalizer) {

    fun validate(list: RuleList, type: RuleType, input: String, defaultRegion: String?): PatternValidation =
        when (type) {
            RuleType.PREFIX -> validatePrefix(input)
            RuleType.NUMBER -> validateNumber(input, defaultRegion)
            RuleType.REGEX -> validateRegex(input, list)
        }

    fun validatePrefix(input: String): PatternValidation {
        val cleaned = input.filterNot { it.isWhitespace() || it in PhoneNumberNormalizer.VISUAL_SEPARATORS }
        if (cleaned.isEmpty()) return PatternValidation.Invalid(PatternError.EMPTY)
        val digits = when {
            cleaned.startsWith('+') -> cleaned.substring(1)
            cleaned.startsWith("00") -> cleaned.substring(2)
            else -> return PatternValidation.Invalid(PatternError.PREFIX_FORMAT)
        }
        if (digits.isEmpty() || digits.any { it !in '0'..'9' } || digits.startsWith('0')) {
            return PatternValidation.Invalid(PatternError.PREFIX_FORMAT)
        }
        if (digits.length > MAX_INTERNATIONAL_DIGITS) return PatternValidation.Invalid(PatternError.PREFIX_TOO_LONG)
        return PatternValidation.Valid("+$digits")
    }

    fun validateNumber(input: String, defaultRegion: String?): PatternValidation {
        if (input.isBlank()) return PatternValidation.Invalid(PatternError.EMPTY)
        val international = normalizer.normalize(input, defaultRegion)?.international
            ?: return PatternValidation.Invalid(PatternError.NUMBER_FORMAT)
        return PatternValidation.Valid(international)
    }

    fun validateRegex(input: String, list: RuleList): PatternValidation {
        val source = input.trim()
        if (source.isEmpty()) return PatternValidation.Invalid(PatternError.EMPTY)
        if (source.length > MAX_REGEX_LENGTH) return PatternValidation.Invalid(PatternError.REGEX_TOO_LONG)
        val regex = try {
            Regex(source)
        } catch (_: PatternSyntaxException) {
            return PatternValidation.Invalid(PatternError.REGEX_SYNTAX)
        }
        if (list == RuleList.BLACKLIST && BROADNESS_SAMPLES.all { regex.containsMatchIn(it) }) {
            return PatternValidation.Invalid(PatternError.REGEX_TOO_BROAD)
        }
        return PatternValidation.Valid(source)
    }

    private companion object {
        /** Longueur maximale d'un numéro E.164 (indicatif compris). */
        const val MAX_INTERNATIONAL_DIGITS = 15
        const val MAX_REGEX_LENGTH = 200

        /** Numéros plausibles répartis sur tous les continents (France, Guadeloupe, États-Unis,
         *  Royaume-Uni, Allemagne, Chine, Brésil, Nigeria). */
        val BROADNESS_SAMPLES = listOf(
            "+33612345678",
            "+590690123456",
            "+12125550123",
            "+447911123456",
            "+4915123456789",
            "+8613800138000",
            "+5511987654321",
            "+2348031234567",
        )
    }
}
