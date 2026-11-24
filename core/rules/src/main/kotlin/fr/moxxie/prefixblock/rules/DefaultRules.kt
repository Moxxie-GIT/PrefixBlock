package fr.moxxie.prefixblock.rules

/**
 * Règles installées à la création de la base de données.
 *
 * - Liste noire : indicatifs souvent utilisés par les appels Wangiri.
 * - Liste blanche : indicatifs protégés par défaut. +1 couvre tout le plan de numérotation
 *   nord-américain (États-Unis, Canada et une vingtaine de pays des Caraïbes) ; un préfixe plus
 *   précis en liste noire (ex. +1876) reste prioritaire.
 * - +44 n'est volontairement pas bloqué globalement.
 */
object DefaultRules {

    val initial: List<Rule> = listOf(
        blacklist("+870", "Inmarsat (satellite)"),
        blacklist("+674", "Nauru"),
        blacklist("+960", "Maldives"),
        blacklist("+685", "Samoa"),
        blacklist("+683", "Niue"),
        whitelist("+33", "France métropolitaine"),
        whitelist("+590", "Guadeloupe, Saint-Barthélemy, Saint-Martin"),
        whitelist("+1", "Amérique du Nord et Caraïbes (NANP)"),
    )

    private fun blacklist(prefix: String, label: String) =
        Rule(list = RuleList.BLACKLIST, type = RuleType.PREFIX, pattern = prefix, label = label)

    private fun whitelist(prefix: String, label: String) =
        Rule(list = RuleList.WHITELIST, type = RuleType.PREFIX, pattern = prefix, label = label)
}
