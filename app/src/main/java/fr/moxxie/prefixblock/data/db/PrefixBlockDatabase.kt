package fr.moxxie.prefixblock.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base locale de PrefixBlock (stockage chiffré par l'utilisateur, donc inaccessible avant le
 * premier déverrouillage après un redémarrage).
 *
 * Le schéma est exporté dans `app/schemas/` : toute évolution passe par une migration
 * (jamais de migration destructive, qui effacerait les règles de l'utilisateur).
 */
@Database(
    entities = [RuleEntity::class, BlockedCallEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PrefixBlockDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        const val NAME = "prefixblock.db"
    }
}
