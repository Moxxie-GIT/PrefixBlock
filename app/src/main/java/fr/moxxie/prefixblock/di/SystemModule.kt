package fr.moxxie.prefixblock.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.data.region.DefaultRegionProvider
import fr.moxxie.prefixblock.data.region.TelephonyRegionProvider
import fr.moxxie.prefixblock.data.role.AndroidCallScreeningRoleRepository
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import java.util.Locale
import javax.inject.Qualifier

/**
 * Langue des textes de l'interface (`R.string.app_locale`), qui peut différer de celle du système :
 * sert aux calculs qui en dépendent hors de Compose (ex. premier jour de la semaine).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UiLocale

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

    companion object {
        @Provides
        @UiLocale
        fun provideUiLocale(@ApplicationContext context: Context): Locale =
            Locale.forLanguageTag(context.getString(R.string.app_locale))
    }
}
