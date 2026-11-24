package fr.moxxie.prefixblock.screening

import fr.moxxie.prefixblock.rules.NormalizedNumber
import fr.moxxie.prefixblock.rules.Rule

/**
 * Appel tel que présenté par Telecom, réduit à ce dont le filtrage a besoin.
 * Indépendant des classes Android pour être testé sur la JVM.
 *
 * @property number numéro présenté, ou `null` s'il est absent ou non numérique.
 * @property numberPresented `false` si l'appelant a masqué son numéro (présentation restreinte).
 */
data class IncomingCall(
    val incoming: Boolean,
    val number: String?,
    val numberPresented: Boolean = true,
)

/** Décision envoyée à Telecom pour un appel. */
sealed interface ScreeningVerdict {
    data class Allow(val reason: AllowReason, val rule: Rule? = null) : ScreeningVerdict
    data class Block(val number: NormalizedNumber, val rule: Rule) : ScreeningVerdict
}

/** Raison d'une autorisation, utile au diagnostic (journal de débogage, tests). */
enum class AllowReason {
    /** Appel sortant : jamais filtré. */
    NOT_INCOMING,

    /** Numéro absent ou masqué : toujours autorisé. */
    NO_NUMBER,

    /** Une règle de liste blanche correspond. */
    WHITELISTED,

    /** Aucune règle ne correspond. */
    NO_MATCH,

    /** Règles illisibles (base inaccessible) : fail-open. */
    ENGINE_UNAVAILABLE,

    /** Décision trop lente : fail-open avant le délai de Telecom. */
    TIMEOUT,

    /** Erreur interne : fail-open. */
    ERROR,
}

/**
 * Extrait le numéro d'une adresse d'appel Telecom (`Call.Details.getHandle()`).
 *
 * - `tel:+33612345678` → `+33612345678`
 * - `sip:+33612345678@ims.example;user=phone` → `+33612345678` (partie utilisateur)
 * - autre schéma (`voicemail:`…) ou adresse vide → `null` (l'appel passera)
 *
 * @param schemeSpecificPart partie décodée de l'URI (`Uri.getSchemeSpecificPart()`).
 */
fun numberFromAddress(scheme: String?, schemeSpecificPart: String?): String? {
    val number = when (scheme?.lowercase()) {
        "tel" -> schemeSpecificPart
        "sip" -> schemeSpecificPart?.substringBefore('@')?.substringBefore(';')
        else -> null
    }
    return number?.trim()?.takeIf { it.isNotEmpty() }
}
