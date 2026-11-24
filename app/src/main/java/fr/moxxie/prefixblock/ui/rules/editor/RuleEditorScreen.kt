package fr.moxxie.prefixblock.ui.rules.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.rules.PatternError
import fr.moxxie.prefixblock.rules.PatternValidation
import fr.moxxie.prefixblock.rules.PrefixCoverage
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.ui.components.appPluralStringResource
import fr.moxxie.prefixblock.ui.components.appLocale
import fr.moxxie.prefixblock.ui.components.countryName
import fr.moxxie.prefixblock.ui.components.labelRes
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

@Composable
fun RuleEditorScreen(
    ruleId: Long?,
    list: RuleList,
    onClose: () -> Unit,
    viewModel: RuleEditorViewModel = hiltViewModel<RuleEditorViewModel, RuleEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(ruleId, list) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.done) { if (uiState.done) onClose() }

    RuleEditorContent(
        uiState = uiState,
        onClose = onClose,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onTypeChange = viewModel::onTypeChange,
        onInputChange = viewModel::onInputChange,
        onLabelChange = viewModel::onLabelChange,
        onPriorityChange = viewModel::onPriorityChange,
        onEnabledChange = viewModel::onEnabledChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorContent(
    uiState: RuleEditorUiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTypeChange: (RuleType) -> Unit,
    onInputChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onPriorityChange: (Boolean) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(uiState.titleRes())) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = uiState.canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.rule_editor_type), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RuleType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = uiState.type == type,
                        onClick = { onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, RuleType.entries.size),
                        enabled = !uiState.loading,
                    ) {
                        Text(stringResource(if (type == RuleType.REGEX) R.string.rule_type_regex_short else type.labelRes))
                    }
                }
            }

            PatternField(uiState, onInputChange)
            uiState.coverage?.let { CoverageInfo(it, uiState.list) }

            OutlinedTextField(
                value = uiState.label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(R.string.rule_editor_label)) },
                singleLine = true,
                enabled = !uiState.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                if (uiState.priorityApplies) {
                    SwitchRow(
                        title = stringResource(R.string.rule_priority),
                        body = stringResource(R.string.rule_editor_priority_body),
                        checked = uiState.priority,
                        onCheckedChange = onPriorityChange,
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.rule_editor_enabled),
                    body = null,
                    checked = uiState.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            if (!uiState.isNew && !uiState.loading) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                    Text(stringResource(R.string.rule_editor_delete), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.rule_editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.rule_editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PatternField(uiState: RuleEditorUiState, onInputChange: (String) -> Unit) {
    val validation = uiState.validation
    val error = when {
        validation is PatternValidation.Invalid -> stringResource(validation.error.messageRes())
        uiState.saveError == SaveError.DUPLICATE -> stringResource(R.string.save_error_duplicate)
        uiState.saveError == SaveError.NOT_FOUND -> stringResource(R.string.save_error_not_found)
        else -> null
    }
    val normalized = (validation as? PatternValidation.Valid)?.pattern?.takeIf { it != uiState.input.trim() }
    val supporting = error
        ?: normalized?.let { stringResource(R.string.rule_editor_normalized, it) }
        ?: stringResource(uiState.type.hintRes())
    val isRegex = uiState.type == RuleType.REGEX

    OutlinedTextField(
        value = uiState.input,
        onValueChange = onInputChange,
        label = { Text(stringResource(uiState.type.labelRes)) },
        supportingText = { Text(supporting) },
        isError = error != null,
        singleLine = true,
        enabled = !uiState.loading,
        textStyle = if (isRegex) TextStyle(fontFamily = FontFamily.Monospace) else TextStyle.Default,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isRegex) KeyboardType.Ascii else KeyboardType.Phone,
            autoCorrectEnabled = false,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Pays couverts par le préfixe saisi, pour éviter de bloquer tout un pays par mégarde (ex. +44). */
@Composable
private fun CoverageInfo(coverage: PrefixCoverage, list: RuleList) {
    val countryCode = coverage.countryCode
    val text = when {
        countryCode == null -> stringResource(R.string.rule_editor_coverage_multiple)
        coverage.regionCodes.all { it == NON_GEOGRAPHIC_REGION } ->
            stringResource(R.string.rule_editor_coverage_non_geo, countryCode)
        else -> stringResource(R.string.rule_editor_coverage, countryCode, countryNames(coverage.regionCodes))
    }
    val wholeCountryBlocked = list == RuleList.BLACKLIST && (coverage.wholeCountryCode || countryCode == null)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        if (wholeCountryBlocked) {
            Text(
                text = stringResource(R.string.rule_editor_coverage_whole_country),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun countryNames(regionCodes: List<String>): String {
    val locale = appLocale()
    val names = regionCodes
        .filter { it != NON_GEOGRAPHIC_REGION }
        .map { countryName(it, locale) }
    val shown = names.take(MAX_COUNTRY_NAMES).joinToString(", ")
    val hidden = names.size - MAX_COUNTRY_NAMES
    return if (hidden > 0) appPluralStringResource(R.plurals.rule_editor_coverage_more, hidden, shown, hidden) else shown
}

@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = body?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

private fun RuleEditorUiState.titleRes(): Int = when {
    !isNew -> R.string.rule_editor_title_edit
    list == RuleList.WHITELIST -> R.string.rule_editor_title_new_whitelist
    else -> R.string.rule_editor_title_new_blacklist
}

private fun RuleType.hintRes(): Int = when (this) {
    RuleType.NUMBER -> R.string.rule_editor_hint_number
    RuleType.PREFIX -> R.string.rule_editor_hint_prefix
    RuleType.REGEX -> R.string.rule_editor_hint_regex
}

private fun PatternError.messageRes(): Int = when (this) {
    PatternError.EMPTY -> R.string.pattern_error_empty
    PatternError.PREFIX_FORMAT -> R.string.pattern_error_prefix_format
    PatternError.PREFIX_TOO_LONG -> R.string.pattern_error_prefix_too_long
    PatternError.NUMBER_FORMAT -> R.string.pattern_error_number_format
    PatternError.REGEX_SYNTAX -> R.string.pattern_error_regex_syntax
    PatternError.REGEX_TOO_LONG -> R.string.pattern_error_regex_too_long
    PatternError.REGEX_TOO_BROAD -> R.string.pattern_error_regex_too_broad
}

private const val NON_GEOGRAPHIC_REGION = "001"
private const val MAX_COUNTRY_NAMES = 4

@Preview
@Composable
private fun RuleEditorPreview() {
    PrefixBlockTheme {
        RuleEditorContent(
            uiState = RuleEditorUiState(
                list = RuleList.BLACKLIST,
                input = "0044",
                validation = PatternValidation.Valid("+44"),
                coverage = PrefixCoverage(44, listOf("GB", "GG", "IM", "JE"), wholeCountryCode = true),
            ),
            onClose = {}, onSave = {}, onDelete = {}, onTypeChange = {}, onInputChange = {},
            onLabelChange = {}, onPriorityChange = {}, onEnabledChange = {},
        )
    }
}
