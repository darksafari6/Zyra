package com.safari.zyra.core.network

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiToolLoopTest {
    @Test
    fun functionCallIsExecutedAndFunctionResponseIsReturnedBeforeFinalAnswer() = runTest {
        var executed = 0
        var secondTurn: List<GeminiToolTurn> = emptyList()
        val model = object : GeminiToolModel {
            var count = 0
            override fun generate(contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>) = flow {
                count++
                if (count == 1) {
                    emit(GeminiPart.FunctionCall(GeminiFunctionCall("call-1", "device_info", JSONObject())))
                } else {
                    secondTurn = contents
                    emit(GeminiPart.Text("Battery is 82%."))
                }
            }
        }
        val executor = GeminiToolExecutor { name, _ ->
            assertEquals("device_info", name)
            executed++
            GeminiToolExecution("SUCCESS", "Battery: 82%.")
        }
        val output = GeminiToolLoop(model, executor).stream(
            listOf(GeminiToolTurn("user", listOf(GeminiPart.Text("Check battery")))),
            listOf(GeminiFunctionDeclaration("device_info", "Read device info", JSONObject()))
        ).toList().joinToString("")

        assertEquals("Battery is 82%.", output)
        assertEquals(1, executed)
        assertEquals("model", secondTurn[1].role)
        assertEquals("user", secondTurn[2].role)
        assertTrue((secondTurn[2].parts.single() as GeminiPart.Text).value.contains("functionResponse"))
        assertTrue((secondTurn[2].parts.single() as GeminiPart.Text).value.contains("SUCCESS"))
    }

    @Test
    fun iterationLimitPreventsAnExtraToolExecution() = runTest {
        var executed = 0
        val model = object : GeminiToolModel {
            override fun generate(contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>) = flow {
                emit(GeminiPart.FunctionCall(GeminiFunctionCall(null, "delete", JSONObject())))
            }
        }
        val output = GeminiToolLoop(model, GeminiToolExecutor { _, _ ->
            executed++
            GeminiToolExecution("SUCCESS", "done")
        }, maxIterations = 1).stream(
            listOf(GeminiToolTurn("user", listOf(GeminiPart.Text("Do it")))), emptyList()
        ).toList().joinToString("")

        assertEquals(0, executed)
        assertTrue(output.contains("tool-call limit"))
    }
}
