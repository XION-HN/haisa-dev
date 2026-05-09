package com.haisa.sdk.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun moduleManifest_resolveEnvVars_replacesInstallDir() {
        val manifest = ModuleManifest(
            moduleId = "env-jdk",
            envVars = mapOf(
                "JAVA_HOME" to "{{install_dir}}/usr/lib/jvm/openjdk-17",
                "PATH" to "{{install_dir}}/bin:{{old_path}}"
            )
        )

        val resolved = manifest.resolveEnvVars(
            installDir = "/data/data/com.haisa/files/modules/env-jdk/17.0.8",
            oldPath = "/system/bin"
        )

        assertEquals(
            "/data/data/com.haisa/files/modules/env-jdk/17.0.8/usr/lib/jvm/openjdk-17",
            resolved["JAVA_HOME"]
        )
        assertEquals(
            "/data/data/com.haisa/files/modules/env-jdk/17.0.8/bin:/system/bin",
            resolved["PATH"]
        )
    }

    @Test
    fun moduleManifest_defaultValues_areCorrect() {
        val manifest = ModuleManifest(moduleId = "env-base")

        assertEquals("1.0.0", manifest.version)
        assertEquals("aarch64", manifest.arch)
        assertEquals(emptyList<String>(), manifest.dependencies)
        assertEquals(emptyMap<String, String>(), manifest.envVars)
    }

    @Test
    fun installProgress_statusTransition() {
        val progress = InstallProgress(
            moduleId = "env-jdk",
            status = InstallStatus.DOWNLOADING,
            progressPercent = 50,
            downloadedBytes = 90 * 1024 * 1024,
            totalBytes = 180 * 1024 * 1024
        )

        assertEquals("env-jdk", progress.moduleId)
        assertEquals(InstallStatus.DOWNLOADING, progress.status)
        assertEquals(50, progress.progressPercent)
        assertTrue(progress.downloadedBytes < progress.totalBytes)
    }

    @Test
    fun projectTemplate_requiredModules_androidJava() {
        val modules = com.haisa.sdk.util.PathResolver.resolveModuleId("android-java")
        assertTrue(modules.contains("env-jdk"))
        assertTrue(modules.contains("env-cc"))
    }

    @Test
    fun projectTemplate_requiredModules_python() {
        val modules = com.haisa.sdk.util.PathResolver.resolveModuleId("python")
        assertTrue(modules.contains("env-python"))
    }

    @Test
    fun projectTemplate_requiredModules_nodejs() {
        val modules = com.haisa.sdk.util.PathResolver.resolveModuleId("nodejs")
        assertTrue(modules.contains("env-node"))
    }

    @Test
    fun projectTemplate_requiredModules_unknown() {
        val modules = com.haisa.sdk.util.PathResolver.resolveModuleId("unknown")
        assertTrue(modules.contains("env-base"))
    }

    @Test
    fun projectConfig_creation() {
        val config = ProjectConfig(
            name = "TestApp",
            path = "/projects/TestApp",
            template = ProjectTemplate.ANDROID_JAVA,
            requiredModules = listOf("env-jdk", "env-cc"),
            buildTool = "gradle"
        )

        assertEquals("TestApp", config.name)
        assertEquals(ProjectTemplate.ANDROID_JAVA, config.template)
        assertEquals("gradle", config.buildTool)
        assertEquals(2, config.requiredModules.size)
    }
}