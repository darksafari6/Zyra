package com.safari.zyra.device.control

import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.network.GeminiToolExecution
import com.safari.zyra.core.network.GeminiToolExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

data class PendingToolAction(val id: String, val toolName: String, val arguments: JSONObject, val preview: String, val risk: ToolRisk, val requiredPermissions: Set<String>)

@Singleton
class ToolExecutionEngine @Inject constructor(
    private val registry: ToolRegistry,
    private val settings: SettingsRepository,
    private val audit: ToolAuditLogger,
    private val policy: ToolPolicy
) : GeminiToolExecutor {
    private val confirmedMedium = mutableSetOf<String>()
    private val pendingState = MutableStateFlow<PendingToolAction?>(null)
    val pending: StateFlow<PendingToolAction?> = pendingState.asStateFlow()

    override suspend fun execute(name: String, arguments: JSONObject): GeminiToolExecution {
        val result = executeTool(name, arguments)
        return GeminiToolExecution(when (result) { is ToolResult.Success -> "SUCCESS"; is ToolResult.NeedsUserAction -> "NEEDS_USER_ACTION"; is ToolResult.Denied -> "DENIED"; is ToolResult.Failed -> "FAILED" }, result.detail)
    }

    suspend fun executeTool(name: String, arguments: JSONObject): ToolResult {
        val tool = registry.get(name) ?: return ToolResult.Denied("This tool is not available.")
        if (pendingState.value != null) return ToolResult.NeedsUserAction("Another action is already waiting for your confirmation. Confirm or cancel it first.")
        val current = settings.settings.first()
        val policyResult = policy.evaluate(tool, current, confirmedMedium)
        if (policyResult != null) {
            val pendingAction = PendingToolAction(UUID.randomUUID().toString(), name, JSONObject(arguments.toString()),
                preview(tool, arguments), tool.risk, tool.requiredPermissions)
            pendingState.value = pendingAction
            val result = ToolResult.NeedsUserAction(policyResult.detail, pendingAction.id)
            audit.record(ToolAuditEntry(pendingAction.id, name, pendingAction.preview, result))
            return result
        }
        return runTool(tool, arguments)
    }

    suspend fun confirmPending(id: String): ToolResult {
        val pendingAction = pendingState.value ?: return ToolResult.Denied("There is no pending action.")
        if (pendingAction.id != id) return ToolResult.Denied("This confirmation is no longer current.")
        val current = settings.settings.first()
        if (!current.toolsEnabled || current.toolToggles[pendingAction.toolName] != true) {
            pendingState.value = null
            return ToolResult.Denied("Device tools were disabled before confirmation.")
        }
        val tool = registry.get(pendingAction.toolName) ?: return ToolResult.Denied("This tool is no longer available.")
        if (tool.risk == ToolRisk.MEDIUM) confirmedMedium += tool.name
        pendingState.value = null
        return runTool(tool, pendingAction.arguments)
    }

    fun cancelPending(id: String) {
        if (pendingState.value?.id == id) pendingState.value = null
    }

    fun resetSessionConfirmations() { confirmedMedium.clear() }

    private suspend fun runTool(tool: ZyraTool, args: JSONObject): ToolResult {
        val id = UUID.randomUUID().toString()
        val result = try {
            withTimeout(12_000) { tool.execute(args) }
        } catch (timeout: TimeoutCancellationException) {
            ToolResult.Failed("The tool timed out before Android confirmed the action.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            ToolResult.Failed(failure.message ?: "The action timed out or failed.")
        }
        val auditResult = if (tool.name in setOf("clipboard_read", "notification_summary", "current_location") &&
            result is ToolResult.Success) ToolResult.Success("Sensitive data was accessed at the user's request; contents omitted from the action log.")
        else result
        audit.record(ToolAuditEntry(id, tool.name, preview(tool, args), auditResult))
        return result
    }

    private fun preview(tool: ZyraTool, args: JSONObject): String =
        "${tool.description}\nRequested details: ${args.toString().take(500)}"
}
