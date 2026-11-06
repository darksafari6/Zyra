package com.safari.zyra.core.data.security

interface ApiKeyStore {
    fun read(): String
    fun write(value: String)
    fun clear()
}