package com.safari.zyra.device.control

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(factory: AndroidToolFactory) {
    private val byName = factory.create().associateBy { it.name }
    fun get(name: String): ZyraTool? = byName[name]
    fun all(): List<ZyraTool> = byName.values.sortedBy { it.name }
}
