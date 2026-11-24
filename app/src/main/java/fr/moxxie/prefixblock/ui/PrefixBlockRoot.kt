package fr.moxxie.prefixblock.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.moxxie.prefixblock.ui.navigation.PrefixBlockNavHost
import fr.moxxie.prefixblock.ui.navigation.TopLevelDestination
import fr.moxxie.prefixblock.ui.navigation.isOnTopLevel
import fr.moxxie.prefixblock.ui.navigation.navigateToTopLevel

/** Structure générale de l'application : barre de navigation inférieure + contenu. */
@Composable
fun PrefixBlockRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination.isOnTopLevel(destination)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination.route) },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    if (selected) destination.selectedIcon else destination.icon,
                                ),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // Les insets déjà appliqués ici sont consommés pour que les Scaffold des écrans
        // n'ajoutent pas une seconde fois la marge de la barre d'état ou de navigation.
        PrefixBlockNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}
