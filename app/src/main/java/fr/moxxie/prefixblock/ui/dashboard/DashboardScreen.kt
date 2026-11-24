package fr.moxxie.prefixblock.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.ui.components.SectionHeader
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

@Composable
fun DashboardScreen(
    onConfigureProtection: () -> Unit,
    onOpenBlacklist: () -> Unit,
    onOpenWhitelist: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshProtectionStatus() }

    DashboardContent(
        uiState = uiState,
        onConfigureProtection = onConfigureProtection,
        onOpenBlacklist = onOpenBlacklist,
        onOpenWhitelist = onOpenWhitelist,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onConfigureProtection: () -> Unit,
    onOpenBlacklist: () -> Unit,
    onOpenWhitelist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProtectionStatusCard(
                    active = uiState.protectionActive,
                    onConfigure = onConfigureProtection,
                )
            }
            item {
                SectionHeader(stringResource(R.string.dashboard_blocked_calls))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(uiState.blockedToday, R.string.stats_today, Modifier.weight(1f))
                    StatTile(uiState.blockedThisWeek, R.string.stats_this_week, Modifier.weight(1f))
                    StatTile(uiState.blockedThisMonth, R.string.stats_this_month, Modifier.weight(1f))
                }
            }
            item {
                SectionHeader(stringResource(R.string.dashboard_recent_calls))
                Text(
                    text = stringResource(R.string.dashboard_recent_calls_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SectionHeader(stringResource(R.string.dashboard_quick_access))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortcutCard(
                        icon = R.drawable.ic_block,
                        title = R.string.rules_blacklist,
                        body = R.string.dashboard_blacklist_shortcut_body,
                        onClick = onOpenBlacklist,
                    )
                    ShortcutCard(
                        icon = R.drawable.ic_verified_user,
                        title = R.string.rules_whitelist,
                        body = R.string.dashboard_whitelist_shortcut_body,
                        onClick = onOpenWhitelist,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectionStatusCard(active: Boolean, onConfigure: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) colors.primaryContainer else colors.errorContainer,
            contentColor = if (active) colors.onPrimaryContainer else colors.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(if (active) R.drawable.ic_verified_user else R.drawable.ic_gpp_bad),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(
                        if (active) R.string.dashboard_protection_active_title
                        else R.string.dashboard_protection_inactive_title,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = stringResource(
                    if (active) R.string.dashboard_protection_active_body
                    else R.string.dashboard_protection_inactive_body,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!active) {
                Button(
                    onClick = onConfigure,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError,
                    ),
                ) {
                    Text(stringResource(R.string.dashboard_protection_configure))
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: Int, @StringRes label: Int, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShortcutCard(icon: Int, @StringRes title: Int, @StringRes body: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(title)) },
            supportingContent = { Text(stringResource(body)) },
            leadingContent = { Icon(painterResource(icon), contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Preview(name = "Protection inactive")
@Composable
private fun DashboardInactivePreview() {
    PrefixBlockTheme {
        DashboardContent(DashboardUiState(protectionActive = false), {}, {}, {})
    }
}

@Preview(name = "Protection active (sombre)", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardActivePreview() {
    PrefixBlockTheme {
        DashboardContent(
            DashboardUiState(protectionActive = true, blockedToday = 3, blockedThisWeek = 17, blockedThisMonth = 52),
            {}, {}, {},
        )
    }
}
