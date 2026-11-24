package fr.moxxie.prefixblock.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.data.backup.ImportResult
import fr.moxxie.prefixblock.ui.components.appPluralStringResource
import fr.moxxie.prefixblock.ui.components.SectionHeader
import fr.moxxie.prefixblock.ui.components.openDefaultAppsSettings
import fr.moxxie.prefixblock.ui.components.rememberScreeningRoleRequest
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRoleStatus() }
    val context = LocalContext.current
    val requestScreeningRole = rememberScreeningRoleRequest(onResult = viewModel::refreshRoleStatus)

    // Sélecteur de documents Android : aucune permission de stockage n'est nécessaire.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME)) { uri ->
        uri?.let { viewModel.export(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import(it.toString()) }
    }

    SettingsContent(
        uiState = uiState,
        onScreeningRoleClick = {
            if (uiState.screeningRoleHeld) context.openDefaultAppsSettings() else requestScreeningRole()
        },
        onOpenDefaultApps = context::openDefaultAppsSettings,
        onDynamicColorChange = viewModel::setDynamicColor,
        onExport = { exportLauncher.launch(viewModel.exportFileName) },
        // Certains gestionnaires de fichiers ne reconnaissent pas le type JSON : on accepte aussi le texte.
        onImport = { importLauncher.launch(arrayOf(JSON_MIME, "text/plain", "application/octet-stream")) },
        onMessageShown = viewModel::messageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onScreeningRoleClick: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { backupMessageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SettingsSectionHeader(stringResource(R.string.settings_section_protection)) }
            item {
                SettingsItem(
                    icon = R.drawable.ic_shield,
                    title = stringResource(R.string.settings_screening_role),
                    body = stringResource(
                        if (uiState.screeningRoleHeld) R.string.settings_screening_role_held
                        else R.string.settings_screening_role_not_held,
                    ),
                    onClick = onScreeningRoleClick,
                )
            }
            item {
                SettingsItem(
                    icon = R.drawable.ic_settings,
                    title = stringResource(R.string.settings_default_apps),
                    body = stringResource(R.string.settings_default_apps_body),
                    onClick = onOpenDefaultApps,
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_backup)) }
            item {
                SettingsItem(
                    icon = R.drawable.ic_upload,
                    title = stringResource(R.string.settings_export),
                    body = stringResource(R.string.settings_export_body),
                    onClick = onExport.takeUnless { uiState.busy },
                )
            }
            item {
                SettingsItem(
                    icon = R.drawable.ic_download,
                    title = stringResource(R.string.settings_import),
                    body = stringResource(R.string.settings_import_body),
                    onClick = onImport.takeUnless { uiState.busy },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item { SettingsSectionHeader(stringResource(R.string.settings_section_appearance)) }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                        supportingContent = { Text(stringResource(R.string.settings_dynamic_color_body)) },
                        leadingContent = { Icon(painterResource(R.drawable.ic_palette), contentDescription = null) },
                        trailingContent = { Switch(checked = uiState.dynamicColor, onCheckedChange = null) },
                        modifier = Modifier.toggleable(
                            value = uiState.dynamicColor,
                            role = Role.Switch,
                            onValueChange = onDynamicColorChange,
                        ),
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
            item {
                SettingsItem(
                    icon = R.drawable.ic_verified_user,
                    title = stringResource(R.string.settings_privacy_offline),
                    body = stringResource(R.string.settings_privacy_offline_body),
                )
            }
            item {
                SettingsItem(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.settings_permissions),
                    body = stringResource(R.string.settings_permissions_body),
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                SettingsItem(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.settings_version),
                    body = uiState.versionName,
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(icon: Int, title: String, body: String, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
}

@Composable
private fun backupMessageText(message: BackupMessage): String = when (message) {
    is BackupMessage.Exported -> appPluralStringResource(R.plurals.settings_exported, message.count, message.count)
    BackupMessage.ExportFailed -> stringResource(R.string.settings_export_failed)
    is BackupMessage.Imported -> when (val result = message.result) {
        is ImportResult.Imported -> stringResource(
            R.string.settings_import_report,
            appPluralStringResource(R.plurals.settings_import_added, result.added, result.added),
            appPluralStringResource(R.plurals.settings_import_present, result.alreadyPresent, result.alreadyPresent),
            appPluralStringResource(R.plurals.settings_import_invalid, result.invalid, result.invalid),
        )
        ImportResult.NotABackup -> stringResource(R.string.settings_import_not_backup)
        is ImportResult.UnsupportedVersion -> stringResource(R.string.settings_import_unsupported, result.version)
        ImportResult.ReadError -> stringResource(R.string.settings_import_read_error)
    }
}

private const val JSON_MIME = "application/json"

@Preview
@Composable
private fun SettingsPreview() {
    PrefixBlockTheme {
        SettingsContent(SettingsUiState(screeningRoleHeld = true), {}, {}, {}, {}, {}, {})
    }
}
