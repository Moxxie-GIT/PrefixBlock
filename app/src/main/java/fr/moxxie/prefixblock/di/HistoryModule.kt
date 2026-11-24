package fr.moxxie.prefixblock.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.data.history.BlockedCallRepository
import fr.moxxie.prefixblock.data.history.RoomBlockedCallRepository

/** Historique des appels rejetés. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {

    @Binds
    abstract fun bindBlockedCallRepository(impl: RoomBlockedCallRepository): BlockedCallRepository
}
