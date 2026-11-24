package fr.moxxie.prefixblock.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.moxxie.prefixblock.rules.DefaultRules
import fr.moxxie.prefixblock.rules.Rule

/**
 * Installe les règles initiales (CLAUDE.md §8) à la création de la base, dans la même
 * transaction : elles existent avant toute lecture. Une fois supprimées par l'utilisateur,
 * elles ne reviennent pas.
 */
class DefaultRulesCallback(
    private val clock: () -> Long = System::currentTimeMillis,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = clock()
        DefaultRules.initial.forEach { rule ->
            db.insert(RuleEntity.TABLE, SQLiteDatabase.CONFLICT_IGNORE, rule.toContentValues(now))
        }
    }

    private fun Rule.toContentValues(createdAt: Long) = contentValuesOf(
        "list" to list.name,
        "type" to type.name,
        "pattern" to pattern,
        "label" to label,
        "enabled" to enabled,
        "priority" to priority,
        "created_at" to createdAt,
    )
}
