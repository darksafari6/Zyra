package com.safari.zyra.device.control

import com.safari.zyra.core.data.settings.ZyraSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolPolicyTest {
    private fun tool(risk: ToolRisk) = object : ZyraTool {
        override val name = "test_tool"
        override val description = "test"
        override val parametersSchema = mockk<JSONObject>()
        override val risk = risk
        override val requiredPermissions = emptySet<String>()
        override suspend fun execute(arguments: JSONObject) = ToolResult.Success("ok")
    }

    @Test fun globalAndIndividualSwitchesMustBothBeEnabled() {
        val policy = ToolPolicy()
        assertEquals("Device tools are disabled by the global kill switch.",
            (policy.evaluate(tool(ToolRisk.LOW), ZyraSettings(), emptySet()) as ToolResult.Denied).detail)
        val globalOn = ZyraSettings(toolsEnabled = true)
        assertEquals("The test_tool tool is disabled in Settings.",
            (policy.evaluate(tool(ToolRisk.LOW), globalOn, emptySet()) as ToolResult.Denied).detail)
        assertNull(policy.evaluate(tool(ToolRisk.LOW), globalOn.copy(toolToggles = mapOf("test_tool" to true)), emptySet()))
    }

    @Test fun mediumConfirmsOnceAndHighConfirmsEveryTime() {
        val policy = ToolPolicy()
        val enabled = ZyraSettings(toolsEnabled = true, toolToggles = mapOf("test_tool" to true))
        assertEquals(ToolRisk.MEDIUM, tool(ToolRisk.MEDIUM).risk)
        assertEquals(true, policy.evaluate(tool(ToolRisk.MEDIUM), enabled, emptySet()) is ToolResult.NeedsUserAction)
        assertNull(policy.evaluate(tool(ToolRisk.MEDIUM), enabled, setOf("test_tool")))
        assertEquals(true, policy.evaluate(tool(ToolRisk.HIGH), enabled, setOf("test_tool")) is ToolResult.NeedsUserAction)
    }
}
