package fr.moxxie.prefixblock.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.data.history.BlockedCall
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.ui.components.DisplayedCall
import fr.moxxie.prefixblock.ui.components.EmptyState
import fr.moxxie.prefixblock.ui.components.appLocale
import fr.moxxie.prefixblock.ui.components.formatCallDateTime
import fr.moxxie.prefixblock.ui.components.labelRes
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onPeriodChange = viewModel::onPeriodChange,
        onAllowNumber = viewModel::allowNumber,
        onAllowCountryCode = viewModel::allowCountryCode,
        onDelete = { viewModel.delete(it.call) },
        onDeleteAll = viewModel::deleteAll,
        onMessageShown = viewModel::messageShown,
    )
}

/** Historique des appels rejetés : recherche, filtre par date, suppression, autorisation directe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onAllowNumber: (DisplayedCall) -> Unit,
    onAllowCountryCode: (DisplayedCall) -> Unit,
    onDelete: (DisplayedCall) -> Unit,
    onDeleteAll: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { messageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    val historyEmpty = !uiState.loading && uiState.calls.isEmpty() && !uiState.filtered

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                actions = {
                    if (uiState.calls.isNotEmpty() && !uiState.filtered) {
                        IconButton(onClick = { confirmDeleteAll = true }) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.history_delete_all))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (historyEmpty) {
                EmptyState(
                    icon = R.drawable.ic_phone_disabled,
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                )
                return@Column
            }
            Filters(uiState, onQueryChange, onPeriodChange)
            when {
                uiState.loading -> Unit
                uiState.calls.isEmpty() -> EmptyState(
                    icon = R.drawable.ic_search,
                    title = stringResource(R.string.history_filtered_empty_title),
                    body = stringResource(R.string.history_filtered_empty_body),
                )
                else -> LazyColumn {
                    items(uiState.calls, key = { it.call.id }) { call ->
                        CallRow(call, onAllowNumber, onAllowCountryCode, onDelete)
                    }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.history_delete_all_confirm_title)) },
            text = { Text(stringResource(R.string.history_delete_all_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDeleteAll = false; onDeleteAll() }) {
                    Text(stringResource(R.string.history_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Filters(
    uiState: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.history_search)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            HistoryPeriod.entries.forEach { period ->
                FilterChip(
                    selected = uiState.period == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(stringResource(period.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun CallRow(
    call: DisplayedCall,
    onAllowNumber: (DisplayedCall) -> Unit,
    onAllowCountryCode: (DisplayedCall) -> Unit,
    onDelete: (DisplayedCall) -> Unit,
) {
    val locale = appLocale()
    val date = remember(call.call.timestamp, locale) { formatCallDateTime(call.call.timestamp, locale) }
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        overlineContent = { Text(date) },
        headlineContent = { Text(call.displayNumber) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.dashboard_recent_call_rule,
                    call.call.rulePattern,
                    stringResource(call.call.ruleType.labelRes),
                ),
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.history_more_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_allow_number)) },
                        onClick = { menuOpen = false; onAllowNumber(call) },
                    )
                    call.call.countryCode?.let { code ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_allow_prefix, "+$code")) },
                            onClick = { menuOpen = false; onAllowCountryCode(call) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete)) },
                        onClick = { menuOpen = false; onDelete(call) },
                    )
                }
            }
        },
    )
}

@Composable
private fun messageText(message: HistoryMessage): String = when (message) {
    is HistoryMessage.NumberAllowed -> stringResource(R.string.history_number_allowed, message.number)
    is HistoryMessage.PrefixAllowed -> stringResource(R.string.history_prefix_allowed, message.prefix)
    HistoryMessage.AlreadyAllowed -> stringResource(R.string.history_already_allowed)
    HistoryMessage.CannotAllow -> stringResource(R.string.history_cannot_allow)
}

@StringRes
private fun HistoryPeriod.labelRes(): Int = when (this) {
    HistoryPeriod.ALL -> R.string.history_period_all
    HistoryPeriod.TODAY -> R.string.history_period_today
    HistoryPeriod.LAST_7_DAYS -> R.string.history_period_7_days
    HistoryPeriod.LAST_30_DAYS -> R.string.history_period_30_days
}

@Preview
@Composable
private fun HistoryPreview() {
    val calls = listOf(
        DisplayedCall(BlockedCall(1, 1_795_537_516_768, "+870773111632", 870, 1, "+870", RuleType.PREFIX), "+870 773 111 632"),
        DisplayedCall(BlockedCall(2, 1_795_437_516_768, "+67455512345", 674, 2, "+674", RuleType.PREFIX), "+674 555 1234"),
    )
    PrefixBlockTheme {
        HistoryContent(HistoryUiState(calls = calls, loading = false), {}, {}, {}, {}, {}, {}, {})
    }
}
