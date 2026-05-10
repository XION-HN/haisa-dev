package com.haisa.sdk.ide

import org.junit.Assert.*
import org.junit.Test

class HaisaIdeApiTest {

    @Test
    fun apiInterfaceDefinesAllRequiredMethods() {
        val methods = HaisaIdeApi::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue(methods.contains("getAvailableModules"))
        assertTrue(methods.contains("installModule"))
        assertTrue(methods.contains("uninstallModule"))
        assertTrue(methods.contains("isModuleInstalled"))
        assertTrue(methods.contains("getModuleEnvironment"))
        assertTrue(methods.contains("executeBuild"))
        assertTrue(methods.contains("createProject"))
        assertTrue(methods.contains("openTerminal"))
        assertTrue(methods.contains("addProjectListener"))
        assertTrue(methods.contains("removeProjectListener"))
    }

    @Test
    fun terminalSessionInterfaceDefinesRequiredMethods() {
        val methods = HaisaIdeApi.TerminalSession::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue(methods.contains("write"))
        assertTrue(methods.contains("readOutput"))
        assertTrue(methods.contains("close"))
    }

    @Test
    fun projectListenerHasDefaultImplementations() {
        val listener = object : HaisaIdeApi.ProjectListener {}
        listener.onProjectCreated(
            com.haisa.sdk.model.ProjectConfig(
                name = "test",
                path = "/tmp/test",
                template = com.haisa.sdk.model.ProjectTemplate.PYTHON,
                requiredModules = emptyList(),
                buildTool = "pip"
            )
        )
        listener.onBuildStarted("/tmp/test")
        listener.onBuildCompleted("/tmp/test", true)
        listener.onModuleInstalled("env-python")
        listener.onModuleUninstalled("env-python")
    }
}
