package fr.moxxie.prefixblock.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Export / import des règles au format JSON versionné (CLAUDE.md §8, format v1) :
 *
 * ```json
 * { "format": "prefixblock-rules", "version": 1,
 *   "rules": [ { "list": "BLACKLIST", "type": "PREFIX", "pattern": "+870", "label": "Inmarsat",
 *                "enabled": true, "priority": false } ] }
 * ```
 *
 * Le décodage ne valide pas les motifs : l'import les fait passer par [RuleValidator] comme une
 * saisie manuelle. Une entrée mal formée est comptée et ignorée, sans faire échouer tout le fichier.
 */
object RulesBackup {

    const val FORMAT = "prefixblock-rules"
    const val VERSION = 1

    sealed interface DecodeResult {
        /** @param rules règles lues (identifiants à 0) ; [malformedEntries] entrées illisibles ignorées. */
        data class Success(val rules: List<Rule>, val malformedEntries: Int) : DecodeResult

        /** Pas un fichier PrefixBlock (JSON invalide ou champ `format` absent). */
        data object NotABackup : DecodeResult

        /** Fichier d'une version plus récente de l'application. */
        data class UnsupportedVersion(val version: Int) : DecodeResult
    }

    @Serializable
    private data class FileV1(val format: String, val version: Int, val rules: List<RuleV1>)

    @Serializable
    private data class RuleV1(
        val list: RuleList,
        val type: RuleType,
        val pattern: String,
        val label: String? = null,
        val enabled: Boolean = true,
        val priority: Boolean = false,
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun encode(rules: List<Rule>): String = json.encodeToString(
        FileV1.serializer(),
        FileV1(
            format = FORMAT,
            version = VERSION,
            rules = rules.map { RuleV1(it.list, it.type, it.pattern, it.label, it.enabled, it.priority) },
        ),
    )

    fun decode(text: String): DecodeResult {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: IllegalArgumentException) {
            null
        } ?: return DecodeResult.NotABackup

        val format = (root["format"] as? JsonPrimitive)?.content
        if (format != FORMAT) return DecodeResult.NotABackup
        val version = root["version"]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }
            ?: return DecodeResult.NotABackup
        if (version != VERSION) return DecodeResult.UnsupportedVersion(version)

        val entries = root["rules"] as? JsonArray ?: return DecodeResult.Success(emptyList(), 0)
        val rules = entries.mapNotNull { entry ->
            try {
                val rule = json.decodeFromJsonElement(RuleV1.serializer(), entry)
                Rule(
                    list = rule.list,
                    type = rule.type,
                    pattern = rule.pattern,
                    label = rule.label,
                    enabled = rule.enabled,
                    priority = rule.priority,
                )
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return DecodeResult.Success(rules, malformedEntries = entries.size - rules.size)
    }
}
