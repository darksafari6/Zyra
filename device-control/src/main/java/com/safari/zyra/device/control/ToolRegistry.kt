package com.safari.zyra.device.control

import javax.inject.Inject
import javax.inject.Singleton
import com.safari.zyra.core.network.GeminiFunctionDeclaration

@Singleton
class ToolRegistry @Inject constructor(factory: AndroidToolFactory, memoryTools: MemoryTools) {
    private val byName = (factory.create() + memoryTools.create()).associateBy { it.name }
    fun get(name: String): ZyraTool? = byName[name]
    fun all(): List<ZyraTool> = byName.values.sortedBy { it.name }
    fun declarations(enabled: Map<String, Boolean>): List<GeminiFunctionDeclaration> = all().filter { enabled[it.name] == true }.map { GeminiFunctionDeclaration(it.name, it.description, it.parametersSchema) }
}
