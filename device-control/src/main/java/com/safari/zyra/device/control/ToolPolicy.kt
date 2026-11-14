package com.safari.zyra.device.control

import com.safari.zyra.core.data.settings.ZyraSettings

class ToolPolicy {
    fun evaluate(tool: ZyraTool, settings: ZyraSettings, confirmedThisSession: Set<String>): ToolResult? {
        if (!settings.toolsEnabled) return ToolResult.Denied("Device tools are disabled by the global kill switch.")
        if (settings.toolToggles[tool.name] != true) return ToolResult.Denied("The ${tool.name} tool is disabled in Settings.")
        return when (tool.risk) {
            ToolRisk.LOW -> null
            ToolRisk.MEDIUM -> if (tool.name in confirmedThisSession) null
                else ToolResult.NeedsUserAction("Confirm once this session to allow ${tool.name}.")
            ToolRisk.HIGH -> ToolResult.NeedsUserAction("Confirm this exact ${tool.name} action before it can run.")
        }
    }
}
