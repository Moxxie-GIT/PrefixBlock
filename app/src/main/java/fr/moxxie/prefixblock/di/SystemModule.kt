package fr.moxxie.prefixblock.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.data.region.DefaultRegionProvider
import fr.moxxie.prefixblock.data.region.TelephonyRegionProvider
import fr.moxxie.prefixblock.data.role.AndroidCallScreeningRoleRepository
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository

/** Liaisons vers les services système Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds
    abstract fun bindCallScreeningRoleRepository(
        impl: AndroidCallScreeningRoleRepository,
    ): CallScreeningRoleRepository

    @Binds
    abstract fun bindDefaultRegionProvider(impl: TelephonyRegionProvider): DefaultRegionProvider
}
