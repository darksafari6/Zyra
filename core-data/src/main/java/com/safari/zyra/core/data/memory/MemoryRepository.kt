package com.safari.zyra.core.data.memory

import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    val facts: Flow<List<MemoryFact>>
    suspend fun remember(key: String, value: String)
    suspend fun forget(key: String): Boolean
    suspend fun delete(id: Long)
    suspend fun clear()
    suspend fun latest(limit: Int = 30): List<MemoryFact>
}
