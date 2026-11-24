package fr.moxxie.prefixblock.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import fr.moxxie.prefixblock.data.region.DefaultRegionProvider
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.data.rules.SaveRuleResult
import fr.moxxie.prefixblock.di.ApplicationScope
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.screening.CallScreener
import fr.moxxie.prefixblock.screening.IncomingCall
import fr.moxxie.prefixblock.screening.ScreeningVerdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Commandes de test sur appareil (version debug uniquement), en attendant l'interface de gestion
 * des règles (phase 4). Le résultat s'affiche dans la sortie de `am broadcast` (`data="…"`).
 *
 * ```
 * R="-n fr.moxxie.prefixblock/.debug.DebugCommandReceiver -a fr.moxxie.prefixblock.debug"
 * adb shell am broadcast $R.LIST_RULES
 * adb shell am broadcast $R.ADD_RULE --es list BLACKLIST --es type NUMBER --es pattern +590690123456
 * adb shell am broadcast $R.DELETE_RULE --el id 12
 * adb shell am broadcast $R.SCREEN --es number 00870773111632
 * ```
 * `SCREEN` passe le numéro dans la même chaîne de décision que le service, sans rien enregistrer.
 */
@AndroidEntryPoint
class DebugCommandReceiver : BroadcastReceiver() {

    @Inject lateinit var rules: RuleRepository
    @Inject lateinit var screener: CallScreener
    @Inject lateinit var normalizer: PhoneNumberNormalizer
    @Inject lateinit var regionProvider: DefaultRegionProvider
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        appScope.launch {
            val result = try {
                execute(intent)
            } catch (e: Exception) {
                "Erreur : $e"
            }
            Log.i(TAG, result)
            pending.resultData = result
            pending.finish()
        }
    }

    private suspend fun execute(intent: Intent): String = when (intent.action?.substringAfterLast('.')) {
        "LIST_RULES" -> rules.getRules().joinToString("\n") { it.describe() }.ifEmpty { "Aucune règle" }

        "ADD_RULE" -> {
            val draft = RuleDraft(
                list = RuleList.valueOf(intent.required("list").uppercase()),
                type = RuleType.valueOf(intent.required("type").uppercase()),
                input = intent.required("pattern"),
                label = intent.getStringExtra("label"),
                priority = intent.getBooleanExtra("priority", false),
            )
            when (val saved = rules.addRule(draft)) {
                is SaveRuleResult.Saved -> "Ajoutée : ${saved.rule.describe()}"
                is SaveRuleResult.Invalid -> "Refusée : ${saved.error}"
                SaveRuleResult.Duplicate -> "Refusée : doublon"
                SaveRuleResult.NotFound -> "Refusée : introuvable"
            }
        }

        "DELETE_RULE" -> {
            val id = intent.getLongExtra("id", -1)
            require(id > 0) { "extra --el id manquant" }
            rules.deleteRule(id)
            "Supprimée : #$id"
        }

        "SCREEN" -> {
            val raw = intent.required("number")
            val region = regionProvider.currentRegion()
            val normalized = normalizer.normalize(raw, region)
            val verdict = screener.screen(IncomingCall(incoming = true, number = raw))
            val outcome = when (verdict) {
                is ScreeningVerdict.Block -> "BLOQUÉ par ${verdict.rule.describe()}"
                is ScreeningVerdict.Allow ->
                    "AUTORISÉ (${verdict.reason}${verdict.rule?.let { " : ${it.describe()}" } ?: ""})"
            }
            "$outcome | région=$region international=${normalized?.international} indicatif=${normalized?.countryCode}"
        }

        else -> "Action inconnue : ${intent.action}"
    }

    private fun Intent.required(name: String): String =
        requireNotNull(getStringExtra(name)) { "extra --es $name manquant" }

    private fun Rule.describe(): String =
        "#$id $list $type $pattern" +
            (if (!enabled) " [désactivée]" else "") +
            (if (priority) " [prioritaire]" else "") +
            (label?.let { " ($it)" } ?: "")

    private companion object {
        const val TAG = "PrefixBlockDebug"
    }
}
