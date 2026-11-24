package fr.moxxie.prefixblock.rules

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

/** Numéros d'exemple valides fournis par les métadonnées de libphonenumber. */
internal object TestNumbers {
    private val util = PhoneNumberUtil.getInstance()

    fun example(region: String): PhoneNumber = util.getExampleNumber(region)
    fun exampleMobile(region: String): PhoneNumber =
        util.getExampleNumberForType(region, PhoneNumberUtil.PhoneNumberType.MOBILE)

    fun nonGeo(countryCode: Int): PhoneNumber = util.getExampleNumberForNonGeoEntity(countryCode)

    fun e164(number: PhoneNumber): String = util.format(number, PhoneNumberFormat.E164)
    fun international(number: PhoneNumber): String = util.format(number, PhoneNumberFormat.INTERNATIONAL)
    fun national(number: PhoneNumber): String = util.format(number, PhoneNumberFormat.NATIONAL)

    /** Numéro au format international, tel qu'Android le présente le plus souvent. */
    fun presented(international: String) = NormalizedNumber(
        raw = international,
        international = international,
        countryCode = null,
    )
}
