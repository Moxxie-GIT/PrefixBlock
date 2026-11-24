package fr.moxxie.prefixblock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY id")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules ORDER BY id")
    suspend fun getAll(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE list = :list AND type = :type AND pattern = :pattern LIMIT 1")
    suspend fun find(list: RuleList, type: RuleType, pattern: String): RuleEntity?

    /** @return identifiant de la règle insérée. */
    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE rules SET priority = :priority WHERE id = :id")
    suspend fun setPriority(id: Long, priority: Boolean)
}
