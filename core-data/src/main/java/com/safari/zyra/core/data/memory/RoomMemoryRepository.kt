package com.safari.zyra.core.data.memory

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomMemoryRepository @Inject constructor(private val dao: MemoryFactDao) : MemoryRepository {
    override val facts: Flow<List<MemoryFact>> = dao.observeAll()
    override suspend fun remember(key: String, value: String) {
        require(key.isNotBlank() && key.length <= 80) { "Memory title must be 1–80 characters." }
        require(value.isNotBlank() && value.length <= 500) { "Memory must be 1–500 characters." }
        require(!SecretDetector.looksSensitive("$key $value")) {
            "I can't save passwords, API keys, card numbers, or other secrets."
        }
        dao.upsert(MemoryFact(factKey = key.trim(), factValue = value.trim()))
    }
    override suspend fun forget(key: String): Boolean = dao.deleteByKey(key.trim()) > 0
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun clear() = dao.clear()
    override suspend fun latest(limit: Int): List<MemoryFact> = dao.latest(limit.coerceIn(1, 100))
}

object SecretDetector {
    private val patterns = listOf(
        Regex("(?i)(password|passcode|secret|api[_ -]?key|access[_ -]?token|private[_ -]?key)\\s*(?:is|[:=])\\s*\\S+"),
        Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)"),
        Regex("(?i)-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
        Regex("(?i)bearer\\s+[a-z0-9._~+/=-]{12,}"),
        Regex("AIza[0-9A-Za-z_-]{30,}"),
        Regex("AKIA[0-9A-Z]{16}"),
        Regex("(?i)gh[pousr]_[A-Za-z0-9]{20,}"),
        Regex("(?i)sk-[0-9A-Za-z]{20,}"),
        Regex("[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,}")
    )
    fun looksSensitive(value: String): Boolean = patterns.any { it.containsMatchIn(value) }
}
