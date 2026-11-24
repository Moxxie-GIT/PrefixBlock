package fr.moxxie.prefixblock.rules

import java.util.regex.PatternSyntaxException

/**
 * Moteur de décision : instantané immuable et thread-safe d'un ensemble de règles, compilé une
 * fois (regex précompilées) pour répondre rapidement dans `onScreenCall()`.
 *
 * Ordre d'évaluation (le premier niveau qui produit une correspondance décide) :
 * 1. Liste blanche, numéro complet → autorisé (priorité absolue).
 * 2. Liste noire, numéro complet → bloqué.
 * 3. Règles prioritaires (préfixes puis regex), résolues comme aux niveaux 4 et 5.
 * 4. Préfixes : le plus long l'emporte ; à longueur égale, l'autorisation l'emporte.
 * 5. Regex : une autorisation l'emporte sur un blocage.
 * 6. Aucune correspondance → autorisé.
 *
 * Les règles désactivées et les expressions invalides sont ignorées (jamais de blocage par erreur).
 * Les règles de numéro et de préfixe ne s'appliquent qu'aux numéros résolus au format international.
 */
class RuleEngine(rules: Collection<Rule>) {

    private class CompiledRegex(val rule: Rule, val regex: Regex)

    private class Tier(val prefixes: List<Rule>, val regexes: List<CompiledRegex>) {

        fun resolve(number: NormalizedNumber): Decision? {
            number.international?.let { international ->
                prefixes
                    .filter { international.startsWith(it.pattern) }
                    .maxWithOrNull(compareBy<Rule> { it.pattern.length }.thenBy { it.list == RuleList.WHITELIST })
                    ?.let { return it.toDecision() }
            }
            val key = number.matchKey
            val matching = regexes.filter { it.regex.containsMatchIn(key) }
            val winner = matching.firstOrNull { it.rule.list == RuleList.WHITELIST } ?: matching.firstOrNull()
            return winner?.rule?.toDecision()
        }
    }

    private val whitelistNumbers: Map<String, Rule>
    private val blacklistNumbers: Map<String, Rule>
    private val priorityTier: Tier
    private val normalTier: Tier

    /** Nombre de règles actives effectivement prises en compte. */
    val activeRuleCount: Int

    init {
        val active = rules.filter { it.enabled && it.pattern.isNotBlank() }
        whitelistNumbers = active.numbers(RuleList.WHITELIST)
        blacklistNumbers = active.numbers(RuleList.BLACKLIST)

        val prefixes = active.filter { it.type == RuleType.PREFIX }
        val regexes = active.filter { it.type == RuleType.REGEX }.mapNotNull { it.compile() }
        priorityTier = Tier(prefixes.filter { it.priority }, regexes.filter { it.rule.priority })
        normalTier = Tier(prefixes.filterNot { it.priority }, regexes.filterNot { it.rule.priority })

        activeRuleCount = whitelistNumbers.size + blacklistNumbers.size + prefixes.size + regexes.size
    }

    /** @param number numéro normalisé, ou `null` si l'appelant est masqué ou absent. */
    fun evaluate(number: NormalizedNumber?): Decision {
        if (number == null) return Decision.Allow(rule = null)
        number.international?.let { international ->
            whitelistNumbers[international]?.let { return Decision.Allow(it) }
            blacklistNumbers[international]?.let { return Decision.Block(it) }
        }
        return priorityTier.resolve(number)
            ?: normalTier.resolve(number)
            ?: Decision.Allow(rule = null)
    }

    private companion object {
        fun List<Rule>.numbers(list: RuleList): Map<String, Rule> =
            filter { it.type == RuleType.NUMBER && it.list == list }.associateBy { it.pattern }

        fun Rule.compile(): CompiledRegex? =
            try {
                CompiledRegex(this, Regex(pattern))
            } catch (_: PatternSyntaxException) {
                null
            }

        fun Rule.toDecision(): Decision = when (list) {
            RuleList.WHITELIST -> Decision.Allow(this)
            RuleList.BLACKLIST -> Decision.Block(this)
        }
    }
}
