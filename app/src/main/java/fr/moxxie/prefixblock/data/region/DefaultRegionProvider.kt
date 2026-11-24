package fr.moxxie.prefixblock.data.region

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Région utilisée pour interpréter les numéros au format national (ex. `0690…` en Guadeloupe).
 */
fun interface DefaultRegionProvider {
    /** Code pays ISO 3166-1 alpha-2 (casse quelconque), ou `null` s'il est inconnu. */
    fun currentRegion(): String?
}

/**
 * Pays du réseau mobile, puis de la carte SIM, puis de la langue du système.
 * Aucune de ces lectures ne nécessite de permission.
 */
@Singleton
class TelephonyRegionProvider @Inject constructor(
    @ApplicationContext context: Context,
) : DefaultRegionProvider {

    private val telephony: TelephonyManager? = context.getSystemService(TelephonyManager::class.java)

    override fun currentRegion(): String? =
        telephony?.networkCountryIso.nonBlank()
            ?: telephony?.simCountryIso.nonBlank()
            ?: Locale.getDefault().country.nonBlank()

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
}
