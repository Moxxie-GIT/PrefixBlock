package fr.moxxie.prefixblock.ui.navigation

import kotlinx.serialization.Serializable

// Routes typées de Navigation Compose (sérialisées par kotlinx.serialization).

@Serializable
data object DashboardRoute

/** @param showWhitelist ouvre directement l'onglet « Liste blanche ». */
@Serializable
data class RulesRoute(val showWhitelist: Boolean = false)

@Serializable
data object HistoryRoute

@Serializable
data object DetectionRoute

@Serializable
data object SettingsRoute
