package fr.moxxie.prefixblock.ui.rules

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.rules.DefaultRules
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.ui.components.EmptyState
import fr.moxxie.prefixblock.ui.components.labelRes
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

private enum class RulesTab(
    @StringRes val label: Int,
    @StringRes val hint: Int,
    @DrawableRes val emptyIcon: Int,
    @StringRes val emptyTitle: Int,
    @StringRes val emptyBody: Int,
) {
    BLACKLIST(
        label = R.string.rules_blacklist,
        hint = R.string.rules_blacklist_hint,
        emptyIcon = R.drawable.ic_block,
        emptyTitle = R.string.rules_blacklist_empty_title,
        emptyBody = R.string.rules_blacklist_empty_body,
    ),
    WHITELIST(
        label = R.string.rules_whitelist,
        hint = R.string.rules_whitelist_hint,
        emptyIcon = R.drawable.ic_verified_user,
        emptyTitle = R.string.rules_whitelist_empty_title,
        emptyBody = R.string.rules_whitelist_empty_body,
    ),
}

@Composable
fun RulesScreen(
    initialShowWhitelist: Boolean,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RulesContent(
        uiState = uiState,
        initialShowWhitelist = initialShowWhitelist,
        onToggleEnabled = viewModel::setEnabled,
    )
}

/** Gestion des règles : un onglet par liste (noire / blanche). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesContent(
    uiState: RulesUiState,
    initialShowWhitelist: Boolean,
    onToggleEnabled: (Rule, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (initialShowWhitelist) RulesTab.WHITELIST.ordinal else RulesTab.BLACKLIST.ordinal)
    }
    val tab = RulesTab.entries[selectedTab]
    val rules = when (tab) {
        RulesTab.BLACKLIST -> uiState.blacklist
        RulesTab.WHITELIST -> uiState.whitelist
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_rules)) }) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                RulesTab.entries.forEach { entry ->
                    Tab(
                        selected = entry == tab,
                        onClick = { selectedTab = entry.ordinal },
                        text = { Text(stringResource(entry.label)) },
                    )
                }
            }
            when {
                uiState.loading -> Unit
                rules.isEmpty() -> EmptyState(
                    icon = tab.emptyIcon,
                    title = stringResource(tab.emptyTitle),
                    body = stringResource(tab.emptyBody),
                )
                else -> LazyColumn {
                    item {
                        Text(
                            text = stringResource(tab.hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(rule = rule, onToggleEnabled = onToggleEnabled)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleRow(rule: Rule, onToggleEnabled: (Rule, Boolean) -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = rule.pattern,
                fontFamily = if (rule.type == RuleType.REGEX) FontFamily.Monospace else null,
            )
        },
        supportingContent = { Text(ruleDescription(rule)) },
        trailingContent = { Switch(checked = rule.enabled, onCheckedChange = null) },
        modifier = Modifier.toggleable(
            value = rule.enabled,
            role = Role.Switch,
            onValueChange = { onToggleEnabled(rule, it) },
        ),
    )
}

@Composable
private fun ruleDescription(rule: Rule): String {
    val type = stringResource(rule.type.labelRes)
    val priority = if (rule.priority) stringResource(R.string.rule_priority) else null
    return listOfNotNull(rule.label, type, priority).joinToString(" · ")
}

@Preview
@Composable
private fun RulesPreview() {
    val rules = DefaultRules.initial.mapIndexed { i, rule -> rule.copy(id = i + 1L) }
    PrefixBlockTheme {
        RulesContent(
            uiState = RulesUiState(
                blacklist = rules.filter { it.list == RuleList.BLACKLIST },
                whitelist = rules.filter { it.list == RuleList.WHITELIST },
                loading = false,
            ),
            initialShowWhitelist = false,
            onToggleEnabled = { _, _ -> },
        )
    }
}
