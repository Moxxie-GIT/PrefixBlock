package fr.moxxie.prefixblock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.BuildConfig
import fr.moxxie.prefixblock.data.backup.ImportResult
import fr.moxxie.prefixblock.data.backup.RulesBackupRepository
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import fr.moxxie.prefixblock.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Retour d'une sauvegarde, affiché une fois (snackbar). */
sealed interface BackupMessage {
    data class Exported(val count: Int) : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val result: ImportResult) : BackupMessage
}

data class SettingsUiState(
    val screeningRoleHeld: Boolean = false,
    val dynamicColor: Boolean = false,
    val busy: Boolean = false,
    val message: BackupMessage? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val roleRepository: CallScreeningRoleRepository,
    private val settings: SettingsRepository,
    private val backup: RulesBackupRepository,
    private val clock: Clock,
) : ViewModel() {

    private val roleHeld = MutableStateFlow(roleRepository.isRoleHeld())
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<BackupMessage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(roleHeld, settings.dynamicColor, busy, message) {
            held, dynamic, busy, message ->
        SettingsUiState(screeningRoleHeld = held, dynamicColor = dynamic, busy = busy, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(screeningRoleHeld = roleHeld.value))

    /** Nom proposé pour le fichier d'export, daté pour garder plusieurs sauvegardes. */
    val exportFileName: String get() = "prefixblock-regles-${LocalDate.now(clock)}.json"

    fun refreshRoleStatus() {
        roleHeld.value = roleRepository.isRoleHeld()
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun export(uri: String) = runBackup {
        backup.export(uri)?.let { BackupMessage.Exported(it) } ?: BackupMessage.ExportFailed
    }

    fun import(uri: String) = runBackup { BackupMessage.Imported(backup.import(uri)) }

    fun messageShown() {
        message.value = null
    }

    private fun runBackup(action: suspend () -> BackupMessage) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            message.value = action()
            busy.value = false
        }
    }
}
