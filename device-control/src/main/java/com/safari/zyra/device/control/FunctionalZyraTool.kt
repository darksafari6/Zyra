package com.safari.zyra.device.control

import org.json.JSONObject

class FunctionalZyraTool(
    override val name: String,
    override val description: String,
    override val parametersSchema: JSONObject,
    override val risk: ToolRisk,
    override val requiredPermissions: Set<String> = emptySet(),
    private val action: suspend (JSONObject) -> ToolResult
) : ZyraTool {
    override suspend fun execute(arguments: JSONObject): ToolResult = try {
        action(arguments)
    } catch (security: SecurityException) {
        ToolResult.Denied("Android denied this action. Check the permission or system setting.")
    } catch (invalid: IllegalArgumentException) {
        ToolResult.Failed(invalid.message ?: "The supplied action details are invalid.")
    } catch (failure: Exception) {
        ToolResult.Failed(failure.message ?: "The action could not be completed.")
    }
}
