package fr.moxxie.prefixblock.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Préférences de l'utilisateur (DataStore, fichier local non sauvegardé dans le cloud). */
interface SettingsRepository {
    /** Couleurs tirées du fond d'écran (Material You, Android 12+). Désactivé par défaut. */
    val dynamicColor: Flow<Boolean>
    suspend fun setDynamicColor(enabled: Boolean)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    override val dynamicColor: Flow<Boolean> = dataStore.data
        // Fichier illisible : on garde les valeurs par défaut plutôt que de planter.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DYNAMIC_COLOR] ?: false }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    private companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
