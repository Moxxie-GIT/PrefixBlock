package fr.moxxie.prefixblock.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Insert
    suspend fun insert(call: BlockedCallEntity): Long

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedCallEntity>>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("DELETE FROM blocked_calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()
}
