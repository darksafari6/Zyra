package com.safari.zyra.core.data.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryFactDao {
    @Query("SELECT * FROM memory_facts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryFact>>
    @Query("SELECT * FROM memory_facts ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<MemoryFact>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fact: MemoryFact): Long
    @Query("DELETE FROM memory_facts WHERE factKey = :key")
    suspend fun deleteByKey(key: String): Int
    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("DELETE FROM memory_facts")
    suspend fun clear()
}
