package fr.moxxie.prefixblock.screening

import fr.moxxie.prefixblock.data.region.DefaultRegionProvider
import fr.moxxie.prefixblock.rules.Decision
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Décide du sort d'un appel (CLAUDE.md §5 et §7). Sans dépendance Android directe.
 *
 * Les garanties de délai et le repli « autoriser » en cas d'erreur sont assurés par
 * [FailOpenScreening], pas ici.
 */
@Singleton
class CallScreener @Inject constructor(
    private val engines: RuleEngineProvider,
    private val normalizer: PhoneNumberNormalizer,
    private val regionProvider: DefaultRegionProvider,
) {

    suspend fun screen(call: IncomingCall): ScreeningVerdict = coroutineScope {
        if (!call.incoming) return@coroutineScope ScreeningVerdict.Allow(AllowReason.NOT_INCOMING)
        if (!call.numberPresented || call.number.isNullOrBlank()) {
            return@coroutineScope ScreeningVerdict.Allow(AllowReason.NO_NUMBER)
        }

        // Chargement des règles et normalisation (métadonnées libphonenumber) en parallèle.
        val engine = async { engines.awaitEngine() }
        val number = normalizer.normalize(call.number, regionProvider.currentRegion())
        if (number == null) {
            engine.cancel()
            return@coroutineScope ScreeningVerdict.Allow(AllowReason.NO_NUMBER)
        }

        when (val decision = engine.await()?.evaluate(number)) {
            null -> ScreeningVerdict.Allow(AllowReason.ENGINE_UNAVAILABLE)
            is Decision.Block -> ScreeningVerdict.Block(number, decision.rule)
            is Decision.Allow -> ScreeningVerdict.Allow(
                reason = if (decision.rule != null) AllowReason.WHITELISTED else AllowReason.NO_MATCH,
                rule = decision.rule,
            )
        }
    }
}
