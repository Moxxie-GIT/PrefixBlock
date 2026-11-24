package fr.moxxie.prefixblock.data.backup

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.data.rules.SaveRuleResult
import fr.moxxie.prefixblock.rules.RulesBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Résultat d'un import, affiché à l'utilisateur. */
sealed interface ImportResult {
    /**
     * @param added règles ajoutées ; [alreadyPresent] identiques à une règle existante ;
     * [invalid] entrées illisibles ou motifs refusés par la validation.
     */
    data class Imported(val added: Int, val alreadyPresent: Int, val invalid: Int) : ImportResult
    data object NotABackup : ImportResult
    data class UnsupportedVersion(val version: Int) : ImportResult
    data object ReadError : ImportResult
}

/**
 * Lecture et écriture de fichiers choisis par l'utilisateur (sélecteur de documents Android :
 * aucune permission de stockage nécessaire). Séparé pour tester l'import sans Android.
 */
interface BackupFiles {
    /** @throws IOException si le fichier est illisible ou trop gros. */
    suspend fun read(uri: String): String

    /** @throws IOException si l'écriture échoue. */
    suspend fun write(uri: String, text: String)
}

/**
 * Export et import des règles (CLAUDE.md §8, format v1). L'import **ajoute** les règles absentes,
 * après la même validation qu'une saisie manuelle ; il ne modifie ni ne supprime aucune règle existante.
 */
@Singleton
class RulesBackupRepository @Inject constructor(
    private val rules: RuleRepository,
    private val files: BackupFiles,
) {

    /** @return nombre de règles exportées, ou `null` si l'écriture a échoué. */
    suspend fun export(uri: String): Int? {
        val all = rules.getRules()
        return try {
            files.write(uri, RulesBackup.encode(all))
            all.size
        } catch (_: IOException) {
            null
        }
    }

    suspend fun import(uri: String): ImportResult {
        val text = try {
            files.read(uri)
        } catch (_: IOException) {
            return ImportResult.ReadError
        }
        return when (val decoded = RulesBackup.decode(text)) {
            RulesBackup.DecodeResult.NotABackup -> ImportResult.NotABackup
            is RulesBackup.DecodeResult.UnsupportedVersion -> ImportResult.UnsupportedVersion(decoded.version)
            is RulesBackup.DecodeResult.Success -> {
                var added = 0
                var alreadyPresent = 0
                var invalid = decoded.malformedEntries
                decoded.rules.forEach { rule ->
                    val draft = RuleDraft(rule.list, rule.type, rule.pattern, rule.label, rule.enabled, rule.priority)
                    when (rules.addRule(draft)) {
                        is SaveRuleResult.Saved -> added++
                        SaveRuleResult.Duplicate -> alreadyPresent++
                        else -> invalid++
                    }
                }
                ImportResult.Imported(added, alreadyPresent, invalid)
            }
        }
    }
}

/** Accès aux documents via le `ContentResolver` (URI fournies par le sélecteur de documents). */
@Singleton
class ContentResolverBackupFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupFiles {

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Fichier introuvable")
        stream.use { input ->
            val content = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                content.write(chunk, 0, read)
                if (content.size() > MAX_FILE_BYTES) throw IOException("Fichier trop gros")
            }
            content.toString(Charsets.UTF_8.name())
        }
    }

    override suspend fun write(uri: String, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw IOException("Écriture impossible")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private companion object {
        /** Une sauvegarde de plusieurs milliers de règles pèse quelques centaines de Ko. */
        const val MAX_FILE_BYTES = 1_000_000
    }
}
