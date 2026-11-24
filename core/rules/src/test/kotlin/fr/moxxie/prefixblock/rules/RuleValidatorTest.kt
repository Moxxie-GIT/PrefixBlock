package fr.moxxie.prefixblock.rules

import fr.moxxie.prefixblock.rules.PatternError.EMPTY
import fr.moxxie.prefixblock.rules.PatternError.NUMBER_FORMAT
import fr.moxxie.prefixblock.rules.PatternError.PREFIX_FORMAT
import fr.moxxie.prefixblock.rules.PatternError.PREFIX_TOO_LONG
import fr.moxxie.prefixblock.rules.PatternError.REGEX_SYNTAX
import fr.moxxie.prefixblock.rules.PatternError.REGEX_TOO_BROAD
import fr.moxxie.prefixblock.rules.PatternError.REGEX_TOO_LONG
import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleValidatorTest {

    private val validator = RuleValidator(PhoneNumberNormalizer())

    private fun valid(pattern: String) = PatternValidation.Valid(pattern)
    private fun invalid(error: PatternError) = PatternValidation.Invalid(error)

    @Test
    fun `prefixes are normalized to plus and digits`() {
        assertEquals(valid("+870"), validator.validatePrefix("+870"))
        assertEquals(valid("+870"), validator.validatePrefix("00870"))
        assertEquals(valid("+1876"), validator.validatePrefix(" +1 876 "))
        assertEquals(valid("+1876"), validator.validatePrefix("+1-876"))
    }

    @Test
    fun `prefixes must be international`() {
        assertEquals(invalid(EMPTY), validator.validatePrefix(""))
        assertEquals(invalid(PREFIX_FORMAT), validator.validatePrefix("870"))
        assertEquals(invalid(PREFIX_FORMAT), validator.validatePrefix("+"))
        assertEquals(invalid(PREFIX_FORMAT), validator.validatePrefix("+0870"))
        assertEquals(invalid(PREFIX_FORMAT), validator.validatePrefix("+87a"))
        assertEquals(invalid(PREFIX_TOO_LONG), validator.validatePrefix("+1234567890123456"))
    }

    @Test
    fun `complete numbers are normalized with the default region`() {
        assertEquals(valid("+590690123456"), validator.validateNumber("0690 12 34 56", "GP"))
        assertEquals(valid("+447911123456"), validator.validateNumber("+44 7911 123456", "GP"))
        assertEquals(invalid(EMPTY), validator.validateNumber(" ", "GP"))
        assertEquals(invalid(NUMBER_FORMAT), validator.validateNumber("abc", "GP"))
    }

    @Test
    fun `regex syntax and length are checked`() {
        assertEquals(valid("""^\+44(7624|7781)"""), validator.validateRegex("""^\+44(7624|7781)""", BLACKLIST))
        assertEquals(invalid(REGEX_SYNTAX), validator.validateRegex("[", BLACKLIST))
        assertEquals(invalid(EMPTY), validator.validateRegex("  ", BLACKLIST))
        assertEquals(invalid(REGEX_TOO_LONG), validator.validateRegex("1".repeat(201), BLACKLIST))
    }

    @Test
    fun `blocking regex matching every country is refused`() {
        assertEquals(invalid(REGEX_TOO_BROAD), validator.validateRegex(".*", BLACKLIST))
        assertEquals(invalid(REGEX_TOO_BROAD), validator.validateRegex("""^\+""", BLACKLIST))
        assertEquals(invalid(REGEX_TOO_BROAD), validator.validateRegex("""\d""", BLACKLIST))
    }

    @Test
    fun `broad allowing regex is harmless and accepted`() {
        assertEquals(valid(".*"), validator.validateRegex(".*", WHITELIST))
    }

    @Test
    fun `broad but not universal blocking regex is accepted`() {
        // Ne touche pas +1 : la décision reste explicite et protégée par la liste blanche.
        assertEquals(valid("""^\+[2-9]"""), validator.validateRegex("""^\+[2-9]""", BLACKLIST))
    }

    @Test
    fun `validate dispatches on rule type`() {
        assertEquals(valid("+870"), validator.validate(BLACKLIST, RuleType.PREFIX, "00870", "GP"))
        assertEquals(valid("+33612345678"), validator.validate(WHITELIST, RuleType.NUMBER, "0612345678", "FR"))
        assertEquals(invalid(REGEX_SYNTAX), validator.validate(BLACKLIST, RuleType.REGEX, "(", "GP"))
    }
}
