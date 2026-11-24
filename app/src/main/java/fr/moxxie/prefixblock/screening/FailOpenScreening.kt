package fr.moxxie.prefixblock.screening

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Garantit la règle fail-open du filtrage :
 * - exactement **une** réponse par appel ;
 * - une exception pendant la décision → autoriser ;
 * - pas de décision après [launch]`.budgetMillis` → autoriser, et la décision tardive est ignorée.
 *
 * L'échéance est une coroutine distincte : elle répond même si la décision est bloquée dans du
 * code non coopératif.
 */
object FailOpenScreening {

    /** Budget interne, nettement sous le délai de Telecom (5 s), au-delà duquel l'appel sonne. */
    const val DEFAULT_BUDGET_MILLIS = 2_000L

    fun launch(
        scope: CoroutineScope,
        budgetMillis: Long = DEFAULT_BUDGET_MILLIS,
        decide: suspend () -> ScreeningVerdict,
        respond: (ScreeningVerdict) -> Unit,
    ) {
        val responded = AtomicBoolean(false)
        val respondOnce = { verdict: ScreeningVerdict ->
            if (responded.compareAndSet(false, true)) respond(verdict)
        }

        val decision = scope.launch {
            val verdict = try {
                decide()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ScreeningVerdict.Allow(AllowReason.ERROR)
            }
            respondOnce(verdict)
        }
        val deadline = scope.launch {
            delay(budgetMillis)
            respondOnce(ScreeningVerdict.Allow(AllowReason.TIMEOUT))
            decision.cancel()
        }
        decision.invokeOnCompletion { deadline.cancel() }
    }
}
