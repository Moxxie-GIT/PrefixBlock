package fr.moxxie.prefixblock.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.data.backup.BackupFiles
import fr.moxxie.prefixblock.data.backup.ContentResolverBackupFiles
import fr.moxxie.prefixblock.data.settings.DataStoreSettingsRepository
import fr.moxxie.prefixblock.data.settings.SettingsRepository

/** Préférences et sauvegarde des règles. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindBackupFiles(impl: ContentResolverBackupFiles): BackupFiles
}
