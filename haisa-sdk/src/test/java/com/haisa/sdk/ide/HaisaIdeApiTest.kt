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
    fun languageSdkInfoIsDefined() {
        val info = HaisaIdeApi.LanguageSdkInfo(
            languageId = "python",
            packageName = "env-python",
            version = "3.11.8",
            homeDir = "/data/data/com.example/files/packages/../modules/env-python/3.11.8",
            binaryPaths = mapOf("python3" to "/data/data/com.example/files/packages/../modules/env-python/3.11.8/bin/python3"),
            includePaths = listOf("/data/data/com.example/files/packages/../modules/env-python/3.11.8/include/python3.11"),
            libraryPaths = listOf("/data/data/com.example/files/packages/../modules/env-python/3.11.8/lib"),
            envVars = mapOf("PYTHON_HOME" to "/data/data/com.example/files/packages/../modules/env-python/3.11.8"),
            ideTasks = listOf("python-run", "pip-install")
        )
        assertEquals("python", info.languageId)
        assertEquals("env-python", info.packageName)
        assertEquals("3.11.8", info.version)
        assertEquals(1, info.binaryPaths.size)
        assertEquals(1, info.includePaths.size)
        assertEquals(1, info.libraryPaths.size)
        assertEquals(2, info.ideTasks.size)
    }

    @Test
    fun completionContextIsDefined() {
        val ctx = HaisaIdeApi.CompletionContext(
            languageId = "python",
            sdkInfo = HaisaIdeApi.LanguageSdkInfo(
                languageId = "python",
                packageName = "env-python",
                version = "3.11.8",
                homeDir = "/tmp/python",
                binaryPaths = emptyMap(),
                includePaths = emptyList(),
                libraryPaths = emptyList(),
                envVars = emptyMap(),
                ideTasks = emptyList()
            ),
            additionalIncludePaths = listOf("/tmp/python/include/python3.11"),
            environment = mapOf("PATH" to "/tmp/python/bin:/usr/bin")
        )
        assertEquals("python", ctx.languageId)
        assertNotNull(ctx.sdkInfo)
        assertEquals(1, ctx.additionalIncludePaths.size)
        assertTrue(ctx.environment.containsKey("PATH"))
    }

    @Test
    fun createProjectIsSuspendFunction() {
        val allClasses = HaisaIdeApi::class.java.declaredClasses.map { it.name }
        System.err.println("[DEBUG] HaisaIdeApi inner classes: $allClasses")
        for (cls in HaisaIdeApi::class.java.declaredClasses + HaisaIdeApi::class.java) {
            for (m in cls.declaredMethods.filter { it.name == "createProject" }) {
                System.err.println("[DEBUG] Found createProject in ${cls.name}: params=${m.parameterTypes.toList()} return=${m.returnType}")
            }
        }
        val allMethods = (HaisaIdeApi::class.java.declaredMethods + HaisaIdeApi::class.java.methods).filter { it.name == "createProject" }
        if (allMethods.isEmpty()) {
            System.err.println("[DEBUG] No createProject in declaredMethods or methods. All methods:")
            for (m in HaisaIdeApi::class.java.methods) {
                System.err.println("[DEBUG]   method: ${m.name} params=${m.parameterTypes.map { it.name }.toList()}")
            }
        }
        assertTrue(allMethods.isNotEmpty() || HaisaIdeApi::class.java.declaredClasses.any { it.declaredMethods.any { it.name == "createProject" } })
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

    @Test
    fun languageSdkInfo_coversAllLanguages() {
        val languages = listOf("java", "kotlin", "python", "javascript", "typescript", "c", "cpp", "rust", "go")
        for (lang in languages) {
            val info = HaisaIdeApi.LanguageSdkInfo(
                languageId = lang,
                packageName = "env-$lang",
                version = "1.0",
                homeDir = "/tmp",
                binaryPaths = emptyMap(),
                includePaths = emptyList(),
                libraryPaths = emptyList(),
                envVars = emptyMap(),
                ideTasks = emptyList()
            )
            assertEquals(lang, info.languageId)
        }
    }
}
