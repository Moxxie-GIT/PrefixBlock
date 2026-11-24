package fr.moxxie.prefixblock.screening

import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.di.ApplicationScope
import fr.moxxie.prefixblock.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moteur de règles gardé en mémoire et recompilé à chaque modification des règles, pour que
 * `onScreenCall()` n'ait pas à lire la base ni à compiler de regex.
 *
 * Le chargement démarre dès la création (injection dans le service), en parallèle de la
 * normalisation du numéro.
 */
@Singleton
class RuleEngineProvider @Inject constructor(
    repository: RuleRepository,
    @ApplicationScope scope: CoroutineScope,
) {

    sealed interface State {
        data object Loading : State
        data class Ready(val engine: RuleEngine) : State
        data class Failed(val cause: Throwable) : State
    }

    val state: StateFlow<State> = repository.observeRules()
        .map<_, State> { rules -> State.Ready(RuleEngine(rules)) }
        .catch { emit(State.Failed(it)) }
        .stateIn(scope, SharingStarted.Eagerly, State.Loading)

    /** Moteur courant, après le premier chargement ; `null` si les règles sont illisibles. */
    suspend fun awaitEngine(): RuleEngine? =
        when (val loaded = state.first { it !is State.Loading }) {
            is State.Ready -> loaded.engine
            else -> null
        }
}
