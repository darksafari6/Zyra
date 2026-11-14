package com.safari.zyra.device.control

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(tools: Set<@JvmSuppressWildcards ZyraTool>) {
    private val byName = tools.associateBy { it.name }
    fun get(name: String): ZyraTool? = byName[name]
    fun all(): List<ZyraTool> = byName.values.sortedBy { it.name }
}
