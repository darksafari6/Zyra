package com.safari.zyra.device.control

import com.safari.zyra.core.data.memory.MemoryRepository
import com.safari.zyra.core.data.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class MemoryTools @Inject constructor(private val memories: MemoryRepository, private val settings: SettingsRepository) {
    fun create(): Set<ZyraTool> = setOf(
        FunctionalZyraTool(
            "remember_fact", "Save a non-sensitive user-approved fact to long-term memory.",
            JSONObject().put("type", "OBJECT").put("properties", JSONObject()
                .put("key", JSONObject().put("type", "STRING").put("description", "Short memory title"))
                .put("value", JSONObject().put("type", "STRING").put("description", "Fact to remember")))
                .put("required", JSONArray().put("key").put("value")),
            ToolRisk.MEDIUM
        ) { args ->
            if (!settings.settings.first().memoryEnabled) return@FunctionalZyraTool ToolResult.Denied("Long-term memory is off. Enable it in Settings first.")
            val key = ToolArgumentValidator.requiredString(args, "key", 80)
            val value = ToolArgumentValidator.requiredString(args, "value", 500)
            memories.remember(key, value)
            ToolResult.Success("Saved the fact to Zyra memory.")
        },
        FunctionalZyraTool(
            "forget_fact", "Delete a stored memory by its exact title.",
            JSONObject().put("type", "OBJECT").put("properties", JSONObject()
                .put("key", JSONObject().put("type", "STRING").put("description", "Exact memory title")))
                .put("required", JSONArray().put("key")),
            ToolRisk.HIGH
        ) { args ->
            if (!settings.settings.first().memoryEnabled) return@FunctionalZyraTool ToolResult.Denied("Long-term memory is off.")
            val key = ToolArgumentValidator.requiredString(args, "key", 80)
            if (memories.forget(key)) ToolResult.Success("Deleted that memory.")
            else ToolResult.Failed("No saved memory with that exact title was found.")
        }
    )
}
