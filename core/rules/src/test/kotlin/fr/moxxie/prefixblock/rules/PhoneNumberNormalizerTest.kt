package fr.moxxie.prefixblock.rules

import fr.moxxie.prefixblock.rules.TestNumbers.e164
import fr.moxxie.prefixblock.rules.TestNumbers.example
import fr.moxxie.prefixblock.rules.TestNumbers.exampleMobile
import fr.moxxie.prefixblock.rules.TestNumbers.international
import fr.moxxie.prefixblock.rules.TestNumbers.national
import fr.moxxie.prefixblock.rules.TestNumbers.nonGeo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {

    private val normalizer = PhoneNumberNormalizer()
    private val inmarsat = nonGeo(870)
    private val inmarsatE164 = e164(inmarsat)

    @Test
    fun `absent or hidden number gives null`() {
        assertNull(normalizer.normalize(null, "GP"))
        assertNull(normalizer.normalize("", "GP"))
        assertNull(normalizer.normalize("   ", "GP"))
    }

    @Test
    fun `international form with separators is kept`() {
        val result = normalizer.normalize(international(inmarsat), "GP")!!
        assertEquals(inmarsatE164, result.international)
        assertEquals(870, result.countryCode)
    }

    @Test
    fun `dashes dots and parentheses are ignored`() {
        assertEquals("+447911123456", normalizer.normalize("+44-7911-123.456", "GP")?.international)
        assertEquals("+12125550123", normalizer.normalize("+1 (212) 555-0123", "GP")?.international)
    }

    @Test
    fun `international form is kept even when the number is not attributed`() {
        // Les numéros Wangiri sont souvent usurpés : le préfixe présenté doit rester filtrable.
        val result = normalizer.normalize("+8701234", "GP")!!
        assertEquals("+8701234", result.international)
        assertEquals(870, result.countryCode)
    }

    @Test
    fun `00 international dialling prefix is resolved in Guadeloupe`() {
        val presented = "00" + inmarsatE164.removePrefix("+")
        assertEquals(inmarsatE164, normalizer.normalize(presented, "GP")?.international)
    }

    @Test
    fun `011 international dialling prefix is resolved in the United States`() {
        val presented = "011 " + inmarsatE164.removePrefix("+")
        assertEquals(inmarsatE164, normalizer.normalize(presented, "US")?.international)
    }

    @Test
    fun `national Guadeloupe number gets +590`() {
        val mobile = exampleMobile("GP")
        val result = normalizer.normalize(national(mobile), "GP")!!
        assertEquals(e164(mobile), result.international)
        assertEquals(590, result.countryCode)
    }

    @Test
    fun `national French number gets +33`() {
        assertEquals("+33612345678", normalizer.normalize("06 12 34 56 78", "FR")?.international)
    }

    @Test
    fun `region is case insensitive as returned by TelephonyManager`() {
        assertEquals("+33612345678", normalizer.normalize("0612345678", "fr")?.international)
    }

    @Test
    fun `international number presented without plus is inferred only when valid`() {
        val presented = inmarsatE164.removePrefix("+")
        assertEquals(inmarsatE164, normalizer.normalize(presented, "GP")?.international)
    }

    @Test
    fun `short or ambiguous digits are never guessed`() {
        assertNull(normalizer.normalize("12345", "GP")!!.international)
        assertNull(normalizer.normalize("3900", "GP")!!.international)
    }

    @Test
    fun `unknown region still resolves the 00 prefix`() {
        assertEquals("+447911123456", normalizer.normalize("00447911123456", null)?.international)
        assertEquals("+447911123456", normalizer.normalize("00447911123456", "XX")?.international)
    }

    @Test
    fun `unknown region cannot resolve a national number`() {
        assertNull(normalizer.normalize("0690123456", null)!!.international)
    }

    @Test
    fun `non dialable presentations are unresolved but keep their digits`() {
        val result = normalizer.normalize("*31#", "GP")!!
        assertNull(result.international)
        assertEquals("31", result.matchKey)
        assertNull(normalizer.normalize("Anonymous", "GP")!!.international)
    }

    @Test
    fun `NANP numbers share country code 1 whatever the territory`() {
        assertEquals(1, normalizer.normalize(international(example("JM")), "GP")?.countryCode)
        assertEquals(1, normalizer.normalize(international(example("US")), "GP")?.countryCode)
        assertEquals(1, normalizer.normalize(international(example("CA")), "GP")?.countryCode)
    }

    @Test
    fun `absurdly long input is unresolved without crashing`() {
        assertNull(normalizer.normalize("+" + "9".repeat(40), "GP")!!.international)
    }
}
