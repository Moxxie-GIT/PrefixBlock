package fr.moxxie.prefixblock.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleValidator
import java.time.Clock
import javax.inject.Singleton

/** Moteur de règles (module `:core:rules`) et dépôt associé. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RulesModule {

    @Binds
    abstract fun bindRuleRepository(impl: RoomRuleRepository): RuleRepository

    companion object {
        @Provides
        @Singleton
        fun providePhoneNumberNormalizer(): PhoneNumberNormalizer = PhoneNumberNormalizer()

        @Provides
        @Singleton
        fun provideRuleValidator(normalizer: PhoneNumberNormalizer): RuleValidator = RuleValidator(normalizer)

        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
