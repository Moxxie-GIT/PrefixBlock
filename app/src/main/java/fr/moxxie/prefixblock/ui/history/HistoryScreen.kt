package fr.moxxie.prefixblock.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.ui.components.EmptyState

/** Historique des appels rejetés (alimenté par Room à partir de la phase 2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_history)) }) },
        modifier = modifier,
    ) { padding ->
        EmptyState(
            icon = R.drawable.ic_phone_disabled,
            title = stringResource(R.string.history_empty_title),
            body = stringResource(R.string.history_empty_body),
            modifier = Modifier.padding(padding),
        )
    }
}
