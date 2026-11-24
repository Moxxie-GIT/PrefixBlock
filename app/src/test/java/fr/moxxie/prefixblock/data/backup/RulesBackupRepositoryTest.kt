package fr.moxxie.prefixblock.data.backup

import fr.moxxie.prefixblock.data.rules.FakeRuleDao
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import fr.moxxie.prefixblock.rules.RuleType.NUMBER
import fr.moxxie.prefixblock.rules.RuleType.PREFIX
import fr.moxxie.prefixblock.rules.RuleType.REGEX
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock

class RulesBackupRepositoryTest {

    private class MemoryFiles : BackupFiles {
        val content = mutableMapOf<String, String>()
        var failWrites = false
        override suspend fun read(uri: String): String = content[uri] ?: throw IOException("absent")
        override suspend fun write(uri: String, text: String) {
            if (failWrites) throw IOException("disque plein")
            content[uri] = text
        }
    }

    private fun repository() = RoomRuleRepository(
        FakeRuleDao(), RuleValidator(PhoneNumberNormalizer()), { "gp" }, Clock.systemUTC(),
    )

    private val files = MemoryFiles()

    @Test
    fun `rules exported from one phone are restored on another`() = runTest {
        val source = repository()
        source.addRule(RuleDraft(BLACKLIST, PREFIX, "+870", label = "Inmarsat"))
        source.addRule(RuleDraft(BLACKLIST, REGEX, """^\+4475\d{8}$""", enabled = false, priority = true))
        source.addRule(RuleDraft(WHITELIST, NUMBER, "+590690000000"))
        assertEquals(3, RulesBackupRepository(source, files).export("content://sauvegarde"))

        val target = repository()
        val result = RulesBackupRepository(target, files).import("content://sauvegarde")

        assertEquals(ImportResult.Imported(added = 3, alreadyPresent = 0, invalid = 0), result)
        assertEquals(
            source.getRules().map { it.copy(id = 0) }.toSet(),
            target.getRules().map { it.copy(id = 0) }.toSet(),
        )
    }

    @Test
    fun `import adds missing rules and never changes existing ones`() = runTest {
        val rules = repository()
        rules.addRule(RuleDraft(BLACKLIST, PREFIX, "+870", label = "Mon libellé", enabled = false))
        files.content["f"] = """
            {"format":"prefixblock-rules","version":1,"rules":[
              {"list":"BLACKLIST","type":"PREFIX","pattern":"+870","label":"Inmarsat","enabled":true},
              {"list":"BLACKLIST","type":"PREFIX","pattern":"00674"}
            ]}
        """.trimIndent()

        val result = RulesBackupRepository(rules, files).import("f")

        assertEquals(ImportResult.Imported(added = 1, alreadyPresent = 1, invalid = 0), result)
        val inmarsat = rules.getRules().single { it.pattern == "+870" }
        assertEquals("Mon libellé", inmarsat.label)
        assertFalse(inmarsat.enabled)
        assertTrue(rules.getRules().any { it.pattern == "+674" })
    }

    @Test
    fun `imported patterns go through the same validation as manual input`() = runTest {
        val rules = repository()
        files.content["f"] = """
            {"format":"prefixblock-rules","version":1,"rules":[
              {"list":"BLACKLIST","type":"REGEX","pattern":".*"},
              {"list":"BLACKLIST","type":"PREFIX","pattern":"870"},
              {"list":"BLACKLIST","type":"PREFIX"}
            ]}
        """.trimIndent()

        val result = RulesBackupRepository(rules, files).import("f")

        assertEquals(ImportResult.Imported(added = 0, alreadyPresent = 0, invalid = 3), result)
        assertTrue("une regex bloquant tous les pays ne doit jamais entrer", rules.getRules().isEmpty())
    }

    @Test
    fun `unreadable or foreign files are reported`() = runTest {
        val backup = RulesBackupRepository(repository(), files)
        assertEquals(ImportResult.ReadError, backup.import("absent"))

        files.content["photo"] = "\u0089PNG"
        assertEquals(ImportResult.NotABackup, backup.import("photo"))

        files.content["v2"] = """{"format":"prefixblock-rules","version":2,"rules":[]}"""
        assertEquals(ImportResult.UnsupportedVersion(2), backup.import("v2"))
    }

    @Test
    fun `failed export is reported`() = runTest {
        files.failWrites = true
        assertNull(RulesBackupRepository(repository(), files).export("f"))
    }
}
