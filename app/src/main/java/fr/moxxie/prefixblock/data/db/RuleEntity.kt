package fr.moxxie.prefixblock.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType

/** Règle persistée. Un même motif ne peut figurer qu'une fois par liste et par type. */
@Entity(
    tableName = RuleEntity.TABLE,
    indices = [Index(value = ["list", "type", "pattern"], unique = true)],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val list: RuleList,
    val type: RuleType,
    val pattern: String,
    val label: String?,
    val enabled: Boolean,
    val priority: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toDomain() = Rule(
        id = id,
        list = list,
        type = type,
        pattern = pattern,
        label = label,
        enabled = enabled,
        priority = priority,
    )

    companion object {
        const val TABLE = "rules"

        fun fromDomain(rule: Rule, createdAt: Long) = RuleEntity(
            id = rule.id,
            list = rule.list,
            type = rule.type,
            pattern = rule.pattern,
            label = rule.label,
            enabled = rule.enabled,
            priority = rule.priority,
            createdAt = createdAt,
        )
    }
}
