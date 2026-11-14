package com.safari.zyra.device.control

import org.json.JSONObject

enum class ToolRisk { LOW, MEDIUM, HIGH }

sealed interface ToolResult {
    val detail: String
    data class Success(override val detail: String) : ToolResult
    data class NeedsUserAction(override val detail: String, val actionId: String? = null) : ToolResult
    data class Denied(override val detail: String) : ToolResult
    data class Failed(override val detail: String) : ToolResult
}

interface ZyraTool {
    val name: String
    val description: String
    val parametersSchema: JSONObject
    val risk: ToolRisk
    val requiredPermissions: Set<String>
    fun permissionsFor(arguments: JSONObject): Set<String> = requiredPermissions
    suspend fun execute(arguments: JSONObject): ToolResult
}

data class ToolCall(val id: String, val name: String, val arguments: JSONObject)

data class ToolAuditEntry(
    val id: String,
    val toolName: String,
    val argumentsSummary: String,
    val result: ToolResult,
    val occurredAt: Long = System.currentTimeMillis()
)
