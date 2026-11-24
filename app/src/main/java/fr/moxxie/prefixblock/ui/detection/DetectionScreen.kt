package fr.moxxie.prefixblock.ui.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.ui.components.EmptyState

/**
 * Détection de campagnes de spam (phase 5). Fonctionne exclusivement en mode observation :
 * aucune détection ne bloque un appel sans règle explicite validée par l'utilisateur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_detection)) }) },
        modifier = modifier,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_visibility), contentDescription = null)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.detection_observation_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.detection_observation_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            EmptyState(
                icon = R.drawable.ic_radar,
                title = stringResource(R.string.detection_empty_title),
                body = stringResource(R.string.detection_empty_body),
            )
        }
    }
}
