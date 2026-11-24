package fr.moxxie.prefixblock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import fr.moxxie.prefixblock.ui.dashboard.DashboardScreen
import fr.moxxie.prefixblock.ui.detection.DetectionScreen
import fr.moxxie.prefixblock.ui.history.HistoryScreen
import fr.moxxie.prefixblock.ui.rules.RulesScreen
import fr.moxxie.prefixblock.ui.settings.SettingsScreen

@Composable
fun PrefixBlockNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onOpenBlacklist = {
                    navController.navigateToTopLevel(RulesRoute(showWhitelist = false), restoreState = false)
                },
                onOpenWhitelist = {
                    navController.navigateToTopLevel(RulesRoute(showWhitelist = true), restoreState = false)
                },
            )
        }
        composable<RulesRoute> { entry ->
            RulesScreen(initialShowWhitelist = entry.toRoute<RulesRoute>().showWhitelist)
        }
        composable<HistoryRoute> { HistoryScreen() }
        composable<DetectionRoute> { DetectionScreen() }
        composable<SettingsRoute> { SettingsScreen() }
    }
}

/**
 * Navigue vers une destination principale en conservant une seule pile par onglet :
 * on revient à l'accueil en sauvegardant l'état de l'onglet quitté.
 *
 * @param restoreState `false` pour forcer les arguments de [route] plutôt que de restaurer
 * l'état précédemment sauvegardé de l'onglet (ex. : ouvrir directement la liste blanche).
 */
fun NavController.navigateToTopLevel(route: Any, restoreState: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

fun NavDestination?.isOnTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
