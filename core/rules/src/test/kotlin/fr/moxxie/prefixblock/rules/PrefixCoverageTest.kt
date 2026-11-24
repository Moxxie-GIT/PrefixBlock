package fr.moxxie.prefixblock.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefixCoverageTest {

    private val normalizer = PhoneNumberNormalizer()

    @Test
    fun `country code alone covers the whole country and its dependencies`() {
        val uk = normalizer.prefixCoverage("+44")!!
        assertEquals(44, uk.countryCode)
        assertTrue(uk.wholeCountryCode)
        assertTrue(uk.regionCodes.containsAll(listOf("GB", "GG", "IM", "JE")))
    }

    @Test
    fun `longer prefix covers only part of the country`() {
        val ukMobiles = normalizer.prefixCoverage("+4475")!!
        assertEquals(44, ukMobiles.countryCode)
        assertFalse(ukMobiles.wholeCountryCode)
    }

    @Test
    fun `non geographic service is reported as region 001`() {
        val inmarsat = normalizer.prefixCoverage("+870")!!
        assertEquals(870, inmarsat.countryCode)
        assertEquals(listOf("001"), inmarsat.regionCodes)
    }

    @Test
    fun `NANP prefix belongs to country code 1`() {
        val jamaica = normalizer.prefixCoverage("+1876")!!
        assertEquals(1, jamaica.countryCode)
        assertFalse(jamaica.wholeCountryCode)
        assertTrue("JM" in jamaica.regionCodes)
    }

    @Test
    fun `prefix shorter than a country code spans several countries`() {
        val europe = normalizer.prefixCoverage("+3")!!
        assertNull(europe.countryCode)
        assertFalse(europe.wholeCountryCode)
    }

    @Test
    fun `malformed prefix has no coverage`() {
        assertNull(normalizer.prefixCoverage("870"))
        assertNull(normalizer.prefixCoverage("+"))
        assertNull(normalizer.prefixCoverage("+87a"))
    }

    @Test
    fun `international number is grouped for display`() {
        assertEquals("+33 6 12 34 56 78", normalizer.formatForDisplay("+33612345678"))
        assertEquals("+590 690 12 34 56", normalizer.formatForDisplay("+590690123456"))
    }

    @Test
    fun `display formatting leaves unknown input unchanged`() {
        assertEquals("0612", normalizer.formatForDisplay("0612"))
        assertEquals("+999", normalizer.formatForDisplay("+999"))
    }
}
