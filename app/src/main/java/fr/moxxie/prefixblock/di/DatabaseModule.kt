package fr.moxxie.prefixblock.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.data.db.BlockedCallDao
import fr.moxxie.prefixblock.data.db.DefaultRulesCallback
import fr.moxxie.prefixblock.data.db.PrefixBlockDatabase
import fr.moxxie.prefixblock.data.db.RuleDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrefixBlockDatabase =
        Room.databaseBuilder(context, PrefixBlockDatabase::class.java, PrefixBlockDatabase.NAME)
            .addCallback(DefaultRulesCallback())
            .build()

    @Provides
    fun provideRuleDao(database: PrefixBlockDatabase): RuleDao = database.ruleDao()

    @Provides
    fun provideBlockedCallDao(database: PrefixBlockDatabase): BlockedCallDao = database.blockedCallDao()
}
