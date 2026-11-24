package fr.moxxie.prefixblock.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.moxxie.prefixblock.rules.RuleType

/**
 * Appel rejeté. Seules les informations utiles à l'historique sont conservées.
 * Le motif et le type de la règle sont copiés pour rester lisibles si la règle est supprimée
 * ensuite (le lien [ruleId] passe alors à `null`).
 */
@Entity(
    tableName = "blocked_calls",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("timestamp"), Index("rule_id")],
)
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Date de l'appel, en millisecondes depuis l'epoch. */
    val timestamp: Long,
    /** Forme internationale si elle est connue, sinon le numéro tel que présenté. */
    val number: String,
    @ColumnInfo(name = "country_code") val countryCode: Int?,
    @ColumnInfo(name = "rule_id") val ruleId: Long?,
    @ColumnInfo(name = "rule_pattern") val rulePattern: String,
    @ColumnInfo(name = "rule_type") val ruleType: RuleType,
)
