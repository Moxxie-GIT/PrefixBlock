package fr.moxxie.prefixblock.rules

/** Liste d'appartenance d'une règle ; elle détermine l'action appliquée. */
enum class RuleList {
    /** Liste noire : l'appel est rejeté. */
    BLACKLIST,

    /** Liste blanche : l'appel est autorisé. */
    WHITELIST,
}

enum class RuleType {
    /** Numéro complet au format international (`+590690123456`), comparaison exacte. */
    NUMBER,

    /** Préfixe international (`+870`) : tous les numéros qui commencent ainsi. */
    PREFIX,

    /**
     * Expression régulière recherchée (et non comparée en entier) dans le numéro au format
     * international, `+` inclus. Utiliser `^` et `$` pour ancrer.
     */
    REGEX,
}

/**
 * Règle de filtrage.
 *
 * @property pattern pour [RuleType.NUMBER] et [RuleType.PREFIX] : forme normalisée « + chiffres »
 * produite par [RuleValidator] ; pour [RuleType.REGEX] : l'expression source.
 * @property priority règle prioritaire, évaluée avant les règles ordinaires
 * (sans effet sur [RuleType.NUMBER], déjà évalué en premier).
 */
data class Rule(
    val id: Long = 0,
    val list: RuleList,
    val type: RuleType,
    val pattern: String,
    val label: String? = null,
    val enabled: Boolean = true,
    val priority: Boolean = false,
)

/** Résultat de l'évaluation d'un appel par le [RuleEngine]. */
sealed interface Decision {
    /** @property rule règle d'autorisation appliquée, ou `null` si aucune règle ne correspond. */
    data class Allow(val rule: Rule?) : Decision

    /** @property rule règle de blocage appliquée (conservée dans l'historique). */
    data class Block(val rule: Rule) : Decision
}
