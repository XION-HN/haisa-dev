package com.haisa.sdk.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathResolverTest {

    private val baseDir = "/data/data/com.haisa/files"

    @Test
    fun getModulesBaseDir_returnsCorrectPath() {
        val result = PathResolver.getModulesBaseDir(baseDir)
        assertEquals("$baseDir/modules", result)
    }

    @Test
    fun getModuleDir_returnsCorrectPath() {
        val result = PathResolver.getModuleDir(baseDir, "env-jdk", "17.0.8")
        assertEquals("$baseDir/modules/env-jdk/17.0.8", result)
    }

    @Test
    fun getModuleBinDir_returnsCorrectPath() {
        val result = PathResolver.getModuleBinDir(baseDir, "env-jdk", "17.0.8")
        assertEquals("$baseDir/modules/env-jdk/17.0.8/bin", result)
    }

    @Test
    fun getModuleLibDir_returnsCorrectPath() {
        val result = PathResolver.getModuleLibDir(baseDir, "env-jdk", "17.0.8")
        assertEquals("$baseDir/modules/env-jdk/17.0.8/lib", result)
    }

    @Test
    fun getActiveSymlinkPath_returnsCorrectPath() {
        val result = PathResolver.getActiveSymlinkPath(baseDir, "env-jdk")
        assertEquals("$baseDir/modules/env-jdk/active", result)
    }

    @Test
    fun resolveModuleId_androidJava_returnsJdkAndCc() {
        val result = PathResolver.resolveModuleId("android-java")
        assertTrue(result.contains("env-jdk"))
        assertTrue(result.contains("env-cc"))
    }

    @Test
    fun resolveModuleId_python_returnsPython() {
        val result = PathResolver.resolveModuleId("python")
        assertEquals(listOf("env-python"), result)
    }

    @Test
    fun resolveModuleId_cNative_returnsCc() {
        val result = PathResolver.resolveModuleId("c-native")
        assertEquals(listOf("env-cc"), result)
    }

    @Test
    fun resolveModuleId_nodejs_returnsNode() {
        val result = PathResolver.resolveModuleId("nodejs")
        assertEquals(listOf("env-node"), result)
    }

    @Test
    fun resolveModuleId_unknown_returnsBase() {
        val result = PathResolver.resolveModuleId("unknown")
        assertEquals(listOf("env-base"), result)
    }
}