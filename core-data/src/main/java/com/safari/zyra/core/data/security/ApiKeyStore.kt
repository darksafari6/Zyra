package com.safari.zyra.core.data.security

import kotlinx.coroutines.flow.StateFlow

interface ApiKeyStore {
    val hasKey: StateFlow<Boolean>
    fun read(): String
    fun write(value: String)
    fun clear()
}