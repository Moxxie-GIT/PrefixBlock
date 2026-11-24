package fr.moxxie.prefixblock.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.moxxie.prefixblock.R
import kotlin.reflect.KClass

/** Destinations de la barre de navigation principale, dans l'ordre d'affichage. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    DASHBOARD(
        route = DashboardRoute,
        routeClass = DashboardRoute::class,
        label = R.string.nav_dashboard,
        icon = R.drawable.ic_home,
        selectedIcon = R.drawable.ic_home_filled,
    ),
    RULES(
        route = RulesRoute(),
        routeClass = RulesRoute::class,
        label = R.string.nav_rules,
        icon = R.drawable.ic_rule,
        selectedIcon = R.drawable.ic_rule_filled,
    ),
    HISTORY(
        route = HistoryRoute,
        routeClass = HistoryRoute::class,
        label = R.string.nav_history,
        icon = R.drawable.ic_history,
        selectedIcon = R.drawable.ic_history_filled,
    ),
    DETECTION(
        route = DetectionRoute,
        routeClass = DetectionRoute::class,
        label = R.string.nav_detection,
        icon = R.drawable.ic_radar,
        selectedIcon = R.drawable.ic_radar_filled,
    ),
    SETTINGS(
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
        label = R.string.nav_settings,
        icon = R.drawable.ic_settings,
        selectedIcon = R.drawable.ic_settings_filled,
    ),
}
