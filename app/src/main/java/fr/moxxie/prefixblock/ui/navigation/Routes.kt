package fr.moxxie.prefixblock.ui.navigation

import kotlinx.serialization.Serializable

// Routes typées de Navigation Compose (sérialisées par kotlinx.serialization).

@Serializable
data object DashboardRoute

/** @param showWhitelist ouvre directement l'onglet « Liste blanche ». */
@Serializable
data class RulesRoute(val showWhitelist: Boolean = false)

/**
 * Éditeur de règle (écran plein, hors barre de navigation).
 * @param ruleId règle à modifier, ou [NEW_RULE] pour en créer une dans la liste indiquée.
 */
@Serializable
data class RuleEditorRoute(val ruleId: Long = NEW_RULE, val whitelist: Boolean = false) {
    companion object {
        const val NEW_RULE = 0L
    }
}

@Serializable
data object HistoryRoute

@Serializable
data object DetectionRoute

@Serializable
data object SettingsRoute
