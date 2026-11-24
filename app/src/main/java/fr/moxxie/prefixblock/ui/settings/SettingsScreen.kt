package fr.moxxie.prefixblock.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.ui.components.SectionHeader

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRoleStatus() }
    SettingsContent(uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        modifier = modifier,
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SettingsSectionHeader(stringResource(R.string.settings_section_protection)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_screening_role)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (uiState.screeningRoleHeld) R.string.settings_screening_role_held
                                else R.string.settings_screening_role_not_held,
                            ),
                        )
                    },
                    leadingContent = { Icon(painterResource(R.drawable.ic_shield), contentDescription = null) },
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_privacy_offline)) },
                    supportingContent = { Text(stringResource(R.string.settings_privacy_offline_body)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_verified_user), contentDescription = null) },
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    supportingContent = { Text(uiState.versionName) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
}
