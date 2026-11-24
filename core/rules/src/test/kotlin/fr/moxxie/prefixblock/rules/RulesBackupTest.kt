package fr.moxxie.prefixblock.rules

import fr.moxxie.prefixblock.rules.RulesBackup.DecodeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesBackupTest {

    private fun decodeOk(text: String) = RulesBackup.decode(text) as DecodeResult.Success

    @Test
    fun `export then import gives the same rules`() {
        val rules = DefaultRules.initial + listOf(
            Rule(list = RuleList.WHITELIST, type = RuleType.NUMBER, pattern = "+590690000000"),
            Rule(
                list = RuleList.BLACKLIST, type = RuleType.REGEX, pattern = """^\+4475\d{8}$""",
                enabled = false, priority = true,
            ),
        )
        val decoded = decodeOk(RulesBackup.encode(rules))
        assertEquals(rules.map { it.copy(id = 0) }, decoded.rules)
        assertEquals(0, decoded.malformedEntries)
    }

    @Test
    fun `example from the specification is read`() {
        val text = """
            {
              "format": "prefixblock-rules",
              "version": 1,
              "rules": [
                { "list": "BLACKLIST", "type": "PREFIX", "pattern": "+870", "label": "Inmarsat", "enabled": true, "priority": false },
                { "list": "WHITELIST", "type": "NUMBER", "pattern": "+590690000000", "enabled": true, "priority": false },
                { "list": "BLACKLIST", "type": "REGEX", "pattern": "^\\+4475\\d{8}$", "enabled": false, "priority": false }
              ]
            }
        """.trimIndent()
        val rules = decodeOk(text).rules
        assertEquals(3, rules.size)
        assertEquals("Inmarsat", rules[0].label)
        assertEquals(null, rules[1].label)
        assertEquals("""^\+4475\d{8}$""", rules[2].pattern)
        assertFalse(rules[2].enabled)
    }

    @Test
    fun `export is versioned and omits empty labels`() {
        val text = RulesBackup.encode(listOf(Rule(list = RuleList.BLACKLIST, type = RuleType.PREFIX, pattern = "+674")))
        assertTrue(""""format": "prefixblock-rules"""" in text)
        assertTrue(""""version": 1""" in text)
        assertFalse("label" in text)
    }

    @Test
    fun `missing optional fields take safe defaults`() {
        val rule = decodeOk(
            """{"format":"prefixblock-rules","version":1,"rules":[{"list":"BLACKLIST","type":"PREFIX","pattern":"+683"}]}""",
        ).rules.single()
        assertTrue(rule.enabled)
        assertFalse(rule.priority)
    }

    @Test
    fun `malformed entries are skipped and counted`() {
        val decoded = decodeOk(
            """
            {"format":"prefixblock-rules","version":1,"rules":[
              {"list":"BLACKLIST","type":"PREFIX","pattern":"+870"},
              {"list":"GREYLIST","type":"PREFIX","pattern":"+871"},
              {"list":"BLACKLIST","type":"PREFIX"},
              "texte"
            ]}
            """.trimIndent(),
        )
        assertEquals(listOf("+870"), decoded.rules.map { it.pattern })
        assertEquals(3, decoded.malformedEntries)
    }

    @Test
    fun `unknown fields from a newer minor format are ignored`() {
        val decoded = decodeOk(
            """{"format":"prefixblock-rules","version":1,"exportedAt":"2026-11-24","rules":[{"list":"BLACKLIST","type":"PREFIX","pattern":"+870","color":"red"}]}""",
        )
        assertEquals(1, decoded.rules.size)
    }

    @Test
    fun `other files are rejected`() {
        assertEquals(DecodeResult.NotABackup, RulesBackup.decode("pas du json"))
        assertEquals(DecodeResult.NotABackup, RulesBackup.decode("[]"))
        assertEquals(DecodeResult.NotABackup, RulesBackup.decode("""{"format":"autre","version":1,"rules":[]}"""))
        assertEquals(DecodeResult.NotABackup, RulesBackup.decode("""{"format":"prefixblock-rules","rules":[]}"""))
    }

    @Test
    fun `newer version is reported`() {
        assertEquals(
            DecodeResult.UnsupportedVersion(2),
            RulesBackup.decode("""{"format":"prefixblock-rules","version":2,"rules":[]}"""),
        )
    }
}
