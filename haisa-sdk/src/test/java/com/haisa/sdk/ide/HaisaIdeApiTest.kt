package com.haisa.sdk.ide

import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import org.junit.Assert.*
import org.junit.Test

class HaisaIdeApiTest {

    @Test
    fun apiInterfaceIsDefined() {
        assertNotNull(HaisaIdeApi::class.java)
        assertTrue(HaisaIdeApi::class.java.isInterface)
    }

    @Test
    fun terminalSessionInterfaceIsDefined() {
        assertNotNull(HaisaIdeApi.TerminalSession::class.java)
        assertTrue(HaisaIdeApi.TerminalSession::class.java.isInterface)
    }

    @Test
    fun projectListenerHasDefaultImplementations() {
        val listener = object : HaisaIdeApi.ProjectListener {}
        listener.onProjectCreated(
            ProjectConfig(
                name = "test",
                path = "/tmp/test",
                template = ProjectTemplate.PYTHON,
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
